package com.apps.unsealed.ui.screens.penpals.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.screens.selectrecipient.widgets.StampImage
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.PaperCream

@Composable
fun PenPalsGridContent(
    letters: List<PenpalLetter>,
    hasArchive: Boolean,
    onLetterClick: (Int) -> Unit,
    onArchiveClick: () -> Unit,
    stickerCatalog: List<CatalogItemDto>,
    stampCatalog: List<CatalogItemDto>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(letters.size) { index ->
            GridLetterCard(
                letter = letters[index],
                onClick = { onLetterClick(index) },
                stickerCatalog = stickerCatalog,
                stampCatalog = stampCatalog,
            )
        }
        if (hasArchive) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ArchiveBottomCard(
                    onClick = onArchiveClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun GridLetterCard(
    letter: PenpalLetter,
    onClick: () -> Unit,
    stickerCatalog: List<CatalogItemDto>,
    stampCatalog: List<CatalogItemDto>,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val regionContentDescription = stringResource(
        R.string.penpals_card_region_label,
        stringResource(letter.region.labelRes),
    )

    Box(
        modifier = modifier
            .aspectRatio(EnvelopeAspectRatio)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Mirrors DeskCard.kt's fallback reconstruction — see
        // docs/penpals-screen-spec.md §Context.
        if (letter.envelopeCompositeImageUrl != null) {
            AsyncImage(
                model = letter.envelopeCompositeImageUrl,
                contentDescription = regionContentDescription,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(letter.envelope.drawableRes),
                error = painterResource(letter.envelope.drawableRes),
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                ,
            )
        } else {
            AsyncImage(
                model = letter.envelopeImageUrl,
                contentDescription = regionContentDescription,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(letter.envelope.drawableRes),
                error = painterResource(letter.envelope.drawableRes),
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                text = letter.senderName,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = CaveatFontFamily,
                fontSize = 13.sp,
                color = InkDefault,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
            Text(
                text = stringResource(R.string.penpals_card_region_label, stringResource(letter.region.labelRes)),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = CaveatFontFamily,
                fontSize = 14.sp,
                color = InkDefault,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
            )
            letter.stampId?.let { id ->
                stampCatalog.find { it.id == id }?.let { stamp ->
                    val isLandscape = stamp.isLandscape
                    StampImage(
                        item = stamp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(
                                width = if (isLandscape) 26.dp else 22.dp,
                                height = if (isLandscape) 20.dp else 26.dp,
                            ),
                    )
                }
            }
            letter.envelopeStickerId?.let { id ->
                stickerCatalog.find { it.id == id }?.let { sticker ->
                    StampImage(
                        item = sticker,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Bottom-of-stack teaser offering an early reroll of today's stack for
 * energy (`POST /penpals/feed/refresh`, `docs/be/daily_penpal_stack.md` §6)
 * — shown alongside the regular stack, not gating anything behind it.
 */
@Composable
fun ArchiveBottomCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(PaperCream, shape)
            .border(1.5.dp, BrandGold, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.penpals_refresh_card_icon_desc),
            tint = BrandGold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.penpals_refresh_card_title),
            color = BrandInk,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.penpals_refresh_card_subtitle),
            color = BrandInk.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
