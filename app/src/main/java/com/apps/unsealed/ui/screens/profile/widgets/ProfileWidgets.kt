package com.apps.unsealed.ui.screens.profile.widgets

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Markunread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.core.model.getPenpalLanguageByCode
import com.apps.unsealed.core.util.calculateAge
import com.apps.unsealed.core.util.calculateZodiac
import com.apps.unsealed.core.util.formatGender
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.auth.data.InterestDto
import com.apps.unsealed.feature.auth.data.UserDto
import com.apps.unsealed.feature.letters.data.ShowcaseStatus
import com.apps.unsealed.ui.components.AnchoredDropdownMenu
import com.apps.unsealed.ui.components.DropdownMenuAction
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.screens.compose.widgets.DeviceCapabilityBottomSheet
import com.apps.unsealed.ui.screens.inbox.widgets.LetterPaperAspectRatio
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionItem
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionUiState
import com.apps.unsealed.ui.screens.selectrecipient.constants.EnvelopeAspectRatio
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.InkDefault
import com.apps.unsealed.ui.theme.NunitoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnsealedProfileContent(
    user: UserDto,
    interests: List<InterestDto>,
    postalCollection: PostalCollectionUiState,
    onDraftClick: () -> Unit,
    onAddressBookClick: () -> Unit,
    onStampBookClick: () -> Unit = {},
    onInviteFriendsClick: () -> Unit = {},
    onTipsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShowcaseItemAction: (letterId: String, status: ShowcaseStatus) -> Unit,
    onShowcaseItemClick: (index: Int) -> Unit,
    isUploadingPhoto: Boolean = false,
    onAvatarClick: () -> Unit = {},
) {
    val nickname = user.nickname
    val continent = user.continent
    val age = calculateAge(user.birthday)
    val gender = formatGender(user.gender)
    val zodiac = calculateZodiac(user.birthday)
    val bio = user.bio
    val photoUrl = user.photoUrl

    var selectedStationeryTab by remember { mutableStateOf(0) } // 0: Stamp Album, 1: Stationery

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Status Bar Spacer & Top Bar ────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Markunread,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.profile_content_pass_title),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandCardDark)
                        .border(1.dp, BrandCardStroke, CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.profile_content_settings_desc),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── Main Passport / Profile Card ───────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrandCardDark)
                    .border(1.dp, BrandCardStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header: Photo + Basic Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Avatar Photo with (+) Edit/Update Badge
                        val avatarInteractionSource = remember { MutableInteractionSource() }
                        val avatarPressScale = rememberPressScale(avatarInteractionSource)

                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .graphicsLayer {
                                    scaleX = avatarPressScale
                                    scaleY = avatarPressScale
                                }
                                .clickable(
                                    interactionSource = avatarInteractionSource,
                                    indication = null,
                                    enabled = !isUploadingPhoto,
                                    onClick = onAvatarClick,
                                ),
                        ) {
                            // Main avatar circle
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(2.dp, BrandGold.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = nickname,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = BrandGold,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }

                                if (isUploadingPhoto) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = BrandGold,
                                            strokeWidth = 2.5.dp,
                                        )
                                    }
                                }
                            }

                            // Small (+) Add/Update Photo Badge on Bottom-Right
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(BrandGold)
                                    .border(2.dp, BrandCardDark, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.profile_update_photo_desc),
                                    tint = BrandInkDeep,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }

                        // Name & Details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = nickname.ifBlank { stringResource(R.string.profile_content_default_nickname) },
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = stringResource(R.string.profile_content_verified_desc),
                                    tint = BrandGold,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            if (!continent.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = BrandGoldDim,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = continent,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 12.sp,
                                        color = BrandGoldDim,
                                    )
                                }
                            }

                            // Meta Pills: Age, Gender, Zodiac
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                if (age != null) {
                                    Text(
                                        text = stringResource(R.string.profile_content_age_years, age),
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                                if (gender != null) {
                                    Text(
                                        text = gender,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                                if (zodiac != null) {
                                    Text(
                                        text = zodiac,
                                        fontFamily = NunitoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Bio Section
                    if (!bio.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatQuote,
                                    contentDescription = null,
                                    tint = BrandGold.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = bio,
                                    fontFamily = NunitoFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Quick Stats Grid ───────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickStatCard(
                    emoji = "📝",
                    title = stringResource(R.string.profile_content_stat_drafts_title),
                    subtitle = stringResource(R.string.profile_content_stat_drafts_subtitle),
                    onClick = onDraftClick,
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    emoji = "📬",
                    title = stringResource(R.string.profile_menu_address_book),
                    subtitle = stringResource(R.string.profile_content_stat_address_book_subtitle),
                    onClick = onAddressBookClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Snail Mail Keepsake Album Card ─────────────────────────────────
        item {
            KeepsakeAlbumEntryCard(
                onClick = onStampBookClick,
            )
        }

        // ── Interests Section ──────────────────────────────────────────────
        if (interests.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.profile_content_interests_label),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BrandGoldDim,
                        letterSpacing = 1.sp,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        interests.forEach { interest ->
                            InterestChip(interest = interest)
                        }
                    }
                }
            }
        }

        // ── Languages Section ──────────────────────────────────────────────
        if (user.languages.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "🌐", fontSize = 13.sp)
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
                        user.languages.forEach { language ->
                            val lang = getPenpalLanguageByCode(language.code)
                            val name = lang?.nativeName ?: language.code
                            val flag = lang?.flag ?: "🌐"
                            LanguageProficiencyCard(name = name, level = language.level, flag = flag)
                        }
                    }
                }
            }
        }

        // ── Postal Collection Showcase (Tabs: Envelope / Paper) ────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandCardDark)
                    .border(1.dp, BrandCardStroke, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header row with tab pills
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

                        // Tab switcher
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

                    // Real letters/envelopes the owner opted public (compose
                    // screen's "Show in Postal Collection" toggle) — not a
                    // static design catalog.
                    PostalCollectionShowcase(
                        state = postalCollection,
                        showEnvelopeFace = selectedStationeryTab == 0,
                        onItemAction = onShowcaseItemAction,
                        onItemClick = onShowcaseItemClick,
                    )
                }
            }
        }

        // ── Navigation & Help Cards ────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PostalNavigationCard(
                    emoji = "👥",
                    title = stringResource(R.string.profile_content_invite_title),
                    subtitle = stringResource(R.string.profile_content_invite_subtitle),
                    onClick = onInviteFriendsClick,
                )
                PostalNavigationCard(
                    emoji = "💡",
                    title = stringResource(R.string.profile_content_tips_title),
                    subtitle = stringResource(R.string.profile_content_tips_subtitle),
                    onClick = onTipsClick,
                )
            }
        }

        // ── Bottom Spacers ────────────────────────────────────────────────
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(20.dp))
        }
    }
}

// Helper extension to handle optional CharSequence blank check
private fun String?.isNull信Blank(): Boolean = this.isNullOrBlank()

@Composable
fun QuickStatCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandCardDark,
                        Color(0xFF332519),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(BrandGold.copy(alpha = 0.35f), BrandCardStroke)
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // Skeuomorphic "wax disc" behind emoji
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                BrandGold.copy(alpha = 0.22f),
                                BrandGoldDeep.copy(alpha = 0.08f),
                            )
                        )
                    )
                    .border(1.dp, BrandGold.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
            Text(
                text = title,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontFamily = NunitoFontFamily,
                fontSize = 10.sp,
                color = BrandGoldDim,
                maxLines = 1,
            )
        }
    }
}

/**
 * Prominent entry card in Profile for navigating to the Snail Mail Keepsake Album ("Buku Kenangan").
 * Skeuomorphic art style — looks like a real postal keepsake album, warm parchment feel.
 */
@Composable
fun KeepsakeAlbumEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF332519),
                        BrandCardDark,
                        Color(0xFF2C1E15),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        BrandGold.copy(alpha = 0.45f),
                        BrandCardStroke,
                        BrandGold.copy(alpha = 0.2f),
                    )
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Stamp icon — fixed width, no shrinking
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                BrandGold.copy(alpha = 0.30f),
                                Color(0xFFD4AF37).copy(alpha = 0.08f),
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(BrandGold.copy(alpha = 0.55f), BrandGoldDeep.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "📚", fontSize = 26.sp)
            }

            // Text section — takes remaining space between icon and arrow
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_keepsake_title),
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                Text(
                    text = stringResource(R.string.profile_keepsake_subtitle),
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.60f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Arrow — fixed size, never expands
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BrandGold.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .border(1.dp, BrandGold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "›", fontSize = 20.sp, color = BrandGold, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
fun InterestChip(interest: InterestDto) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF332519),
                        BrandCardDark,
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(BrandGold.copy(alpha = 0.3f), BrandCardStroke)
                ),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = interest.emoji, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = interest.name,
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
fun LanguageProficiencyCard(name: String, level: Int, flag: String = "🌐") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF332519), BrandCardDark)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(BrandGold.copy(alpha = 0.3f), BrandCardStroke)
                ),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = flag,
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = name,
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.width(10.dp))
        // Wax-dot proficiency bar
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= level)
                                Brush.radialGradient(listOf(BrandGold, BrandGoldDeep))
                            else
                                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent))
                        ),
                )
            }
        }
    }
}

/** [readOnly] hides each card's "···" pause/delete menu — for someone
 * else's collection ([com.apps.unsealed.ui.screens.publicprofile.PublicProfileBottomSheet],
 * which has no [onItemAction] of its own to wire up management actions to). */
@Composable
fun PostalCollectionShowcase(
    state: PostalCollectionUiState,
    showEnvelopeFace: Boolean,
    onItemAction: (letterId: String, status: ShowcaseStatus) -> Unit,
    onItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    when (state) {
        is PostalCollectionUiState.Loading -> Box(
            modifier = modifier.fillMaxWidth().height(70.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = BrandGold,
                strokeWidth = 2.dp,
            )
        }
        is PostalCollectionUiState.Error -> Text(
            text = state.message,
            fontFamily = NunitoFontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = modifier,
        )
        is PostalCollectionUiState.Success -> if (state.items.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_content_postal_collection_empty),
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = modifier,
            )
        } else {
            LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.items, key = { _, item -> item.letterId }) { index, item ->
                    PostalCollectionCard(
                        item = item,
                        showEnvelopeFace = showEnvelopeFace,
                        onClick = { onItemClick(index) },
                        onAction = { status -> onItemAction(item.letterId, status) },
                        readOnly = readOnly,
                    )
                }
            }
        }
    }
}

/** One card — [showEnvelopeFace] switches between the envelope face
 * ([PostalCollectionItem.envelopeCompositeImageUrl], landscape) and the
 * paper/letter face ([PostalCollectionItem.compositeImageUrl], portrait) of
 * the *same* underlying letter, matching [PostalCollectionShowcase]'s tab —
 * same envelope/paper flatten pattern as `MailboxFullLetterViewer.kt`. */
@Composable
private fun PostalCollectionCard(
    item: PostalCollectionItem,
    showEnvelopeFace: Boolean,
    onClick: () -> Unit,
    onAction: (ShowcaseStatus) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    // Pause/Reactivate change what's visible to anyone viewing this profile,
    // so they're confirmed via dialog first — Delete stays a direct action
    // from the menu (already the least-surprising of the three: soft-unlist,
    // matches its own explicit label).
    var pendingStatusChange by remember { mutableStateOf<ShowcaseStatus?>(null) }
    // Explains the text-instead-of-image fallback below — same
    // DeviceCapabilityBottomSheet copy already shown at compose time for the
    // same root cause, so the explanation is consistent app-wide.
    var isCapabilityInfoVisible by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val cardScale = rememberPressScale(cardInteractionSource)

    Box(
        modifier = modifier
            .height(90.dp)
            .aspectRatio(if (showEnvelopeFace) EnvelopeAspectRatio else LetterPaperAspectRatio)
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(cardShape)
            .border(1.dp, BrandCardStroke, cardShape)
            // Card body always opens the letter/paper face in full view
            // (see PostalCollectionFullView) — the "···" button below has
            // its own nested clickable, which intercepts its own taps.
            .clickable(interactionSource = cardInteractionSource, indication = null, onClick = onClick),
    ) {
        val fallbackDrawable = if (showEnvelopeFace) item.envelope.drawableRes else item.paperTemplate.drawableRes
        val faceCompositeUrl = if (showEnvelopeFace) item.envelopeCompositeImageUrl else item.compositeImageUrl
        AsyncImage(
            model = faceCompositeUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(fallbackDrawable),
            error = painterResource(fallbackDrawable),
            modifier = Modifier.fillMaxSize(),
        )
        // Paper face with no composite (sender's device skipped compositing)
        // — small excerpt over the fallback texture above, same source as
        // PostalCollectionFullView's full-size fallback, so the thumbnail
        // isn't a bare generic swatch indistinguishable from any other
        // low-density sender's card. Envelope face has no text of its own.
        if (!showEnvelopeFace && faceCompositeUrl == null && !item.bodyText.isNullOrBlank()) {
            Text(
                text = item.bodyText,
                fontFamily = CaveatFontFamily,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = InkDefault,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(6.dp),
            )
            // Tappable "why is this text, not a picture" affordance — bottom
            // corner so it doesn't collide with the "···" menu up top.
            val infoInteractionSource = remember { MutableInteractionSource() }
            val infoScale = rememberPressScale(infoInteractionSource)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(18.dp)
                    .graphicsLayer { scaleX = infoScale; scaleY = infoScale }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = infoInteractionSource,
                        indication = null,
                        onClick = { isCapabilityInfoVisible = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.profile_content_showcase_no_preview_desc),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        // Pause/Delete only make sense for the owner managing their own
        // showcase — skipped entirely for someone else's (readOnly) collection.
        if (!readOnly) {
            val interactionSource = remember { MutableInteractionSource() }
            val scale = rememberPressScale(interactionSource)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { isMenuExpanded = true }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.profile_content_showcase_more_desc),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                AnchoredDropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                    if (item.showcaseStatus == ShowcaseStatus.PAUSED) {
                        DropdownMenuAction(
                            icon = Icons.Filled.Refresh,
                            label = stringResource(R.string.profile_content_showcase_reactivate),
                            onClick = { isMenuExpanded = false; pendingStatusChange = ShowcaseStatus.VISIBLE },
                        )
                    } else {
                        DropdownMenuAction(
                            icon = Icons.Filled.Pause,
                            label = stringResource(R.string.profile_content_showcase_pause),
                            onClick = { isMenuExpanded = false; pendingStatusChange = ShowcaseStatus.PAUSED },
                        )
                    }
                    DropdownMenuAction(
                        icon = Icons.Filled.Delete,
                        label = stringResource(R.string.profile_content_showcase_delete),
                        onClick = { isMenuExpanded = false; onAction(ShowcaseStatus.DELETED) },
                    )
                }
            }
        }
    }

    when (pendingStatusChange) {
        ShowcaseStatus.PAUSED -> LetterlyCenterDialog(
            title = stringResource(R.string.profile_content_showcase_pause_dialog_title),
            body = stringResource(R.string.profile_content_showcase_pause_dialog_body),
            primaryCtaText = stringResource(R.string.profile_content_showcase_pause_dialog_confirm),
            onPrimaryClick = { onAction(ShowcaseStatus.PAUSED); pendingStatusChange = null },
            secondaryCtaText = stringResource(R.string.profile_content_showcase_pause_dialog_cancel),
            onSecondaryClick = { pendingStatusChange = null },
            onDismissRequest = { pendingStatusChange = null },
        )
        ShowcaseStatus.VISIBLE -> LetterlyCenterDialog(
            title = stringResource(R.string.profile_content_showcase_reactivate_dialog_title),
            body = stringResource(R.string.profile_content_showcase_reactivate_dialog_body),
            primaryCtaText = stringResource(R.string.profile_content_showcase_reactivate_dialog_confirm),
            onPrimaryClick = { onAction(ShowcaseStatus.VISIBLE); pendingStatusChange = null },
            secondaryCtaText = stringResource(R.string.profile_content_showcase_reactivate_dialog_cancel),
            onSecondaryClick = { pendingStatusChange = null },
            onDismissRequest = { pendingStatusChange = null },
        )
        ShowcaseStatus.DELETED, null -> Unit
    }

    if (isCapabilityInfoVisible) {
        DeviceCapabilityBottomSheet(onDismiss = { isCapabilityInfoVisible = false })
    }
}

@Composable
fun PostalTabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected)
                    Brush.linearGradient(listOf(BrandGold, Color(0xFFD4822A)))
                else
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .then(
                if (!isSelected) Modifier.border(
                    0.5.dp, BrandGoldDim.copy(alpha = 0.3f), RoundedCornerShape(50)
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = NunitoFontFamily,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp,
            color = if (isSelected) BrandInkDeep else Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun PostalNavigationCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF332519),
                        BrandCardDark,
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(BrandGold.copy(alpha = 0.3f), BrandCardStroke)
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Skeuomorphic stamp-circle emoji holder
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                BrandGold.copy(alpha = 0.22f),
                                BrandGoldDeep.copy(alpha = 0.06f),
                            )
                        )
                    )
                    .border(1.dp, BrandGold.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    fontFamily = NunitoFontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }

        Text(text = "›", fontSize = 24.sp, color = BrandGold.copy(alpha = 0.7f), fontWeight = FontWeight.Light)
    }
}
