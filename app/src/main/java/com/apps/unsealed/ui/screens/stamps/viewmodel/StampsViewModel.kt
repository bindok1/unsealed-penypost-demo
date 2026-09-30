package com.apps.unsealed.ui.screens.stamps.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.core.analytics.AnalyticsRepository
import com.apps.unsealed.core.data.StampsEnergyTabRequestHolder
import com.apps.unsealed.feature.auth.data.AuthRepository
import com.apps.unsealed.feature.auth.data.AuthResult
import com.apps.unsealed.feature.store.data.StoreRepository
import com.apps.unsealed.ui.screens.stamps.constants.ComingSoonStoreFeature
import com.apps.unsealed.ui.screens.stamps.constants.EnergyProductGrants
import com.apps.unsealed.ui.screens.stamps.constants.StampsTab
import com.apps.unsealed.ui.screens.stamps.constants.StoreFilterCategory
import com.apps.unsealed.ui.screens.stamps.state.AssetUi
import com.apps.unsealed.ui.screens.stamps.state.CatalogItemUi
import com.apps.unsealed.ui.screens.stamps.state.CreatorUi
import com.apps.unsealed.ui.screens.stamps.state.ItemDetailUi
import com.apps.unsealed.ui.screens.stamps.state.ShowcaseItemUi
import com.apps.unsealed.ui.screens.stamps.state.StampsUiState
import com.apps.unsealed.ui.screens.stamps.state.StoreSection
import com.apps.unsealed.ui.screens.stamps.state.toDailyQuestItem
import com.apps.unsealed.ui.screens.stamps.state.toDailyRewardStatusItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StampsViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
    private val rewardsRepository: com.apps.unsealed.feature.rewards.data.RewardsRepository,
    private val authRepository: AuthRepository,
    private val storeRepository: StoreRepository,
    stampsEnergyTabRequestHolder: StampsEnergyTabRequestHolder,
) : ViewModel() {

    // STORE is the default tab — users land directly on the Creator Marketplace.
    // ENERGI is the override if another screen requested it (e.g. Delivery Tracking boost dialog).
    private val _uiState = MutableStateFlow(
        StampsUiState(
            isLoading = true,
            selectedTab = if (stampsEnergyTabRequestHolder.consume()) StampsTab.ENERGI else StampsTab.STORE,
        ),
    )
    val uiState: StateFlow<StampsUiState> = _uiState.asStateFlow()

    // No init{}-time loadData() call — StampsScreen's LaunchedEffect(Unit)
    // covers both the first load and every subsequent tab revisit (see its
    // doc comment for why a Composable-level effect refires on tab switches
    // even though this ViewModel instance itself survives them). A second
    // call here would just double the initial fetch for no benefit.

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            coroutineScope {
                val dailyDeferred = async { rewardsRepository.getDailyStatus() }
                val questsDeferred = async { rewardsRepository.getDailyQuests() }
                val profileDeferred = async { authRepository.fetchMe() }

                val dailyResult = dailyDeferred.await()
                val questsResult = questsDeferred.await()
                val profileResult = profileDeferred.await()

                val dailyItem = (dailyResult as? AuthResult.Success)?.data?.toDailyRewardStatusItem()
                val questItems = (questsResult as? AuthResult.Success)?.data?.map { it.toDailyQuestItem() }.orEmpty()
                val userEnergy = (profileResult as? AuthResult.Success)?.data?.energy ?: 100

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dailyStatus = dailyItem ?: it.dailyStatus,
                        quests = questItems,
                        userEnergy = userEnergy,
                    )
                }
            }
        }
    }

    // ─── Rewards / Hadiah tab ──────────────────────────────────────────────────

    fun claimDailyReward() {
        if (_uiState.value.isClaimingDaily || _uiState.value.dailyStatus?.canClaimToday == false) return

        viewModelScope.launch {
            _uiState.update { it.copy(isClaimingDaily = true) }
            when (val result = rewardsRepository.claimDailyReward()) {
                is AuthResult.Success -> {
                    val data = result.data
                    _uiState.update { state ->
                        val updatedSchedule = state.dailyStatus?.schedule?.map { dayItem ->
                            if (dayItem.isCurrent) dayItem.copy(isClaimed = true) else dayItem
                        }.orEmpty()
                        val updatedDaily = state.dailyStatus?.copy(
                            canClaimToday = false,
                            currentStreak = data.streak,
                            schedule = updatedSchedule,
                        )
                        state.copy(
                            isClaimingDaily = false,
                            userEnergy = data.currentEnergyBalance,
                            dailyStatus = updatedDaily,
                            recentlyClaimedEnergy = data.energyGranted,
                            unlockedSpecialItem = data.grantedSpecialItem,
                            showClaimCelebration = true,
                        )
                    }
                    analytics.logEvent(
                        "daily_reward_claimed",
                        bundleOf("streak" to data.streak, "energy_granted" to data.energyGranted),
                    )
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isClaimingDaily = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun claimQuest(questId: String) {
        if (questId in _uiState.value.claimingQuestIds) return

        viewModelScope.launch {
            _uiState.update { it.copy(claimingQuestIds = it.claimingQuestIds + questId) }
            when (val result = rewardsRepository.claimQuest(questId)) {
                is AuthResult.Success -> {
                    val data = result.data
                    _uiState.update { state ->
                        val updatedQuests = state.quests.map { quest ->
                            if (quest.id == questId) quest.copy(isClaimed = true) else quest
                        }
                        state.copy(
                            claimingQuestIds = state.claimingQuestIds - questId,
                            userEnergy = data.currentEnergyBalance,
                            quests = updatedQuests,
                        )
                    }
                    analytics.logEvent("quest_claimed", bundleOf("quest_id" to questId))
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            claimingQuestIds = it.claimingQuestIds - questId,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showClaimCelebration = false) }
    }

    fun unlockStamp(stampId: String) {
        viewModelScope.launch {
            when (val result = rewardsRepository.unlockStamp(stampId)) {
                is AuthResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            userEnergy = result.data.currentEnergyBalance,
                            showClaimCelebration = true,
                            comingSoonFeature = null,
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun unlockPaper(paperId: String) {
        viewModelScope.launch {
            when (val result = rewardsRepository.unlockPaper(paperId)) {
                is AuthResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            userEnergy = result.data.currentEnergyBalance,
                            showClaimCelebration = true,
                            comingSoonFeature = null,
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun onBuyStampPackClick(packId: String) {
        analytics.logEvent("coming_soon_clicked", bundleOf("feature" to "buy_stamps", "pack_id" to packId))
        _uiState.update { it.copy(comingSoonFeature = ComingSoonStoreFeature.BUY_STAMPS) }
    }

    fun onBuyPaperPackClick(packId: String) {
        analytics.logEvent("coming_soon_clicked", bundleOf("feature" to "buy_paper", "pack_id" to packId))
        _uiState.update { it.copy(comingSoonFeature = ComingSoonStoreFeature.BUY_PAPER) }
    }

    fun onJoinSupporterClubClick(tierId: String) {
        analytics.logEvent("coming_soon_clicked", bundleOf("feature" to "supporter_club", "tier_id" to tierId))
        _uiState.update { it.copy(comingSoonFeature = ComingSoonStoreFeature.SUPPORTER_CLUB) }
    }

    fun onComingSoonDismiss() {
        _uiState.update { it.copy(comingSoonFeature = null) }
    }

    fun onTabSelected(tab: StampsTab) {
        if (_uiState.value.selectedTab != tab) {
            analytics.logEvent("stamps_tab_selected", bundleOf("tab_name" to tab.name.lowercase()))
        }
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // ─── Energy tab ───────────────────────────────────────────────────────────

    fun onBuyEnergyClick() {
        // Purchases was never configured without a key (UnsealedApplication)
        // — PaywallDialog would crash calling Purchases.sharedInstance.
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) return
        analytics.logEvent("energy_paywall_opened")
        _uiState.update { it.copy(showEnergyPaywall = true) }
    }

    fun onEnergyPaywallDismissed() {
        _uiState.update { it.copy(showEnergyPaywall = false) }
    }

    /**
     * CLIENT-ONLY OPTIMISTIC UPDATE — no RevenueCat webhook or backend top-up
     * endpoint exists yet, so this bumps userEnergy locally via
     * EnergyProductGrants instead of trusting a server response (contrast
     * with claimDailyReward()/claimQuest()/unlockStamp()/unlockPaper() above,
     * which all trust currentEnergyBalance from a mutation response).
     * TODO(energy-backend): once the webhook + top-up endpoint exist, replace
     * with a re-fetch (authRepository.fetchMe()) or a dedicated
     * confirm-purchase endpoint call for the authoritative balance.
     */
    fun onEnergyPurchaseCompleted(productId: String) {
        val grant = EnergyProductGrants.getValue(productId)
        analytics.logEvent(
            "energy_purchase_success",
            bundleOf(
                "product_id" to productId,
                "energy_granted" to grant,
            ),
        )
        _uiState.update { it.copy(showEnergyPaywall = false, userEnergy = (it.userEnergy ?: 0) + grant) }
    }

    fun onEnergyPurchaseError(message: String) {
        analytics.logEvent("energy_purchase_failed", bundleOf("error_message" to message))
        _uiState.update { it.copy(showEnergyPaywall = false, energyPurchaseError = message) }
    }

    fun onEnergyPurchaseErrorDismissed() {
        _uiState.update { it.copy(energyPurchaseError = null) }
    }

    // ─── Peny Store (STORE tab) ───────────────────────────────────────────────

    /**
     * Loads both the showcase carousel and the first page of the catalog.
     * Called lazily from [StampsScreen] when the STORE tab is first selected
     * and the catalog is still empty.
     */
    fun loadStoreData() {
        analytics.logEvent("store_opened")
        analytics.logEvent("shop_opened")
        viewModelScope.launch {
            _uiState.update { it.copy(store = it.store.copy(isLoadingShowcase = true, isLoadingCatalog = true)) }
            coroutineScope {
                val showcaseDeferred = async { storeRepository.getShowcase() }
                val catalogDeferred = async {
                    storeRepository.getCatalog(
                        category = _uiState.value.store.selectedFilter.queryParam,
                        after = null,
                    )
                }
                val inventoryDeferred = async { storeRepository.getMyInventory() }

                val showcaseResult = showcaseDeferred.await()
                val catalogResult = catalogDeferred.await()
                val inventoryResult = inventoryDeferred.await()

                val inventoryOwnedIds = (inventoryResult as? AuthResult.Success)?.data
                    ?.map { it.id }?.toSet().orEmpty()

                _uiState.update { state ->
                    val combinedOwnedIds = state.store.ownedItemIds + inventoryOwnedIds

                    val mappedShowcase = (showcaseResult as? AuthResult.Success)?.data
                        ?.map {
                            val ui = it.toShowcaseItemUi()
                            if (ui.viewerOwns || combinedOwnedIds.contains(ui.id)) ui.copy(viewerOwns = true) else ui
                        }
                        ?: state.store.showcase

                    val mappedCatalog = (catalogResult as? AuthResult.Success)?.data?.items
                        ?.map {
                            val ui = it.toCatalogItemUi()
                            if (ui.viewerOwns || combinedOwnedIds.contains(ui.id)) ui.copy(viewerOwns = true) else ui
                        }
                        ?: state.store.catalog

                    val allOwnedIds = combinedOwnedIds +
                        mappedShowcase.filter { it.viewerOwns }.map { it.id } +
                        mappedCatalog.filter { it.viewerOwns }.map { it.id }

                    state.copy(
                        store = state.store.copy(
                            isLoadingShowcase = false,
                            isLoadingCatalog = false,
                            ownedItemIds = allOwnedIds,
                            showcase = mappedShowcase,
                            catalog = mappedCatalog,
                            nextCursor = (catalogResult as? AuthResult.Success)?.data?.nextCursor,
                            errorMessage = (catalogResult as? AuthResult.Error)?.message,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Changes the active category filter, resets pagination, and reloads the catalog.
     */
    fun onStoreFilterSelected(filter: StoreFilterCategory) {
        if (_uiState.value.store.selectedFilter == filter) return
        analytics.logEvent("store_filter_selected", bundleOf("category" to filter.queryParam))
        _uiState.update { it.copy(store = it.store.copy(selectedFilter = filter, catalog = emptyList(), nextCursor = null)) }
        viewModelScope.launch {
            _uiState.update { it.copy(store = it.store.copy(isLoadingCatalog = true)) }
            when (val result = storeRepository.getCatalog(category = filter.queryParam, after = null)) {
                is AuthResult.Success -> _uiState.update { state ->
                    val mappedItems = result.data.items.map {
                        val ui = it.toCatalogItemUi()
                        if (ui.viewerOwns || state.store.ownedItemIds.contains(ui.id)) ui.copy(viewerOwns = true) else ui
                    }
                    val newOwnedIds = state.store.ownedItemIds + mappedItems.filter { it.viewerOwns }.map { it.id }
                    state.copy(
                        store = state.store.copy(
                            isLoadingCatalog = false,
                            ownedItemIds = newOwnedIds,
                            catalog = mappedItems,
                            nextCursor = result.data.nextCursor,
                        ),
                    )
                }
                is AuthResult.Error -> _uiState.update { state ->
                    state.copy(store = state.store.copy(isLoadingCatalog = false, errorMessage = result.message))
                }
            }
        }
    }

    /**
     * Fetches the next page of catalog items using the stored cursor.
     * No-op if already loading or cursor is null (end of catalog).
     */
    fun loadMoreCatalog() {
        val store = _uiState.value.store
        if (store.isLoadingMore || store.nextCursor == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(store = it.store.copy(isLoadingMore = true)) }
            when (val result = storeRepository.getCatalog(
                category = store.selectedFilter.queryParam,
                after = store.nextCursor,
            )) {
                is AuthResult.Success -> _uiState.update { state ->
                    val newItems = result.data.items.map {
                        val ui = it.toCatalogItemUi()
                        if (ui.viewerOwns || state.store.ownedItemIds.contains(ui.id)) ui.copy(viewerOwns = true) else ui
                    }
                    val newOwnedIds = state.store.ownedItemIds + newItems.filter { it.viewerOwns }.map { it.id }
                    state.copy(
                        store = state.store.copy(
                            isLoadingMore = false,
                            ownedItemIds = newOwnedIds,
                            catalog = state.store.catalog + newItems,
                            nextCursor = result.data.nextCursor,
                        ),
                    )
                }
                is AuthResult.Error -> _uiState.update { state ->
                    state.copy(store = state.store.copy(isLoadingMore = false, errorMessage = result.message))
                }
            }
        }
    }

    /**
     * Opens the product detail bottom sheet for the given item ID.
     * Fetches detail in background; sheet renders skeleton until loaded.
     * Also fires a non-blocking view increment after a 1.5s delay.
     */
    fun onProductTapped(id: String) {
        _uiState.update { it.copy(store = it.store.copy(selectedItemId = id, isLoadingDetail = true, itemDetail = null)) }
        viewModelScope.launch {
            when (val result = storeRepository.getItemDetail(id)) {
                is AuthResult.Success -> {
                    val detailUi = result.data.toItemDetailUi()
                    analytics.logEvent(
                        "store_item_viewed",
                        bundleOf(
                            "item_id" to id,
                            "item_title" to detailUi.title,
                            "category" to detailUi.category,
                            "tier" to detailUi.tier,
                        ),
                    )
                    analytics.logEvent(
                        "product_viewed",
                        bundleOf(
                            "product_id" to id,
                            "product_name" to detailUi.title,
                            "category" to detailUi.category,
                            "tier" to detailUi.tier,
                        ),
                    )
                    _uiState.update { state ->
                        val isOwned = detailUi.viewerOwns
                        val updatedOwnedIds = if (isOwned) state.store.ownedItemIds + id else state.store.ownedItemIds
                        val updatedCatalog = if (isOwned) {
                            state.store.catalog.map { if (it.id == id) it.copy(viewerOwns = true) else it }
                        } else state.store.catalog
                        val updatedShowcase = if (isOwned) {
                            state.store.showcase.map { if (it.id == id) it.copy(viewerOwns = true) else it }
                        } else state.store.showcase

                        state.copy(
                            store = state.store.copy(
                                isLoadingDetail = false,
                                itemDetail = detailUi,
                                ownedItemIds = updatedOwnedIds,
                                catalog = updatedCatalog,
                                showcase = updatedShowcase,
                            ),
                        )
                    }
                }
                is AuthResult.Error -> _uiState.update { state ->
                    state.copy(store = state.store.copy(isLoadingDetail = false, errorMessage = result.message))
                }
            }
        }
        // Fire-and-forget view increment after 1.5s — backend is non-blocking.
        viewModelScope.launch {
            delay(1500)
            storeRepository.incrementViewAsync(id)
        }
    }

    fun dismissProductDetail() {
        _uiState.update { it.copy(store = it.store.copy(selectedItemId = null, itemDetail = null)) }
    }

    // ─── IAP Purchase (RevenueCat Offering: "creator_store") ─────────────────

    /**
     * Maps an item's [tier] string (e.g. "tier_1") to the corresponding
     * RevenueCat product ID registered in Play Console and configured in the
     * "creator_store" Offering: `creator_pack_tier_<N>`.
     */
    fun tierToRcProductId(tier: String): String {
        // API returns "tier_1" … "tier_5"; RC product IDs are "creator_pack_tier_1" … "creator_pack_tier_5".
        val tierNumber = tier.removePrefix("tier_").trim()
        return "creator_pack_tier_$tierNumber"
    }

    /**
     * Called when user taps "Beli" in [ProductDetailBottomSheet].
     * Sets [StoreSection.isPurchasing] = true so the CTA shows a loading
     * spinner while the RC package lookup happens in [StampsScreen].
     * The actual [com.revenuecat.purchases.Package] lookup + purchasePackage()
     * is done in the Composable because RC's Activity reference is needed.
     */
    fun onBuyItemClick(itemId: String? = null, tier: String? = null) {
        val params = bundleOf()
        if (itemId != null) params.putString("item_id", itemId)
        if (tier != null) params.putString("tier", tier)
        analytics.logEvent("store_purchase_initiated", params)
        _uiState.update { it.copy(store = it.store.copy(isPurchasing = true, purchasingItemId = itemId)) }
    }

    /**
     * Called from [StampsScreen]'s [PaywallListener.onPurchaseCompleted] after
     * RevenueCat confirms the transaction. Re-fetches the item detail so
     * [ItemDetailUi.viewerOwns] flips to `true` (server confirms via webhook).
     */
    fun onStorePurchaseSuccess(itemId: String) {
        val catalogItem = _uiState.value.store.catalog.find { it.id == itemId }
        val showcaseItem = _uiState.value.store.showcase.find { it.id == itemId }
        val detailItem = _uiState.value.store.itemDetail?.takeIf { it.id == itemId }

        val tier = catalogItem?.tier ?: showcaseItem?.tier ?: detailItem?.tier
        val category = catalogItem?.category ?: showcaseItem?.category ?: detailItem?.category

        val params = bundleOf("item_id" to itemId)
        if (tier != null) params.putString("tier", tier)
        if (category != null) params.putString("category", category)
        analytics.logEvent("store_purchase_success", params)

        val productParams = bundleOf("product_id" to itemId)
        if (tier != null) productParams.putString("tier", tier)
        if (category != null) productParams.putString("category", category)
        analytics.logEvent("product_purchased", productParams)

        _uiState.update { state ->
            val updatedDetail = state.store.itemDetail?.let {
                if (it.id == itemId) it.copy(viewerOwns = true) else it
            }
            val updatedCatalog = state.store.catalog.map {
                if (it.id == itemId) it.copy(viewerOwns = true) else it
            }
            val updatedShowcase = state.store.showcase.map {
                if (it.id == itemId) it.copy(viewerOwns = true) else it
            }
            state.copy(
                store = state.store.copy(
                    isPurchasing = false,
                    purchasingItemId = null,
                    ownedItemIds = state.store.ownedItemIds + itemId,
                    itemDetail = updatedDetail ?: state.store.itemDetail,
                    catalog = updatedCatalog,
                    showcase = updatedShowcase,
                    showPurchaseSuccess = true,
                )
            )
        }
        viewModelScope.launch {
            // Refetch to get server-confirmed viewerOwns = true
            when (val result = storeRepository.getItemDetail(itemId)) {
                is AuthResult.Success -> _uiState.update { state ->
                    val detailUi = result.data.toItemDetailUi()
                    state.copy(
                        store = state.store.copy(
                            itemDetail = detailUi,
                            ownedItemIds = state.store.ownedItemIds + itemId,
                            catalog = state.store.catalog.map { if (it.id == itemId) it.copy(viewerOwns = true) else it },
                            showcase = state.store.showcase.map { if (it.id == itemId) it.copy(viewerOwns = true) else it },
                        ),
                    )
                }
                is AuthResult.Error -> Unit
            }
        }
    }

    /** Called when user cancels the RC paywall — no error shown. */
    fun onStorePurchaseCancelled() {
        val itemId = _uiState.value.store.purchasingItemId
        val params = bundleOf()
        if (itemId != null) params.putString("item_id", itemId)
        analytics.logEvent("store_purchase_cancelled", params)
        _uiState.update { it.copy(store = it.store.copy(isPurchasing = false, purchasingItemId = null)) }
    }

    /** Called when RC purchase fails with an error (not a user cancellation). */
    fun onStorePurchaseError(message: String) {
        val itemId = _uiState.value.store.purchasingItemId
        val params = bundleOf("error_message" to message)
        if (itemId != null) params.putString("item_id", itemId)
        analytics.logEvent("store_purchase_failed", params)
        _uiState.update { it.copy(store = it.store.copy(isPurchasing = false, purchasingItemId = null, purchaseError = message)) }
    }

    /**
     * Called after RevenueCat offerings are fetched to cache localized price strings.
     * [priceByTier] maps tier key (e.g. "tier_1") to a formatted local price string
     * (e.g. "$3.99", "₱89", "Rp35.000") so catalog cards show the user's local currency.
     */
    fun onRcPricesLoaded(priceByTier: Map<String, String>) {
        if (priceByTier.isEmpty()) return
        _uiState.update { it.copy(store = it.store.copy(localizedPriceByTier = priceByTier)) }
    }

    fun dismissStorePurchaseSuccess() {
        _uiState.update { it.copy(store = it.store.copy(showPurchaseSuccess = false)) }
    }

    fun dismissStorePurchaseError() {
        _uiState.update { it.copy(store = it.store.copy(purchaseError = null)) }
    }

    /**
     * Optimistic like toggle: flips the icon & count immediately in the UI,
     * then confirms with the server. Rolls back on network failure.
     */
    fun toggleLike(itemId: String) {
        val wasLiked = _uiState.value.store.catalog.find { it.id == itemId }?.viewerHasLiked
            ?: _uiState.value.store.itemDetail?.takeIf { it.id == itemId }?.viewerHasLiked
            ?: return
        val nowLiked = !wasLiked
        // 1. Optimistic update
        _uiState.update { state ->
            val updated = state.store.catalog.map {
                if (it.id == itemId) it.copy(
                    viewerHasLiked = !wasLiked,
                    likesCount = if (wasLiked) it.likesCount - 1 else it.likesCount + 1,
                ) else it
            }
            val updatedDetail = state.store.itemDetail?.let {
                if (it.id == itemId) it.copy(
                    viewerHasLiked = !wasLiked,
                    likesCount = if (wasLiked) it.likesCount - 1 else it.likesCount + 1,
                ) else it
            }
            state.copy(store = state.store.copy(catalog = updated, itemDetail = updatedDetail))
        }
        // 2. API call → confirm or rollback
        viewModelScope.launch {
            when (val result = storeRepository.toggleLike(itemId)) {
                is AuthResult.Success -> {
                    val serverData = result.data
                    if (nowLiked) {
                        analytics.logEvent("store_item_liked", bundleOf("item_id" to itemId))
                    }
                    _uiState.update { state ->
                        val confirmed = state.store.catalog.map {
                            if (it.id == itemId) it.copy(
                                viewerHasLiked = serverData.hasLiked,
                                likesCount = serverData.likesCount,
                            ) else it
                        }
                        val confirmedDetail = state.store.itemDetail?.let {
                            if (it.id == itemId) it.copy(
                                viewerHasLiked = serverData.hasLiked,
                                likesCount = serverData.likesCount,
                            ) else it
                        }
                        state.copy(store = state.store.copy(catalog = confirmed, itemDetail = confirmedDetail))
                    }
                }
                is AuthResult.Error -> {
                    // Rollback to the original state
                    _uiState.update { state ->
                        val rolled = state.store.catalog.map {
                            if (it.id == itemId) it.copy(
                                viewerHasLiked = wasLiked,
                                likesCount = if (wasLiked) it.likesCount + 1 else it.likesCount - 1,
                            ) else it
                        }
                        val rolledDetail = state.store.itemDetail?.let {
                            if (it.id == itemId) it.copy(
                                viewerHasLiked = wasLiked,
                                likesCount = if (wasLiked) it.likesCount + 1 else it.likesCount - 1,
                            ) else it
                        }
                        state.copy(store = state.store.copy(catalog = rolled, itemDetail = rolledDetail))
                    }
                }
            }
        }
    }

    // ─── DTO → UI mapping (private helpers) ──────────────────────────────────

    private fun com.apps.unsealed.feature.store.data.ShowcaseItemDto.toShowcaseItemUi() = ShowcaseItemUi(
        id = id,
        title = title,
        subDescription = subDescription,
        bannerUrl = showcaseBannerUrl,
        thumbnailUrl = thumbnailUrl,
        tier = tier,
        category = category,
        creatorName = creatorName,
        priceIdr = priceIdr,
        priceUsd = priceUsd,
        viewerOwns = viewerOwns,
    )

    private fun com.apps.unsealed.feature.store.data.CatalogItemDto.toCatalogItemUi() = CatalogItemUi(
        id = id,
        title = title,
        subDescription = subDescription,
        thumbnailUrl = thumbnailUrl,
        category = category,
        tier = tier,
        priceIdr = priceIdr,
        priceUsd = priceUsd,
        creatorId = creatorId,
        creatorName = creatorName,
        stampsCount = stampsCount,
        stickersCount = stickersCount,
        papersCount = papersCount,
        envelopesCount = envelopesCount,
        likesCount = likesCount,
        viewerHasLiked = viewerHasLiked,
        viewerOwns = viewerOwns,
        assets = assets.sortedBy { it.sortOrder }.map { AssetUi(it.assetType, it.assetUrl, it.sortOrder) },
    )

    private fun com.apps.unsealed.feature.store.data.ItemDetailDto.toItemDetailUi() = ItemDetailUi(
        id = id,
        title = title,
        description = description,
        subDescription = subDescription,
        thumbnailUrl = thumbnailUrl,
        bannerUrl = showcaseBannerUrl,
        tier = tier,
        priceIdr = priceIdr,
        priceUsd = priceUsd,
        category = category,
        stampsCount = stampsCount,
        stickersCount = stickersCount,
        papersCount = papersCount,
        envelopesCount = envelopesCount,
        likesCount = likesCount,
        viewerHasLiked = viewerHasLiked,
        viewerOwns = viewerOwns,
        assets = assets.sortedBy { it.sortOrder }.map { AssetUi(it.assetType, it.assetUrl, it.sortOrder) },
        creator = CreatorUi(
            id = creator.id,
            name = creator.name,
            avatarUrl = creator.avatarUrl,
            bio = creator.bio,
            externalLink = creator.externalLink,
        ),
    )
}
