package com.apps.unsealed.ui.screens.compose.constants

import android.content.Context
import com.apps.unsealed.ui.screens.compose.state.ComposeUiState
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.apps.unsealed.R
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Downloadable Google Fonts (compose-screen-spec.md's Font Picker "more fonts"
 * extension) — separate from the 6 bundled [LetterFont] entries, which ship
 * in the APK and need no network. These are fetched on demand, only when the
 * user explicitly taps a card in [FontPickerSheet]'s "More Fonts" section,
 * via Android's official Downloadable Fonts mechanism (cached system-wide by
 * Google Play Services — not a custom HTTP/storage stack).
 */
private const val GoogleFontsProviderAuthority = "com.google.android.gms.fonts"
private const val GoogleFontsProviderPackage = "com.google.android.gms"

val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = GoogleFontsProviderAuthority,
    providerPackage = GoogleFontsProviderPackage,
    certificates = R.array.com_google_android_gms_fonts_certs,
)

data class DownloadableFontEntry(val googleFontName: String, val label: String)

val DownloadableFontCatalog = listOf(
    DownloadableFontEntry("Patrick Hand", "Patrick Hand"),
    DownloadableFontEntry("Dancing Script", "Dancing Script"),
    DownloadableFontEntry("Reenie Beanie", "Reenie Beanie"),
    DownloadableFontEntry("Permanent Marker", "Permanent Marker"),
)

enum class FontDownloadStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED }

fun googleFontFamily(fontName: String): FontFamily =
    FontFamily(Font(GoogleFont(fontName), GoogleFontProvider))

/**
 * Explicitly triggers a download via the lower-level [FontsContractCompat]
 * API and suspends until Play Services reports success/failure. This is the
 * "does the user know when it's done" signal driving the picker's
 * spinner-to-checkmark transition — simply using [googleFontFamily] inside a
 * `Text` would also eventually swap the font in once resolved, but gives no
 * explicit completion callback to react to.
 */
suspend fun downloadGoogleFont(context: Context, fontName: String): Typeface =
    suspendCancellableCoroutine { continuation ->
        val request = FontRequest(
            GoogleFontsProviderAuthority,
            GoogleFontsProviderPackage,
            "name=$fontName&besteffort=true",
            R.array.com_google_android_gms_fonts_certs,
        )
        FontsContractCompat.requestFont(
            context,
            request,
            object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface) {
                    if (continuation.isActive) continuation.resume(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("Font fetch failed for \"$fontName\", reason=$reason"))
                    }
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

/** Resolves the currently active font family: a downloaded Google Font takes
 * priority over the bundled [ComposeUiState.selectedFont] when set. */
fun ComposeUiState.activeFontFamily(): FontFamily =
    selectedGoogleFontName?.let { googleFontFamily(it) } ?: selectedFont.fontFamily
