package com.apps.unsealed.ui.screens.penpals.widgets

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apps.unsealed.R
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.MinCompositingDensity
import com.apps.unsealed.core.util.buildInstagramStoryIntent
import com.apps.unsealed.core.util.buildSystemChooserIntent
import com.apps.unsealed.core.util.buildWhatsAppIntent
import com.apps.unsealed.core.util.downloadImageBytes
import com.apps.unsealed.core.util.isPackageInstalled
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.core.util.rememberPressScale
import com.apps.unsealed.core.util.toCompositeBytes
import com.apps.unsealed.core.util.toHexString
import com.apps.unsealed.core.util.writeShareCacheFile
import com.apps.unsealed.ui.screens.penpals.state.PenpalLetter
import com.apps.unsealed.ui.theme.BrandCardDark
import com.apps.unsealed.ui.theme.BrandCardStroke
import com.apps.unsealed.ui.theme.BrandGold
import com.apps.unsealed.ui.theme.BrandInkDeep
import com.apps.unsealed.ui.theme.SurfaceCream
import com.apps.unsealed.ui.theme.ToolbarFrostedDark
import kotlinx.coroutines.launch

/**
 * Preview-then-pick-a-target share sheet for a read letter, opened from
 * [OpenLetterOverlay]'s `ActionRail` Share icon and `SenderInfoBar`'s
 * "Share Mail" menu item — both now converge here instead of the old
 * text-only `ACTION_SEND`. Hand-rolled slide-up (no `ModalBottomSheet`,
 * matching [OpenLetterOverlay]'s own hand-rolled overlay) per
 * docs/motion-rules.md §3.4: enter slower ([LetterlySpring.Gentle]), exit
 * faster ([LetterlySpring.Stiff]).
 *
 * [graphicsLayer] is [com.apps.unsealed.core.util.captureIntoGraphicsLayer]-attached
 * to the letter-card `Box` still composed behind this sheet — captured once on
 * mount into [ImageBitmap] for the preview and reused as the same
 * [ByteArray] for every share target, so there's only ever one capture pass
 * per open, regardless of which target the user ends up tapping.
 *
 * [graphicsLayer] is `null` for callers with no on-screen card to capture
 * (e.g. [com.apps.unsealed.ui.screens.selectrecipient.screen.LetterSentScreen],
 * right after send) — those callers must guarantee [letter]'s
 * [PenpalLetter.compositeImageUrl] is non-null instead, since that's the only
 * other source of share bytes.
 *
 * For plain-text letters (no [PenpalLetter.compositeImageUrl]) the only
 * source of share bytes is capturing [graphicsLayer] live, and that capture
 * is gated on the *viewer's* screen density the same way `ComposeScreen.kt`
 * gates Annotate/Insert-Image/Add-Sticker — below [MinCompositingDensity] the
 * capture comes out too soft to send (see `CompositingCapability.kt`), so
 * this sheet skips it entirely on those devices instead of sharing a broken
 * image.
 */
@Composable
fun ShareLetterSheet(
    letter: PenpalLetter,
    graphicsLayer: GraphicsLayer?,
    onDismiss: () -> Unit,
    onSaveImageRequested: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isReducedMotion = rememberIsReducedMotion()
    val isCompositingSupported = LocalDensity.current.density >= MinCompositingDensity

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    var shareBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    // True only when this letter has neither a downloadable composite (no
    // compositeImageUrl) nor a capturable on-screen card the viewer's device
    // can render legibly (density < MinCompositingDensity) — the sheet has
    // no legible bytes to offer at all, as opposed to shareBytes being null
    // for the ordinary "still loading" moment right after mount.
    var isUnavailableOnThisDevice by remember { mutableStateOf(false) }
    LaunchedEffect(letter.id) {
        // Prefer the sender's already-composited image (good quality
        // regardless of the viewer's own screen density) over re-capturing
        // the on-screen card, which is only as sharp as the *viewer's*
        // device — see CompositingCapability.kt's MinCompositingDensity.
        // Only letters with no compositeImageUrl (plain-text) fall back to
        // an on-screen capture, and only when this device's density can
        // actually render that capture legibly — otherwise there's nothing
        // safe to share and the sheet says so instead of producing a
        // corrupted/illegible image.
        val bytes = letter.compositeImageUrl?.let { url -> downloadImageBytes(context, url) }
            ?: if (isCompositingSupported) graphicsLayer?.toCompositeBytes() else null
        if (bytes == null) {
            isUnavailableOnThisDevice = letter.compositeImageUrl == null && !isCompositingSupported
            return@LaunchedEffect
        }
        shareBytes = bytes
        previewBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
    }

    // No deep link appended yet — no backend route to view a letter by URL
    // exists (see docs/todo.md); revisit once one ships post-release.
    val shareCaption = stringResource(R.string.open_letter_share_text, letter.senderName, letter.bodyText.take(200))
    val chooserTitle = stringResource(R.string.open_letter_share_chooser_title)
    val appUnavailableMessage = stringResource(R.string.share_letter_app_unavailable)
    val storagePermissionDeniedMessage = stringResource(R.string.share_letter_storage_permission_denied)

    // The actual save + success/failure feedback is owned by the caller
    // (OpenLetterOverlay), not here — closeSheet() below tears down this
    // composable (and its rememberCoroutineScope) right after the click, so
    // a save kicked off from *this* scope could get cancelled mid-write
    // before it finishes. Only the fast, synchronous permission check stays
    // local.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            shareBytes?.let(onSaveImageRequested)
        } else {
            Toast.makeText(context, storagePermissionDeniedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun closeSheet() {
        isVisible = false
    }

    fun onSaveImageClick() {
        val bytes = shareBytes ?: return
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            onSaveImageRequested(bytes)
        }
        closeSheet()
    }

    fun onInstagramClick() {
        val bytes = shareBytes ?: return
        scope.launch {
            runCatching {
                val uri = writeShareCacheFile(context, bytes, letter.id)
                context.grantUriPermission("com.instagram.android", uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(
                    buildInstagramStoryIntent(uri, BrandGold.toHexString(), BrandInkDeep.toHexString()),
                )
            }.onFailure {
                Toast.makeText(context, appUnavailableMessage, Toast.LENGTH_SHORT).show()
            }
        }
        closeSheet()
    }

    fun onWhatsAppClick() {
        val bytes = shareBytes ?: return
        scope.launch {
            val targetPackage = when {
                context.isPackageInstalled("com.whatsapp") -> "com.whatsapp"
                context.isPackageInstalled("com.whatsapp.w4b") -> "com.whatsapp.w4b"
                else -> null
            }
            if (targetPackage == null) {
                Toast.makeText(context, appUnavailableMessage, Toast.LENGTH_SHORT).show()
                return@launch
            }
            runCatching {
                val uri = writeShareCacheFile(context, bytes, letter.id)
                context.startActivity(buildWhatsAppIntent(uri, shareCaption, targetPackage))
            }.onFailure {
                Toast.makeText(context, appUnavailableMessage, Toast.LENGTH_SHORT).show()
            }
        }
        closeSheet()
    }

    fun onMoreClick() {
        val bytes = shareBytes ?: return
        scope.launch {
            val uri = writeShareCacheFile(context, bytes, letter.id)
            context.startActivity(buildSystemChooserIntent(uri, shareCaption, chooserTitle))
        }
        closeSheet()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenHeightPx = constraints.maxHeight.toFloat()

        val slideFraction by animateFloatAsState(
            targetValue = if (isVisible) 0f else 1f,
            animationSpec = reducedMotionSpring(
                if (isVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
                isReducedMotion,
            ),
            label = "shareSheetSlide",
            finishedListener = { fraction -> if (fraction >= 0.98f) onDismiss() },
        )
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = reducedMotionSpring(LetterlySpring.Gentle, isReducedMotion),
            label = "shareSheetScrimAlpha",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandInkDeep.copy(alpha = 0.7f * scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::closeSheet,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = screenHeightPx * slideFraction }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BrandCardDark)
                .border(1.dp, BrandCardStroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.share_letter_sheet_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCream)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (isUnavailableOnThisDevice) {
                    Text(
                        text = stringResource(R.string.share_letter_device_unsupported),
                        color = BrandInkDeep.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    previewBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val targetsEnabled = shareBytes != null
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShareTargetIcon(
                    painter = painterResource(R.drawable.ic_instagram),
                    label = stringResource(R.string.share_letter_target_instagram),
                    enabled = targetsEnabled && context.isPackageInstalled("com.instagram.android"),
                    onClick = ::onInstagramClick,
                )
                ShareTargetIcon(
                    painter = painterResource(R.drawable.ic_whatsapp),
                    label = stringResource(R.string.share_letter_target_whatsapp),
                    enabled = targetsEnabled &&
                        (context.isPackageInstalled("com.whatsapp") || context.isPackageInstalled("com.whatsapp.w4b")),
                    onClick = ::onWhatsAppClick,
                )
                ShareTargetIcon(
                    icon = Icons.Filled.FileDownload,
                    label = stringResource(R.string.share_letter_target_save),
                    enabled = targetsEnabled,
                    onClick = ::onSaveImageClick,
                )
                ShareTargetIcon(
                    icon = Icons.Filled.MoreHoriz,
                    label = stringResource(R.string.share_letter_target_more),
                    enabled = targetsEnabled,
                    onClick = ::onMoreClick,
                )
            }
        }
    }
}

@Composable
private fun ShareTargetIcon(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)
    val contentAlpha = if (enabled) 1f else 0.35f
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale; alpha = contentAlpha }
                .size(48.dp)
                .clip(CircleShape)
                .background(ToolbarFrostedDark)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            } else if (painter != null) {
                Icon(painter = painter, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 0.85f else 0.4f),
            fontSize = 11.sp,
        )
    }
}
