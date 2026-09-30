package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.components.collectibles.CollectibleCtaBanner
import com.apps.unsealed.ui.components.collectibles.CollectibleSectionHeader

private val StampCardBorderSelected = Color(0xFF1F2937)

/** Shared-bounds key connecting the envelope's stamp-corner placeholder box
 * (`EnvelopeCard.kt`'s `EnvelopeHeaderRow`) to this overlay's outer panel —
 * that one small box is literally the same transitioning element as this
 * panel, per Compose's `SharedTransitionLayout`/container-transform model. */
internal const val StampSheetSharedKey = "stampSheetContainer"

/** motion-rules.md §3.5: "Stamp punya bounce paling dramatis" — a bespoke,
 * lower-damping spring for the select pulse, more dramatic than the shared
 * [com.apps.unsealed.core.util.LetterlySpring.Bouncy], same local-override
 * pattern `FontPickerSheet.kt` already uses. */
private val StampSelectSpring = spring<Float>(dampingRatio = 0.3f, stiffness = 300f)
private val BouncyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

/**
 * "Select Stamps" panel — horizontal row over [stamps] (CMS-driven catalog,
 * see `feature/catalog/data/` and `SelectRecipientViewModel.stampCatalog`),
 * rendered via [StampImage] (real art when the catalog has it, [StampSwatch]'s
 * vector stand-in otherwise). This is the *same* picker that satisfies the
 * "must pick a stamp before send" requirement — there is no separate gate,
 * selecting here is what flips the screen title to "Ready to Send".
 *
 * No scrim — the envelope stays fully visible (not dimmed) underneath while
 * picking, matching the reference layout (`stamps.png`); the transparent
 * background here still intercepts taps to dismiss on tap-outside.
 *
 * Deliberately a plain in-tree overlay, not a `ModalBottomSheet`/`Dialog`:
 * shared-element transitions need both ends of the transition (this panel
 * and the envelope's stamp-corner box) in the *same* composition/window.
 * A `Dialog` renders into a separate Android window, which broke that (and,
 * separately, was the root cause of an earlier full-screen-sizing bug) — see
 * the removed `TopSheet.kt`. Caller is expected to host this inside an
 * `AnimatedVisibility` within a `SharedTransitionLayout`, matching the
 * pattern in `SelectRecipientScreen.kt`.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.StampPickerOverlay(
    stamps: List<CatalogItemDto>,
    selectedStamp: CatalogItemDto?,
    onStampSelect: (CatalogItemDto) -> Unit,
    onDismiss: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onNavigateToStore: ((source: String) -> Unit)? = null,
) {
    val basicStamps = remember(stamps) { stamps.filter { !it.isPremium } }
    val creatorStamps = remember(stamps) { stamps.filter { it.isPremium } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = StampSheetSharedKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            with(animatedVisibilityScope) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .animateEnterExit(enter = fadeIn(), exit = fadeOut()),
                ) {
                    Text(
                        stringResource(R.string.stamp_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(18.dp))

                    // ── Section 1: Koleksi Dasar ─────────────────────────────
                    CollectibleSectionHeader(
                        title = stringResource(R.string.picker_category_basic),
                        countText = stringResource(R.string.picker_collectibles_count, basicStamps.size),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        basicStamps.forEach { stamp ->
                            StampCard(
                                stamp = stamp,
                                isSelected = stamp.id == selectedStamp?.id,
                                onClick = { onStampSelect(stamp) },
                                modifier = Modifier.width(96.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Section 2: Koleksi Saya (Kreator) ─────────────────────
                    CollectibleSectionHeader(
                        title = stringResource(R.string.picker_category_collectibles),
                        countText = if (creatorStamps.isNotEmpty()) stringResource(R.string.picker_collectibles_count, creatorStamps.size) else null,
                        isCreatorSection = true,
                        actionText = if (creatorStamps.isNotEmpty()) stringResource(R.string.picker_collectibles_find_more_short) else null,
                        onActionClick = if (creatorStamps.isNotEmpty()) {
                            {
                                onDismiss()
                                onNavigateToStore?.invoke("header_action")
                            }
                        } else null,
                    )
                    Spacer(Modifier.height(10.dp))

                    if (creatorStamps.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            creatorStamps.forEach { stamp ->
                                StampCard(
                                    stamp = stamp,
                                    isSelected = stamp.id == selectedStamp?.id,
                                    onClick = { onStampSelect(stamp) },
                                    modifier = Modifier.width(96.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Direct CTA banner underneath My Collectibles
                    CollectibleCtaBanner(
                        title = stringResource(R.string.picker_collectibles_stamp_pitch),
                        description = stringResource(R.string.picker_collectibles_stamp_desc),
                        ctaText = stringResource(R.string.picker_collectibles_find_more_short),
                        onCtaClick = {
                            onDismiss()
                            onNavigateToStore?.invoke(if (creatorStamps.isNotEmpty()) "below_items_cta" else "empty_showcase_cta")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StampCard(
    stamp: CatalogItemDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.15f, reducedMotionSpring(StampSelectSpring, isReducedMotion))
            scale.animateTo(1f, reducedMotionSpring(StampSelectSpring, isReducedMotion))
        } else {
            scale.animateTo(1f, reducedMotionSpring(StampSelectSpring, isReducedMotion))
        }
    }
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = reducedMotionSpring(BouncyDpSpring, isReducedMotion),
        label = "stampCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) StampCardBorderSelected else StampCardBorderSelected.copy(alpha = 0.15f),
        label = "stampCardBorderColor",
    )

    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StampImage(
                item = stamp,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(stamp.name, style = MaterialTheme.typography.labelSmall)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.stamp_picker_selected_desc),
                tint = StampCardBorderSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}
