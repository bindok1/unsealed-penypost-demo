package com.apps.unsealed.ui.screens.inbox.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.inbox.state.MailboxRoomItem
import com.apps.unsealed.ui.screens.inbox.state.MailboxUiState
import com.apps.unsealed.ui.screens.inbox.viewmodel.MailboxViewModel
import com.apps.unsealed.ui.screens.inbox.widgets.MailboxRoomCard
import com.apps.unsealed.ui.theme.NunitoFontFamily

// ─── Constants ────────────────────────────────────────────────────────────────

/** Scroll offset (px) over which the large title fully fades out and the
 *  compact centered title fully fades in. Tuned to ~1.5× the large title's
 *  visible height so the transition feels natural on first scroll. */
private const val TitleCollapseThresholdPx = 140

// ─── Screen ───────────────────────────────────────────────────────────────────

/** Room list — one row per correspondent (`GET /mailbox`). Replaces the old
 * strictly-received-mail Inbox: a room shows up here for both sides the
 * moment either party sends a letter, without waiting for delivery or a
 * reply — see `docs/be/letters_api.md`'s Mailbox section. */
@Composable
fun MailboxScreen(
    onRoomClick: (MailboxRoomItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MailboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadMailbox(isSilent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /** 0f = large title fully visible, 1f = compact centered title fully visible. */
    val titleCollapseFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / TitleCollapseThresholdPx.toFloat())
                    .coerceIn(0f, 1f)
            }
        }
    }

    // Background (blur + dark overlay) is owned by MainActivity for this route,
    // so MailboxScreen is fully transparent — no background drawn here.
    Column(modifier = modifier.fillMaxSize()) {

        // ── Sticky compact centered title (alpha 0→1 as user scrolls) ──────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.mailbox_title),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White.copy(alpha = titleCollapseFraction),
                modifier = Modifier.graphicsLayer {
                    val scale = 0.92f + 0.08f * titleCollapseFraction
                    scaleX = scale
                    scaleY = scale
                },
            )
        }

        // ── Scrollable list ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Large title item — fades out as user scrolls
            item {
                Text(
                    text = stringResource(R.string.mailbox_title),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    color = Color.White.copy(alpha = 1f - titleCollapseFraction),
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 20.dp)
                        .graphicsLayer {
                            translationY = -16.dp.toPx() * titleCollapseFraction
                        },
                )
            }

            // Room cards with stagger entrance — real data from MailboxViewModel
            when (val state = uiState) {
                is MailboxUiState.Loading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is MailboxUiState.Error -> item {
                    Text(
                        text = state.message,
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
                is MailboxUiState.Success -> if (state.rooms.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.mailbox_empty_label),
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        )
                    }
                } else {
                    itemsIndexed(state.rooms) { index, room ->
                        MailboxRoomCard(
                            room = room,
                            entranceIndex = index,
                            onClick = { onRoomClick(room) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            // Bottom padding so last card clears the nav bar
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
