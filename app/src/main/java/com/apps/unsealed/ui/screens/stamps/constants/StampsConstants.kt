package com.apps.unsealed.ui.screens.stamps.constants

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.selectrecipient.constants.StampDesign
import com.apps.unsealed.ui.theme.PaperTemplate

enum class ComingSoonStoreFeature {
    BUY_STAMPS,
    BUY_PAPER,
    SUPPORTER_CLUB,
}

// STORE is the first entry — this is the default landing tab when the screen
// opens. HADIAH and ENERGI are secondary tabs. Enum declaration order drives
// the tab selector rendering order in StampsTabSelector.
enum class StampsTab { STORE, HADIAH, ENERGI }

/**
 * Client-side mirror of RevenueCat's product id -> Energy grant, matching the
 * "current" Offering's penypost_energy_50/120/300 configured in the RC
 * dashboard (Play Console product IDs — see the Monetize > Products listing).
 * No webhook/backend top-up endpoint exists yet, so purchase completion
 * trusts this map directly instead of a server-confirmed balance — see
 * StampsViewModel.onEnergyPurchaseCompleted().
 */
val EnergyProductGrants: Map<String, Int> = mapOf(
    "penypost_energy_50" to 50,
    "penypost_energy_120" to 120,
    "penypost_energy_300" to 300,
)

data class StampPackEntry(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    val priceLabel: String,
    val energyPrice: Int,
    val stampDesign: StampDesign,
)

data class PaperPackEntry(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    val priceLabel: String,
    val energyPrice: Int,
    val paperTemplate: PaperTemplate,
)

data class SubscriptionPlanEntry(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    val priceLabel: String,
    val icon: ImageVector,
)

val dummyStampPacks: List<StampPackEntry> = listOf(
    StampPackEntry("stamp_pack_rose", R.string.stamps_screen_pack_starter_title, R.string.stamps_screen_pack_starter_subtitle, "50 ⚡", 50, StampDesign.ROSE),
    StampPackEntry("stamp_pack_ocean", R.string.stamps_screen_pack_ocean_title, R.string.stamps_screen_pack_ocean_subtitle, "50 ⚡", 50, StampDesign.OCEAN),
    StampPackEntry("stamp_pack_marigold", R.string.stamps_screen_pack_marigold_title, R.string.stamps_screen_pack_marigold_subtitle, "50 ⚡", 50, StampDesign.MARIGOLD),
    StampPackEntry("stamp_pack_sage", R.string.stamps_screen_pack_sage_title, R.string.stamps_screen_pack_sage_subtitle, "50 ⚡", 50, StampDesign.SAGE),
    StampPackEntry("stamp_pack_plum", R.string.stamps_screen_pack_plum_title, R.string.stamps_screen_pack_plum_subtitle, "50 ⚡", 50, StampDesign.PLUM),
    StampPackEntry("stamp_pack_ink", R.string.stamps_screen_pack_ink_title, R.string.stamps_screen_pack_ink_subtitle, "50 ⚡", 50, StampDesign.INK),
)

val dummyPaperPacks: List<PaperPackEntry> = listOf(
    PaperPackEntry("paper_pack_grid", R.string.stamps_screen_paper_grid_title, R.string.stamps_screen_paper_grid_subtitle, "40 ⚡", 40, PaperTemplate.GRID),
    PaperPackEntry("paper_pack_lined", R.string.stamps_screen_paper_lined_title, R.string.stamps_screen_paper_lined_subtitle, "40 ⚡", 40, PaperTemplate.LINED),
    PaperPackEntry("paper_pack_indigo", R.string.stamps_screen_paper_indigo_title, R.string.stamps_screen_paper_indigo_subtitle, "40 ⚡", 40, PaperTemplate.INDIGO),
    PaperPackEntry("paper_pack_aged_kraft", R.string.stamps_screen_paper_aged_kraft_title, R.string.stamps_screen_paper_aged_kraft_subtitle, "40 ⚡", 40, PaperTemplate.AGED_KRAFT),
    PaperPackEntry("paper_pack_soft_beige", R.string.stamps_screen_paper_soft_beige_title, R.string.stamps_screen_paper_soft_beige_subtitle, "40 ⚡", 40, PaperTemplate.SOFT_BEIGE),
    PaperPackEntry("paper_pack_dotted", R.string.stamps_screen_paper_dotted_title, R.string.stamps_screen_paper_dotted_subtitle, "40 ⚡", 40, PaperTemplate.DOTTED),
    PaperPackEntry("paper_pack_plain_white", R.string.stamps_screen_paper_plain_white_title, R.string.stamps_screen_paper_plain_white_subtitle, "40 ⚡", 40, PaperTemplate.PLAIN_WHITE),
    PaperPackEntry("paper_pack_blush_marble", R.string.stamps_screen_paper_blush_marble_title, R.string.stamps_screen_paper_blush_marble_subtitle, "40 ⚡", 40, PaperTemplate.BLUSH_MARBLE),
)

val dummySupporterTiers: List<SubscriptionPlanEntry> = listOf(
    SubscriptionPlanEntry("supporter_club_monthly", R.string.stamps_screen_supporter_club_title, R.string.stamps_screen_supporter_club_subtitle, "Rp 25.000/bulan", Icons.Filled.VolunteerActivism),
)
