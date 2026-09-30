package com.apps.unsealed.ui.screens.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.addressbook.data.AddressBookContactDto
import com.apps.unsealed.feature.addressbook.data.AddressBookRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.ui.screens.profile.state.AddressBookEntry
import com.apps.unsealed.ui.screens.profile.state.AddressBookUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun AddressBookContactDto.toAddressBookEntry() = AddressBookEntry(
    id = id,
    userId = targetUserId,
    name = displayName,
    country = location,
    photoUrl = photoUrl,
    isOnline = isOnline ?: false,
)

/** Drives the "Address Book" profile screen — loads from `GET /api/v1/address-book` and deletes via `DELETE /api/v1/address-book/{id}`. */
@HiltViewModel
class AddressBookViewModel @Inject constructor(
    private val addressBookRepository: AddressBookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddressBookUiState>(AddressBookUiState.Loading)
    val uiState: StateFlow<AddressBookUiState> = _uiState.asStateFlow()

    /** Address book record IDs with an in-flight delete request. */
    private val _deletingIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingIds: StateFlow<Set<String>> = _deletingIds.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = AddressBookUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = addressBookRepository.getAddressBook()) {
                is AuthResult.Success -> AddressBookUiState.Success(result.data.map { it.toAddressBookEntry() })
                is AuthResult.Error -> AddressBookUiState.Error(result.message)
            }
        }
    }

    /** Deletes contact with [id]. On success, removes it from the current list without a full reload. */
    fun deleteContact(id: String) {
        _deletingIds.update { it + id }
        viewModelScope.launch {
            when (val result = addressBookRepository.deleteContact(id)) {
                is AuthResult.Success -> {
                    val current = _uiState.value
                    if (current is AddressBookUiState.Success) {
                        _uiState.value = current.copy(items = current.items.filterNot { it.id == id })
                    }
                }
                is AuthResult.Error -> _deleteError.value = result.message
            }
            _deletingIds.update { it - id }
        }
    }

    fun resetDeleteError() {
        _deleteError.value = null
    }
}
