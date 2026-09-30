package com.apps.unsealed.ui.screens.stamps.state

import com.apps.unsealed.ui.screens.stamps.constants.ComingSoonStoreFeature
import com.apps.unsealed.ui.screens.stamps.constants.StampsTab

data class StampsUiState(
    val isLoading: Boolean = false,
    val userEnergy: Int? = null,
    val dailyStatus: DailyRewardStatusItem? = null,
    val quests: List<DailyQuestItem> = emptyList(),
    val isClaimingDaily: Boolean = false,
    val claimingQuestIds: Set<String> = emptySet(),
    val recentlyClaimedEnergy: Int? = null,
    val unlockedSpecialItem: String? = null,
    val showClaimCelebration: Boolean = false,
    val comingSoonFeature: ComingSoonStoreFeature? = null,
    val errorMessage: String? = null,
    val selectedTab: StampsTab = StampsTab.STORE,
    val showEnergyPaywall: Boolean = false,
    val energyPurchaseError: String? = null,
    /** Isolated state for the Peny Store (STORE tab). */
    val store: StoreSection = StoreSection(),
)

