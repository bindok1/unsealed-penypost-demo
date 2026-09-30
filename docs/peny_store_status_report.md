# 🏪 Peny Store (Creator Marketplace) — Milestone Implementation Report

> **Tanggal**: 26 September 2026  
> **Status**: Feature Implemented & Verified (CRUD, UI, Navigation, Optimistic Likes, Bug Fixes)  
> **Pending**: End-to-End Google Play Sandbox Payment Verification  

---

## 1. Ringkasan Eksekutif

Layar **Stamps Screen** telah berhasil ditransformasikan menjadi **Peny Store** — sebuah etalase pasar kreator (*Creator Marketplace*) digital dengan nuansa kantor pos artisan. Fitur ini dirancang sebagai etalase utama (tab pertama & default) tempat pengguna dapat menemukan, mengoleksi, dan membeli karya ilustrator independen (berupa prangko, kertas surat, stiker, amplop, dan bundel).

---

## 2. Arsitektur & Komponen yang Telah Selesai Dibangun

### A. Data Layer (`com.apps.unsealed.feature.store.data`)
* **`StoreApi.kt`**:
  * `GET /api/v1/shop/showcase` — Banner promosi featured/editorial.
  * `GET /api/v1/items` — Katalog produk dengan filter kategori & cursor pagination.
  * `GET /api/v1/items/:id` — Detail produk lengkap (preview aset, kreator, status kepemilikan).
  * `POST /api/v1/items/:id/like` — Toggle suka/batal suka.
  * `POST /api/v1/items/:id/view` — Non-blocking increment counter tampilan item.
* **`StoreDto.kt`**: Model DTO Moshi (`ShowcaseResponseDto`, `CatalogResponseDto`, `ItemDetailDto`, `PublicCreatorDto`, `ItemAssetDto`, `LikeToggleDto`).
* **`StoreRepository.kt`**: Repository dengan wrapper `AuthResult<T>` dan penanganan error terisolasi.

### B. State & ViewModel (`com.apps.unsealed.ui.screens.stamps`)
* **`StoreUiState.kt`**:
  * Menampung state katalog, showcase banner, filter terpilih, cursor pagination, status pembelian IAP (`isPurchasing`, `purchasingItemId`), dan item detail aktif.
* **`StampsViewModel.kt`**:
  * Default tab diset ke `StampsTab.STORE`.
  * **Optimistic Like Toggle**: Pembaruan instan pada UI saat icon hati ditekan, dengan rollback otomatis bila koneksi gagal.
  * **Keyset Pagination**: Deteksi scroll hingga item terakhir untuk memuat batch katalog berikutnya (`next_cursor`).
  * **RevenueCat Mapping**: Mengonversi `tier` backend (`tier_1` … `tier_5`) ke Product ID RevenueCat (`creator_pack_tier_1` … `creator_pack_tier_5`).
  * Refetch detail produk pasca-pembelian untuk mengonfirmasi status kepemilikan `viewerOwns = true`.

### C. Komponen UI (`com.apps.unsealed.ui.screens.stamps.widgets`)
* **`PenyStoreSection.kt`**:
  * Container utama berlatar belakang tekstur kayu pos (`shop_bg.webp`) dengan scrim gelap kontras tinggi.
  * Integrasi *Showcase Banner Carousel*, *Store Filter Chips*, dan 2-kolom *Catalog Grid*.
* **`ShowcaseBannerCarousel.kt`**:
  * Carousel banner otomatis & manual swipe dengan dot indicator dan border aksen emas (`BrandGold`).
  * Tap banner langsung membuka sheet detail produk terkait.
* **`StoreFilterChips.kt` & `StoreFilterCategory.kt`**:
  * Baris chip filter horizontal: *Semua*, *Prangko*, *Kertas*, *Stiker*, *Amplop*, *Bundle*.
* **`ProductCard.kt`**:
  * Kartu produk 2-kolom dengan thumbnail Coil, nama kreator, judul paket, harga IDR terformat, dan tombol suka interaktif.
* **`ProductDetailBottomSheet.kt`**:
  * Pager aset artwork resolusi penuh (520dp) dengan badge tipe aset (*Prangko 1/4*, dll.).
  * Rincian isi paket (badge jumlah prangko/kertas/stiker/amplop).
  * Tombol CTA dinamis: **"Beli — RpX"** (dengan loading spinner) atau **"Sudah Dimiliki ✓"** (disabled).
  * **Kartu Kreator**: Avatar, nama, bio, dan tombol eksternal dengan feedback ripple penuh.
* **`StoreSkeletonSection.kt`**:
  * Shimmer loading placeholder saat memuat banner dan katalog produk.

---

## 3. Bug Fixes & Penguatan Sistem (Hardening)

* **Fix `ActivityNotFoundException` saat Klik Profil Kreator**:
  * **Masalah**: Tautan eksternal yang tidak memiliki skema (misal `instagram.com/user` atau `@user`) memicu crash fatal Android `ActivityNotFoundException`.
  * **Solusi**: Diterapkan fungsi sanitasi dan normalisasi URL `openCreatorLink(...)` yang otomatis menambahkan skema `https://` atau memformat `@handle`, dibungkus dalam blok `try-catch`, dan memunculkan toast ramah pengguna jika tidak ada aplikasi browser yang tersedia.
  * **UX Upgrade**: Seluruh area kartu profil kreator (`Surface`) kini dapat diklik, memberikan kemudahan akses bagi pengguna.
* **Klarifikasi Logcat `ProfileInstaller`**:
  * Mengonfirmasi bahwa log `Failed to open file ... baseline.prof` adalah proses instalasi runtime ART Android debug dan bukan bug pada data profil pengguna.

---

## 4. Status Pengujian Saat Ini

| Area Pengujian | Status | Catatan |
|---|---|---|
| **CRUD Katalog & Detail** | ✅ Lulus | Showcase, listing katalog, detail bottom sheet berjalan mulus. |
| **Formatting Harga & Currency** | ✅ Lulus | Terformat rapi dalam IDR (`Rp35.000`) dan USD fallback. |
| **Optimistic Likes & Rollback** | ✅ Lulus | Ikon hati berubah instan dan rollback saat offline. |
| **Navigasi Profil Kreator** | ✅ Lulus | Tautan URL aman tanpa risiko crash. |
| **Kompilasi Kotlin (`compileDevDebugKotlin`)** | ✅ Lulus | Build berhasil 100% tanpa error kompilasi. |
| **IAP / Transaksi Pembayaran** | ⏳ Menunggu Tes | Siap diuji via Google Play License Tester Sandbox. |

---

## 5. Rencana Langkah Berikutnya (Next Steps)

1. **Pengujian IAP di Sandbox**:
   * Melakukan test purchase dengan akun Google Play License Tester untuk memvalidasi alur dari Google Play Billing &rarr; Webhook RevenueCat &rarr; Pembaruan `viewerOwns` di backend.
2. **Penyederhanaan & Integrasi UI Pemilih Aset (Letter Composer & Select Recipient)**:
   * Menghubungkan aset yang telah dibeli ke dalam *Letter Canvas* dan *Envelope Screen*.
   * Mengatur kategori pemilih (*"Koleksi Dasar"* vs *"Koleksi Saya"*).
   * Menampilkan preview showcase koleksi berbayar dengan CTA **"Beli Koleksi"** bagi pengguna yang belum membeli, guna meningkatkan *awareness* terhadap Peny Store.
