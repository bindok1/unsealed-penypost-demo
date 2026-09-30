package com.apps.unsealed.ui.screens.tracking.state

/**
 * Which renderer [com.apps.unsealed.ui.screens.tracking.widgets.DeliveryRouteMap]
 * uses, driven by the `map_render_mode` Firebase Remote Config key (see
 * [com.apps.unsealed.core.util.RemoteConfigKeys.MAP_RENDER_MODE]) so the app
 * can switch renderers with no release — e.g. reverting to the free
 * self-hosted [GEOJSON] renderer if Mapbox usage ever starts costing money.
 */
enum class MapRenderMode(val remoteValue: String) {
    /** Self-hosted, offline MapLibre renderer over a bundled world-countries GeoJSON. Free, default. */
    GEOJSON("geojson"),

    /** Mapbox Standard-style renderer (3D buildings, dynamic lighting) — free up to ~25k MAU/month. */
    MAPBOX("mapbox"),
    ;

    companion object {
        /** Unknown/blank remote values safely fall back to [GEOJSON]. */
        fun fromRemoteValue(value: String): MapRenderMode =
            entries.find { it.remoteValue == value } ?: GEOJSON
    }
}
