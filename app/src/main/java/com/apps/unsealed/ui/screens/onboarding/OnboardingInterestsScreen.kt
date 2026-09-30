package com.apps.unsealed.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LetterlyButton

/**
 * Onboarding step 6 — interests picker.
 * Fetches catalog via GET /api/v1/interests and renders a multi-select chip grid.
 * Calls PUT /api/v1/auth/me/interests on submit, which also transitions
 * [AuthViewModel.uiState] to [AuthUiState.Authenticated] — the wizard still
 * has one more (skippable) step after this one, [OnboardingNotifyHourPrefScreen],
 * so [onNext] navigates there rather than to the main app.
 *
 * At least one interest is required — [AuthRepository.needsOnboarding] treats
 * an empty interest set as incomplete, so this step has no skip option.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingInterestsScreen(
    onNext: () -> Unit,
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val interests by viewModel.interests.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val selectedIds = remember { mutableStateSetOf<String>() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.loadInterests()
    }

    OnboardingScaffold(
        step = 6,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_interests_title),
        subtitle = stringResource(R.string.onboarding_interests_subtitle),
    ) {
        if (interests.isEmpty() && !isLoading) {
            Text(
                text = stringResource(R.string.onboarding_interests_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { viewModel.loadInterests() }) {
                Text(stringResource(R.string.onboarding_interests_retry))
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                interests.forEach { interest ->
                    val isSelected = interest.id in selectedIds
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedIds.remove(interest.id)
                            else selectedIds.add(interest.id)
                        },
                        // Interest names come from backend catalog — not translated here
                        label = { Text("${interest.emoji} ${interest.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        LetterlyButton(
            text = stringResource(R.string.onboarding_interests_cta_done),
            onClick = {
                focusManager.clearFocus()
                viewModel.putInterests(selectedIds.toList()) {
                    onNext()
                }
            },
            enabled = !isLoading && selectedIds.isNotEmpty(),
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
