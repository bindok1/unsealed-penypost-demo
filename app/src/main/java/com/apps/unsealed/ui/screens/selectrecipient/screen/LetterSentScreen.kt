package com.apps.unsealed.ui.screens.selectrecipient.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.components.LetterlyOutlinedButton
import com.apps.unsealed.ui.screens.compose.widgets.DeviceCapabilityBottomSheet
import com.apps.unsealed.ui.screens.inbox.state.formatExpectedDelivery
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.penpals.widgets.ShareLetterSheet
import com.apps.unsealed.core.util.saveImageToGallery
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeDesign
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.screens.tracking.state.transportModeFor
import com.apps.unsealed.ui.screens.tracking.widgets.DeliveryRouteMap
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BrandInkMid
import kotlinx.coroutines.launch

/**
 * Full-screen confirmation shown right after [SendSuccessOverlay] finishes,
 * for every send path (reply and fresh). [estimatedArrivalAt] is the raw
 * `estimated_arrival_at` from `SendLetterResponse`, formatted the same way
 * Inbox already does — real backend value (same-continent ~6h, cross-continent
 * ~24h), not a hardcoded "1 day". [recipientContinent] is the raw continent
 * string threaded through from the send flow (see `UnsealedNavHost`'s
 * `LetterSentRecipientContinentArg`) — combined with the viewer's own
 * continent (from [AuthViewModel]) to pick a [transportModeFor] and draw the
 * [DeliveryRouteMap] at its just-sent (0%) state, with its entrance animation
 * playing once.
 *
 * Owns its own full-screen background (dark warm gradient, see
 * [MainActivity]'s `isLetterSentRoute` skip) instead of the shared
 * `bg_texture`.
 *
 * Two bottom CTAs, which pair depends on [isPublic] (`visibility` sent on
 * `POST /letters` — see `docs/be/penpals_api.md`):
 * - **Private** (default): [onContinueClick] (primary, filled) always resets
 *   the app back to the PenPal feed; [onWriteNewLetterClick] (secondary,
 *   outlined) goes straight to a fresh Compose screen. This is the original
 *   "point of no return" pair — nothing to go back to for a 1:1 letter.
 * - **Public**: the letter is also showcased on the sender's own profile
 *   (`ShowcaseStatus`, `docs/be/letters_api.md`) and browsable by anyone in
 *   `GET /penpals/feed` — a private-style delivery-confirmation pair reads
 *   like a room-chat DM here, so instead: [onViewProfileClick] (primary) to
 *   see it in the Postal Collection grid, and Share (secondary) via
 *   [ShareLetterSheet] using [compositeImageUrl]. If [compositeImageUrl] is
 *   null (device skipped compositing), the Share button stays visible but
 *   opens [DeviceCapabilityBottomSheet] instead of the real share flow —
 *   same explanation already shown at compose time for that case.
 *
 * Hardware back always mirrors [onContinueClick] regardless of [isPublic] —
 * still a "point of no return" screen either way.
 */
@Composable
fun LetterSentScreen(
    estimatedArrivalAt: String,
    recipientContinent: String,
    onContinueClick: () -> Unit,
    onWriteNewLetterClick: () -> Unit,
    modifier: Modifier = Modifier,
    recipientName: String = "",
    letterId: String = "",
    onMapClick: (() -> Unit)? = null,
    isPublic: Boolean = false,
    compositeImageUrl: String? = null,
    onViewProfileClick: () -> Unit = {},
) {
    BackHandler(onBack = onContinueClick)

    var isShareSheetVisible by remember { mutableStateOf(false) }
    var isShareUnavailableVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.share_letter_saved_toast)
    val saveFailedMessage = stringResource(R.string.share_letter_save_failed_toast)

    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    val senderContinentRaw = when (val state = authUiState) {
        is AuthUiState.Authenticated -> state.user.continent
        is AuthUiState.NeedsOnboarding -> state.user.continent
        else -> null
    }
    val senderName = when (val state = authUiState) {
        is AuthUiState.Authenticated -> state.user.nickname
        is AuthUiState.NeedsOnboarding -> state.user.nickname
        else -> null
    }
    val senderRegion = senderContinentRaw?.let(PenpalRegion::fromApiString) ?: PenpalRegion.ASIA
    val recipientRegion = PenpalRegion.fromApiString(recipientContinent)
    val transportMode = transportModeFor(senderRegion, recipientRegion)

    // "Just sent a letter" is the app's clearest moment of delight — the
    // best (and only) spot Google's in-app review bottom sheet gets fired
    // from. See AuthViewModel.maybeRequestInAppReview / InAppReviewManager
    // for why this is safe to call on every send (reply or fresh) instead of
    // gating it to "first letter only" here.
    LaunchedEffect(Unit) {
        (context as? Activity)?.let { activity -> authViewModel.maybeRequestInAppReview(activity) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to BrandInkMid,
                    1f to BrandInkDeep,
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))

            DeliveryRouteMap(
                senderContinent = senderRegion,
                recipientContinent = recipientRegion,
                progress = 0f,
                transportMode = transportMode,
                animateEntrance = true,
                senderName = senderName,
                recipientName = recipientName,
                letterId = letterId,
                onMapClick = onMapClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.letter_sent_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.letter_sent_body),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.open_letter_expected_delivery, formatExpectedDelivery(estimatedArrivalAt)),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.55f),
            )

            Spacer(Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            if (isPublic) {
                LetterlyButton(
                    text = stringResource(R.string.letter_sent_view_profile_cta),
                    onClick = onViewProfileClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LetterlyOutlinedButton(
                    text = stringResource(R.string.letter_sent_share_cta),
                    onClick = {
                        if (compositeImageUrl != null) isShareSheetVisible = true
                        else isShareUnavailableVisible = true
                    },
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LetterlyButton(
                    text = stringResource(R.string.letter_sent_cta),
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LetterlyOutlinedButton(
                    text = stringResource(R.string.letter_sent_write_new_cta),
                    onClick = onWriteNewLetterClick,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (isShareSheetVisible && compositeImageUrl != null) {
        ShareLetterSheet(
            // Synthetic — this screen never fetched/rendered a real feed
            // item, only the fields ShareLetterSheet itself reads
            // (compositeImageUrl/id) are meaningful; the rest are unused
            // placeholders. bodyText is empty (never threaded through nav
            // args — letters can be up to 2000 chars, far too long for a
            // route segment), so the share caption loses its excerpt.
            letter = PenpalLetter(
                id = letterId,
                senderId = "",
                senderName = senderName.orEmpty(),
                region = recipientRegion,
                envelope = EnvelopeDesign.ENVELOPE_1,
                envelopeImageUrl = "",
                compositeImageUrl = compositeImageUrl,
                bodyText = "",
                postedAt = "",
                likeCount = 0,
                viewerHasLiked = false,
                isOnline = false,
            ),
            graphicsLayer = null,
            onDismiss = { isShareSheetVisible = false },
            onSaveImageRequested = { bytes ->
                scope.launch {
                    val uri = saveImageToGallery(context, bytes, "letter_$letterId.webp")
                    Toast.makeText(context, if (uri != null) savedMessage else saveFailedMessage, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    if (isShareUnavailableVisible) {
        DeviceCapabilityBottomSheet(onDismiss = { isShareUnavailableVisible = false })
    }
}
