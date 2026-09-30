package com.apps.unsealed.core.network

import com.apps.unsealed.core.util.RemoteConfigKeys
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every backend request's scheme/host/port (path/query untouched)
 * to the current [RemoteConfigKeys.API_BASE_URL] value — lets the API
 * domain be swapped remotely (incident failover, migration) without an app
 * release, on top of Retrofit's build-time `baseUrl` (`BuildConfig.BASE_URL`).
 *
 * Falls back to the request's original URL unchanged if the Remote Config
 * value isn't a well-formed URL (covers "key not set yet" and any bad
 * console value alike — never crashes the request over it).
 */
@Singleton
class RemoteConfigBaseUrlInterceptor @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val override = remoteConfig.getString(RemoteConfigKeys.API_BASE_URL).toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme(override.scheme)
            .host(override.host)
            .port(override.port)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
