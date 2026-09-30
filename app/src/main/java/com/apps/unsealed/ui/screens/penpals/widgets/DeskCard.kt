package com.apps.unsealed.ui.screens.penpals.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardFocusedElevation
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardFocusedScale
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardLiftedElevation
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardLiftedScale
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardRestElevation
import com.apps.unsealed.ui.screens.penpals.constants.DeskCardRestScale
import com.apps.unsealed.ui.screens.penpals.constants.DeskDimmedAlpha
import com.apps.unsealed.ui.screens.penpals.constants.DeskFocusedCardZIndex
import com.apps.unsealed.ui.screens.penpals.state.DeskCardState
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.selectrecipient.widgets.StampImage
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One postcard scattered freely on the desk.
 */
@Composable
fun DeskCard(
    letter: PenpalLetter,
    index: Int,
    state: DeskCardState,
    cardWidth: Dp,
    cardWidthPx: Float,
    cardHeightPx: Float,
    deskWidthPx: Float,
    deskHeightPx: Float,
    scatterTopPx: Float,
    isFocused: Boolean,
    isAnyFocused: Boolean,
    onDragStart: () -> Unit,
    onRequestFocus: () -> Unit,
    onRequestOpen: () -> Unit,
    stickerCatalog: List<CatalogItemDto>,
    stampCatalog: List<CatalogItemDto>,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val cardHeight = with(density) { cardHeightPx.toDp() }

    // ── Stagger entrance ──────────────────────────────────────────────────────
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        isVisible = true
    }
    val entranceOffsetYPx by animateFloatAsState(
        targetValue = if (isVisible) 0f else with(density) { 56.dp.toPx() },
        animationSpec = reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
        label = "entranceY",
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
        label = "entranceAlpha",
    )

    // ── Lift-off-the-desk feedback while dragging ─────────────────────────────
    var isDragging by remember { mutableStateOf(false) }
    val liftScale by animateFloatAsState(
        targetValue = when {
            isFocused -> DeskCardFocusedScale
            isDragging -> DeskCardLiftedScale
            else -> DeskCardRestScale
        },
        animationSpec = reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        label = "liftScale",
    )
    val elevationPx by animateFloatAsState(
        targetValue = with(density) {
            when {
                isFocused -> DeskCardFocusedElevation
                isDragging -> DeskCardLiftedElevation
                else -> DeskCardRestElevation
            }.toPx()
        },
        animationSpec = reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        label = "elevation",
    )

    // ── Focus mode: slide/grow to screen center, flatten rotation ─────────────
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        label = "focusProgress",
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (isAnyFocused && !isFocused) DeskDimmedAlpha else 1f,
        animationSpec = reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
        label = "dimAlpha",
    )
    val focusCenterX = (deskWidthPx - cardWidthPx) / 2f
    val focusCenterY = (deskHeightPx - cardHeightPx) / 2f

    val canInteract = !isAnyFocused || isFocused
    val interactionSource = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                translationX = lerp(state.offsetX.value, focusCenterX, focusProgress)
                translationY = lerp(state.offsetY.value, focusCenterY, focusProgress) + entranceOffsetYPx
                rotationZ = lerp(state.rotationZ, 0f, focusProgress)
                scaleX = liftScale
                scaleY = liftScale
                alpha = entranceAlpha * dimAlpha
                shadowElevation = elevationPx
                shape = cardShape
                clip = true
            }
            .zIndex(if (isFocused) DeskFocusedCardZIndex else state.zIndex.floatValue)
            .then(
                if (!isAnyFocused) {
                    Modifier.pointerInput(letter.id) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                onDragStart()
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = (state.offsetX.value + dragAmount.x)
                                    .coerceIn(0f, (deskWidthPx - cardWidthPx).coerceAtLeast(0f))
                                val newY = (state.offsetY.value + dragAmount.y)
                                    .coerceIn(scatterTopPx, (deskHeightPx - cardHeightPx).coerceAtLeast(scatterTopPx))
                                scope.launch { state.offsetX.snapTo(newX) }
                                scope.launch { state.offsetY.snapTo(newY) }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (canInteract) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (isFocused) onRequestOpen() else onRequestFocus() },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        // envelopeCompositeImageUrl (background + sender name/region text +
        // stamp + sticker pre-baked, see docs/penpals-screen-spec.md §Context)
        // is preferred when present; otherwise reconstruct an identical-looking
        // result client-side from the structured fields that are always sent
        // regardless of compositing support.
        if (letter.envelopeCompositeImageUrl != null) {
            AsyncImage(
                model = letter.envelopeCompositeImageUrl,
                contentDescription = stringResource(
                    R.string.penpals_card_region_label,
                    stringResource(letter.region.labelRes),
                ),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(letter.envelope.drawableRes),
                error = painterResource(letter.envelope.drawableRes),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = letter.envelopeImageUrl,
                contentDescription = stringResource(
                    R.string.penpals_card_region_label,
                    stringResource(letter.region.labelRes),
                ),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(letter.envelope.drawableRes),
                error = painterResource(letter.envelope.drawableRes),
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = letter.senderName,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = CaveatFontFamily,
                fontSize = 16.sp,
                color = InkDefault,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
            Text(
                text = stringResource(R.string.penpals_card_region_label, stringResource(letter.region.labelRes)),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = CaveatFontFamily,
                fontSize = 18.sp,
                color = InkDefault,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
            )
            letter.stampId?.let { id ->
                stampCatalog.find { it.id == id }?.let { stamp ->
                    val isLandscape = stamp.isLandscape
                    StampImage(
                        item = stamp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(
                                width = if (isLandscape) 36.dp else 32.dp,
                                height = if (isLandscape) 28.dp else 36.dp,
                            ),
                    )
                }
            }
            letter.envelopeStickerId?.let { id ->
                stickerCatalog.find { it.id == id }?.let { sticker ->
                    StampImage(
                        item = sticker,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp),
                    )
                }
            }
        }
    }
}
