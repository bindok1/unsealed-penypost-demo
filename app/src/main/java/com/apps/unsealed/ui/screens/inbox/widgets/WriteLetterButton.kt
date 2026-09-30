package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/** Icon+text pill, same `ToolbarFrostedDark` stadium/border/shadow pattern as
 * `NextButton` (`SelectRecipientScreen.kt`) — reply CTA shown once at the
 * bottom of [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]'s
 * thread list. [hasDraft] swaps the label to "Continue Writing" when a local
 * draft already exists for this correspondent (Draft-on-Exit) — [onClick]'s
 * behavior (fresh letter vs. resuming that draft) is decided by the caller. */
@Composable
fun WriteLetterButton(onClick: () -> Unit, modifier: Modifier = Modifier, hasDraft: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .height(60.dp)
            .clip(shape)
            .background(ToolbarFrostedDark)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Create,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(
                if (hasDraft) R.string.open_letter_continue_writing else R.string.open_letter_write_letter,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
