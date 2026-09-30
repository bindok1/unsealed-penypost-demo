package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.stamps.state.AssetUi
import com.apps.unsealed.ui.screens.stamps.state.CatalogItemUi
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BuyButtonBlue
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import java.text.NumberFormat
import java.util.Locale

/**
 * A product item in the Peny Store catalog.
 * Renders the product directly without an enclosing card container:
 * - Row 1: Title + Buy pill (PriceTag) in 1 row.
 * - Row 2: Creator name ("oleh ...") + Like button with count.
 * - Row 3: Sub-description.
 * - Horizontally scrollable list of the product's artworks (assets: stamps, papers, stickers, envelopes),
 *   sized compactly (~124dp).
 * Tapping any artwork or the product opens [ProductDetailBottomSheet].
 */
@Composable
fun ProductCard(
    item: CatalogItemUi,
    onClick: () -> Unit,
    onBuyClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    localizedPrice: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
    ) {
        // ── Row 1: Title + Buy Pill ──────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = item.title,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PriceTag(
                priceIdr = item.priceIdr,
                onClick = onBuyClick,
                isLoading = isPurchasing,
                isOwned = item.viewerOwns,
                localizedPrice = localizedPrice,
            )
        }

        Spacer(Modifier.height(3.dp))

        // ── Row 2: Creator Name + Likes ──────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.store_by_creator, item.creatorName),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                color = BrandGold.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLikeClick,
                ),
            ) {
                Icon(
                    imageVector = if (item.viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (item.viewerHasLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "${item.likesCount}",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }

        // ── Row 3: Description ───────────────────────────────────────────────
        if (item.subDescription.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = item.subDescription,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── Horizontally Scrollable Artworks (List Karyanya) ─────────────────
        // Directly renders only authentic artworks (stamps, papers, etc.).
        // Packaging thumbnails are completely eliminated and never loaded.
        if (item.assets.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                item.assets.forEach { asset ->
                    ArtworkStampItem(
                        asset = asset,
                        totalCount = item.assets.size,
                        onClick = onClick,
                        itemCategory = item.category,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkStampItem(
    asset: AssetUi,
    totalCount: Int,
    onClick: () -> Unit,
    itemCategory: String = "",
) {
    val isEnvelope = asset.assetType.equals("ENVELOPE", ignoreCase = true) ||
        asset.assetType.equals("AMPLOP", ignoreCase = true) ||
        itemCategory.equals("ENVELOPE", ignoreCase = true) ||
        itemCategory.equals("AMPLOP", ignoreCase = true)

    val itemModifier = if (isEnvelope) {
        Modifier
            .height(115.dp)
            .aspectRatio(EnvelopeAspectRatio)
    } else {
        Modifier
            .height(115.dp)
            .defaultMinSize(minWidth = 86.dp)
    }

    Box(
        modifier = itemModifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = asset.assetUrl,
            contentDescription = null,
            contentScale = if (isEnvelope) ContentScale.Crop else ContentScale.Fit,
            modifier = (if (isEnvelope) Modifier.fillMaxSize() else Modifier.height(115.dp).wrapContentWidth())
                .clip(RoundedCornerShape(12.dp)),
        )

        if (asset.assetType.isNotBlank() && totalCount > 1) {
            val typeLabel = when (asset.assetType.uppercase()) {
                "STAMP" -> stringResource(R.string.store_filter_prangko)
                "PAPER" -> stringResource(R.string.store_filter_kertas)
                "STICKER" -> stringResource(R.string.store_filter_stiker)
                "ENVELOPE" -> stringResource(R.string.store_filter_amplop)
                else -> asset.assetType
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BrandInkDeep.copy(alpha = 0.75f),
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.BottomStart),
            ) {
                Text(
                    text = typeLabel,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = BrandGold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Formats a Long as Indonesian Rupiah using the system's currency formatter.
 * e.g. 35000 → "Rp35.000"
 * The "Rp" symbol comes from [java.util.Currency] — not hardcoded — so it
 * respects the IDR currency definition from the JVM/Android runtime.
 */
fun Long.toIdrCurrencyString(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

/**
 * Price shown as a filled pill / buy CTA directly on the catalog feed.
 * Clicking directly initiates the purchase flow.
 *
 * [localizedPrice] — when non-null, this pre-formatted string from RevenueCat
 * (e.g. "$3.99", "₱89") is shown directly. Falls back to [priceIdr] formatted
 * as IDR only when RC hasn't loaded yet.
 */
@Composable
fun PriceTag(
    priceIdr: Long,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isOwned: Boolean = false,
    localizedPrice: String? = null,
) {
    val containerColor = when {
        isOwned -> Color.White.copy(alpha = 0.15f)
        else -> BuyButtonBlue
    }
    val contentColor = when {
        isOwned -> Color.White.copy(alpha = 0.7f)
        else -> Color.White
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier.clickable(
            enabled = !isLoading && !isOwned,
            onClick = onClick,
        ),
    ) {
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(13.dp),
                )
            }
        } else if (isOwned) {
            Text(
                text = stringResource(R.string.store_cta_owned),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
        } else {
            Text(
                text = localizedPrice ?: priceIdr.toIdrCurrencyString(),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * A featured rounded rectangle banner card in the Peny Store catalog.
 * Rendered periodically (e.g. every 3-4 items) for visual rhythm:
 * - Rounded rectangle shape (16:9 aspect ratio) with soft rounded corners (18dp).
 * - Uses the product's thumbnail as full-bleed background image.
 * - Title, sub-description, creator name, and Buy CTA (PriceTag) are overlaid
 *   on top of the thumbnail background with a dark gradient scrim for readability.
 * - Like button with count is placed at the top-right.
 */
@Composable
fun FeaturedThumbnailProductCard(
    item: CatalogItemUi,
    onClick: () -> Unit,
    onBuyClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    localizedPrice: String? = null,
) {
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        shape = cardShape,
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.22f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageSource = item.thumbnailUrl.ifBlank {
                item.assets.firstOrNull()?.assetUrl.orEmpty()
            }
            if (imageSource.isNotBlank()) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Dark gradient scrim for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.35f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = 0.60f),
                            1.0f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )

            // Top-right like button pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = if (item.viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.viewerHasLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${item.likesCount}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }
            }

            // Bottom overlay: SubDescription, Title, Creator, PriceTag
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (item.subDescription.isNotBlank()) {
                    Text(
                        text = item.subDescription,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = item.title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.store_by_creator, item.creatorName),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = BrandGold.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    PriceTag(
                        priceIdr = item.priceIdr,
                        onClick = onBuyClick,
                        isLoading = isPurchasing,
                        isOwned = item.viewerOwns,
                        localizedPrice = localizedPrice,
                    )
                }
            }
        }
    }
}

/**
 * A full-width square card in the Peny Store catalog used when filtering by Paper.
 * - Square shape (1:1 aspect ratio) spanning full width.
 * - Rounded corners (18dp) for a soft feel.
 * - Uses the product's thumbnail as full background.
 * - Title, description, creator, and Buy CTA (PriceTag) are overlaid
 *   on top of the thumbnail background with a dark gradient scrim for readability.
 * - Like button is placed at the top-right.
 */
@Composable
fun SquarePaperProductCard(
    item: CatalogItemUi,
    onClick: () -> Unit,
    onBuyClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    localizedPrice: String? = null,
) {
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        shape = cardShape,
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.22f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageSource = item.thumbnailUrl.ifBlank {
                item.assets.firstOrNull()?.assetUrl.orEmpty()
            }
            if (imageSource.isNotBlank()) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Dark gradient scrim for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.72f to Color.Black.copy(alpha = 0.60f),
                            1.0f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )

            // Top-right like button pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = if (item.viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.viewerHasLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${item.likesCount}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }

            // Bottom overlay: SubDescription, Title, Creator, PriceTag
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (item.subDescription.isNotBlank()) {
                    Text(
                        text = item.subDescription,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                }

                Text(
                    text = item.title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.store_by_creator, item.creatorName),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = BrandGold.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    PriceTag(
                        priceIdr = item.priceIdr,
                        onClick = onBuyClick,
                        isLoading = isPurchasing,
                        isOwned = item.viewerOwns,
                        localizedPrice = localizedPrice,
                    )
                }
            }
        }
    }
}

/**
 * A full-width landscape card in the Peny Store catalog used for envelope items.
 * - Landscape shape with [EnvelopeAspectRatio] (1720:900 / ~1.91:1) spanning full width.
 * - Rounded corners (18dp) with subtle border.
 * - Uses the envelope's thumbnail (or first asset) as full-bleed background.
 * - Title, sub-description, creator name, and Buy CTA (PriceTag) are overlaid
 *   on top of the thumbnail background with a dark gradient scrim for readability.
 * - Like button is placed at the top-right.
 */
@Composable
fun LandscapeEnvelopeProductCard(
    item: CatalogItemUi,
    onClick: () -> Unit,
    onBuyClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    localizedPrice: String? = null,
) {
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        shape = cardShape,
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.22f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(EnvelopeAspectRatio)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageSource = item.thumbnailUrl.ifBlank {
                item.assets.firstOrNull()?.assetUrl.orEmpty()
            }
            if (imageSource.isNotBlank()) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Dark gradient scrim for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.30f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.60f),
                            1.0f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )

            // Top-right like button pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = if (item.viewerHasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.viewerHasLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${item.likesCount}",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                    )
                }
            }

            // Bottom overlay: SubDescription, Title, Creator, PriceTag
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (item.subDescription.isNotBlank()) {
                    Text(
                        text = item.subDescription,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }

                Text(
                    text = item.title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.store_by_creator, item.creatorName),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = BrandGold.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    PriceTag(
                        priceIdr = item.priceIdr,
                        onClick = onBuyClick,
                        isLoading = isPurchasing,
                        isOwned = item.viewerOwns,
                        localizedPrice = localizedPrice,
                    )
                }
            }
        }
    }
}

