package com.apps.unsealed.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.addressbook.data.AddressBookRepository
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.auth.data.InterestDto
import com.apps.unsealed.feature.auth.data.PublicProfileDto
import com.apps.unsealed.feature.blocks.data.BlocksRepository
import com.apps.unsealed.feature.letters.data.LettersRepository
import com.apps.unsealed.feature.reports.data.ReportsRepository
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionUiState
import com.apps.unsealed.ui.screens.profile.state.toPostalCollectionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class PublicProfileUiState {
    /** Initial / in-flight fetch. */
    object Loading : PublicProfileUiState()

    /**
     * Profile fetched successfully.
     *
     * [interests] is the resolved interest catalog list — joined from the
     * raw ID list in [profile] against the interests catalog. It may be
     * empty if the catalog hasn't loaded yet or the user chose no interests.
     */
    data class Success(
        val profile: PublicProfileDto,
        val interests: List<InterestDto>,
    ) : PublicProfileUiState()

    /**
     * The server returned 404 for the requested user ID.
     * Covers both "user does not exist" and "a block relationship exists
     * between viewer and target" — the two are intentionally indistinguishable.
     */
    object NotFound : PublicProfileUiState()

    /** Network / server error. */
    data class Error(val message: String) : PublicProfileUiState()
}

/** State of an in-flight/completed "add to address book" request against the viewed profile. */
sealed class AddressBookRequestState {
    object Idle : AddressBookRequestState()
    object Sending : AddressBookRequestState()
    object Sent : AddressBookRequestState()
    data class Error(val message: String) : AddressBookRequestState()
}

/** State of an in-flight/completed report against the viewed profile. */
sealed class ReportUserState {
    object Idle : ReportUserState()
    object Submitting : ReportUserState()
    object Submitted : ReportUserState()
    data class Error(val message: String) : ReportUserState()
}

/** State of an in-flight/completed block against the viewed profile. */
sealed class BlockUserState {
    object Idle : BlockUserState()
    object Blocking : BlockUserState()
    object Blocked : BlockUserState()
    data class Error(val message: String) : BlockUserState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * Drives the public profile bottom sheet.
 *
 * Call [load] with the target user ID whenever the sheet is about to be
 * shown. The ViewModel resets to [PublicProfileUiState.Loading] on each call
 * so successive profile taps don't briefly flash stale data.
 */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val addressBookRepository: AddressBookRepository,
    private val reportsRepository: ReportsRepository,
    private val blocksRepository: BlocksRepository,
    private val lettersRepository: LettersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    /** This user's Postal Collection Showcase (`GET /users/{id}/showcase`,
     * unlike [com.apps.unsealed.feature.auth.AuthViewModel.loadPostalCollection]'s
     * owner-only `GET /letters/me/showcase`) — already server-filtered to
     * `showcase_status = "visible"`, same as [UnsealedProfileContent]'s. */
    private val _postalCollection = MutableStateFlow<PostalCollectionUiState>(PostalCollectionUiState.Loading)
    val postalCollection: StateFlow<PostalCollectionUiState> = _postalCollection.asStateFlow()

    private val _addressBookState = MutableStateFlow<AddressBookRequestState>(AddressBookRequestState.Idle)
    val addressBookState: StateFlow<AddressBookRequestState> = _addressBookState.asStateFlow()

    private val _reportState = MutableStateFlow<ReportUserState>(ReportUserState.Idle)
    val reportState: StateFlow<ReportUserState> = _reportState.asStateFlow()

    private val _blockState = MutableStateFlow<BlockUserState>(BlockUserState.Idle)
    val blockState: StateFlow<BlockUserState> = _blockState.asStateFlow()

    /**
     * Fetches the public profile and, on success, enriches the interest IDs
     * with labels from the interest catalog (loaded in parallel).
     */
    fun load(userId: String) {
        _uiState.value = PublicProfileUiState.Loading
        _addressBookState.value = AddressBookRequestState.Idle
        _reportState.value = ReportUserState.Idle
        _blockState.value = BlockUserState.Idle
        _postalCollection.value = PostalCollectionUiState.Loading
        viewModelScope.launch {
            _postalCollection.value = when (val result = lettersRepository.getUserShowcase(userId)) {
                is AuthResult.Success -> PostalCollectionUiState.Success(
                    result.data.items.map { it.toPostalCollectionItem() },
                )
                is AuthResult.Error -> PostalCollectionUiState.Error(result.message)
            }
        }
        viewModelScope.launch {
            // Load profile and interest catalog concurrently.
            val profileResult = repository.fetchPublicProfile(userId)
            val catalogResult = repository.getInterests()

            val catalog: List<InterestDto> =
                (catalogResult as? AuthResult.Success)?.data.orEmpty()

            _uiState.value = when (profileResult) {
                is AuthResult.Success -> {
                    val profile = profileResult.data
                    if (profile == null) {
                        PublicProfileUiState.NotFound
                    } else {
                        val ids = profile.interests.toSet()
                        PublicProfileUiState.Success(
                            profile = profile,
                            interests = catalog.filter { it.id in ids },
                        )
                    }
                }
                is AuthResult.Error -> PublicProfileUiState.Error(profileResult.message)
            }
        }
    }

    /** Sends an "add to address book" request against [userId]. See [AddressBookRequestState]. */
    fun addToAddressBook(userId: String) {
        _addressBookState.value = AddressBookRequestState.Sending
        viewModelScope.launch {
            _addressBookState.value = when (val result = addressBookRepository.addToAddressBook(userId)) {
                is AuthResult.Success -> AddressBookRequestState.Sent
                is AuthResult.Error -> AddressBookRequestState.Error(result.message)
            }
        }
    }

    /** Resets [addressBookState] back to [AddressBookRequestState.Idle], e.g. after a dialog is dismissed. */
    fun resetAddressBookState() {
        _addressBookState.value = AddressBookRequestState.Idle
    }

    /** Reports [userId]'s profile (`target_type = "user"`). See [ReportUserState]. */
    fun reportUser(userId: String, reason: String, note: String?) {
        _reportState.value = ReportUserState.Submitting
        viewModelScope.launch {
            _reportState.value = when (val result = reportsRepository.reportUser(userId, reason, note)) {
                is AuthResult.Success -> ReportUserState.Submitted
                is AuthResult.Error -> ReportUserState.Error(result.message)
            }
        }
    }

    /** Resets [reportState] back to [ReportUserState.Idle], e.g. after the dialog is dismissed. */
    fun resetReportState() {
        _reportState.value = ReportUserState.Idle
    }

    /** Blocks [userId]. See [BlockUserState]. */
    fun blockUser(userId: String) {
        _blockState.value = BlockUserState.Blocking
        viewModelScope.launch {
            _blockState.value = when (val result = blocksRepository.blockUser(userId)) {
                is AuthResult.Success -> BlockUserState.Blocked
                is AuthResult.Error -> BlockUserState.Error(result.message)
            }
        }
    }

    /** Resets [blockState] back to [BlockUserState.Idle]. */
    fun resetBlockState() {
        _blockState.value = BlockUserState.Idle
    }
}
