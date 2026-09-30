package com.apps.unsealed.ui.screens.onboarding

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.apps.unsealed.R
import com.apps.unsealed.feature.auth.AuthViewModel
import com.apps.unsealed.ui.components.LetterlyButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding step 5 — profile photo.
 *
 * Picks an image via the system photo picker (no runtime permission needed,
 * see OnboardingPermissionsScreen for the *storage* permission, which is a
 * separate, pre-API-29-only concern for *saving* images, not picking them),
 * then presigns + uploads it through [AuthViewModel.uploadProfilePhoto] —
 * same R2 presign flow `ComposeViewModel` uses for composite letter images
 * (see docs/be/api_contract.md's "storage" section: the resulting `public_url`
 * is PATCHed as `photo_url` here).
 */
@Composable
fun OnboardingPhotoScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    // Activity-scoped — see SplashScreen.kt's doc comment.
    viewModel: AuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val isUploading by viewModel.isUploadingPhoto.collectAsState()
    val uploadError by viewModel.photoUploadError.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) selectedUri = uri }

    OnboardingScaffold(
        step = 5,
        totalSteps = 7,
        title = stringResource(R.string.onboarding_photo_title),
        subtitle = stringResource(R.string.onboarding_photo_subtitle),
        onSkip = onSkip,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
        ) {
            if (selectedUri != null) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = stringResource(R.string.onboarding_photo_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(text = "📷", style = MaterialTheme.typography.displayLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (selectedUri != null) R.string.onboarding_photo_pick_hint_change
                else R.string.onboarding_photo_pick_hint,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (uploadError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uploadError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        LetterlyButton(
            text = stringResource(
                if (selectedUri != null) R.string.onboarding_photo_cta_with_photo
                else R.string.onboarding_photo_cta,
            ),
            onClick = {
                focusManager.clearFocus()
                val uri = selectedUri
                if (uri == null) {
                    onNext()
                    return@LetterlyButton
                }
                coroutineScope.launch {
                    val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    if (bytes == null) return@launch
                    viewModel.uploadProfilePhoto(
                        bytes = bytes,
                        contentType = contentType,
                        fileExtension = contentType.substringAfter("/", "jpg"),
                        onSuccess = { onNext() },
                    )
                }
            },
            enabled = !isUploading,
            isLoading = isUploading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
