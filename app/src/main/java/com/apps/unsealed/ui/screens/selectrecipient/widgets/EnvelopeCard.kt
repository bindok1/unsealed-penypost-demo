package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.selectrecipient.state.RecipientMode
import androidx.compose.foundation.shape.CircleShape
import com.apps.unsealed.ui.screens.selectrecipient.state.SelectRecipientUiState
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault

private val StampPlaceholderSize = DpSize(56.dp, 64.dp)
private val StampLandscapePlaceholderSize = DpSize(64.dp, 48.dp)
private val StickerSlotSize = 44.dp

/** Dotted-outline rect, matching a real envelope's "place stamp here"
 * guide (stamps.png reference) more closely than a solid border would. */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, strokeWidth: Dp = 1.dp) = drawBehind {
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

/**
 * The envelope itself, styled as a real rectangular container using one of
 * [EnvelopeDesign]'s background images (motion-rules.md §3.2 — "amplop
 * terasa seperti bisa diangkat" — this is the one physically-weighty
 * entrance on the screen, via [LetterlySpring.Envelope]).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EnvelopeCard(
    uiState: SelectRecipientUiState,
    senderName: String,
    senderCountry: String,
    onShareContactClick: () -> Unit,
    onPenpalsClick: () -> Unit,
    onEditRecipientClick: () -> Unit,
    onStampClick: () -> Unit = {},
    onStickerClick: () -> Unit = {},
    onEnvelopeClick: () -> Unit = {},
    showStickerPlaceholder: Boolean = true,
    sharedTransitionScope: SharedTransitionScope,
    stampAnimatedVisibilityScope: AnimatedVisibilityScope?,
    stickerAnimatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
        label = "envelopeCardScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "envelopeCardAlpha",
    )

    val envelopeModel: Any = if (uiState.selectedEnvelope.imageUrl.isNotBlank()) {
        uiState.selectedEnvelope.imageUrl
    } else {
        EnvelopeDesign.fromApiString(uiState.selectedEnvelope.id).drawableRes
    }

    val envelopeFontFamily = uiState.selectedFont.fontFamily

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .fillMaxWidth()
            .aspectRatio(EnvelopeAspectRatio)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AsyncImage(
            model = envelopeModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEnvelopeClick,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            EnvelopeHeaderRow(
                uiState = uiState,
                senderName = senderName,
                senderCountry = senderCountry,
                fontFamily = envelopeFontFamily,
                onStampClick = onStampClick,
                onEnvelopeClick = onEnvelopeClick,
                sharedTransitionScope = sharedTransitionScope,
                stampAnimatedVisibilityScope = stampAnimatedVisibilityScope,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.recipientMode == RecipientMode.KNOWN_USER && uiState.recipientName != null) {
                    if (uiState.isReplyFlow) {
                        KnownRecipientLabel(name = uiState.recipientName, fontFamily = envelopeFontFamily)
                    } else {
                        RecipientContactAddressLabel(
                            name = uiState.recipientName,
                            location = uiState.recipientContinent,
                            fontFamily = envelopeFontFamily,
                            onClick = onEditRecipientClick,
                        )
                    }
                } else if (uiState.hasRecipient && uiState.selectedRegion != null) {
                    RecipientAddressLabel(
                        region = uiState.selectedRegion,
                        fontFamily = envelopeFontFamily,
                        onClick = onEditRecipientClick,
                    )
                } else {
                    AddRecipientButton(
                        onShareContactClick = onShareContactClick,
                        onPenpalsClick = onPenpalsClick,
                    )
                }
            }
        }

        if (uiState.selectedSticker != null || showStickerPlaceholder) {
            with(sharedTransitionScope) {
                val stickerInteractionSource = remember { MutableInteractionSource() }
                val stickerPressScale = rememberPressScale(stickerInteractionSource)

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(StickerSlotSize)
                        .graphicsLayer { scaleX = stickerPressScale; scaleY = stickerPressScale }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = stickerInteractionSource,
                            indication = null,
                            onClick = onStickerClick,
                        )
                        .then(
                            if (uiState.selectedSticker == null && showStickerPlaceholder) {
                                Modifier.dashedBorder(color = InkDefault.copy(alpha = 0.35f), cornerRadius = 22.dp)
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (stickerAnimatedVisibilityScope != null) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = StickerSheetSharedKey),
                                    animatedVisibilityScope = stickerAnimatedVisibilityScope,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.selectedSticker != null) {
                        EnvelopeStickerBadge(item = uiState.selectedSticker, modifier = Modifier.fillMaxSize())
                    } else if (showStickerPlaceholder) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Add Sticker",
                            tint = InkDefault.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Top edge of the envelope, styled like a real one: return-address corner
 * (sender's own name/country, from [AuthUiState.Authenticated.user]) on the
 * left, postage-stamp corner on the right. This row is captured as part of
 * the envelope composite sent to the backend (`docs/penpals-screen-spec.md`
 * §Context), so it must reflect the real sender, not a placeholder. The
 * actual recipient address lives centered in the envelope body, see
 * [RecipientAddressLabel]/[AddRecipientButton]. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EnvelopeHeaderRow(
    uiState: SelectRecipientUiState,
    senderName: String,
    senderCountry: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onStampClick: () -> Unit,
    onEnvelopeClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    stampAnimatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        val senderInteraction = remember { MutableInteractionSource() }
        val senderScale = rememberPressScale(senderInteraction)

        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = senderScale; scaleY = senderScale }
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = senderInteraction,
                    indication = null,
                    onClick = onEnvelopeClick,
                ),
        ) {
            Text(
                senderName,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = fontFamily,
                fontSize = 18.sp,
                color = InkDefault,
            )
            Text(
                senderCountry,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = fontFamily,
                fontSize = 14.sp,
                color = InkDefault.copy(alpha = 0.6f),
            )
        }

        with(sharedTransitionScope) {
            val stampInteraction = remember { MutableInteractionSource() }
            val stampScale = rememberPressScale(stampInteraction)

            val isLandscape = uiState.selectedStamp?.isLandscape == true
            val stampSize = if (isLandscape) StampLandscapePlaceholderSize else StampPlaceholderSize

            Box(
                modifier = Modifier
                    .size(stampSize.width, stampSize.height)
                    .graphicsLayer { scaleX = stampScale; scaleY = stampScale }
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = stampInteraction,
                        indication = null,
                        onClick = onStampClick,
                    )
                    .then(
                        if (stampAnimatedVisibilityScope != null) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = StampSheetSharedKey),
                                animatedVisibilityScope = stampAnimatedVisibilityScope,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .dashedBorder(
                        color = InkDefault.copy(alpha = if (uiState.selectedStamp != null) 0.15f else 0.4f),
                        cornerRadius = 4.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.selectedStamp != null) {
                    StampArrival(item = uiState.selectedStamp, modifier = Modifier.fillMaxSize().padding(3.dp))
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Select Stamp",
                            tint = InkDefault.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Stamp",
                            fontFamily = CaveatFontFamily,
                            fontSize = 13.sp,
                            color = InkDefault.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

/** The recipient's "address", centered in the envelope body — the actual
 * destination, once picked. Tapping it reopens the picker to edit. */
@Composable
private fun RecipientAddressLabel(
    region: PenpalRegion,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Text(
        text = stringResource(R.string.select_recipient_penpal_in_region, stringResource(region.labelRes)),
        style = MaterialTheme.typography.titleMedium,
        fontFamily = fontFamily,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        color = InkDefault,
        textAlign = TextAlign.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Reply-flow recipient display — the recipient is fixed to whoever sent the
 * letter being replied to ([RecipientMode.KNOWN_USER]), so unlike
 * [RecipientAddressLabel] this is plain non-interactive text: no click target
 * back into the region picker, since re-targeting a reply mid-flow would
 * silently turn it into a send to an unrelated random penpal. */
@Composable
private fun KnownRecipientLabel(
    name: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.select_recipient_known_recipient, name),
        style = MaterialTheme.typography.titleMedium,
        fontFamily = fontFamily,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        color = InkDefault,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Contact-picked recipient display — interactive address label that can be tapped to change contact. */
@Composable
private fun RecipientContactAddressLabel(
    name: String,
    location: String?,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.select_recipient_to_recipient, name),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = fontFamily,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = InkDefault,
            textAlign = TextAlign.Center,
        )
        if (!location.isNullOrBlank()) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = fontFamily,
                fontSize = 15.sp,
                color = InkDefault.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** motion-rules.md §3.5 — stamp "lands" on the envelope with the most
 * dramatic bounce on screen: drops in from above + scales up from small.
 * Plays right after the picker's own container-transform shrink-back
 * finishes (see [StampSheetSharedKey]), on top of that motion rather than
 * instead of it. Keyed on [item]'s id so re-picking a different stamp
 * replays the arrival. */
@Composable
private fun StampArrival(item: CatalogItemDto, modifier: Modifier = Modifier) {
    val isReducedMotion = rememberIsReducedMotion()
    val progress = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
    }
    StampImage(
        item = item,
        modifier = modifier.graphicsLayer {
            scaleX = 0.4f + 0.6f * progress.value
            scaleY = 0.4f + 0.6f * progress.value
            translationY = (1f - progress.value) * -60.dp.toPx()
            alpha = progress.value
        },
    )
}

/** Sticker's own bottom-end slot arrival — same bounce shape as [StampArrival]
 * (`docs/envelope-sticker-spec.md` §5), just a different size/position, and
 * rendered via [StampImage] since it's generic over any [CatalogItemDto]
 * (its blank-`imageUrl` fallback never triggers here — stickers are 100%
 * CMS-driven, no bundled fallback catalog). */
@Composable
private fun EnvelopeStickerBadge(item: CatalogItemDto, modifier: Modifier = Modifier) {
    val isReducedMotion = rememberIsReducedMotion()
    val progress = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
    }
    StampImage(
        item = item,
        modifier = modifier.graphicsLayer {
            scaleX = 0.4f + 0.6f * progress.value
            scaleY = 0.4f + 0.6f * progress.value
            translationY = (1f - progress.value) * -60.dp.toPx()
            alpha = progress.value
        },
    )
}

/** Anchored dropdown (not a `ModalBottomSheet`) — mirrors
 * [com.apps.unsealed.ui.screens.compose.widgets.OverflowMenuButton]'s pattern
 * exactly, since this is the same shape of problem: a small fixed 2-item
 * action menu triggered from a button, not a rich scrollable choice. */
@Composable
private fun AddRecipientButton(
    onShareContactClick: () -> Unit,
    onPenpalsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val reduced = rememberIsReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    LaunchedEffect(showMenu) {
        if (showMenu) isMenuVisible = true
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { showMenu = true },
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = InkDefault, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.select_recipient_add_recipient),
                style = MaterialTheme.typography.labelLarge,
                color = InkDefault,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (showMenu) {
            Popup(
                alignment = Alignment.BottomStart,
                offset = with(density) { IntOffset(0, -8.dp.roundToPx()) },
                onDismissRequest = { isMenuVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                val menuScale by animateFloatAsState(
                    targetValue = if (isMenuVisible) 1f else 0.85f,
                    animationSpec = reducedMotionSpring(
                        if (isMenuVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
                        reduced,
                    ),
                    label = "addRecipientMenuScale",
                )
                val menuAlpha by animateFloatAsState(
                    targetValue = if (isMenuVisible) 1f else 0f,
                    animationSpec = tween(if (isMenuVisible) 380 else 250),
                    label = "addRecipientMenuAlpha",
                    finishedListener = { finalValue -> if (finalValue <= 0f) showMenu = false },
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2A241C),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .width(240.dp)
                        .graphicsLayer {
                            scaleX = menuScale
                            scaleY = menuScale
                            alpha = menuAlpha
                            transformOrigin = TransformOrigin(0f, 1f)
                        },
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        AddRecipientMenuItem(
                            icon = Icons.Filled.Contacts,
                            label = stringResource(R.string.add_recipient_share_contact),
                            onClick = { isMenuVisible = false; onShareContactClick() },
                        )
                        AddRecipientMenuItem(
                            icon = Icons.Filled.Public,
                            label = stringResource(R.string.add_recipient_penpals),
                            onClick = { isMenuVisible = false; onPenpalsClick() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRecipientMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 14.dp))
    }
}
