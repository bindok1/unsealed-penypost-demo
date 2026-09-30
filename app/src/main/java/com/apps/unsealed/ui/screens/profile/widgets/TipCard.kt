package com.apps.unsealed.ui.screens.profile.widgets

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.screens.profile.state.TipEntry
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.NunitoFontFamily

private val GentleSizeSpring = spring<IntSize>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
fun TipCard(tip: TipEntry, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val isReducedMotion = rememberIsReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
        label = "tipChevronRotation",
    )

    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(cardShape)
            .background(BrandCardDark)
            .border(
                width = 1.dp,
                color = if (isExpanded) BrandGold.copy(alpha = 0.35f) else BrandCardStroke,
                shape = cardShape,
            )
            .clickable(interactionSource = interactionSource, indication = null) { isExpanded = !isExpanded }
            .animateContentSize(reducedMotionSpring(GentleSizeSpring, isReducedMotion))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tip.icon,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(tip.titleRes),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (isExpanded) BrandGold else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }
        if (isExpanded) {
            Text(
                text = stringResource(tip.bodyRes),
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 52.dp, top = 10.dp, end = 4.dp),
            )
        }
    }
}
