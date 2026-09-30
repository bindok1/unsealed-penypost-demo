package com.apps.unsealed.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LoginPromptBottomSheet
import com.apps.unsealed.ui.components.rememberAuthGuard
import com.apps.unsealed.ui.components.rememberStampsEnergyTabRequester
import com.apps.unsealed.ui.components.rememberWriteExitGuard
import com.apps.unsealed.ui.screens.auth.LoginScreen
import com.apps.unsealed.ui.screens.auth.RegisterScreen
import com.apps.unsealed.ui.screens.auth.SplashScreen
import com.apps.unsealed.ui.screens.compose.screen.ComposeScreen
import com.apps.unsealed.ui.screens.inbox.screen.MailboxScreen
import com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingGenderBirthdayScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingInterestsScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingLanguageBioScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingNotifyHourPrefScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingPermissionsScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingPhotoScreen
import com.apps.unsealed.ui.screens.onboarding.OnboardingTosScreen
import com.apps.unsealed.ui.screens.penpals.screen.PenPalsScreen
import com.apps.unsealed.ui.screens.profile.screen.BlockedUsersScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileAddressBookScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileInviteFriendsScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileDraftScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileSettingsScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileStampBookScreen
import com.apps.unsealed.ui.screens.profile.screen.ProfileTipsScreen
import com.apps.unsealed.ui.screens.profile.viewmodel.ProfileDraftViewModel
import com.apps.unsealed.ui.screens.selectrecipient.screen.LetterSentScreen
import com.apps.unsealed.ui.screens.selectrecipient.screen.SelectRecipientScreen
import com.apps.unsealed.ui.screens.stamps.screen.StampsScreen
import com.apps.unsealed.ui.screens.tracking.screen.DeliveryTrackingScreen
import com.apps.unsealed.ui.screens.welcome.WelcomeCarouselScreen

/** Stack-only destination (not a bottom-nav tab), so it's kept as a plain
 * route constant here instead of a [Destinations] entry — [Destinations] is
 * purpose-built for the 4 tabs iterated by the bottom nav bar. */
internal const val SelectRecipientRoute = "select_recipient"
internal const val StoreRoute = "store"

/** Reply variant — recipient already known (see [MailboxThreadRoute]'s Write
 * Letter CTA), so it skips the region-picker mock entirely. A *separate*
 * route (not optional query args on [SelectRecipientRoute]) so
 * [SelectRecipientRoute]'s literal presence in [HideBottomNavRoutes] keeps
 * working unchanged for the base case — see [MainActivity]'s
 * `isSelectRecipientRoute` for how the reply variant is matched too. */
internal const val SelectRecipientReplyRoute = "select_recipient/reply"
internal const val SelectRecipientUserIdArg = "recipientId"
internal const val SelectRecipientNameArg = "recipientName"
internal const val SelectRecipientContinentArg = "recipientContinent"
private const val SelectRecipientReplyRoutePattern =
    "$SelectRecipientReplyRoute/{$SelectRecipientUserIdArg}/{$SelectRecipientNameArg}/{$SelectRecipientContinentArg}"
private fun selectRecipientReplyRoute(recipientId: String, recipientName: String, recipientContinent: String) =
    "$SelectRecipientReplyRoute/${Uri.encode(recipientId)}/${Uri.encode(recipientName)}/${Uri.encode(recipientContinent)}"

/** Reply variant of the Write tab — same rationale as
 * [SelectRecipientReplyRoute] above, kept separate from [Destinations.Write]'s
 * plain `"write"` route so bottom-nav tab highlighting (matched via
 * `startsWith` in [com.apps.unsealed.ui.components.UnsealedBottomNavBar])
 * still resolves to the Write tab for this variant. `internal` (not
 * `private`) because [com.apps.unsealed.ui.screens.compose.ComposeViewModel]
 * also reads these via its own `SavedStateHandle` to populate
 * [com.apps.unsealed.core.database.DraftEntity.recipientId]/`recipientName`. */
private const val WriteReplyRoute = "write/reply"
internal const val WriteReplyUserIdArg = "replyToUserId"
internal const val WriteReplyNameArg = "replyToName"
internal const val WriteReplyContinentArg = "replyToContinent"
private const val WriteReplyRoutePattern =
    "$WriteReplyRoute/{$WriteReplyUserIdArg}/{$WriteReplyNameArg}/{$WriteReplyContinentArg}"
private fun writeReplyRoute(recipientId: String, recipientName: String, recipientContinent: String) =
    "$WriteReplyRoute/${Uri.encode(recipientId)}/${Uri.encode(recipientName)}/${Uri.encode(recipientContinent)}"

/** Shown after a successful send (reply or fresh) — replaces the old
 * divergent post-send navigation (straight to Inbox for a fresh send,
 * `popBackStack(Inbox)` for a reply, which silently did nothing when the
 * reply started from PenPals feed since Inbox was never on that back stack).
 * Its own CTA always resets to the PenPal feed, see its `composable(...)`. */
internal const val LetterSentRoute = "letter_sent"
internal const val LetterSentEstimatedArrivalArg = "estimatedArrivalAt"
internal const val LetterSentRecipientContinentArg = "recipientContinent"
/** Unknown (region-matched send, no known recipient identity yet) — a path
 * segment, so it can't just be blank; [UnknownRecipientNameSentinel] stands
 * in and gets unwrapped back to blank on read.
 * [com.apps.unsealed.ui.screens.selectrecipient.screen.LetterSentScreen]
 * falls back to a continent-only map label in that case. */
internal const val LetterSentRecipientNameArg = "recipientName"
internal const val LetterSentLetterIdArg = "letterId"
/** `"public"`/`"private"` — see `SelectRecipientUiState.sentVisibility`,
 * drives which post-send CTAs [com.apps.unsealed.ui.screens.selectrecipient.screen.LetterSentScreen]
 * shows (delivery tracking vs. View in Profile/Share). */
internal const val LetterSentVisibilityArg = "visibility"
/** Same sentinel trick as [LetterSentLetterIdArg] — null (no composited
 * image, e.g. low-density device) can't be a blank path segment. */
internal const val LetterSentCompositeImageUrlArg = "compositeImageUrl"
private const val UnknownRecipientNameSentinel = "_"
private const val UnknownLetterIdSentinel = "_"
private const val UnknownCompositeImageUrlSentinel = "_"
private const val LetterSentRoutePattern =
    "$LetterSentRoute/{$LetterSentEstimatedArrivalArg}/{$LetterSentRecipientContinentArg}/{$LetterSentRecipientNameArg}/" +
        "{$LetterSentLetterIdArg}/{$LetterSentVisibilityArg}/{$LetterSentCompositeImageUrlArg}"
private fun letterSentRoute(
    estimatedArrivalAt: String,
    recipientContinent: String,
    recipientName: String,
    letterId: String = "",
    visibility: String = "private",
    compositeImageUrl: String? = null,
) = "$LetterSentRoute/${Uri.encode(estimatedArrivalAt)}/${Uri.encode(recipientContinent)}/" +
        "${Uri.encode(recipientName.ifBlank { UnknownRecipientNameSentinel })}/" +
        "${Uri.encode(letterId.ifBlank { UnknownLetterIdSentinel })}/" +
        "${Uri.encode(visibility)}/" +
        Uri.encode(compositeImageUrl?.ifBlank { null } ?: UnknownCompositeImageUrlSentinel)

/** "Continue Editing" — Draft-on-Exit (`docs/todo.md` #5), same 1-arg
 * pattern as [LetterSentRoute] above. `ComposeViewModel` reads [WriteDraftIdArg]
 * via its own `SavedStateHandle` to load the saved [com.apps.unsealed.core.database.DraftEntity]
 * row on init — no explicit param threading needed through [WriteRouteContent]. */
private const val WriteDraftRoute = "write/draft"
internal const val WriteDraftIdArg = "draftId"
private const val WriteDraftRoutePattern = "$WriteDraftRoute/{$WriteDraftIdArg}"
private fun writeDraftRoute(draftId: String) = "$WriteDraftRoute/${Uri.encode(draftId)}"

/** "Continue Writing" a reply draft (see [com.apps.unsealed.ui.screens.inbox.widgets.WriteLetterButton]
 * on [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]) — carries
 * *both* [WriteReplyRoutePattern]'s recipient args and [WriteDraftRoutePattern]'s
 * draftId, unlike the plain "Continue Editing" route above (reached from the
 * Drafts list, which has no thread/recipient-continent context to hand).
 * Without the recipient args, `WriteRouteContent`'s `onSendClick` can't tell
 * this draft already has a known recipient and falls back to the full
 * [SelectRecipientRoute] picker on Send — the bug this route exists to avoid. */
private const val WriteReplyDraftRoute = "write/reply"
private const val WriteReplyDraftRoutePattern =
    "$WriteReplyDraftRoute/{$WriteReplyUserIdArg}/{$WriteReplyNameArg}/{$WriteReplyContinentArg}/draft/{$WriteDraftIdArg}"
private fun writeReplyDraftRoute(recipientId: String, recipientName: String, recipientContinent: String, draftId: String) =
    "$WriteReplyDraftRoute/${Uri.encode(recipientId)}/${Uri.encode(recipientName)}/${Uri.encode(recipientContinent)}" +
        "/draft/${Uri.encode(draftId)}"

/** Stack-only destination, same rationale as [SelectRecipientRoute] above.
 * Carries the correspondent's id/name/continent as nav args — [MailboxThreadNameArg]/
 * [MailboxThreadContinentArg] are passed straight from the tapped
 * `MailboxRoomItem` (already known client-side from `GET /mailbox`) since
 * `GET /mailbox/{threadId}` itself carries no correspondent display info
 * (see `docs/be/letters_api.md` gap note). [MainActivity] compares against
 * the base [MailboxThreadRoute] constant (not the full pattern) since
 * `NavBackStackEntry.destination.route` resolves to the route *pattern*, not
 * the filled-in value. */
internal const val MailboxThreadRoute = "mailbox_thread"
internal const val MailboxThreadIdArg = "correspondentId"
internal const val MailboxThreadNameArg = "correspondentName"
internal const val MailboxThreadContinentArg = "correspondentContinent"
private const val MailboxThreadRoutePattern =
    "$MailboxThreadRoute/{$MailboxThreadIdArg}/{$MailboxThreadNameArg}/{$MailboxThreadContinentArg}"
internal fun mailboxThreadRoute(id: String, name: String, continent: String) =
    "$MailboxThreadRoute/${Uri.encode(id)}/${Uri.encode(name)}/${Uri.encode(continent)}"

/** Stack-only destination, same rationale as [SelectRecipientRoute] above.
 * [DeliveryTrackingOtherPartyContinentArg] is the correspondent's continent
 * (already known client-side wherever this is launched from — the thread's
 * own [MailboxThreadContinentArg]) — the viewer's own continent comes from
 * [com.apps.unsealed.feature.auth.AuthViewModel] inside the screen itself,
 * and everything else (`sent_at`/`estimated_arrival_at`/`status`) is fetched
 * fresh via `GET /letters/{id}` by [DeliveryTrackingLetterIdArg], see
 * `DeliveryTrackingViewModel`. */
internal const val DeliveryTrackingRoute = "delivery_tracking"
internal const val DeliveryTrackingLetterIdArg = "letterId"
internal const val DeliveryTrackingOtherPartyContinentArg = "otherPartyContinent"
private const val DeliveryTrackingRoutePattern =
    "$DeliveryTrackingRoute/{$DeliveryTrackingLetterIdArg}/{$DeliveryTrackingOtherPartyContinentArg}"
internal fun deliveryTrackingRoute(letterId: String, otherPartyContinent: String) =
    "$DeliveryTrackingRoute/${Uri.encode(letterId)}/${Uri.encode(otherPartyContinent)}"

/** Stack-only destinations fanning out from the Profile tab, same rationale
 * as [SelectRecipientRoute] above. Grouped in [ProfileStackRoutes] so
 * [com.apps.unsealed.MainActivity] can hide the bottom nav bar on all of
 * them with one `in` check instead of five `!=` comparisons. */
internal const val ProfileDraftRoute = "profile_draft"
internal const val ProfileAddressBookRoute = "profile_address_book"
internal const val ProfileStampBookRoute = "profile_stamp_book"
internal const val ProfileInviteFriendsRoute = "profile_invite_friends"
internal const val ProfileTipsRoute = "profile_tips"
internal const val ProfileSettingsRoute = "profile_settings"
internal const val ProfileBlockedUsersRoute = "profile_blocked_users"
internal val ProfileStackRoutes = listOf(
    ProfileDraftRoute,
    ProfileAddressBookRoute,
    ProfileStampBookRoute,
    ProfileInviteFriendsRoute,
    ProfileTipsRoute,
    ProfileSettingsRoute,
    ProfileBlockedUsersRoute,
)

/** All routes where the bottom nav bar should be hidden. */
internal val HideBottomNavRoutes: Set<String> = buildSet {
    add(SplashRoute)
    add(WelcomeRoute)
    add(LoginRoute)
    add(RegisterRoute)
    addAll(OnboardingRoutes)
    add(SelectRecipientRoute)
    add(StoreRoute)
    addAll(ProfileStackRoutes)
}

@Composable
fun UnsealedNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {
        // ── Auth flow ─────────────────────────────────────────────────────────
        composable(SplashRoute) {
            SplashScreen(
                onNavigateToWelcome = {
                    navController.navigate(WelcomeRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingTosRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(WelcomeRoute) {
            WelcomeCarouselScreen(
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingTosRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
            )
        }
        composable(LoginRoute) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingTosRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(RegisterRoute) {
            RegisterScreen(
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingTosRoute) {
                        popUpTo(RegisterRoute) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Onboarding wizard ──────────────────────────────────────────────────────
        composable(OnboardingTosRoute) {
            OnboardingTosScreen(
                onNext = { navController.navigate(OnboardingPermissionsRoute) },
                onFinish = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(OnboardingPermissionsRoute) {
            OnboardingPermissionsScreen(
                onNext = { navController.navigate(OnboardingGenderBirthdayRoute) },
                onSkip = { navController.navigate(OnboardingGenderBirthdayRoute) },
            )
        }
        composable(OnboardingGenderBirthdayRoute) {
            OnboardingGenderBirthdayScreen(
                onNext = { navController.navigate(OnboardingLanguageBioRoute) },
            )
        }
        composable(OnboardingLanguageBioRoute) {
            OnboardingLanguageBioScreen(
                onNext = { navController.navigate(OnboardingPhotoRoute) },
                onSkip = { navController.navigate(OnboardingPhotoRoute) },
            )
        }
        composable(OnboardingPhotoRoute) {
            OnboardingPhotoScreen(
                onNext = { navController.navigate(OnboardingInterestsRoute) },
                onSkip = { navController.navigate(OnboardingInterestsRoute) },
            )
        }
        composable(OnboardingInterestsRoute) {
            OnboardingInterestsScreen(
                onNext = { navController.navigate(OnboardingNotifyHourRoute) },
            )
        }
        composable(OnboardingNotifyHourRoute) {
            OnboardingNotifyHourPrefScreen(
                onFinish = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Main app tabs ─────────────────────────────────────────────────────
        composable(Destinations.PenPals.route) {
            PenPalsScreen(
                onProfileClick = { navController.navigate(Destinations.Profile.route) },
                onReplyClick = { recipientId, recipientName, recipientContinent ->
                    navController.navigate(writeReplyRoute(recipientId, recipientName, recipientContinent))
                },
                onNavigateToLogin = { navController.navigate(LoginRoute) },
            )
        }
        composable(Destinations.Inbox.route) {
            MailboxScreen(
                onRoomClick = { room ->
                    navController.navigate(
                        mailboxThreadRoute(room.correspondentId, room.correspondentName, room.correspondentContinent),
                    )
                },
            )
        }
        composable(Destinations.Write.route) {
            WriteRouteContent(navController = navController, replyToUserId = null, replyToName = null, replyToContinent = null)
        }
        composable(
            route = WriteReplyRoutePattern,
            arguments = listOf(
                navArgument(WriteReplyUserIdArg) { type = NavType.StringType },
                navArgument(WriteReplyNameArg) { type = NavType.StringType },
                navArgument(WriteReplyContinentArg) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            WriteRouteContent(
                navController = navController,
                replyToUserId = backStackEntry.arguments?.getString(WriteReplyUserIdArg),
                replyToName = backStackEntry.arguments?.getString(WriteReplyNameArg),
                replyToContinent = backStackEntry.arguments?.getString(WriteReplyContinentArg),
            )
        }
        composable(
            route = WriteDraftRoutePattern,
            arguments = listOf(navArgument(WriteDraftIdArg) { type = NavType.StringType }),
        ) {
            WriteRouteContent(navController = navController, replyToUserId = null, replyToName = null, replyToContinent = null)
        }
        composable(
            route = WriteReplyDraftRoutePattern,
            arguments = listOf(
                navArgument(WriteReplyUserIdArg) { type = NavType.StringType },
                navArgument(WriteReplyNameArg) { type = NavType.StringType },
                navArgument(WriteReplyContinentArg) { type = NavType.StringType },
                navArgument(WriteDraftIdArg) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            WriteRouteContent(
                navController = navController,
                replyToUserId = backStackEntry.arguments?.getString(WriteReplyUserIdArg),
                replyToName = backStackEntry.arguments?.getString(WriteReplyNameArg),
                replyToContinent = backStackEntry.arguments?.getString(WriteReplyContinentArg),
            )
        }
        composable(Destinations.Stamps.route) {
            StampsScreen(
                onInviteFriendsClick = { navController.navigate(ProfileInviteFriendsRoute) },
            )
        }
        composable(Destinations.Profile.route) {
            ProfileScreen(
                onDraftClick = { navController.navigate(ProfileDraftRoute) },
                onAddressBookClick = { navController.navigate(ProfileAddressBookRoute) },
                onStampBookClick = { navController.navigate(ProfileStampBookRoute) },
                onInviteFriendsClick = { navController.navigate(ProfileInviteFriendsRoute) },
                onTipsClick = { navController.navigate(ProfileTipsRoute) },
                onSettingsClick = { navController.navigate(ProfileSettingsRoute) },
                onLoginClick = { navController.navigate(LoginRoute) },
            )
        }

        // ── Profile sub-screens ───────────────────────────────────────────────
        composable(ProfileDraftRoute) {
            val viewModel: ProfileDraftViewModel = hiltViewModel()
            val draftUiState by viewModel.uiState.collectAsState()
            ProfileDraftScreen(
                uiState = draftUiState,
                onBackClick = { navController.popBackStack() },
                onDraftClick = { draft -> navController.navigate(writeDraftRoute(draft.id)) },
                onDeleteDraft = viewModel::onDeleteDraft,
            )
        }
        composable(ProfileAddressBookRoute) {
            ProfileAddressBookScreen(onBackClick = { navController.popBackStack() })
        }
        composable(ProfileStampBookRoute) {
            ProfileStampBookScreen(
                onBackClick = { navController.popBackStack() },
                onExploreStoreClick = {
                    navController.navigate(Destinations.Stamps.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ProfileInviteFriendsRoute) {
            ProfileInviteFriendsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(ProfileTipsRoute) {
            ProfileTipsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(ProfileSettingsRoute) {
            // Activity-scoped, not the NavBackStackEntry-scoped default — must
            // be the same shared instance LoginScreen/MainActivity/PenPalsScreen
            // observe (see LoginScreen.kt's doc comment), otherwise signOut()/
            // deleteAccount() below update a throwaway instance's uiState and
            // LoginScreen's LaunchedEffect(uiState) never learns about it,
            // still sees its last-known Authenticated state, and immediately
            // bounces back to onNavigateToMain() instead of staying on login.
            val authViewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
            ProfileSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onBlockedUsersClick = { navController.navigate(ProfileBlockedUsersRoute) },
                onLogoutClick = {
                    authViewModel.signOut {
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onDeleteAccountClick = { onSuccess, onError ->
                    authViewModel.deleteAccount(
                        onSuccess = {
                            onSuccess()
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onError = onError,
                    )
                },
            )
        }

        composable(ProfileBlockedUsersRoute) {
            BlockedUsersScreen(onBackClick = { navController.popBackStack() })
        }

        composable(StoreRoute) {
            StampsScreen(
                onBackClick = { navController.popBackStack() },
                onInviteFriendsClick = { navController.navigate(ProfileInviteFriendsRoute) },
            )
        }

        composable(SelectRecipientRoute) {
            SelectRecipientScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToStore = { navController.navigate(StoreRoute) },
                onSendSuccess = { estimatedArrivalAt, recipientContinent, recipientName, letterId, visibility, compositeImageUrl ->
                    navController.navigate(
                        letterSentRoute(
                            estimatedArrivalAt, recipientContinent, recipientName, letterId, visibility, compositeImageUrl,
                        ),
                    )
                },
            )
        }
        composable(
            route = SelectRecipientReplyRoutePattern,
            arguments = listOf(
                navArgument(SelectRecipientUserIdArg) { type = NavType.StringType },
                navArgument(SelectRecipientNameArg) { type = NavType.StringType },
                navArgument(SelectRecipientContinentArg) { type = NavType.StringType },
            ),
        ) {
            SelectRecipientScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToStore = { navController.navigate(StoreRoute) },
                onSendSuccess = { estimatedArrivalAt, recipientContinent, recipientName, letterId, visibility, compositeImageUrl ->
                    navController.navigate(
                        letterSentRoute(
                            estimatedArrivalAt, recipientContinent, recipientName, letterId, visibility, compositeImageUrl,
                        ),
                    )
                },
            )
        }
        composable(
            route = LetterSentRoutePattern,
            arguments = listOf(
                navArgument(LetterSentEstimatedArrivalArg) { type = NavType.StringType },
                navArgument(LetterSentRecipientContinentArg) { type = NavType.StringType },
                navArgument(LetterSentRecipientNameArg) { type = NavType.StringType },
                navArgument(LetterSentLetterIdArg) { type = NavType.StringType },
                navArgument(LetterSentVisibilityArg) { type = NavType.StringType },
                navArgument(LetterSentCompositeImageUrlArg) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val estimatedArrivalAt = backStackEntry.arguments?.getString(LetterSentEstimatedArrivalArg).orEmpty()
            val recipientContinent = backStackEntry.arguments?.getString(LetterSentRecipientContinentArg).orEmpty()
            val recipientNameArg = backStackEntry.arguments?.getString(LetterSentRecipientNameArg).orEmpty()
            val recipientName = if (recipientNameArg == UnknownRecipientNameSentinel) "" else recipientNameArg
            val letterIdArg = backStackEntry.arguments?.getString(LetterSentLetterIdArg).orEmpty()
            val letterId = if (letterIdArg == UnknownLetterIdSentinel) "" else letterIdArg
            val visibility = backStackEntry.arguments?.getString(LetterSentVisibilityArg).orEmpty().ifBlank { "private" }
            val compositeImageUrlArg = backStackEntry.arguments?.getString(LetterSentCompositeImageUrlArg).orEmpty()
            val compositeImageUrl = if (compositeImageUrlArg == UnknownCompositeImageUrlSentinel) null else compositeImageUrlArg
            LetterSentScreen(
                estimatedArrivalAt = estimatedArrivalAt,
                recipientContinent = recipientContinent,
                recipientName = recipientName,
                letterId = letterId,
                isPublic = visibility == "public",
                compositeImageUrl = compositeImageUrl,
                onViewProfileClick = { navController.navigate(Destinations.Profile.route) },
                onMapClick = {
                    if (letterId.isNotBlank()) {
                        navController.navigate(deliveryTrackingRoute(letterId, recipientContinent))
                    }
                },
                onContinueClick = {
                    navController.navigate(Destinations.PenPals.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onWriteNewLetterClick = {
                    navController.navigate(Destinations.Write.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = MailboxThreadRoutePattern,
            arguments = listOf(
                navArgument(MailboxThreadIdArg) { type = NavType.StringType },
                navArgument(MailboxThreadNameArg) { type = NavType.StringType },
                navArgument(MailboxThreadContinentArg) { type = NavType.StringType },
            ),
        ) {
            MailboxThreadScreen(
                onBackClick = { navController.popBackStack() },
                onWriteLetterClick = { recipientId, recipientName, recipientContinent ->
                    navController.navigate(writeReplyRoute(recipientId, recipientName, recipientContinent))
                },
                onContinueDraftClick = { draftId, recipientId, recipientName, recipientContinent ->
                    navController.navigate(writeReplyDraftRoute(recipientId, recipientName, recipientContinent, draftId))
                },
                onTrackLetterClick = { letterId, otherPartyContinent ->
                    navController.navigate(deliveryTrackingRoute(letterId, otherPartyContinent))
                },
            )
        }
        composable(
            route = DeliveryTrackingRoutePattern,
            arguments = listOf(
                navArgument(DeliveryTrackingLetterIdArg) { type = NavType.StringType },
                navArgument(DeliveryTrackingOtherPartyContinentArg) { type = NavType.StringType },
            ),
        ) {
            val stampsEnergyTabRequester = rememberStampsEnergyTabRequester()
            DeliveryTrackingScreen(
                onBackClick = { navController.popBackStack() },
                onBuyEnergyClick = {
                    stampsEnergyTabRequester.request()
                    navController.navigate(Destinations.Stamps.route)
                },
            )
        }
    }
}

/** Shared body for [Destinations.Write]'s plain route and its reply/draft
 * variants ([WriteReplyRoutePattern]/[WriteDraftRoutePattern]) — only the Send
 * destination differs based on whether a recipient is already known. */
@Composable
private fun WriteRouteContent(
    navController: NavHostController,
    replyToUserId: String?,
    replyToName: String?,
    replyToContinent: String?,
) {
    // Guarded locally (not via the shared MainActivity guard) since only
    // the final Send action needs auth — drafting stays guest-friendly.
    val authViewModel: AuthViewModel = hiltViewModel()
    val authGuard = rememberAuthGuard(authViewModel)
    val context = LocalContext.current

    // Draft-on-Exit (docs/todo.md #5): system back press is interceptable
    // here directly since ComposeScreen is still composed when it fires —
    // unlike the bottom-nav tab-tap case, which needs WriteExitCoordinator's
    // cross-scope bridge (see MainActivity.kt). Both trigger sites share the
    // same coordinator/dialog, so no local dialog is mounted here.
    val writeExitGuard = rememberWriteExitGuard()
    val stampsEnergyTabRequester = rememberStampsEnergyTabRequester()
    BackHandler {
        writeExitGuard.requestExit { navController.popBackStack() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ComposeScreen(
            // Backs the sticky reply banner's tap target when there's no
            // original letter to open inline (StickyReplyBanner.kt) — this
            // is a peek, not a real exit (the user is coming right back via
            // "Continue Writing"), so it silently saves-and-goes instead of
            // interrupting with the Save/Discard dialog the system
            // BackHandler above shows.
            onBackClick = { writeExitGuard.saveAndExit { navController.popBackStack() } },
            onBuyEnergyClick = {
                stampsEnergyTabRequester.request()
                navController.navigate(Destinations.Stamps.route)
            },
            onNavigateToStore = {
                navController.navigate(StoreRoute)
            },
            onSendClick = {
                authGuard.guard {
                    navController.navigate(
                        if (replyToUserId != null && replyToName != null && replyToContinent != null) {
                            selectRecipientReplyRoute(replyToUserId, replyToName, replyToContinent)
                        } else {
                            SelectRecipientRoute
                        },
                    )
                }
            },
        )
        if (authGuard.isPromptVisible) {
            LoginPromptBottomSheet(
                onDismiss = authGuard.dismiss,
                onSignInClick = { navController.navigate(LoginRoute) },
            )
        }
    }
}
