package com.apps.unsealed.ui.screens.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.blocks.data.BlockedUserDto
import com.apps.unsealed.feature.blocks.data.BlocksRepository
import com.apps.unsealed.ui.screens.profile.state.BlockedUserEntry
import com.apps.unsealed.ui.screens.profile.state.BlockedUsersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val BlockedAtFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun BlockedUserDto.toBlockedUserEntry() = BlockedUserEntry(
    userId = userId,
    userName = userName,
    blockedAtLabel = runCatching { BlockedAtFormatter.format(Instant.parse(blockedAt)) }.getOrDefault(blockedAt),
)

/** Drives the "Blocked Users" settings screen — see `docs/be/trust_and_safety_api.md` §2. */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val blocksRepository: BlocksRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BlockedUsersUiState>(BlockedUsersUiState.Loading)
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    /** User IDs with an in-flight unblock request — lets each row show its own spinner. */
    private val _unblockingIds = MutableStateFlow<Set<String>>(emptySet())
    val unblockingIds: StateFlow<Set<String>> = _unblockingIds.asStateFlow()

    private val _unblockError = MutableStateFlow<String?>(null)
    val unblockError: StateFlow<String?> = _unblockError.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = BlockedUsersUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = blocksRepository.getBlockedUsers()) {
                is AuthResult.Success -> BlockedUsersUiState.Success(result.data.map { it.toBlockedUserEntry() })
                is AuthResult.Error -> BlockedUsersUiState.Error(result.message)
            }
        }
    }

    /** Unblocks [userId]. On success, removes it from the current list without a full reload. */
    fun unblock(userId: String) {
        _unblockingIds.update { it + userId }
        viewModelScope.launch {
            when (val result = blocksRepository.unblockUser(userId)) {
                is AuthResult.Success -> {
                    val current = _uiState.value
                    if (current is BlockedUsersUiState.Success) {
                        _uiState.value = current.copy(items = current.items.filterNot { it.userId == userId })
                    }
                }
                is AuthResult.Error -> _unblockError.value = result.message
            }
            _unblockingIds.update { it - userId }
        }
    }

    /** Clears [unblockError], e.g. after the toast is shown. */
    fun resetUnblockError() {
        _unblockError.value = null
    }
}
