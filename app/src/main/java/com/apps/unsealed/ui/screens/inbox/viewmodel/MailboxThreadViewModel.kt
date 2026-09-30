package com.apps.unsealed.ui.screens.inbox.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.blocks.data.BlocksRepository
import com.apps.unsealed.feature.catalog.data.CatalogRepository
import com.apps.unsealed.feature.draft.data.DraftRepository
import com.apps.unsealed.feature.mailbox.data.MailboxRepository
import com.apps.unsealed.feature.reports.data.ReportsRepository
import com.apps.unsealed.navigation.MailboxThreadContinentArg
import com.apps.unsealed.navigation.MailboxThreadIdArg
import com.apps.unsealed.navigation.MailboxThreadNameArg
import com.apps.unsealed.ui.screens.inbox.state.MailboxThreadUiState
import com.apps.unsealed.ui.screens.inbox.state.ThreadBlockState
import com.apps.unsealed.ui.screens.inbox.state.ThreadReportState
import com.apps.unsealed.ui.screens.inbox.state.toThreadLetterItem
import com.apps.unsealed.ui.screens.selectrecipient.constants.localEnvelopeCatalog
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStampCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class MailboxThreadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsRepository,
    private val authRepository: AuthRepository,
    private val mailboxRepository: MailboxRepository,
    private val catalogRepository: CatalogRepository,
    private val reportsRepository: ReportsRepository,
    private val blocksRepository: BlocksRepository,
    private val draftRepository: DraftRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MailboxThreadUiState>(MailboxThreadUiState.Loading)
    val uiState: StateFlow<MailboxThreadUiState> = _uiState.asStateFlow()

    private val _reportState = MutableStateFlow<ThreadReportState>(ThreadReportState.Idle)
    val reportState: StateFlow<ThreadReportState> = _reportState.asStateFlow()

    private val _blockState = MutableStateFlow<ThreadBlockState>(ThreadBlockState.Idle)
    val blockState: StateFlow<ThreadBlockState> = _blockState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                loadThread(isSilent = _uiState.value is MailboxThreadUiState.Success)
                val letters = (_uiState.value as? MailboxThreadUiState.Success)?.letters.orEmpty()
                val hasInTransit = letters.any { it.deliveryStatus != com.apps.unsealed.ui.screens.inbox.state.LetterDeliveryStatus.DELIVERED }
                val delayTime = if (hasInTransit) 5_000L else 20_000L
                delay(delayTime)
            }
        }
    }

    fun loadThread(isSilent: Boolean = false) {
        val correspondentId = savedStateHandle.get<String>(MailboxThreadIdArg)
        val correspondentName = savedStateHandle.get<String>(MailboxThreadNameArg)
        val correspondentContinent = savedStateHandle.get<String>(MailboxThreadContinentArg)
        if (correspondentId == null || correspondentName == null || correspondentContinent == null) {
            _uiState.value = MailboxThreadUiState.Error("Missing thread arguments")
            return
        }
        if (!isSilent && _uiState.value !is MailboxThreadUiState.Success) {
            _uiState.value = MailboxThreadUiState.Loading
        }
        viewModelScope.launch {
            coroutineScope {
                val catalogDeferred = async { catalogRepository.getEnvelopes() }
                val stampDeferred = async { catalogRepository.getStamps() }
                val threadDeferred = async { mailboxRepository.getThread(correspondentId) }
                val blocksDeferred = async { blocksRepository.getBlockedUsers() }
                val draftDeferred = async { draftRepository.getLatestByRecipientId(correspondentId) }

                val envelopeCatalog = (catalogDeferred.await() as? AuthResult.Success)?.data
                    ?: localEnvelopeCatalog(context)
                val stampCatalog = (stampDeferred.await() as? AuthResult.Success)?.data
                    ?: localStampCatalog(context)
                val viewerId = authRepository.currentUser?.uid.orEmpty()
                val isBlocked = (blocksDeferred.await() as? AuthResult.Success)?.data
                    ?.any { it.userId == correspondentId } == true
                val draftId = draftDeferred.await()?.draftId

                when (val result = threadDeferred.await()) {
                    is AuthResult.Success -> {
                        _uiState.value = MailboxThreadUiState.Success(
                            correspondentId = correspondentId,
                            correspondentName = correspondentName,
                            correspondentContinent = correspondentContinent,
                            letters = result.data.map { it.toThreadLetterItem(viewerId, envelopeCatalog, stampCatalog) },
                            isBlocked = isBlocked,
                            draftId = draftId,
                        )
                        // GET /mailbox/{threadId} marks this room's letters read
                        // server-side, but MailboxRepository.totalUnreadCount only
                        // updates from a GET /mailbox response — without this resync
                        // the nav-bar badge stays stuck at its pre-read count until
                        // the app is restarted. Gated to the initial (non-silent) load
                        // so the 5-20s background poll below doesn't refetch it every tick.
                        if (!isSilent) {
                            launch { mailboxRepository.getMailbox() }
                            analytics.logEvent("thread_opened")
                        }
                    }
                    is AuthResult.Error -> {
                        if (_uiState.value !is MailboxThreadUiState.Success) {
                            _uiState.value = MailboxThreadUiState.Error(result.message)
                        }
                    }
                }
            }
        }
    }

    /** Reports [userId]'s profile (`target_type = "user"`). See [ThreadReportState]. */
    fun reportUser(userId: String, reason: String, note: String?) {
        _reportState.value = ThreadReportState.Submitting
        viewModelScope.launch {
            _reportState.value = when (val result = reportsRepository.reportUser(userId, reason, note)) {
                is AuthResult.Success -> ThreadReportState.Submitted
                is AuthResult.Error -> ThreadReportState.Error(result.message)
            }
        }
    }

    /** Resets [reportState] back to [ThreadReportState.Idle], e.g. after the dialog is dismissed. */
    fun resetReportState() {
        _reportState.value = ThreadReportState.Idle
    }

    /** Blocks [userId]. See [ThreadBlockState]. */
    fun blockUser(userId: String) {
        _blockState.value = ThreadBlockState.Blocking
        viewModelScope.launch {
            _blockState.value = when (val result = blocksRepository.blockUser(userId)) {
                is AuthResult.Success -> {
                    val current = _uiState.value
                    if (current is MailboxThreadUiState.Success) {
                        _uiState.value = current.copy(isBlocked = true)
                    }
                    ThreadBlockState.Blocked
                }
                is AuthResult.Error -> ThreadBlockState.Error(result.message)
            }
        }
    }

    /** Unblocks [userId]. See [ThreadBlockState]. */
    fun unblockUser(userId: String) {
        _blockState.value = ThreadBlockState.Unblocking
        viewModelScope.launch {
            _blockState.value = when (val result = blocksRepository.unblockUser(userId)) {
                is AuthResult.Success -> {
                    val current = _uiState.value
                    if (current is MailboxThreadUiState.Success) {
                        _uiState.value = current.copy(isBlocked = false)
                    }
                    ThreadBlockState.Unblocked
                }
                is AuthResult.Error -> ThreadBlockState.Error(result.message)
            }
        }
    }

    /** Resets [blockState] back to [ThreadBlockState.Idle]. */
    fun resetBlockState() {
        _blockState.value = ThreadBlockState.Idle
    }
}
