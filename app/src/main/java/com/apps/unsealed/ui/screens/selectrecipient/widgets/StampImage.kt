package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.StampDesign

import kotlinx.coroutines.CancellationException

/** Renders a CMS-driven catalog [item] (`feature/catalog/data/`): real art
 * via Coil when the catalog provides one (CMS-uploaded stamps always will),
 * or [StampSwatch]'s procedural drawing — keyed off whichever legacy
 * [StampDesign] shares [item]'s id, falling back to [StampDesign.INK] for an
 * id with neither an image nor a legacy match. The swatch fallback covers
 * two cases: [CatalogItemDto.imageUrl] being blank (bundled placeholders),
 * and a non-blank URL that fails to load (404/unreachable CMS asset) — both
 * degrade to the same procedural stamp instead of an empty/broken tile. */
@Composable
fun StampImage(item: CatalogItemDto, modifier: Modifier = Modifier, edgeColor: Color = Color(0xFFF5EDD8)) {
    val design = remember(item.id) { StampDesign.fromApiString(item.id) }
    var isImageBroken by remember(item.imageUrl) { mutableStateOf(false) }

    if (item.imageUrl.isBlank() || isImageBroken) {
        StampSwatch(design = design, modifier = modifier, edgeColor = edgeColor)
    } else {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            onState = { state ->
                if (state is AsyncImagePainter.State.Error) {
                    val throwable = state.result.throwable
                    if (throwable !is CancellationException) {
                        isImageBroken = true
                    }
                }
            },
            modifier = modifier,
        )
    }
}

private const val PerforationsPerEdge = 6

/** Draws [design]'s accent-color rect with a ring of paper-colored punched
 * circles along each edge to fake a postage stamp's scalloped border. */
@Composable
fun StampSwatch(
    design: StampDesign,
    modifier: Modifier = Modifier,
    edgeColor: Color = Color(0xFFF5EDD8),
) {
    Canvas(modifier.fillMaxSize()) {
        drawRoundRect(color = design.accentColor, cornerRadius = CornerRadius(size.minDimension * 0.08f))

        val holeRadius = size.minDimension * 0.06f
        val stepX = size.width / PerforationsPerEdge
        val stepY = size.height / PerforationsPerEdge
        for (i in 0 until PerforationsPerEdge) {
            val x = stepX * (i + 0.5f)
            drawCircle(edgeColor, holeRadius, Offset(x, 0f))
            drawCircle(edgeColor, holeRadius, Offset(x, size.height))
        }
        for (i in 0 until PerforationsPerEdge) {
            val y = stepY * (i + 0.5f)
            drawCircle(edgeColor, holeRadius, Offset(0f, y))
            drawCircle(edgeColor, holeRadius, Offset(size.width, y))
        }
    }
}
