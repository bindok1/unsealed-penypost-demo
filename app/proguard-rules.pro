# ── Unsealed Proguard / R8 Rules ─────────────────────────────────────────────

# ── 1. App Data Classes & DTOs (Moshi / Gson / Serialization) ─────────────────
# Keep all data models and DTOs so JSON field names are never stripped or mangled
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.apps.unsealed.core.network.** { *; }
-keep class com.apps.unsealed.core.database.** { *; }
-keep class com.apps.unsealed.feature.**.data.** { *; }
-keep class com.apps.unsealed.ui.screens.**.state.** { *; }
-keep class com.apps.unsealed.ui.theme.** { *; }

# ── 2. Retrofit & OkHttp ───────────────────────────────────────────────────────
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$Java8
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class okhttp3.ResponseBody
-keep,allowobfuscation,allowshrinking class okhttp3.RequestBody

# ── 3. Moshi ───────────────────────────────────────────────────────────────────
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-dontwarn com.squareup.moshi.**

# ── 4. Room Database ───────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── 5. Google Credential Manager & Google Sign-In ──────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ── 6. MapLibre Android SDK ───────────────────────────────────────────────────
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# ── 7. Coil 3 Image Loader ─────────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── 8. Firebase & Crashlytics ─────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable,*Annotation*
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public *;
}
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── 9. Konfetti & Haze ─────────────────────────────────────────────────────────
-keep class nl.dionsegijn.konfetti.** { *; }
-keep class dev.chrisbanes.haze.** { *; }

# ── 10. Coroutines ────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── 11. Mapbox Maps SDK ────────────────────────────────────────────────────────
-keep class com.mapbox.** { *; }
-dontwarn com.mapbox.**

# ── 12. RevenueCat ─────────────────────────────────────────────────────────────
-keep class com.revenuecat.purchases.** { *; }
