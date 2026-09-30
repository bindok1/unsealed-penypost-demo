package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShimmerBase = Color(0x33FFFFFF)
private val ShimmerHighlight = Color(0x66FFFFFF)
private val ShimmerEdge = Color(0x22FFFFFF)

/**
 * Shimmer skeleton displayed while the Peny Store catalog is loading.
 * Shows a banner placeholder, filter chip row, and product rows with artwork shelves.
 */
@Composable
fun StoreSkeletonSection(
    modifier: Modifier = Modifier,
    showBanner: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "storeShimmer")
    val sweepOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "storeSweepOffset",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerEdge, ShimmerHighlight, ShimmerEdge, ShimmerBase),
        start = Offset(sweepOffset * 1000f - 500f, 0f),
        end = Offset(sweepOffset * 1000f + 500f, 1000f),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (showBanner) {
            // Showcase banner placeholder (4:3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(shimmerBrush),
            )

            Spacer(Modifier.height(12.dp))
        }

        // Filter chips placeholder
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerBrush),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Product item placeholders (matches ProductCard layout)
        repeat(2) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title + Price pill
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(shimmerBrush),
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Creator + desc
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush),
                )

                Spacer(Modifier.height(10.dp))

                // Artwork shelf placeholders (mix of landscape ~150dp and portrait ~86dp, height 115dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .height(115.dp)
                            .width(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush),
                    )
                    Box(
                        modifier = Modifier
                            .height(115.dp)
                            .width(86.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush),
                    )
                    Box(
                        modifier = Modifier
                            .height(115.dp)
                            .width(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
