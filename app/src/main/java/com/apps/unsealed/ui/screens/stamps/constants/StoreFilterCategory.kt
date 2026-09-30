package com.apps.unsealed.ui.screens.stamps.constants

import androidx.annotation.StringRes
import com.apps.unsealed.R

/**
 * Filter categories shown as horizontal chips on the Peny Store tab.
 * [queryParam] maps directly to the `?category=` query param accepted by
 * `GET /api/v1/items` — `null` means no filter (show all items).
 * [labelRes] is the localised chip label displayed in [StoreFilterChips].
 */
enum class StoreFilterCategory(val queryParam: String?, @get:StringRes val labelRes: Int) {
    ALL(null, R.string.store_filter_all),
    STAMP("stamp", R.string.store_filter_prangko),
    PAPER("paper", R.string.store_filter_kertas),
    STICKER("sticker", R.string.store_filter_stiker),
    ENVELOPE("envelope", R.string.store_filter_amplop),
    BUNDLE("bundle", R.string.store_filter_bundle),
}
