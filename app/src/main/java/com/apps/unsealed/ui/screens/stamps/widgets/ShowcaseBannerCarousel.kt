package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.apps.unsealed.ui.screens.stamps.state.ShowcaseItemUi
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay

/**
 * 16:9 Showcase carousel card for the Peny Store catalog.
 * Displays admin-curated promo items with auto-advance every 4 seconds,
 * crossfade transitions, title, creator, price pill, and dot indicators.
 * Tapping anywhere on the card opens product detail.
 * Tapping the price pill triggers purchase directly.
 */
@Composable
fun ShowcaseBannerCarousel(
    items: List<ShowcaseItemUi>,
    onItemClick: (String) -> Unit,
    onBuyClick: (ShowcaseItemUi) -> Unit = {},
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    purchasingItemId: String? = null,
) {
    if (items.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance every 4 seconds when multiple items exist; pause while purchasing
    LaunchedEffect(items.size, isPurchasing) {
        if (items.size <= 1 || isPurchasing) return@LaunchedEffect
        while (true) {
            delay(4_000)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    val currentItem = items.getOrElse(currentIndex) { items.first() }
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        shape = cardShape,
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.22f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onItemClick(currentItem.id) },
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "showcaseCrossfade",
                modifier = Modifier.fillMaxSize(),
            ) { item ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = item.bannerUrl.ifBlank { item.thumbnailUrl },
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Bottom gradient scrim for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.30f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
            )

            // Content overlay (sub-description, title, creator, price)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (currentItem.subDescription.isNotBlank()) {
                    Text(
                        text = currentItem.subDescription,
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
                    text = currentItem.title,
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
                        text = stringResource(R.string.store_by_creator, currentItem.creatorName),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = BrandGold.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    PriceTag(
                        priceIdr = currentItem.priceIdr,
                        onClick = { onBuyClick(currentItem) },
                        isLoading = isPurchasing && purchasingItemId == currentItem.id,
                        isOwned = currentItem.viewerOwns,
                    )
                }

                // Dot indicators
                if (items.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (i == currentIndex) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentIndex) BrandGold
                                        else Color.White.copy(alpha = 0.35f),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
