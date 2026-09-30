package com.apps.unsealed.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.apps.unsealed.R

/**
 * Center dialog prompting the user to update the app.
 *
 * If [isMandatory] is true, the dialog cannot be dismissed via back button or outside tap,
 * and the "Later" option is hidden.
 */
@Composable
fun AppUpdateDialog(
    isMandatory: Boolean,
    onUpdateClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    LetterlyCenterDialog(
        title = stringResource(
            if (isMandatory) R.string.app_update_mandatory_title else R.string.app_update_optional_title
        ),
        body = stringResource(
            if (isMandatory) R.string.app_update_mandatory_body else R.string.app_update_optional_body
        ),
        primaryCtaText = stringResource(R.string.app_update_cta),
        onPrimaryClick = onUpdateClick,
        secondaryCtaText = if (!isMandatory) stringResource(R.string.app_update_later) else null,
        onSecondaryClick = if (!isMandatory) onDismissRequest else null,
        onDismissRequest = onDismissRequest,
        isDismissable = !isMandatory,
        closeOnPrimaryClick = !isMandatory,
    )
}
