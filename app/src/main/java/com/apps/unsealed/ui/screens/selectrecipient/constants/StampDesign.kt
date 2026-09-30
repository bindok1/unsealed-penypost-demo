package com.apps.unsealed.ui.screens.selectrecipient.constants

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.apps.unsealed.R

/** No prangko/stamp image assets exist anywhere in this project. Vector-drawn
 * swatches stand in instead, same precedent as `AnnotateCanvas.kt`'s
 * `drawHeartStamp` (a procedural shape substituting for a missing PNG).
 * Also backs [localStampCatalog]'s bundled fallback ids/names/colors until
 * the CMS-driven catalog (`feature/catalog/data/`) is reachable. */
enum class StampDesign(val accentColor: Color, @StringRes val labelRes: Int) {
    ROSE(Color(0xFFE07A9A), R.string.stamp_design_rose),
    OCEAN(Color(0xFF4A90A4), R.string.stamp_design_ocean),
    MARIGOLD(Color(0xFFE8A33D), R.string.stamp_design_marigold),
    SAGE(Color(0xFF7C9A6E), R.string.stamp_design_sage),
    PLUM(Color(0xFF8B5D8F), R.string.stamp_design_plum),
    INK(Color(0xFF3A3A3A), R.string.stamp_design_ink),
    ;

    companion object {
        /** [value] is a real backend/CMS stamp id (e.g. `"STAMP_ENCELIOPSIS_NUDICAULIS"`)
         * more often than one of these legacy enum names — `valueOf(value)`
         * throws for any id that isn't a legacy match, which used to crash
         * mailbox loading outright. Same `fromApiString`/`firstOrNull`
         * fallback precedent as [EnvelopeDesign.fromApiString]. */
        fun fromApiString(value: String): StampDesign =
            entries.firstOrNull { it.name == value } ?: INK
    }
}
