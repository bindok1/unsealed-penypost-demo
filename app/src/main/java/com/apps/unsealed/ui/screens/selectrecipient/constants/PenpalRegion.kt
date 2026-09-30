package com.apps.unsealed.ui.screens.selectrecipient.constants

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.apps.unsealed.R

/**
 * World region catalog for the "Penpals around the world" flow.
 *
 * [apiLabel] is the exact string the backend expects in the `region` query
 * param for `GET /penpals/feed` and in `GET /penpals/regions` responses.
 *
 * [dummyPenpalCount] is the local fallback shown in [RegionPickerSheet] if
 * the live `/penpals/regions` call hasn't returned yet or failed.
 * Antarctica's tiny count is intentional flavor, not a placeholder bug.
 */
enum class PenpalRegion(
    @StringRes val labelRes: Int,
    val dummyPenpalCount: Int,
    val apiLabel: String,
    @DrawableRes val iconRes: Int,
    val emoji: String,
) {
    AFRICA(R.string.penpal_region_africa, 140, "Africa", R.drawable.ic_region_africa, "🌍"),
    ANTARCTICA(R.string.penpal_region_antarctica, 6, "Antarctica", R.drawable.ic_region_antarctica, "❄️"),
    ASIA(R.string.penpal_region_asia, 480, "Asia", R.drawable.ic_region_asia, "🌏"),
    EUROPE(R.string.penpal_region_europe, 260, "Europe", R.drawable.ic_region_europe, "🏰"),
    NORTH_AMERICA(R.string.penpal_region_north_america, 310, "North America", R.drawable.ic_region_north_america, "🌲"),
    OCEANIA(R.string.penpal_region_oceania, 75, "Oceania", R.drawable.ic_region_oceania, "🏝️"),
    SOUTH_AMERICA(R.string.penpal_region_south_america, 95, "South America", R.drawable.ic_region_south_america, "🦜");

    companion object {
        /** Maps the backend's `region` string (e.g. `"North America"`) to the enum.
         *  Falls back to [ASIA] for unknown values. */
        fun fromApiString(value: String): PenpalRegion =
            entries.firstOrNull { it.apiLabel == value } ?: ASIA
    }
}
