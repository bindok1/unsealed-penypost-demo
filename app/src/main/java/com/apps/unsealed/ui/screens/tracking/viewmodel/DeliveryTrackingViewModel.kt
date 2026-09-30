package com.apps.unsealed.ui.screens.tracking.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.letters.data.LettersRepository
import com.apps.unsealed.feature.letters.data.UnlockLetterResult
import com.apps.unsealed.navigation.DeliveryTrackingLetterIdArg
import com.apps.unsealed.navigation.DeliveryTrackingOtherPartyContinentArg
import com.apps.unsealed.ui.screens.inbox.state.formatExpectedDelivery
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.tracking.state.DeliveryTrackingUiState
import com.apps.unsealed.ui.screens.tracking.state.UnlockLetterState
import com.apps.unsealed.ui.screens.tracking.state.calculateProgress
import com.apps.unsealed.ui.screens.tracking.state.toEpochMillis
import com.apps.unsealed.ui.screens.tracking.state.transportModeFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Poll interval for catching the `IN_TRANSIT -> DELIVERED` flip while this
 * screen is on the back stack — light REST polling, not a websocket, since
 * nothing here needs sub-minute precision (see `docs/message-tracking.md`).
 * Progress itself is recomputed from the same fetch, not pushed separately. */
private const val PollIntervalMillis = 60_000L

@HiltViewModel
class DeliveryTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lettersRepository: LettersRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val letterId = savedStateHandle.get<String>(DeliveryTrackingLetterIdArg).orEmpty()
    private val otherPartyContinent = savedStateHandle.get<String>(DeliveryTrackingOtherPartyContinentArg).orEmpty()

    private val _uiState = MutableStateFlow<DeliveryTrackingUiState>(DeliveryTrackingUiState.Loading)
    val uiState: StateFlow<DeliveryTrackingUiState> = _uiState.asStateFlow()

    private val _unlockState = MutableStateFlow<UnlockLetterState>(UnlockLetterState.Idle)
    val unlockState: StateFlow<UnlockLetterState> = _unlockState.asStateFlow()

    /** Own continent/name don't change mid-session — fetched once and reused
     * across polls instead of re-fetching the profile every 60s. [viewerEnergy]
     * is the exception: it's re-synced locally from [unlockLetter]'s response
     * after a spend, since that's the only in-app action that changes it. */
    private var viewerContinent: String? = null
    private var viewerName: String? = null
    private var viewerEnergy: Int? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                val delivered = refresh()
                if (delivered) break
                val currentProgress = (_uiState.value as? DeliveryTrackingUiState.Success)?.progress ?: 0f
                val delayTime = if (currentProgress >= 0.9f) 3_000L else 10_000L
                delay(delayTime)
            }
        }
    }

    private suspend fun refresh(): Boolean {
        if (viewerContinent == null) {
            when (val result = authRepository.fetchMe()) {
                is AuthResult.Success -> {
                    viewerContinent = result.data?.continent
                    viewerName = result.data?.nickname
                    viewerEnergy = result.data?.energy
                }
                is AuthResult.Error -> Unit
            }
        }
        val viewerId = authRepository.currentUser?.uid.orEmpty()

        return when (val result = lettersRepository.getLetter(letterId)) {
            is AuthResult.Success -> {
                val letter = result.data
                val isFromViewer = letter.senderId == viewerId
                val viewerRegion = viewerContinent?.let(PenpalRegion::fromApiString) ?: PenpalRegion.ASIA
                val otherRegion = PenpalRegion.fromApiString(otherPartyContinent)
                val senderRegion = if (isFromViewer) viewerRegion else otherRegion
                val recipientRegion = if (isFromViewer) otherRegion else viewerRegion
                val nowMillis = System.currentTimeMillis()
                val estimatedArrivalMillis = letter.estimatedArrivalAt.toEpochMillis()
                val isDelivered = letter.deliveryStatus == "DELIVERED" || nowMillis >= estimatedArrivalMillis
                val progress = if (isDelivered) {
                    1f
                } else {
                    calculateProgress(
                        sentAtMillis = letter.postedAt.toEpochMillis(),
                        estimatedArrivalAtMillis = estimatedArrivalMillis,
                        nowMillis = nowMillis,
                    )
                }
                // letter.senderName and letter.recipientName are denormalized on the DTO.
                _uiState.value = DeliveryTrackingUiState.Success(
                    senderContinent = senderRegion,
                    recipientContinent = recipientRegion,
                    transportMode = transportModeFor(senderRegion, recipientRegion),
                    progress = progress,
                    sentAtLabel = formatExpectedDelivery(letter.postedAt),
                    estimatedArrivalAtLabel = formatExpectedDelivery(letter.estimatedArrivalAt),
                    isDelivered = isDelivered,
                    senderName = if (isFromViewer) viewerName else letter.senderName,
                    recipientName = if (isFromViewer) letter.recipientName else viewerName,
                    letterId = letterId,
                    isViewerRecipient = !isFromViewer,
                    viewerEnergy = viewerEnergy,
                    envelope = letter.envelope,
                    envelopeCompositeImageUrl = letter.envelopeCompositeImageUrl,
                )
                isDelivered
            }
            is AuthResult.Error -> {
                _uiState.value = DeliveryTrackingUiState.Error(result.message)
                true
            }
        }
    }

    /** Spends energy to instant-deliver this letter — only ever called from a
     * state where [DeliveryTrackingUiState.Success.isViewerRecipient] is true
     * (the screen gates the CTA on that), but the backend re-validates
     * recipient + `in_transit` regardless. */
    fun unlockLetter() {
        val current = _uiState.value as? DeliveryTrackingUiState.Success
        if (current != null && !current.isViewerRecipient) {
            return
        }
        viewModelScope.launch {
            _unlockState.value = UnlockLetterState.Unlocking
            when (val result = lettersRepository.unlockLetter(letterId)) {
                is UnlockLetterResult.Success -> {
                    viewerEnergy = result.data.energyRemaining
                    _unlockState.value = UnlockLetterState.Success(result.data.energyRemaining)
                    refresh()
                }
                is UnlockLetterResult.InsufficientEnergy -> {
                    _unlockState.value = UnlockLetterState.InsufficientEnergy(result.required, result.available)
                }
                is UnlockLetterResult.Error -> {
                    _unlockState.value = UnlockLetterState.Error(result.message)
                }
            }
        }
    }

    fun resetUnlockState() {
        _unlockState.value = UnlockLetterState.Idle
    }
}
