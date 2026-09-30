# Setup Project — Unsealed Android

Panduan untuk onboard developer baru: setup flavor, Firebase Auth, dan endpoint tabel.

---

## Build Flavors

Project ini punya dua flavor: `dev` dan `prod`. Switch via **Android Studio → Build > Select Build Variant**.

| Variant | BASE_URL | Kapan dipakai |
|---|---|---|
| `devDebug` | `http://10.0.2.2:8080/` | Local development (emulator ke localhost) |
| `devRelease` | `http://10.0.2.2:8080/` | Test release build ke device fisik (ganti URL di build.gradle.kts) |
| `prodDebug` | `https://api.unsealed.app/` | Test vs. production backend |
| `prodRelease` | `https://api.unsealed.app/` | Production APK/AAB |

> **Device fisik**: `10.0.2.2` hanya jalan di emulator. Kalau test di device fisik, ubah `BASE_URL` flavor `dev` ke IP mesinmu (mis. `http://192.168.1.x:8080/`).

### Cara switch flavor di Android Studio
1. Buka **Build > Select Build Variant** (atau panel **Build Variants** di kiri bawah)
2. Pilih `devDebug` atau `prodDebug`
3. Sync Gradle — `BuildConfig.BASE_URL` dan `BuildConfig.GOOGLE_WEB_CLIENT_ID` otomatis berubah

---

## Firebase Auth Setup

Auth dilakukan sepenuhnya di client via Firebase Authentication. **Backend tidak punya endpoint `/login`**.

### Credential yang sudah ada
`google-services.json` sudah ada di root project. Firebase project: `unsealed` (project number: `642530889152`).

### Google Sign-In (Credential Manager)
- Web OAuth Client ID (digunakan sebagai `serverClientId`):  
  `642530889152-v8q4i551n33sf39h4rchfgpvkh92gv0b.apps.googleusercontent.com`  
  Sudah di-hardcode di `BuildConfig.GOOGLE_WEB_CLIENT_ID` (via `app/build.gradle.kts`).
- SHA-1 fingerprint terdaftar: `4e2f2c0dfea266e2d684a7f0536c483a42c84fe2` (untuk package `com.apps.unsealed`)

### Kalau SHA-1 beda (device/keystore baru)
```bash
# Debug keystore (default Android Studio)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Daftarkan SHA-1 baru di [Firebase Console → Project Settings → Your Apps → Android](https://console.firebase.google.com).

---

## Alur Auth di App

```
App Launch
    └→ SplashScreen (cek Firebase + GET /auth/me)
           ├→ null Firebase user      → LoginScreen (Google Sign-In)
           ├→ 404 dari backend        → RegisterScreen (nickname + continent)
           │       └→ POST /register  → OnboardingFlow (4 langkah)
           ├→ 200 + onboarding kosong → OnboardingFlow langsung
           └→ 200 + onboarding selesai → Main App (PenPals tab)
```

---

## Endpoint Table

Semua endpoint di bawah prefix `/api/v1`. Semua butuh header:
```
Authorization: Bearer <firebase_id_token>
```

| Method | Path | Body | Response | Keterangan |
|--------|------|------|----------|------------|
| `GET` | `/auth/me` | — | `UserDto` | Cek user. `404` = user baru belum register |
| `POST` | `/auth/register` | `{nickname, continent}` | `UserDto` | Registrasi pertama kali |
| `PATCH` | `/auth/me` | `PatchMeRequest` (semua optional) | `UserDto` | Update profil parsial |
| `PUT` | `/auth/me/interests` | `{interest_ids: [...]}` | `UserDto` | Replace seluruh set interest |
| `GET` | `/interests` | — | `{success, data: InterestDto[]}` | Catalog semua interest |
| `POST` | `/storage/presign` | `{file_name, content_type}` | `{upload_url, public_url, key}` | Dapatkan presigned URL untuk upload foto |

### Shape: `UserDto`
```json
{
  "id": "firebase-uid",
  "nickname": "string",
  "continent": "string",
  "is_premium": false,
  "gender": "m" | "f" | null,
  "birthday": "YYYY-MM-DD" | null,
  "language": "id" | "en",
  "photo_url": "https://..." | null,
  "bio": "string (max 300 chars)",
  "interests": [{ "id": "string", "name": "string", "emoji": "string" }]
}
```

### Shape: `PatchMeRequest`
Semua field optional — kirim hanya yang ingin diubah:
```json
{
  "nickname": "optional",
  "continent": "optional",
  "fcm_token": "optional",
  "gender": "m",
  "birthday": "2000-01-31",
  "language": "en",
  "photo_url": "https://...",
  "bio": "up to 300 characters"
}
```

---

## Chucker (HTTP Inspector)

Chucker otomatis aktif di semua **debug** build. Tidak ada setup tambahan.

- **Cara lihat traffic**: buka notification drawer di emulator/device → tap notif Chucker
- **Cara buka manual**: jalankan dari notification bar atau buka dari launcher

Di **release** build, Chucker diganti otomatis dengan `chucker-noop` (no-op) — tidak ada overhead.

---

## Dependency Overview

| Kategori | Library | Versi |
|---|---|---|
| Auth | Firebase Auth (BOM) | 33.8.0 |
| Sign-In | Credential Manager + GoogleId | 1.5.0 / 1.1.1 |
| Network | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| JSON | Moshi + KotlinJsonAdapterFactory | 1.15.2 |
| Inspector | Chucker | 4.1.0 |
| DI | Hilt | 2.60.1 |
