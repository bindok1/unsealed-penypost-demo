package com.apps.unsealed.ui.screens.profile.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.profile.viewmodel.CollectibleAssetUi
import com.apps.unsealed.ui.screens.profile.viewmodel.KeepsakeCategory
import com.apps.unsealed.ui.screens.profile.viewmodel.KeepsakeUiState
import com.apps.unsealed.ui.screens.profile.viewmodel.ProfileStampBookViewModel
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BuyButtonBlue
import com.apps.unsealed.ui.theme.NunitoFontFamily
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Snail Mail Keepsake Album Screen ("Buku Kenangan Snail Mail"):
 * A nostalgic collector's sanctuary showcasing every stamp, letter paper,
 * envelope, and sticker the user has acquired from Peny Store.
 */
@Composable
fun ProfileStampBookScreen(
    onBackClick: () -> Unit,
    onExploreStoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileStampBookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inspectedItem by remember { mutableStateOf<CollectibleAssetUi?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandInkDeep),
    ) {
        LetterlyScreenHeader(
            title = stringResource(R.string.profile_keepsake_title),
            onBackClick = onBackClick,
        )

        when (val state = uiState) {
            is KeepsakeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandGold)
                }
            }

            is KeepsakeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = state.message,
                            fontFamily = NunitoFontFamily,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                        )
                        Button(
                            onClick = { viewModel.loadInventory() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.profile_keepsake_retry),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = BrandInkDeep,
                            )
                        }
                    }
                }
            }

            is KeepsakeUiState.Success -> {
                KeepsakeAlbumContent(
                    state = state,
                    onSelectCategory = viewModel::selectCategory,
                    onItemClick = { inspectedItem = it },
                    onExploreStoreClick = onExploreStoreClick,
                )
            }
        }
    }

    inspectedItem?.let { item ->
        KeepsakeInspectDialog(
            item = item,
            onDismiss = { inspectedItem = null },
        )
    }
}

@Composable
private fun KeepsakeAlbumContent(
    state: KeepsakeUiState.Success,
    onSelectCategory: (KeepsakeCategory) -> Unit,
    onItemClick: (CollectibleAssetUi) -> Unit,
    onExploreStoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = state.displayedItems
    val isEnvelopeOrPaperOnly = state.selectedCategory == KeepsakeCategory.PAPER ||
        state.selectedCategory == KeepsakeCategory.ENVELOPE
    val gridColumns = if (isEnvelopeOrPaperOnly) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Top Keepsake Header Banner ─────────────────────────────────────
        item(span = { GridItemSpan(maxLineSpan) }) {
            KeepsakeIntroCard(
                totalStamps = state.totalStampsCount,
                totalPapers = state.totalPapersCount,
                totalStickers = state.totalStickersCount,
                totalEnvelopes = state.totalEnvelopesCount,
            )
        }

        // ── Category Filter Pills ──────────────────────────────────────────
        item(span = { GridItemSpan(maxLineSpan) }) {
            KeepsakeFilterRow(
                selected = state.selectedCategory,
                totalAll = state.allItems.size,
                totalStamps = state.totalStampsCount,
                totalPapers = state.totalPapersCount,
                totalStickers = state.totalStickersCount,
                totalEnvelopes = state.totalEnvelopesCount,
                onSelect = onSelectCategory,
            )
        }

        // ── Empty State or Grid of Collectibles ────────────────────────────
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                KeepsakeEmptyState(onExploreStoreClick = onExploreStoreClick)
            }
        } else {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                CollectibleGridCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.staggerEntrance(index),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/**
 * Top warm introductory card highlighting the nostalgia of snail mail.
 */
@Composable
private fun KeepsakeIntroCard(
    totalStamps: Int,
    totalPapers: Int,
    totalStickers: Int,
    totalEnvelopes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandCardDark,
        border = BorderStroke(1.dp, BrandCardStroke),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = stringResource(R.string.profile_keepsake_badge),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGold,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.profile_keepsake_quote),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 18.sp,
            )

            // Mini stats badges — emoji icon + count, no text label
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (totalStamps > 0) MiniStatBadge(emoji = "🎟", count = totalStamps)
                if (totalPapers > 0) MiniStatBadge(emoji = "📄", count = totalPapers)
                if (totalStickers > 0) MiniStatBadge(emoji = "🌟", count = totalStickers)
                if (totalEnvelopes > 0) MiniStatBadge(emoji = "✉️", count = totalEnvelopes)
            }
        }
    }
}

@Composable
private fun MiniStatBadge(emoji: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF332519), BrandCardDark)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(BrandGold.copy(alpha = 0.3f), BrandCardStroke)
                ),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(text = emoji, fontSize = 13.sp)
        Text(
            text = count.toString(),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = BrandGold,
        )
    }
}

/**
 * Filter pills for selecting stationery category.
 */
@Composable
private fun KeepsakeFilterRow(
    selected: KeepsakeCategory,
    totalAll: Int,
    totalStamps: Int,
    totalPapers: Int,
    totalStickers: Int,
    totalEnvelopes: Int,
    onSelect: (KeepsakeCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(
        Triple(KeepsakeCategory.ALL, stringResource(R.string.profile_keepsake_tab_all), totalAll),
        Triple(KeepsakeCategory.STAMP, stringResource(R.string.profile_keepsake_tab_stamps), totalStamps),
        Triple(KeepsakeCategory.PAPER, stringResource(R.string.profile_keepsake_tab_papers), totalPapers),
        Triple(KeepsakeCategory.STICKER, stringResource(R.string.profile_keepsake_tab_stickers), totalStickers),
        Triple(KeepsakeCategory.ENVELOPE, stringResource(R.string.profile_keepsake_tab_envelopes), totalEnvelopes),
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(filters) { (cat, label, count) ->
            val isSelected = selected == cat
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) BrandGold else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) BrandGold else Color.White.copy(alpha = 0.15f),
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(cat) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) BrandInkDeep else Color.White,
                    )
                    if (count > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) BrandInkDeep.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = count.toString(),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isSelected) BrandInkDeep else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Collectible Card in the Keepsake Album Grid.
 */
@Composable
private fun CollectibleGridCard(
    item: CollectibleAssetUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    val isEnvelope = item.assetType.equals("ENVELOPE", ignoreCase = true)
    val isPaper = item.assetType.equals("PAPER", ignoreCase = true)

    val cardShape = RoundedCornerShape(14.dp)

    Surface(
        shape = cardShape,
        color = BrandCardDark,
        border = BorderStroke(1.dp, BrandCardStroke),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Artwork container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when {
                            isEnvelope -> Modifier.aspectRatio(EnvelopeAspectRatio)
                            isPaper -> Modifier.aspectRatio(3f / 4f)
                            else -> Modifier.aspectRatio(1f)
                        }
                    )
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.assetUrl,
                    contentDescription = item.packTitle,
                    contentScale = if (isEnvelope) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isEnvelope) 0.dp else 8.dp),
                )

                // Category emoji tag — skeuomorphic pill, emoji only
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(BrandInkDeep.copy(alpha = 0.88f))
                        .border(0.5.dp, BrandGold.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = when (item.assetType.uppercase()) {
                            "STAMP" -> "🎟"
                            "PAPER" -> "📄"
                            "STICKER" -> "🌟"
                            "ENVELOPE" -> "✉️"
                            else -> "📦"
                        },
                        fontSize = 11.sp,
                    )
                }
            }

            // Info bottom strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = item.packTitle,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.profile_keepsake_creator_credit, item.creatorName),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp,
                    color = BrandGoldDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Inspect Dialog Physics Constants ─────────────────────────────────────────

/** Max 3D tilt angle in degrees when dragging across the full card width/height. */
private const val MaxTiltDeg = 18f

/** Shimmer sweep animation duration in ms — one full diagonal pass. */
private const val GlossSweepDurationMs = 1_800

/** How long the gloss pauses between sweeps (off-screen dwell). */
private const val GlossPauseDurationMs = 2_400

/**
 * Full inspection dialog when a collectible is tapped.
 *
 * Interactive effects:
 * - **Magnetic Lift** — dialog enters with a bouncy scale-up from 0.85 + subtle translateY,
 *   simulating the item being "picked up from a table".
 * - **Drag-to-Tilt 3D** — drag a finger over the artwork to tilt the card in 3D (rotationX/Y
 *   via graphicsLayer), snaps back when released.
 * - **Light Gloss Sweep** — a semi-transparent diagonal gradient sweeps across the artwork
 *   in a loop, mimicking the holographic sheen of a real stamp/sticker.
 * - **Double-tap Flip** — double-tap the artwork to flip the card 180° revealing metadata
 *   on the back (creator, date, serial hint); double-tap again to flip back.
 */
@Composable
private fun KeepsakeInspectDialog(
    item: CollectibleAssetUi,
    onDismiss: () -> Unit,
) {
    val isEnvelope = item.assetType.equals("ENVELOPE", ignoreCase = true)
    val isPaper = item.assetType.equals("PAPER", ignoreCase = true)
    val isReducedMotion = rememberIsReducedMotion()

    val formattedDate = remember(item.unlockedAt) {
        formatUnlockedDate(item.unlockedAt)
    }

    // ── Magnetic Lift entry ────────────────────────────────────────────────
    val entryScale = remember { Animatable(if (isReducedMotion) 1f else 0.85f) }
    val entryTranslateY = remember { Animatable(if (isReducedMotion) 0f else 40f) }
    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            launch {
                entryScale.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch {
                entryTranslateY.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    // ── Drag-to-Tilt state ─────────────────────────────────────────────────
    val scope = rememberCoroutineScope()
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val tiltX = remember { Animatable(0f) } // rotationX (up/down drag)
    val tiltY = remember { Animatable(0f) } // rotationY (left/right drag)

    // ── Light Gloss Sweep ──────────────────────────────────────────────────
    // glossOffset goes from -1f (shimmer fully off-screen left) to +2f (fully off right)
    val glossOffset = remember { Animatable(-1f) }
    LaunchedEffect(isReducedMotion) {
        if (isReducedMotion) return@LaunchedEffect
        while (true) {
            glossOffset.snapTo(-1f)
            glossOffset.animateTo(
                targetValue = 2f,
                animationSpec = tween(durationMillis = GlossSweepDurationMs),
            )
            // pause off-screen before looping
            kotlinx.coroutines.delay(GlossPauseDurationMs.toLong())
        }
    }

    // ── Double-tap Flip ────────────────────────────────────────────────────
    var isFlipped by remember { mutableStateOf(false) }
    val flipRotation = remember { Animatable(0f) }
    val flipSpec = if (isReducedMotion) tween<Float>(0) else tween(420)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = BrandCardDark,
            border = BorderStroke(1.5.dp, BrandGold.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = entryScale.value
                    scaleY = entryScale.value
                    translationY = entryTranslateY.value
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top header: Type & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Emoji + type badge — skeuomorphic
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandGold.copy(alpha = 0.20f), BrandGold.copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, BrandGold.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = when (item.assetType.uppercase()) {
                                "STAMP" -> "🎟"
                                "PAPER" -> "📄"
                                "STICKER" -> "🌟"
                                "ENVELOPE" -> "✉️"
                                else -> "📦"
                            },
                            fontSize = 13.sp,
                        )
                        Text(
                            text = getCollectibleTypeLabel(item.assetType),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGold,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.profile_keepsake_inspect_close),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // ── Interactive Artwork Hero ───────────────────────────────
                // The entire card flips on double-tap; tilt + gloss apply to
                // whichever face is currently visible.
                val cardAspect = when {
                    isEnvelope -> EnvelopeAspectRatio
                    isPaper -> 3f / 4f
                    else -> 1f
                }

                // Flip container — rotates around Y axis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(cardAspect)
                        .onSizeChanged { cardSize = it }
                        .graphicsLayer {
                            // Tilt (drag) — cameraDistance makes it look 3D
                            rotationX = tiltX.value
                            rotationY = tiltY.value + flipRotation.value
                            cameraDistance = 12f * density
                        }
                        // Drag-to-Tilt gesture
                        .pointerInput(cardSize) {
                            if (isReducedMotion) return@pointerInput
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch { tiltX.animateTo(0f, LetterlySpring.Bouncy) }
                                    scope.launch { tiltY.animateTo(0f, LetterlySpring.Bouncy) }
                                },
                                onDragCancel = {
                                    scope.launch { tiltX.animateTo(0f, LetterlySpring.Bouncy) }
                                    scope.launch { tiltY.animateTo(0f, LetterlySpring.Bouncy) }
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                if (cardSize.width > 0 && cardSize.height > 0) {
                                    val newRotX = (tiltX.value - dragAmount.y / cardSize.height * MaxTiltDeg * 2)
                                        .coerceIn(-MaxTiltDeg, MaxTiltDeg)
                                    val newRotY = (tiltY.value + dragAmount.x / cardSize.width * MaxTiltDeg * 2)
                                        .coerceIn(-MaxTiltDeg, MaxTiltDeg)
                                    scope.launch { tiltX.snapTo(newRotX) }
                                    scope.launch { tiltY.snapTo(newRotY) }
                                }
                            }
                        }
                        // Double-tap Flip gesture
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scope.launch {
                                        val target = if (isFlipped) 0f else 180f
                                        flipRotation.animateTo(target, flipSpec)
                                        isFlipped = !isFlipped
                                    }
                                },
                            )
                        },
                ) {
                    // Determine which face to render based on flip angle
                    // We show back side when rotation is between 90–270°
                    val absRot = (flipRotation.value % 360f + 360f) % 360f
                    val showBack = absRot in 90f..270f

                    if (!showBack) {
                        // ── Front face: artwork + gloss sweep ─────────────
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = item.assetUrl,
                                contentDescription = item.packTitle,
                                contentScale = if (isEnvelope) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isEnvelope) 0.dp else 12.dp),
                            )

                            // ── Light Gloss Sweep overlay ──────────────────
                            // Uses drawWithContent so we have access to actual pixel size —
                            // required for linearGradient Offset to span the full card
                            // (normalised 0–1 coords don't work on Modifier.background).
                            if (!isReducedMotion) {
                                val g = glossOffset.value
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .drawWithContent {
                                            drawContent()
                                            val w = size.width
                                            val h = size.height
                                            // sweep band width ≈ 60% of card width, moves left→right
                                            val bandHalf = w * 0.30f
                                            val centerX = g * (w + bandHalf * 2) - bandHalf
                                            drawRect(
                                                brush = Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to Color.Transparent,
                                                        0.35f to Color.White.copy(alpha = 0.0f),
                                                        0.5f to Color.White.copy(alpha = 0.22f),
                                                        0.65f to Color.White.copy(alpha = 0.0f),
                                                        1.0f to Color.Transparent,
                                                    ),
                                                    start = Offset(centerX - bandHalf, 0f),
                                                    end = Offset(centerX + bandHalf, h),
                                                ),
                                                size = size,
                                            )
                                        },
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1C1206), BrandCardDark),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, Float.POSITIVE_INFINITY),
                                    )
                                )
                                .border(
                                    1.dp,
                                    BrandGold.copy(alpha = 0.25f),
                                    RoundedCornerShape(16.dp),
                                )
                                // Mirror horizontally so text reads correctly through the flip
                                .graphicsLayer { scaleX = -1f },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(12.dp),
                            ) {
                                // Postmark circle decoration — compact size
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(
                                            1.5.dp,
                                            BrandGold.copy(alpha = 0.45f),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = "🏷️", fontSize = 20.sp)
                                }

                                Text(
                                    text = item.packTitle,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    MetaRow(
                                        label = stringResource(R.string.profile_keepsake_meta_creator),
                                        value = item.creatorName,
                                    )
                                    if (formattedDate.isNotBlank()) {
                                        MetaRow(
                                            label = stringResource(R.string.profile_keepsake_meta_collected),
                                            value = formattedDate,
                                        )
                                    }
                                    MetaRow(
                                        label = stringResource(R.string.profile_keepsake_meta_type),
                                        value = getCollectibleTypeLabel(item.assetType),
                                    )
                                }
                            }
                        }
                    }
                }

                // Details: Pack title & Creator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.packTitle,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.profile_keepsake_creator_credit, item.creatorName),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = BrandGold.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                }

                // Badge: Official Store & Date
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(R.string.profile_keepsake_official_badge),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                        }

                        if (formattedDate.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.profile_keepsake_unlocked_date, formattedDate),
                                fontFamily = NunitoFontFamily,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }

                // Close CTA button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_keepsake_inspect_close),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandInkDeep,
                    )
                }
            }
        }
    }
}

/**
 * Small two-column label/value row used on the card back-face metadata panel.
 */
@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = BrandGold.copy(alpha = 0.75f),
        )
        Text(
            text = value,
            fontFamily = NunitoFontFamily,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun getCollectibleTypeLabel(assetType: String): String =
    when (assetType.uppercase()) {
        "STAMP" -> stringResource(R.string.profile_keepsake_type_stamp)
        "PAPER" -> stringResource(R.string.profile_keepsake_type_paper)
        "STICKER" -> stringResource(R.string.profile_keepsake_type_sticker)
        "ENVELOPE" -> stringResource(R.string.profile_keepsake_type_envelope)
        else -> assetType
    }

/**
 * Empty state when the user has no collectibles yet.
 */
@Composable
private fun KeepsakeEmptyState(
    onExploreStoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_keepsake_album),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )

        Text(
            text = stringResource(R.string.profile_keepsake_empty_title),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.profile_keepsake_empty_body),
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onExploreStoreClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BuyButtonBlue),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.profile_keepsake_empty_cta),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
            )
        }
    }
}

private fun formatUnlockedDate(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val parsed = Instant.parse(isoString)
        val zdt = parsed.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()).format(zdt)
    } catch (e: Exception) {
        isoString.substringBefore("T")
    }
}
