package com.apps.unsealed.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.LetterlyButton

/**
 * Onboarding step 2 — combined runtime permission primer for notifications
 * (letter-arrival pushes) and storage (saving/sharing letter images), see
 * docs/copywriting/cp.md §A "Push Permission" for the base copy this
 * screen's title/CTA are drawn from.
 *
 * Both permissions are optional — declining either one just means the
 * corresponding feature (push notifications / saving to gallery on API <29)
 * silently degrades later rather than blocking onboarding, so this step
 * always has a Skip.
 */
@Composable
fun OnboardingPermissionsScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    // WRITE_EXTERNAL_STORAGE is only meaningful pre-scoped-storage (API <29,
    // see ShareLetterActions.saveImageToGallery); on 29+ it's a no-op to
    // request it, so we skip straight through instead of showing a second
    // system dialog the user can't do anything useful with.
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onNext() }

    // POST_NOTIFICATIONS only exists as a runtime permission from API 33+;
    // below that, notifications are granted by default and there's nothing
    // to request.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            onNext()
        }
    }

    OnboardingScaffold(
        step = 2,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_permissions_title),
        subtitle = stringResource(R.string.onboarding_permissions_subtitle),
        onSkip = onSkip,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        PermissionRow(
            emoji = "🔔",
            title = stringResource(R.string.onboarding_permissions_notification_title),
            body = stringResource(R.string.onboarding_permissions_notification_body),
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionRow(
            emoji = "🖼️",
            title = stringResource(R.string.onboarding_permissions_storage_title),
            body = stringResource(R.string.onboarding_permissions_storage_body),
        )

        Spacer(modifier = Modifier.weight(1f))

        LetterlyButton(
            text = stringResource(R.string.onboarding_permissions_cta),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionRow(emoji: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
