package com.apps.unsealed.ui.screens.profile.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.unsealed.R
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.components.LetterlyCenterDialog
import com.apps.unsealed.ui.components.LetterlyOutlinedButton
import com.apps.unsealed.ui.components.LetterlyScreenHeader
import com.apps.unsealed.ui.screens.profile.state.InviteGenerateUiState
import com.apps.unsealed.ui.screens.profile.state.InviteRedeemUiState
import com.apps.unsealed.ui.screens.profile.viewmodel.InviteViewModel
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandGoldDim
import com.apps.unsealed.ui.theme.NunitoFontFamily

private const val InviteCodeMaxLength = 8

/**
 * "Invite Friends" profile sub-screen — generates/shares the viewer's own invite code
 * (`POST /api/v1/invites`) and lets them redeem a friend's code by hand
 * (`POST /api/v1/invites/{code}/redeem`), see `docs/be_updet/invite_friend_api.md` §2.
 * Manual code entry is used for the redeem side (rather than Install Referrer / App Links)
 * since the doc calls that out as an explicit fallback ("gampang diketik manual") and it needs
 * no Manifest/Play Console wiring to ship.
 */
@Composable
fun ProfileInviteFriendsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteViewModel = hiltViewModel(),
) {
    val inviteState by viewModel.inviteState.collectAsStateWithLifecycle()
    val redeemState by viewModel.redeemState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var redeemCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.generateInvite()
        viewModel.consumePendingInviteCodeIfAny()
    }

    LaunchedEffect(redeemState) {
        val state = redeemState
        if (state is InviteRedeemUiState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            viewModel.resetRedeemState()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LetterlyScreenHeader(title = stringResource(R.string.profile_invite_title), onBackClick = onBackClick)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                InviteShareCard(
                    state = inviteState,
                    onRetry = viewModel::generateInvite,
                    onCopy = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, context.getString(R.string.profile_invite_copy_success), Toast.LENGTH_SHORT).show()
                    },
                    onShare = { code, deepLinkUrl ->
                        val shareText = context.getString(R.string.profile_invite_share_message, code, deepLinkUrl)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, context.getString(R.string.profile_invite_share_chooser_title)),
                        )
                    },
                )
            }
            item {
                InviteRedeemCard(
                    code = redeemCode,
                    onCodeChange = { redeemCode = it.uppercase().take(InviteCodeMaxLength) },
                    isSubmitting = redeemState is InviteRedeemUiState.Loading,
                    onSubmit = { viewModel.redeemInvite(redeemCode) },
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    val redeemSuccess = redeemState as? InviteRedeemUiState.Success
    if (redeemSuccess != null) {
        LetterlyCenterDialog(
            title = stringResource(R.string.profile_invite_redeem_success_title),
            body = stringResource(R.string.profile_invite_redeem_success_body, redeemSuccess.inviterName),
            primaryCtaText = stringResource(R.string.profile_invite_redeem_success_cta),
            onPrimaryClick = { viewModel.resetRedeemState(); redeemCode = "" },
            onDismissRequest = { viewModel.resetRedeemState(); redeemCode = "" },
        )
    }
}

@Composable
private fun InviteShareCard(
    state: InviteGenerateUiState,
    onRetry: () -> Unit,
    onCopy: (code: String) -> Unit,
    onShare: (code: String, deepLinkUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandCardDark)
            .border(1.dp, BrandCardStroke, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_invite_section_share_title),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.profile_invite_section_share_body),
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
        )

        when (state) {
            is InviteGenerateUiState.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = BrandGold, strokeWidth = 2.dp)
            }

            is InviteGenerateUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = state.message,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
                LetterlyButton(
                    text = stringResource(R.string.profile_invite_generate_retry_cta),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is InviteGenerateUiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.code,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 4.sp,
                        color = BrandGold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LetterlyOutlinedButton(
                        text = stringResource(R.string.profile_invite_copy_cta),
                        onClick = { onCopy(state.code) },
                        modifier = Modifier.weight(1f),
                    )
                    LetterlyButton(
                        text = stringResource(R.string.profile_invite_share_cta),
                        onClick = { onShare(state.code, state.deepLinkUrl) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.height(16.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRedeemCard(
    code: String,
    onCodeChange: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandCardDark)
            .border(1.dp, BrandCardStroke, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_invite_section_redeem_title),
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.profile_invite_section_redeem_body),
            fontFamily = NunitoFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
        )

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.profile_invite_redeem_label)) },
            placeholder = { Text(stringResource(R.string.profile_invite_redeem_placeholder)) },
            enabled = !isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = BrandGold,
                unfocusedBorderColor = BrandCardStroke,
                focusedLabelColor = BrandGold,
                unfocusedLabelColor = BrandGoldDim,
                cursorColor = BrandGold,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
            ),
        )

        LetterlyButton(
            text = stringResource(R.string.profile_invite_redeem_cta),
            onClick = onSubmit,
            enabled = code.isNotBlank() && !isSubmitting,
            isLoading = isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
