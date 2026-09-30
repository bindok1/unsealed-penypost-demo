package com.apps.unsealed.ui.screens.profile.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.compose.widgets.DeviceCapabilityBottomSheet
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionItem
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.SurfaceCream
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Full-screen letter reader for the profile's Postal Collection Showcase —
 * same full-bleed slide-up-from-below shell as PenPals'
 * `OpenLetterOverlay.kt` (vertical pager, tap-anywhere to toggle chrome,
 * top/bottom gradients, close button), minus the social action rail/sender
 * info bar/report/block — none of that applies to the owner's own sent
 * letters. Always opens straight to the letter/paper face (never the
 * envelope face) regardless of which Postal Collection tab was tapped from,
 * matching `OpenLetterOverlay`'s own behavior (it has no envelope view).
 */
@Composable
fun PostalCollectionFullView(
    items: List<PostalCollectionItem>,
    initialIndex: Int,
    isOpen: Boolean,
    onClose: () -> Unit,
    onFullyClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    if (items.isEmpty()) return

    var controlsVisible by remember { mutableStateOf(true) }
    var isCapabilityInfoVisible by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Reset chrome visibility on every swipe, same as OpenLetterOverlay —
    // otherwise a hidden-controls state from the previous page would carry
    // over and strand the user with no way to see the close button.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                controlsVisible = true
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
    }

    val slideOffsetFraction by animateFloatAsState(
        targetValue = if (isOpen) 0f else 1f,
        animationSpec = reducedMotionSpring(
            if (isOpen) LetterlySpring.Gentle else LetterlySpring.Stiff,
            isReducedMotion,
        ),
        label = "postalFullViewSlideOffset",
        finishedListener = { fraction -> if (fraction >= 0.98f) onFullyClosed() },
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(200), isReducedMotion),
        label = "postalFullViewOverlayAlpha",
    )
    val controlsAlpha by animateFloatAsState(
        targetValue = if (isOpen && controlsVisible) 1f else 0f,
        animationSpec = reducedMotionSpring(tween(220), isReducedMotion),
        label = "postalFullViewControlsAlpha",
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenHeightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandInkDeep.copy(alpha = 0.95f))
                .graphicsLayer {
                    translationY = screenHeightPx * slideOffsetFraction
                    alpha = overlayAlpha
                },
        ) {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = isOpen,
                modifier = Modifier.fillMaxSize(),
                key = { items[it].letterId },
            ) { page ->
                val item = items[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                controlsVisible = !controlsVisible
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 76.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 500.dp)
                                .background(SurfaceCream)
                                .border(1.dp, Color.White.copy(alpha = 0.18f))
                                .clipToBounds(),
                        ) {
                            if (item.compositeImageUrl != null) {
                                AsyncImage(
                                    model = item.compositeImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    placeholder = painterResource(item.paperTemplate.drawableRes),
                                    error = painterResource(item.paperTemplate.drawableRes),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 500.dp),
                                )
                            } else {
                                // Sender's device skipped compositing — same
                                // bodyText-over-paper-texture fallback
                                // OpenLetterOverlay uses for seed letters,
                                // see docs/be/letters_api.md §"Update (2026-08-18)".
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 500.dp),
                                ) {
                                    Image(
                                        painter = painterResource(item.paperTemplate.drawableRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize(),
                                    )
                                    Text(
                                        text = item.bodyText.orEmpty(),
                                        fontFamily = CaveatFontFamily,
                                        fontSize = 20.sp,
                                        lineHeight = 30.sp,
                                        color = InkDefault,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                    )
                                }
                            }
                        }

                        if (item.compositeImageUrl == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clip(CircleShape)
                                    .background(ToolbarFrostedDark)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { isCapabilityInfoVisible = true },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = stringResource(R.string.profile_content_showcase_no_preview_caption),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // Top gradient overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart)
                    .height(130.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to Color(0x99000000),
                            1f to Color.Transparent,
                        ),
                    )
                    .graphicsLayer { alpha = overlayAlpha * controlsAlpha },
            )

            PostalCollectionCloseButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .graphicsLayer { alpha = overlayAlpha * controlsAlpha },
            )
        }
    }

    if (isCapabilityInfoVisible) {
        DeviceCapabilityBottomSheet(onDismiss = { isCapabilityInfoVisible = false })
    }
}

@Composable
private fun PostalCollectionCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(40.dp)
            .clip(CircleShape)
            .background(ToolbarFrostedDark)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.open_letter_close_desc),
            tint = Color.White,
        )
    }
}
