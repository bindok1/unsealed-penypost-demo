package com.apps.unsealed.core.util

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalGraphicsContext
import java.io.ByteArrayOutputStream

// NOTE: a fixed-target-resolution export pass (recording bigger than the
// on-screen size so low-density devices don't produce a soft composite) was
// attempted here and reverted — it captured blank output instead of scaled
// content and couldn't be safely debugged without an on-device test loop.
// Revisit separately, with real device/emulator verification per change
// rather than reasoning from decompiled Compose UI sources alone.

/** Below this [LocalDensity][androidx.compose.ui.platform.LocalDensity]
 * `.density` value, [captureIntoGraphicsLayer] produces composites too soft
 * (at worst, illegible noise) to send — confirmed on a real Android 8
 * mid-range device. Compose/Annotate/Insert-Image/Add-Sticker stay visible
 * but dimmed on devices below this threshold instead of disappearing (see
 * `DeviceCapabilityBottomSheet.kt`), and Send falls back to a plain-text
 * letter for them (see `ComposeViewModel.compositeAndSaveDraftForSend`).
 * `2.5f` is a
 * starting guess (between xhdpi/2.0 and xxhdpi/3.0); tune from real device
 * reports rather than more density buckets guessed in the abstract. */
const val MinCompositingDensity = 1.7f

/** No public `rememberGraphicsLayer()` composable exists in this Compose UI
 * version (androidx.compose.ui:ui-graphics 1.11.3) — built from the same
 * public pieces its internal equivalent (`GraphicsContextObserver`) uses:
 * [LocalGraphicsContext] + `GraphicsContext.createGraphicsLayer()`/
 * `releaseGraphicsLayer()`. */
@Composable
fun rememberGraphicsLayer(): GraphicsLayer {
    val graphicsContext = LocalGraphicsContext.current
    val graphicsLayer = remember { graphicsContext.createGraphicsLayer() }
    DisposableEffect(graphicsLayer) {
        onDispose { graphicsContext.releaseGraphicsLayer(graphicsLayer) }
    }
    return graphicsLayer
}

/**
 * Pins [graphicsLayer] to whatever this modifier is attached to, recording
 * every draw pass so [GraphicsLayer.toCompositeBytes] can flatten the subtree
 * (paper + styled text + inserted images + annotate strokes — everything
 * [LetterCanvas] renders) into a bitmap on demand. Shared by Send, "Simpan
 * ke Foto", and "Bagikan Surat" — one compositor, three callers — see
 * docs/be/letters_api.md's compositing rationale and docs/todo.md #2.
 */
fun Modifier.captureIntoGraphicsLayer(graphicsLayer: GraphicsLayer): Modifier =
    this.drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
    }

/** Flattens whatever [Modifier.captureIntoGraphicsLayer] last recorded into a
 * WebP — smaller than PNG for the mixed photo + line-art content a letter can
 * contain, at a quality high enough that text edges and thin annotate strokes
 * don't show visible lossy block artifacts. */
suspend fun GraphicsLayer.toCompositeBytes(): ByteArray {
    // On API 28+, GraphicsLayer.toImageBitmap() returns a Config.HARDWARE
    // bitmap (Bitmap.createBitmap(Picture) always does) — its pixels live in
    // GPU-only memory that Bitmap.compress()'s software encoder can't read,
    // silently producing a blank result instead of throwing. Copying to a
    // software config first (a no-op cost-wise on API <28, where the source
    // is already software-backed) avoids this regardless of which internal
    // GraphicsLayer snapshot path ends up used on a given device.
    val bitmap = toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    val stream = ByteArrayOutputStream()
    @Suppress("DEPRECATION") // WEBP_LOSSY/WEBP_LOSSLESS need API 30; minSdk here is 24.
    bitmap.compress(Bitmap.CompressFormat.WEBP, 92, stream)
    return stream.toByteArray()
}
