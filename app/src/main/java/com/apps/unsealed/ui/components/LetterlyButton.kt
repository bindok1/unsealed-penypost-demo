package com.apps.unsealed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.NunitoFontFamily

/**
 * Primary CTA button shared di seluruh app — menggantikan `Button` Material biasa.
 *
 * Desain: warm gold fill, teks gelap, pojok bulat kawai (20dp), no-ripple,
 * scale-only press feedback via [rememberPressScale] (motion-rules.md §4).
 * Loading state: teks diganti spinner, tombol tetap disabled.
 *
 * @param text        Label teks tombol.
 * @param onClick     Callback saat tombol ditekan (tidak dipanggil saat [isLoading] atau ![ enabled]).
 * @param modifier    Modifier standar — biasanya `.fillMaxWidth()`.
 * @param enabled     False = tombol dimmed (alpha 0.45) dan tidak bisa ditekan.
 * @param isLoading   True = tampil [CircularProgressIndicator] kecil, tombol tidak bisa ditekan.
 * @param containerColor Warna background tombol. Default: [BrandGold].
 * @param contentColor   Warna teks/ikon/spinner. Default: [BrandInk].
 * @param shape       Shape tombol. Default: [RoundedCornerShape](20.dp) — kawai & konsisten.
 * @param leadingIcon Slot opsional ikon di kiri teks (mis. Google logo).
 */
@Composable
fun LetterlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = BrandGold,
    contentColor: Color = BrandInk,
    shape: Shape = RoundedCornerShape(20.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val isClickable = enabled && !isLoading

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                color = if (isClickable) containerColor else containerColor.copy(alpha = 0.55f),
                shape = shape,
            )
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isClickable) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .alpha(if (isClickable) 1f else 0.55f),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = contentColor,
                    letterSpacing = 0.1.sp,
                )
            }
        }
    }
}

/**
 * Outlined / ghost variant — border [containerColor], transparent fill, teks [containerColor].
 * Dipakai untuk aksi sekunder (mis. "Lewati" atau "Nanti saja") di sebelah [LetterlyButton].
 *
 * Shape dan press feedback identik dengan [LetterlyButton].
 */
@Composable
fun LetterlyOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = BrandGold,
    contentColor: Color = BrandGold,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            // thin border via nested background trick (no BorderStroke needed)
            .background(borderColor.copy(alpha = if (enabled) 0.18f else 0.08f), shape)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Text(
            text = text,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            letterSpacing = 0.1.sp,
        )
    }
}
