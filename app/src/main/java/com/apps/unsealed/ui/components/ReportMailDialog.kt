package com.apps.unsealed.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.theme.BrandGold
import kotlinx.coroutines.delay

/** Full backend reason enum (`docs/be/trust_and_safety_api.md` §1). [EMPTY_MAIL] is letter-only
 * (`target_type=letter`); [UNDERAGE]/[IMPERSONATION]/[OTHER] are user-report-only
 * (`target_type=user`) — callers pass the subset relevant to their `target_type` via
 * [ReportMailDialog]'s `reasons` param. */
enum class ReportReason(val apiValue: String, @StringRes val labelRes: Int) {
    SPAM("spam", R.string.report_reason_spam),
    HARASSMENT("harassment", R.string.report_reason_harassment),
    INAPPROPRIATE_CONTENT("inappropriate_content", R.string.report_reason_inappropriate_content),
    EMPTY_MAIL("empty_mail", R.string.report_reason_empty_mail),
    UNDERAGE("underage", R.string.report_reason_underage),
    IMPERSONATION("impersonation", R.string.report_reason_impersonation),
    OTHER("other", R.string.report_reason_other),
}

/**
 * Center dialog for reporting a piece of content — reason list + optional note, Submit + Cancel.
 * Same `Dialog`/`Surface(20.dp)`/`AnimatedVisibility` motion recipe as [LetterlyCenterDialog]
 * (Gentle in, Stiff out, 150ms-delayed close so the exit animation always finishes before the
 * real callback fires) but can't reuse that composable directly since its body is a single
 * string, not a reason list + text field. Shared here (not under a screen package) because
 * `docs/be/trust_and_safety_api.md` §1 already earmarks the identical dialog for Inbox's
 * `OpenLetterScreen.kt` once that entry point is wired up too.
 */
@Composable
fun ReportMailDialog(
    reasons: List<ReportReason>,
    isSubmitting: Boolean,
    onSubmit: (reason: ReportReason, note: String?) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = R.string.report_mail_dialog_title,
    @StringRes bodyRes: Int = R.string.report_mail_dialog_body,
) {
    var visible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(Unit) { visible = true }

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

    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!isSubmitting) requestClose(onDismissRequest) }) {
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
                    Image(
                        painter = painterResource(R.drawable.mascot_splashscreen),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(titleRes),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(bodyRes),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        reasons.forEach { reason ->
                            ReasonRow(
                                reason = reason,
                                isSelected = reason == selectedReason,
                                enabled = !isSubmitting,
                                onClick = { selectedReason = reason },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.report_mail_note_label)) },
                        enabled = !isSubmitting,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(20.dp))
                    LetterlyButton(
                        text = stringResource(R.string.report_mail_cta_submit),
                        onClick = { selectedReason?.let { onSubmit(it, note.takeIf { n -> n.isNotBlank() }) } },
                        enabled = selectedReason != null && !isSubmitting,
                        isLoading = isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    LetterlyOutlinedButton(
                        text = stringResource(R.string.report_mail_cta_cancel),
                        onClick = { if (!isSubmitting) requestClose(onDismissRequest) },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** One selectable reason row — label + [Icons.Filled.Check] that scales in via
 * [LetterlySpring.Bouncy] when selected. Mirrors `RegionPickerSheet.kt`'s `RegionRow`; this app
 * never uses Material `RadioButton`. */
@Composable
private fun ReasonRow(
    reason: ReportReason,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val checkScale = remember { Animatable(if (isSelected) 1f else 0f) }
    LaunchedEffect(isSelected) {
        checkScale.animateTo(
            if (isSelected) 1f else 0f,
            reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(reason.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
            )
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                    alpha = checkScale.value
                },
            )
        }
    }
}
