package com.apps.unsealed.ui.screens.tracking.state

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.maplibre.android.geometry.LatLng

/**
 * Point [fraction] of the way along the great-circle (geodesic) arc between
 * [start] and [end] — the actual reason flight paths look curved on a flat
 * map, computed for real instead of faked with a Bezier bow. Standard
 * spherical-interpolation formula (haversine central angle, then the
 * intermediate-point formula from the Aviation Formulary).
 */
fun greatCircleInterpolate(start: LatLng, end: LatLng, fraction: Float): LatLng {
    val lat1 = Math.toRadians(start.latitude)
    val lon1 = Math.toRadians(start.longitude)
    val lat2 = Math.toRadians(end.latitude)
    val lon2 = Math.toRadians(end.longitude)

    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val haversine = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val angularDistance = 2 * asin(sqrt(haversine).coerceIn(-1.0, 1.0))

    if (angularDistance == 0.0) return start

    val f = fraction.toDouble().coerceIn(0.0, 1.0)
    val a = sin((1 - f) * angularDistance) / sin(angularDistance)
    val b = sin(f * angularDistance) / sin(angularDistance)
    val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
    val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
    val z = a * sin(lat1) + b * sin(lat2)

    val lat = atan2(z, sqrt(x * x + y * y))
    val lon = atan2(y, x)
    return LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
}

/** Samples the great-circle arc from [start] to [end] at [segments] evenly
 * spaced points from 0 up to [upToFraction] (inclusive) — the point list a
 * `LineString` needs to render a smooth curve, not just two endpoints. */
fun sampleGreatCircle(start: LatLng, end: LatLng, upToFraction: Float, segments: Int = 48): List<LatLng> {
    val clamped = upToFraction.coerceIn(0f, 1f)
    if (clamped <= 0f) return listOf(start)
    val pointCount = (segments * clamped).toInt().coerceAtLeast(1)
    val points = (0..pointCount).map { i ->
        greatCircleInterpolate(start, end, clamped * i / pointCount)
    }
    return points
}
