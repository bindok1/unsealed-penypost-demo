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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.apps.unsealed.ui.screens.profile.state.AddressBookEntry
import com.apps.unsealed.ui.screens.profile.state.AddressBookUiState
import com.apps.unsealed.ui.screens.profile.viewmodel.AddressBookViewModel
import com.apps.unsealed.ui.screens.profile.widgets.AddressBookRow
import com.apps.unsealed.ui.screens.profile.widgets.CenteredMessage
import com.apps.unsealed.ui.screens.publicprofile.PublicProfileBottomSheet
import com.apps.unsealed.ui.theme.BrandGold

private val DeleteRed = Color(0xFFE53935)

/**
 * Address Book sub-screen: lists saved penpal contacts loaded from `GET /api/v1/address-book`.
 * Tapping a contact opens [PublicProfileBottomSheet]; delete action removes via `DELETE /api/v1/address-book/{id}`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAddressBookScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddressBookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deletingIds by viewModel.deletingIds.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingDeleteEntry by remember { mutableStateOf<AddressBookEntry?>(null) }
    var selectedPublicProfileUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deleteError) {
        deleteError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.resetDeleteError()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(
            title = stringResource(R.string.profile_address_book_title),
            onBackClick = onBackClick,
        )

        when (val state = uiState) {
            is AddressBookUiState.Loading -> CenteredMessage {
                CircularProgressIndicator(color = BrandGold)
            }
            is AddressBookUiState.Error -> CenteredMessage {
                Text(
                    text = state.message,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is AddressBookUiState.Success -> {
                if (state.items.isEmpty()) {
                    CenteredMessage {
                        Text(
                            text = stringResource(R.string.profile_address_book_empty_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.profile_address_book_empty_body),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.items, key = { _, item -> item.id }) { index, entry ->
                            val isDeleting = deletingIds.contains(entry.id)
                            AddressBookRow(
                                entry = entry,
                                isDeleting = isDeleting,
                                onClick = { selectedPublicProfileUserId = entry.userId },
                                onDeleteClick = { pendingDeleteEntry = entry },
                                modifier = Modifier.staggerEntrance(index),
                            )
                        }
                        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                    }
                }
            }
        }
    }

    // Public Profile sheet when tapping contact row
    selectedPublicProfileUserId?.let { userId ->
        PublicProfileBottomSheet(
            userId = userId,
            onDismiss = {
                selectedPublicProfileUserId = null
                // Refresh list in case something changed
                viewModel.load()
            },
        )
    }

    // Confirm Delete Dialog
    pendingDeleteEntry?.let { entry ->
        LetterlyCenterDialog(
            title = stringResource(R.string.profile_address_book_delete_dialog_title),
            body = stringResource(R.string.profile_address_book_delete_dialog_body, entry.name),
            primaryCtaText = stringResource(R.string.profile_address_book_delete_confirm_cta),
            onPrimaryClick = {
                viewModel.deleteContact(entry.id)
                pendingDeleteEntry = null
            },
            secondaryCtaText = stringResource(R.string.profile_address_book_delete_cancel_cta),
            onSecondaryClick = { pendingDeleteEntry = null },
            onDismissRequest = { pendingDeleteEntry = null },
            primaryContainerColor = DeleteRed,
            primaryContentColor = Color.White,
        )
    }
}
