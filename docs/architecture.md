# Unsealed — Architecture Guide
*Untuk developer manusia & AI agent yang lanjut ngerjain app ini*

> **Update terakhir:** Juli 2026 — berlaku setelah refactor brand palette & komponen `LetterlyButton`.

---

## Daftar Isi

1. [Gambaran Besar](#1-gambaran-besar)
2. [Stack Teknologi](#2-stack-teknologi)
3. [Struktur Direktori](#3-struktur-direktori)
4. [Layer Arsitektur](#4-layer-arsitektur)
5. [Network Layer](#5-network-layer)
6. [Auth & State Machine](#6-auth--state-machine)
7. [Navigation Flow](#7-navigation-flow)
8. [UI Layer — Design System](#8-ui-layer--design-system)
9. [Motion & Animation Rules](#9-motion--animation-rules)
10. [Dependency Injection (Hilt)](#10-dependency-injection-hilt)
11. [Build Variants](#11-build-variants)
12. [Aturan Wajib untuk AI Agent](#12-aturan-wajib-untuk-ai-agent)

---

## 1. Gambaran Besar

**Unsealed** adalah app surat-menyurat digital (pen pal) dengan estetika *warm kawai* — seperti benar-benar duduk menulis surat di atas kertas fisik. User menulis surat, menghias dengan annotasi tangan, kirim ke pen pal dari belahan dunia lain.

```
Android (Jetpack Compose)
        │
        │  HTTPS (Retrofit + OkHttp)
        ▼
Railway Backend (unsealed-be-development.up.railway.app)
        │
        │  REST API  /api/v1/...
        ▼
    PostgreSQL
        │
Firebase Auth ──── Google Sign-In ──── user token ──── backend (via Bearer JWT)
```

**Auth Model:** Firebase Authentication dipakai sebagai identity provider. Backend memvalidasi Firebase ID Token di setiap request — bukan session/cookie sendiri.

---

## 2. Stack Teknologi

| Kategori | Library | Catatan |
|---|---|---|
| **UI** | Jetpack Compose + Material3 | `androidx.compose.*` |
| **Navigation** | Navigation Compose | Single-activity, `NavHostController` |
| **DI** | Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel` |
| **Network** | Retrofit2 + OkHttp3 | JSON parsing via Moshi |
| **JSON** | Moshi + KotlinJsonAdapterFactory | `@JsonClass(generateAdapter = true)` |
| **Auth** | Firebase Auth + Google Credential Manager | `FirebaseAuth`, `CredentialManager` |
| **Crash Reporting** | Firebase Crashlytics | Automated crash & non-fatal exception tracking |
| **Analytics** | Firebase Analytics | Event tracking via `AnalyticsRepository` (`core/analytics/`) |
| **Debug HTTP inspector** | Chucker | Hanya di `DEBUG` builds |
| **Font** | Nunito Variable (`nunito_variable.ttf`) | UI font utama |
| **Letter fonts** | Caveat, Kalam, ArchitectsDaughter, IndieFlower, NothingYouCouldDo, ShadowsIntoLight | Hanya di letter canvas |

---

## 3. Struktur Direktori

```
app/src/main/java/com/apps/unsealed/
├── MainActivity.kt              ← Single Activity, UnsealedTheme wrapper
├── UnsealedApplication.kt       ← @HiltAndroidApp
│
├── core/
│   ├── analytics/
│   │   └── AnalyticsRepository.kt ← Wrapper FirebaseAnalytics (logEvent, setUserId)
│   ├── network/
│   │   ├── AuthInterceptor.kt   ← Inject Firebase token ke setiap request
│   │   ├── FirebaseModule.kt    ← @Provides FirebaseAuth, FirebaseCrashlytics & FirebaseAnalytics (Hilt Singleton)
│   │   └── NetworkModule.kt    ← @Provides Moshi, OkHttpClient, Retrofit
│   └── util/
│       ├── LetterlySpring.kt    ← Named spring presets (Snappy/Bouncy/Gentle/Stiff/Envelope)
│       ├── MotionUtils.kt       ← rememberPressScale, staggerEntrance, rememberIsReducedMotion
│       ├── SoftwareBlur.kt      ← Pre-compute blurred bitmap (Inbox bg)
│       └── TiltParallax.kt      ← Gyroscope parallax untuk bg_texture
│
├── feature/
│   └── auth/
│       ├── AuthViewModel.kt     ← AuthUiState machine, semua auth actions
│       └── data/
│           ├── AuthApi.kt       ← Retrofit interface
│           ├── AuthDto.kt       ← UserDto, InterestDto, request/response bodies
│           └── AuthRepository.kt ← Google Sign-In flow, Firebase → backend bridge
│
├── navigation/
│   ├── Destinations.kt          ← Bottom nav routes + auth routes
│   └── UnsealedNavHost.kt       ← NavHost: semua composable routes dan navigate() calls
│
└── ui/
    ├── components/              ← SHARED COMPONENTS (wajib dipakai, jangan inline)
    │   ├── LetterlyButton.kt    ← Primary CTA + Outlined variant
    │   ├── LetterlySlider.kt    ← Pengganti Material Slider
    │   ├── LetterlyMenuRow.kt   ← Row ikon+judul+trailing (Profile menus)
    │   ├── LetterlyStoreCard.kt ← Kartu toko (Square/Wide variant)
    │   ├── LetterlyScreenHeader.kt ← Back arrow + judul (stack sub-screens)
    │   ├── AnchoredDropdownMenu.kt ← Dark chrome popup anchored ke trigger
    │   ├── BottomNavBar.kt      ← iOS-style tab bar
    │   └── GoogleSignInButton.kt ← "Sign in with Google" branded button
    │
    ├── screens/
    │   ├── auth/
    │   │   ├── SplashScreen.kt  ← Auth routing gate, animated logo
    │   │   ├── LoginScreen.kt   ← Google Sign-In CTA
    │   │   └── RegisterScreen.kt ← Nickname + continent picker
    │   ├── onboarding/
    │   │   ├── OnboardingScaffold.kt      ← Shared step indicator + skip
    │   │   ├── OnboardingGenderBirthdayScreen.kt
    │   │   ├── OnboardingLanguageBioScreen.kt
    │   │   ├── OnboardingPhotoScreen.kt
    │   │   └── OnboardingInterestsScreen.kt
    │   ├── compose/             ← Layar nulis surat (jantung app)
    │   ├── inbox/
    │   ├── penpals/
    │   ├── profile/
    │   ├── selectrecipient/
    │   └── stamps/
    │
    └── theme/
        ├── Color.kt             ← Brand tokens (BrandGold, BrandInk, SurfaceCream, ...)
        ├── Theme.kt             ← UnsealedTheme — MaterialTheme wrapper
        ├── Type.kt              ← NunitoFontFamily, UnsealedTypography, handwriting fonts
        └── PaperTemplate.kt     ← Paper texture enum
```

---

## 4. Layer Arsitektur

```
┌──────────────────────────────────────────┐
│              UI Layer                    │
│  Screen Composables (ui/screens/)        │
│  Shared Components (ui/components/)      │
│  hiltViewModel() → AuthViewModel         │
└──────────────┬───────────────────────────┘
               │ StateFlow<AuthUiState>
               │ fun signIn / register / patchMe
               ▼
┌──────────────────────────────────────────┐
│           ViewModel Layer                │
│  AuthViewModel (@HiltViewModel)          │
│  - Holds AuthUiState machine             │
│  - Calls AuthRepository                  │
└──────────────┬───────────────────────────┘
               │ suspend fun + AuthResult<T>
               ▼
┌──────────────────────────────────────────┐
│           Repository Layer               │
│  AuthRepository (@Singleton)             │
│  - Google Sign-In via CredentialManager  │
│  - Firebase signInWithCredential()       │
│  - Calls AuthApi (Retrofit)              │
└──────────┬───────────────┬───────────────┘
           │               │
           ▼               ▼
┌──────────────┐   ┌──────────────────────┐
│ Firebase SDK │   │  Retrofit + OkHttp   │
│ FirebaseAuth │   │  AuthInterceptor      │
│ (token source│   │  (injects Bearer JWT) │
│  & sign-in)  │   │  → Railway backend    │
└──────────────┘   └──────────────────────┘
```

**Prinsip:** UI tidak pernah memanggil Firebase atau Retrofit langsung. Semua melalui ViewModel → Repository.

---

## 5. Network Layer

### Setup (NetworkModule.kt)

```
Moshi
  └── KotlinJsonAdapterFactory

OkHttpClient
  ├── AuthInterceptor         ← Firebase ID Token di-inject ke semua request
  ├── ChuckerInterceptor      ← Debug only
  └── HttpLoggingInterceptor  ← Debug only, BODY level

Retrofit
  ├── baseUrl = BuildConfig.BASE_URL
  ├── client = OkHttpClient
  └── MoshiConverterFactory
```

### AuthInterceptor — Cara kerja

```
Setiap HTTP request:
  1. Ambil Firebase currentUser
  2. Kalau ada user → getIdToken(forceRefresh=false).await()
  3. Inject header: "Authorization: Bearer <token>"
  4. Kalau tidak ada user → request dikirim tanpa Authorization header

PENTING: getIdToken() dijalankan dengan runBlocking karena OkHttp interceptor
adalah synchronous. Token Firebase auto-refresh setiap 1 jam oleh SDK.
```

### AuthApi — Endpoints

| Method | Endpoint | Auth | Catatan |
|---|---|---|---|
| `GET` | `/api/v1/auth/me` | Bearer | Return `UserDto` atau **404** (user baru) |
| `POST` | `/api/v1/auth/register` | Bearer | Body: `{nickname, continent}` → `UserDto` 201 |
| `PATCH` | `/api/v1/auth/me` | Bearer | Partial update — kirim hanya field yang berubah |
| `PUT` | `/api/v1/auth/me/interests` | Bearer | Replace seluruh set interest (bukan additive) |
| `GET` | `/api/v1/interests` | Bearer | Katalog interest untuk picker |

### Response Pattern — AuthResult

```kotlin
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>()
}
```

Repository selalu return `AuthResult` — tidak pernah throw exception ke ViewModel.

### Parsing

Moshi dengan `@JsonClass(generateAdapter = true)` + `@Json(name = "snake_case")`.
DTO juga memiliki `@Serializable` annotation — ini legacy, parsing aktual dilakukan Moshi.

---

## 6. Auth & State Machine

### AuthUiState

```kotlin
sealed class AuthUiState {
    object Loading          // Splash masih tampil, check sedang berjalan
    object Unauthenticated  // Tidak ada Firebase user → LoginScreen
    object NeedsRegister    // Firebase user ada, belum POST /register → RegisterScreen
    data class NeedsOnboarding(val user: UserDto)  // Onboarding belum selesai
    data class Authenticated(val user: UserDto)    // Full access → main app
    data class Error(val message: String)          // Fallback ke login
}
```

### Routing Decision Tree

```
App launch
    │
    ▼
SplashScreen ─── checkAuthState() via AuthViewModel.init
    │
    ├── Loading ──────────────────── Tunggu
    ├── Unauthenticated ──────────── → LoginScreen → Google Sign-In
    ├── NeedsRegister ────────────── → RegisterScreen → {nickname + continent}
    ├── NeedsOnboarding ──────────── → OnboardingGenderBirthday
    │                                 → OnboardingLanguageBio
    │                                 → OnboardingPhoto
    │                                 → OnboardingInterests
    │                                 → Main App (PenPals)
    ├── Authenticated ────────────── → Main App (PenPals)
    └── Error ────────────────────── → LoginScreen
```

### Onboarding Completion Logic

`UserDto.needsOnboarding()` = true kalau **gender == null** ATAU **birthday == null** ATAU **interests.isEmpty()**

Setiap step: `viewModel.patchMe(PatchMeRequest(...))` untuk partial update.
Step terakhir (interests): `viewModel.putInterests(ids)` → sekaligus set state ke `Authenticated`.

---

## 7. Navigation Flow

### Semua Routes

```
Auth stack (bottom nav hidden):
  splash
  login
  register

Onboarding wizard (bottom nav hidden):
  onboarding/gender_birthday
  onboarding/language_bio
  onboarding/photo
  onboarding/interests

Main tabs (bottom nav visible):
  penpals     [Tab 1]
  inbox       [Tab 2]
  write       [Tab 3]
  stamps      [Tab 4]
  profile     [Tab 5]

Stack-only (bottom nav hidden):
  select_recipient
  open_letter/{letterId}     ← satu-satunya route dengan nav argument
  profile_draft
  profile_address_book
  profile_stamp_book
  profile_tips
  profile_settings
```

### Aturan Bottom Nav Visibility

Set `HideBottomNavRoutes` di `UnsealedNavHost.kt` — bottom nav disembunyikan di:
- Auth stack (splash, login, register)
- Semua onboarding routes
- select_recipient
- Semua profile sub-screens
- open_letter/\* (matched via `currentRoute?.startsWith(OpenLetterRoute)`)

### Tab Navigation Pattern

```kotlin
navController.navigate(destination.route) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
// Scroll position & state tiap tab tersimpan saat pindah, dikembalikan saat kembali.
```

### Background Strategy (MainActivity.kt)

- **Auth/onboarding screens:** Layar menyediakan background sendiri. `bg_texture` TIDAK dirender.
- **Main app (non-inbox):** `bg_texture` + tilt parallax via gyroscope (`rememberTiltParallaxOffset`).
- **Inbox / OpenLetter:** `bg_texture` di-blur via software (`softwareBlurredBitmap`, pre-computed) + dark tint overlay.

---

## 8. UI Layer — Design System

### Brand Palette (Color.kt)

| Token | Hex | Fungsi |
|---|---|---|
| `BrandGold` | `#C9966A` | CTA utama, tombol, active dot, chip selected |
| `BrandGoldDim` | `#7D5A3C` | Subtitle, hint, placeholder, label sekunder |
| `BrandGoldDeep` | `#5C3D1E` | Pressed state, border, divider |
| `BrandInk` | `#1C1109` | Text di atas BrandGold, dark bg elemen |
| `BrandInkDeep` | `#0F0A06` | Dark mode bg, splash bg |
| `BrandInkMid` | `#1C1109` | Dark gradient mid-stop |
| `BrandInkAccent` | `#2A1A0E` | Dark gradient accent-stop |
| `BrandCardDark` | `#1E1309` | Frosted card bg (login/overlay) |
| `BrandCardStroke` | `#3D2510` | Card border di atas dark surface |
| `SurfaceCream` | `#FAF3E8` | Light mode bg (onboarding, register) |
| `SurfaceCardLight` | `#F0E6D3` | Card/panel di atas cream |
| `SurfaceBorderLight` | `#DDD0BC` | Divider/border di atas light surface |

> ⚠️ LoginScreen dan SplashScreen menggunakan token warm-dark secara langsung (hardcoded private vals),
> bukan `MaterialTheme.colorScheme`, karena mereka selalu gelap terlepas dari dark/light mode sistem.

### Theme.kt — Material3 Mapping

```
MaterialTheme.colorScheme.primary        = BrandGold
MaterialTheme.colorScheme.onPrimary      = BrandInk
MaterialTheme.colorScheme.background     = SurfaceCream (light) / BrandInkDeep (dark)
MaterialTheme.colorScheme.surface        = SurfaceCardLight (light) / BrandCardDark (dark)
MaterialTheme.colorScheme.onBackground   = BrandInk (light) / SurfaceCream (dark)
```

### Typography (Type.kt)

| Font Family | Dipakai untuk |
|---|---|
| `NunitoFontFamily` | **Semua UI text** — labels, titles, buttons, chips. Variable font (wght axis). |
| `CaveatFontFamily` | Default letter body font (handwriting, canvas) |
| `KalamFontFamily` | Alternatif letter font (punya Bold face asli) |
| `ArchitectsDaughterFontFamily` | Alternatif letter font |
| `IndieFlowerFontFamily` | Alternatif letter font |
| `NothingYouCouldDoFontFamily` | Alternatif letter font |
| `ShadowsIntoLightFontFamily` | Alternatif letter font |

### Shared Components — Tabel Wajib

Lihat `docs/component-library.md` untuk API lengkap.

| Komponen | Menggantikan | Dipakai di |
|---|---|---|
| `LetterlyButton` | `Button` Material3 | Semua CTA utama |
| `LetterlyOutlinedButton` | `OutlinedButton` Material3 | Aksi sekunder |
| `LetterlySlider` | `Slider` Material3 | Semua slider |
| `LetterlyMenuRow` | Inline Row styling | Menu items Profile |
| `LetterlyStoreCard` | Inline kartu | Stamps/paper store |
| `LetterlyScreenHeader` | Inline back+title | Sub-screens Profile |
| `AnchoredDropdownMenu` | `DropdownMenu` Material3 | Overflow menus |

**Aturan ekstraksi:** Control UI yang dipakai 2+ tempat → extract ke `ui/components/` + update `docs/component-library.md`.

---

## 9. Motion & Animation Rules

Dokumen lengkap: `docs/motion-rules.md`

### Press feedback — HANYA scale, NO ripple

```kotlin
// ✅ BENAR — semua tappable element wajib pakai ini
val interactionSource = remember { MutableInteractionSource() }
val scale = rememberPressScale(interactionSource)

Modifier
    .graphicsLayer { scaleX = scale; scaleY = scale }
    .clickable(interactionSource = interactionSource, indication = null) { ... }

// ❌ SALAH — ada ripple → dilarang
Modifier.clickable { ... }
```

### Named Spring Presets (LetterlySpring)

| Preset | Dipakai untuk |
|---|---|
| `Snappy` | Tombol, chip, toggle kecil — respons cepat |
| `Bouncy` | FAB, card tap, stamp — playful |
| `Gentle` | Modal, bottom sheet, page transition |
| `Stiff` | Error shake, confirmation pulse |
| `Envelope` | Animasi amplop/kertas — lambat dan berat |

### Reduced motion

```kotlin
val reduced = rememberIsReducedMotion()
val spec = reducedMotionSpring(LetterlySpring.Bouncy, reduced)
// reduced=true → spec menjadi snap() — langsung ke state akhir
```

### Stagger entrance

```kotlin
Modifier.staggerEntrance(index)
// translateX -20dp → 0 + fade, 40ms per item, reduced-motion aware
```

---

## 10. Dependency Injection (Hilt)

### Modul yang ada

| Modul | Scope | Isi |
|---|---|---|
| `NetworkModule` | `SingletonComponent` | Moshi, OkHttpClient, Retrofit |
| `FirebaseModule` | `SingletonComponent` | FirebaseAuth, FirebaseCrashlytics, FirebaseAnalytics |

### Inject Chain

```
FirebaseModule ─── FirebaseAuth, FirebaseCrashlytics, FirebaseAnalytics
                         │                 │                    │
                         ├── AuthInterceptor @Singleton         │
                         │                 │                    │
                         └── AuthRepository @Singleton          └── AnalyticsRepository @Singleton
                                  │                                          │
NetworkModule ─── Retrofit ───────┘                  inject ke ViewModel ───┘
```

### Menambah Feature Baru yang Butuh API

1. Buat `FeatureApi.kt` di `feature/yourfeature/data/`
2. Buat `FeatureRepository.kt` dengan `@Inject constructor(private val retrofit: Retrofit)`
3. Lazy-create API: `private val api by lazy { retrofit.create(FeatureApi::class.java) }`
4. Buat `FeatureViewModel @HiltViewModel` inject repository
5. **Tidak perlu Hilt module baru** — Retrofit sudah singleton

---

## 11. Build Variants

| Flavor | BASE_URL |
|---|---|
| `dev` | `https://unsealed-be-development.up.railway.app/` |
| `prod` | `https://api.unsealed.app/` |

| Build Type | Chucker | Logging |
|---|---|---|
| `*Debug` | ✅ aktif | BODY level |
| `*Release` | ❌ | — |

`BuildConfig.BASE_URL` → dibaca di `NetworkModule.kt`
`BuildConfig.GOOGLE_WEB_CLIENT_ID` → dibaca di `AuthRepository.kt`

---

## 12. Aturan Wajib untuk AI Agent

> Baca dulu sebelum ngerjain apapun di project ini.

### Harus dilakukan ✅

- Tombol → **selalu** `LetterlyButton` / `LetterlyOutlinedButton`
- Press feedback → **selalu** `rememberPressScale` + `indication = null`
- Warna → token dari `Color.kt`, bukan hex hardcode baru
- Slider → **selalu** `LetterlySlider`
- Spring → pakai `LetterlySpring.*`
- Error state → selalu di-handle ke user
- Strings → lewat `strings.xml`
- Komponen yang dipakai 2+ kali → extract ke `ui/components/`
- **Setiap aksi user yang meaningful** (kirim surat, like, claim reward, buka thread, dll) → log ke `AnalyticsRepository.logEvent()` di ViewModel. Gunakan nama event `snake_case`, tambahkan params `bundleOf(...)` untuk context yang berguna.

### Jangan dilakukan ❌

- Jangan import `material3.Button` / `material3.Slider` di layar baru
- Jangan `RoundedCornerShape(...)` inline untuk tombol
- Jangan `.clickable { }` tanpa mematikan ripple indication
- Jangan akses `FirebaseAuth` atau Retrofit langsung di Screen/ViewModel
- Jangan akses `FirebaseAnalytics` langsung — selalu lewat `AnalyticsRepository`
- Jangan hardcode URL baru — pakai `BuildConfig.BASE_URL`
- Jangan ubah `Theme.kt` untuk screen-specific styling

### Pre-commit Checklist 📋

- [ ] Tombol pakai `LetterlyButton`
- [ ] Semua tappable pakai `rememberPressScale` + `indication = null`
- [ ] Warna dari brand tokens
- [ ] Error state di-handle
- [ ] Strings ada di `strings.xml`
- [ ] Komponen 2+ kali pakai → di-extract ke `ui/components/`
- [ ] Aksi user meaningful → `analytics.logEvent(...)` di ViewModel

---

*Unsealed Architecture Guide · Jetpack Compose · Hilt · Retrofit · Firebase Auth*
