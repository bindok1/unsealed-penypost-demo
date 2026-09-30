package com.apps.unsealed.ui.screens.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.store.data.OwnedItemDto
import com.apps.unsealed.feature.store.data.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class KeepsakeCategory {
    ALL,
    STAMP,
    PAPER,
    STICKER,
    ENVELOPE,
}

data class CollectibleAssetUi(
    val id: String,
    val packId: String,
    val packTitle: String,
    val creatorName: String,
    val unlockedVia: String,
    val unlockedAt: String,
    val assetType: String, // "STAMP", "PAPER", "STICKER", "ENVELOPE"
    val assetUrl: String,
    val sortOrder: Int,
)

sealed interface KeepsakeUiState {
    data object Loading : KeepsakeUiState
    data class Error(val message: String) : KeepsakeUiState
    data class Success(
        val allItems: List<CollectibleAssetUi>,
        val selectedCategory: KeepsakeCategory = KeepsakeCategory.ALL,
        val totalStampsCount: Int = 0,
        val totalPapersCount: Int = 0,
        val totalStickersCount: Int = 0,
        val totalEnvelopesCount: Int = 0,
    ) : KeepsakeUiState {
        val displayedItems: List<CollectibleAssetUi>
            get() = when (selectedCategory) {
                KeepsakeCategory.ALL -> allItems
                KeepsakeCategory.STAMP -> allItems.filter { it.assetType.equals("STAMP", ignoreCase = true) }
                KeepsakeCategory.PAPER -> allItems.filter { it.assetType.equals("PAPER", ignoreCase = true) }
                KeepsakeCategory.STICKER -> allItems.filter { it.assetType.equals("STICKER", ignoreCase = true) }
                KeepsakeCategory.ENVELOPE -> allItems.filter { it.assetType.equals("ENVELOPE", ignoreCase = true) }
            }
    }
}

/**
 * ViewModel driving the Snail Mail Keepsake Album ("Buku Kenangan Snail Mail").
 * Fetches and flattens all user-owned collectibles (stamps, papers, stickers, envelopes)
 * from GET /api/v1/me/inventory.
 */
@HiltViewModel
class ProfileStampBookViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<KeepsakeUiState>(KeepsakeUiState.Loading)
    val uiState: StateFlow<KeepsakeUiState> = _uiState.asStateFlow()

    init {
        loadInventory()
    }

    fun loadInventory() {
        _uiState.value = KeepsakeUiState.Loading
        viewModelScope.launch {
            when (val result = storeRepository.getMyInventory()) {
                is AuthResult.Success -> {
                    val rawPacks: List<OwnedItemDto> = result.data
                    val collectibles = mutableListOf<CollectibleAssetUi>()

                    for (pack in rawPacks) {
                        if (pack.assets.isNotEmpty()) {
                            pack.assets.sortedBy { it.sortOrder }.forEachIndexed { idx, asset ->
                                collectibles.add(
                                    CollectibleAssetUi(
                                        id = "${pack.id}_${asset.assetType}_${asset.sortOrder}_$idx",
                                        packId = pack.id,
                                        packTitle = pack.title,
                                        creatorName = pack.creatorName.orEmpty().ifBlank { "Peny Creator" },
                                        unlockedVia = pack.unlockedVia,
                                        unlockedAt = pack.unlockedAt,
                                        assetType = asset.assetType.uppercase(),
                                        assetUrl = asset.assetUrl,
                                        sortOrder = asset.sortOrder,
                                    )
                                )
                            }
                        } else if (pack.thumbnailUrl.isNotBlank()) {
                            val type = when {
                                pack.category.contains("stamp", ignoreCase = true) -> "STAMP"
                                pack.category.contains("paper", ignoreCase = true) -> "PAPER"
                                pack.category.contains("sticker", ignoreCase = true) -> "STICKER"
                                pack.category.contains("envelope", ignoreCase = true) -> "ENVELOPE"
                                else -> "STAMP"
                            }
                            collectibles.add(
                                CollectibleAssetUi(
                                    id = pack.id,
                                    packId = pack.id,
                                    packTitle = pack.title,
                                    creatorName = pack.creatorName.orEmpty().ifBlank { "Peny Creator" },
                                    unlockedVia = pack.unlockedVia,
                                    unlockedAt = pack.unlockedAt,
                                    assetType = type,
                                    assetUrl = pack.thumbnailUrl,
                                    sortOrder = 0,
                                )
                            )
                        }
                    }

                    val stampsCount = collectibles.count { it.assetType.equals("STAMP", ignoreCase = true) }
                    val papersCount = collectibles.count { it.assetType.equals("PAPER", ignoreCase = true) }
                    val stickersCount = collectibles.count { it.assetType.equals("STICKER", ignoreCase = true) }
                    val envelopesCount = collectibles.count { it.assetType.equals("ENVELOPE", ignoreCase = true) }

                    _uiState.value = KeepsakeUiState.Success(
                        allItems = collectibles,
                        selectedCategory = KeepsakeCategory.ALL,
                        totalStampsCount = stampsCount,
                        totalPapersCount = papersCount,
                        totalStickersCount = stickersCount,
                        totalEnvelopesCount = envelopesCount,
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = KeepsakeUiState.Error(result.message)
                }
            }
        }
    }

    fun selectCategory(category: KeepsakeCategory) {
        _uiState.update { current ->
            if (current is KeepsakeUiState.Success) {
                current.copy(selectedCategory = category)
            } else current
        }
    }
}
