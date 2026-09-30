package com.apps.unsealed

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.apps.unsealed.ui.components.AppUpdateDownloadedBanner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apps.unsealed.core.data.InstallReferrerPreferences
import com.apps.unsealed.core.data.PendingInviteCodeHolder
import com.apps.unsealed.core.util.TiltParallaxOverscan
import com.apps.unsealed.core.util.rememberTiltParallaxOffset
import com.apps.unsealed.core.util.softwareBlurredBitmap
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.invite.data.InstallReferrerReader
import com.apps.unsealed.navigation.AuthAndOnboardingRoutes
import com.apps.unsealed.navigation.Destinations
import com.apps.unsealed.navigation.DeliveryTrackingRoute
import com.apps.unsealed.navigation.HideBottomNavRoutes
import com.apps.unsealed.navigation.LetterSentRoute
import com.apps.unsealed.navigation.LoginRoute
import com.apps.unsealed.navigation.MailboxThreadRoute
import com.apps.unsealed.navigation.ProfileInviteFriendsRoute
import com.apps.unsealed.navigation.SelectRecipientRoute
import com.apps.unsealed.navigation.UnsealedNavHost
import com.apps.unsealed.core.update.AppUpdateUiState
import com.apps.unsealed.core.update.AppUpdateViewModel
import com.apps.unsealed.ui.components.AppUpdateDialog
import com.apps.unsealed.ui.components.LoginPromptBottomSheet
import com.apps.unsealed.ui.components.SaveDraftDialog
import com.apps.unsealed.ui.components.UnsealedBottomNavBar
import com.apps.unsealed.ui.components.rememberAuthGuard
import com.apps.unsealed.ui.components.rememberWriteExitGuard
import com.apps.unsealed.ui.screens.inbox.viewmodel.MailboxBadgeViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.UnsealedTheme
import com.chuckerteam.chucker.api.Chucker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Extracts `{code}` from an incoming `https://peny-dashboard-xi.vercel.app/invite/{code}`
 * App Link (`docs/be_updet/invite_friend_api.md` §5.1.B) — null for any other launch intent. */
private fun Uri.extractInviteCode(): String? =
    pathSegments.takeIf { it.size >= 2 && it[0] == "invite" }?.get(1)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var pendingInviteCodeHolder: PendingInviteCodeHolder
    @Inject lateinit var installReferrerReader: InstallReferrerReader
    @Inject lateinit var installReferrerPreferences: InstallReferrerPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val inviteCode = intent?.data?.extractInviteCode()
        if (inviteCode != null) {
            pendingInviteCodeHolder.set(inviteCode)
        }
        setContent {
            UnsealedTheme {
                UnsealedApp(hasPendingInvite = inviteCode != null)
            }
        }
        checkInstallReferrerOnce()
    }

    /** Queries the Play Install Referrer once per install (docs/be_updet/invite_friend_api.md
     * §5.1.A) — the resulting code (if any) just sits in [InstallReferrerPreferences] until
     * [AuthViewModel.register] finds a use for it post-signup, so this never blocks startup. */
    private fun checkInstallReferrerOnce() {
        lifecycleScope.launch {
            if (installReferrerPreferences.hasCheckedReferrer()) return@launch
            val code = installReferrerReader.readInviteCode()
            installReferrerPreferences.markReferrerChecked()
            if (code != null) {
                installReferrerPreferences.savePendingInviteCode(code)
            }
        }
    }
}

@Composable
private fun UnsealedApp(hasPendingInvite: Boolean = false) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Jump straight to the redeem screen when launched via an Invite Friends App Link —
    // InviteViewModel.consumePendingInviteCodeIfAny() fires the actual redeem call once
    // ProfileInviteFriendsScreen loads. Keyed on the (fixed-per-launch) boolean so this only
    // ever fires once, not on every recomposition.
    LaunchedEffect(hasPendingInvite) {
        if (hasPendingInvite) {
            navController.navigate(ProfileInviteFriendsRoute)
        }
    }

    val tiltOffset by rememberTiltParallaxOffset()
    val context = LocalContext.current

    // Shared across the whole tab set so guest taps on the Profile tab (and any
    // future guarded nav-bar destination) show one contextual login prompt
    // instead of each screen's own hiltViewModel() instance racing separately.
    val authViewModel: AuthViewModel = hiltViewModel()
    val authGuard = rememberAuthGuard(authViewModel)

    // App-scoped (same hiltViewModel()-at-this-level pattern as authViewModel above)
    // so the Mailbox tab's unread badge is populated as soon as the app opens,
    // not only after the user has visited the Mailbox tab at least once.
    val mailboxBadgeViewModel: MailboxBadgeViewModel = hiltViewModel()
    val mailboxUnreadCount by mailboxBadgeViewModel.unreadCount.collectAsState()

    val appUpdateViewModel: AppUpdateViewModel = hiltViewModel()
    val appUpdateState by appUpdateViewModel.updateState.collectAsState()
    val isUpdateDownloaded by appUpdateViewModel.isUpdateDownloaded.collectAsState()

    LifecycleResumeEffect(appUpdateViewModel) {
        (context as? Activity)?.let { activity ->
            appUpdateViewModel.onResumeCheck(activity)
        }
        onPauseOrDispose { }
    }

    // Draft-on-Exit (docs/todo.md #5) — bridges into ComposeViewModel's
    // unsaved-content state via the WriteExitCoordinator singleton (see its
    // doc comment); shares the same request/resolve flow as the BackHandler
    // in UnsealedNavHost's WriteRouteContent.
    val writeExitGuard = rememberWriteExitGuard()

    // NavBackStackEntry.destination.route resolves to the route *pattern*
    // (e.g. "mailbox_thread/{correspondentId}/{correspondentName}/{correspondentContinent}"),
    // not the filled-in value, so the thread detail screen is matched via
    // startsWith rather than equality.
    val isMailboxThreadRoute = currentRoute?.startsWith(MailboxThreadRoute) == true
    val isInboxRoute = currentRoute == Destinations.Inbox.route || isMailboxThreadRoute

    // Same startsWith rationale as isMailboxThreadRoute above — the reply variant
    // ("select_recipient/reply/{...}") extends this route's pattern rather
    // than matching the base "select_recipient" string in HideBottomNavRoutes.
    val isSelectRecipientRoute = currentRoute?.startsWith(SelectRecipientRoute) == true

    // Same startsWith rationale — the post-send confirmation route also
    // carries an arg (the estimated arrival timestamp).
    val isLetterSentRoute = currentRoute?.startsWith(LetterSentRoute) == true

    // Same startsWith rationale — carries letterId/continent args.
    val isDeliveryTrackingRoute = currentRoute?.startsWith(DeliveryTrackingRoute) == true

    // Same startsWith rationale — covers the plain "write" tab root as well
    // as its "write/reply/{...}"/"write/draft/{...}" variants.
    val isWriteRoute = currentRoute?.startsWith(Destinations.Write.route) == true

    val isStampsRoute = currentRoute == Destinations.Stamps.route

    // Auth & onboarding screens own their own backgrounds — bg_texture must NOT
    // be rendered behind them. Note: HideBottomNavRoutes must NOT be used here because
    // in-app sub-screens (e.g. SelectRecipient, Profile sub-screens) hide the bottom
    // nav bar but still require the in-app bg_texture background.
    // StampsScreen also manages its own full-screen background (shop_bg for STORE tab,
    // solid BrandInkDeep for the other tabs) — skip bg_texture there too.
    val isAuthOrOnboardingRoute = currentRoute == null ||
        currentRoute in AuthAndOnboardingRoutes ||
        currentRoute.startsWith("onboarding")

    // Pre-compute blurred bitmap once and cache it — doing this inside remember means
    // it's only decoded + scaled once per composition, not on every recomposition.
    val blurredBitmap = remember {
        context.softwareBlurredBitmap(R.drawable.device, scaleFactor = 0.01f)
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isInboxRoute) 0.25f else 0f,
        label = "inbox_overlay_alpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Full-screen background ──────────────────────────────────────────────
        // bg_texture is the in-app background (desk/wood aesthetic for PenPals,
        // Write, Stamps, Profile, etc.). It must NOT appear behind auth or
        // onboarding screens — those screens supply their own backgrounds.
        // LetterSentScreen paints its own full-screen gradient + bobbing-mascot
        // animation (docs: no Lottie, kept as a simple Compose infiniteTransition
        // instead) — the shared bg_texture must not show through behind it.
        if (!isAuthOrOnboardingRoute && !isLetterSentRoute) {
            if ((isInboxRoute || isStampsRoute) && blurredBitmap != null) {
                // Inbox & Stamps: pre-computed software-blurred bitmap — Inbox uses it
                // for its bokeh card backdrop; Stamps uses it so bg_texture is still
                // visible but won't compete with the STORE tab's shop_bg overlay.
                Image(
                    bitmap = blurredBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // All other in-app routes: bg_texture with tilt parallax
                val painter = androidx.compose.ui.res.painterResource(R.drawable.device)
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = TiltParallaxOverscan
                            scaleY = TiltParallaxOverscan
                            translationX = tiltOffset.x
                            translationY = tiltOffset.y
                        },
                )
            }
        }

        // Dark tint overlay — only visible on Inbox, just enough to keep card text
        // legible without crushing the bokeh blur underneath into flat black. A flat
        // Color (not a Brush.verticalGradient) is used deliberately here: the previous
        // gradient + graphicsLayer{alpha=...} combination rendered far darker than its
        // nominal alpha (e.g. targetValue=0.25f visually read as ~opaque black) — a flat
        // semi-transparent Color.background() composites at exactly its stated alpha.
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha)),
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Hide bottom nav on splash, login, register, onboarding, and stack-only screens.
                val hideNav = currentRoute == null ||
                    currentRoute in HideBottomNavRoutes ||
                    isSelectRecipientRoute ||
                    isMailboxThreadRoute ||
                    isLetterSentRoute ||
                    isDeliveryTrackingRoute
                if (!hideNav) {
                    UnsealedBottomNavBar(
                        currentRoute = currentRoute,
                        badgeCounts = mapOf(Destinations.Inbox to mailboxUnreadCount),
                    ) { destination ->
                        val navigate = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        val proceed = {
                            // Profile shows the guest's own account — guard it; other
                            // tabs (PenPals/Inbox/Write/Stamps) stay guest-browsable.
                            if (destination == Destinations.Profile) {
                                authGuard.guard(navigate)
                            } else {
                                navigate()
                            }
                        }
                        // Re-tapping the already-active Write tab isn't "leaving" —
                        // only guard an actual tab switch away from it.
                        if (isWriteRoute && destination != Destinations.Write) {
                            writeExitGuard.requestExit(proceed)
                        } else {
                            proceed()
                        }
                    }
                }
            },
        ) { innerPadding ->
            // .padding() alone only reserves visual space — it doesn't mark these
            // insets "consumed" for descendants. Without consumeWindowInsets, any
            // screen further down that reads WindowInsets.navigationBars/.ime via
            // .imePadding()/.navigationBarsPadding()/etc. sees the full raw value
            // again and double-counts the nav bar height already reserved here.
            UnsealedNavHost(
                navController = navController,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }

        if (authGuard.isPromptVisible) {
            LoginPromptBottomSheet(
                onDismiss = authGuard.dismiss,
                onSignInClick = { navController.navigate(LoginRoute) },
            )
        }

        // Mounted here (not inside WriteRouteContent) because this is the one
        // scope guaranteed to survive the bottom-nav tab-tap navigation that
        // can trigger it — see WriteExitCoordinator's doc comment.
        if (writeExitGuard.isDialogVisible) {
            SaveDraftDialog(
                onSave = writeExitGuard.confirmSave,
                onDiscard = writeExitGuard.confirmDiscard,
                onDismissRequest = writeExitGuard.cancel,
            )
        }

        (appUpdateState as? AppUpdateUiState.UpdateAvailable)?.let { updateState ->
            AppUpdateDialog(
                isMandatory = updateState.isMandatory,
                onUpdateClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        appUpdateViewModel.startUpdate(
                            activity = activity,
                            isMandatory = updateState.isMandatory,
                            fallbackUrl = updateState.storeUrl,
                        )
                    } else {
                        appUpdateViewModel.openStore(context, updateState.storeUrl)
                    }
                    if (!updateState.isMandatory) {
                        appUpdateViewModel.dismissSoftUpdate()
                    }
                },
                onDismissRequest = {
                    appUpdateViewModel.dismissSoftUpdate()
                },
            )
        }

        AppUpdateDownloadedBanner(
            visible = isUpdateDownloaded,
            onRestartClick = appUpdateViewModel::completeUpdate,
        )

        // ── Chucker Debug Floating Button (Debug builds only) ──────────────────
        if (BuildConfig.DEBUG) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                FloatingActionButton(
                    onClick = {
                        context.startActivity(Chucker.getLaunchIntent(context))
                    },
                    containerColor = BrandGold,
                    contentColor = BrandInk,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .zIndex(9999f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = "Open Chucker HTTP Inspector",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
