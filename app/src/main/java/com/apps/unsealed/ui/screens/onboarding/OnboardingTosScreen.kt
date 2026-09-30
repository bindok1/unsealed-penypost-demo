package com.apps.unsealed.ui.screens.onboarding

import android.graphics.Typeface
import android.text.Spanned
import android.text.SpannedString
import android.text.style.StyleSpan
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LegalLinks
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.auth.data.PatchMeRequest
import com.apps.unsealed.ui.components.LegalDocumentBottomSheet
import com.apps.unsealed.ui.components.LetterlyButton
import java.time.Instant

/**
 * Onboarding step 1 — Terms of Service + Privacy Policy acceptance.
 *
 * Calls `PATCH /api/v1/auth/me` with `tos_accepted_at` set to the current
 * ISO 8601 timestamp on confirm. The backend gate in `LetterService.Send`
 * and the penpal match service returns 403 until this field is set, so
 * this step **cannot be skipped** — it's a hard prerequisite for all core
 * features (sending letters, matching penpals).
 *
 * Smart routing after acceptance:
 * - **Existing users** (gender/birthday/interests already set, only TOS was
 *   missing after the migration) → [onFinish] navigates directly to main app.
 * - **New users** (still need to fill gender/birthday etc.) → [onNext] walks
 *   them through the remaining onboarding wizard steps.
 */
@Composable
fun OnboardingTosScreen(
    onNext: () -> Unit,
    onFinish: () -> Unit = {},
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var tosChecked by remember { mutableStateOf(false) }
    var legalSheetUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            val error = uiState as AuthUiState.Error
            val rawMsg = error.message.trim()
            val isTechnical = rawMsg.contains("tos_accepted_at", ignoreCase = true) ||
                rawMsg.contains("in the future", ignoreCase = true) ||
                rawMsg.startsWith("http", ignoreCase = true) ||
                rawMsg.contains("exception", ignoreCase = true) ||
                rawMsg.contains("failed to update profile", ignoreCase = true) ||
                rawMsg.contains("gagal memperbarui profil", ignoreCase = true)
            val friendlyMessage = if (rawMsg.isNotBlank() && !isTechnical) {
                rawMsg
            } else {
                context.getString(R.string.onboarding_tos_error)
            }
            snackbarHostState.showSnackbar(
                message = friendlyMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val termsUrl   = LegalLinks.TERMS_OF_SERVICE_URL
    val privacyUrl = LegalLinks.PRIVACY_POLICY_URL
    val termsTitle   = stringResource(R.string.legal_document_terms_title)
    val privacyTitle = stringResource(R.string.legal_document_privacy_title)

    // Build an AnnotatedString from the checkbox label so the <b>Terms of
    // Service</b> and <b>Privacy Policy</b> labels are bold and tappable
    // links. Read via resources.getText() (not stringResource(), which calls
    // Resources.getString() and strips all styling info per the Android
    // docs) so the <b> tags survive as real StyleSpans. The two bold spans
    // are then matched by position (1st = terms, 2nd = privacy) rather than
    // hardcoded English text, since translated labels (e.g. "Syarat
    // Layanan") won't match an indexOf on the English string.
    val checkboxText = buildAnnotatedString {
        val text    = context.resources.getText(R.string.onboarding_tos_checkbox)
        val spanned = text as? Spanned ?: SpannedString(text)
        val plain   = spanned.toString()
        append(plain)

        val boldRanges = spanned.getSpans(0, spanned.length, StyleSpan::class.java)
            .filter { it.style == Typeface.BOLD }
            .map { spanned.getSpanStart(it) to spanned.getSpanEnd(it) }
            .sortedBy { (start, _) -> start }

        boldRanges.forEachIndexed { index, (start, end) ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)

            val url = when (index) {
                0 -> termsUrl
                1 -> privacyUrl
                else -> null
            }
            if (url != null) {
                addStringAnnotation("URL", url, start, end)
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold),
                    start, end,
                )
            }
        }
    }

    OnboardingScaffold(
        step              = 1,
        totalSteps         = 7,
        title             = stringResource(R.string.onboarding_tos_title),
        subtitle          = stringResource(R.string.onboarding_tos_subtitle),
        snackbarHostState = snackbarHostState,
        // No skip — TOS acceptance is mandatory for all core features.
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // ── TOS checkbox row ───────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Checkbox(
                checked = tosChecked,
                onCheckedChange = { tosChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            ClickableText(
                text  = checkboxText,
                style = TextStyle(
                    color      = MaterialTheme.colorScheme.onBackground,
                    fontSize   = MaterialTheme.typography.bodyMedium.fontSize,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                ),
                onClick = { offset ->
                    checkboxText.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { annotation -> legalSheetUrl = annotation.item }
                        ?: run { tosChecked = !tosChecked }
                },
                modifier = Modifier.weight(1f),
            )
        }

        LetterlyButton(
            text      = stringResource(R.string.onboarding_tos_cta),
            onClick   = {
                viewModel.patchMe(
                    PatchMeRequest(tosAcceptedAt = Instant.now().toString()),
                ) { updatedUser ->
                    // Existing users who only missed TOS (gender/birthday/interests
                    // already set) go straight to the main app; new users continue
                    // through the remaining onboarding wizard steps.
                    if (updatedUser.gender != null &&
                        updatedUser.birthday != null &&
                        updatedUser.interests.isNotEmpty()
                    ) {
                        onFinish()
                    } else {
                        onNext()
                    }
                }
            },
            enabled   = !isLoading && tosChecked,
            isLoading = isLoading,
            modifier  = Modifier.fillMaxWidth(),
        )
    }

    legalSheetUrl?.let { url ->
        LegalDocumentBottomSheet(
            title     = if (url == termsUrl) termsTitle else privacyTitle,
            url       = url,
            onDismiss = { legalSheetUrl = null },
        )
    }
}
