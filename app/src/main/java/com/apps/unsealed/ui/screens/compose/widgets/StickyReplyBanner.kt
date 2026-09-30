package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily

/**
 * Floating sticky pill banner shown at the top of the Compose screen when replying
 * to a letter, allowing the user to tap and inspect what their correspondent wrote.
 */
@Composable
fun StickyReplyBanner(
    recipientName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasLetterBody: Boolean = true,
) {
    AnimatedVisibility(
        visible = recipientName.isNotBlank(),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrandGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.sticky_reply_banner_reply_desc),
                        tint = BrandGoldDeep,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sticky_reply_banner_replying_to, recipientName),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandInkDeep,
                        fontFamily = NunitoFontFamily,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = stringResource(
                            if (hasLetterBody) {
                                R.string.sticky_reply_banner_read_original
                            } else {
                                R.string.sticky_reply_banner_view_thread
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGoldDeep,
                        fontFamily = NunitoFontFamily,
                        fontSize = 10.sp,
                    )
                }

                // Always shown — a hint-less banner reads as inert, even
                // though the whole Surface is clickable either way. Without a
                // letter body to open inline, the affordance instead points
                // back at the thread (see onClick's wiring at the call site),
                // so the user can hop back and forth peeking at the
                // conversation instead of hitting a dead end.
                if (hasLetterBody) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_book),
                        contentDescription = stringResource(R.string.sticky_reply_banner_read_desc),
                        tint = BrandGoldDeep.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.sticky_reply_banner_view_thread_desc),
                        tint = BrandGoldDeep.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
