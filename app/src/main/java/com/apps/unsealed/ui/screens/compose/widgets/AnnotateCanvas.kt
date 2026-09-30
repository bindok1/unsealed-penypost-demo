package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private const val WaveAmplitudeDp = 6f
private const val WaveWavelengthDp = 24f
private const val WaveSampleStepDp = 4f

/**
 * Layer 4 from compose-screen-spec.md §2/§5 — the hand-drawn annotation
 * layer. [annotateState].paths are always rendered (annotations stay visible
 * after leaving Annotate Mode, per spec), but the pointer-input gesture
 * detector below only attaches while [isAnnotateMode] is true. When it's
 * false, this plain [Canvas] registers no pointer interest at all, so taps
 * and drags fall through to the [LetterCanvas] text field beneath it.
 */
@Composable
fun AnnotateCanvas(
    annotateState: AnnotateState,
    isAnnotateMode: Boolean,
    onPathCommit: (AnnotatePath) -> Unit,
    onErase: (point: Offset, radiusPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val eraserRadiusPx = remember(density) { with(density) { 16.dp.toPx() } }
    var currentPath by remember { mutableStateOf<AnnotatePath?>(null) }

    val gestureModifier = if (isAnnotateMode) {
        Modifier.pointerInput(annotateState.selectedTool, annotateState.inkColor, annotateState.strokeWidth) {
            when (annotateState.selectedTool) {
                AnnotateTool.DECO -> detectTapGestures { offset ->
                    onPathCommit(
                        AnnotatePath(
                            points = listOf(offset),
                            color = annotateState.inkColor,
                            strokeWidth = annotateState.strokeWidth,
                            tool = AnnotateTool.DECO,
                        ),
                    )
                }
                AnnotateTool.ERASER -> detectDragGestures { change, _ ->
                    change.consume()
                    onErase(change.position, eraserRadiusPx)
                }
                AnnotateTool.PEN, AnnotateTool.FINE, AnnotateTool.WAVE -> detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = AnnotatePath(
                            points = listOf(offset),
                            color = annotateState.inkColor,
                            strokeWidth = annotateState.strokeWidth,
                            tool = annotateState.selectedTool,
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPath = currentPath?.let { it.copy(points = it.points + change.position) }
                    },
                    onDragEnd = {
                        currentPath?.let(onPathCommit)
                        currentPath = null
                    },
                    onDragCancel = { currentPath = null },
                )
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier.then(gestureModifier)) {
        annotateState.paths.forEach { drawAnnotatePath(it) }
        currentPath?.let { drawAnnotatePath(it) }
    }
}

private fun DrawScope.drawAnnotatePath(path: AnnotatePath) {
    when (path.tool) {
        AnnotateTool.PEN, AnnotateTool.FINE -> drawSmoothStroke(path.points, path.color, path.strokeWidth)
        AnnotateTool.WAVE -> drawSmoothStroke(waveify(path.points), path.color, path.strokeWidth)
        AnnotateTool.DECO -> path.points.firstOrNull()?.let { drawHeartStamp(it, path.color, path.strokeWidth) }
        AnnotateTool.ERASER -> Unit // eraser never produces a committed path
    }
}

private fun DrawScope.drawSmoothStroke(points: List<Offset>, color: Color, strokeWidth: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color = color, radius = strokeWidth / 2f, center = points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            val midX = (points[i].x + points[i + 1].x) / 2f
            val midY = (points[i].y + points[i + 1].y) / 2f
            quadraticTo(points[i].x, points[i].y, midX, midY)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Resamples [points] at a fixed arc-length step, then perturbs each sample
 * perpendicular to its local tangent by a sine wave — turns a rough drag
 * gesture into a decorative "wavy underline" per compose-screen-spec.md §5
 * (there's no wave-preset asset/infra to draw on, so this is a procedural
 * stand-in driven entirely by the user's own gesture). */
private fun DrawScope.waveify(points: List<Offset>): List<Offset> {
    if (points.size < 2) return points
    val stepPx = WaveSampleStepDp.dp.toPx()

    val resampled = mutableListOf(points.first())
    var travelled = 0f
    var nextTarget = stepPx
    var segIndex = 0
    while (segIndex < points.size - 1) {
        val segStart = points[segIndex]
        val segEnd = points[segIndex + 1]
        val segLength = (segEnd - segStart).getDistance()
        if (segLength == 0f || travelled + segLength < nextTarget) {
            travelled += segLength
            segIndex++
            continue
        }
        val t = (nextTarget - travelled) / segLength
        resampled += Offset(segStart.x + (segEnd.x - segStart.x) * t, segStart.y + (segEnd.y - segStart.y) * t)
        nextTarget += stepPx
    }
    resampled += points.last()

    val amplitudePx = WaveAmplitudeDp.dp.toPx()
    val wavelengthPx = WaveWavelengthDp.dp.toPx()
    var distanceAlong = 0f
    return resampled.mapIndexed { index, point ->
        if (index == 0 || index == resampled.size - 1) return@mapIndexed point
        val prev = resampled[index - 1]
        val segment = point - prev
        val length = segment.getDistance()
        distanceAlong += length
        if (length == 0f) return@mapIndexed point
        val tangent = Offset(segment.x / length, segment.y / length)
        val perpendicular = Offset(-tangent.y, tangent.x)
        val offsetAmount = amplitudePx * sin(2f * Math.PI.toFloat() * distanceAlong / wavelengthPx)
        Offset(point.x + perpendicular.x * offsetAmount, point.y + perpendicular.y * offsetAmount)
    }
}

/** Procedural filled-heart stamp for the DECO tool — substitutes the spec's
 * asset-based stamp grid (heart/star/flower PNGs), since no sticker assets
 * exist in this project. [strokeWidth] is repurposed here as the stamp's
 * render size (see the field's usage note in ComposeUiState.kt/AnnotatePath). */
private fun DrawScope.drawHeartStamp(center: Offset, color: Color, strokeWidth: Float) {
    val scale = strokeWidth / 40f // path authored against a ~40px reference size
    val cx = center.x
    val cy = center.y
    val path = Path().apply {
        moveTo(cx, cy + 10f * scale)
        cubicTo(
            cx - 20f * scale, cy - 15f * scale,
            cx - 10f * scale, cy - 30f * scale,
            cx, cy - 12f * scale,
        )
        cubicTo(
            cx + 10f * scale, cy - 30f * scale,
            cx + 20f * scale, cy - 15f * scale,
            cx, cy + 10f * scale,
        )
        close()
    }
    drawPath(path = path, color = color)
}
