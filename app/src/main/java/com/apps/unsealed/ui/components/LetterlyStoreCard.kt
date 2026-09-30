package com.apps.unsealed.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.InkDefault

/** Same ratio as [com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign]'s
 * `EnvelopeAspectRatio` — duplicated (not imported) since `ui/components` sits
 * lower in the dependency graph than screen packages, and every "hero banner"
 * card in the app should read as the same family regardless. */
private const val WideCardAspectRatio = 1720f / 900f

private val StoreCardBackground = Color(0xFFF2F0EA)

/** Fresh Dp-typed spring mirroring [LetterlySpring.Bouncy] — the shared object
 * only exposes `spring<Float>`, which can't back `animateDpAsState`. Same
 * precedent as `FontPickerSheet.kt`'s private `BouncyDpSpring`. */
private val BouncyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

enum class StoreCardShape { Square, Wide }

/**
 * Shared title + subtitle + image card for the Stamps store (docs/component-library.md's
 * extraction rule — [StoreCardShape.Square] generalizes `FontPickerCard` /
 * `StampCard` (`StampPickerOverlay.kt`), [StoreCardShape.Wide] generalizes
 * `EnvelopeCard`'s full-bleed-image-with-overlay recipe).
 *
 * - [StoreCardShape.Square]: grid item, selectable — border + check badge
 *   animate with [isSelected] (motion-rules.md §3.7).
 * - [StoreCardShape.Wide]: full-bleed hero banner, not selectable — [isSelected]
 *   is ignored; communicate state (e.g. "Claimed") through [badge] instead.
 *
 * [image] is a content slot rather than `imageRes`/`imageUrl` params so today's
 * bundled-drawable or procedurally-drawn callers, and a future Coil `AsyncImage`
 * (once RevenueCat provides remote product art), both work with zero changes here.
 */
@Composable
fun LetterlyStoreCard(
    shape: StoreCardShape,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isSelected: Boolean = false,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    image: @Composable BoxScope.() -> Unit,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val isSquare = shape == StoreCardShape.Square
    val cardShape = RoundedCornerShape(if (isSquare) 16.dp else 12.dp)

    val selectionScale = remember { Animatable(1f) }
    LaunchedEffect(isSelected, isSquare) {
        if (!isSquare) return@LaunchedEffect
        if (isSelected) {
            selectionScale.animateTo(1.05f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
            selectionScale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        } else {
            selectionScale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        }
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = reducedMotionSpring(BouncyDpSpring, isReducedMotion),
        label = "storeCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) InkDefault else InkDefault.copy(alpha = 0.15f),
        label = "storeCardBorderColor",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = pressScale * selectionScale.value
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSquare) Modifier.aspectRatio(1f) else Modifier.fillMaxWidth().aspectRatio(WideCardAspectRatio),
            )
            .clip(cardShape)
            .then(
                if (isSquare) {
                    Modifier.background(StoreCardBackground).border(borderWidth, borderColor, cardShape)
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        if (isSquare) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.size(48.dp), content = image)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkDefault,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkDefault.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(InkDefault),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), content = image)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)),
                    ),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        if (badge != null) {
            Box(modifier = Modifier.fillMaxSize(), content = badge)
        }
    }
}
