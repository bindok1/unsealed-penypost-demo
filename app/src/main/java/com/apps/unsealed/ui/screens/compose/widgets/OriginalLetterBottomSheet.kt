package com.apps.unsealed.ui.screens.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.screens.penpals.state.formattedPostedAt
import com.apps.unsealed.ui.theme.PaperTemplate
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDeep
import com.apps.unsealed.ui.theme.BrandInk
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.CaveatFontFamily
import com.apps.unsealed.ui.theme.NunitoFontFamily
import com.apps.unsealed.ui.theme.SurfaceBorderLight
import com.apps.unsealed.ui.theme.SurfaceCardLight
import com.apps.unsealed.ui.theme.SurfaceCream

/**
 * Cozy modal bottom sheet allowing the user to view the full original letter
 * from their correspondent while drafting a reply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalLetterBottomSheet(
    letter: PenpalLetter,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCream,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Drag indicator + Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letter.region.emoji,
                            fontSize = 20.sp,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.original_letter_sheet_title, letter.senderName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandInkDeep,
                            fontFamily = NunitoFontFamily,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(letter.region.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandInk.copy(alpha = 0.7f),
                                fontFamily = NunitoFontFamily,
                            )
                            Text(
                                text = " · ${letter.formattedPostedAt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandInk.copy(alpha = 0.5f),
                                fontFamily = NunitoFontFamily,
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.open_letter_close_desc),
                        tint = BrandInkDeep,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Skeuomorphic parchment paper container — prefers the sender's
            // flattened composite image (image-mode letters) over bodyText,
            // same fallback order as OpenLetterOverlay's full-feed reader.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardLight,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
            ) {
                if (letter.compositeImageUrl != null) {
                    AsyncImage(
                        model = letter.compositeImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        placeholder = painterResource(PaperTemplate.LINED.drawableRes),
                        error = painterResource(PaperTemplate.LINED.drawableRes),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = letter.bodyText.ifBlank { stringResource(R.string.original_letter_sheet_empty_body) },
                            fontFamily = CaveatFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            color = BrandInkDeep,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGoldDeep,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Text(
                    text = stringResource(R.string.original_letter_sheet_cta_back),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = NunitoFontFamily,
                )
            }
        }
    }
}
