package com.apps.unsealed.ui.components.collectibles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.SurfaceCardLight

/**
 * Teaser representation of a creator collectible item for empty-state showcases.
 */
data class CollectibleTeaserItem(
    val id: String,
    val title: String,
    val creatorName: String? = null,
    val imageUrl: String? = null,
    val drawableRes: Int? = null,
)

/**
 * Category header for item picker sheets (e.g. "KOLEKSI DASAR" vs "KOLEKSI SAYA").
 */
@Composable
fun CollectibleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    countText: String? = null,
    isCreatorSection: Boolean = false,
    isDarkTheme: Boolean = false,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isCreatorSection) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(14.dp),
                )
            }

            Text(
                text = title.uppercase(),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = if (isDarkTheme) {
                    if (isCreatorSection) BrandGold else Color.White.copy(alpha = 0.7f)
                } else {
                    if (isCreatorSection) Color(0xFFB45309) else BrandInk.copy(alpha = 0.7f)
                },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (countText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkTheme) BrandGold.copy(alpha = 0.15f) else BrandGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = countText,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (isDarkTheme) BrandGold else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            if (onActionClick != null && actionText != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = actionText,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDarkTheme) BrandGold else Color(0xFFB45309),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isDarkTheme) BrandGold else Color(0xFFB45309),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}

/**
 * Trailing card shown at the end of the user's owned collectibles row.
 * Invites them to explore more creator pieces in the Peny Store.
 */
@Composable
fun FindMoreCollectiblesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    title: String = stringResource(R.string.picker_collectibles_find_more_card_title),
    subtitle: String = stringResource(R.string.picker_collectibles_find_more_short),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    val bgColor = if (isDarkTheme) Color.White.copy(alpha = 0.06f) else SurfaceCardLight
    val borderColor = if (isDarkTheme) BrandGold.copy(alpha = 0.45f) else BrandGold.copy(alpha = 0.6f)
    val titleColor = if (isDarkTheme) BrandGold else Color(0xFFB45309)
    val subColor = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else BrandInkDeep

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = if (isDarkTheme) 0.2f else 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDarkTheme) BrandGold else Color(0xFFB45309),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = titleColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = subColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Showcase banner shown in the "Koleksi Saya" section when the user hasn't bought
 * any creator collectibles yet. Displays preview teaser cards with lock badges,
 * inspiring copy, and a prominent "Beli Koleksi" CTA button.
 */
@Composable
fun CollectibleEmptyShowcaseCard(
    teaserItems: List<CollectibleTeaserItem>,
    onBuyCollectiblesClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    title: String = stringResource(R.string.picker_collectibles_empty_title),
    description: String = stringResource(R.string.picker_collectibles_empty_desc),
    ctaText: String = stringResource(R.string.picker_collectibles_find_more_short),
) {
    val cardBg = if (isDarkTheme) {
        Color.White.copy(alpha = 0.05f)
    } else {
        SurfaceCardLight.copy(alpha = 0.95f)
    }

    val borderColor = if (isDarkTheme) {
        BrandGold.copy(alpha = 0.35f)
    } else {
        BrandGold.copy(alpha = 0.45f)
    }

    val primaryTextColor = if (isDarkTheme) Color.White else BrandInkDeep
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.75f) else BrandInk.copy(alpha = 0.75f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── Teaser preview row ──────────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(teaserItems, key = { it.id }) { item ->
                    TeaserItemThumbnail(
                        item = item,
                        onClick = onBuyCollectiblesClick,
                        isDarkTheme = isDarkTheme,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Headline & Description ─────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = primaryTextColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = secondaryTextColor,
                    )
                }

                Spacer(Modifier.width(12.dp))

                // ── CTA Button ─────────────────────────────────────────────
                Button(
                    onClick = onBuyCollectiblesClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) BrandGold else Color(0xFFB45309),
                        contentColor = if (isDarkTheme) BrandInkDeep else Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Text(
                        text = ctaText,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Direct CTA banner placed underneath a collectibles section.
 * Presents aesthetic headline, description, and "Find More →" button in a cohesive card,
 * without displaying teaser items in a horizontal list.
 */
@Composable
fun CollectibleCtaBanner(
    title: String,
    description: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
) {
    val cardBg = if (isDarkTheme) {
        Color.White.copy(alpha = 0.05f)
    } else {
        SurfaceCardLight.copy(alpha = 0.95f)
    }

    val borderColor = if (isDarkTheme) {
        BrandGold.copy(alpha = 0.35f)
    } else {
        BrandGold.copy(alpha = 0.45f)
    }

    val primaryTextColor = if (isDarkTheme) Color.White else BrandInkDeep
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.75f) else BrandInk.copy(alpha = 0.75f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = primaryTextColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = secondaryTextColor,
                )
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onCtaClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BrandGold else Color(0xFFB45309),
                    contentColor = if (isDarkTheme) BrandInkDeep else Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp),
            ) {
                Text(
                    text = ctaText,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TeaserItemThumbnail(
    item: CollectibleTeaserItem,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val thumbShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .size(width = 68.dp, height = 80.dp)
            .clip(thumbShape)
            .background(if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White)
            .border(
                1.dp,
                if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                thumbShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (item.drawableRes != null) {
            Image(
                painter = painterResource(item.drawableRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.title.take(1),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = BrandGold,
                )
            }
        }

        // Dark scrim overlay for preview feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
        )

        // Lock icon on top right
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.picker_badge_creator),
                tint = BrandGold,
                modifier = Modifier.size(10.dp),
            )
        }

        // Title at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .align(Alignment.BottomCenter),
        ) {
            Text(
                text = item.title,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Default Teaser Data Providers ─────────────────────────────────────────────

fun samplePaperTeasers(): List<CollectibleTeaserItem> = listOf(
    CollectibleTeaserItem("teaser_paper_1", "Autumn Kraft", "Studio Kembara", drawableRes = R.drawable.kertas_kraft_tua),
    CollectibleTeaserItem("teaser_paper_2", "Linen Buram", "Artisan Post", drawableRes = R.drawable.kertas_buram_halus),
    CollectibleTeaserItem("teaser_paper_3", "Indigo Night", "Studio Kembara", drawableRes = R.drawable.blue_paper_texture),
)

fun sampleStickerTeasers(): List<CollectibleTeaserItem> = listOf(
    CollectibleTeaserItem("teaser_sticker_1", "Peny Wave", "Studio Kembara", drawableRes = R.drawable.peny_swim),
    CollectibleTeaserItem("teaser_sticker_2", "Peny Stroll", "Peny Studio", drawableRes = R.drawable.peny_walk),
    CollectibleTeaserItem("teaser_sticker_3", "Little Fish", "Artisan Post", drawableRes = R.drawable.ic_fish),
)

fun sampleEnvelopeTeasers(): List<CollectibleTeaserItem> = listOf(
    CollectibleTeaserItem("teaser_env_1", "Maple Kraft", "Studio Kembara", drawableRes = R.drawable.envelope_1),
    CollectibleTeaserItem("teaser_env_2", "Indigo Night", "Peny Studio", drawableRes = R.drawable.envelope_2),
    CollectibleTeaserItem("teaser_env_3", "Botanical Herb", "Artisan Post", drawableRes = R.drawable.envelope_3),
)

fun sampleStampTeasers(): List<CollectibleTeaserItem> = listOf(
    CollectibleTeaserItem("teaser_stamp_1", "Autumn Leaf", "Studio Kembara"),
    CollectibleTeaserItem("teaser_stamp_2", "Polar Airmail", "Arctic Studio"),
    CollectibleTeaserItem("teaser_stamp_3", "Golden Vintage", "Artisan Pos"),
)
