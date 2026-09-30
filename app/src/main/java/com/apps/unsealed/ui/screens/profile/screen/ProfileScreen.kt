package com.apps.unsealed.ui.screens.profile.screen

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthUiState
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.feature.auth.data.InterestDto
import com.apps.unsealed.feature.auth.data.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.apps.unsealed.ui.components.LetterlyButton
import com.apps.unsealed.ui.components.LoginPromptBottomSheet
import com.apps.unsealed.ui.components.rememberAuthGuard
import com.apps.unsealed.ui.screens.profile.state.PostalCollectionUiState
import com.apps.unsealed.ui.screens.profile.state.ShowcaseActionState
import com.apps.unsealed.ui.screens.profile.widgets.PostalCollectionFullView
import com.apps.unsealed.ui.screens.profile.widgets.UnsealedProfileContent
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.NunitoFontFamily

@Composable
fun ProfileScreen(
    onDraftClick: () -> Unit,
    onAddressBookClick: () -> Unit,
    onStampBookClick: () -> Unit = {},
    onInviteFriendsClick: () -> Unit = {},
    onTipsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Same shared Activity-scoped instance as PenPalsScreen/MainActivity's
    // authGuard — see PenPalsScreen.kt's comment on why the default
    // NavBackStackEntry-scoped hiltViewModel() caused stale "Guest" state.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val uiState by viewModel.uiState.collectAsState()
    val interestCatalog by viewModel.interests.collectAsState()
    val postalCollection by viewModel.postalCollection.collectAsState()
    val showcaseActionState by viewModel.showcaseActionState.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    val photoUploadError by viewModel.photoUploadError.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authGuard = rememberAuthGuard(viewModel)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    viewModel.uploadProfilePhoto(
                        bytes = bytes,
                        contentType = contentType,
                        fileExtension = contentType.substringAfter("/", "jpg"),
                        onSuccess = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_photo_updated_success),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }
        }
    }

    var fullViewIndex by remember { mutableStateOf<Int?>(null) }
    var isFullViewOpen by remember { mutableStateOf(false) }

    LaunchedEffect(photoUploadError) {
        val error = photoUploadError
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Unauthenticated) {
            authGuard.guard {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInterests()
        viewModel.loadPostalCollection()
    }

    LaunchedEffect(showcaseActionState) {
        val state = showcaseActionState
        if (state is ShowcaseActionState.Error) {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            viewModel.resetShowcaseActionState()
        }
    }

    val user: UserDto? = when (val state = uiState) {
        is AuthUiState.Authenticated -> state.user
        is AuthUiState.NeedsOnboarding -> state.user
        else -> null
    }
    val userInterests: List<InterestDto> = remember(user?.interests, interestCatalog) {
        val ids = user?.interests.orEmpty().toSet()
        interestCatalog.filter { it.id in ids }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandInkDeep),
    ) {
        when {
            uiState is AuthUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandGold,
                )
            }

            uiState is AuthUiState.Error -> {
                val errorState = uiState as AuthUiState.Error
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = errorState.message,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp,
                        fontFamily = NunitoFontFamily,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandCardDark)
                            .border(1.dp, BrandCardStroke, CircleShape)
                            .clickable { viewModel.checkAuthState() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Retry",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NunitoFontFamily,
                        )
                    }
                }
            }

            uiState is AuthUiState.Unauthenticated && user == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.mascot_splashscreen),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.auth_guard_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NunitoFontFamily,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auth_guard_body),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontFamily = NunitoFontFamily,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    LetterlyButton(
                        text = stringResource(R.string.auth_guard_cta_sign_in),
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth(0.7f),
                    )
                }
            }

            user != null -> {
                UnsealedProfileContent(
                    user = user,
                    interests = userInterests,
                    postalCollection = postalCollection,
                    onDraftClick = onDraftClick,
                    onAddressBookClick = onAddressBookClick,
                    onStampBookClick = onStampBookClick,
                    onInviteFriendsClick = onInviteFriendsClick,
                    onTipsClick = onTipsClick,
                    onSettingsClick = onSettingsClick,
                    onShowcaseItemAction = viewModel::updateShowcaseStatus,
                    onShowcaseItemClick = { index ->
                        fullViewIndex = index
                        isFullViewOpen = true
                    },
                    isUploadingPhoto = isUploadingPhoto,
                    onAvatarClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
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

        if (authGuard.isPromptVisible) {
            LoginPromptBottomSheet(
                onDismiss = authGuard.dismiss,
                onSignInClick = onLoginClick,
            )
        }
    }
}
