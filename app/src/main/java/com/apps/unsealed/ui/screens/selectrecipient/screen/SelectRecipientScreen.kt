package com.apps.unsealed.ui.screens.selectrecipient.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.core.util.MinCompositingDensity
import com.apps.unsealed.core.util.captureIntoGraphicsLayer
import com.apps.unsealed.core.util.rememberGraphicsLayer
import com.apps.unsealed.core.util.toCompositeBytes
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyTopSnackbar
import com.apps.unsealed.ui.components.SnackbarStyle
import com.apps.unsealed.ui.screens.compose.widgets.ComingSoonBottomSheet
import com.apps.unsealed.ui.screens.compose.widgets.FontPickerSheet
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.selectrecipient.constants.StampDesign
import com.apps.unsealed.ui.screens.selectrecipient.state.ComingSoonFeature
import com.apps.unsealed.ui.screens.selectrecipient.state.SelectRecipientUiState
import com.apps.unsealed.ui.screens.selectrecipient.viewmodel.SelectRecipientViewModel
import com.apps.unsealed.ui.screens.selectrecipient.widgets.EnvelopeCard
import com.apps.unsealed.ui.screens.selectrecipient.widgets.EnvelopeEditToolbar
import com.apps.unsealed.ui.screens.selectrecipient.widgets.EnvelopePickerSheet
import com.apps.unsealed.ui.screens.selectrecipient.widgets.RegionPickerSheet
import com.apps.unsealed.ui.screens.selectrecipient.widgets.SelectRecipientHeader
import com.apps.unsealed.ui.screens.selectrecipient.widgets.SendSuccessOverlay
import com.apps.unsealed.ui.screens.selectrecipient.widgets.StampPickerOverlay
import com.apps.unsealed.ui.screens.selectrecipient.widgets.StickerPickerOverlay
import kotlinx.coroutines.launch

/**
 * Front-end "Select Recipient" screen, reached from ComposeScreen's Send button.
 * Layout: header (back + animated heading/hint + Next/Send action) ->
 * envelope card (background = one of [EnvelopeDesign]) -> always-visible edit toolbar.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SelectRecipientScreen(
    onBackClick: () -> Unit,
    onSendSuccess: (
        estimatedArrivalAt: String,
        recipientContinent: String,
        recipientName: String,
        letterId: String,
        visibility: String,
        compositeImageUrl: String?,
    ) -> Unit,
    onNavigateToStore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: SelectRecipientViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val regionCounts by viewModel.regionCounts.collectAsState()
    val addressBookContacts by viewModel.addressBookContacts.collectAsState()
    val isAddressBookLoading by viewModel.isAddressBookLoading.collectAsState()
    val envelopeCatalog by viewModel.envelopeCatalog.collectAsState()
    val stampCatalog by viewModel.stampCatalog.collectAsState()
    val stickerCatalog by viewModel.stickerCatalog.collectAsState()

    // Return-address corner of EnvelopeCard's header — this gets captured
    // into the composite sent to the backend (docs/penpals-screen-spec.md
    // §Context), so it must be the real sender, not a placeholder.
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    val senderName = when (val state = authUiState) {
        is AuthUiState.Authenticated -> state.user.nickname
        is AuthUiState.NeedsOnboarding -> state.user.nickname
        else -> stringResource(R.string.select_recipient_sender_name)
    }
    val senderCountry = when (val state = authUiState) {
        is AuthUiState.Authenticated -> stringResource(PenpalRegion.fromApiString(state.user.continent).labelRes)
        is AuthUiState.NeedsOnboarding -> stringResource(PenpalRegion.fromApiString(state.user.continent).labelRes)
        else -> stringResource(R.string.select_recipient_sender_country)
    }

    BackHandler(enabled = uiState.isStampPickerOpen || uiState.isStickerPickerOpen) {
        viewModel.onStampPickerOpenChange(false)
        viewModel.onStickerPickerOpenChange(false)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.loadCollectibles(forceRefresh = true)
        onPauseOrDispose {}
    }

    // StampPickerOverlay/StickerPickerOverlay both bleed edge-to-edge behind
    // the status bar with a white background — status bar icons need to flip
    // to dark to stay legible there, and back to light against this screen's
    // own dark wood background once both are closed.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            uiState.isStampPickerOpen || uiState.isStickerPickerOpen
    }

    val coroutineScope = rememberCoroutineScope()
    val envelopeGraphicsLayer = rememberGraphicsLayer()
    val isCompositingSupported = LocalDensity.current.density >= MinCompositingDensity
    var isCapturingEnvelope by remember { mutableStateOf(false) }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        var stampAnimatedVisibilityScope by remember {
            mutableStateOf<AnimatedVisibilityScope?>(null)
        }
        var stickerAnimatedVisibilityScope by remember {
            mutableStateOf<AnimatedVisibilityScope?>(null)
        }

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SelectRecipientHeader(
                    hasRecipient = uiState.hasRecipient,
                    isReadyToSend = uiState.isReadyToSend,
                    canSend = uiState.canSend,
                    onBackClick = onBackClick,
                    onNextClick = { viewModel.onStampPickerOpenChange(true) },
                    onSendClick = {
                        coroutineScope.launch {
                            val envelopeBytes = if (isCompositingSupported) {
                                isCapturingEnvelope = true
                                val bytes = try {
                                    withFrameNanos {}
                                    withFrameNanos {}
                                    envelopeGraphicsLayer.toCompositeBytes()
                                } finally {
                                    isCapturingEnvelope = false
                                }
                                bytes
                            } else {
                                null
                            }
                            viewModel.onSendClick(envelopeBytes)
                        }
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    EnvelopeCard(
                        uiState = uiState,
                        senderName = senderName,
                        senderCountry = senderCountry,
                        onShareContactClick = viewModel::onShareContactClick,
                        onPenpalsClick = viewModel::onPenpalsClick,
                        onEditRecipientClick = viewModel::onEditRecipientClick,
                        onStampClick = { viewModel.onStampPickerOpenChange(true) },
                        onStickerClick = { viewModel.onStickerToggleRequest() },
                        onEnvelopeClick = { viewModel.onEnvelopePickerOpenChange(true) },
                        showStickerPlaceholder = !isCapturingEnvelope,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        stampAnimatedVisibilityScope = stampAnimatedVisibilityScope,
                        stickerAnimatedVisibilityScope = stickerAnimatedVisibilityScope,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .let {
                                if (isCompositingSupported) {
                                    it.captureIntoGraphicsLayer(envelopeGraphicsLayer)
                                } else {
                                    it
                                }
                            },
                    )
                }

                EnvelopeEditToolbar(
                    isStampSelected = uiState.selectedStamp != null,
                    onTextClick = { viewModel.onFontPickerOpenChange(true) },
                    onEnvelopeClick = { viewModel.onEnvelopePickerOpenChange(true) },
                    onStickerClick = { viewModel.onStickerToggleRequest() },
                    onStampClick = { viewModel.onStampPickerOpenChange(true) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                Spacer(Modifier.height(8.dp))
            }

            AnimatedVisibility(
                visible = uiState.isStampPickerOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                SideEffect { stampAnimatedVisibilityScope = this@AnimatedVisibility }
                with(this@SharedTransitionLayout) {
                    StampPickerOverlay(
                        stamps = stampCatalog,
                        selectedStamp = uiState.selectedStamp,
                        onStampSelect = viewModel::onStampSelect,
                        onDismiss = { viewModel.onStampPickerOpenChange(false) },
                        onNavigateToStore = { source ->
                            viewModel.onFindMoreCollectiblesClick("stamp", source)
                            onNavigateToStore?.invoke()
                        },
                        animatedVisibilityScope = this@AnimatedVisibility,
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isStickerPickerOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                SideEffect { stickerAnimatedVisibilityScope = this@AnimatedVisibility }
                with(this@SharedTransitionLayout) {
                    StickerPickerOverlay(
                        stickers = stickerCatalog,
                        selectedSticker = uiState.selectedSticker,
                        onStickerSelect = viewModel::onStickerSelect,
                        onDismiss = { viewModel.onStickerPickerOpenChange(false) },
                        onNavigateToStore = { source ->
                            viewModel.onFindMoreCollectiblesClick("sticker", source)
                            onNavigateToStore?.invoke()
                        },
                        animatedVisibilityScope = this@AnimatedVisibility,
                    )
                }
            }

            LetterlyTopSnackbar(
                message = uiState.sendError,
                style = SnackbarStyle.Error,
                onDismiss = viewModel::dismissError,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (uiState.isFontPickerOpen) {
        FontPickerSheet(
            selectedFont = uiState.selectedFont,
            onFontSelect = viewModel::onFontSelect,
            onDismiss = { viewModel.onFontPickerOpenChange(false) },
        )
    }

    if (uiState.isRecipientPickerOpen) {
        RegionPickerSheet(
            selectedRegion = uiState.selectedRegion,
            selectedContactId = uiState.recipientId,
            onRegionSelect = viewModel::onRegionSelect,
            onContactSelect = viewModel::onContactSelect,
            onDismiss = { viewModel.onRecipientPickerOpenChange(false) },
            liveRegionCounts = regionCounts,
            addressBookContacts = addressBookContacts,
            isAddressBookLoading = isAddressBookLoading,
            initialTab = uiState.initialPickerTab,
        )
    }
    if (uiState.isEnvelopePickerOpen) {
        EnvelopePickerSheet(
            envelopes = envelopeCatalog,
            selectedEnvelope = uiState.selectedEnvelope,
            onEnvelopeSelect = viewModel::onEnvelopeSelect,
            onDismiss = { viewModel.onEnvelopePickerOpenChange(false) },
            onNavigateToStore = { source ->
                viewModel.onFindMoreCollectiblesClick("envelope", source)
                onNavigateToStore?.invoke()
            },
        )
    }
    uiState.comingSoonFeature?.let { feature ->
        ComingSoonBottomSheet(
            title = stringResource(feature.titleRes()),
            onDismiss = viewModel::onComingSoonDismiss,
        )
    }

    // Auto-prompted right when the user picks "send to a new penpal via
    // region" (see SelectRecipientViewModel.onRegionSelect) instead of only
    // living behind the compose screen's hidden overflow-menu toggle — same
    // dialog/copy either way, just surfaced proactively here.
    if (uiState.showPublicShowcaseDialog) {
        LetterlyCenterDialog(
            title = stringResource(R.string.compose_public_showcase_dialog_title),
            body = stringResource(R.string.compose_public_showcase_dialog_body),
            primaryCtaText = stringResource(R.string.compose_public_showcase_dialog_confirm),
            onPrimaryClick = viewModel::onPublicShowcaseConfirm,
            secondaryCtaText = stringResource(R.string.compose_public_showcase_dialog_cancel),
            onSecondaryClick = viewModel::onPublicShowcaseDismiss,
            onDismissRequest = viewModel::onPublicShowcaseDismiss,
        )
    }

    if (uiState.isSending) {
        val estimatedArrivalAt = uiState.sentEstimatedArrivalAt
        val recipientContinent = uiState.selectedRegion?.apiLabel ?: uiState.recipientContinent
        val letterId = uiState.sentLetterId.orEmpty()
        SendSuccessOverlay(
            envelope = uiState.selectedEnvelope,
            onFinished = {
                if (estimatedArrivalAt != null && recipientContinent != null) {
                    onSendSuccess(
                        estimatedArrivalAt,
                        recipientContinent,
                        uiState.recipientName.orEmpty(),
                        letterId,
                        uiState.sentVisibility,
                        uiState.sentCompositeImageUrl,
                    )
                }
            },
        )
    }
}

private fun ComingSoonFeature.titleRes(): Int = when (this) {
    ComingSoonFeature.TEXT -> R.string.envelope_edit_text_desc
    ComingSoonFeature.STICKER -> R.string.envelope_edit_sticker_desc
    ComingSoonFeature.SHARE_CONTACT -> R.string.add_recipient_share_contact
}
