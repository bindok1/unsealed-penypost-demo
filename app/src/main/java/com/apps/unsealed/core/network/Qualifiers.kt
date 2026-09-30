package com.apps.unsealed.core.network

import javax.inject.Qualifier

/** [okhttp3.OkHttpClient] without [AuthInterceptor]/[TokenRefreshAuthenticator] —
 * for requests to third-party hosts that aren't this app's backend (R2
 * presigned upload PUTs, Coil image loads of public catalog/composite-image
 * URLs) where attaching a Firebase bearer token is unnecessary at best and a
 * credential leak or signature-breaking header at worst. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedClient
