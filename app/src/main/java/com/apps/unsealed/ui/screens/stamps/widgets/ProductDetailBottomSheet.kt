package com.apps.unsealed.ui.screens.stamps.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.screens.stamps.state.ItemDetailUi
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BuyButtonBlue
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay

/**
 * Full-screen bottom sheet showing product detail: banner, description, asset
 * preview strip, creator card, like toggle, and Buy / Owned CTA.
 *
 * [onViewItem] is called after a 1.5s delay (non-blocking view increment).
 * [onToggleLike] triggers an optimistic like flip in the ViewModel.
 * [onBuyClick] should open the RevenueCat paywall for this item's tier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailBottomSheet(
    detail: ItemDetailUi?,
    isLoadingDetail: Boolean,
    isPurchasing: Boolean = false,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Localized price string from RevenueCat (e.g. "$3.99", "₱89"). Null → falls back to IDR. */
    localizedPrice: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Non-blocking view increment after 1.5s
    LaunchedEffect(detail?.id) {
        if (detail != null) {
            delay(1_500)
            // ViewModel already calls incrementViewAsync on onProductTapped;
            // this LaunchedEffect is a safety net if detail arrives after 1.5s.
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandInkDeep,
        modifier = modifier,
    ) {
        if (isLoadingDetail || detail == null) {
            ProductDetailSkeleton()
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header: Title, like button, description & Buy CTA ───────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Title + Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = detail.title,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleLike,
                        ),
                    ) {
                        Icon(
                            imageVector = if (detail.viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (detail.viewerHasLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${detail.likesCount}",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }

                if (detail.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = detail.description,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 19.sp,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Buy / Owned CTA on top ──────────────────────────────────
                DetailBuyCta(
                    detail = detail,
                    isPurchasing = isPurchasing,
                    onBuyClick = onBuyClick,
                    localizedPrice = localizedPrice,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Hero: Full-width artwork pager ──────────────────────────────
            // Handcrafted artworks rendered full width so intricate handmade
            // texture and perforations can be clearly inspected.
            val heroAssetUrls = detail.assets.map { it.assetUrl }
                .ifEmpty { listOfNotNull(detail.bannerUrl.ifBlank { null }) }

            val isEnvelope = detail.category.equals("envelope", ignoreCase = true) ||
                detail.assets.any { it.assetType.equals("envelope", ignoreCase = true) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEnvelope) {
                            Modifier
                                .padding(horizontal = 20.dp)
                                .aspectRatio(EnvelopeAspectRatio)
                        } else {
                            Modifier.height(520.dp)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val pagerState = rememberPagerState(pageCount = { heroAssetUrls.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isEnvelope) Modifier.fillMaxSize()
                                else Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = heroAssetUrls[page],
                            contentDescription = detail.title,
                            contentScale = if (isEnvelope) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }
                }

                // Asset type badge on top of artwork
                if (detail.assets.isNotEmpty() && pagerState.currentPage < detail.assets.size) {
                    val currentAsset = detail.assets[pagerState.currentPage]
                    val typeLabel = when (currentAsset.assetType.uppercase()) {
                        "STAMP" -> stringResource(R.string.store_filter_prangko)
                        "PAPER" -> stringResource(R.string.store_filter_kertas)
                        "STICKER" -> stringResource(R.string.store_filter_stiker)
                        "ENVELOPE" -> stringResource(R.string.store_filter_amplop)
                        else -> currentAsset.assetType
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandInkDeep.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                    ) {
                        Text(
                            text = "$typeLabel ${pagerState.currentPage + 1}/${heroAssetUrls.size}",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Dot indicators if more than 1 asset
                if (heroAssetUrls.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        heroAssetUrls.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == pagerState.currentPage) BrandGold
                                        else Color.White.copy(alpha = 0.45f),
                                    ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Below Artwork: Sub-description, Badges, Creator ─────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Sub-description under the artwork
                if (detail.subDescription.isNotBlank()) {
                    Text(
                        text = detail.subDescription,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = BrandGold.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // ── Asset count badges ──────────────────────────────────────
                AssetCountBadgeRow(detail)

                Spacer(Modifier.height(14.dp))

                // ── Creator card ────────────────────────────────────────────
                val hasExternalLink = detail.creator.externalLink.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ToolbarFrostedDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (hasExternalLink) {
                                Modifier.clickable {
                                    openCreatorLink(context, detail.creator.externalLink)
                                }
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        // Avatar
                        if (detail.creator.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = detail.creator.avatarUrl,
                                contentDescription = detail.creator.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = detail.creator.name.take(1).uppercase(),
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = BrandGold,
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.creator.name,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (detail.creator.bio.isNotBlank()) {
                                Text(
                                    text = detail.creator.bio,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        // External link icon
                        if (hasExternalLink) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = stringResource(R.string.store_view_external_link),
                                    tint = BrandGold,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Buy / Owned CTA shown full-width at the bottom of the product detail sheet.
 */
@Composable
private fun DetailBuyCta(
    detail: ItemDetailUi,
    isPurchasing: Boolean,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
    localizedPrice: String? = null,
) {
    if (detail.viewerOwns) {
        Button(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                disabledContentColor = Color.White.copy(alpha = 0.6f),
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.store_cta_owned),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Button(
            onClick = onBuyClick,
            enabled = !isPurchasing,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyButtonBlue,
                contentColor = Color.White,
                disabledContainerColor = BuyButtonBlue.copy(alpha = 0.6f),
                disabledContentColor = Color.White,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isPurchasing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                val priceText = localizedPrice ?: detail.priceIdr.toIdrCurrencyString()
                Text(
                    text = stringResource(R.string.store_cta_buy, priceText),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Asset count badge row ─────────────────────────────────────────────────────

@Composable
private fun AssetCountBadgeRow(detail: ItemDetailUi) {
    val badges = buildList {
        if (detail.stampsCount > 0) add(stringResource(R.string.store_badge_stamps_count, detail.stampsCount))
        if (detail.stickersCount > 0) add(stringResource(R.string.store_badge_stickers_count, detail.stickersCount))
        if (detail.papersCount > 0) add(stringResource(R.string.store_badge_paper_count, detail.papersCount))
        if (detail.envelopesCount > 0) add(stringResource(R.string.store_badge_envelopes_count, detail.envelopesCount))
    }
    if (badges.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        badges.forEach { label ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandGold.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = label,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = BrandGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Detail skeleton (shown while isLoadingDetail = true) ─────────────────────

@Composable
private fun ProductDetailSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title, description & CTA placeholder (top)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(Modifier.fillMaxWidth(0.7f).height(22.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.07f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.07f)))

            Spacer(Modifier.height(14.dp))

            // CTA placeholder on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.09f)),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Artwork placeholder (full width, 520dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Sub-desc placeholder
            Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.07f)))

            Spacer(Modifier.height(14.dp))

            // Badges placeholder
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.06f)))
                Box(Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.06f)))
            }

            Spacer(Modifier.height(14.dp))

            // Creator card placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Safely parses and launches an external creator link.
 * Handles missing http/https schemes, social handle syntax (@username),
 * and prevents ActivityNotFoundException crashes.
 */
private fun openCreatorLink(context: Context, rawLink: String) {
    val trimmed = rawLink.trim()
    if (trimmed.isBlank()) return

    val url = when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("@") -> "https://instagram.com/${trimmed.removePrefix("@")}"
        else -> "https://$trimmed"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.store_error_open_link),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
