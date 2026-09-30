# App Update Rules & Operation Guide

*Convention & Operation doc — panduan cara kerja, arsitektur, dan operasional pembaruan aplikasi (In-App Update) via Firebase Remote Config.*

---

## 1. Prinsip Utama: Real-Time Saat Aplikasi Sedang Dibuka / Dimainkan

PenyPost menggunakan **Firebase Remote Config Real-Time Updates** (`addOnConfigUpdateListener`). 

Artinya:
* **Tidak perlu menunggu user menutup dan membuka ulang aplikasi.**
* Ketika admin menaikkan versi di Firebase Console dan menekan **Publish Changes**, aplikasi yang **sedang aktif digunakan user** (misal sedang membaca surat, menulis pesan, atau melihat toko) akan menerima event notifikasi detik itu juga secara otomatis.
* Begitu event diterima, `AppUpdateViewModel` langsung memicu aktivasi config baru (`remoteConfig.activate()`) dan menampilkan dialog pembaruan di layar yang sedang aktif.

---

## 2. Dua Tingkat Pembaruan (Update Modes)

Sistem membedakan dua tingkat urgensi pembaruan:

| Mode | Kapan Digunakan | Syarat Versi | Perilaku UI |
|---|---|---|---|
| **Soft Update (Opsional / Santai)** | Fitur baru, stiker baru, perbaikan minor tampilan. | `min_required_version_code <= currentVersion < latest_version_code` | Tampil dialog ramah dengan tombol **"Perbarui Sekarang"** dan **"Nanti Saja"**. User bisa menutup dialog dan tetap memakai aplikasi untuk sesi tersebut. |
| **Force Update (Wajib / Darurat)** | Breaking API changes, migrasi database, security patch, bug fatal yang merusak data surat. | `currentVersion < min_required_version_code` | Tampil dialog **terkunci** (`isDismissable = false`). Tombol "Nanti Saja" disembunyikan. Menekan tombol back atau mengetuk area luar dialog **diabaikan**. User wajib update ke Play Store agar bisa melanjutkan. |

---

## 3. Aturan Versi (`versionCode` vs `versionName`)

Pemeriksaan versi **SELALU** membandingkan angka integer `versionCode` dari `app/build.gradle.kts`, **BUKAN** `versionName`.

* **`versionCode`** (contoh: `25`): Angka bulat yang selalu naik di setiap build/rilis baru. Angka ini yang dibaca oleh sistem Android, Play Store, dan Remote Config.
* **`versionName`** (contoh: `"1.0.24"`): Hanya string representasi visual untuk manusia di menu profil/tentang aplikasi.

```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 25       // <--- Angka ini yang dibandingkan
    versionName = "1.0.24" // <--- Jangan bandingkan string ini
}
```

---

## 4. Kunci Konfigurasi Firebase (Remote Config Keys)

Didefinisikan di `com.apps.unsealed.core.util.RemoteConfigKeys`:

| Key di Firebase Console | Tipe | Fallback Default | Deskripsi |
|---|---|---|---|
| `min_required_version_code` | Number / String | `BuildConfig.VERSION_CODE` | Batas minimum `versionCode` yang diizinkan untuk membuka app. Jika versi user di bawah angka ini, dialog **Force Update** muncul. |
| `latest_version_code` | Number / String | `BuildConfig.VERSION_CODE` | `versionCode` versi rilis terbaru di store. Jika versi user di bawah angka ini namun masih $\ge$ `min_required_version_code`, dialog **Soft Update** muncul. |
| `update_play_store_url` | String | URL Play Store PenyPost | URL deep-link atau web store yang dibuka saat user menekan tombol update. |

> **Prinsip Fallback Aman:** Local fallback default diisi dengan nilai `BuildConfig.VERSION_CODE` milik aplikasi itu sendiri. Ini menjamin saat app baru pertama kali diinstal, sedang offline, atau Firebase belum ter-fetch, **tidak akan pernah muncul dialog update palsu / keliru**.

---

## 5. Arsitektur Komponen

1. **`AppUpdateViewModel`** (`com.apps.unsealed.core.update`):
   - Mendaftarkan `addOnConfigUpdateListener` ke Firebase Remote Config untuk mendengarkan perubahan secara real-time.
   - Mengintegrasikan `com.google.android.play:app-update-ktx` (`AppUpdateManager`):
     - Memeriksa ketersediaan update Google Play via `appUpdateManager.appUpdateInfo`.
     - Menjalankan **Flexible Update** (`AppUpdateType.FLEXIBLE`) untuk background download, atau **Immediate Update** (`AppUpdateType.IMMEDIATE`) untuk force update.
     - Memantau proses download via `InstallStateUpdatedListener`. Saat status `InstallStatus.DOWNLOADED`, state `isUpdateDownloaded` menjadi `true`.
     - Menyediakan fungsi `completeUpdate()` untuk me-restart dan menginstal update yang sudah diunduh di background.
   - Jika Google Play API tidak tersedia (misal di build debug / sideloaded APK), otomatis fallback memanggil `openStore()` untuk membuka halaman Play Store via `market://details?id=...`.
2. **`AppUpdateDialog`** (`com.apps.unsealed.ui.components`):
   - Dibangun di atas `LetterlyCenterDialog` sehingga visualnya 100% konsisten dengan desain sistem PenyPost (maskot Peny 🐧, rounded cards, spring animations, warna `BrandGold`).
   - Menerima parameter `isMandatory: Boolean` dan `closeOnPrimaryClick: Boolean` untuk mengunci/membuka dismissability dialog.
3. **`AppUpdateDownloadedBanner`** (`com.apps.unsealed.ui.components`):
   - Banner melayang di bagian bawah layar yang muncul saat download background Google Play selesai.
   - Memiliki tombol **"Mulai Ulang" / "Restart"** untuk menerapkan pembaruan tanpa repot buka Play Store manual.
4. **`MainActivity`** (`com.apps.unsealed.MainActivity`):
   - Meng-observe `appUpdateViewModel.updateState` dan `isUpdateDownloaded` di level root Compose (`UnsealedApp`).
   - Memeriksa update status pada `LifecycleResumeEffect` (`onResumeCheck`).

---

## 6. Aturan String Resources (Localization)

Sesuai `docs/string-resources.md`, seluruh teks dialog dan banner **tidak boleh di-hardcode** dan wajib terdaftar di ketiga file locale:

* `app/src/main/res/values/strings.xml` (Default English)
* `app/src/main/res/values-id/strings.xml` (Indonesian)
* `app/src/main/res/values-in/strings.xml` (Indonesian legacy)

Daftar string yang digunakan:

| String Key | English (`values`) | Indonesian (`values-id` & `values-in`) |
|---|---|---|
| `app_update_mandatory_title` | Update Required | Pembaruan Diperlukan |
| `app_update_mandatory_body` | A newer version of PenyPost is required to keep exchanging letters. Please update now to continue your journey! | Versi baru PenyPost diperlukan agar pengiriman surat tetap lancar. Silakan perbarui sekarang untuk melanjutkan perjalananmu! |
| `app_update_optional_title` | New Update Available! 🐧 | Pembaruan Tersedia! 🐧 |
| `app_update_optional_body` | A fresh version of PenyPost is here with lovely new features and improvements. Would you like to update now? | Versi terbaru PenyPost sudah hadir dengan fitur dan perbaikan baru. Mau perbarui sekarang? |
| `app_update_cta` | Update Now | Perbarui Sekarang |
| `app_update_later` | Later | Nanti Saja |
| `app_update_downloaded_message` | Update downloaded and ready to install! | Pembaruan telah selesai diunduh dan siap dipasang! |
| `app_update_downloaded_action` | Restart | Mulai Ulang |

---

## 7. Panduan Operasional di Firebase Console

### Skenario A: Rilis Versi Baru Biasa (Flexible Background Download)
Misal versi di Play Store sudah naik ke `versionCode = 26`, sedangkan versi lama `25` masih aman digunakan:
1. Buka **Firebase Console** $\rightarrow$ **Remote Config**.
2. Ubah `latest_version_code` menjadi `26`.
3. Biarkan `min_required_version_code` tetap `25` (atau versi lama lainnya).
4. Klik **Publish Changes**.
5. 👉 User yang sedang membuka app versi 25 akan langsung melihat dialog *"Pembaruan Tersedia! 🐧"*. Saat user menekan *"Perbarui Sekarang"*, Google Play akan **mengunduh update di latar belakang** selagi user tetap bisa membaca/menulis surat. Saat selesai, banner *"Pembaruan telah selesai diunduh"* muncul dengan tombol *"Mulai Ulang"*.

### Skenario B: Rilis Urgent / Breaking Changes (Wajib Update / Force Update)
Misal ada perubahan fatal pada endpoint backend atau format surat di versi `26`:
1. Buka **Firebase Console** $\rightarrow$ **Remote Config**.
2. Ubah `min_required_version_code` menjadi `26`.
3. Pastikan `latest_version_code` juga `26`.
4. Klik **Publish Changes**.
5. 👉 User di versi $< 26$ yang sedang memainkan aplikasi seketika akan diblokir oleh dialog *"Pembaruan Diperlukan"*, tidak bisa di-cancel sampai mereka menekan tombol untuk update.

