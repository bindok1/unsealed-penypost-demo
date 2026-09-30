package com.apps.unsealed.ui.screens.publicprofile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.model.getPenpalLanguageByCode
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.auth.AddressBookRequestState
import com.apps.unsealed.feature.auth.BlockUserState
import com.apps.unsealed.feature.auth.PublicProfileUiState
import com.apps.unsealed.feature.auth.PublicProfileViewModel
import com.apps.unsealed.feature.auth.ReportUserState
import com.apps.unsealed.feature.auth.data.InterestDto
import com.apps.unsealed.feature.auth.data.PublicProfileDto
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.ReportMailDialog
import com.apps.unsealed.ui.components.ReportReason
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionUiState
import com.apps.unsealed.ui.screens.profile.widgets.LanguageProficiencyCard
import com.apps.unsealed.ui.screens.profile.widgets.PostalCollectionFullView
import com.apps.unsealed.ui.screens.profile.widgets.PostalCollectionShowcase
import com.apps.unsealed.ui.screens.profile.widgets.PostalTabPill
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── Entry point ──────────────────────────────────────────────────────────────

/**
 * Bottom sheet showing the public profile for [userId].
 *
 * Triggers a network fetch on first composition and each time [userId] changes.
 * On 404 (user not found **or** block exists) the sheet displays a neutral
 * "profile unavailable" message — same UX as a true 404, by design.
 *
 * @param userId   The target user's ID (`uid_...`). Must be stable across recompositions.
 * @param onDismiss Callback to hide the sheet and clear [userId] in the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileBottomSheet(
    userId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    viewModel: PublicProfileViewModel = hiltViewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }

    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandCardDark,
        modifier = modifier,
    ) {
        when (val state = uiState) {
            is PublicProfileUiState.Loading -> LoadingContent()
            is PublicProfileUiState.NotFound -> NotFoundContent()
            is PublicProfileUiState.Error   -> ErrorContent(state.message)
            is PublicProfileUiState.Success -> ProfileContent(
                profile = state.profile,
                interests = state.interests,
                viewModel = viewModel,
                onDismiss = onDismiss,
            )
        }
    }
}

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandGold)
    }
}

@Composable
private fun NotFoundContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.public_profile_not_found_title),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.public_profile_not_found_body),
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            fontFamily = NunitoFontFamily,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

// ─── Success content ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    profile: PublicProfileDto,
    interests: List<InterestDto>,
    viewModel: PublicProfileViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showAddressBookConfirmDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var selectedStationeryTab by remember { mutableStateOf(0) } // 0: Envelopes, 1: Paper — same as UnsealedProfileContent
    var fullViewIndex by remember { mutableStateOf<Int?>(null) }
    var isFullViewOpen by remember { mutableStateOf(false) }

    val addressBookState by viewModel.addressBookState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val blockState by viewModel.blockState.collectAsState()
    val postalCollection by viewModel.postalCollection.collectAsState()

    // ── Block ───────────────────────────────────────────────────────────────
    LaunchedEffect(blockState) {
        when (val state = blockState) {
            is BlockUserState.Blocked -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.block_user_blocked_toast),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.resetBlockState()
                // A block relationship makes this profile a 404 on next fetch anyway
                // (see PublicProfileUiState.NotFound's doc comment) — closing here
                // instead of waiting for a re-fetch to flash that state.
                onDismiss()
            }
            is BlockUserState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetBlockState()
            }
            else -> Unit
        }
    }

    if (showBlockConfirmDialog) {
        LetterlyCenterDialog(
            title = stringResource(R.string.block_user_confirm_dialog_title, profile.nickname),
            body = stringResource(R.string.block_user_confirm_dialog_body),
            primaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_confirm),
            onPrimaryClick = { showBlockConfirmDialog = false; viewModel.blockUser(profile.id) },
            secondaryCtaText = stringResource(R.string.block_user_confirm_dialog_cta_cancel),
            onSecondaryClick = { showBlockConfirmDialog = false },
            onDismissRequest = { showBlockConfirmDialog = false },
        )
    }

    // ── Report ─────────────────────────────────────────────────────────────
    LaunchedEffect(reportState) {
        when (val state = reportState) {
            is ReportUserState.Submitted -> {
                showReportDialog = false
                Toast.makeText(
                    context,
                    context.getString(R.string.public_profile_report_submitted_toast),
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.resetReportState()
            }
            is ReportUserState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetReportState()
            }
            else -> Unit
        }
    }

    if (showReportDialog) {
        ReportMailDialog(
            reasons = listOf(
                ReportReason.HARASSMENT,
                ReportReason.SPAM,
                ReportReason.INAPPROPRIATE_CONTENT,
                ReportReason.UNDERAGE,
                ReportReason.IMPERSONATION,
                ReportReason.OTHER,
            ),
            isSubmitting = reportState is ReportUserState.Submitting,
            onSubmit = { reason, note -> viewModel.reportUser(profile.id, reason.apiValue, note) },
            onDismissRequest = { showReportDialog = false },
            titleRes = R.string.report_user_dialog_title,
            bodyRes = R.string.report_user_dialog_body,
        )
    }

    // ── Add to address book ──────────────────────────────────────────────────
    LaunchedEffect(addressBookState) {
        if (addressBookState is AddressBookRequestState.Error) {
            Toast.makeText(
                context,
                (addressBookState as AddressBookRequestState.Error).message,
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.resetAddressBookState()
        }
    }

    if (showAddressBookConfirmDialog) {
        LetterlyCenterDialog(
            title = stringResource(R.string.public_profile_add_to_address_book_dialog_title, profile.nickname),
            body = stringResource(R.string.public_profile_add_to_address_book_dialog_body),
            primaryCtaText = stringResource(R.string.public_profile_add_to_address_book_cta_send),
            onPrimaryClick = {
                showAddressBookConfirmDialog = false
                viewModel.addToAddressBook(profile.id)
            },
            secondaryCtaText = stringResource(R.string.public_profile_add_to_address_book_cta_cancel),
            onSecondaryClick = { showAddressBookConfirmDialog = false },
            onDismissRequest = { showAddressBookConfirmDialog = false },
        )
    }

    if (addressBookState is AddressBookRequestState.Sent) {
        LetterlyCenterDialog(
            title = stringResource(R.string.public_profile_address_book_sent_dialog_title),
            body = stringResource(R.string.public_profile_address_book_sent_dialog_body),
            primaryCtaText = stringResource(R.string.public_profile_address_book_sent_dialog_cta),
            onPrimaryClick = { viewModel.resetAddressBookState() },
            onDismissRequest = { viewModel.resetAddressBookState() },
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Hero card ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E2620), BrandCardDark)
                        )
                    )
                    .border(1.5.dp, BrandCardStroke, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                ProfileMoreMenuButton(
                    expanded = moreMenuExpanded,
                    onExpandedChange = { moreMenuExpanded = it },
                    onReportClick = { showReportDialog = true },
                    onAddToAddressBookClick = { showAddressBookConfirmDialog = true },
                    onBlockClick = { showBlockConfirmDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, BrandGold, CircleShape)
                                .background(BrandInkDeep),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!profile.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile.photoUrl,
                                    contentDescription = profile.nickname,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Name — used to carry a "Verified"/premium badge
                            // here, but the backend dropped the subscription
                            // concept in favor of the Energy currency and no
                            // longer sends is_premium at all (see AuthDto.kt).
                            Text(
                                text = profile.nickname,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            // Continent
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = BrandGoldDim,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = profile.continent,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 13.sp,
                                    color = BrandGoldDim,
                                )
                            }
                        }
                    }

                    // ── Online status bar ────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (profile.isOnline) Color(0xFF4CAF50)
                                        else Color.White.copy(alpha = 0.3f)
                                    ),
                            )
                            Text(
                                text = if (profile.isOnline) {
                                    stringResource(R.string.public_profile_online_now)
                                } else {
                                    profile.lastActiveAt?.let { formatRelativeTime(it) }
                                        ?: stringResource(R.string.public_profile_last_seen_unknown)
                                },
                                fontFamily = NunitoFontFamily,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                        }
                        Text(
                            text = stringResource(R.string.public_profile_member_label),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGold,
                        )
                    }
                }
            }

            // ── Bio ─────────────────────────────────────────────────────────────
            if (!profile.bio.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandInkDeep.copy(alpha = 0.4f))
                        .border(1.dp, BrandCardStroke, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Filled.FormatQuote,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.public_profile_about_section_label),
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrandGoldDim,
                                letterSpacing = 0.8.sp,
                            )
                        }
                        Text(
                            text = profile.bio,
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 21.sp,
                        )
                    }
                }
            }

            // ── Interests ────────────────────────────────────────────────────────
            if (interests.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.public_profile_interests_section_label),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BrandGoldDim,
                        letterSpacing = 0.8.sp,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        interests.forEach { interest ->
                            PublicInterestChip(interest = interest)
                        }
                    }
                }
            }

            // ── Languages ───────────────────────────────────────────────────────
            val languages = profile.languages.orEmpty()
            if (languages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = null,
                            tint = BrandGoldDim,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.profile_content_languages_label),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGoldDim,
                            letterSpacing = 1.sp,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        languages.forEach { language ->
                            // Same free-form-code fallback as UnsealedProfileContent.
                            val lang = getPenpalLanguageByCode(language.code)
                            val name = lang?.nativeName ?: language.code
                            val flag = lang?.flag ?: "🌐"
                            LanguageProficiencyCard(name = name, level = language.level, flag = flag)
                        }
                    }
                }
            }

            // ── Postal Collection Showcase (Tabs: Envelope / Paper) ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandInkDeep.copy(alpha = 0.4f))
                    .border(1.dp, BrandCardStroke, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_content_postal_collection_label),
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BrandGoldDim,
                            letterSpacing = 1.sp,
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(2.dp),
                        ) {
                            PostalTabPill(
                                text = stringResource(R.string.profile_content_tab_envelopes),
                                isSelected = selectedStationeryTab == 0,
                                onClick = { selectedStationeryTab = 0 },
                            )
                            PostalTabPill(
                                text = stringResource(R.string.profile_content_tab_paper),
                                isSelected = selectedStationeryTab == 1,
                                onClick = { selectedStationeryTab = 1 },
                            )
                        }
                    }

                    // Read-only — no pause/delete menu, this isn't the owner's
                    // own collection (see PostalCollectionShowcase's readOnly doc).
                    PostalCollectionShowcase(
                        state = postalCollection,
                        showEnvelopeFace = selectedStationeryTab == 0,
                        onItemAction = { _, _ -> },
                        onItemClick = { index -> fullViewIndex = index; isFullViewOpen = true },
                        readOnly = true,
                    )
                }
            }
        }

        fullViewIndex?.let { index ->
            val showcaseItems = (postalCollection as? PostalCollectionUiState.Success)?.items.orEmpty()
            if (showcaseItems.isNotEmpty()) {
                BackHandler(enabled = isFullViewOpen) { isFullViewOpen = false }
                PostalCollectionFullView(
                    items = showcaseItems,
                    initialIndex = index.coerceIn(showcaseItems.indices),
                    isOpen = isFullViewOpen,
                    onClose = { isFullViewOpen = false },
                    onFullyClosed = { fullViewIndex = null },
                )
            }
        }
    }
}

// ─── Components ───────────────────────────────────────────────────────────────

private val MoreButtonSize = 36.dp
private val MoreDropdownGap = 6.dp

/**
 * "···" overflow trigger (Report / Add to Address Book) for the hero card.
 * Same circular-border-button + [AnchoredDropdownMenu] recipe as
 * `OpenLetterOverlay.kt`'s `SenderInfoBar` "···" menu, so it reads as the
 * same control across the app.
 */
@Composable
private fun ProfileMoreMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onReportClick: () -> Unit,
    onAddToAddressBookClick: () -> Unit,
    onBlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val moreSource = remember { MutableInteractionSource() }
    val morePressScale = rememberPressScale(moreSource)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = morePressScale; scaleY = morePressScale }
                .size(MoreButtonSize)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(
                    interactionSource = moreSource,
                    indication = null,
                    onClick = { onExpandedChange(true) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.public_profile_more_desc),
                tint = Color.White,
            )
        }

        AnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            alignment = Alignment.TopEnd,
            offset = with(density) { IntOffset(0, (MoreButtonSize + MoreDropdownGap).roundToPx()) },
            transformOrigin = TransformOrigin(1f, 0f),
        ) {
            DropdownMenuAction(
                icon = Icons.Filled.PersonAdd,
                label = stringResource(R.string.public_profile_menu_add_to_address_book),
                onClick = { onExpandedChange(false); onAddToAddressBookClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.Flag,
                label = stringResource(R.string.public_profile_menu_report),
                onClick = { onExpandedChange(false); onReportClick() },
            )
            DropdownMenuAction(
                icon = Icons.Filled.Block,
                label = stringResource(R.string.public_profile_menu_block),
                onClick = { onExpandedChange(false); onBlockClick() },
            )
        }
    }
}

@Composable
private fun PublicInterestChip(interest: InterestDto) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandInkDeep.copy(alpha = 0.5f))
            .border(1.dp, BrandCardStroke, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = interest.emoji, fontSize = 14.sp)
        Text(
            text = interest.name,
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Formats a raw RFC 3339 [timestamp] into a human-readable relative time
 * string (e.g. "3 min ago", "2 h ago", "Yesterday", "5 days ago").
 *
 * This is intentionally client-side rendering — the server sends the raw
 * timestamp and the client decides how to display it, the same contract
 * used by `GET /auth/me` and `GET /penpals/feed`.
 */
private fun formatRelativeTime(timestamp: String): String = runCatching {
    val then = Instant.parse(timestamp)
    val now = Instant.now()
    val minutesAgo = ChronoUnit.MINUTES.between(then, now)
    when {
        minutesAgo < 1    -> "Just now"
        minutesAgo < 60   -> "$minutesAgo min ago"
        minutesAgo < 1440 -> "${minutesAgo / 60} h ago"
        minutesAgo < 2880 -> "Yesterday"
        else              -> "${minutesAgo / 1440} days ago"
    }
}.getOrDefault("Recently active")
