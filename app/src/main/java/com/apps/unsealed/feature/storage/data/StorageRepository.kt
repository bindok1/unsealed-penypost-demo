package com.apps.unsealed.feature.storage.data

import android.content.Context
import androidx.annotation.StringRes
import com.apps.unsealed.R
import com.apps.unsealed.core.network.UnauthenticatedClient
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit

@Singleton
class StorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
    @UnauthenticatedClient private val rawOkHttpClient: OkHttpClient,
) {
    private val api: StorageApi by lazy { retrofit.create(StorageApi::class.java) }

    suspend fun presign(
        fileName: String,
        contentType: String,
        @StringRes errorRes: Int = R.string.error_storage_presign,
    ): AuthResult<PresignResponse> =
        runCatching { api.presign(PresignRequest(fileName, contentType)).unwrap() }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, errorRes), it)
            },
        )

    /** PUTs [bytes] directly to an R2 presigned URL — deliberately bypasses the
     * app's Firebase-token `AuthInterceptor` via [rawOkHttpClient]/[UnauthenticatedClient],
     * since the URL's own signature is R2's auth and an unexpected
     * `Authorization` header can break signature validation. [contentType]
     * must match what was sent to [presign] — R2 presigned URLs are signed
     * against a specific content-type. */
    suspend fun uploadToPresignedUrl(
        uploadUrl: String,
        bytes: ByteArray,
        contentType: String,
        @StringRes errorRes: Int = R.string.error_storage_upload,
    ): AuthResult<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(bytes.toRequestBody(contentType.toMediaType()))
                    .build()
                rawOkHttpClient.newCall(request).execute().use { response ->
                    check(response.isSuccessful) {
                        // R2/S3 always answers a failed PUT with an XML body like
                        // <Error><Code>AccessDenied</Code><Message>...</Message></Error> —
                        // surface it here so Crashlytics shows the real reason (e.g.
                        // AccessDenied from a read-only R2 token, vs. ExpiredRequest)
                        // instead of a bare status code.
                        val errorBody = runCatching { response.body?.string() }.getOrNull()
                        "Upload failed: HTTP ${response.code} - ${errorBody.orEmpty()}"
                    }
                }
            }
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, errorRes), it)
            },
        )
}
