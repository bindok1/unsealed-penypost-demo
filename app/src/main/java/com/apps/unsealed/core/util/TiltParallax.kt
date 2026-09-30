package com.apps.unsealed.core.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

private const val LowPassAlpha = 0.08f
private const val FullTiltAccel = 3f
private const val MaxOffsetPx = 26f

/**
 * Subtle device-tilt parallax for a background image — accelerometer x/y,
 * low-pass filtered so it drifts smoothly instead of jittering, then clamped
 * to a small pixel range so it reads as "the paper shifts slightly as you
 * move the phone" rather than an obvious gyroscope demo. Caller is expected
 * to scale its content up a little (see [TiltParallaxOverscan]) so the
 * translation never reveals an edge.
 *
 * Returns [Offset.Zero] and does nothing when reduced-motion is on or the
 * device has no accelerometer — same static background as before either way.
 */
@Composable
fun rememberTiltParallaxOffset(): State<Offset> {
    val context = LocalContext.current
    val isReducedMotion = rememberIsReducedMotion()
    val offset = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context, isReducedMotion) {
        if (isReducedMotion) {
            offset.value = Offset.Zero
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        var smoothedX = 0f
        var smoothedY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                smoothedX += (event.values[0] - smoothedX) * LowPassAlpha
                smoothedY += (event.values[1] - smoothedY) * LowPassAlpha
                val normalizedX = (smoothedX / FullTiltAccel).coerceIn(-1f, 1f)
                val normalizedY = (smoothedY / FullTiltAccel).coerceIn(-1f, 1f)
                // Tilting right (positive x) should feel like the paper
                // slides the other way, mirroring a real parallax layer.
                offset.value = Offset(x = -normalizedX * MaxOffsetPx, y = normalizedY * MaxOffsetPx)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    return offset
}

/** Scale factor content driven by [rememberTiltParallaxOffset] needs so the
 * max translation offset never uncovers an edge underneath. */
const val TiltParallaxOverscan = 1.08f
