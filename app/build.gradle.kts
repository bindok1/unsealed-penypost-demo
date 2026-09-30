import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
}

// Local-only secrets (gitignored, never committed) — currently just the
// Mapbox public access token for the Remote-Config-gated Mapbox map renderer.
// Absent on CI/other devs' machines is fine: it defaults to "" and the app
// safely falls back to the GeoJSON renderer at runtime (see DeliveryRouteMap.kt).
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.apps.unsealed"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.apps.unsealed"
        minSdk = 24
        targetSdk = 36
        versionCode = 28
        versionName = "1.0.27"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "id", "in")

        // Mapbox public access token (see DeliveryRouteMap.kt / MapRenderMode) —
        // add MAPBOX_ACCESS_TOKEN=pk.xxx to your local.properties to test Mapbox
        // mode. Blank by default so every build still compiles without it.
        buildConfigField(
            "String",
            "MAPBOX_ACCESS_TOKEN",
            "\"${localProperties.getProperty("MAPBOX_ACCESS_TOKEN", "")}\""
        )

        // RevenueCat public Google Play API key (see UnsealedApplication.onCreate) —
        // add REVENUECAT_API_KEY=goog_xxx to your local.properties. Blank by
        // default so every build still compiles; Purchases.configure() is
        // skipped at runtime when it's blank.
        buildConfigField(
            "String",
            "REVENUECAT_API_KEY",
            "\"${localProperties.getProperty("REVENUECAT_API_KEY", "")}\""
        )
    }

    // ── Flavors ──────────────────────────────────────────────────────────────
    // Switch di Android Studio: Build > Select Build Variant → devDebug / prodDebug
    // BASE_URL dipakai oleh NetworkModule via BuildConfig.BASE_URL.
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            versionNameSuffix = "-dev"
            // Railway development backend (HTTPS)
            buildConfigField("String", "BASE_URL", "\"https://unsealed-be-development.up.railway.app/\"")
            // Web OAuth Client ID (type:3) dari google-services.json — dibutuhkan
            // oleh Credential Manager untuk Google Sign-In
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"642530889152-v8q4i551n33sf39h4rchfgpvkh92gv0b.apps.googleusercontent.com\""
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.haze)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.android.installreferrer)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Local DB (Room) ───────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── MapLibre (delivery tracking map — no tile server, bundled GeoJSON only) ─
    implementation(libs.maplibre.android.sdk)

    // ── Mapbox (delivery tracking map — Remote-Config-gated alternate renderer,
    //    Style.STANDARD; falls back to MapLibre/GeoJSON above when the remote
    //    flag is off or MAPBOX_ACCESS_TOKEN isn't configured) ──────────────────
    implementation(libs.mapbox.maps.android)

    // ── Konfetti (celebration effects) ─────────────────────────────────────────
    implementation(libs.konfetti.compose)

    // ── Lottie (Mode Peny's magician-wand trigger glyph) ─────────────────────
    implementation(libs.lottie.compose)

    // ── WorkManager (daily unread-mail reminder) ─────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // ── Play In-App Review (bottom-sheet star rating, no Play Store redirect) ─
    implementation(libs.google.play.review.ktx)

    // ── Play In-App Updates (flexible background download / immediate force update) ─
    implementation(libs.google.play.app.update.ktx)

    // ── RevenueCat (in-app purchases/subscriptions) ───────────────────────────
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui) // PaywallDialog

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)

    // ── Google Sign-In (Credential Manager) ──────────────────────────────────
    implementation(libs.google.identity.credential)
    implementation(libs.google.identity.credential.play)
    implementation(libs.google.identity.googleid)

    // ── Network ───────────────────────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // ── Chucker HTTP Inspector (debug-only) ───────────────────────────────────
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.noop)

    // ── Tests ─────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}