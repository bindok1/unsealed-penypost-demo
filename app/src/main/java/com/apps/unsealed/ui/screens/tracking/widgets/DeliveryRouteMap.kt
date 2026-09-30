package com.apps.unsealed.ui.screens.tracking.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.RemoteConfigKeys
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.tracking.state.MapRenderMode
import com.apps.unsealed.ui.screens.tracking.state.TransportMode
import com.apps.unsealed.ui.screens.tracking.state.WorldCountryLookup
import com.apps.unsealed.ui.screens.tracking.state.sampleGreatCircle
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.IceSkyAccent
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.PaperCream
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.JsonObject
import java.net.URI
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Real-world continent centroid (approximate). `internal` so
 * [com.apps.unsealed.ui.screens.tracking.widgets.MapboxDeliveryRouteMap] reads
 * the same source-of-truth coordinates as the GeoJSON renderer instead of
 * duplicating them.
 */
internal val ContinentCentroids: Map<PenpalRegion, LatLng> = mapOf(
    PenpalRegion.AFRICA to LatLng(1.5, 17.7),
    PenpalRegion.ANTARCTICA to LatLng(-82.0, 0.0),
    PenpalRegion.ASIA to LatLng(34.0, 100.6),
    PenpalRegion.EUROPE to LatLng(54.5, 15.2),
    PenpalRegion.NORTH_AMERICA to LatLng(45.7, -112.4),
    PenpalRegion.OCEANIA to LatLng(-25.0, 133.8), // Australia-centered
    PenpalRegion.SOUTH_AMERICA to LatLng(-8.8, -55.5),
)

/** Stable representative coordinate pairs for deliveries within the same continent
 * so intra-continent letters produce a real route trail instead of collapsing to a single point.
 * `internal` — shared with [MapboxDeliveryRouteMap], see [ContinentCentroids]. */
internal val IntraContinentRouteEndpoints: Map<PenpalRegion, Pair<LatLng, LatLng>> = mapOf(
    PenpalRegion.ASIA to (LatLng(-6.20, 106.81) to LatLng(35.68, 139.76)),       // SE Asia (Jakarta) ➔ East Asia (Tokyo)
    PenpalRegion.EUROPE to (LatLng(37.98, 23.72) to LatLng(59.33, 18.06)),       // S Europe (Athens) ➔ N Europe (Stockholm)
    PenpalRegion.NORTH_AMERICA to (LatLng(34.05, -118.24) to LatLng(40.71, -74.00)), // West Coast (LA) ➔ East Coast (NY)
    PenpalRegion.SOUTH_AMERICA to (LatLng(-33.45, -70.66) to LatLng(-3.73, -38.52)), // S America South ➔ North
    PenpalRegion.AFRICA to (LatLng(-33.92, 18.42) to LatLng(30.04, 31.23)),       // S Africa (Cape Town) ➔ N Africa (Cairo)
    PenpalRegion.OCEANIA to (LatLng(-37.81, 144.96) to LatLng(-17.71, 178.06)),   // Australia (Melbourne) ➔ Pacific (Fiji)
    PenpalRegion.ANTARCTICA to (LatLng(-77.85, 166.67) to LatLng(-64.77, -64.05)), // McMurdo ➔ Palmer
)

internal val DefaultLatLng = LatLng(0.0, 0.0)

private const val WorldSourceId = "unsealed-world-source"
private const val WorldFillLayerId = "unsealed-world-fill"
private const val WorldLineLayerId = "unsealed-world-line"
private const val RouteSourceId = "unsealed-route-source"
private const val RouteLayerId = "unsealed-route-line"
private const val StartPinSourceId = "unsealed-start-pin-source"
private const val StartPinLayerId = "unsealed-start-pin-layer"
private const val StartLabelLayerId = "unsealed-start-label-layer"
private const val EndPinSourceId = "unsealed-end-pin-source"
private const val EndPinLayerId = "unsealed-end-pin-layer"
private const val EndLabelLayerId = "unsealed-end-label-layer"
private const val MarkerSourceId = "unsealed-marker-source"
private const val MarkerLayerId = "unsealed-marker-layer"

/** Registered [Style.Builder.withImage] ids for the marker's data-driven icon */
private const val WalkIconId = "unsealed-icon-walk"
private const val SwimIconId = "unsealed-icon-swim"

/** GeoJSON feature property key the pin SymbolLayers read their text from */
private const val LabelPropertyKey = "label"
private const val MarkerIconPropertyKey = "icon"

// `internal` — shared with MapboxDeliveryRouteMap.kt for visual parity between renderers.
internal const val RouteSegments = 48
internal const val MaxPinRadius = 7f
internal const val MarkerIconDp = 40
internal const val MarkerIconSupersample = 3f

/**
 * Delivery route map for the letter-tracking screen — dispatches to whichever
 * renderer [MapRenderMode] resolves to, so callers never need to know which
 * map SDK is actually drawing.
 *
 * Renderer choice comes from the `map_render_mode` Firebase Remote Config key
 * ([RemoteConfigKeys.MAP_RENDER_MODE]), read once per composition (mirroring
 * the synchronous, no-StateFlow Remote Config pattern used elsewhere in the
 * app, e.g. `LegalLinks`). Defaults to [MapRenderMode.GEOJSON] — the free,
 * self-hosted renderer — until explicitly flipped to `"mapbox"` in the
 * console, and safety-nets back to it if [MapRenderMode.MAPBOX] is selected
 * but [BuildConfig.MAPBOX_ACCESS_TOKEN] isn't configured on this build, so a
 * missing token can never crash the tracking screen.
 */
@Composable
fun DeliveryRouteMap(
    senderContinent: PenpalRegion,
    recipientContinent: PenpalRegion,
    progress: Float,
    transportMode: TransportMode,
    animateEntrance: Boolean,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    recipientName: String? = null,
    letterId: String? = null,
    showPrivacyBadge: Boolean = true,
    onMapClick: (() -> Unit)? = null,
) {
    val renderMode = remember {
        val remoteMode = MapRenderMode.fromRemoteValue(
            Firebase.remoteConfig.getString(RemoteConfigKeys.MAP_RENDER_MODE)
        )
        if (remoteMode == MapRenderMode.MAPBOX && BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) {
            MapRenderMode.GEOJSON
        } else {
            remoteMode
        }
    }

    when (renderMode) {
        MapRenderMode.GEOJSON -> GeoJsonDeliveryRouteMap(
            senderContinent = senderContinent,
            recipientContinent = recipientContinent,
            progress = progress,
            transportMode = transportMode,
            animateEntrance = animateEntrance,
            modifier = modifier,
            senderName = senderName,
            recipientName = recipientName,
            letterId = letterId,
            showPrivacyBadge = showPrivacyBadge,
            onMapClick = onMapClick,
        )
        MapRenderMode.MAPBOX -> MapboxDeliveryRouteMap(
            senderContinent = senderContinent,
            recipientContinent = recipientContinent,
            progress = progress,
            transportMode = transportMode,
            animateEntrance = animateEntrance,
            modifier = modifier,
            senderName = senderName,
            recipientName = recipientName,
            letterId = letterId,
            showPrivacyBadge = showPrivacyBadge,
            onMapClick = onMapClick,
        )
    }
}

/**
 * The original, fully self-hosted MapLibre renderer — bundled
 * `world_countries.geojson` land polygons, no tile server, no API key. Free
 * and the default; see [DeliveryRouteMap] for the Remote-Config dispatch.
 */
@Composable
private fun GeoJsonDeliveryRouteMap(
    senderContinent: PenpalRegion,
    recipientContinent: PenpalRegion,
    progress: Float,
    transportMode: TransportMode,
    animateEntrance: Boolean,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    recipientName: String? = null,
    letterId: String? = null,
    showPrivacyBadge: Boolean = true,
    onMapClick: (() -> Unit)? = null,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val playEntrance = animateEntrance && !isReducedMotion
    val clampedProgress = progress.coerceIn(0f, 1f)
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val (startLatLng, endLatLng) = remember(senderContinent, recipientContinent) {
        if (senderContinent != recipientContinent) {
            val start = ContinentCentroids[senderContinent] ?: DefaultLatLng
            val end = ContinentCentroids[recipientContinent] ?: DefaultLatLng
            start to end
        } else {
            IntraContinentRouteEndpoints[senderContinent] ?: run {
                val start = ContinentCentroids[senderContinent] ?: DefaultLatLng
                val end = ContinentCentroids[recipientContinent] ?: DefaultLatLng
                start to end
            }
        }
    }

    val senderContinentLabel = stringResource(senderContinent.labelRes)
    val recipientContinentLabel = stringResource(recipientContinent.labelRes)
    val senderLabel = if (senderName.isNullOrBlank()) senderContinentLabel else "$senderName · $senderContinentLabel"
    val recipientLabel = if (recipientName.isNullOrBlank()) recipientContinentLabel else "$recipientName · $recipientContinentLabel"
    val markerColorArgb = when (transportMode) {
        TransportMode.WALK -> BrandGold
        TransportMode.SWIM -> IceSkyAccent
    }.toArgb()

    val routeReveal = remember { Animatable(if (playEntrance) 0f else 1f) }
    val pinsScale = remember { Animatable(if (playEntrance) 0f else 1f) }
    val markerScale = remember { Animatable(if (playEntrance) 0f else 1f) }

    LaunchedEffect(playEntrance) {
        if (!playEntrance) return@LaunchedEffect
        launch { routeReveal.animateTo(1f, tween(700)) }
        pinsScale.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
        markerScale.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
    }

    val idleTransition = rememberInfiniteTransition(label = "deliveryMarkerIdle")
    val idlePulse by idleTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isReducedMotion) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idlePulse",
    )

    val context = LocalContext.current
    val density = LocalDensity.current
    val iconPx = with(density) { (MarkerIconDp.dp * MarkerIconSupersample).roundToPx() }
    val mapView = rememberMapViewWithLifecycle()
    var style by remember { mutableStateOf<Style?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentCaption by remember { mutableStateOf("") }
    var markerScreenPos by remember { mutableStateOf<Offset?>(null) }
    // Some OEM builds hit native MapLibre asserts during style/image load (see
    // peniIconBitmap()'s density fix) — this is a last-resort net for any
    // *other* such device-specific native failure we haven't found yet, so
    // it degrades to a friendly card instead of taking down the whole app.
    var mapLoadFailed by remember { mutableStateOf(false) }

    val oceanCaptions = stringArrayResource(R.array.letter_tracking_ocean_captions)
    val passingThroughFormat = stringResource(R.string.letter_tracking_caption_passing_through)

    LaunchedEffect(mapView, onMapClick) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isLogoEnabled = false
            if (onMapClick != null) {
                map.addOnMapClickListener {
                    onMapClick()
                    true
                }
            }
            map.addOnCameraMoveListener {
                val currentMarker = sampleGreatCircle(startLatLng, endLatLng, clampedProgress, RouteSegments).last()
                val pt = map.projection.toScreenLocation(currentMarker)
                markerScreenPos = Offset(pt.x, pt.y)
            }
            map.addOnCameraIdleListener {
                val currentStyle = style ?: return@addOnCameraIdleListener
                updateMarkerPosition(context, map, currentStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
                    currentCaption = cap
                    markerScreenPos = Offset(pt.x, pt.y)
                }
            }
            fun loadMapStyle() {
                // MapLibre's native style/image loading has been observed to throw
                // java.lang.Error (not just Exception) on specific OEM builds — see
                // peniIconBitmap()'s doc comment for the one root cause we've found
                // and fixed. Catching Throwable here is a deliberate last line of
                // defense against *any* such native assert, known or not, so it
                // can't take down the whole app.
                try {
                    map.setStyle(buildStyle(context, markerColorArgb, iconPx)) { loadedStyle ->
                        style = loadedStyle
                        Log.d("DeliveryRouteMap", "setStyle loadedStyle=$loadedStyle")

                        val walkBmp = peniIconBitmap(context, R.drawable.peny_walk, iconPx)
                        val swimBmp = peniIconBitmap(context, R.drawable.peny_swim, iconPx)
                        if (walkBmp != null) loadedStyle.addImage(WalkIconId, walkBmp)
                        if (swimBmp != null) loadedStyle.addImage(SwimIconId, swimBmp)

                        updateRouteAndPins(loadedStyle, startLatLng, endLatLng, routeReveal.value, senderLabel, recipientLabel)
                        updateMarkerPosition(context, map, loadedStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
                            currentCaption = cap
                            markerScreenPos = Offset(pt.x, pt.y)
                        }

                        val isSamePoint = startLatLng.latitude == endLatLng.latitude && startLatLng.longitude == endLatLng.longitude
                        if (isSamePoint) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 2.5))
                        } else {
                            val bounds = LatLngBounds.Builder().include(startLatLng).include(endLatLng).build()
                            if (playEntrance) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 3.5))
                                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 900)
                            } else {
                                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("DeliveryRouteMap", "Failed to load map style, falling back", t)
                    mapLoadFailed = true
                }
            }

            // buildStyle() bakes marker icons into the Style.Builder via withOptionalImage();
            // MapLibre flushes those into the native style (Style.onDidFinishLoadingStyle ->
            // Style.addImage) as soon as setStyle() is called, deriving pixelRatio from the
            // MapView's live size. If this screen enters while the Activity is already
            // RESUMED, setStyle() can fire before Compose has laid out the AndroidView, so
            // width/height are still 0 and it crashes natively with "pixelRatio may not be
            // <= 0". Deferring until the view has a real layout avoids that race.
            if (mapView.width > 0 && mapView.height > 0) {
                loadMapStyle()
            } else {
                mapView.doOnLayout { loadMapStyle() }
            }
        }
    }

    LaunchedEffect(style, startLatLng, endLatLng, senderLabel, recipientLabel) {
        val loadedStyle = style ?: return@LaunchedEffect
        snapshotFlow { routeReveal.value }.collect { reveal ->
            updateRouteAndPins(loadedStyle, startLatLng, endLatLng, reveal, senderLabel, recipientLabel)
        }
    }

    LaunchedEffect(context, style, mapLibreMap, startLatLng, endLatLng, clampedProgress, transportMode, oceanCaptions, passingThroughFormat) {
        val loadedStyle = style ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect
        updateMarkerPosition(context, map, loadedStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
            currentCaption = cap
            markerScreenPos = Offset(pt.x, pt.y)
        }
    }

    LaunchedEffect(style) {
        val loadedStyle = style ?: return@LaunchedEffect
        snapshotFlow { Triple(pinsScale.value, markerScale.value, idlePulse) }.collect { (pins, marker, pulse) ->
            (loadedStyle.getLayer(StartPinLayerId) as? CircleLayer)
                ?.setProperties(PropertyFactory.circleRadius(MaxPinRadius * pins))
            (loadedStyle.getLayer(EndPinLayerId) as? CircleLayer)
                ?.setProperties(PropertyFactory.circleRadius(MaxPinRadius * pins * 0.85f))
            (loadedStyle.getLayer(MarkerLayerId) as? SymbolLayer)
                ?.setProperties(PropertyFactory.iconSize((marker * pulse) / MarkerIconSupersample))
            (loadedStyle.getLayer(StartLabelLayerId) as? SymbolLayer)
                ?.setProperties(PropertyFactory.textOpacity(pins))
            (loadedStyle.getLayer(EndLabelLayerId) as? SymbolLayer)
                ?.setProperties(PropertyFactory.textOpacity(pins))
        }
    }

    Box(
        modifier = if (onMapClick != null) {
            modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onMapClick)
        } else {
            modifier.clip(RoundedCornerShape(20.dp))
        },
    ) {
        if (mapLoadFailed) {
            MapLoadFailedFallback(modifier = Modifier.fillMaxSize())
            return@Box
        }

        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        val pos = markerScreenPos
        if (pos != null && currentCaption.isNotBlank()) {
            PeniSpeechBubble(
                caption = currentCaption,
                screenPos = pos,
                scale = markerScale.value,
            )
        }

        if (showPrivacyBadge) {
            PrivacyRouteBadge(
                onClick = { showPrivacyDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }

        if (showPrivacyDialog) {
            com.apps.unsealed.ui.components.LetterlyCenterDialog(
                title = stringResource(R.string.letter_tracking_privacy_dialog_title),
                body = stringResource(R.string.letter_tracking_privacy_dialog_body),
                primaryCtaText = stringResource(R.string.letter_tracking_privacy_dialog_cta),
                onPrimaryClick = { showPrivacyDialog = false },
                onDismissRequest = { showPrivacyDialog = false },
            )
        }
    }
}

/**
 * Subtle and aesthetic pill chip reassuring users about location privacy.
 * `internal` — shared with [MapboxDeliveryRouteMap].
 */
@Composable
internal fun PrivacyRouteBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.90f),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_privacy_shield),
                contentDescription = stringResource(R.string.letter_tracking_privacy_shield_desc),
                tint = BrandGoldDeep,
                modifier = Modifier.size(13.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(5.dp))
            Text(
                text = stringResource(R.string.letter_tracking_privacy_badge),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = BrandInkDeep,
                fontFamily = com.apps.unsealed.ui.theme.NunitoFontFamily,
            )
        }
    }
}

/**
 * Shown instead of the map when native style/image loading throws — see
 * [GeoJsonDeliveryRouteMap]'s `loadMapStyle()` catch block (and
 * [MapboxDeliveryRouteMap]'s equivalent). Keeps the tracking screen usable
 * (letter status text still works) instead of a hard crash. `internal` —
 * shared between both renderers.
 */
@Composable
internal fun MapLoadFailedFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(PaperCream),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.letter_tracking_map_load_failed),
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            color = BrandInkDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

/**
 * Animated floating speech bubble anchored cleanly above Peni mascot.
 * `internal` — shared with [MapboxDeliveryRouteMap].
 */
@Composable
internal fun PeniSpeechBubble(
    caption: String,
    screenPos: Offset,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    if (caption.isBlank() || scale <= 0.05f) return

    val density = LocalDensity.current
    val tailGapPx = with(density) { 26.dp.toPx() }

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val x = (screenPos.x - placeable.width / 2f).roundToInt()
                    val y = (screenPos.y - placeable.height - tailGapPx).roundToInt()
                    placeable.placeRelative(x, y)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.55f)),
            ) {
                AnimatedContent(
                    targetState = caption,
                    transitionSpec = {
                        (slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it / 3 } +
                                scaleIn(initialScale = 0.85f, animationSpec = tween(350)) +
                                fadeIn(tween(250)))
                            .togetherWith(
                                slideOutVertically(tween(250)) { -it / 3 } +
                                        scaleOut(targetScale = 0.85f, animationSpec = tween(250)) +
                                        fadeOut(tween(200))
                            )
                    },
                    label = "peniBubbleAnim",
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = BrandInkDeep,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            // Downward pointing arrow tail
            Canvas(modifier = Modifier.size(8.dp, 4.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, color = Color.White.copy(alpha = 0.95f))
            }
        }
    }
}

private fun buildStyle(context: Context, markerColorArgb: Int, iconPx: Int): Style.Builder {
    val baseJson = """
    {
      "version": 8,
      "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
      "sources": {},
      "layers": []
    }
    """.trimIndent()
    val builder = Style.Builder().fromJson(baseJson)
        .withOptionalImage(WalkIconId, peniIconBitmap(context, R.drawable.peny_walk, iconPx))
        .withOptionalImage(SwimIconId, peniIconBitmap(context, R.drawable.peny_swim, iconPx))
    return builder.withLayer(
        BackgroundLayer("unsealed-ocean-bg").withProperties(
            PropertyFactory.backgroundColor(Color(0xFFD3EAF5).toArgb()),
        ),
    )
    .withSource(GeoJsonSource(WorldSourceId, URI("asset://world_countries.geojson")))
    .withLayer(
        FillLayer(WorldFillLayerId, WorldSourceId).withProperties(
            PropertyFactory.fillColor(Color(0xFFFFF8E7).toArgb()),
            PropertyFactory.fillOpacity(1f),
        ),
    )
    .withLayer(
        LineLayer(WorldLineLayerId, WorldSourceId).withProperties(
            PropertyFactory.lineColor(Color(0xFFE5CBAA).toArgb()),
            PropertyFactory.lineWidth(1f),
        ),
    )
    .withSource(GeoJsonSource(RouteSourceId, FeatureCollection.fromFeatures(emptyArray())))
    .withLayer(
        LineLayer(RouteLayerId, RouteSourceId).withProperties(
            PropertyFactory.lineColor(markerColorArgb),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineDasharray(arrayOf(2f, 2.2f)),
        ),
    )
    .withSource(GeoJsonSource(StartPinSourceId, FeatureCollection.fromFeatures(emptyArray())))
    .withLayer(
        CircleLayer(StartPinLayerId, StartPinSourceId).withProperties(
            PropertyFactory.circleColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeColor(BrandGoldDeep.toArgb()),
            PropertyFactory.circleStrokeWidth(2.5f),
            PropertyFactory.circleRadius(MaxPinRadius),
        ),
    )
    .withLayer(pinLabelLayer(StartLabelLayerId, StartPinSourceId))
    .withSource(GeoJsonSource(EndPinSourceId, FeatureCollection.fromFeatures(emptyArray())))
    .withLayer(
        CircleLayer(EndPinLayerId, EndPinSourceId).withProperties(
            PropertyFactory.circleColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeColor(BrandGoldDeep.toArgb()),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleOpacity(0.85f),
            PropertyFactory.circleRadius(MaxPinRadius * 0.85f),
        ),
    )
    .withLayer(pinLabelLayer(EndLabelLayerId, EndPinSourceId))
    .withSource(GeoJsonSource(MarkerSourceId, FeatureCollection.fromFeatures(emptyArray())))
    .withLayer(
        SymbolLayer(MarkerLayerId, MarkerSourceId).withProperties(
            PropertyFactory.iconImage(Expression.get(MarkerIconPropertyKey)),
            PropertyFactory.iconSize(1f / MarkerIconSupersample),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
        ),
    )
}

/** `internal` — shared with [MapboxDeliveryRouteMap] for the same density-fix reasoning below. */
internal fun peniIconBitmap(context: Context, drawableRes: Int, iconPx: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return null
    return runCatching {
        // Bitmap.createBitmap() (used internally by Drawable.toBitmap()) leaves
        // density at whatever the platform's static default resolves to — on
        // some OEM builds (observed on a MediaTek device) that default is 0.
        // MapLibre's NativeMapView.addImages() derives pixelRatio as
        // bitmap.getDensity() / DisplayMetrics.DENSITY_DEFAULT, so a 0 density
        // truncates to a 0 pixelRatio and crashes natively with "pixelRatio
        // may not be <= 0". Stamping the device's real density here guarantees
        // it's never 0.
        drawable.toBitmap(iconPx, iconPx).also {
            it.density = context.resources.displayMetrics.densityDpi
        }
    }.getOrNull()
}

private fun Style.Builder.withOptionalImage(id: String, bitmap: Bitmap?): Style.Builder =
    if (bitmap != null) withImage(id, bitmap) else this

private fun pinLabelLayer(layerId: String, sourceId: String): SymbolLayer =
    SymbolLayer(layerId, sourceId).withProperties(
        PropertyFactory.textField(Expression.get(LabelPropertyKey)),
        PropertyFactory.textSize(12f),
        PropertyFactory.textColor(BrandInkDeep.toArgb()),
        PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
        PropertyFactory.textHaloWidth(1.5f),
        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
        PropertyFactory.textOffset(arrayOf(0f, -1.4f)),
        PropertyFactory.textAllowOverlap(true),
        PropertyFactory.textIgnorePlacement(true),
        PropertyFactory.textOpacity(1f),
    )

private fun updateRouteAndPins(
    style: Style,
    start: LatLng,
    end: LatLng,
    routeRevealFraction: Float,
    senderLabel: String,
    recipientLabel: String,
) {
    val revealedPoints = sampleGreatCircle(start, end, routeRevealFraction, RouteSegments)
    val lineString = LineString.fromLngLats(revealedPoints.map { Point.fromLngLat(it.longitude, it.latitude) })
    (style.getSource(RouteSourceId) as? GeoJsonSource)
        ?.setGeoJson(FeatureCollection.fromFeature(Feature.fromGeometry(lineString)))

    val startFeature = labeledPoint(start, senderLabel)
    val endFeature = labeledPoint(end, recipientLabel)
    (style.getSource(StartPinSourceId) as? GeoJsonSource)?.setGeoJson(startFeature)
    (style.getSource(EndPinSourceId) as? GeoJsonSource)?.setGeoJson(endFeature)
}

private fun labeledPoint(latLng: LatLng, label: String): Feature {
    val properties = JsonObject().apply { addProperty(LabelPropertyKey, label) }
    return Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude), properties)
}

private fun updateMarkerPosition(
    context: Context,
    map: MapLibreMap,
    style: Style,
    start: LatLng,
    end: LatLng,
    progress: Float,
    oceanCaptions: Array<String>,
    passingThroughFormat: String,
    onResult: (String, PointF) -> Unit,
) {
    val markerLatLng = sampleGreatCircle(start, end, progress, RouteSegments).last()
    val countryName = WorldCountryLookup.findCountry(markerLatLng.latitude, markerLatLng.longitude, context)

    // Icon is a pure land/sea hit-test against the rendered map (per
    // docs/message-tracking.md) — NOT gated on the overall journey's
    // TransportMode. Some same-continent routes (e.g. Melbourne → Fiji,
    // McMurdo → Palmer) still cross open water mid-route even though
    // transportModeFor() classifies the whole trip as WALK, so Peni must
    // still show swimming whenever the current point has no country under it.
    val icon: String
    val caption: String
    if (!countryName.isNullOrBlank()) {
        icon = WalkIconId
        caption = passingThroughFormat.format(countryName)
    } else {
        icon = SwimIconId
        caption = oceanCaptions.randomOrNull().orEmpty()
    }

    val properties = JsonObject().apply {
        addProperty(MarkerIconPropertyKey, icon)
    }
    val feature = Feature.fromGeometry(Point.fromLngLat(markerLatLng.longitude, markerLatLng.latitude), properties)
    (style.getSource(MarkerSourceId) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeature(feature))
    (style.getLayer(MarkerLayerId) as? SymbolLayer)?.setProperties(PropertyFactory.iconImage(icon))

    val screenPoint: PointF = map.projection.toScreenLocation(markerLatLng)
    onResult(caption, screenPoint)
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycle = lifecycleOwner.lifecycle
        var currentLifecycleState = Lifecycle.State.INITIALIZED

        fun moveTo(target: Lifecycle.State) {
            if (currentLifecycleState < Lifecycle.State.CREATED && target >= Lifecycle.State.CREATED) {
                mapView.onCreate(null)
                currentLifecycleState = Lifecycle.State.CREATED
            }
            if (currentLifecycleState < Lifecycle.State.STARTED && target >= Lifecycle.State.STARTED) {
                mapView.onStart()
                currentLifecycleState = Lifecycle.State.STARTED
            }
            if (currentLifecycleState < Lifecycle.State.RESUMED && target >= Lifecycle.State.RESUMED) {
                mapView.onResume()
                currentLifecycleState = Lifecycle.State.RESUMED
            }
        }

        moveTo(lifecycle.currentState)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> moveTo(Lifecycle.State.CREATED)
                Lifecycle.Event.ON_START -> moveTo(Lifecycle.State.STARTED)
                Lifecycle.Event.ON_RESUME -> moveTo(Lifecycle.State.RESUMED)
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    currentLifecycleState = Lifecycle.State.STARTED
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    currentLifecycleState = Lifecycle.State.CREATED
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                    currentLifecycleState = Lifecycle.State.DESTROYED
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (currentLifecycleState >= Lifecycle.State.RESUMED) {
                mapView.onPause()
            }
            if (currentLifecycleState >= Lifecycle.State.STARTED) {
                mapView.onStop()
            }
            mapView.onDestroy()
        }
    }

    return mapView
}
