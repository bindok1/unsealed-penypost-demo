package com.apps.unsealed.ui.screens.inbox.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.catalog.data.CatalogRepository
import com.apps.unsealed.feature.mailbox.data.MailboxRepository
import com.apps.unsealed.ui.screens.inbox.state.MailboxUiState
import com.apps.unsealed.ui.screens.inbox.state.toMailboxRoomItem
import com.apps.unsealed.ui.screens.selectrecipient.constants.localStampCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MailboxViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mailboxRepository: MailboxRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MailboxUiState>(MailboxUiState.Loading)
    val uiState: StateFlow<MailboxUiState> = _uiState.asStateFlow()

    init {
        loadMailbox()
    }

    fun loadMailbox(isSilent: Boolean = false) {
        if (!isSilent && _uiState.value !is MailboxUiState.Success) {
            _uiState.value = MailboxUiState.Loading
        }
        viewModelScope.launch {
            coroutineScope {
                val stampDeferred = async { catalogRepository.getStamps() }
                val mailboxDeferred = async { mailboxRepository.getMailbox() }

                val stampCatalog = (stampDeferred.await() as? AuthResult.Success)?.data
                    ?: localStampCatalog(context)

                when (val result = mailboxDeferred.await()) {
                    is AuthResult.Success -> {
                        _uiState.value = MailboxUiState.Success(result.data.map { it.toMailboxRoomItem(stampCatalog) })
                    }
                    is AuthResult.Error -> {
                        if (_uiState.value !is MailboxUiState.Success) {
                            _uiState.value = MailboxUiState.Error(result.message)
                        }
                    }
                }
            }
        }
    }
}
