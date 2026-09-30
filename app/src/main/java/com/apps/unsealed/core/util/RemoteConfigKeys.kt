package com.apps.unsealed.core.util

import com.apps.unsealed.BuildConfig

/**
 * Firebase Remote Config keys used across the app, paired with their local
 * fallback defaults — the single source of truth for both
 * [com.apps.unsealed.UnsealedApplication]'s `setDefaultsAsync` call and every
 * reader ([RemoteConfigKeys] callers never need their own fallback string).
 *
 * Console values for these keys are configured separately in the Firebase
 * console — this file only declares the contract (key name + safe local
 * default), not the production value.
 */
object RemoteConfigKeys {

    /**
     * Overrides the API base URL's scheme/host/port per request — see
     * `core/network/RemoteConfigBaseUrlInterceptor.kt`. Path is untouched,
     * so this must still point at the same `/api/v1/...`-style host as
     * [BuildConfig.BASE_URL]. Lets the backend domain be swapped (incident
     * failover, migration) without an app release.
     */
    const val API_BASE_URL = "api_base_url"

    /** See [com.apps.unsealed.core.util.LegalLinks.TERMS_OF_SERVICE_URL]. */
    const val TERMS_OF_SERVICE_URL = "terms_of_service_url"

    /** See [com.apps.unsealed.core.util.LegalLinks.PRIVACY_POLICY_URL]. */
    const val PRIVACY_POLICY_URL = "privacy_policy_url"

    /**
     * Picks which renderer `DeliveryRouteMap` uses — see
     * [com.apps.unsealed.ui.screens.tracking.state.MapRenderMode]. Defaults to
     * the free, self-hosted GeoJSON/MapLibre renderer; flip to `"mapbox"` in
     * the Firebase console to switch to the Mapbox Standard-style renderer
     * (paid past ~25k MAU/month), and flip back any time with no app release.
     */
    const val MAP_RENDER_MODE = "map_render_mode"

    /**
     * Minimum versionCode required to run the app. If [BuildConfig.VERSION_CODE]
     * is lower than this value, a non-dismissible force update dialog is shown.
     */
    const val MIN_REQUIRED_VERSION_CODE = "min_required_version_code"

    /**
     * Latest versionCode available in the store. If [BuildConfig.VERSION_CODE]
     * is lower than this but >= [MIN_REQUIRED_VERSION_CODE], an optional update
     * dialog with a "Later" option is shown.
     */
    const val LATEST_VERSION_CODE = "latest_version_code"

    /**
     * Play Store or website URL opened when user accepts the update prompt.
     */
    const val UPDATE_PLAY_STORE_URL = "update_play_store_url"

    /**
     * Local fallbacks, seeded into Remote Config at startup
     * ([com.apps.unsealed.UnsealedApplication]) so `getString(key)` is never
     * blank — before the first successful fetch, offline, or if a key is
     * left unset in the console.
     */
    fun defaults(): Map<String, String> = mapOf(
        API_BASE_URL to BuildConfig.BASE_URL,
        TERMS_OF_SERVICE_URL to "https://peny-dashboard-xi.vercel.app/terms",
        PRIVACY_POLICY_URL to "https://peny-dashboard-xi.vercel.app/privacy",
        MAP_RENDER_MODE to com.apps.unsealed.ui.screens.tracking.state.MapRenderMode.GEOJSON.remoteValue,
        MIN_REQUIRED_VERSION_CODE to BuildConfig.VERSION_CODE.toString(),
        LATEST_VERSION_CODE to BuildConfig.VERSION_CODE.toString(),
        UPDATE_PLAY_STORE_URL to "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
    )
}
