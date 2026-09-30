package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.compose.widgets.SendButton
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay

/** Back + animated title/subtitle + trailing action button, all in one
 * header block. The subtitle used to live as a static [Footer] pinned to the
 * bottom of the screen — moved here, right under the title, since that's
 * where it's actually read from ("select recipient" -> what do I do next?). */
@Composable
fun SelectRecipientHeader(
    hasRecipient: Boolean,
    isReadyToSend: Boolean,
    canSend: Boolean,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.select_recipient_back_desc),
                tint = Color.White,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onBackClick)
                    .padding(8.dp),
            )
            TypewriterTitle(
                isReadyToSend = isReadyToSend,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Crossfade(targetState = canSend, animationSpec = tween(200), label = "headerAction") { ready ->
                if (ready) {
                    SendButton(onClick = onSendClick, highlighted = true)
                } else {
                    NextButton(enabled = hasRecipient, onClick = onNextClick)
                }
            }
        }
        HeaderSubtitle(canSend = canSend, modifier = Modifier.padding(start = 48.dp, top = 2.dp))
    }
}

/** motion-rules.md §3.6-adjacent "stagger"/reveal feel: types the title out
 * character-by-character on the "Select Recipient" -> "Ready to Send"
 * transition instead of a flat crossfade, so the moment the letter is ready
 * reads as deliberate, not just a state flip. */
@Composable
private fun TypewriterTitle(isReadyToSend: Boolean, modifier: Modifier = Modifier) {
    val isReducedMotion = rememberIsReducedMotion()
    val selectingText = stringResource(R.string.select_recipient_title_selecting)
    val readyText = stringResource(R.string.select_recipient_title_ready)
    var displayText by remember { mutableStateOf(if (isReadyToSend) readyText else selectingText) }

    LaunchedEffect(isReadyToSend) {
        if (isReadyToSend) {
            if (isReducedMotion) {
                displayText = readyText
            } else {
                for (charCount in 0..readyText.length) {
                    displayText = readyText.take(charCount)
                    delay(28L)
                }
            }
        } else {
            displayText = selectingText
        }
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier,
    )
}

@Composable
private fun HeaderSubtitle(canSend: Boolean, modifier: Modifier = Modifier) {
    Crossfade(targetState = canSend, animationSpec = tween(200), label = "headerSubtitle", modifier = modifier) { ready ->
        Text(
            text = stringResource(
                if (ready) R.string.select_recipient_send_hint else R.string.select_recipient_hint_add_recipient,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

/** Header's action button before everything's ready — becomes the
 * highlighted [SendButton] once [canSend] flips true (see [SelectRecipientHeader]). Tapping
 * it while a recipient is set but no stamp is picked yet opens the same
 * [StampPickerOverlay] the toolbar's Stamp icon does. */
@Composable
private fun NextButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.4f, label = "nextButtonAlpha")
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale; this.alpha = alpha }
            .height(48.dp)
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(ToolbarFrostedDark)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.select_recipient_next),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
