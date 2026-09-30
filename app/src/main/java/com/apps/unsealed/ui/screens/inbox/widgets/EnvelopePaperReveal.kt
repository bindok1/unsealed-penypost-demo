package com.apps.unsealed.ui.screens.inbox.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.ui.screens.inbox.state.ThreadLetterItem
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.PaperTemplate

/** Letter "photo" aspect ratio — matches the [PaperTemplate] asset ratio
 * (1240x1754px, A4 portrait) so the stand-in image never looks stretched.
 * Not private: reused by [com.apps.unsealed.ui.screens.inbox.widgets.MailboxFullLetterViewer]
 * so the full-screen letter view matches this inline reveal's proportions. */
const val LetterPaperAspectRatio = 1240f / 1754f

// ─── Envelope → Paper reveal (motion-rules.md §3.6) ──────────────────────────

/** Tap-to-open envelope→paper reveal for one delivered letter inside
 * [com.apps.unsealed.ui.screens.inbox.screen.MailboxThreadScreen]'s thread
 * list. Renders envelope art without sticker-overlay reconstruction —
 * `GET /mailbox/{threadId}` carries no `envelope_sticker_id`/
 * `envelope_composite_image_url` fields (see `docs/be/letters_api.md` gap
 * note), unlike the old single-letter `GET /letters/{id}` flow. */
@Composable
fun EnvelopePaperReveal(
    item: ThreadLetterItem,
    isOpened: Boolean,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()

    val infiniteTransition = rememberInfiniteTransition(label = "envelopeGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val aspectRatio by animateFloatAsState(
        targetValue = if (isOpened) LetterPaperAspectRatio else EnvelopeAspectRatio,
        animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
        label = "threadLetterAspectRatio",
    )
    val envelopeScale by animateFloatAsState(
        targetValue = if (isOpened) 0.85f else 1f,
        animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
        label = "envelopeScale",
    )
    val envelopeAlpha by animateFloatAsState(
        targetValue = if (isOpened) 0f else 1f,
        animationSpec = tween(300),
        label = "envelopeAlpha",
    )
    val paperScale by animateFloatAsState(
        targetValue = if (isOpened) 1f else 0.85f,
        animationSpec = reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion),
        label = "paperScale",
    )
    val paperAlpha by animateFloatAsState(
        targetValue = if (isOpened) 1f else 0f,
        animationSpec = tween(300, delayMillis = if (isOpened) 100 else 0),
        label = "paperAlpha",
    )

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (!isOpened && !item.isFromViewer) BrandGold.copy(alpha = glowPulse) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenClick,
            ),
    ) {
        // envelopeImageUrl is resolved against the CMS-driven catalog
        // (feature/catalog/data/) — item.envelope's enum is only the
        // placeholder/error fallback here, not the primary source, so a
        // CMS-added design still renders correctly instead of silently
        // showing ENVELOPE_1's art.
        AsyncImage(
            model = item.envelopeImageUrl,
            contentDescription = stringResource(R.string.open_letter_envelope_icon_desc),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(item.envelope.drawableRes),
            error = painterResource(item.envelope.drawableRes),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = envelopeScale
                    scaleY = envelopeScale
                    alpha = envelopeAlpha
                },
        )
        // compositeImageUrl is the real letter content (flattened WebP — see
        // LetterCompositor.kt/docs/be/letters_api.md) for senders whose
        // device supports compositing (MinCompositingDensity); null both for
        // legacy letters sent before compositing existed AND for letters
        // sent from a device below that density threshold — either way,
        // bodyText is rendered as plain text over letterPaper instead of
        // leaving blank paper texture with nothing on it.
        if (item.compositeImageUrl != null) {
            AsyncImage(
                model = item.compositeImageUrl,
                contentDescription = stringResource(R.string.open_letter_paper_icon_desc),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(item.letterPaper.drawableRes),
                error = painterResource(item.letterPaper.drawableRes),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = paperScale
                        scaleY = paperScale
                        alpha = paperAlpha
                    },
            )
        } else {
            PlainTextLetterPaper(
                bodyText = item.bodyText,
                paperTemplate = item.letterPaper,
                paperUrl = item.paperUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = paperScale
                        scaleY = paperScale
                        alpha = paperAlpha
                    },
            )
        }

        if (!isOpened && !item.isFromViewer) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, BrandGold.copy(alpha = glowPulse)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mail,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.letter_tracking_celebration_tap_hint),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontFamily = NunitoFontFamily,
                    )
                }
            }
        }
    }
}

/** Fallback for [EnvelopePaperReveal] when a letter has no
 * [ThreadLetterItem.compositeImageUrl] — same [CaveatFontFamily]/[InkDefault]
 * treatment as the PenPals feed's letter text (`OpenLetterOverlay.kt`), so a
 * plain-text letter still reads as "handwritten on paper" rather than a
 * degraded state. */
@Composable
fun PlainTextLetterPaper(
    bodyText: String,
    paperTemplate: PaperTemplate,
    modifier: Modifier = Modifier,
    paperUrl: String? = null,
) {
    Box(modifier = modifier) {
        if (!paperUrl.isNullOrBlank()) {
            AsyncImage(
                model = paperUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(paperTemplate.drawableRes),
                error = painterResource(paperTemplate.drawableRes),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(paperTemplate.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = bodyText,
            fontFamily = CaveatFontFamily,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = InkDefault,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        )
    }
}

/** Small mode indicator (envelope ↔ paper icon) + the per-item caption —
 * same slot/position regardless of mode (letter/paper icon swaps, caption
 * text itself is not mode-driven). Tapping [EnvelopePaperReveal] itself opens
 * [MailboxFullLetterViewer] directly, so this caption is a status row only,
 * not a tap target. */
@Composable
fun ModeCaption(item: ThreadLetterItem, isOpened: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isOpened) Icons.Filled.Description else Icons.Filled.Mail,
            contentDescription = if (isOpened) {
                stringResource(R.string.open_letter_paper_icon_desc)
            } else {
                stringResource(R.string.open_letter_envelope_icon_desc)
            },
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = item.metaLabel,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
