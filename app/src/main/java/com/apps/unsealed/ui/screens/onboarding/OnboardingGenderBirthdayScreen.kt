package com.apps.unsealed.ui.screens.onboarding

import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.ui.components.LetterlyButton
import java.util.Calendar

/** Play Store age-gate minimum — must stay in sync with the IARC rating
 * questionnaire answer in Play Console. */
private const val MinAgeYears = 13

/** Whole-years age as of today from a "YYYY-MM-DD" birthday string. */
private fun ageFromBirthday(birthday: String): Int? = runCatching {
    val (year, month, day) = birthday.split("-").map { it.toInt() }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - year
    val birthdayNotYetThisYear = (today.get(Calendar.MONTH) + 1 < month) ||
        (today.get(Calendar.MONTH) + 1 == month && today.get(Calendar.DAY_OF_MONTH) < day)
    if (birthdayNotYetThisYear) age -= 1
    age
}.getOrNull()

/**
 * Onboarding step 3 — gender + birthday.
 * Calls PATCH /api/v1/auth/me on submit; on success navigates to language/bio step.
 *
 * Unlike the other onboarding steps, this one has **no Skip** — it's the
 * Play Store age gate (`MinAgeYears`), so it can't be bypassed without
 * defeating the point of the check.
 */
@Composable
fun OnboardingGenderBirthdayScreen(
    onNext: () -> Unit,
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var selectedGender by remember { mutableStateOf<String?>(null) } // "m" | "f"
    var selectedBirthday by remember { mutableStateOf<String?>(null) } // "YYYY-MM-DD"
    val age = selectedBirthday?.let(::ageFromBirthday)
    val isUnderMinAge = age != null && age < MinAgeYears

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedBirthday = "%04d-%02d-%02d".format(year, month + 1, day)
            },
            calendar.get(Calendar.YEAR) - MinAgeYears,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).also { dialog ->
            dialog.datePicker.maxDate = System.currentTimeMillis()
        }
    }

    OnboardingScaffold(
        step = 3,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_gender_birthday_title),
        subtitle = stringResource(R.string.onboarding_gender_birthday_subtitle),
    ) {
        // ── Gender ─────────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.onboarding_gender_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "m" to stringResource(R.string.onboarding_gender_male),
                "f" to stringResource(R.string.onboarding_gender_female),
            ).forEach { (value, label) ->
                FilterChip(
                    selected = selectedGender == value,
                    onClick = { selectedGender = value },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Birthday ───────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.onboarding_birthday_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = { datePickerDialog.show() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (selectedBirthday != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = selectedBirthday ?: stringResource(R.string.onboarding_birthday_placeholder),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (isUnderMinAge) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_birthday_age_gate_error, MinAgeYears),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        LetterlyButton(
            text = stringResource(R.string.onboarding_gender_birthday_cta),
            onClick = {
                focusManager.clearFocus()
                viewModel.patchMe(
                    PatchMeRequest(
                        gender = selectedGender,
                        birthday = selectedBirthday,
                    )
                ) { onNext() }
            },
            enabled = !isLoading && selectedBirthday != null && !isUnderMinAge,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
