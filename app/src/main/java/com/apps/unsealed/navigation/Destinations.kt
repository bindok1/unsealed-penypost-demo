package com.apps.unsealed.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Markunread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.apps.unsealed.R

sealed class Destinations(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object PenPals : Destinations("penpals", R.string.nav_penpals, Icons.Filled.Public)
    data object Inbox : Destinations("inbox", R.string.nav_mailbox, Icons.Filled.Mail)
    data object Write : Destinations("write", R.string.nav_write, Icons.Filled.Create)
    data object Stamps : Destinations("stamps", R.string.nav_stamps, Icons.Filled.ColorLens)
    data object Profile : Destinations("profile", R.string.nav_profile, Icons.Filled.Person)

    companion object {
        // `by lazy` is required here: accessing Destinations.Inbox (or any sub-object) before
        // this list is first read would cause JVM to return a partially-initialised
        // Destinations$Inbox class (INSTANCE still null), producing a NullPointerException
        // inside BottomNavBar when iterating the list. `by lazy` defers list creation until
        // the first actual read, by which point all data-object INSTANCE fields are set.
        val entries: List<Destinations> by lazy { listOf(PenPals, Inbox, Write, Stamps, Profile) }
    }
}

// ── Auth stack ────────────────────────────────────────────────────────────────
// These are stack-only destinations (not bottom-nav tabs) — no icon/label needed.

internal const val SplashRoute = "splash"
internal const val WelcomeRoute = "welcome"
internal const val LoginRoute = "login"
internal const val RegisterRoute = "register"

// ── Onboarding wizard steps ───────────────────────────────────────────────────
internal const val OnboardingTosRoute            = "onboarding/tos"
internal const val OnboardingPermissionsRoute    = "onboarding/permissions"
internal const val OnboardingGenderBirthdayRoute = "onboarding/gender_birthday"
internal const val OnboardingLanguageBioRoute    = "onboarding/language_bio"
internal const val OnboardingPhotoRoute          = "onboarding/photo"
internal const val OnboardingInterestsRoute      = "onboarding/interests"
internal const val OnboardingNotifyHourRoute     = "onboarding/notify_hour"

/** All onboarding routes — used to hide the bottom nav bar on these screens. */
internal val OnboardingRoutes = listOf(
    OnboardingTosRoute,
    OnboardingPermissionsRoute,
    OnboardingGenderBirthdayRoute,
    OnboardingLanguageBioRoute,
    OnboardingPhotoRoute,
    OnboardingInterestsRoute,
    OnboardingNotifyHourRoute,
)

/** All auth & onboarding routes — used by MainActivity to omit the global bg_texture background. */
internal val AuthAndOnboardingRoutes: Set<String> = buildSet {
    add(SplashRoute)
    add(WelcomeRoute)
    add(LoginRoute)
    add(RegisterRoute)
    addAll(OnboardingRoutes)
}

