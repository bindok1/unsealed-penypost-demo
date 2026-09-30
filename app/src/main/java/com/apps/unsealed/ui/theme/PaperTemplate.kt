package com.apps.unsealed.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.apps.unsealed.R

/**
 * Textured paper backgrounds (compose-screen-spec.md §7), selectable from
 * the same "Ganti Kertas" sheet as the flat [PaperColor] swatches. Assets
 * are 1240x1754px WEBP (A4 portrait ratio) in `res/drawable/`, rendered
 * full-bleed with `ContentScale.Crop` — see [drawableRes].
 */
enum class PaperTemplate(@DrawableRes val drawableRes: Int, @StringRes val labelRes: Int) {
    GRID(R.drawable.kertas_kraft, R.string.paper_template_grid),
    LINED(R.drawable.kertas_putih_garis, R.string.paper_template_lined),
    INDIGO(R.drawable.blue_paper_texture, R.string.paper_template_indigo),
    AGED_KRAFT(R.drawable.kertas_kraft_tua, R.string.paper_template_aged_kraft),
    SOFT_BEIGE(R.drawable.kertas_buram_halus, R.string.paper_template_soft_beige),
    DOTTED(R.drawable.kertas_six, R.string.paper_template_dotted),
    PLAIN_WHITE(R.drawable.kertas_seven, R.string.paper_template_plain_white),
    BLUSH_MARBLE(R.drawable.kertas_eight, R.string.paper_template_blush_marble),
}
