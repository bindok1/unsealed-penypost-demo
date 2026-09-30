package com.apps.unsealed.core.util

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

/**
 * Hosted legal documents, shown in-app via [com.apps.unsealed.ui.components.LegalDocumentBottomSheet]
 * instead of launching an external browser.
 *
 * Backed by Firebase Remote Config ([RemoteConfigKeys.TERMS_OF_SERVICE_URL] /
 * [RemoteConfigKeys.PRIVACY_POLICY_URL]) so these can be updated without a
 * release. [RemoteConfigKeys.defaults] is seeded in
 * [com.apps.unsealed.UnsealedApplication.onCreate] before any UI can read
 * these properties, so `getString(...)` below is never blank.
 */
object LegalLinks {
    val TERMS_OF_SERVICE_URL: String
        get() = Firebase.remoteConfig.getString(RemoteConfigKeys.TERMS_OF_SERVICE_URL)

    val PRIVACY_POLICY_URL: String
        get() = Firebase.remoteConfig.getString(RemoteConfigKeys.PRIVACY_POLICY_URL)
}
