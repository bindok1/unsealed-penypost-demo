# 🎨 Plan & Spesifikasi: Categorized Collectibles Picker
*Letter Paper · Stickers · Envelopes · Stamps*

> **Tujuan**: Menyederhanakan dan meng-upgrade UI pemilih item (Kertas, Stiker, Amplop, Prangko) di [ComposeScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/compose/screen/ComposeScreen.kt) dan [SelectRecipientScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/selectrecipient/screen/SelectRecipientScreen.kt).  
> Mengelompokkan item menjadi **"Koleksi Dasar"** dan **"Koleksi Saya"**, serta menghadirkan **Showcase Preview + Tombol "Beli Koleksi"** bagi pengguna yang belum membeli item agar meningkatkan *conversion & awareness* terhadap **Peny Store**.

---

## 1. Problem Statement & Latar Belakang

1. **Aset Pembelian Belum Terhubung**: Pengguna yang telah membeli paket di Peny Store belum dapat menggunakan kertas, stiker, amplop, atau prangko yang telah dibeli di kanvas surat.
2. **Ketiadaan Pembeda Item**: Tidak ada penanda visual mana item bawaan gratis (*starter items*) dan mana item eksklusif hasil karya kreator.
3. **Peluang Monetisasi Terlewat (Awareness Gap)**: Saat pengguna sedang fokus menulis surat atau menempel perangko di amplop, tidak ada petunjuk visual bahwa mereka bisa mempercantik surat mereka dengan karya kreator di Peny Store.

---

## 2. Konsep UI/UX: Dua Kategori dalam Bottom Sheet

Di setiap Bottom Sheet / Overlay pemilih item, daftar item dibagi menjadi 2 seksi yang jelas:

```
┌────────────────────────────────────────────────────────┐
│                   Pilih Kertas Surat                   │
│                                                        │
│  KOLEKSI DASAR                                         │
│  [ Classic ]  [ Vintage ]  [ Kraft ]  [ Linen ] …      │
│                                                        │
│  ────────────────────────────────────────────────────  │
│                                                        │
│  KOLEKSI SAYA (Karya Kreator)                          │
│                                                        │
│  [Kondisi A: Sudah Punya Item]                         │
│  [ 🌟 Autumn Leaves ]  [ 🌟 Botanical ]  [ + Toko ]    │
│                                                        │
│  [Kondisi B: Belum Punya Item]                         │
│  [ 🔒 Preview 1 ]  [ 🔒 Preview 2 ]  [ 🔒 Preview 3 ]   │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ✨ Percantik suratmu dengan karya ilustrator!    │  │
│  │ [ 🛍️ Beli Koleksi di Toko ]                      │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 3. Detail Perilaku (Behavior Specification)

### A. Kondisi User Sudah Memiliki Koleksi (`inventory.items.isNotEmpty()`)
1. **Header**: Menampilkan title **"Koleksi Saya"** dengan chip jumlah item yang dimiliki (misal: `"3 item"`), dan aksi cepat `Cari Lainnya →` di sisi kanan header yang langsung membuka Peny Store.
2. **Badge Koleksi**:
   - Setiap kartu item memiliki badge kecil atau border aksen emas (`BrandGold`) untuk membedakannya dari koleksi dasar.
   - Nama kreator ditampilkan di bawah nama item.
3. **Trailing Shortcut ke Toko (`FindMoreCollectiblesCard`)**:
   - Di ujung akhir scroll horizontal deretan item terdapat kartu khusus `FindMoreCollectiblesCard` ("Cari Koleksi Lain • Jelajahi Toko") dengan aksen border emas dan ikon tambah/toko, sehingga user selalu bisa menjelajah koleksi baru kapan saja.

### B. Kondisi User BELUM Memiliki Koleksi (`inventory.items.isEmpty()`)
1. **Header Tetap Muncul**: Judul **"Koleksi Saya"** atau **"Koleksi Kreator"** **TIDAK DISEMBUNYIKAN**.
2. **Showcase Teaser (Preview)**:
   - Menampilkan 3-4 thumbnail karya terpopuler/terbaru dari Peny Store dalam mode preview (sedikit semi-transparan dengan badge ikon gembok halus `Icons.Default.Lock` atau bintang `Icons.Default.Star`).
   - Jika pengguna mengetuk salah satu item preview ini, langsung membuka Peny Store.
3. **CTA Banner "Beli Koleksi"**:
   - Diletakkan tepat di bawah baris preview.
   - Desain frosted glass gelap dengan border aksen emas halus.
   - Tombol utama: **"Beli Koleksi" / "Lihat di Toko" 🛍️**.
   - **Directing ke Toko**: Mengarahkan pengguna langsung ke tab Peny Store (`Destinations.Stamps`).

---

## 3.1. Post-Purchase Celebration Modal (`StorePurchaseCelebrationModal.kt`)

Setelah user berhasil membeli paket koleksi di Peny Store (melalui Google Play / RevenueCat), aplikasi menampilkan modal perayaan hangat (*stationery-themed celebration modal*):
- **Efek Konfetti**: Pancaran konfetti warna-warni bernuansa emas, peach, lavender, dan rose via `KonfettiView`.
- **Animasi Ambien**: Efek *floating* halus dan *pulsing glow* pada kartu item koleksi.
- **Copywriting Hangat (Bilingual)**:
  - *ID*: *"Sentuhan Indah untuk Suratmu — Terima kasih telah mengapresiasi karya kreator! Koleksi istimewa ini kini tersimpan rapi di laci meja tulismu, siap menemani setiap cerita tulus yang kamu kirimkan ke sahabat pena."*
  - *EN*: *"A Lovely Addition to Your Desk — Thank you for supporting this creator! This handcrafted piece is now tucked safely in your desk drawer, ready to accompany your heartfelt letters to penpals near and far."*
- **Preview Item**: Menampilkan preview produk yang dibeli beserta nama kreator dan rincian aset di dalamnya.
- **Tombol Aksi**: Tombol *"Simpan ke Meja Tulis"* dan *"Lihat Koleksi Lainnya"*.

---

## 4. Rincian Pemilih Item per Layar

### 1. `PaperPickerSheet.kt` ([ComposeScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/compose/screen/ComposeScreen.kt))
* **Fungsi**: Memilih kertas latar belakang kanvas surat.
* **Koleksi Dasar**: 8 template klasik yang sudah ada (`PaperTemplate.kt` — Classic, Vintage, Kraft, dll.).
* **Koleksi Saya**: Kertas surat bertekstur/bermotif dari paket yang dibeli (`asset_type == "PAPER"`).
* **CTA Toko**: Pre-filter `StoreFilterCategory.PAPER`.

### 2. `StickerPickerSheet.kt` ([ComposeScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/compose/screen/ComposeScreen.kt) & [SelectRecipientScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/selectrecipient/screen/SelectRecipientScreen.kt))
* **Fungsi**: Menempel stiker dekorasi pada kertas surat atau bagian luar amplop.
* **Koleksi Dasar**: Paket stiker bawaan Peny (emotikon, stempel lilin dasar, bunga mini).
* **Koleksi Saya**: Stiker ilustrasi eksklusif dari paket kreator (`asset_type == "STICKER"`).
* **CTA Toko**: Pre-filter `StoreFilterCategory.STICKER`.

### 3. `EnvelopePickerSheet.kt` ([SelectRecipientScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/selectrecipient/screen/SelectRecipientScreen.kt))
* **Fungsi**: Memilih warna dan motif amplop luar.
* **Koleksi Dasar**: Warna amplop solid bawaan (Manila, Olive, Rose, Navy, dll.).
* **Koleksi Saya**: Amplop berpola dan ilustratif dari paket kreator (`asset_type == "ENVELOPE"`).
* **CTA Toko**: Pre-filter `StoreFilterCategory.ENVELOPE`.

### 4. `StampPickerOverlay.kt` ([SelectRecipientScreen.kt](file:///Users/niozihni/AndroidStudioProjects/unsealed/app/src/main/java/com/apps/unsealed/ui/screens/selectrecipient/screen/SelectRecipientScreen.kt))
* **Fungsi**: Menempel prangko di pojok kanan atas amplop sebelum dikirim.
* **Koleksi Dasar**: Prangko standar pos (6 desain vector bawaan).
* **Koleksi Saya**: Prangko unik bertepi gerigi dari kreator (`asset_type == "STAMP"`).
* **CTA Toko**: Pre-filter `StoreFilterCategory.STAMP`.

---

## 5. Model Data & Integrasi Backend

### Endpoint Sumber Data:
```http
GET /api/v1/me/inventory
Authorization: Bearer <firebase_id_token>
```

### Model UI Komposabel (`CollectibleItemUi`):
```kotlin
sealed interface CollectibleItemUi {
    val id: String
    val title: String
    val previewUrl: String
    val isOwned: Boolean

    data class DefaultAsset(
        override val id: String,
        override val title: String,
        override val previewUrl: String,
        val localDrawableRes: Int? = null,
    ) : CollectibleItemUi {
        override val isOwned: Boolean = true
    }

    data class CreatorAsset(
        override val id: String,
        override val title: String,
        override val previewUrl: String,
        val creatorName: String,
        val packTitle: String,
        override val isOwned: Boolean,
        val priceIdr: Long = 0,
    ) : CollectibleItemUi
}
```

---

## 6. Roadmap & Tahapan Eksekusi

```
Phase 1: Shared Models & Inventory Fetching
 ├── Buat InventoryRepository & DTO (/api/v1/me/inventory)
 └── Inject ke ComposeViewModel & SelectRecipientViewModel

Phase 2: Komponen UI Shared (CollectiblePickerSection)
 ├── SectionHeader ("Koleksi Dasar" / "Koleksi Saya")
 ├── CollectibleItemCard (support local drawable & network Coil)
 └── CollectibleEmptyShowcaseBanner ("Beli Koleksi" CTA + Smart Directing)

Phase 3: Refactor Pickers di ComposeScreen
 ├── Update PaperPickerSheet.kt
 └── Update StickerPickerSheet.kt

Phase 4: Refactor Pickers di SelectRecipientScreen
 ├── Update EnvelopePickerSheet.kt
 └── Update StampPickerOverlay.kt

Phase 5: Navigasi Antar Screen
 └── Callback onNavigateToStore(category) terhubung ke NavHost & StampsScreen
```
