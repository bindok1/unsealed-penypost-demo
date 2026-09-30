package com.apps.unsealed.ui.screens.stamps.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.LetterlyStoreCard
import com.apps.unsealed.ui.components.StoreCardShape
import com.apps.unsealed.ui.screens.stamps.constants.SubscriptionPlanEntry
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.LemonYellow
import com.apps.unsealed.ui.theme.NunitoFontFamily

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Color.White,
        modifier = modifier,
    )
}

@Composable
fun SubscriptionCard(
    plan: SubscriptionPlanEntry,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LetterlyStoreCard(
        shape = StoreCardShape.Wide,
        title = stringResource(plan.titleRes),
        subtitle = stringResource(plan.subtitleRes),
        onClick = onSubscribeClick,
        modifier = modifier,
        image = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(LemonYellow, InkDefault))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = plan.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp),
                )
            }
        },
        badge = {
            PillLabel(
                text = stringResource(R.string.stamps_screen_plan_subscribe_cta),
                color = BrandGold,
            )
        },
    )
}

@Composable
fun PillLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f),
        modifier = modifier,
    ) {
        Text(
            text = text,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
