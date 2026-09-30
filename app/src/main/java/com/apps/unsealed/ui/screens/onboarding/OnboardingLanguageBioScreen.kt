package com.apps.unsealed.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.model.getPenpalLanguageByCode
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.auth.data.LanguageProficiencyDto
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.screens.onboarding.widgets.PenpalLanguagePickerBottomSheet

private const val MAX_BIO_LENGTH = 300

/**
 * Onboarding step 4 — penpal language preference + bio.
 * Calls PATCH /api/v1/auth/me on submit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingLanguageBioScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val focusManager = LocalFocusManager.current

    var selectedLanguages by remember { mutableStateOf(setOf("id", "en")) }
    var isPickerSheetVisible by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }

    val selectorInteractionSource = remember { MutableInteractionSource() }
    val selectorPressScale = rememberPressScale(selectorInteractionSource)

    OnboardingScaffold(
        step = 4,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_language_bio_title),
        subtitle = stringResource(R.string.onboarding_language_bio_subtitle),
        onSkip = onSkip,
        scrollable = true,
    ) {
        // ── Penpal languages ──────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.onboarding_language_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Multi-select trigger card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = selectorPressScale
                    scaleY = selectorPressScale
                }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(
                    interactionSource = selectorInteractionSource,
                    indication = null,
                    onClick = { isPickerSheetVisible = true },
                )
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.onboarding_language_card_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Selected Language Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedLanguages.forEach { code ->
                        val lang = getPenpalLanguageByCode(code)
                        if (lang != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(text = lang.flag, fontSize = 14.sp)
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (selectedLanguages.size > 1) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedLanguages = selectedLanguages - code
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Bio ────────────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.onboarding_bio_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= MAX_BIO_LENGTH) bio = it },
            placeholder = { Text(stringResource(R.string.onboarding_bio_placeholder)) },
            maxLines = 5,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                Text(
                    text = stringResource(R.string.onboarding_bio_counter, bio.length, MAX_BIO_LENGTH),
                    color = if (bio.length >= MAX_BIO_LENGTH) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        Spacer(modifier = Modifier.height(28.dp))

        LetterlyButton(
            text = stringResource(R.string.onboarding_language_bio_cta),
            onClick = {
                focusManager.clearFocus()
                // This picker only collects which languages, not a per-language
                // proficiency level — default to 5 (fluent/native) for all of
                // them; a level control can be added to the picker later.
                val languages = selectedLanguages.map { code -> LanguageProficiencyDto(code = code, level = 5) }
                viewModel.putLanguages(languages) {
                    viewModel.patchMe(PatchMeRequest(bio = bio.trim().ifEmpty { null })) { onNext() }
                }
            },
            enabled = !isLoading && selectedLanguages.isNotEmpty(),
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (isPickerSheetVisible) {
        PenpalLanguagePickerBottomSheet(
            selectedLanguageCodes = selectedLanguages,
            onConfirm = { updatedLanguages ->
                selectedLanguages = updatedLanguages
            },
            onDismiss = { isPickerSheetVisible = false },
        )
    }
}
