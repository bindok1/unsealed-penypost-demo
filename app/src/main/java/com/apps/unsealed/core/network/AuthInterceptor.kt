package com.apps.unsealed.core.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches a Firebase ID token to every backend
 * request as `Authorization: Bearer <token>`.
 *
 * Uses [runBlocking] deliberately — OkHttp interceptors run on a background
 * thread and the call must be synchronous from OkHttp's perspective.
 * [FirebaseAuth.currentUser] is null-safe: no token is added if the user is
 * not signed in, so public/unprotected endpoints still work.
 *
 * Exception handling:
 * Token retrieval can fail due to network loss, handshake failures, or host
 * unreachable to `securetoken.googleapis.com`. Any Firebase network exception or
 * network-related [com.google.firebase.FirebaseException] is converted to [IOException]
 * so OkHttp handles it within standard I/O error boundaries instead of crashing the thread.
 * Non-network failures (such as deleted/disabled users) are safely swallowed with
 * a warning log, allowing the request to proceed without a token and trigger 401 handling.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val auth: FirebaseAuth,
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentUser = auth.currentUser
        val token = if (currentUser != null) {
            try {
                runBlocking {
                    currentUser.getIdToken(/* forceRefresh = */ false).await()?.token
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.isNetworkOrConnectionIssue()) {
                    Log.w(TAG, "Network error retrieving Firebase token: ${e.message}")
                    throw IOException("Network error retrieving Firebase auth token: ${e.message}", e)
                }
                Log.w(TAG, "Non-network error retrieving Firebase token: ${e.message}", e)
                null
            }
        } else {
            null
        }

        val request = chain.request().newBuilder().apply {
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
