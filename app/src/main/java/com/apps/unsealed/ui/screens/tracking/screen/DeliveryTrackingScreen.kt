package com.apps.unsealed.ui.screens.tracking.screen

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberAnimatedCount
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.screens.tracking.state.DeliveryTrackingUiState
import com.apps.unsealed.ui.screens.tracking.state.TransportMode
import com.apps.unsealed.ui.screens.tracking.state.UnlockLetterState
import com.apps.unsealed.ui.screens.tracking.state.flavorTextFor
import com.apps.unsealed.ui.screens.tracking.viewmodel.DeliveryTrackingViewModel
import com.apps.unsealed.ui.screens.tracking.widgets.DeliveredCelebrationModal
import com.apps.unsealed.ui.screens.tracking.widgets.DeliveryRouteMap
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Persistent tracking screen — reached from a Mailbox thread's "on its way,
 * tap to track" row for a letter the viewer sent. Full-screen map view with
 * floating translucent header (including Boost CTA) and draggable collapsible
 * skeuomorphic widget.
 */
@Composable
fun DeliveryTrackingScreen(
    onBackClick: () -> Unit,
    onBuyEnergyClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryTrackingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val unlockState by viewModel.unlockState.collectAsState()
    var isCardExpanded by rememberSaveable { mutableStateOf(true) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showBoostConfirm by remember { mutableStateOf(false) }
    var showCelebrationModal by remember { mutableStateOf(false) }
    var hasTriggeredAutoCelebration by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(unlockState) {
        when (val state = unlockState) {
            is UnlockLetterState.Success -> {
                showBoostConfirm = false
                showCelebrationModal = true
                viewModel.resetUnlockState()
            }
            is UnlockLetterState.InsufficientEnergy -> {
                // Jemput bola: instead of a dead-end error toast, close the
                // confirm dialog and offer the buy-Energy dialog right away
                // (docs/copywriting/cp.md) — resetUnlockState() waits until
                // that dialog is dismissed so state.required/available stay
                // available to it below.
                showBoostConfirm = false
            }
            is UnlockLetterState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUnlockState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(uiState) {
        val success = uiState as? DeliveryTrackingUiState.Success
        if (success != null && success.isDelivered && success.isViewerRecipient && !hasTriggeredAutoCelebration) {
            hasTriggeredAutoCelebration = true
            // Gentle delay allowing user to see progress hit 100% and unlock icon pop before celebration
            delay(650)
            showCelebrationModal = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandInkDeep),
    ) {
        when (val state = uiState) {
            is DeliveryTrackingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandGold)
                }
            }
            is DeliveryTrackingUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is DeliveryTrackingUiState.Success -> {
                // Full-Screen Interactive World Map
                DeliveryRouteMap(
                    senderContinent = state.senderContinent,
                    recipientContinent = state.recipientContinent,
                    progress = state.progress,
                    transportMode = state.transportMode,
                    animateEntrance = false,
                    senderName = state.senderName,
                    recipientName = state.recipientName,
                    letterId = state.letterId,
                    modifier = Modifier.fillMaxSize(),
                )

                // Translucent Floating Header Bar (Back button + Title + Boost CTA grouped on Left)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TrackingBackButton(onClick = onBackClick)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ToolbarFrostedDark)
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.letter_tracking_title),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                        )
                    }

                    // Boost Energy / Open Letter Widget (Only available for recipient)
                    if (state.isViewerRecipient) {
                        if (!state.isDelivered) {
                            TopBoostEnergyPill(
                                energy = state.viewerEnergy,
                                isUnlocking = unlockState is UnlockLetterState.Unlocking,
                                onClick = { showBoostConfirm = true },
                            )
                        } else {
                            TopOpenLetterPill(
                                onClick = { showCelebrationModal = true },
                            )
                        }
                    }
                }

                // Draggable Floating Tracking Widget (Collapsible Skeuomorphic Badge/Card)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                ) {
                    if (isCardExpanded) {
                        // Expanded Tracking Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 440.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(ToolbarFrostedDark)
                                .border(1.dp, BrandGold.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Drag Handle Pill Indicator
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.35f)),
                                    )
                                }

                                val animatedProgress by animateFloatAsState(
                                    targetValue = state.progress,
                                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                                    label = "trackingProgress",
                                )

                                // Transport Badge + Route Info + Minimize Icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BrandGold.copy(alpha = if (state.isDelivered) 0.35f else 0.18f))
                                            .border(1.5.dp, BrandGold.copy(alpha = if (state.isDelivered) 0.9f else 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (state.isDelivered) {
                                                Icons.Filled.LockOpen
                                            } else {
                                                when (state.transportMode) {
                                                    TransportMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                                    TransportMode.SWIM -> Icons.Filled.Pool
                                                }
                                            },
                                            contentDescription = null,
                                            tint = BrandGold,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(
                                                R.string.letter_tracking_route_label,
                                                stringResource(state.senderContinent.labelRes),
                                                stringResource(state.recipientContinent.labelRes),
                                            ),
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = if (state.isDelivered) {
                                                stringResource(R.string.letter_tracking_delivered_label)
                                            } else {
                                                stringResource(flavorTextFor(state.transportMode, state.progress))
                                            },
                                            fontFamily = NunitoFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable { isCardExpanded = false },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Minimize card",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = BrandGold,
                                    trackColor = Color.White.copy(alpha = 0.16f),
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(R.string.letter_tracking_sent_label, state.sentAtLabel),
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(R.string.letter_tracking_eta_label, state.estimatedArrivalAtLabel),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    } else {
                        // Minimized Tracking Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ToolbarFrostedDark)
                                .border(1.5.dp, BrandGold.copy(alpha = 0.8f), RoundedCornerShape(50))
                                .clickable { isCardExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = when (state.transportMode) {
                                        TransportMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                        TransportMode.SWIM -> Icons.Filled.Pool
                                    },
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = stringResource(
                                    R.string.letter_tracking_route_label,
                                    stringResource(state.senderContinent.labelRes),
                                    stringResource(state.recipientContinent.labelRes),
                                ),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                            )

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Expand card",
                                    tint = BrandInkDeep,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBoostConfirm) {
        LetterlyCenterDialog(
            title = stringResource(R.string.letter_tracking_boost_confirm_title),
            body = stringResource(R.string.letter_tracking_boost_confirm_body),
            primaryCtaText = stringResource(R.string.letter_tracking_boost_confirm_cta),
            onPrimaryClick = {
                viewModel.unlockLetter()
                showBoostConfirm = false
            },
            secondaryCtaText = stringResource(R.string.letter_tracking_boost_confirm_cancel),
            onSecondaryClick = { showBoostConfirm = false },
            onDismissRequest = { showBoostConfirm = false },
        )
    }

    val insufficientEnergyState = unlockState as? UnlockLetterState.InsufficientEnergy
    if (insufficientEnergyState != null) {
        LetterlyCenterDialog(
            title = stringResource(R.string.letter_tracking_boost_insufficient_title),
            body = stringResource(
                R.string.error_letters_unlock_insufficient_energy,
                insufficientEnergyState.required,
                insufficientEnergyState.available,
            ),
            primaryCtaText = stringResource(R.string.letter_tracking_boost_insufficient_cta),
            onPrimaryClick = {
                viewModel.resetUnlockState()
                onBuyEnergyClick()
            },
            secondaryCtaText = stringResource(R.string.letter_tracking_boost_confirm_cancel),
            onSecondaryClick = viewModel::resetUnlockState,
            onDismissRequest = viewModel::resetUnlockState,
        )
    }

    if (showCelebrationModal) {
        val successState = uiState as? DeliveryTrackingUiState.Success
        DeliveredCelebrationModal(
            senderName = successState?.senderName.orEmpty(),
            envelopeDesignName = successState?.envelope,
            envelopeCompositeImageUrl = successState?.envelopeCompositeImageUrl,
            onOpenLetterClick = {
                showCelebrationModal = false
                onBackClick()
            },
            onDismiss = {
                showCelebrationModal = false
                onBackClick()
            },
        )
    }
}

@Composable
private fun TopBoostEnergyPill(
    energy: Int?,
    isUnlocking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isUnlocking,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isUnlocking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = BrandGoldDeep,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_fish),
                    contentDescription = stringResource(R.string.letter_tracking_boost_confirm_title),
                    tint = BrandGoldDeep,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (energy != null) {
                Text(
                    text = "${rememberAnimatedCount(energy)}",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = BrandInkDeep,
                )
            }
        }
    }
}

@Composable
private fun TopOpenLetterPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            .background(BrandGold)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Mail,
            contentDescription = null,
            tint = BrandInkDeep,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.letter_tracking_celebration_cta_open),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = BrandInkDeep,
        )
    }
}

@Composable
private fun TrackingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .size(40.dp)
            .clip(CircleShape)
            .background(ToolbarFrostedDark)
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.open_letter_back_desc),
            tint = Color.White,
        )
    }
}
