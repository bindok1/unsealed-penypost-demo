package com.apps.unsealed.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.SurfaceCream
import kotlinx.coroutines.delay

/**
 * Splash screen — light cream background, title/subtitle up top, Peni mascot
 * peeking up from the bottom 40% of the screen (full width).
 * Does NOT use bg_texture (that's for the letter canvas inside the app).
 *
 * Routing table:
 *  - [AuthUiState.Unauthenticated], first install    → [onNavigateToWelcome]
 *  - [AuthUiState.Unauthenticated], already onboarded → [onNavigateToMain] (guest)
 *  - [AuthUiState.NeedsRegister]    → [onNavigateToRegister]
 *  - [AuthUiState.NeedsOnboarding]  → [onNavigateToOnboarding]
 *  - [AuthUiState.Authenticated]    → [onNavigateToMain]
 *
 * [viewModel] deliberately defaults to the Activity-scoped [AuthViewModel]
 * instance (same one [com.apps.unsealed.MainActivity]'s `authGuard` and
 * `PenPalsScreen` use) rather than the `NavBackStackEntry`-scoped default —
 * every screen in the Splash → Login/Register → Onboarding chain shares
 * this same instance so a sign-in/register/onboarding-step update is
 * visible everywhere immediately, instead of only within that one screen's
 * own throwaway `AuthViewModel` (which is what previously left the main
 * app stuck on "Hi, Guest" after a full Login-screen sign-in, even though
 * the backend profile was created/onboarded correctly).
 */
@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasSeenWelcome by viewModel.hasSeenWelcome.collectAsState()

    // ── Entrance animation ────────────────────────────────────────────────────
    val mascotScale = remember { Animatable(0.72f) }
    val mascotAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        textAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        mascotAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        mascotScale.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        delay(600)
    }

    // ── Routing ─────────────────────────────────────────────────────────────
    // Waits on [hasSeenWelcome] too (null = still loading from disk) so a
    // guest's first-ever launch always resolves to the Welcome carousel
    // rather than racing straight past it into guest mode.
    LaunchedEffect(uiState, hasSeenWelcome) {
        val seenWelcome = hasSeenWelcome ?: return@LaunchedEffect
        when (uiState) {
            is AuthUiState.Loading -> Unit
            is AuthUiState.Unauthenticated, is AuthUiState.Error -> {
                delay(5000)
                if (!seenWelcome) onNavigateToWelcome() else onNavigateToMain()
            }
            is AuthUiState.NeedsRegister    -> { delay(200); onNavigateToRegister() }
            is AuthUiState.NeedsOnboarding  -> { delay(200); onNavigateToOnboarding() }
            is AuthUiState.Authenticated    -> { delay(200); onNavigateToMain() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCream),
    ) {
        // ── Title + subtitle — top of screen ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.splash_title),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = BrandInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.splash_subtitle),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = BrandGoldDim,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(textAlpha.value),
            )
        }

        // ── Peni mascot — bottom 40% of screen, full width ────────────────────
        Image(
            painter = painterResource(R.drawable.mascot_splashscreen),
            contentDescription = stringResource(R.string.splash_logo_desc),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .scale(mascotScale.value)
                .alpha(mascotAlpha.value),
        )
    }
}
