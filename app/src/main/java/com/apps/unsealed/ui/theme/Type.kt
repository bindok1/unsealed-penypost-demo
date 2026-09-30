package com.apps.unsealed.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R

/**
 * Nunito[wght].ttf is a variable font — each weight needs an explicit `wght` axis
 * setting via [FontVariation.Settings], otherwise every declared weight renders
 * using the font's default instance (regular) regardless of the [FontWeight] tag.
 */
@OptIn(ExperimentalTextApi::class)
val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_variable, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.nunito_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val defaultTypography = Typography()

val UnsealedTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = NunitoFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = NunitoFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = NunitoFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = NunitoFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = NunitoFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = NunitoFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = NunitoFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = NunitoFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = NunitoFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = NunitoFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = NunitoFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = NunitoFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = NunitoFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = NunitoFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = NunitoFontFamily),
)

/** Caveat-Regular.ttf is a static instance (not variable) — no `wght` axis to
 * configure, unlike [NunitoFontFamily] above. */
val CaveatFontFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
)

/** Kalam ships a real Bold face (unlike the other handwriting fonts below),
 * so it's registered here instead of relying on Compose's synthetic bold. */
val KalamFontFamily = FontFamily(
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold),
)

/** These four ship only a Regular face in Google Fonts — Bold/Italic are
 * synthesized by the platform when requested, which is expected. */
val ArchitectsDaughterFontFamily = FontFamily(
    Font(R.font.architects_daughter_regular, FontWeight.Normal),
)

val IndieFlowerFontFamily = FontFamily(
    Font(R.font.indie_flower_regular, FontWeight.Normal),
)

val NothingYouCouldDoFontFamily = FontFamily(
    Font(R.font.nothing_you_could_do_regular, FontWeight.Normal),
)

val ShadowsIntoLightFontFamily = FontFamily(
    Font(R.font.shadows_into_light_regular, FontWeight.Normal),
)

/** Letter body text style used by the Compose Screen's letter canvas. Kept
 * separate from [UnsealedTypography] since handwriting fonts need a custom
 * line height unrelated to Material's type scale. */
val LetterBodyStyle = TextStyle(
    fontFamily = CaveatFontFamily,
    fontSize = 15.sp,
    lineHeight = 28.sp,
)
