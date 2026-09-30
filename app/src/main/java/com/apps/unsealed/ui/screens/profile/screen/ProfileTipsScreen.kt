package com.apps.unsealed.ui.screens.profile.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apps.unsealed.R
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.profile.constants.dummyTips
import com.apps.unsealed.ui.screens.profile.widgets.TipCard

/** Tips sub-screen: how-to guides ("why sign in", how stamps work, etc).
 * Rows expand in place on tap rather than navigating — motion-rules.md's
 * Gentle spring drives the body reveal + chevron rotation. */
@Composable
fun ProfileTipsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(title = stringResource(R.string.profile_tips_title), onBackClick = onBackClick)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(dummyTips.size, key = { dummyTips[it].id }) { index ->
                TipCard(tip = dummyTips[index], modifier = Modifier.staggerEntrance(index))
            }
            item {
                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
