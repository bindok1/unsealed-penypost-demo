package com.apps.unsealed.ui.screens.profile.screen

import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.unsealed.R
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.profile.state.BlockedUserEntry
import com.apps.unsealed.ui.screens.profile.state.BlockedUsersUiState
import com.apps.unsealed.ui.screens.profile.viewmodel.BlockedUsersViewModel
import com.apps.unsealed.ui.screens.profile.widgets.BlockedUserRow
import com.apps.unsealed.ui.screens.profile.widgets.CenteredMessage
import com.apps.unsealed.ui.theme.BrandGold

/**
 * "Blocked Users" settings sub-screen — list from `GET /api/v1/blocks`, unblock per-row via
 * `DELETE /api/v1/blocks/{id}`. See `docs/be/trust_and_safety_api.md` §2.
 */
@Composable
fun BlockedUsersScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockedUsersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unblockingIds by viewModel.unblockingIds.collectAsStateWithLifecycle()
    val unblockError by viewModel.unblockError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingUnblock by remember { mutableStateOf<BlockedUserEntry?>(null) }

    LaunchedEffect(unblockError) {
        unblockError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.resetUnblockError()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(title = stringResource(R.string.blocked_users_title), onBackClick = onBackClick)

        when (val state = uiState) {
            is BlockedUsersUiState.Loading -> CenteredMessage {
                CircularProgressIndicator(color = BrandGold)
            }
            is BlockedUsersUiState.Error -> CenteredMessage {
                Text(
                    text = state.message,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is BlockedUsersUiState.Success -> {
                if (state.items.isEmpty()) {
                    CenteredMessage {
                        Text(
                            text = stringResource(R.string.blocked_users_empty_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.blocked_users_empty_body),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items.size) { index ->
                            val entry = state.items[index]
                            BlockedUserRow(
                                entry = entry,
                                isUnblocking = entry.userId in unblockingIds,
                                onUnblockClick = { pendingUnblock = entry },
                                modifier = Modifier.staggerEntrance(index),
                            )
                        }
                        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                    }
                }
            }
        }
    }

    val unblockTarget = pendingUnblock
    if (unblockTarget != null) {
        LetterlyCenterDialog(
            title = stringResource(R.string.unblock_user_confirm_dialog_title, unblockTarget.userName),
            body = stringResource(R.string.unblock_user_confirm_dialog_body),
            primaryCtaText = stringResource(R.string.unblock_user_confirm_dialog_cta_confirm),
            onPrimaryClick = {
                viewModel.unblock(unblockTarget.userId)
                pendingUnblock = null
            },
            secondaryCtaText = stringResource(R.string.unblock_user_confirm_dialog_cta_cancel),
            onSecondaryClick = { pendingUnblock = null },
            onDismissRequest = { pendingUnblock = null },
        )
    }
}
