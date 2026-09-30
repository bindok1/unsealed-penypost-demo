package com.apps.unsealed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light — onboarding, register, profile (warm cream surface) ─────────────
// primary = BrandGold dipakai oleh: FilledButton, FilterChip selected,
// active step-dot, OutlinedTextField focus indicator.
private val LightColors = lightColorScheme(
    primary             = BrandGold,
    onPrimary           = BrandInk,
    primaryContainer    = SurfaceCardLight,
    onPrimaryContainer  = BrandGoldDeep,
    secondary           = BrandGoldDim,
    onSecondary         = White,
    secondaryContainer  = SurfaceCardLight,
    onSecondaryContainer = BrandGoldDeep,
    background          = SurfaceCream,
    onBackground        = BrandInk,
    surface             = SurfaceCardLight,
    onSurface           = BrandInk,
    surfaceVariant      = SurfaceCream,
    onSurfaceVariant    = BrandGoldDim,
    outline             = SurfaceBorderLight,
    outlineVariant      = SurfaceBorderLight.copy(alpha = 0.5f),
    error               = Color(0xFFB94040),
    onError             = White,
)

// ── Dark — dipakai otomatis kalau device dark mode aktif ────────────────
// LoginScreen dan layar canvas sudah pakai hardcoded warm-dark tokens sendiri
// (mereka tidak bergantung pada MaterialTheme.colorScheme), tapi layar lain
// yang muncul saat dark-mode sistem aktif akan pakai skema ini.
private val DarkColors = darkColorScheme(
    primary             = BrandGold,
    onPrimary           = BrandInk,
    primaryContainer    = BrandCardDark,
    onPrimaryContainer  = BrandGold,
    secondary           = BrandGoldDim,
    onSecondary         = BrandInk,
    secondaryContainer  = BrandCardDark,
    onSecondaryContainer = BrandGoldDim,
    background          = BrandInkDeep,
    onBackground        = SurfaceCream,
    surface             = BrandCardDark,
    onSurface           = SurfaceCream,
    surfaceVariant      = BrandInkMid,
    onSurfaceVariant    = BrandGoldDim,
    outline             = BrandCardStroke,
    outlineVariant      = BrandCardStroke.copy(alpha = 0.5f),
    error               = Color(0xFFFF6B6B),
    onError             = BrandInk,
)

@Composable
fun UnsealedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = UnsealedTypography,
        content = content,
    )
}
