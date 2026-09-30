package com.apps.unsealed.core.network

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.apps.unsealed.BuildConfig
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = Firebase.remoteConfig

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: AuthInterceptor,
        remoteConfigBaseUrlInterceptor: RemoteConfigBaseUrlInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Rewrite host/scheme first, then attach auth/logging — order
            // doesn't affect correctness here, just keeps "where does the
            // request end up going" first in the chain for readability.
            .addInterceptor(remoteConfigBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenRefreshAuthenticator)

        if (BuildConfig.DEBUG) {
            // Chucker: visual HTTP inspector in notification drawer (debug builds only)
            builder.addInterceptor(
                ChuckerInterceptor.Builder(context)
                    .alwaysReadResponseBody(true)
                    .build()
            )
            // OkHttp body logging for Logcat
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    /**
     * `baseUrl` here is only a well-formed template for Retrofit's relative
     * path resolution — the actual scheme/host/port used per request is
     * decided at call time by [RemoteConfigBaseUrlInterceptor] (via
     * [okHttpClient]), so `BuildConfig.BASE_URL`'s per-flavor value doubles
     * as the Remote Config default (see [com.apps.unsealed.core.util.RemoteConfigKeys.defaults]).
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    /** No auth interceptor/authenticator — see [UnauthenticatedClient]. Debug builds still
     * get Chucker/Logcat logging (observation only, adds no headers) so raw R2 PUT
     * requests/responses (e.g. presigned upload failures) are inspectable like any
     * other call, instead of being invisible in Logcat. */
    @Provides
    @Singleton
    @UnauthenticatedClient
    fun provideUnauthenticatedOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                ChuckerInterceptor.Builder(context)
                    .alwaysReadResponseBody(true)
                    .build()
            )
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    /** App-wide Coil `ImageLoader` (wired in as the Coil3 singleton via
     * `UnsealedApplication`'s `SingletonImageLoader.Factory`), built on the
     * unauthenticated client — envelope/stamp catalog art and composite
     * letter images are all public R2 URLs, no Firebase token needed. Without
     * this, `AsyncImage`/`coil3.network.okhttp` has no network fetcher
     * registered at all and any `https://` model silently fails to load
     * (only `android.resource://` local-fallback URIs would work). */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @UnauthenticatedClient okHttpClient: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(okHttpClient)) }
        .build()
}
