package com.apps.unsealed.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.ui.components.LetterlyButton

/** `notify_hour_pref` enum values sent to `PATCH /auth/me` — see
 * `docs/be/daily_penpal_stack.md` §3/§4. UI labels are localized; these
 * values are not. */
private val NotifyHourOptions = listOf(
    "morning" to R.string.onboarding_notify_hour_morning,
    "afternoon" to R.string.onboarding_notify_hour_afternoon,
    "evening" to R.string.onboarding_notify_hour_evening,
)

/**
 * Onboarding step 7 (final) — when the viewer wants their daily letter stack
 * ready / to be notified about it. Skippable — the backend already defaults
 * to `"morning"` for anyone who never sets this, and it stays editable later
 * from Profile Settings.
 */
@Composable
fun OnboardingNotifyHourPrefScreen(
    onFinish: () -> Unit,
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val focusManager = LocalFocusManager.current

    var selected by remember { mutableStateOf<String?>(null) }

    OnboardingScaffold(
        step = 7,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_notify_hour_title),
        subtitle = stringResource(R.string.onboarding_notify_hour_subtitle),
        onSkip = onFinish,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NotifyHourOptions.forEach { (value, labelRes) ->
                val label = stringResource(labelRes)
                FilterChip(
                    selected = selected == value,
                    onClick = { selected = value },
                    label = {
                        Text(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        LetterlyButton(
            text = stringResource(R.string.onboarding_notify_hour_cta),
            onClick = {
                focusManager.clearFocus()
                val choice = selected
                if (choice == null) {
                    onFinish()
                } else {
                    viewModel.patchMe(PatchMeRequest(notifyHourPref = choice)) { onFinish() }
                }
            },
            enabled = !isLoading,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
