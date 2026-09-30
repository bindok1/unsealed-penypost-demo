package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction
import com.apps.unsealed.ui.theme.BrandGold
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A single user-inserted photo, draggable/pinch-resizable/rotatable in place
 * (compose-screen-spec.md §8's "layer draggable" — see
 * `docs/image-edit-mode-handoff.md` for the full design rationale, including
 * why this got its own file instead of living inline in `LetterCanvas.kt`).
 *
 * Gestures apply regardless of selection — [isSelected] only controls
 * whether the dashed selection border and [ImageActionsMenu] trigger are
 * shown, so a user can grab and move any image without a separate
 * "select first" step. A plain tap alone (no drag/pinch) doesn't produce a
 * [detectTransformGestures] delta, so tapping the photo directly also calls
 * [onSelect] to cover that case. Actual deselection ("tap elsewhere confirms
 * placement") is handled by a full-canvas scrim in `LetterCanvas.kt`, not
 * here.
 */
@Composable
fun EditableImage(
    image: ImageInstance,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTransform: (pan: Offset, zoom: Float, rotationDeg: Float) -> Unit,
    onBaseSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit,
    onReplaceClick: () -> Unit,
    onCropClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onSendToBackClick: () -> Unit,
    onBringToFrontClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResetScaleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isCropped = image.cropRect != FullCropRect && image.baseWidthPx > 0f && image.baseHeightPx > 0f
    val interactionSource = remember { MutableInteractionSource() }

    // ImageActionsMenu anchors to the photo's *actual visual* top edge, not
    // a constant offset from its unscaled layout box — a fixed offset left a
    // huge gap when the photo was shrunk (the box's reported layout size
    // never changes, only its graphicsLayer scale) and could overlap the
    // photo when enlarged. See docs/image-edit-mode-handoff.md.
    val effectiveWidthPx = if (isCropped) image.baseWidthPx * image.cropRect.width else image.baseWidthPx
    val effectiveHeightPx = if (isCropped) image.baseHeightPx * image.cropRect.height else image.baseHeightPx
    val menuOffsetY = with(density) {
        val gapPx = ImageActionsMenuGap.toPx()
        (visualTopEdgePx(effectiveWidthPx, effectiveHeightPx, image.scale, image.rotation) - gapPx).toDp()
    }

    Box(
        // Position-only wrapper — deliberately carries no transform of its
        // own, so ImageActionsMenu (a sibling of the scaled/rotated photo Box
        // below, not a child of it) stays a constant size and upright no
        // matter how the photo itself is scaled or rotated. Previously the
        // controls lived *inside* the transformed node and shrank/rotated
        // along with the image — this structure is the fix for that.
        modifier.offset { IntOffset(image.offsetX.roundToInt(), image.offsetY.roundToInt()) },
    ) {
        Box(
            // pointerInput and graphicsLayer must stay on this same node:
            // Compose only hit-tests a gesture against the visually
            // scaled/rotated area when the detector and the transform share
            // one modifier chain. Moving graphicsLayer to a child node (to
            // keep it away from the controls above) would make drag/pinch
            // targets track the image's *unscaled* bounds instead of what's
            // actually on screen.
            Modifier
                .pointerInput(image.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onSelect()
                        onTransform(pan, zoom, rotation)
                    }
                }
                .graphicsLayer {
                    scaleX = image.scale
                    scaleY = image.scale
                    rotationZ = image.rotation
                },
        ) {
            val photoModifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
                .then(if (isSelected) Modifier.dashedSelectionBorder() else Modifier)

            if (isCropped) {
                // "Trim" model: the box shrinks to the cropped sub-rect's own
                // size, and the full photo is drawn at its original (base)
                // size but shifted so only the kept region falls inside the
                // box — clipped there. Not a re-encode of the source photo,
                // just a display-time crop; see docs/image-edit-mode-handoff.md.
                val cropWidthDp = with(density) { (image.baseWidthPx * image.cropRect.width).toDp() }
                val cropHeightDp = with(density) { (image.baseHeightPx * image.cropRect.height).toDp() }
                val fullWidthDp = with(density) { image.baseWidthPx.toDp() }
                val fullHeightDp = with(density) { image.baseHeightPx.toDp() }
                val innerOffsetX = with(density) { (-image.baseWidthPx * image.cropRect.left).toDp() }
                val innerOffsetY = with(density) { (-image.baseHeightPx * image.cropRect.top).toDp() }
                Box(photoModifier.size(cropWidthDp, cropHeightDp).clipToBounds()) {
                    AsyncImage(
                        model = image.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(fullWidthDp, fullHeightDp)
                            .offset(x = innerOffsetX, y = innerOffsetY),
                    )
                }
            } else {
                AsyncImage(
                    model = image.uri,
                    contentDescription = null,
                    modifier = photoModifier
                        .fillMaxWidth(EditableImageBaseWidthFraction)
                        .onSizeChanged { onBaseSizeMeasured(it.width.toFloat(), it.height.toFloat()) },
                )
            }

            if (isSelected) {
                CornerResizeHandle(
                    imageScale = image.scale,
                    widthPx = effectiveWidthPx,
                    heightPx = effectiveHeightPx,
                    onSelect = onSelect,
                    onResize = { zoomDelta -> onTransform(Offset.Zero, zoomDelta, 0f) },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }

        if (isSelected) {
            ImageActionsMenu(
                onReplaceClick = onReplaceClick,
                onCropClick = onCropClick,
                onDuplicateClick = onDuplicateClick,
                onSendToBackClick = onSendToBackClick,
                onBringToFrontClick = onBringToFrontClick,
                onDeleteClick = onDeleteClick,
                onResetScaleClick = onResetScaleClick,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = menuOffsetY),
            )
        }
    }
}

/** Full-photo crop editor shown in place of [EditableImage] while
 * [ComposeUiState.croppingImageId] matches this image — always displays the
 * *entire* uncropped photo (at its cached [ImageInstance.baseWidthPx]/
 * [ImageInstance.baseHeightPx]) with a [CropFrame] overlay so the user can
 * see everything available to crop into, regardless of any crop already
 * applied. No drag/pinch/rotate here — repositioning happens in normal mode,
 * this is crop-only. */
@Composable
fun ImageCropOverlay(
    image: ImageInstance,
    onCropChange: (Rect) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val boxWidth = with(density) { image.baseWidthPx.toDp() }
    val boxHeight = with(density) { image.baseHeightPx.toDp() }

    Box(
        modifier
            .offset { IntOffset(image.offsetX.roundToInt(), image.offsetY.roundToInt()) }
            .graphicsLayer {
                scaleX = image.scale
                scaleY = image.scale
                rotationZ = image.rotation
            },
    ) {
        Box(Modifier.size(boxWidth, boxHeight)) {
            AsyncImage(
                model = image.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            CropFrame(initialCropRect = image.cropRect, onCropRectChange = onCropChange)
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -ImageActionsMenuGap)
                .background(Color(0xFF2A241C), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp),
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.image_crop_reset), color = Color.White)
            }
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.image_crop_done), color = Color.White)
            }
        }
    }
}

private const val CropMinFraction = 0.15f
private val CropHandleHitRadius = 28.dp

/** Drag any of the 4 corners to live-resize the crop window (fractions
 * 0f..1f per edge). Runs entirely on locally-remembered state during the
 * gesture — same pattern as `AnnotateCanvas.kt`'s `currentPath` — and only
 * calls [onCropRectChange] once per completed drag, since nothing downstream
 * needs a per-pixel stream of updates while dragging. */
@Composable
private fun CropFrame(
    initialCropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cropRect by remember { mutableStateOf(initialCropRect) }
    var activeCorner by remember { mutableStateOf(-1) }
    val density = LocalDensity.current
    val handleHitRadiusPx = remember(density) { with(density) { CropHandleHitRadius.toPx() } }
    val strokeWidthPx = remember(density) { with(density) { 2.dp.toPx() } }
    val gripRadiusPx = remember(density) { with(density) { 5.dp.toPx() } }

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val corners = listOf(
                            Offset(cropRect.left * w, cropRect.top * h),
                            Offset(cropRect.right * w, cropRect.top * h),
                            Offset(cropRect.right * w, cropRect.bottom * h),
                            Offset(cropRect.left * w, cropRect.bottom * h),
                        )
                        val nearest = corners.indices.minByOrNull { (corners[it] - start).getDistance() } ?: -1
                        activeCorner = if (nearest != -1 && (corners[nearest] - start).getDistance() <= handleHitRadiusPx) {
                            nearest
                        } else {
                            -1
                        }
                    },
                    onDragEnd = { onCropRectChange(cropRect) },
                    onDrag = { change, dragAmount ->
                        if (activeCorner == -1) return@detectDragGestures
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectDragGestures
                        val dxFrac = dragAmount.x / w
                        val dyFrac = dragAmount.y / h
                        cropRect = when (activeCorner) {
                            0 -> cropRect.copy(
                                left = (cropRect.left + dxFrac).coerceIn(0f, cropRect.right - CropMinFraction),
                                top = (cropRect.top + dyFrac).coerceIn(0f, cropRect.bottom - CropMinFraction),
                            )
                            1 -> cropRect.copy(
                                right = (cropRect.right + dxFrac).coerceIn(cropRect.left + CropMinFraction, 1f),
                                top = (cropRect.top + dyFrac).coerceIn(0f, cropRect.bottom - CropMinFraction),
                            )
                            2 -> cropRect.copy(
                                right = (cropRect.right + dxFrac).coerceIn(cropRect.left + CropMinFraction, 1f),
                                bottom = (cropRect.bottom + dyFrac).coerceIn(cropRect.top + CropMinFraction, 1f),
                            )
                            else -> cropRect.copy(
                                left = (cropRect.left + dxFrac).coerceIn(0f, cropRect.right - CropMinFraction),
                                bottom = (cropRect.bottom + dyFrac).coerceIn(cropRect.top + CropMinFraction, 1f),
                            )
                        }
                    },
                )
            },
    ) {
        drawCropScrimAndFrame(cropRect, strokeWidthPx, gripRadiusPx)
    }
}

private fun DrawScope.drawCropScrimAndFrame(cropRect: Rect, strokeWidthPx: Float, gripRadiusPx: Float) {
    val w = size.width
    val h = size.height
    val rectPx = Rect(cropRect.left * w, cropRect.top * h, cropRect.right * w, cropRect.bottom * h)
    val scrim = Color.Black.copy(alpha = 0.55f)
    drawRect(scrim, topLeft = Offset.Zero, size = Size(w, rectPx.top))
    drawRect(scrim, topLeft = Offset(0f, rectPx.bottom), size = Size(w, h - rectPx.bottom))
    drawRect(scrim, topLeft = Offset(0f, rectPx.top), size = Size(rectPx.left, rectPx.height))
    drawRect(scrim, topLeft = Offset(rectPx.right, rectPx.top), size = Size(w - rectPx.right, rectPx.height))
    drawRect(
        color = Color.White,
        topLeft = rectPx.topLeft,
        size = rectPx.size,
        style = Stroke(width = strokeWidthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))),
    )
    listOf(rectPx.topLeft, Offset(rectPx.right, rectPx.top), rectPx.bottomRight, Offset(rectPx.left, rectPx.bottom))
        .forEach { drawCircle(Color.White, radius = gripRadiusPx, center = it) }
}

private val ImageActionsMenuTriggerSize = 32.dp
private val ImageActionsMenuGap = 44.dp

private val ResizeHandleTouchSize = 44.dp
private val ResizeHandleVisualSize = 28.dp

/**
 * 1-finger corner drag handle positioned at the bottom-right corner of a selected photo/sticker.
 * Dragging outward scales up, dragging inward scales down.
 * An inverse scale is applied so the handle maintains a constant, easily-touchable
 * physical size on screen regardless of how zoomed-in or tiny the image is.
 */
@Composable
private fun CornerResizeHandle(
    imageScale: Float,
    widthPx: Float,
    heightPx: Float,
    onSelect: () -> Unit,
    onResize: (zoomDelta: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = ResizeHandleTouchSize / 2, y = ResizeHandleTouchSize / 2)
            .graphicsLayer {
                val invScale = 1f / imageScale.coerceAtLeast(0.1f)
                scaleX = invScale
                scaleY = invScale
            }
            .size(ResizeHandleTouchSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = if (widthPx > 0f) widthPx else 300f
                        val h = if (heightPx > 0f) heightPx else 300f
                        val halfW = w / 2f
                        val halfH = h / 2f
                        val diag = sqrt(halfW * halfW + halfH * halfH)
                        if (diag <= 0f) return@detectDragGestures

                        val ux = halfW / diag
                        val uy = halfH / diag

                        val dr = dragAmount.x * ux + dragAmount.y * uy
                        val zoomDelta = 1f + (dr / diag)
                        if (zoomDelta > 0.001f) {
                            onResize(zoomDelta)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ResizeHandleVisualSize)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF2A241C))
                .border(BorderStroke(1.5.dp, BrandGold), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInFull,
                contentDescription = stringResource(R.string.image_edit_resize_desc),
                tint = BrandGold,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Crop is implemented ([ImageCropOverlay]/[CropFrame] below) but hidden
 * from the menu for now — flip this back on once it's had another pass
 * (see `docs/image-edit-mode-handoff.md`). Keeping the implementation in
 * place rather than deleting it, since the plan is to improve rather than
 * drop the feature. */
private const val CropFeatureEnabled = false

/** The "···" trigger + dropdown replacing the old two-icon (change/delete)
 * row — consolidates every per-image action behind one anchored menu built
 * on the shared [AnchoredDropdownMenu] chrome (see
 * `docs/component-library.md`), matching the Overflow Menu's pattern from
 * compose-screen-spec.md §6. */
@Composable
private fun ImageActionsMenu(
    onReplaceClick: () -> Unit,
    onCropClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onSendToBackClick: () -> Unit,
    onBringToFrontClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResetScaleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(modifier) {
        Box(
            Modifier
                .size(ImageActionsMenuTriggerSize)
                .clip(CircleShape)
                .background(Color(0xFF2A241C))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.image_edit_more_desc),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        AnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            alignment = Alignment.TopCenter,
            offset = with(density) { IntOffset(0, (ImageActionsMenuTriggerSize + 6.dp).roundToPx()) },
            transformOrigin = TransformOrigin(0.5f, 0f),
            width = 200.dp,
        ) {
            DropdownMenuAction(
                icon = Icons.Filled.Image,
                label = stringResource(R.string.image_edit_change_desc),
                onClick = { expanded = false; onReplaceClick() },
            )
            if (CropFeatureEnabled) {
                DropdownMenuAction(
                    icon = Icons.Filled.Crop,
                    label = stringResource(R.string.image_edit_crop_desc),
                    onClick = { expanded = false; onCropClick() },
                )
            }
            DropdownMenuAction(
                icon = Icons.Filled.Refresh,
                label = stringResource(R.string.image_edit_reset_scale),
                onClick = { expanded = false; onResetScaleClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.ContentCopy,
                label = stringResource(R.string.image_edit_duplicate_desc),
                onClick = { expanded = false; onDuplicateClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.FlipToBack,
                label = stringResource(R.string.image_edit_send_to_back_desc),
                onClick = { expanded = false; onSendToBackClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.FlipToFront,
                label = stringResource(R.string.image_edit_bring_to_front_desc),
                onClick = { expanded = false; onBringToFrontClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.image_edit_delete_desc),
                onClick = { expanded = false; onDeleteClick() },
            )
        }
    }
}

private const val EditableImageBaseWidthFraction = 0.7f

/** Y-position (in px, measured from the *unscaled* box's own top edge — see
 * [EditableImage]) of the highest point of an image after it's been scaled
 * and rotated about its center. Used to anchor [ImageActionsMenu] to the
 * photo's real visual top edge instead of a constant offset that drifts as
 * soon as scale != 1 or rotation != 0. Returns 0f if the size isn't known
 * yet (falls back to sitting right at the unscaled top edge). */
private fun visualTopEdgePx(widthPx: Float, heightPx: Float, scale: Float, rotationDegrees: Float): Float {
    if (widthPx <= 0f || heightPx <= 0f) return 0f
    val halfWidth = widthPx / 2f * scale
    val halfHeight = heightPx / 2f * scale
    val angleRad = Math.toRadians(rotationDegrees.toDouble())
    val cosAngle = cos(angleRad).toFloat()
    val sinAngle = sin(angleRad).toFloat()
    val cornerYs = listOf(
        Offset(-halfWidth, -halfHeight),
        Offset(halfWidth, -halfHeight),
        Offset(halfWidth, halfHeight),
        Offset(-halfWidth, halfHeight),
    ).map { corner -> corner.x * sinAngle + corner.y * cosAngle }
    return heightPx / 2f + cornerYs.min()
}

/** Dashed outline communicating "this photo is in edit mode" — tap elsewhere
 * on the canvas to confirm its placement and dismiss it (handled by
 * `LetterCanvas.kt`'s full-canvas scrim, not here). */
private fun Modifier.dashedSelectionBorder(): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = Color.White,
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)),
    )
}
