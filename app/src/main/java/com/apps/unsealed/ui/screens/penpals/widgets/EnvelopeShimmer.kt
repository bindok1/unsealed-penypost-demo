package com.apps.unsealed.ui.screens.penpals.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.apps.unsealed.ui.screens.penpals.constants.deskScatterOffset
import com.apps.unsealed.ui.screens.penpals.constants.deskScatterRotation
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.SurfaceCardLight
import com.apps.unsealed.ui.theme.SurfaceCream

// ─── Shimmer palette ─────────────────────────────────────────────────────────

private val ShimmerBase      = SurfaceCardLight              // warm parchment
private val ShimmerHighlight = SurfaceCream                  // near-white cream
private val ShimmerEdge      = Color(0xFFF0E8D8)             // soft sepia edge

// ─── Envelope Stack Shimmer ───────────────────────────────────────────────────

/**
 * Zero-layout-shift loading skeleton for loose desk cards.
 *
 * Renders [count] empty postcard-shaped rectangles at the exact same
 * [deskScatterOffset]/[deskScatterRotation] positions the real [DeskCard]s
 * will use once loaded.
 */
@Composable
fun EnvelopeStackShimmer(
    deskWidthPx: Float,
    deskHeightPx: Float,
    scatterTopPx: Float,
    cardWidth: Dp,
    count: Int = 4,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // Shimmer sweep animation — 1200 ms linear loop
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweepOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val cardWidthPx = with(density) { cardWidth.toPx() }
    val cardHeightPx = cardWidthPx / EnvelopeAspectRatio
    val cardHeight = with(density) { cardHeightPx.toDp() }
    val scatterAreaHeightPx = (deskHeightPx - scatterTopPx).coerceAtLeast(cardHeightPx)
    val cardShape = RoundedCornerShape(12.dp)

    val shimmerWidthPx = deskWidthPx

    Box(modifier = modifier.fillMaxSize()) {
        for (index in 0 until count) {
            val seed = index
            val offset: Offset = deskScatterOffset(
                seed = seed,
                areaWidthPx = deskWidthPx,
                areaHeightPx = scatterAreaHeightPx,
                cardWidthPx = cardWidthPx,
                cardHeightPx = cardHeightPx,
            )
            val rotation = deskScatterRotation(seed)

            val cardCenterX = shimmerWidthPx / 2f
            val sweepX = cardCenterX + sweepOffset * shimmerWidthPx
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    ShimmerBase,
                    ShimmerEdge,
                    ShimmerHighlight,
                    ShimmerEdge,
                    ShimmerBase,
                ),
                start = Offset(sweepX - shimmerWidthPx * 0.5f, 0f),
                end   = Offset(sweepX + shimmerWidthPx * 0.5f, 0f),
            )

            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    .graphicsLayer {
                        translationX = offset.x
                        translationY = scatterTopPx + offset.y
                        rotationZ = rotation
                        shadowElevation = with(density) { 2.dp.toPx() }
                        shape = cardShape
                        clip = true
                    }
                    .zIndex(index.toFloat())
                    .clip(cardShape)
                    .background(shimmerBrush),
            )
        }
    }
}

// ─── Grid Shimmer ──────────────────────────────────────────────────────────

/**
 * Zero-layout-shift loading skeleton for [PenPalsGridContent] — same shimmer
 * palette/sweep motion as [EnvelopeStackShimmer], laid out in the identical
 * 2-column grid (columns/padding/arrangement match [PenPalsGridContent]
 * exactly) instead of scattered absolute positions, since grid cards don't
 * have a free-floating desk to scatter across.
 */
@Composable
fun PenPalsGridShimmer(
    count: Int = 6,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "gridShimmer")
    val sweepFraction by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gridShimmerSweep",
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(count) {
            ShimmerCard(sweepFraction = sweepFraction, modifier = Modifier.fillMaxWidth().aspectRatio(EnvelopeAspectRatio))
        }
    }
}

/** One skeleton card — sweep gradient scoped to its own measured width via
 * [BoxWithConstraints] instead of a shared desk width, since grid items don't
 * share one absolute coordinate space the way [EnvelopeStackShimmer]'s
 * scattered cards do. */
@Composable
private fun ShimmerCard(sweepFraction: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cardShape = RoundedCornerShape(12.dp)
    BoxWithConstraints(modifier = modifier.clip(cardShape)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val sweepX = sweepFraction * widthPx
        val shimmerBrush = Brush.linearGradient(
            colors = listOf(
                ShimmerBase,
                ShimmerEdge,
                ShimmerHighlight,
                ShimmerEdge,
                ShimmerBase,
            ),
            start = Offset(sweepX - widthPx * 0.5f, 0f),
            end = Offset(sweepX + widthPx * 0.5f, 0f),
        )
        Box(Modifier.fillMaxSize().background(shimmerBrush))
    }
}
