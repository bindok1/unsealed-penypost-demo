package com.apps.unsealed.ui.screens.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.core.data.PendingInviteCodeHolder
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.invite.data.InviteRepository
import com.apps.unsealed.ui.screens.profile.state.InviteGenerateUiState
import com.apps.unsealed.ui.screens.profile.state.InviteRedeemUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Drives [com.apps.unsealed.ui.screens.profile.screen.ProfileInviteFriendsScreen] — generates
 * the viewer's own invite code (`POST /api/v1/invites`) and redeems a friend's code
 * (`POST /api/v1/invites/{code}/redeem`), see `docs/be_updet/invite_friend_api.md` §2. */
@HiltViewModel
class InviteViewModel @Inject constructor(
    private val inviteRepository: InviteRepository,
    private val pendingInviteCodeHolder: PendingInviteCodeHolder,
) : ViewModel() {

    private val _inviteState = MutableStateFlow<InviteGenerateUiState>(InviteGenerateUiState.Loading)
    val inviteState: StateFlow<InviteGenerateUiState> = _inviteState.asStateFlow()

    private val _redeemState = MutableStateFlow<InviteRedeemUiState>(InviteRedeemUiState.Idle)
    val redeemState: StateFlow<InviteRedeemUiState> = _redeemState.asStateFlow()

    /** Generates a new code every time this is called — cheapest MVP option per
     * `invite_friend_api.md` §2 ("boleh reuse ... pilihan implementasi, gak ngubah kontrak"). */
    fun generateInvite() {
        _inviteState.value = InviteGenerateUiState.Loading
        viewModelScope.launch {
            _inviteState.value = when (val result = inviteRepository.generateInvite()) {
                is AuthResult.Success -> InviteGenerateUiState.Success(result.data.code, result.data.deepLinkUrl)
                is AuthResult.Error -> InviteGenerateUiState.Error(result.message)
            }
        }
    }

    fun redeemInvite(code: String) {
        _redeemState.value = InviteRedeemUiState.Loading
        viewModelScope.launch {
            _redeemState.value = when (val result = inviteRepository.redeemInvite(code)) {
                is AuthResult.Success -> InviteRedeemUiState.Success(result.data.inviterName)
                is AuthResult.Error -> InviteRedeemUiState.Error(result.message)
            }
        }
    }

    fun resetRedeemState() {
        _redeemState.value = InviteRedeemUiState.Idle
    }

    /** Auto-redeems a code captured from an incoming App Link, if one is pending — see
     * [PendingInviteCodeHolder]. No-op (stays [InviteRedeemUiState.Idle]) when the screen was
     * opened normally from the Profile tab instead of via a deep link. */
    fun consumePendingInviteCodeIfAny() {
        val code = pendingInviteCodeHolder.consume() ?: return
        redeemInvite(code)
    }
}
