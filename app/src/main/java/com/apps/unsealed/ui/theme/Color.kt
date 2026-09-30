package com.apps.unsealed.ui.theme

import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

// ── Unsealed brand warm cozy palette ──────────────────────────────────────────
// Modern Warm Cozy Skeuomorphism — vibrant warm honey, soft espresso, milk cream, antarctic sky.

/** Warm honey gold — warna CTA utama, tombol primer, active dot, chip selected. Matches Peni's beak & feet. */
val BrandGold      = Color(0xFFE8984E)

/** Warm amber sepia — subtitle, hint, placeholder, label sekunder. */
val BrandGoldDim   = Color(0xFF9E6B43)

/** Deep chestnut — pressed state, divider, border subtle. */
val BrandGoldDeep  = Color(0xFF73441B)

/** Soft espresso ink — text onPrimary, text utama (substitusi hitam pekat/kaku). */
val BrandInk       = Color(0xFF2C221E)

/** Cozy dark espresso — dark-mode background, deep containers. */
val BrandInkDeep   = Color(0xFF1C1613)

/** Warm dark espresso — mid-tone dark bg (dark gradient stop, card bg). */
val BrandInkMid    = Color(0xFF281E19)

/** Subtle lift — lighter warm dark (gradient accent stop). */
val BrandInkAccent = Color(0xFF382B24)

/** Frosted card bg — layar gelap (login, overlay). */
val BrandCardDark  = Color(0xFF281F1A)

/** Card border sepia — stroke di atas surface gelap. */
val BrandCardStroke = Color(0xFF4D382B)

/** Cozy milk cream — light-mode background (clean, spacious, warm). */
val SurfaceCream     = Color(0xFFFFFDF9)

/** Soft warm parchment — kartu/panel di atas SurfaceCream. */
val SurfaceCardLight = Color(0xFFF6EFE3)

/** Gentle warm border — divider/border di atas surface terang. */
val SurfaceBorderLight = Color(0xFFE6DAC8)

// ── Antarctic & Kawaii Accent Swatches ────────────────────────────────────────

/** Soft Antarctic sky blue — accent for ice/polar theme. */
val IceSkyBlue      = Color(0xFFE5F3F9)

/** Vibrant Antarctic sky blue — active accent / badges. */
val IceSkyAccent    = Color(0xFF5CA3D0)

/** Calm, non-intrusive blue for store purchase CTA (avoids aggressive pushy-sale appearance). */
val BuyButtonBlue   = Color(0xFF2563EB)

// ── Letter paper swatches ─────────────────────────────────────────────────────

val PaperCream      = Color(0xFFFFF8E7)
val PaperStrawberry = Color(0xFFFCE4EC)
val PaperLavender   = Color(0xFFF3E5F5)
val PaperMint       = Color(0xFFE8F5E9)
val PaperSky        = Color(0xFFE1F5FE)

enum class PaperColor(val color: Color, val label: String) {
    CREAM(PaperCream, "Ivory"),
    STRAWBERRY(PaperStrawberry, "Strawberry"),
    LAVENDER(PaperLavender, "Lavender"),
    MINT(PaperMint, "Mint"),
    SKY(PaperSky, "Sky"),
}

// ── Annotate / compose canvas tokens ─────────────────────────────────────────

/** Accent for the ON-state Annotate icon pill. */
val LemonYellow = Color(0xFFF4D35E)

/** Frosted-glass toolbar background, exact value from the design spec. */
val ToolbarFrostedDark = Color(0xB3140C04)

/** Default ink/text color for typed letter body. */
val InkDefault = Color(0xFF1F2937)
