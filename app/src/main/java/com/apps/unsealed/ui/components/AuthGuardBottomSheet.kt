package com.apps.unsealed.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.theme.NunitoFontFamily

/**
 * Contextual login interrupt shown when a guest taps a sensitive action
 * (open a penpal letter, view own profile, send a letter). Navigates to [com.apps.unsealed.ui.screens.auth.LoginScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPromptBottomSheet(
    onDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.mascot_splashscreen),
                contentDescription = null,
                modifier = Modifier.size(68.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.auth_guard_title),
                fontFamily = NunitoFontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.auth_guard_body),
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))

            LetterlyButton(
                text = stringResource(R.string.auth_guard_cta_sign_in),
                onClick = {
                    onDismiss()
                    onSignInClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            LetterlyOutlinedButton(
                text = stringResource(R.string.auth_guard_cta_dismiss),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** State + actions returned by [rememberAuthGuard]. */
data class AuthGuardState(
    val isPromptVisible: Boolean,
    val isLoading: Boolean,
    val errorMessage: String?,
    /** Runs [action] immediately if already logged in, otherwise shows the login prompt and
     * defers [action] until sign-in succeeds. */
    val guard: (action: () -> Unit) -> Unit,
    val dismiss: () -> Unit,
)

/**
 * Hook backing the "tap a guest-mode action → show login prompt → resume the
 * action on success" flow used across [com.apps.unsealed.ui.screens.penpals.screen.PenPalsScreen],
 * the Profile tab, and the Write/Send flow.
 */
@Composable
fun rememberAuthGuard(
    viewModel: AuthViewModel,
    onNeedsRegisterOrOnboarding: (AuthUiState) -> Unit = {},
): AuthGuardState {
    val uiState by viewModel.uiState.collectAsState()
    var isPromptVisible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(uiState, isPromptVisible) {
        if (!isPromptVisible) return@LaunchedEffect
        when (uiState) {
            is AuthUiState.Authenticated -> {
                isPromptVisible = false
                val action = pendingAction
                pendingAction = null
                action?.invoke()
            }
            is AuthUiState.NeedsRegister, is AuthUiState.NeedsOnboarding -> {
                isPromptVisible = false
                pendingAction = null
                onNeedsRegisterOrOnboarding(uiState)
            }
            else -> Unit
        }
    }

    return AuthGuardState(
        isPromptVisible = isPromptVisible,
        isLoading = isPromptVisible && uiState is AuthUiState.Loading,
        errorMessage = (uiState as? AuthUiState.Error)?.message,
        guard = { action ->
            if (viewModel.isLoggedIn) {
                action()
            } else {
                pendingAction = action
                isPromptVisible = true
            }
        },
        dismiss = {
            isPromptVisible = false
            pendingAction = null
        },
    )
}
