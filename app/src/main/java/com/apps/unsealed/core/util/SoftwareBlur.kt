package com.apps.unsealed.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.DrawableRes

/**
 * Decodes [resId] into a [Bitmap], then applies a cheap software blur by:
 * 1. Scaling the bitmap DOWN to [scaleFactor] × original size.
 * 2. Scaling it back UP to the original size with bilinear filtering enabled.
 *
 * The heavy downscale destroys high-frequency detail and the upscale
 * with bilinear filtering smooths what remains — this is the classic
 * "fast software blur" used on pre-API-31 Android where [android.graphics.RenderEffect]
 * and `Modifier.blur()` are unavailable.
 *
 * [scaleFactor] controls blur strength: smaller = blurrier (0.06 gives a strong bokeh
 * look; 0.1 is more subtle). Returns null if decoding fails.
 *
 * **Call inside `remember {}` to compute only once per composition.**
 */
fun Context.softwareBlurredBitmap(
    @DrawableRes resId: Int,
    scaleFactor: Float = 0.06f,
): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inSampleSize = 2  // pre-subsample to keep memory usage low
    }
    val source = BitmapFactory.decodeResource(resources, resId, options) ?: return null

    val smallW = (source.width * scaleFactor).toInt().coerceAtLeast(1)
    val smallH = (source.height * scaleFactor).toInt().coerceAtLeast(1)

    // Step 1: shrink
    val small = Bitmap.createScaledBitmap(source, smallW, smallH, true /* filter */)
    source.recycle()

    // Step 2: stretch back with filtering — produces the blur effect
    val result = Bitmap.createBitmap(small.width * (1f / scaleFactor).toInt(),
        small.height * (1f / scaleFactor).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint().apply { isFilterBitmap = true }
    canvas.drawBitmap(
        small,
        null,
        android.graphics.RectF(0f, 0f, result.width.toFloat(), result.height.toFloat()),
        paint,
    )
    small.recycle()
    return result
}
