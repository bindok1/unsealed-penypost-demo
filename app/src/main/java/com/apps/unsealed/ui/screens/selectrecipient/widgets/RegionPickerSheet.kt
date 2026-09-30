package com.apps.unsealed.ui.screens.selectrecipient.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.feature.addressbook.data.AddressBookContactDto
import com.apps.unsealed.ui.screens.selectrecipient.constants.PenpalRegion
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.BrandInkMid
import kotlinx.coroutines.delay

private val OnlineGreen = Color(0xFF4CAF50)
private val SnappyDpSpring = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)

/**
 * Unified "Select Recipient" bottom sheet with tab switcher:
 * - Tab 0: 🌍 By Region (7 world regions with custom vector illustrations & penpal counts)
 * - Tab 1: 📖 Address Book (saved penpal contacts)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPickerSheet(
    selectedRegion: PenpalRegion?,
    selectedContactId: String? = null,
    onRegionSelect: (PenpalRegion) -> Unit,
    onContactSelect: (AddressBookContactDto) -> Unit = {},
    onDismiss: () -> Unit,
    liveRegionCounts: Map<String, Int> = emptyMap(),
    addressBookContacts: List<AddressBookContactDto> = emptyList(),
    isAddressBookLoading: Boolean = false,
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandInkMid,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            // Header Title
            Text(
                text = stringResource(R.string.recipient_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(14.dp))

            // Tab Switcher Pills
            RecipientTabSelector(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                contactCount = addressBookContacts.size,
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle Description
            Text(
                text = if (selectedTab == 0) {
                    stringResource(R.string.recipient_picker_regions_subtitle)
                } else {
                    stringResource(R.string.recipient_picker_address_book_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )

            Spacer(Modifier.height(14.dp))

            if (selectedTab == 0) {
                // ── TAB 0: REGIONS LIST ──────────────────────────────────────────
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(PenpalRegion.entries, key = { _, region -> region.name }) { index, region ->
                        val isSelected = region == selectedRegion
                        val penpalCount = liveRegionCounts[region.apiLabel] ?: region.dummyPenpalCount

                        RegionRow(
                            region = region,
                            index = index,
                            isSelected = isSelected,
                            penpalCount = penpalCount,
                            onClick = {
                                onRegionSelect(region)
                                onDismiss()
                            },
                        )
                    }
                    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                }
            } else {
                // ── TAB 1: ADDRESS BOOK CONTACTS ─────────────────────────────────
                if (isAddressBookLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = BrandGold,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                } else if (addressBookContacts.isEmpty()) {
                    AddressBookEmptyCard()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(addressBookContacts, key = { _, contact -> contact.id }) { index, contact ->
                            val isSelected = contact.targetUserId == selectedContactId

                            AddressBookPickerContactRow(
                                contact = contact,
                                index = index,
                                isSelected = isSelected,
                                onClick = {
                                    onContactSelect(contact)
                                    onDismiss()
                                },
                            )
                        }
                        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                    }
                }
            }
        }
    }
}

/**
 * Segmented Pill Tab Bar for switching between "By Region" and "Address Book".
 */
@Composable
private fun RecipientTabSelector(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    contactCount: Int,
    modifier: Modifier = Modifier,
) {
    val interactionSource0 = remember { MutableInteractionSource() }
    val interactionSource1 = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandInkDeep)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Tab 0: By Region
        val isTab0Active = selectedTab == 0
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isTab0Active) BrandGold.copy(alpha = 0.2f) else Color.Transparent,
                )
                .border(
                    width = if (isTab0Active) 1.dp else 0.dp,
                    color = if (isTab0Active) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(
                    interactionSource = interactionSource0,
                    indication = null,
                    onClick = { onTabSelect(0) },
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = if (isTab0Active) BrandGold else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.recipient_picker_tab_regions),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isTab0Active) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTab0Active) BrandGold else Color.White.copy(alpha = 0.65f),
                )
            }
        }

        // Tab 1: Address Book
        val isTab1Active = selectedTab == 1
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isTab1Active) BrandGold.copy(alpha = 0.2f) else Color.Transparent,
                )
                .border(
                    width = if (isTab1Active) 1.dp else 0.dp,
                    color = if (isTab1Active) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(
                    interactionSource = interactionSource1,
                    indication = null,
                    onClick = { onTabSelect(1) },
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = if (isTab1Active) BrandGold else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (contactCount > 0) {
                        "${stringResource(R.string.recipient_picker_tab_address_book)} ($contactCount)"
                    } else {
                        stringResource(R.string.recipient_picker_tab_address_book)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isTab1Active) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTab1Active) BrandGold else Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

/**
 * Single row item for a World Region.
 */
@Composable
private fun RegionRow(
    region: PenpalRegion,
    index: Int,
    isSelected: Boolean,
    penpalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        isVisible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-16).dp,
        animationSpec = reducedMotionSpring(SnappyDpSpring, isReducedMotion),
        label = "regionRowOffsetX",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "regionRowAlpha",
    )
    val checkScale = remember { Animatable(if (isSelected) 1f else 0f) }
    LaunchedEffect(isSelected) {
        checkScale.animateTo(
            if (isSelected) 1f else 0f,
            reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.toPx()
                scaleX = pressScale
                scaleY = pressScale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) BrandGold.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Continent Illustration Badge (1.5x enlarged)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) BrandGold.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.08f),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) BrandGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(region.iconRes),
                    contentDescription = stringResource(region.labelRes),
                    modifier = Modifier.size(56.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(region.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = if (isSelected) BrandGold else Color.White,
                )
                Text(
                    text = if (penpalCount >= 100) {
                        stringResource(R.string.region_picker_penpal_count_many)
                    } else {
                        stringResource(R.string.region_picker_penpal_count_few)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }

        // Selection Checkmark Circle
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isSelected) BrandGold else Color.White.copy(alpha = 0.08f))
                .border(1.dp, if (isSelected) BrandGold else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = BrandInkMid,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = checkScale.value
                            scaleY = checkScale.value
                        },
                )
            }
        }
    }
}

/**
 * Single row item for a contact from Address Book.
 */
@Composable
private fun AddressBookPickerContactRow(
    contact: AddressBookContactDto,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isReducedMotion = rememberIsReducedMotion()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        isVisible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-16).dp,
        animationSpec = reducedMotionSpring(SnappyDpSpring, isReducedMotion),
        label = "contactRowOffsetX",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "contactRowAlpha",
    )
    val checkScale = remember { Animatable(if (isSelected) 1f else 0f) }
    LaunchedEffect(isSelected) {
        checkScale.animateTo(
            if (isSelected) 1f else 0f,
            reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.toPx()
                scaleX = pressScale
                scaleY = pressScale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) BrandGold.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Contact Avatar with Gold border & online badge
            Box(modifier = Modifier.size(54.dp)) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(BrandGold, BrandGoldDim)),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!contact.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(contact.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = contact.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Online indicator dot
                if (contact.isOnline == true) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                            .border(2.dp, BrandInkMid, CircleShape),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isSelected) BrandGold else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.location,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!contact.bio.isNullOrBlank()) {
                    Text(
                        text = contact.bio,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Selection Checkmark Circle
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isSelected) BrandGold else Color.White.copy(alpha = 0.08f))
                .border(1.dp, if (isSelected) BrandGold else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = BrandInkMid,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = checkScale.value
                            scaleY = checkScale.value
                        },
                )
            }
        }
    }
}

/**
 * Empty state shown when user has no contacts in Address Book yet.
 */
@Composable
private fun AddressBookEmptyCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = 0.15f))
                    .border(1.dp, BrandGold.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(26.dp),
                )
            }

            Text(
                text = stringResource(R.string.recipient_picker_address_book_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.recipient_picker_address_book_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}
