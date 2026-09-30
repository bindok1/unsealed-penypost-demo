package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio

/**
 * Dummy "amplop terbang" send-success visual (compose-screen-spec.md §10's
 * eventual real design intent, approximated here since there's no backend to
 * drive a real send). The envelope launches up and tilts using
 * [LetterlySpring.Envelope] for the heavy-paper feel, then a "Sent!" message
 * fades in; [onFinished] fires once the sequence completes.
 */
@Composable
fun SendSuccessOverlay(
    envelope: CatalogItemDto,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    val translateY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val messageAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        translateY.animateTo(-1400f, reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion))
        rotation.animateTo(-8f, reducedMotionSpring(LetterlySpring.Envelope, isReducedMotion))
    }
    LaunchedEffect(Unit) {
        messageAlpha.animateTo(1f, tween(300, delayMillis = 250))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = envelope.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(EnvelopeAspectRatio)
                .graphicsLayer {
                    translationY = translateY.value
                    rotationZ = rotation.value
                }
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            stringResource(R.string.send_success_message),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier
                .padding(top = 220.dp)
                .graphicsLayer { alpha = messageAlpha.value },
        )
    }
}
