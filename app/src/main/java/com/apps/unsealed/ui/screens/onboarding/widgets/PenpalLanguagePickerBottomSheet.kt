package com.apps.unsealed.ui.screens.onboarding.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apps.unsealed.R
import com.apps.unsealed.core.model.AllPenpalLanguages
import com.apps.unsealed.core.model.PenpalLanguage
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenpalLanguagePickerBottomSheet(
    selectedLanguageCodes: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedCodes by remember(selectedLanguageCodes) {
        mutableStateOf(selectedLanguageCodes.ifEmpty { setOf("id") })
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            AllPenpalLanguages
        } else {
            val query = searchQuery.trim().lowercase()
            AllPenpalLanguages.filter { lang ->
                lang.nativeName.lowercase().contains(query) ||
                    lang.englishName.lowercase().contains(query) ||
                    lang.code.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BrandInkMid,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.fillMaxHeight(0.85f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_language_picker_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_language_picker_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.onboarding_language_search_placeholder),
                        color = Color.White.copy(alpha = 0.45f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = BrandGold,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Language List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredLanguages, key = { it.code }) { language ->
                    val isSelected = tempSelectedCodes.contains(language.code)
                    PenpalLanguageItemRow(
                        language = language,
                        isSelected = isSelected,
                        onClick = {
                            tempSelectedCodes = if (isSelected) {
                                // Allow deselecting only if at least 1 remains
                                if (tempSelectedCodes.size > 1) {
                                    tempSelectedCodes - language.code
                                } else {
                                    tempSelectedCodes
                                }
                            } else {
                                tempSelectedCodes + language.code
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Submit Button
            LetterlyButton(
                text = stringResource(R.string.onboarding_language_done_cta, tempSelectedCodes.size),
                onClick = {
                    onConfirm(tempSelectedCodes)
                    onDismiss()
                },
                enabled = tempSelectedCodes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun PenpalLanguageItemRow(
    language: PenpalLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val checkAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(150),
        label = "languageCheckAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) BrandGold.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) BrandGold.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = language.flag,
                fontSize = 24.sp,
            )
            Column {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BrandGold else Color.White,
                )
                if (!language.nativeName.equals(language.englishName, ignoreCase = true)) {
                    Text(
                        text = language.englishName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) BrandGold else Color.White.copy(alpha = 0.1f),
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) BrandGold else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = BrandInkMid,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { alpha = checkAlpha },
                )
            }
        }
    }
}
