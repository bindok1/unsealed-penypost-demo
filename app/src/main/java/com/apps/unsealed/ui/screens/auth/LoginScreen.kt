package com.apps.unsealed.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.GoogleSignInButton
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.screens.profile.widgets.LanguagePickerBottomSheet
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.IceSkyAccent
import com.apps.unsealed.ui.theme.IceSkyBlue
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.PaperCream
import com.apps.unsealed.ui.theme.SurfaceBorderLight
import com.apps.unsealed.ui.theme.SurfaceCardLight
import com.apps.unsealed.ui.theme.SurfaceCream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

/** Email/Password auth targets either SignIn or SignUp */
private enum class EmailAuthMode { SignIn, SignUp }

// ── Login brand colours ──────────────────────────────────────────────────────
private val LoginBgCream    = SurfaceCream
private val LoginBgSky      = IceSkyBlue
private val LoginGold       = BrandGold
private val LoginGoldDim    = BrandGoldDim
private val LoginCardBg     = SurfaceCardLight
private val LoginCardStroke = SurfaceBorderLight

/**
 * Login screen — warm epistolary entry point featuring a cute tactile Penpal
 * Letter suite with animated flight trail, wax seal, air-mail marks, and a
 * sleek, frameless bottom action area.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val isLoading    = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    // ── Language picker state ───────────────────────────────────────────────
    var isLanguagePickerVisible by remember { mutableStateOf(false) }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLangCode = if (!currentLocales.isEmpty) {
        currentLocales.toLanguageTags()
    } else {
        Locale.getDefault().language
    }
    val currentLangLabel = if (currentLangCode.startsWith("id", ignoreCase = true) || currentLangCode.startsWith("in", ignoreCase = true)) {
        "🇮🇩 ID"
    } else {
        "🇬🇧 EN"
    }

    // ── Email form expand & auth mode state ─────────────────────────────────
    var isEmailFormExpanded by remember { mutableStateOf(false) }
    var emailAuthMode by remember { mutableStateOf(EmailAuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var emailFormError by remember { mutableStateOf<String?>(null) }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }

    fun submitEmailAuth() {
        focusManager.clearFocus()
        emailFormError = null
        resetStatusMessage = null
        val trimmedEmail = email.trim()
        when (emailAuthMode) {
            EmailAuthMode.SignIn -> viewModel.signInWithEmail(trimmedEmail, password) { emailFormError = it }
            EmailAuthMode.SignUp -> viewModel.signUpWithEmail(trimmedEmail, password) { emailFormError = it }
        }
    }

    fun requestPasswordReset() {
        focusManager.clearFocus()
        emailFormError = null
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            resetStatusMessage = context.getString(R.string.login_reset_email_needs_email)
            return
        }
        viewModel.sendPasswordResetEmail(trimmedEmail) { success, message ->
            resetStatusMessage = if (success) context.getString(R.string.login_reset_email_sent) else message
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.NeedsRegister -> onNavigateToRegister()
            is AuthUiState.NeedsOnboarding -> onNavigateToOnboarding()
            is AuthUiState.Authenticated -> onNavigateToMain()
            else -> Unit
        }
    }

    // Auto-scroll when expanding email form
    LaunchedEffect(isEmailFormExpanded) {
        if (isEmailFormExpanded) {
            delay(150)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // ── Entrance animations ─────────────────────────────────────────────────
    val logoScale  = remember { Animatable(0.85f) }
    val logoAlpha  = remember { Animatable(0f) }
    var actionsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        delay(120)
        actionsVisible = true
    }

    // ── Continuous floating animations for Cute Penpal Letter Suite ───────────
    val infiniteTransition = rememberInfiniteTransition(label = "login_penpal_motion")
    val heroFloatY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroFloatY",
    )
    val heroTilt by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroTilt",
    )
    val envelopeFloatY by infiniteTransition.animateFloat(
        initialValue = -5.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "envelopeFloatY",
    )
    val envelopeTilt by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "envelopeTilt",
    )
    val waxSealPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waxSealPulse",
    )

    // Dynamic sizes for compacting hero when typing / email expanded
    val heroTopPadding by animateDpAsState(
        targetValue = if (isEmailFormExpanded) 4.dp else 18.dp,
        label = "heroTopPadding",
    )
    val appTitleSize by animateFloatAsState(
        targetValue = if (isEmailFormExpanded) 22f else 28f,
        label = "appTitleSize",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to LoginBgSky,
                        0.32f to LoginBgCream,
                        1.0f to LoginBgCream,
                    ),
                ),
            ),
    ) {
        // ── Atmospheric Particles & Sparkles Canvas ─────────────────────────
        LoginAtmosphericSparklesCanvas(modifier = Modifier.fillMaxSize())

        // ── Custom Postal Stationery Canvas Watermarks ──────────────────────
        PostalStationeryCanvasBackground(modifier = Modifier.fillMaxSize())

        // ── Faint Decorative Flying Envelope (Bottom-Right) ─────────────────
        Image(
            painter = painterResource(R.drawable.envelope_2),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 0.dp, bottom = 16.dp)
                .alpha(0.06f)
                .graphicsLayer { rotationZ = -12f },
        )

        // ── Scrollable Content with Bottom-Docked Action Buttons ───────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── TOP: CUTE PENPAL LETTER & TRAVEL SHOWCASE (HERO) ─────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = heroTopPadding, bottom = if (isEmailFormExpanded) 4.dp else 12.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value),
                ) {
                    // Cute Penpal Letter Suite Card
                    CutePenpalLetterCard(
                        isCompact = isEmailFormExpanded,
                        floatY = heroFloatY,
                        tilt = heroTilt,
                        envelopeFloatY = envelopeFloatY,
                        envelopeTilt = envelopeTilt,
                        sealPulse = waxSealPulse,
                    )

                    Spacer(Modifier.height(if (isEmailFormExpanded) 12.dp else 60.dp))

                    // App Name
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = NunitoFontFamily,
                        fontSize = appTitleSize.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandInk,
                        letterSpacing = (-0.5).sp,
                    )

                    // Diamond divider and tagline (hidden in compact email mode to maximize keyboard space)
                    AnimatedVisibility(visible = !isEmailFormExpanded) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Box(Modifier.width(24.dp).height(1.2.dp).background(LoginGold.copy(alpha = 0.4f)))
                                Box(
                                    Modifier
                                        .padding(horizontal = 6.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(LoginGold),
                                )
                                Box(Modifier.width(24.dp).height(1.2.dp).background(LoginGold.copy(alpha = 0.4f)))
                            }

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = stringResource(R.string.login_tagline),
                                fontFamily = NunitoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LoginGoldDim,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.heightIn(min = 16.dp))

                // ── BOTTOM: FRAMELESS & BREATHABLE ACTION SECTION ───────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                ) {
                    AnimatedVisibility(
                        visible = actionsVisible,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            tween(420, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 5 },
                        ),
                    ) {
                        AnimatedContent(
                            targetState = isEmailFormExpanded,
                            transitionSpec = {
                                if (targetState) {
                                    (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 6 })
                                        .togetherWith(fadeOut(tween(160)))
                                } else {
                                    (fadeIn(tween(260)) + slideInVertically(tween(300)) { -it / 6 })
                                        .togetherWith(fadeOut(tween(160)))
                                }
                            },
                            label = "ActionsExpandTransition",
                            modifier = Modifier.fillMaxWidth(),
                        ) { expanded ->
                            if (!expanded) {
                                // ── STATE 1: SLEEK FRAMELESS OPTIONS ─────────────────
                                SleekFramelessAuthOptions(
                                    isLoading = isLoading,
                                    errorMessage = errorMessage,
                                    onGoogleSignInClick = { viewModel.signInWithGoogle(context) },
                                    onEmailOptionClick = {
                                        emailAuthMode = EmailAuthMode.SignIn
                                        emailFormError = null
                                        resetStatusMessage = null
                                        isEmailFormExpanded = true
                                    },
                                )
                            } else {
                                // ── STATE 2: COMPACT EMAIL & PASSWORD FORM CARD ──────
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = LoginCardBg,
                                    border = BorderStroke(1.dp, LoginCardStroke),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    ExpandedEmailAuthForm(
                                        authMode = emailAuthMode,
                                        onAuthModeChanged = { mode ->
                                            emailAuthMode = mode
                                            emailFormError = null
                                            resetStatusMessage = null
                                        },
                                        email = email,
                                        onEmailChange = {
                                            email = it
                                            emailFormError = null
                                            resetStatusMessage = null
                                        },
                                        password = password,
                                        onPasswordChange = {
                                            password = it
                                            emailFormError = null
                                        },
                                        isPasswordVisible = isPasswordVisible,
                                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                                        emailFormError = emailFormError,
                                        resetStatusMessage = resetStatusMessage,
                                        isLoading = isLoading,
                                        onBackClick = {
                                            focusManager.clearFocus()
                                            emailFormError = null
                                            resetStatusMessage = null
                                            isEmailFormExpanded = false
                                        },
                                        onForgotPasswordClick = { requestPasswordReset() },
                                        onSubmitClick = { submitEmailAuth() },
                                        onFieldFocused = {
                                            coroutineScope.launch {
                                                delay(200)
                                                scrollState.animateScrollTo(scrollState.maxValue)
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Floating Language Toggle (top-right) ────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PaperCream.copy(alpha = 0.92f))
                .border(1.dp, SurfaceBorderLight, RoundedCornerShape(20.dp))
                .clickable { isLanguagePickerVisible = true }
                .padding(horizontal = 13.dp, vertical = 6.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentLangLabel,
                fontFamily = NunitoFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandInkDeep,
            )
        }
    }

    if (isLanguagePickerVisible) {
        LanguagePickerBottomSheet(
            currentLanguageCode = currentLangCode,
            onLanguageSelected = { langCode ->
                val localeList = if (langCode == "id") {
                    LocaleListCompat.forLanguageTags("id,in")
                } else {
                    LocaleListCompat.forLanguageTags(langCode)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            },
            onDismiss = { isLanguagePickerVisible = false },
        )
    }
}

/**
 * Cute Penpal Letter & Travel Suite Showcase.
 */
@Composable
private fun CutePenpalLetterCard(
    isCompact: Boolean,
    floatY: Float,
    tilt: Float,
    envelopeFloatY: Float,
    envelopeTilt: Float,
    sealPulse: Float,
) {
    val density = LocalContext.current.resources.displayMetrics.density

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 80.dp else 195.dp)
            .graphicsLayer {
                translationY = if (!isCompact) floatY * density else 0f
                rotationZ = if (!isCompact) tilt else 0f
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isCompact) {
            // Compact Header Mode when typing in form
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperCream)
                    .border(1.dp, BrandGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal
                    = 14.dp, vertical = 8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.envelope_1),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "💌 Penpal Letters & Stories",
                    fontFamily = CaveatFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandInk,
                )
            }
        } else {
            // ── Full Cute Tactile Epistolary Showcase ──

            // 1. Backing angled shadow envelope / card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .height(144.dp)
                    .graphicsLayer { rotationZ = 3.5f }
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = BrandGoldDeep.copy(alpha = 0.12f),
                    )
                    .background(SurfaceCardLight, RoundedCornerShape(18.dp))
                    .border(1.dp, SurfaceBorderLight, RoundedCornerShape(18.dp)),
            )

            // 2. Main cozy tactile letter paper (Stationery)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(152.dp)
                    .graphicsLayer { rotationZ = -2f }
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = BrandGoldDeep.copy(alpha = 0.15f),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperCream)
                    .border(1.2.dp, BrandGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                // Top row: Air Mail pill + Cute perforated stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IceSkyAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, IceSkyAccent.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "✈️ PAR AVION • AIR MAIL",
                            fontSize = 8.5.sp,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = IceSkyAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        )
                    }

                    // Perforated stamp with globe / region icon
                    Box(
                        modifier = Modifier
                            .size(36.dp, 44.dp)
                            .shadow(2.dp, RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFF9E8), RoundedCornerShape(4.dp))
                            .border(1.dp, BrandGold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_region_asia),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Handwritten letter body snippet
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .padding(top = 26.dp),
                ) {
                    Text(
                        text = "Dear Penpal across the sea,",
                        fontFamily = CaveatFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandInk,
                    )
                    Text(
                        text = "Every letter brings us closer... 💌",
                        fontFamily = CaveatFontFamily,
                        fontSize = 14.5.sp,
                        color = BrandInk.copy(alpha = 0.78f),
                        lineHeight = 18.sp,
                    )
                }
            }

            // 3. Front Focal Point: Cute Floating Love Envelope with Pulsing Heart Wax Seal
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = 8.dp)
                    .graphicsLayer {
                        translationY = envelopeFloatY * density
                        rotationZ = envelopeTilt
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Outer warm glowing halo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(BrandGold.copy(alpha = 0.28f), Color.Transparent),
                            ),
                        ),
                )

                // Sealed Love Envelope
                Image(
                    painter = painterResource(R.drawable.envelope_1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color(0xFFD48B38).copy(alpha = 0.4f),
                        ),
                )

                // Pulsing Heart Wax Seal Badge in the center/bottom of the envelope
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 4.dp)
                        .scale(sealPulse)
                        .size(28.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFE84E4E), Color(0xFF9E1E1E)),
                            ),
                            CircleShape,
                        )
                        .border(1.2.dp, Color(0xFFFFD59E), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }

                // Tiny drifting sparkle heart top-right
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE84E4E).copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(14.dp)
                        .scale(sealPulse * 0.9f)
                        .graphicsLayer { rotationZ = 15f },
                )
            }
        }
    }
}

/**
 * State 1: Sleek, Frameless Auth Options (Google + Refined Email Pill).
 */
@Composable
private fun SleekFramelessAuthOptions(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onEmailOptionClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Global / Firebase error banner
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            ) {
                Text(
                    text = if (errorMessage == stringResource(R.string.error_network)) {
                        errorMessage
                    } else {
                        stringResource(R.string.login_error_generic) + "\n" + errorMessage
                    },
                    fontFamily = NunitoFontFamily,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // Primary: Google Sign-In Button
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = LoginGold,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(34.dp),
                )
            }
        } else {
            GoogleSignInButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Subtle "or" divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 2.dp),
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(SurfaceBorderLight))
            Text(
                text = stringResource(R.string.login_or_divider),
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = LoginGoldDim,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Box(Modifier.weight(1f).height(1.dp).background(SurfaceBorderLight))
        }

        Spacer(Modifier.height(14.dp))

        // Secondary: Sleek Email Option Pill Button
        SleekEmailPillButton(
            text = stringResource(R.string.login_continue_with_email),
            onClick = onEmailOptionClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))

        // Terms note
        Text(
            text = stringResource(R.string.login_terms),
            fontFamily = NunitoFontFamily,
            fontSize = 11.sp,
            color = LoginGoldDim.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/**
 * Sleek secondary action pill button for "Lanjut dengan Email".
 */
@Composable
private fun SleekEmailPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PaperCream,
        border = BorderStroke(1.2.dp, BrandGold.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
        modifier = modifier
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = LoginGold,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = BrandInk,
            )
        }
    }
}

/**
 * State 2: Expanded Email & Password Form with Sign In / Sign Up Tabs.
 */
@Composable
private fun ExpandedEmailAuthForm(
    authMode: EmailAuthMode,
    onAuthModeChanged: (EmailAuthMode) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    emailFormError: String?,
    resetStatusMessage: String?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onFieldFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSignIn = authMode == EmailAuthMode.SignIn

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Top row: Back button + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.login_back_to_options),
                    tint = BrandInk,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = stringResource(
                    if (isSignIn) R.string.login_sign_in_tab else R.string.login_create_account_tab,
                ) + " ${stringResource(R.string.login_email_label)}",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = BrandInk,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Segmented Switcher: [ Masuk ] | [ Daftar ]
        AuthModePillSwitcher(
            selectedMode = authMode,
            onModeSelected = onAuthModeChanged,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LoginGold,
            unfocusedBorderColor = LoginCardStroke,
            focusedLabelColor = LoginGoldDim,
            unfocusedLabelColor = LoginGoldDim.copy(alpha = 0.75f),
            cursorColor = LoginGold,
            focusedContainerColor = SurfaceCream,
            unfocusedContainerColor = SurfaceCream.copy(alpha = 0.65f),
            focusedTextColor = BrandInkDeep,
            unfocusedTextColor = BrandInkDeep,
        )

        // Email TextField
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.login_email_label), fontFamily = NunitoFontFamily) },
            placeholder = { Text(stringResource(R.string.login_email_placeholder), fontFamily = NunitoFontFamily) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = LoginGoldDim,
                    modifier = Modifier.size(19.dp),
                )
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocused() },
        )

        Spacer(Modifier.height(8.dp))

        // Password TextField
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label), fontFamily = NunitoFontFamily) },
            placeholder = { Text(stringResource(R.string.login_password_placeholder), fontFamily = NunitoFontFamily) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = LoginGoldDim,
                    modifier = Modifier.size(19.dp),
                )
            },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (email.isNotBlank() && password.length >= 6 && !isLoading) {
                    onSubmitClick()
                }
            }),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (isPasswordVisible) R.string.login_password_hide_desc else R.string.login_password_show_desc,
                        ),
                        tint = LoginGoldDim,
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFieldFocused() },
        )

        // Helper / password hint for registration
        if (!isSignIn) {
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = "• " + stringResource(R.string.login_password_hint_create),
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    color = LoginGoldDim.copy(alpha = 0.8f),
                )
            }
        }

        // Inline error message
        if (emailFormError != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = emailFormError,
                    fontFamily = NunitoFontFamily,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        // Forgot password button (only in Sign In mode)
        if (isSignIn) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = LoginGoldDim,
                    modifier = Modifier.clickable(enabled = !isLoading) { onForgotPasswordClick() },
                )
            }
        }

        // Password reset status notification
        if (resetStatusMessage != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LoginGold.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, LoginGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = resetStatusMessage,
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    color = BrandInkDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Submit Button
        LetterlyButton(
            text = stringResource(
                if (isSignIn) R.string.login_sign_in_with_email_cta
                else R.string.login_sign_up_with_email_cta,
            ),
            onClick = onSubmitClick,
            enabled = !isLoading && email.isNotBlank() && password.length >= 6,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        // Quick Mode Switch Link at bottom
        Text(
            text = stringResource(
                if (isSignIn) R.string.login_switch_to_sign_up
                else R.string.login_switch_to_sign_in,
            ),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = LoginGoldDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) {
                    onAuthModeChanged(if (isSignIn) EmailAuthMode.SignUp else EmailAuthMode.SignIn)
                }
                .padding(vertical = 4.dp),
        )
    }
}

/**
 * Segmented Pill Switcher for [Masuk] and [Daftar].
 */
@Composable
private fun AuthModePillSwitcher(
    selectedMode: EmailAuthMode,
    onModeSelected: (EmailAuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceBorderLight.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Masuk (Sign In) Tab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selectedMode == EmailAuthMode.SignIn) LoginGold else Color.Transparent,
                    )
                    .clickable { onModeSelected(EmailAuthMode.SignIn) }
                    .padding(vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(R.string.login_sign_in_tab),
                    fontFamily = NunitoFontFamily,
                    fontWeight = if (selectedMode == EmailAuthMode.SignIn) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = if (selectedMode == EmailAuthMode.SignIn) BrandInkDeep else LoginGoldDim,
                )
            }

            // Daftar (Sign Up) Tab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selectedMode == EmailAuthMode.SignUp) LoginGold else Color.Transparent,
                    )
                    .clickable { onModeSelected(EmailAuthMode.SignUp) }
                    .padding(vertical = 7.dp),
            ) {
                Text(
                    text = stringResource(R.string.login_create_account_tab),
                    fontFamily = NunitoFontFamily,
                    fontWeight = if (selectedMode == EmailAuthMode.SignUp) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = if (selectedMode == EmailAuthMode.SignUp) BrandInkDeep else LoginGoldDim,
                )
            }
        }
    }
}

/**
 * Atmospheric Particles / Drifting Sparkles Canvas.
 */
@Composable
private fun LoginAtmosphericSparklesCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "login_sparkles")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alphaAnim",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        fun drawGleam(cx: Float, cy: Float, radius: Float, intensity: Float) {
            val starPath = Path().apply {
                moveTo(cx, cy - radius)
                cubicTo(cx, cy, cx, cy, cx + radius, cy)
                cubicTo(cx, cy, cx, cy, cx, cy + radius)
                cubicTo(cx, cy, cx, cy, cx - radius, cy)
                cubicTo(cx, cy, cx, cy, cx, cy - radius)
                close()
            }
            drawPath(
                path = starPath,
                color = BrandGold.copy(alpha = intensity * alphaAnim),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f * alphaAnim),
                radius = radius * 0.35f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
        }

        drawGleam(width * 0.14f, height * 0.10f, 7.dp.toPx(), 0.35f)
        drawGleam(width * 0.88f, height * 0.18f, 9.dp.toPx(), 0.40f)
        drawGleam(width * 0.82f, height * 0.38f, 6.dp.toPx(), 0.30f)
        drawGleam(width * 0.10f, height * 0.55f, 8.dp.toPx(), 0.32f)
        drawGleam(width * 0.90f, height * 0.72f, 7.dp.toPx(), 0.30f)
    }
}

/**
 * Custom Canvas Drawing for Postmark Watermark and Epistolary Wave Lines.
 */
@Composable
private fun PostalStationeryCanvasBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Postmark in the top right area
        val postmarkCenter = androidx.compose.ui.geometry.Offset(width * 0.86f, height * 0.12f)
        val outerRadius = 46.dp.toPx()
        val innerRadius = 38.dp.toPx()
        val postmarkColor = LoginGold.copy(alpha = 0.13f)
        val waveColor = LoginGold.copy(alpha = 0.11f)

        // Outer & inner circles
        drawCircle(
            color = postmarkColor,
            radius = outerRadius,
            center = postmarkCenter,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            color = postmarkColor.copy(alpha = 0.07f),
            radius = innerRadius,
            center = postmarkCenter,
            style = Stroke(width = 1.dp.toPx()),
        )

        // Postmark wavy cancellation lines extending to the left
        for (i in -1..1) {
            val yOffset = postmarkCenter.y + (i * 9.dp.toPx())
            val path = Path().apply {
                moveTo(postmarkCenter.x - outerRadius - 6.dp.toPx(), yOffset)
                val waveLength = 20.dp.toPx()
                val waveHeight = 3.5.dp.toPx()
                for (step in 0..3) {
                    val startX = postmarkCenter.x - outerRadius - 6.dp.toPx() - (step * waveLength)
                    cubicTo(
                        startX - waveLength * 0.25f, yOffset - waveHeight,
                        startX - waveLength * 0.75f, yOffset + waveHeight,
                        startX - waveLength, yOffset,
                    )
                }
            }
            drawPath(path = path, color = waveColor, style = Stroke(width = 1.4.dp.toPx()))
        }

        // 2. Postmark in the bottom left area
        val bottomStampCenter = androidx.compose.ui.geometry.Offset(width * 0.12f, height * 0.88f)
        val bottomRadius = 36.dp.toPx()
        drawCircle(
            color = LoginGold.copy(alpha = 0.08f),
            radius = bottomRadius,
            center = bottomStampCenter,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            color = LoginGold.copy(alpha = 0.05f),
            radius = bottomRadius * 0.8f,
            center = bottomStampCenter,
            style = Stroke(width = 1.dp.toPx()),
        )

        // Bottom wavy lines extending to the right
        for (i in -1..1) {
            val yOffset = bottomStampCenter.y + (i * 8.dp.toPx())
            val path = Path().apply {
                moveTo(bottomStampCenter.x + bottomRadius + 6.dp.toPx(), yOffset)
                val waveLength = 16.dp.toPx()
                val waveHeight = 3.dp.toPx()
                for (step in 0..3) {
                    val startX = bottomStampCenter.x + bottomRadius + 6.dp.toPx() + (step * waveLength)
                    cubicTo(
                        startX + waveLength * 0.25f, yOffset - waveHeight,
                        startX + waveLength * 0.75f, yOffset + waveHeight,
                        startX + waveLength, yOffset,
                    )
                }
            }
            drawPath(path = path, color = LoginGold.copy(alpha = 0.08f), style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}
