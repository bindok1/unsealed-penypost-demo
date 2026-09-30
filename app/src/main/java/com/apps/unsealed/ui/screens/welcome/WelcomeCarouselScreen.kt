package com.apps.unsealed.ui.screens.welcome

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.EmailSignInButton
import com.apps.unsealed.ui.components.GoogleSignInButton
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.IceSkyAccent
import com.apps.unsealed.ui.theme.IceSkyBlue
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.PaperCream
import com.apps.unsealed.ui.theme.SurfaceBorderLight
import com.apps.unsealed.ui.theme.SurfaceCardLight
import com.apps.unsealed.ui.theme.SurfaceCream
import kotlin.math.cos
import kotlin.math.sin

private data class WelcomeSlideData(
    val titleRes: Int,
    val subtitleRes: Int,
)

private val WelcomeSlides = listOf(
    WelcomeSlideData(R.string.welcome_slide1_title, R.string.welcome_slide1_body),
    WelcomeSlideData(R.string.welcome_slide2_title, R.string.welcome_slide2_body),
    WelcomeSlideData(R.string.welcome_slide3_title, R.string.welcome_slide3_body),
)

/**
 * First-install marketing carousel — shown once (gated by [AuthViewModel.hasSeenWelcome])
 * before a guest ever sees [com.apps.unsealed.ui.screens.penpals.screen.PenPalsScreen].
 *
 * Visual style: Warm Cozy Postal Skeuomorphism with Rich Interactive Canvas
 * Demonstrations (Simulated Letter Crafting, Global Penpal Travel Map, and Peni Courier in motion).
 */
@Composable
fun WelcomeCarouselScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.NeedsRegister -> {
                viewModel.markWelcomeSeen()
                onNavigateToRegister()
            }
            is AuthUiState.NeedsOnboarding -> {
                viewModel.markWelcomeSeen()
                onNavigateToOnboarding()
            }
            is AuthUiState.Authenticated -> {
                viewModel.markWelcomeSeen()
                onNavigateToMain()
            }
            else -> Unit
        }
    }

    val pagerState = rememberPagerState(pageCount = { WelcomeSlides.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to IceSkyBlue,
                        0.30f to SurfaceCream,
                        1.0f to SurfaceCream,
                    )
                )
            ),
    ) {
        // Subtle background polar atmospheric particles
        BackgroundSparkleCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            // ── Carousel Pager ───────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                WelcomeSlideItem(page = page, slide = WelcomeSlides[page])
            }

            // ── Page indicator dots (Tactile Pill) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(WelcomeSlides.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isActive) 26.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) BrandGold else SurfaceBorderLight
                            ),
                    )
                }
            }

            // ── Action area ───────────────────────────────────────────────────
            if (errorMessage != null) {
                Text(
                    text = if (errorMessage == stringResource(R.string.error_network)) {
                        errorMessage
                    } else {
                        stringResource(R.string.login_error_generic) + "\n" + errorMessage
                    },
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    fontFamily = NunitoFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = BrandGold,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GoogleSignInButton(
                        onClick = { viewModel.signInWithGoogle(context) },
                        label = stringResource(R.string.welcome_google_short),
                        modifier = Modifier.weight(1f),
                    )
                    EmailSignInButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.welcome_cta_explore),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = BrandGoldDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) {
                            viewModel.markWelcomeSeen()
                            onNavigateToMain()
                        }
                        .padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SLIDE ITEM CONTAINER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeSlideItem(page: Int, slide: WelcomeSlideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Visual Interactive Showcase Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> LetterCraftingCard()
                1 -> GlobalExchangeCard()
                else -> PeniCourierCard()
            }
        }

        Spacer(Modifier.height(12.dp))

        // Text Description Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(slide.titleRes),
                fontSize = 22.sp,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = BrandInk,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(slide.subtitleRes),
                fontSize = 13.5.sp,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                color = BrandGoldDim,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SLIDE 1: SIMULATED HANDWRITTEN LETTER CRAFTING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LetterCraftingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "letter_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val sealScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sealScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                translationY = floatY * density
                rotationZ = -1.2f
            },
        contentAlignment = Alignment.Center,
    ) {
        // Shadow base envelope
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(260.dp)
                .graphicsLayer { rotationZ = 3.5f }
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = BrandGoldDeep.copy(alpha = 0.12f),
                    ambientColor = BrandGoldDeep.copy(alpha = 0.06f)
                )
                .background(SurfaceCardLight, RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceBorderLight, RoundedCornerShape(20.dp))
        )

        // Real tactile letter sheet
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = BrandGoldDeep.copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(PaperCream)
                .border(1.2.dp, BrandGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            // Air Mail Top-Right Vintage Stamp & Postmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Air Mail Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IceSkyAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IceSkyAccent.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = "✈️ PAR AVION • AIR MAIL",
                        fontSize = 9.sp,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = IceSkyAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Perforated Stamp with Polar Mascot
                Box(
                    modifier = Modifier
                        .size(46.dp, 56.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFF9E6), RoundedCornerShape(4.dp))
                        .border(1.dp, BrandGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_region_antarctica),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Handwritten letter lines
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Dear Penpal across the sea,",
                    fontFamily = CaveatFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Today the snow falls gently here in Antarctica. I brewed a warm cup of tea and thought of far-off stories...",
                    fontFamily = CaveatFontFamily,
                    fontSize = 17.5.sp,
                    color = BrandInk.copy(alpha = 0.85f),
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "With warm thoughts, Peni 🕊️",
                        fontFamily = CaveatFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGoldDeep,
                    )
                }
            }

            // Wax Seal Badge at bottom left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = 6.dp)
                    .scale(sealScale)
                    .size(38.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFE8984E), Color(0xFF9E4B1E))
                        ),
                        CircleShape
                    )
                    .border(1.dp, Color(0xFFFFD59E), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SLIDE 2: GLOBAL PENPALS EXCHANGE & TRAVEL MAP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlobalExchangeCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "flight_anim")
    val flightProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flightProgress"
    )
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = BrandGoldDeep.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardLight)
            .border(1.dp, SurfaceBorderLight, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Simulated Postal Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Tokyo  ➔  Paris",
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandInk
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandGold.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "⏱️ ~14 hrs In Flight",
                        fontFamily = NunitoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandGoldDeep,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Animated Flight Path Arc Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startX = size.width * 0.15f
                    val startY = size.height * 0.70f
                    val endX = size.width * 0.85f
                    val endY = size.height * 0.40f
                    val controlX = size.width * 0.50f
                    val controlY = -size.height * 0.10f

                    val path = Path().apply {
                        moveTo(startX, startY)
                        quadraticTo(controlX, controlY, endX, endY)
                    }

                    // Dotted flight trail
                    drawPath(
                        path = path,
                        color = BrandGold.copy(alpha = 0.45f),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Destination beacon rings
                    drawCircle(
                        color = IceSkyAccent.copy(alpha = 0.25f),
                        radius = 24.dp.toPx() * beaconPulse,
                        center = Offset(endX, endY),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = IceSkyAccent,
                        radius = 5.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    drawCircle(
                        color = BrandGold,
                        radius = 5.dp.toPx(),
                        center = Offset(startX, startY)
                    )

                    // Current position of flying letter along quadratic bezier
                    val t = flightProgress
                    val currentX = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * controlX + t * t * endX
                    val currentY = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * controlY + t * t * endY

                    // Glowing flight letter indicator
                    drawCircle(
                        color = BrandGold,
                        radius = 9.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                }

                // Flying Envelope Icon
                val t = flightProgress
                val startXPct = 0.15f
                val endXPct = 0.85f
                val currentXPct = (startXPct + (endXPct - startXPct) * t)
                val angleDeg = (t - 0.5f) * 28f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = (currentXPct - 0.5f) * 260.dp.toPx()
                            translationY = (-sin(t * Math.PI) * 36.dp.toPx()).toFloat()
                            rotationZ = angleDeg
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.envelope_1),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(4.dp, RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Two Penpal Polaroid Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Badge
                PenpalAvatarBadge(
                    name = "Kirana 🌸",
                    location = "Tokyo, Japan",
                    tag = "#astronomy",
                    iconRes = R.drawable.ic_region_asia,
                    badgeColor = Color(0xFFFFECE0)
                )

                // Recipient Badge
                PenpalAvatarBadge(
                    name = "Lucas ☕",
                    location = "Paris, France",
                    tag = "#literature",
                    iconRes = R.drawable.ic_region_europe,
                    badgeColor = Color(0xFFE3F2FD)
                )
            }
        }
    }
}

@Composable
private fun PenpalAvatarBadge(
    name: String,
    location: String,
    tag: String,
    iconRes: Int,
    badgeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PaperCream,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier.width(135.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    fontFamily = NunitoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandInk
                )
                Text(
                    text = location,
                    fontFamily = NunitoFontFamily,
                    fontSize = 9.sp,
                    color = BrandGoldDim
                )
                Text(
                    text = tag,
                    fontFamily = NunitoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGoldDeep
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SLIDE 3: PENI THE ANTARCTIC COURIER & STAMP REWARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PeniCourierCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "peni_bounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceY"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateAngle"
    )
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = BrandGoldDeep.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardLight)
            .border(1.dp, SurfaceBorderLight, RoundedCornerShape(24.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        // Falling Snow & Speed Lines in background
        PeniSnowfallCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Peni Mascot with Floating Motion
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        translationY = bounceY * density
                        rotationZ = rotateAngle
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.mascot_splashscreen),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Golden Glowing Envelope carried by Peni
                Image(
                    painter = painterResource(R.drawable.envelope_1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-8).dp)
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                )
            }

            Spacer(Modifier.height(10.dp))

            // Stamp Streak & Quest Reward Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PaperCream,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, BrandGold.copy(alpha = 0.5f)),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(starScale)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Daily Postal Streak 🔥",
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Collect Rare Global Stamps",
                                fontFamily = NunitoFontFamily,
                                fontSize = 10.sp,
                                color = BrandGoldDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Stamp Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IceSkyAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IceSkyAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "+1 Stamp",
                            fontFamily = NunitoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceSkyAccent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CANVAS PARTICLES & ATMOSPHERIC EFFECTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BackgroundSparkleCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_sparkles")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier) {
        val points = listOf(
            Offset(size.width * 0.15f, size.height * 0.08f),
            Offset(size.width * 0.85f, size.height * 0.12f),
            Offset(size.width * 0.78f, size.height * 0.28f),
            Offset(size.width * 0.10f, size.height * 0.32f),
            Offset(size.width * 0.90f, size.height * 0.65f),
            Offset(size.width * 0.08f, size.height * 0.72f),
        )

        points.forEachIndexed { i, pt ->
            val ptAlpha = if (i % 2 == 0) alpha else (0.9f - alpha)
            drawCircle(
                color = BrandGold.copy(alpha = ptAlpha * 0.5f),
                radius = (2.5f + (i % 3) * 1.5f).dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun PeniSnowfallCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "snowfall")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val flakeOffsets = listOf(
            0.10f to 0.15f,
            0.30f to 0.05f,
            0.70f to 0.12f,
            0.85f to 0.22f,
            0.20f to 0.65f,
            0.80f to 0.70f,
        )

        flakeOffsets.forEach { (xPct, yPct) ->
            val curY = ((yPct + time) % 1.0f) * size.height
            val curX = (xPct * size.width) + sin(time * 6.28f + xPct * 10f) * 10f
            drawCircle(
                color = IceSkyAccent.copy(alpha = 0.35f),
                radius = 2.dp.toPx(),
                center = Offset(curX, curY)
            )
        }
    }
}
