package com.apps.unsealed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk
import kotlinx.coroutines.delay

/**
 * First true centered [Dialog] in this codebase — every other confirmation
 * surface (`ComingSoonBottomSheet`, `AuthGuardBottomSheet`) is a
 * `ModalBottomSheet`. Visual language borrowed from [AuthGuardBottomSheet]
 * (`LoginPromptBottomSheet`): mascot + bold title + gray body +
 * [LetterlyButton]/[LetterlyOutlinedButton] pair. Reused by both the
 * Draft-on-Exit dialog and the draft-list delete confirmation.
 *
 * [secondaryCtaText]/[onSecondaryClick] are optional — omit both to render
 * a single-button variant (e.g. a one-CTA "success" acknowledgement dialog).
 *
 * Motion per docs/motion-rules.md: enter/exit always combine alpha with
 * scale (never fade-only), [LetterlySpring.Gentle] on the way in,
 * [LetterlySpring.Stiff] (faster) on the way out — same rationale as the
 * Bottom Sheet & Modal rule (§3.4), adapted for a center dialog.
 */
@Composable
fun LetterlyCenterDialog(
    title: String,
    body: String,
    primaryCtaText: String,
    onPrimaryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryCtaText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    primaryContainerColor: Color? = null,
    primaryContentColor: Color? = null,
    showMascot: Boolean = true,
    isDismissable: Boolean = true,
    closeOnPrimaryClick: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(Unit) { visible = true }

    // All three exits (primary, secondary, dismiss) play the same Stiff
    // exit animation before their real callback fires, instead of the
    // dialog just vanishing the instant a button is tapped.
    fun requestClose(action: () -> Unit) {
        pendingAction = action
        visible = false
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(150)
            (pendingAction ?: onDismissRequest)()
        }
    }

    Dialog(
        onDismissRequest = {
            if (isDismissable) requestClose(onDismissRequest)
        },
        properties = DialogProperties(
            dismissOnBackPress = isDismissable,
            dismissOnClickOutside = isDismissable,
        ),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(LetterlySpring.Gentle) + scaleIn(LetterlySpring.Gentle, initialScale = 0.9f),
            exit = fadeOut(LetterlySpring.Stiff) + scaleOut(LetterlySpring.Stiff, targetScale = 0.9f),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (showMascot) {
                        Image(
                            painter = painterResource(R.drawable.mascot_splashscreen),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = body,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    LetterlyButton(
                        text = primaryCtaText,
                        onClick = {
                            if (closeOnPrimaryClick) {
                                requestClose(onPrimaryClick)
                            } else {
                                onPrimaryClick()
                            }
                        },
                        containerColor = primaryContainerColor ?: BrandGold,
                        contentColor = primaryContentColor ?: BrandInk,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (secondaryCtaText != null && onSecondaryClick != null) {
                        Spacer(Modifier.height(10.dp))
                        LetterlyOutlinedButton(
                            text = secondaryCtaText,
                            onClick = { requestClose(onSecondaryClick) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
