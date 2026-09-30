package com.apps.unsealed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.delay

enum class SnackbarStyle {
    Warning,
    Error,
    Info,
}

/**
 * Premium top floating snackbar that slides down smoothly from the top.
 * Features cozy skeuomorphic styling, frosted backdrop, warm gradient borders,
 * auto-dismiss timer, and tap-to-dismiss interaction.
 */
@Composable
fun LetterlyTopSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    style: SnackbarStyle = SnackbarStyle.Warning,
    autoDismissDurationMillis: Long = 4000L,
    icon: ImageVector? = null,
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank() && autoDismissDurationMillis > 0L) {
            delay(autoDismissDurationMillis)
            onDismiss()
        }
    }

    val isVisible = !message.isNullOrBlank()

    val accentColor = when (style) {
        SnackbarStyle.Warning -> BrandGold
        SnackbarStyle.Error -> Color(0xFFE57373)
        SnackbarStyle.Info -> Color(0xFF81D4FA)
    }

    val leadingIcon = icon ?: when (style) {
        SnackbarStyle.Warning -> Icons.Filled.WarningAmber
        SnackbarStyle.Error -> Icons.Filled.Info
        SnackbarStyle.Info -> Icons.Filled.Info
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 350),
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 250),
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
            .zIndex(100f)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (message != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressScale = rememberPressScale(interactionSource)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                        ambientColor = Color.Black.copy(alpha = 0.3f),
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(ToolbarFrostedDark.copy(alpha = 0.95f))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.7f),
                                accentColor.copy(alpha = 0.2f),
                            ),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
