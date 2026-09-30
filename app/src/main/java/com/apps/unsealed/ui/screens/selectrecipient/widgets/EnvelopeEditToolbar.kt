package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.compose.widgets.PillIconToggleButton
import com.apps.unsealed.ui.theme.ToolbarFrostedDark

/**
 * Envelope edit toolbar — always visible below the [EnvelopeCard] (no
 * competing keyboard/annotate mode to hide behind here, unlike
 * ComposeScreen). Mirrors [com.apps.unsealed.ui.screens.compose.widgets.AnnotateToolbar]'s
 * flat-row/SpaceEvenly/[PillIconToggleButton] pattern.
 *
 * Text and Put Sticker are dummy stubs for this pass (route to
 * `ComingSoonBottomSheet`, same as unbuilt ComposeScreen features). Envelope
 * and Stamp are both functional — Stamp's picker is the *same* mechanism
 * that gates "Ready to Send" (see [com.apps.unsealed.ui.screens.selectrecipient.state.SelectRecipientUiState.isReadyToSend]),
 * so [isStampSelected] doubles as a persistent "done" indicator via the
 * shared pill-highlight selected state.
 */
@Composable
fun EnvelopeEditToolbar(
    isStampSelected: Boolean,
    onTextClick: () -> Unit,
    onEnvelopeClick: () -> Unit,
    onStickerClick: () -> Unit,
    onStampClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ToolbarFrostedDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIconToggleButton(
            icon = Icons.Filled.TextFields,
            contentDescription = stringResource(R.string.envelope_edit_text_desc),
            isSelected = false,
            onClick = onTextClick,
        )
        PillIconToggleButton(
            icon = Icons.Filled.MailOutline,
            contentDescription = stringResource(R.string.envelope_edit_envelope_desc),
            isSelected = false,
            onClick = onEnvelopeClick,
        )
        PillIconToggleButton(
            icon = Icons.Filled.EmojiEmotions,
            contentDescription = stringResource(R.string.envelope_edit_sticker_desc),
            isSelected = false,
            onClick = onStickerClick,
        )
        PillIconToggleButton(
            icon = Icons.Filled.LocalPostOffice,
            contentDescription = stringResource(R.string.envelope_edit_stamp_desc),
            isSelected = isStampSelected,
            onClick = onStampClick,
        )
    }
}
