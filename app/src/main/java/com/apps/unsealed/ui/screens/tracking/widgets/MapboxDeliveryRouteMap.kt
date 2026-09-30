package com.apps.unsealed.ui.screens.tracking.widgets

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.tracking.state.TransportMode
import com.apps.unsealed.ui.screens.tracking.state.WorldCountryLookup
import com.apps.unsealed.ui.screens.tracking.state.sampleGreatCircle
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.IceSkyAccent
import com.google.gson.JsonObject
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

/** Mapbox-specific source/layer ids — kept distinct from the GeoJSON renderer's own
 * (`unsealed-*`) ids so the two renderers' native state can never collide. */
private const val MbxRouteSourceId = "mapbox-unsealed-route-source"
private const val MbxRouteLayerId = "mapbox-unsealed-route-line"
private const val MbxStartPinSourceId = "mapbox-unsealed-start-pin-source"
private const val MbxStartPinLayerId = "mapbox-unsealed-start-pin-layer"
private const val MbxStartLabelLayerId = "mapbox-unsealed-start-label-layer"
private const val MbxEndPinSourceId = "mapbox-unsealed-end-pin-source"
private const val MbxEndPinLayerId = "mapbox-unsealed-end-pin-layer"
private const val MbxEndLabelLayerId = "mapbox-unsealed-end-label-layer"
private const val MbxMarkerSourceId = "mapbox-unsealed-marker-source"
private const val MbxMarkerLayerId = "mapbox-unsealed-marker-layer"
private const val MbxWalkIconId = "mapbox-unsealed-icon-walk"
private const val MbxSwimIconId = "mapbox-unsealed-icon-swim"
private const val MbxLabelPropertyKey = "label"
private const val MbxMarkerIconPropertyKey = "icon"

/**
 * Mapbox Standard-style renderer for the same delivery route map — richer 3D
 * buildings/terrain/dynamic lighting basemap, at the cost of a real network
 * request per tile and Mapbox's mandatory attribution/logo (their Terms of
 * Service require these stay visible; unlike the GeoJSON renderer they can't
 * be hidden). Selected via Remote Config — see [DeliveryRouteMap].
 *
 * Structurally mirrors [GeoJsonDeliveryRouteMap]: same entrance/idle
 * animations, same [sampleGreatCircle]/[WorldCountryLookup] route logic, same
 * [PeniSpeechBubble]/[PrivacyRouteBadge]/[MapLoadFailedFallback] overlays —
 * only the map SDK calls differ. [ContinentCentroids] / [IntraContinentRouteEndpoints]
 * are shared with the GeoJSON renderer (not duplicated) so both stay in sync.
 */
@Composable
internal fun MapboxDeliveryRouteMap(
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
    // Defensive fallback only — DeliveryRouteMap's dispatcher already never
    // routes here without a configured token, but this must never crash if
    // that guarantee is somehow bypassed (e.g. this composable called directly).
    if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) {
        Log.e("MapboxDeliveryRouteMap", "Reached with a blank MAPBOX_ACCESS_TOKEN — showing fallback.")
        MapLoadFailedFallback(modifier = modifier.clip(RoundedCornerShape(20.dp)))
        return
    }
    remember { MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN }

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

    val idleTransition = rememberInfiniteTransition(label = "deliveryMarkerIdleMapbox")
    val idlePulse by idleTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isReducedMotion) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idlePulseMapbox",
    )

    val context = LocalContext.current
    val density = LocalDensity.current
    val iconPx = with(density) { (MarkerIconDp.dp * MarkerIconSupersample).roundToPx() }
    val mapView = rememberMapboxMapViewWithLifecycle()
    var style by remember { mutableStateOf<Style?>(null) }
    var currentCaption by remember { mutableStateOf("") }
    var markerScreenPos by remember { mutableStateOf<Offset?>(null) }
    var mapLoadFailed by remember { mutableStateOf(false) }

    val oceanCaptions = stringArrayResource(R.array.letter_tracking_ocean_captions)
    val passingThroughFormat = stringResource(R.string.letter_tracking_caption_passing_through)

    LaunchedEffect(mapView, onMapClick) {
        val mapboxMap = mapView.mapboxMap

        if (onMapClick != null) {
            mapboxMap.addOnMapClickListener {
                onMapClick()
                true
            }
        }
        mapboxMap.addOnCameraChangeListener {
            val currentMarker = sampleGreatCircle(startLatLng, endLatLng, clampedProgress, RouteSegments).last()
            val pt = mapboxMap.pixelForCoordinate(currentMarker.toMapboxPoint())
            markerScreenPos = Offset(pt.x.toFloat(), pt.y.toFloat())
        }
        mapboxMap.addOnMapIdleListener {
            val currentStyle = style ?: return@addOnMapIdleListener
            updateMapboxMarkerPosition(context, mapboxMap, currentStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
                currentCaption = cap
                markerScreenPos = pt
            }
        }

        // Mirrors GeoJsonDeliveryRouteMap's defensive Throwable catch — Mapbox's
        // native style/image loading can fail on the same class of OEM devices.
        try {
            val walkBmp = peniIconBitmap(context, R.drawable.peny_walk, iconPx)
            val swimBmp = peniIconBitmap(context, R.drawable.peny_swim, iconPx)

            // Declarative style DSL (current Mapbox v11 API — loadStyleUri() is
            // deprecated for removal in the next major version). Only adds the
            // route/pin/marker sources+layers on top of the Standard basemap;
            // Standard already renders land/ocean/roads/buildings natively, so
            // (unlike the GeoJSON renderer's buildStyle()) no land layer is needed.
            mapboxMap.loadStyle(
                style(Style.STANDARD) {
                    +geoJsonSource(MbxRouteSourceId) { geometry(LineString.fromLngLats(emptyList())) }
                    +lineLayer(MbxRouteLayerId, MbxRouteSourceId) {
                        lineColor(markerColorArgb)
                        lineWidth(4.0)
                        lineCap(LineCap.ROUND)
                        lineDasharray(listOf(2.0, 2.2))
                    }
                    +geoJsonSource(MbxStartPinSourceId) { geometry(Point.fromLngLat(0.0, 0.0)) }
                    +circleLayer(MbxStartPinLayerId, MbxStartPinSourceId) {
                        circleColor(android.graphics.Color.WHITE)
                        circleStrokeColor(BrandGoldDeep.toArgb())
                        circleStrokeWidth(2.5)
                        circleRadius(MaxPinRadius.toDouble())
                    }
                    +mapboxPinLabelLayer(MbxStartLabelLayerId, MbxStartPinSourceId)
                    +geoJsonSource(MbxEndPinSourceId) { geometry(Point.fromLngLat(0.0, 0.0)) }
                    +circleLayer(MbxEndPinLayerId, MbxEndPinSourceId) {
                        circleColor(android.graphics.Color.WHITE)
                        circleStrokeColor(BrandGoldDeep.toArgb())
                        circleStrokeWidth(2.0)
                        circleOpacity(0.85)
                        circleRadius((MaxPinRadius * 0.85f).toDouble())
                    }
                    +mapboxPinLabelLayer(MbxEndLabelLayerId, MbxEndPinSourceId)
                    +geoJsonSource(MbxMarkerSourceId) { geometry(Point.fromLngLat(0.0, 0.0)) }
                    +symbolLayer(MbxMarkerLayerId, MbxMarkerSourceId) {
                        iconImage(Expression.get(MbxMarkerIconPropertyKey))
                        iconSize((1f / MarkerIconSupersample).toDouble())
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                    }
                    if (walkBmp != null) +image(MbxWalkIconId, walkBmp)
                    if (swimBmp != null) +image(MbxSwimIconId, swimBmp)
                },
            ) { loadedStyle ->
                style = loadedStyle

                updateMapboxRouteAndPins(loadedStyle, startLatLng, endLatLng, routeReveal.value, senderLabel, recipientLabel)
                updateMapboxMarkerPosition(context, mapboxMap, loadedStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
                    currentCaption = cap
                    markerScreenPos = pt
                }

                val isSamePoint = startLatLng.latitude == endLatLng.latitude && startLatLng.longitude == endLatLng.longitude
                if (isSamePoint) {
                    mapboxMap.setCamera(CameraOptions.Builder().center(startLatLng.toMapboxPoint()).zoom(2.5).build())
                } else {
                    mapboxMap.cameraForCoordinates(
                        coordinates = listOf(startLatLng.toMapboxPoint(), endLatLng.toMapboxPoint()),
                        camera = CameraOptions.Builder().build(),
                        coordinatesPadding = EdgeInsets(80.0, 80.0, 80.0, 80.0),
                        maxZoom = null,
                        offset = null,
                    ) { fittedCamera ->
                        if (playEntrance) {
                            mapboxMap.setCamera(CameraOptions.Builder().center(startLatLng.toMapboxPoint()).zoom(3.5).build())
                            mapboxMap.easeTo(fittedCamera, MapAnimationOptions.Builder().duration(900L).build())
                        } else {
                            mapboxMap.setCamera(fittedCamera)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("MapboxDeliveryRouteMap", "Failed to load Mapbox style, falling back", t)
            mapLoadFailed = true
        }
    }

    LaunchedEffect(style, startLatLng, endLatLng, senderLabel, recipientLabel) {
        val loadedStyle = style ?: return@LaunchedEffect
        snapshotFlow { routeReveal.value }.collect { reveal ->
            updateMapboxRouteAndPins(loadedStyle, startLatLng, endLatLng, reveal, senderLabel, recipientLabel)
        }
    }

    LaunchedEffect(context, style, mapView, startLatLng, endLatLng, clampedProgress, transportMode, oceanCaptions, passingThroughFormat) {
        val loadedStyle = style ?: return@LaunchedEffect
        updateMapboxMarkerPosition(context, mapView.mapboxMap, loadedStyle, startLatLng, endLatLng, clampedProgress, oceanCaptions, passingThroughFormat) { cap, pt ->
            currentCaption = cap
            markerScreenPos = pt
        }
    }

    LaunchedEffect(style) {
        val loadedStyle = style ?: return@LaunchedEffect
        snapshotFlow { Triple(pinsScale.value, markerScale.value, idlePulse) }.collect { (pins, marker, pulse) ->
            loadedStyle.getLayerAs<CircleLayer>(MbxStartPinLayerId)?.circleRadius((MaxPinRadius * pins).toDouble())
            loadedStyle.getLayerAs<CircleLayer>(MbxEndPinLayerId)?.circleRadius((MaxPinRadius * pins * 0.85f).toDouble())
            loadedStyle.getLayerAs<SymbolLayer>(MbxMarkerLayerId)?.iconSize(((marker * pulse) / MarkerIconSupersample).toDouble())
            loadedStyle.getLayerAs<SymbolLayer>(MbxStartLabelLayerId)?.textOpacity(pins.toDouble())
            loadedStyle.getLayerAs<SymbolLayer>(MbxEndLabelLayerId)?.textOpacity(pins.toDouble())
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
            // Mapbox's logo + attribution control are required by their Terms of
            // Service to stay visible at the default bottom-start corner — the
            // badge moves to the opposite corner here so the two never collide
            // (the GeoJSON renderer, with no such requirement, keeps it bottom-start).
            PrivacyRouteBadge(
                onClick = { showPrivacyDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
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

private fun LatLng.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

private fun mapboxPinLabelLayer(layerId: String, sourceId: String): SymbolLayer =
    symbolLayer(layerId, sourceId) {
        textField(Expression.get(MbxLabelPropertyKey))
        textSize(12.0)
        textColor(BrandInkDeep.toArgb())
        textHaloColor(android.graphics.Color.WHITE)
        textHaloWidth(1.5)
        textAnchor(TextAnchor.BOTTOM)
        textOffset(listOf(0.0, -1.4))
        textAllowOverlap(true)
        textIgnorePlacement(true)
        textOpacity(1.0)
    }

private fun updateMapboxRouteAndPins(
    style: Style,
    start: LatLng,
    end: LatLng,
    routeRevealFraction: Float,
    senderLabel: String,
    recipientLabel: String,
) {
    val revealedPoints = sampleGreatCircle(start, end, routeRevealFraction, RouteSegments)
    val lineString = LineString.fromLngLats(revealedPoints.map { it.toMapboxPoint() })
    style.getSourceAs<GeoJsonSource>(MbxRouteSourceId)?.geometry(lineString)

    style.getSourceAs<GeoJsonSource>(MbxStartPinSourceId)?.feature(mapboxLabeledFeature(start, senderLabel))
    style.getSourceAs<GeoJsonSource>(MbxEndPinSourceId)?.feature(mapboxLabeledFeature(end, recipientLabel))
}

private fun mapboxLabeledFeature(latLng: LatLng, label: String): Feature {
    val properties = JsonObject().apply { addProperty(MbxLabelPropertyKey, label) }
    return Feature.fromGeometry(latLng.toMapboxPoint(), properties)
}

private fun updateMapboxMarkerPosition(
    context: android.content.Context,
    mapboxMap: com.mapbox.maps.MapboxMap,
    style: Style,
    start: LatLng,
    end: LatLng,
    progress: Float,
    oceanCaptions: Array<String>,
    passingThroughFormat: String,
    onResult: (String, Offset) -> Unit,
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
        icon = MbxWalkIconId
        caption = passingThroughFormat.format(countryName)
    } else {
        icon = MbxSwimIconId
        caption = oceanCaptions.randomOrNull().orEmpty()
    }

    val properties = JsonObject().apply { addProperty(MbxMarkerIconPropertyKey, icon) }
    val feature = Feature.fromGeometry(markerLatLng.toMapboxPoint(), properties)
    style.getSourceAs<GeoJsonSource>(MbxMarkerSourceId)?.feature(feature)
    style.getLayerAs<SymbolLayer>(MbxMarkerLayerId)?.iconImage(icon)

    val screenPoint: ScreenCoordinate = mapboxMap.pixelForCoordinate(markerLatLng.toMapboxPoint())
    onResult(caption, Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat()))
}

@Composable
private fun rememberMapboxMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycle = lifecycleOwner.lifecycle
        var currentLifecycleState = Lifecycle.State.INITIALIZED

        fun moveTo(target: Lifecycle.State) {
            if (currentLifecycleState < Lifecycle.State.STARTED && target >= Lifecycle.State.STARTED) {
                mapView.onStart()
                currentLifecycleState = Lifecycle.State.STARTED
            }
        }

        moveTo(lifecycle.currentState)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> moveTo(Lifecycle.State.STARTED)
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
            if (currentLifecycleState >= Lifecycle.State.STARTED) {
                mapView.onStop()
            }
            mapView.onDestroy()
        }
    }

    return mapView
}
