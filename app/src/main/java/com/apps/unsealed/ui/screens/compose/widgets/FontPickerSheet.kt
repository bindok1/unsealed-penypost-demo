package com.apps.unsealed.ui.screens.compose.widgets

import com.apps.unsealed.ui.screens.compose.constants.*
import com.apps.unsealed.ui.screens.compose.state.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FontCardBackground = Color(0xFFF2F0EA)
private val FontCardBorderSelected = Color(0xFF1F2937)

/** Fresh Dp-typed springs mirroring [LetterlySpring]'s constants — the shared
 * object only exposes `spring<Float>`, which can't back `animateDpAsState`. */
private val SnappyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
private val BouncyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

/**
 * Real Font Picker sheet (compose-screen-spec.md §3), replacing the
 * ComingSoonBottomSheet placeholder. Only [LetterFont.CAVEAT] has a real font
 * asset today — the other 5 entries render with whatever [LetterFont.fontFamily]
 * they currently resolve to (Caveat, per their TODO(P1) comments in
 * ComposeUiState.kt) until real font files are added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPickerSheet(
    selectedFont: LetterFont,
    selectedGoogleFontName: String? = null,
    googleFontStatuses: Map<String, FontDownloadStatus> = emptyMap(),
    onFontSelect: (LetterFont) -> Unit,
    onGoogleFontDownloadStart: (String) -> Unit = {},
    onGoogleFontDownloaded: (String) -> Unit = {},
    onGoogleFontDownloadFailed: (String) -> Unit = {},
    onGoogleFontSelect: (String) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val fonts = LetterFont.entries
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.font_picker_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            fonts.chunked(3).forEachIndexed { rowIndex, rowFonts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowFonts.forEachIndexed { colIndex, font ->
                        FontPickerCard(
                            font = font,
                            isSelected = selectedGoogleFontName == null && font == selectedFont,
                            index = rowIndex * 3 + colIndex,
                            isReducedMotion = isReducedMotion,
                            onClick = { onFontSelect(font) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (rowIndex == 0) Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.font_picker_more_fonts_label), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            DownloadableFontCatalog.chunked(3).forEachIndexed { rowIndex, rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowEntries.forEach { entry ->
                        val status = googleFontStatuses[entry.googleFontName] ?: FontDownloadStatus.NOT_DOWNLOADED
                        GoogleFontCard(
                            entry = entry,
                            status = status,
                            isSelected = selectedGoogleFontName == entry.googleFontName,
                            isReducedMotion = isReducedMotion,
                            onClick = {
                                when (status) {
                                    FontDownloadStatus.NOT_DOWNLOADED, FontDownloadStatus.FAILED -> {
                                        onGoogleFontDownloadStart(entry.googleFontName)
                                        scope.launch {
                                            runCatching { downloadGoogleFont(context, entry.googleFontName) }
                                                .onSuccess { onGoogleFontDownloaded(entry.googleFontName) }
                                                .onFailure { onGoogleFontDownloadFailed(entry.googleFontName) }
                                        }
                                    }
                                    FontDownloadStatus.DOWNLOADED -> onGoogleFontSelect(entry.googleFontName)
                                    FontDownloadStatus.DOWNLOADING -> Unit
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Pad the last row so cards keep a consistent width when the
                    // catalog size isn't a multiple of 3.
                    repeat(3 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
                }
                if (rowIndex < DownloadableFontCatalog.chunked(3).lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FontPickerCard(
    font: LetterFont,
    isSelected: Boolean,
    index: Int,
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        isVisible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-20).dp,
        animationSpec = reducedMotionSpring(SnappyDpSpring, isReducedMotion),
        label = "fontCardOffsetX",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "fontCardAlpha",
    )

    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.05f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
            scale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        } else {
            scale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        }
    }
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = reducedMotionSpring(BouncyDpSpring, isReducedMotion),
        label = "fontCardBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) FontCardBorderSelected else FontCardBorderSelected.copy(alpha = 0.15f),
        label = "fontCardBorderColor",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.toPx()
                this.alpha = alpha
                scaleX = scale.value
                scaleY = scale.value
            }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(FontCardBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.font_picker_preview_text),
                fontFamily = font.fontFamily,
                fontSize = 22.sp,
                color = Color(0xFF1F2937),
            )
            Spacer(Modifier.height(4.dp))
            Text(font.label, style = MaterialTheme.typography.labelSmall)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.font_picker_selected_desc),
                tint = FontCardBorderSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/**
 * A catalog entry from [DownloadableFontCatalog] — dimmed with a cloud-download
 * badge when locked, a spinner while [downloadGoogleFont] is in flight, and the
 * same selected-check badge as [FontPickerCard] once downloaded. Tapping routes
 * through the status-driven `onClick` wired by the caller (see [FontPickerSheet]).
 */
@Composable
private fun GoogleFontCard(
    entry: DownloadableFontEntry,
    status: FontDownloadStatus,
    isSelected: Boolean,
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.05f, reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion))
            scale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        } else {
            scale.animateTo(1f, reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion))
        }
    }

    // Error shake per motion-rules.md §3.9, played once when a download fails.
    // `keyframes { using(...) }` takes an Easing, not a spring spec — the
    // doc's LetterlySpring.Stiff reference is illustrative "feel" only, so
    // FastOutSlowInEasing stands in as the closest sharp/non-bouncy Easing.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == FontDownloadStatus.FAILED && !isReducedMotion) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    (-8f) at 50 using FastOutSlowInEasing
                    8f at 100 using FastOutSlowInEasing
                    (-6f) at 150 using FastOutSlowInEasing
                    6f at 200 using FastOutSlowInEasing
                    (-4f) at 250 using FastOutSlowInEasing
                    4f at 300 using FastOutSlowInEasing
                    0f at 400
                },
            )
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) FontCardBorderSelected else FontCardBorderSelected.copy(alpha = 0.15f),
        label = "googleFontCardBorderColor",
    )
    val contentAlpha = if (status == FontDownloadStatus.NOT_DOWNLOADED || status == FontDownloadStatus.DOWNLOADING) 0.4f else 1f

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = shakeOffset.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(FontCardBackground)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.font_picker_preview_text),
                fontFamily = if (status == FontDownloadStatus.DOWNLOADED) googleFontFamily(entry.googleFontName) else null,
                fontSize = 22.sp,
                color = Color(0xFF1F2937).copy(alpha = contentAlpha),
            )
            Spacer(Modifier.height(4.dp))
            Text(entry.label, style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = contentAlpha))
        }
        when (status) {
            FontDownloadStatus.NOT_DOWNLOADED -> Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = stringResource(R.string.font_picker_download_desc),
                tint = FontCardBorderSelected.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
            )
            FontDownloadStatus.DOWNLOADING -> {
                val downloadingDesc = stringResource(R.string.font_picker_downloading_desc)
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .semantics { contentDescription = downloadingDesc },
                    strokeWidth = 2.dp,
                )
            }
            FontDownloadStatus.DOWNLOADED -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.font_picker_selected_desc),
                tint = FontCardBorderSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            FontDownloadStatus.FAILED -> Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = stringResource(R.string.font_picker_download_failed_desc),
                tint = Color(0xFFB3261E),
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
            )
        }
    }
}
