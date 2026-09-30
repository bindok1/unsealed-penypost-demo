package com.apps.unsealed.core.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Intent-building and file/gallery/clipboard glue for [com.apps.unsealed.ui.screens.penpals.widgets.ShareLetterSheet].
 * Kept framework-light (no Compose except [Color]) so the still-unbuilt
 * ComposeScreen "Bagikan Surat"/"Simpan ke Foto" stubs (docs/todo.md #2) can
 * reuse these functions later without dragging in this screen's UI.
 */

/** Requires the `<queries>` block in AndroidManifest.xml for [packageName] —
 * without it, this silently returns false on API 30+ even when installed. */
@Suppress("DEPRECATION") // PackageInfoFlags overload needs API 33; minSdk here is 24.
fun Context.isPackageInstalled(packageName: String): Boolean =
    runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess

/** Writes [bytes] to a per-letter cache file and returns a `content://` [Uri]
 * via [FileProvider] (see `res/xml/file_paths.xml`'s `shared_letters`
 * cache-path), suitable for `EXTRA_STREAM`/`interactive_asset_uri` grants. */
suspend fun writeShareCacheFile(context: Context, bytes: ByteArray, letterId: String): Uri =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared_letters").apply { mkdirs() }
        val file = File(dir, "letter_$letterId.webp")
        FileOutputStream(file).use { it.write(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

/**
 * Downloads [url] (the letter's `compositeImageUrl`, a public R2 CDN URL)
 * through the app's singleton Coil `ImageLoader` and re-encodes it as WebP
 * bytes. Preferred over re-capturing the on-screen letter card for share/
 * save: a screen capture is only as sharp as the *viewer's* device density
 * (see ShareLetterSheet's caller — soft/broken on sub-[MinCompositingDensity]
 * devices), whereas this reuses the *sender's* already-composited image,
 * unaffected by whoever happens to be reading it. Returns null on any
 * failure (network, decode) so the caller can fall back to a live capture.
 */
suspend fun downloadImageBytes(context: Context, url: String): ByteArray? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context).data(url).build()
            val result = context.imageLoader.execute(request)
            if (result !is SuccessResult) return@runCatching null
            val bitmap = result.image.toBitmap(result.image.width, result.image.height)
            val stream = ByteArrayOutputStream()
            @Suppress("DEPRECATION") // WEBP_LOSSY/WEBP_LOSSLESS need API 30; minSdk here is 24.
            bitmap.compress(Bitmap.CompressFormat.WEBP, 92, stream)
            stream.toByteArray()
        }.getOrNull()
    }

/** Instagram is unreliable about honoring `FLAG_GRANT_READ_URI_PERMISSION`
 * set only on the [Intent] — callers should also explicitly call
 * `context.grantUriPermission("com.instagram.android", imageUri, ...)`
 * before `startActivity`. */
fun buildInstagramStoryIntent(imageUri: Uri, topColorHex: String, bottomColorHex: String): Intent =
    Intent("com.instagram.share.ADD_TO_STORY").apply {
        setPackage("com.instagram.android")
        type = "image/*"
        putExtra("interactive_asset_uri", imageUri)
        putExtra("top_background_color", topColorHex)
        putExtra("bottom_background_color", bottomColorHex)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

/** Caller should try `"com.whatsapp"` then fall back to `"com.whatsapp.w4b"`
 * (WhatsApp Business) if the first isn't installed. */
fun buildWhatsAppIntent(imageUri: Uri, caption: String, packageName: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, caption)
        setPackage(packageName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

/** Generic system chooser — the "More" target, covering Telegram/Discord/X/
 * Facebook/etc. without per-app SDK integration. */
fun buildSystemChooserIntent(imageUri: Uri, caption: String, chooserTitle: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(sendIntent, chooserTitle)
}

/**
 * Saves [bytes] to the gallery, returning the resulting `content://` [Uri]
 * (suitable for `ACTION_VIEW`, e.g. a "View" snackbar action) or null on
 * failure. On API 29+, uses scoped-storage `MediaStore` (no permission needed
 * for the app's own contribution). Below that, writes via the legacy `DATA`
 * column under `Pictures/Unsealed` and triggers a media scan — the caller is
 * responsible for ensuring `WRITE_EXTERNAL_STORAGE` is already granted before
 * calling this on API 24–28.
 */
suspend fun saveImageToGallery(context: Context, bytes: ByteArray, displayName: String): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Unsealed")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@runCatching null
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Unsealed",
                ).apply { mkdirs() }
                val file = File(dir, displayName)
                FileOutputStream(file).use { it.write(bytes) }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                uri
            }
        }.getOrNull()
    }

/** `"#RRGGBB"` string for Instagram's `top_background_color`/
 * `bottom_background_color` extras, which reject ARGB. */
fun Color.toHexString(): String = "#%06X".format(0xFFFFFF and toArgb())
