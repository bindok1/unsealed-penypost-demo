package com.apps.unsealed.ui.screens.penpals.viewmodel

import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.blocks.data.BlocksRepository
import com.apps.unsealed.feature.catalog.data.CatalogItemDto
import com.apps.unsealed.feature.catalog.data.CatalogRepository
import com.apps.unsealed.feature.penpals.data.PenpalsRepository
import com.apps.unsealed.feature.reports.data.ReportsRepository
import com.apps.unsealed.ui.components.ReportReason
import com.apps.unsealed.ui.screens.compose.state.ReplyLetterContextHolder
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.penpals.state.toPenpalLetter
import com.apps.unsealed.ui.screens.selectrecipient.constants.localEnvelopeCatalog
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStampCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val FeedPageLimit = 20

@HiltViewModel
class PenPalsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: AnalyticsRepository,
    private val repository: PenpalsRepository,
    private val catalogRepository: CatalogRepository,
    private val reportsRepository: ReportsRepository,
    private val blocksRepository: BlocksRepository,
    private val authRepository: AuthRepository,
    private val replyLetterContextHolder: ReplyLetterContextHolder,
) : ViewModel() {

    fun prepareReplyLetter(letter: PenpalLetter) {
        replyLetterContextHolder.setReplyLetter(letter)
    }

    private val _letters = MutableStateFlow<List<PenpalLetter>>(emptyList())
    val letters: StateFlow<List<PenpalLetter>> = _letters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Viewer's current energy balance — spent by [refreshFeed], see
     * `docs/be/daily_penpal_stack.md` §6. Refreshed alongside the feed and
     * reconciled from each refresh response's `energy_remaining`, same
     * per-screen local-copy pattern as `StampsViewModel.userEnergy`. */
    private val _energyBalance = MutableStateFlow(0)
    val energyBalance: StateFlow<Int> = _energyBalance.asStateFlow()

    private val _isRefreshingStack = MutableStateFlow(false)
    val isRefreshingStack: StateFlow<Boolean> = _isRefreshingStack.asStateFlow()

    private val _refreshStackError = MutableStateFlow<String?>(null)
    val refreshStackError: StateFlow<String?> = _refreshStackError.asStateFlow()

    /** No bundled fallback — stickers are 100% CMS-driven, see
     * `docs/envelope-sticker-spec.md` §2/§10. */
    private val _stickerCatalog = MutableStateFlow<List<CatalogItemDto>>(emptyList())
    val stickerCatalog: StateFlow<List<CatalogItemDto>> = _stickerCatalog.asStateFlow()

    /** Bundled-fallback stamp catalog, same pattern as `SelectRecipientViewModel` —
     * needed to render the stamp overlay fallback when `envelopeCompositeImageUrl`
     * is null, see `docs/penpals-screen-spec.md` §Context. */
    private val _stampCatalog = MutableStateFlow(localStampCatalog(context))
    val stampCatalog: StateFlow<List<CatalogItemDto>> = _stampCatalog.asStateFlow()

    /** Cached from [loadFeed], reused by [refreshFeed] so a reroll doesn't
     * need to refetch the envelope catalog just to remap items. */
    private var envelopeCatalog: List<CatalogItemDto> = emptyList()

    init {
        loadFeed()
    }

    fun loadFeed() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            coroutineScope {
                val catalogDeferred = async { catalogRepository.getEnvelopes() }
                val stickerDeferred = async { catalogRepository.getStickers() }
                val stampDeferred = async { catalogRepository.getStamps() }
                val feedDeferred = async { repository.getFeed(limit = FeedPageLimit) }
                val profileDeferred = async { authRepository.fetchMe() }
                envelopeCatalog = (catalogDeferred.await() as? AuthResult.Success)?.data
                    ?: localEnvelopeCatalog(context)
                _stickerCatalog.value = (stickerDeferred.await() as? AuthResult.Success)?.data ?: emptyList()
                (stampDeferred.await() as? AuthResult.Success)?.data?.let { _stampCatalog.value = it }
                (profileDeferred.await() as? AuthResult.Success)?.data?.energy?.let { _energyBalance.value = it }
                when (val result = feedDeferred.await()) {
                    is AuthResult.Success -> {
                        _letters.value = result.data.items.map { it.toPenpalLetter(envelopeCatalog) }
                        _isLoading.value = false
                    }
                    is AuthResult.Error -> {
                        _error.value = result.message
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    /**
     * Rerolls today's stack early for `20` energy — see
     * `docs/be/daily_penpal_stack.md` §6. No daily limit server-side, so this
     * can be called repeatedly as long as the balance holds.
     */
    fun refreshFeed() {
        if (_isRefreshingStack.value) return
        _isRefreshingStack.value = true
        _refreshStackError.value = null
        viewModelScope.launch {
            when (val result = repository.refreshFeed()) {
                is AuthResult.Success -> {
                    _letters.value = result.data.items.map { it.toPenpalLetter(envelopeCatalog) }
                    _energyBalance.value = result.data.energyRemaining
                    _isRefreshingStack.value = false
                    analytics.logEvent("feed_refreshed")
                }
                is AuthResult.Error -> {
                    _refreshStackError.value = result.message
                    _isRefreshingStack.value = false
                }
            }
        }
    }

    fun clearRefreshStackError() {
        _refreshStackError.value = null
    }

    /**
     * Optimistic like toggle — state is flipped locally so the UI responds
     * instantly (~0 ms), then the server response reconciles the final values.
     * On network error the local state is reverted so nothing is left
     * inconsistent.
     */
    fun toggleLike(id: String) {
        // 1. Optimistic update
        _letters.update { list ->
            list.map { letter ->
                if (letter.id != id) letter
                else letter.copy(
                    viewerHasLiked = !letter.viewerHasLiked,
                    likeCount = if (letter.viewerHasLiked) letter.likeCount - 1
                                else letter.likeCount + 1,
                )
            }
        }

        // 2. Reconcile with server response
        viewModelScope.launch {
            when (val result = repository.toggleLike(id)) {
                is AuthResult.Success -> {
                    val nowLiked = result.data.viewerHasLiked
                    _letters.update { list ->
                        list.map { letter ->
                            if (letter.id != id) letter
                            else letter.copy(
                                viewerHasLiked = result.data.viewerHasLiked,
                                likeCount      = result.data.likeCount,
                            )
                        }
                    }
                    if (nowLiked) analytics.logEvent("letter_liked")
                }
                is AuthResult.Error -> {
                    // Revert optimistic update — flip back to previous state
                    _letters.update { list ->
                        list.map { letter ->
                            if (letter.id != id) letter
                            else letter.copy(
                                viewerHasLiked = !letter.viewerHasLiked,
                                likeCount = if (letter.viewerHasLiked) letter.likeCount + 1
                                            else letter.likeCount - 1,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Reports a letter (`target_type = "letter"`, see `docs/be/trust_and_safety_api.md` §1).
     * On success, the letter is hidden from this viewer's feed immediately — an optimistic
     * client-side hide, same idea as [toggleLike]'s optimistic update but applied to removal.
     * Returns whether the report succeeded so the caller can close its dialog/overlay.
     */
    suspend fun reportLetter(letterId: String, reason: ReportReason, note: String?): Boolean =
        when (val result = reportsRepository.reportLetter(letterId, reason.apiValue, note)) {
            is AuthResult.Success -> {
                _letters.update { list -> list.filterNot { it.id == letterId } }
                true
            }
            is AuthResult.Error -> false
        }

    /**
     * Blocks [userId] (the sender of the currently open letter). On success, all of that
     * sender's letters are hidden from this viewer's feed immediately — same optimistic
     * client-side hide idea as [reportLetter], just filtered by sender instead of letter id.
     * Returns whether the block succeeded so the caller can close its overlay.
     */
    suspend fun blockUser(userId: String): Boolean =
        when (blocksRepository.blockUser(userId)) {
            is AuthResult.Success -> {
                _letters.update { list -> list.filterNot { it.senderId == userId } }
                true
            }
            is AuthResult.Error -> false
        }
}
