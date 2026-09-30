package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.ui.screens.stamps.constants.StoreFilterCategory
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily

/**
 * Horizontally scrollable row of filter chips for the Peny Store catalog.
 * Each chip maps to a [StoreFilterCategory] and its backend query param.
 */
@Composable
fun StoreFilterChips(
    selected: StoreFilterCategory,
    onSelect: (StoreFilterCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        StoreFilterCategory.entries.forEach { filter ->
            val isActive = selected == filter
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isActive) BrandGold.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onSelect(filter)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(filter.labelRes),
                    fontFamily = NunitoFontFamily,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isActive) BrandGold else Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}
