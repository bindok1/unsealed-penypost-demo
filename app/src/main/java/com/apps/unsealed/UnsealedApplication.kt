package com.apps.unsealed

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import android.util.Log
import com.apps.unsealed.core.util.RemoteConfigKeys
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.maplibre.android.MapLibre

@HiltAndroidApp
class UnsealedApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    // Hilt injects the same ImageLoader NetworkModule builds for direct
    // injection elsewhere — one instance, one OkHttpClient, app-wide.
    @Inject lateinit var imageLoader: ImageLoader

    // Lets UnreadReminderWorker (core/reminder) get its dependencies
    // (@AssistedInject + AuthRepository/MailboxRepository) via Hilt instead of
    // WorkManager's default no-arg factory. Implementing Configuration.Provider
    // switches WorkManager to on-demand initialization (WorkManager.initialize()
    // is called lazily on first access, using this config) — the default App
    // Startup WorkManagerInitializer is removed in AndroidManifest.xml so the
    // two initialization paths don't conflict.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // No API key — DeliveryRouteMap builds its own empty Style from a
        // bundled GeoJSON asset, no tile server involved.
        MapLibre.getInstance(this)

        // In-app purchases/subscriptions (RevenueCat wraps Play Billing —
        // this app never talks to BillingClient directly). Skipped when no
        // key is configured (see REVENUECAT_API_KEY in local.properties) so
        // every build still runs without one.
        if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
            Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
            val configBuilder = PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY)
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                configBuilder.appUserID(uid)
            }
            Purchases.configure(configBuilder.build())
        } else {
            Log.w("UnsealedApplication", "REVENUECAT_API_KEY missing — Purchases not configured")
        }

        // Seed local fallbacks synchronously (so getString() is never blank —
        // see RemoteConfigKeys) and kick a background fetch, before any
        // network call (NetworkModule) or legal-doc read (LegalLinks) can
        // happen. Debug builds fetch every launch; release respects the
        // default 1h Remote Config throttle.
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
                },
            )
            setDefaultsAsync(RemoteConfigKeys.defaults())
            fetchAndActivate()
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
