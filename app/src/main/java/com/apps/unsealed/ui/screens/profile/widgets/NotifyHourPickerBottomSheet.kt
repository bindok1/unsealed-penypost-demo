package com.apps.unsealed.ui.screens.profile.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkMid

data class NotifyHourOption(
    val value: String,
    /** Already carries its own emoji, e.g. "🌅 Morning" — see the
     * `onboarding_notify_hour_*` strings shared with onboarding. */
    val titleRes: Int,
)

/** `notify_hour_pref` values — see `docs/be/daily_penpal_stack.md` §3. */
val NotifyHourPickerOptions = listOf(
    NotifyHourOption("morning", R.string.onboarding_notify_hour_morning),
    NotifyHourOption("afternoon", R.string.onboarding_notify_hour_afternoon),
    NotifyHourOption("evening", R.string.onboarding_notify_hour_evening),
)

/** Same visual pattern as [LanguagePickerBottomSheet] — lets the viewer
 * change when their daily letter stack notification lands, post-onboarding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyHourPickerBottomSheet(
    currentValue: String?,
    onValueSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandInkMid,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_settings_notify_hour_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.profile_settings_notify_hour_picker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(20.dp))

            NotifyHourPickerOptions.forEach { option ->
                val isSelected = currentValue == option.value ||
                    (currentValue == null && option.value == "morning")
                NotifyHourItemRow(
                    option = option,
                    isSelected = isSelected,
                    onClick = {
                        onValueSelected(option.value)
                        onDismiss()
                    },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun NotifyHourItemRow(
    option: NotifyHourOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(150),
        label = "notifyHourCheckAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) BrandGold.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.06f),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(option.titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) BrandGold else Color.White,
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { alpha = checkAlpha },
            )
        }
    }
}
