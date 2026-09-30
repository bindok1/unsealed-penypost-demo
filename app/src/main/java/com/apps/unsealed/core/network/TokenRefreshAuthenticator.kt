package com.apps.unsealed.core.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp [Authenticator] yang menangani respons **401 Unauthorized** dari backend.
 *
 * Berbeda dari [AuthInterceptor] (yang *attaches* token sebelum request),
 * authenticator ini dipanggil **setelah** backend menolak request karena token
 * yang dikirim sudah expire.
 *
 * Flow:
 *  1. Request dikirim dengan token dari [AuthInterceptor] (cached, `forceRefresh=false`).
 *  2. Backend balik 401 (token expired).
 *  3. OkHttp memanggil [authenticate] secara otomatis.
 *  4. Kita force-refresh Firebase token dan ulangi request dengan token baru.
 *  5. Kalau masih 401 setelah retry (token benar-benar invalid/user di-revoke),
 *     return `null` → OkHttp berhenti retry, [AuthRepository.fetchMe] sign out user.
 *
 * Exception handling:
 * Force-refresh menghubungi `securetoken.googleapis.com`. Jika terjadi network error
 * (FirebaseNetworkException / FirebaseException), kita throw [IOException] agar OkHttp
 * menangani sebagai gangguan jaringan dan tidak menandai sesi user sebagai 401 kadaluarsa.
 * Jika kegagalan non-jaringan (misal user di-revoke), return `null` agar 401 diteruskan ke fetchMe.
 *
 * Header sentinel `X-Auth-Retry` mencegah infinite retry loop: kalau request
 * sudah pernah di-retry satu kali dan masih 401, kita stop.
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val auth: FirebaseAuth,
) : Authenticator {

    companion object {
        private const val TAG = "TokenRefreshAuth"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Sudah di-retry sebelumnya dan masih 401 → stop, biarkan fetchMe() handle
        if (response.request.header("X-Auth-Retry") != null) return null

        val currentUser = auth.currentUser ?: return null

        // Force-refresh Firebase token (network call ke Firebase server)
        val freshToken = try {
            runBlocking {
                currentUser.getIdToken(/* forceRefresh= */ true).await()?.token
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e.isNetworkOrConnectionIssue()) {
                Log.w(TAG, "Firebase token force-refresh failed due to network: ${e.message}")
                throw IOException("Firebase token force-refresh network failure: ${e.message}", e)
            }
            Log.w(TAG, "Firebase token force-refresh failed (session invalid/expired): ${e.message}", e)
            null
        } ?: return null  // Tidak ada user atau gagal refresh non-jaringan → stop

        // Ulangi request dengan token baru + sentinel header
        return response.request.newBuilder()
            .header("Authorization", "Bearer $freshToken")
            .header("X-Auth-Retry", "1")
            .build()
    }
}
