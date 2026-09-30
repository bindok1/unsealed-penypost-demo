# Unsealed (PenyPost) — Android App 💌

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM-green.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI%2FMVVM-orange.svg)](https://developer.android.com/topic/architecture)
[![DI](https://img.shields.io/badge/DI-Dagger%20Hilt-red.svg)](https://dagger.dev/hilt/)
[![MinSdk](https://img.shields.io/badge/MinSdk-24%20(Android%207.0)-lightgrey.svg)](https://developer.android.com)
[![TargetSdk](https://img.shields.io/badge/TargetSdk-36%20(Android%2015)-brightgreen.svg)](https://developer.android.com)

**Unsealed (PenyPost)** adalah aplikasi surat-menyurat digital kontemporer (*epistolary slow messaging*) yang dirancang untuk menghidupkan kembali kehangatan bertukar surat antar sahabat pena (*penpals*) di seluruh dunia. Waktu pengiriman disimulasikan secara realistis berdasarkan jarak geografis, dilengkapi dengan koleksi prangko, amplop tematik, kertas surat yang dapat digambari secara bebas, serta pelacakan kurir interaktif.

---

## 🏛️ Catatan Arsitektur (Architecture Notes)

Proyek ini dibangun mengikuti prinsip **Clean Architecture** dengan pola presentasi **MVI / MVVM (Model-View-ViewModel / Model-View-Intent)** yang menerapkan prinsip **Unidirectional Data Flow (UDF)** secara konsisten.

```
app/src/main/java/com/apps/unsealed/
├── core/                  # Core modules & infrastructural capabilities
│   ├── analytics/         # User behavior tracking & event logging
│   ├── database/          # Local persistence (Room Database & DAOs)
│   ├── data/              # Preferences DataStore & shared repositories
│   ├── messaging/         # Firebase Cloud Messaging & push notifications
│   ├── model/             # Shared domain entities & data contracts
│   ├── network/           # Retrofit, OkHttp interceptors, Auth token refresher
│   ├── reminder/          # WorkManager daily background workers
│   └── util/              # Great-Circle math, Remote Config keys, UI extensions
├── feature/               # Feature-scoped business logic & repositories
│   ├── auth/              # Authentication & user profile state management
│   ├── letters/           # Letter exchange, drafts, delivery scheduling
│   ├── mailbox/           # Inbound/outbound letter feeds
│   ├── penpals/           # Discovery, matching, and friendship recommendations
│   ├── storage/           # Presigned Cloudflare R2 upload pipelines
│   └── store/             # Collectibles catalog (stamps, papers, envelopes)
├── navigation/            # Compose Navigation graph & deep linking routes
└── ui/                    # 100% Jetpack Compose presentation layer
    ├── components/        # Reusable design system widgets, dialogs, buttons
    ├── screens/           # Feature screens (Auth, Inbox, Compose, Tracking, Profile)
    └── theme/             # Material 3 typography, color palette, paper templates
```

### 1. Presentation Layer (100% Jetpack Compose)
- **State-Driven UI**: Setiap screen diatur oleh `ViewModel` yang mengekspos `StateFlow<UiState>` immutable, memisahkan secara tegas antara UI rendering dan business logic.
- **Custom Canvas Graphics & Gestures**:
  - `AnnotateCanvas.kt`: Engine kanvas gambar/sketsa tangan bebas di atas kertas surat dengan dukungan stroke smoothing, eraser, dan multi-color picker.
  - `LoginAtmosphericSparklesCanvas.kt`: Efek partikel melayang (*atmospheric sparkle physics*) dan cap lilin berdenyut (*pulsing wax seal*) yang dirender langsung via Compose `Canvas`.
- **Epistolary Route Map & Earth Curvature Math**:
  - `DeliveryRouteMap.kt`: Visualisasi rute pengiriman surat global menggunakan MapLibre GeoJSON. Menghitung kurva lengkung bumi (*Great-Circle calculation*) tanpa bergantung pada tile server berbayar.
- **Dynamic Localization (i18n)**:
  - Mendukung pergantian bahasa dinamis (*English & Indonesian*) secara langsung di dalam aplikasi melalui `AppCompatDelegate.setApplicationLocales`.

### 2. Domain & Data Layer
- **Network & Offline Caching**:
  - Retrofit 2 + OkHttp 4 dengan `AuthInterceptor` untuk otomatisasi Bearer Token Firebase ID.
  - `TokenRefreshAuthenticator`: Mekanisme self-healing untuk memperbarui Firebase Token yang kedaluwarsa secara transparan saat response HTTP `401 Unauthorized`.
  - Moshi dengan Kotlin code generation (`moshi-kotlin-codegen`) untuk deserialisasi JSON berkinerja tinggi.
  - Room Database sebagai single source of truth untuk caching surat lokal dan draft offline.
- **Dependency Injection**:
  - Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Singleton`) dengan konfigurasi modular.
  - `@AssistedInject` untuk `HiltWorkerFactory` pada integrasi WorkManager.
- **Background Tasks & Reliability**:
  - AndroidX WorkManager untuk menjadwalkan notifikasi pengingat surat belum dibaca harian (*idempotent background scheduler*).
  - Chucker HTTP Inspector otomatis aktif pada build `devDebug` untuk mempermudah audit network traffic.

---

## 🛠️ Tech Stack & Libraries

| Kategori | Teknologi / Pustaka |
|---|---|
| **Bahasa & Runtime** | Kotlin 2.0.x, Kotlin Coroutines, Kotlin Flow |
| **UI Framework** | Jetpack Compose BOM, Material 3, Navigation Compose |
| **Media & Image Loading** | Coil 3 (OkHttp network fetcher, disk cache) |
| **Arsitektur & DI** | Android Architecture Components (ViewModel, Lifecycle), Dagger Hilt |
| **Networking** | Retrofit 2, OkHttp 4, Moshi JSON |
| **Database & Cache** | Room Database, Preferences DataStore |
| **Background Processing** | WorkManager, Hilt Work |
| **Peta & Visualisasi** | MapLibre Native SDK, Custom GeoJSON Great-Circle Renderer |
| **Animasi & Efek** | Compose Animation, Konfetti, Lottie Compose |
| **Authentication** | Firebase Authentication, Google Credential Manager |
| **HTTP Debugger** | Chucker Inspector (debug variant only) |

---

## 🚀 Instruksi Kompilasi & Menjalankan Aplikasi (Build Instructions)

### Prasyarat (Prerequisites)
1. **Android Studio**: Android Studio Koala / Ladybug / Meerkat (atau versi yang lebih baru).
2. **JDK**: Java Development Kit (JDK) versi 17 atau 21.
3. **Android SDK**: Compile SDK `36` (Android 15), Min SDK `24` (Android 7.0 Nougat).

### Langkah-langkah Setup:

#### 1. Clone Repositori
```bash
git clone https://github.com/bindok1/unsealed-penypost-demo.git
cd unsealed-penypost-demo
```

#### 2. Konfigurasi Firebase (`google-services.json`)
Aplikasi membutuhkan konfigurasi Firebase client untuk kompilasi plugin Google Services:
1. Salin template yang telah disediakan:
   ```bash
   cp app/google-services.json.example app/google-services.json
   ```
2. *(Opsional)* Jika Anda memiliki file `google-services.json` dari project Firebase Anda sendiri atau project demo yang diberikan, letakkan langsung di dalam direktori `app/`.

#### 3. Pilih Build Variant
1. Buka project di **Android Studio**.
2. Tunggu proses **Gradle Sync** selesai.
3. Buka tab **Build Variants** (di bilah kiri bawah Android Studio).
4. Pastikan varian yang dipilih adalah **`devDebug`**.
   - *Catatan: Varian `devDebug` secara otomatis terhubung ke development backend yang aman (`https://unsealed-be-development.up.railway.app/`).*

#### 4. Jalankan Aplikasi
* Hubungkan perangkat fisik Android (aktifkan USB Debugging) atau jalankan Android Emulator (rekomendasi: API 30+).
* Klik tombol **Run 'app'** (`Shift + F10`) di Android Studio.

---

## 🔑 Catatan Pengujian & Otentikasi (Testing Guide)

* **Otentikasi Email & Password**:
  Pada layar Login, Anda dapat langsung mengklik opsi **Email** untuk mendaftar akun baru (*Sign Up*) atau masuk (*Sign In*).
  > *Catatan: Fitur Google Sign-In (Credential Manager) membutuhkan pendaftaran fingerprint SHA-1 keystore lokal di Firebase Console. Oleh karena itu, untuk proses review atau demo di mesin lokal baru, gunakan metode **Email & Password Authentication** yang dapat langsung berfungsi tanpa konfigurasi keystore tambahan.*

---

## 📄 Lisensi & Hak Cipta
Dibuat untuk keperluan portofolio dan demonstrasi kapabilitas rekayasa perangkat lunak mobile (Android). Hak cipta dilindungi.
