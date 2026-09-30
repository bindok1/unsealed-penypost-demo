package com.apps.unsealed.ui.screens.penpals.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.components.LetterlyOutlinedButton

/**
 * Confirms spending energy to reroll today's stack early
 * (`POST /penpals/feed/refresh`, `docs/be/daily_penpal_stack.md` §6) instead
 * of waiting for tomorrow's stack. Auto-dismisses once a confirmed refresh
 * finishes with no error — [onConfirm] fires the request, [isRefreshing] /
 * [errorMessage] reflect [com.apps.unsealed.ui.screens.penpals.viewmodel.PenPalsViewModel]'s state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeniRefreshFeedBottomSheet(
    energyCost: Int,
    energyBalance: Int,
    isRefreshing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasConfirmed by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing, errorMessage) {
        if (hasConfirmed && !isRefreshing && errorMessage == null) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.penpals_refresh_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.penpals_refresh_sheet_body, energyCost, energyBalance),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            LetterlyButton(
                text = stringResource(R.string.penpals_refresh_sheet_cta_confirm, energyCost),
                onClick = {
                    hasConfirmed = true
                    onConfirm()
                },
                enabled = !isRefreshing && energyBalance >= energyCost,
                isLoading = isRefreshing,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LetterlyOutlinedButton(
                text = stringResource(R.string.penpals_refresh_sheet_cta_dismiss),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.6f),
            )
        }
    }
}
