package com.apps.unsealed.ui.screens.stamps.screen

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.LocalActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.BuildConfig
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberAnimatedCount
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.staggerEntrance
import com.apps.unsealed.ui.components.LetterlyTopSnackbar
import com.apps.unsealed.ui.components.SnackbarStyle
import com.apps.unsealed.ui.screens.compose.widgets.ComingSoonBottomSheet
import com.apps.unsealed.ui.screens.stamps.constants.ComingSoonStoreFeature
import com.apps.unsealed.ui.screens.stamps.constants.EnergyProductGrants
import com.apps.unsealed.ui.screens.stamps.constants.StampsTab
import com.apps.unsealed.ui.screens.stamps.viewmodel.StampsViewModel
import com.apps.unsealed.ui.screens.stamps.widgets.DailyQuestsSection
import com.apps.unsealed.ui.screens.stamps.widgets.DailyStreakRewardCard
import com.apps.unsealed.ui.screens.stamps.widgets.InviteFriendsPromoCard
import com.apps.unsealed.ui.screens.stamps.widgets.PenyStoreSection
import com.apps.unsealed.ui.screens.stamps.widgets.ProductDetailBottomSheet
import com.apps.unsealed.ui.screens.stamps.widgets.ShowcaseBannerCarousel
import com.apps.unsealed.ui.screens.stamps.widgets.SectionHeader
import com.apps.unsealed.ui.screens.stamps.widgets.StampsSkeleton
import com.apps.unsealed.ui.screens.stamps.widgets.StorePurchaseCelebrationModal
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.IceSkyAccent
import com.apps.unsealed.ui.theme.LemonYellow
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Composable
fun StampsScreen(
    onInviteFriendsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: StampsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalActivity.current
    val storePurchaseFailedMessage = stringResource(R.string.error_store_purchase_failed)
    var demoSnackbarMessage by remember { mutableStateOf<String?>(null) }

    val handleBuyEnergyClick: () -> Unit = remember {
        {
            if (BuildConfig.REVENUECAT_API_KEY.isNotBlank() && Purchases.isConfigured) {
                viewModel.onBuyEnergyClick()
            } else {
                viewModel.onEnergyPurchaseCompleted("penypost_energy_50")
                demoSnackbarMessage = "⚡ Demo Mode: +50 Energi berhasil ditambahkan!"
            }
        }
    }

    val initiateStorePurchase: (String, String) -> Unit = remember(activity, storePurchaseFailedMessage) {
        { itemId, tier ->
            if (BuildConfig.REVENUECAT_API_KEY.isNotBlank() && Purchases.isConfigured) {
                viewModel.onBuyItemClick(itemId, tier)
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid != null && Purchases.sharedInstance.isAnonymous) {
                    Purchases.sharedInstance.logIn(currentUid)
                }
                // Attach item_id and target_item_id as subscriber attributes for RevenueCat webhook
                Purchases.sharedInstance.setAttributes(
                    mapOf(
                        "item_id" to itemId,
                        "target_item_id" to itemId,
                    )
                )
                val targetProductId = viewModel.tierToRcProductId(tier)
                Purchases.sharedInstance.getOfferingsWith(
                    onError = { error ->
                        viewModel.onStorePurchaseError(error.message)
                    },
                    onSuccess = { offerings ->
                        val offering = offerings["creator_store"] ?: offerings.current
                        val rcPackage = offering
                            ?.availablePackages
                            ?.firstOrNull { pkg ->
                                pkg.product.id == targetProductId
                            }
                        if (rcPackage == null) {
                            viewModel.onStorePurchaseError(storePurchaseFailedMessage)
                            return@getOfferingsWith
                        }
                        activity?.let { act ->
                            Purchases.sharedInstance.purchaseWith(
                                purchaseParams = com.revenuecat.purchases.PurchaseParams.Builder(act, rcPackage).build(),
                                onError = { error, userCancelled ->
                                    if (userCancelled) {
                                        viewModel.onStorePurchaseCancelled()
                                    } else {
                                        viewModel.onStorePurchaseError(error.message)
                                    }
                                },
                                onSuccess = { _, _ ->
                                    viewModel.onStorePurchaseSuccess(itemId)
                                },
                            )
                        } ?: viewModel.onStorePurchaseError(storePurchaseFailedMessage)
                    },
                )
            } else {
                // Demo Mode: Simulate purchase success & celebration
                viewModel.onStorePurchaseSuccess(itemId)
                demoSnackbarMessage = "✨ Demo Mode: Pembelian berhasil disimulasikan!"
            }
        }
    }
    var entranceIndex = 0

    // Reset / load store data when switching to STORE tab
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == StampsTab.STORE && uiState.store.catalog.isEmpty()) {
            viewModel.loadStoreData()
        }
    }

    // Fetch localized prices from RevenueCat so catalog cards reflect the user's local currency.
    // Runs once when STORE tab is first opened and RC prices haven't been loaded yet.
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == StampsTab.STORE &&
            uiState.store.localizedPriceByTier.isEmpty() &&
            BuildConfig.REVENUECAT_API_KEY.isNotBlank() &&
            Purchases.isConfigured
        ) {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { /* silent — UI falls back to IDR */ },
                onSuccess = { offerings ->
                    val offering = offerings["creator_store"] ?: offerings.current
                    val priceByTier = offering
                        ?.availablePackages
                        ?.mapNotNull { pkg ->
                            // RC product ID format: "creator_pack_tier_1" → tier key "tier_1"
                            val tier = pkg.product.id
                                .removePrefix("creator_pack_")
                                .takeIf { it.startsWith("tier_") }
                                ?: return@mapNotNull null
                            tier to pkg.product.price.formatted
                        }
                        ?.toMap()
                        .orEmpty()
                    viewModel.onRcPricesLoaded(priceByTier)
                },
            )
        }
    }

    LaunchedEffect(Unit) { viewModel.loadData() }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {

        // ── STORE tab: shop_bg texture + dark scrim ───────────────────────
        if (uiState.selectedTab == StampsTab.STORE) {
            Image(
                painter = painterResource(R.drawable.shop_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Header: Title + Energy Pill ────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.stamps_screen_back_desc),
                                tint = Color.White,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.stamps_screen_title),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))
                val userEnergy = uiState.userEnergy
                if (userEnergy != null) {
                    Surface(
                        onClick = handleBuyEnergyClick,
                        shape = RoundedCornerShape(20.dp),
                        color = ToolbarFrostedDark,
                        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(BrandGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "${rememberAnimatedCount(userEnergy)}",
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandGold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // ── Sticky Tab Selector ───────────────────────────────────────
            StampsTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelect = viewModel::onTabSelected,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(6.dp))

            // ── Tab Content ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (uiState.isLoading && uiState.dailyStatus == null && uiState.selectedTab != StampsTab.STORE) {
                    StampsSkeleton()
                } else {
                    when (uiState.selectedTab) {
                        StampsTab.STORE -> {
                            PenyStoreSection(
                                store = uiState.store,
                                onProductTap = viewModel::onProductTapped,
                                onBuyProduct = { item -> if (!item.viewerOwns) initiateStorePurchase(item.id, item.tier) },
                                onBuyShowcaseItem = { item -> if (!item.viewerOwns) initiateStorePurchase(item.id, item.tier) },
                                onLikeClick = viewModel::toggleLike,
                                onFilterSelect = viewModel::onStoreFilterSelected,
                                onLoadMore = viewModel::loadMoreCatalog,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        StampsTab.HADIAH -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 6.dp,
                                    bottom = 16.dp,
                                ),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item {
                                    SectionHeader(stringResource(R.string.stamps_screen_section_claim_rewards))
                                    Spacer(Modifier.height(10.dp))
                                    DailyStreakRewardCard(
                                        dailyStatus = uiState.dailyStatus,
                                        isClaiming = uiState.isClaimingDaily,
                                        onClaimClick = viewModel::claimDailyReward,
                                        modifier = Modifier.staggerEntrance(entranceIndex++),
                                    )
                                }

                                if (uiState.quests.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(20.dp))
                                        DailyQuestsSection(
                                            quests = uiState.quests,
                                            claimingQuestIds = uiState.claimingQuestIds,
                                            onClaimQuestClick = viewModel::claimQuest,
                                            modifier = Modifier.staggerEntrance(entranceIndex++),
                                        )
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(20.dp))
                                    InviteFriendsPromoCard(
                                        onClick = onInviteFriendsClick,
                                        modifier = Modifier.staggerEntrance(entranceIndex++),
                                    )
                                }

                                item {
                                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }

                        StampsTab.ENERGI -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 6.dp,
                                    bottom = 16.dp,
                                ),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item {
                                    EnergyStoreSection(
                                        onBuyEnergyClick = handleBuyEnergyClick,
                                        modifier = Modifier.staggerEntrance(entranceIndex++),
                                    )
                                }
                                item {
                                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.store.selectedItemId != null) {
            ProductDetailBottomSheet(
                detail = uiState.store.itemDetail,
                isLoadingDetail = uiState.store.isLoadingDetail,
                isPurchasing = uiState.store.isPurchasing,
                onDismiss = viewModel::dismissProductDetail,
                onToggleLike = { uiState.store.itemDetail?.id?.let { viewModel.toggleLike(it) } },
                onBuyClick = {
                    val detail = uiState.store.itemDetail ?: return@ProductDetailBottomSheet
                    initiateStorePurchase(detail.id, detail.tier)
                },
                localizedPrice = uiState.store.itemDetail?.tier
                    ?.let { uiState.store.localizedPriceByTier[it] },
            )
        }

        // ── Store Purchase Success Celebration Modal ─────────────────────
        if (uiState.store.showPurchaseSuccess) {
            StorePurchaseCelebrationModal(
                itemDetail = uiState.store.itemDetail,
                onDismiss = {
                    viewModel.dismissStorePurchaseSuccess()
                    viewModel.dismissProductDetail()
                },
                onExploreMore = {
                    viewModel.dismissStorePurchaseSuccess()
                    viewModel.dismissProductDetail()
                },
            )
        }

        // ── Konfetti Celebration on Claim ─────────────────────────────────
        if (uiState.showClaimCelebration) {
            KonfettiView(
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 28f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(0xFFF7C844.toInt(), 0xFFE5A93C.toInt(), 0xFFFEE685.toInt(), 0xFFFFFFFF.toInt()),
                        position = Position.Relative(0.5, 0.35),
                        emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(80),
                    )
                ),
                modifier = Modifier.fillMaxSize(),
            )

            ClaimRewardCelebrationDialog(
                energyGranted = uiState.recentlyClaimedEnergy ?: 20,
                unlockedItem = uiState.unlockedSpecialItem,
                onDismiss = viewModel::dismissCelebration,
            )
        }

        // ── Coming Soon Bottom Sheet ──────────────────────────────────────
        val comingSoonTitle = when (uiState.comingSoonFeature) {
            ComingSoonStoreFeature.BUY_STAMPS -> stringResource(R.string.stamps_screen_coming_soon_buy_stamps)
            ComingSoonStoreFeature.BUY_PAPER -> stringResource(R.string.stamps_screen_coming_soon_buy_paper)
            ComingSoonStoreFeature.SUPPORTER_CLUB -> stringResource(R.string.stamps_screen_coming_soon_supporter_club)
            null -> null
        }
        if (comingSoonTitle != null) {
            ComingSoonBottomSheet(title = comingSoonTitle, onDismiss = viewModel::onComingSoonDismiss)
        }

        // ── RevenueCat Paywall ────────────────────────────────────────────
        if (uiState.showEnergyPaywall) {
            val purchaseFailedMessage = stringResource(R.string.error_stamps_energy_purchase_failed)
            PaywallDialog(
                PaywallDialogOptions.Builder()
                    .setDismissRequest { viewModel.onEnergyPaywallDismissed() }
                    .setListener(object : PaywallListener {
                        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                            val productId = storeTransaction.productIds.firstOrNull()
                            if (productId != null && EnergyProductGrants.containsKey(productId)) {
                                viewModel.onEnergyPurchaseCompleted(productId)
                            } else {
                                viewModel.onEnergyPurchaseError(purchaseFailedMessage)
                            }
                        }

                        override fun onPurchaseError(error: PurchasesError) {
                            viewModel.onEnergyPurchaseError(purchaseFailedMessage)
                        }

                        override fun onPurchaseCancelled() {
                            viewModel.onEnergyPaywallDismissed()
                        }
                    })
                    .build(),
            )
        }


        // Two independent LetterlyTopSnackbar instances — energy purchase errors
        // and store purchase errors are separate flows that don't interfere.
        LetterlyTopSnackbar(
            message = uiState.energyPurchaseError,
            onDismiss = viewModel::onEnergyPurchaseErrorDismissed,
            style = SnackbarStyle.Error,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        LetterlyTopSnackbar(
            message = uiState.store.purchaseError,
            onDismiss = viewModel::dismissStorePurchaseError,
            style = SnackbarStyle.Error,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        // Demo purchase success snackbar (when RevenueCat is not configured)
        LetterlyTopSnackbar(
            message = demoSnackbarMessage,
            onDismiss = { demoSnackbarMessage = null },
            style = SnackbarStyle.Info,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}


@Composable
private fun StampsTabSelector(
    selectedTab: StampsTab,
    onTabSelect: (StampsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandInkDeep)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Enum declaration order = render order: STORE, HADIAH, ENERGI
        listOf(
            StampsTab.STORE  to stringResource(R.string.stamps_screen_tab_store),
            StampsTab.HADIAH to stringResource(R.string.stamps_screen_tab_hadiah),
            StampsTab.ENERGI to stringResource(R.string.stamps_screen_tab_energi),
        ).forEach { (tab, label) ->
            val isActive = selectedTab == tab
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) BrandGold.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (isActive) 1.dp else 0.dp,
                        color = if (isActive) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onTabSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = NunitoFontFamily,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (isActive) BrandGold else Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

/**
 * What Energy actually buys, spelled out in Peny's voice — instant delivery
 * (see DeliveryTrackingViewModel's energy-gated unlock), Mode Peny's AI reply
 * assist (see ComposeViewModel's PenyReplyResult.InsufficientEnergy), and the
 * stamp/paper packs below this section (see [dummyStampPacks]/[dummyPaperPacks]).
 * Framed as benefits rather than a bare "buy currency" pitch, per
 * docs/copywriting/cp.md Rule A (human-first, short, active CTA).
 */
private data class EnergyBenefit(
    val icon: ImageVector,
    val tint: Color,
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val bodyRes: Int,
)

private val EnergyBenefits = listOf(
    EnergyBenefit(Icons.Filled.Bolt, BrandGold, R.string.stamps_energy_store_benefit_delivery_title, R.string.stamps_energy_store_benefit_delivery_body),
    EnergyBenefit(Icons.Filled.AutoAwesome, LemonYellow, R.string.stamps_energy_store_benefit_peny_title, R.string.stamps_energy_store_benefit_peny_body),
    EnergyBenefit(Icons.Filled.LocalPostOffice, IceSkyAccent, R.string.stamps_energy_store_benefit_shop_title, R.string.stamps_energy_store_benefit_shop_body),
)

@Composable
private fun EnergyStoreSection(onBuyEnergyClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ToolbarFrostedDark,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val isReducedMotion = rememberIsReducedMotion()
            var isBadgeVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isBadgeVisible = true }
            val badgeScale by animateFloatAsState(
                targetValue = if (isBadgeVisible) 1f else 0f,
                animationSpec = reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
                label = "energyBadgeScale",
            )
            val infiniteTransition = rememberInfiniteTransition(label = "flyingLetterAnim")
            val letterScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "flyingLetterScale",
            )
            val letterRotation by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "flyingLetterRotation",
            )
            val letterBobY by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "flyingLetterBobY",
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flying_letter),
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            val s = if (isReducedMotion) 1f else letterScale
                            val r = if (isReducedMotion) 0f else letterRotation
                            val y = if (isReducedMotion) 0f else letterBobY
                            scaleX = s
                            scaleY = s
                            rotationZ = r
                            translationY = y
                        },
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.stamps_energy_store_title),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stamps_energy_store_body),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EnergyBenefits.forEachIndexed { index, benefit ->
                    EnergyBenefitRow(benefit = benefit, index = index)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBuyEnergyClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGold, contentColor = BrandInkDeep),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stamps_energy_store_cta),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/** One "what Energy buys" row — icon chip + title + one-line body, staggered
 * in on section entrance per motion-rules.md §3.7 (translateX + fade, not a
 * decorative idle loop). */
@Composable
private fun EnergyBenefitRow(benefit: EnergyBenefit, index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().staggerEntrance(index),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(benefit.tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(benefit.icon, contentDescription = null, tint = benefit.tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(benefit.titleRes),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
            )
            Text(
                text = stringResource(benefit.bodyRes),
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ClaimRewardCelebrationDialog(
    energyGranted: Int,
    unlockedItem: String?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ToolbarFrostedDark,
            border = BorderStroke(1.5.dp, BrandGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(BrandGold.copy(alpha = 0.25f))
                        .border(1.5.dp, BrandGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.stamps_claim_celebration_title),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "+$energyGranted Energi Peni ⚡",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandGold,
                )

                if (unlockedItem != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = if (unlockedItem == "seal_wax_gold") {
                                stringResource(R.string.stamps_milestone_day3_item)
                            } else if (unlockedItem == "stamp_peni_aurora") {
                                stringResource(R.string.stamps_milestone_day7_item)
                            } else {
                                "Bonus Spesial: $unlockedItem ✨"
                            },
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGold,
                        contentColor = BrandInkDeep,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stamps_milestone_close),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
