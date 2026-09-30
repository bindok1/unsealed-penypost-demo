# 📄 Integrasi Paper URL & Optimasi Device Capability Low-Density

> **Tanggal**: 26 September 2026  
> **Status**: Selesai & Terverifikasi (`BUILD SUCCESSFUL`, teruji di perangkat fisik Redmi 9A)  
> **Branch**: `feat/wizzard-compose`

---

## 1. Latar Belakang & Masalah

1. **Tombol "Beli" Showcase Carousel Tidak Merespons**:
   - Di `ShowcaseBannerCarousel.kt`, `PriceTag` tidak memiliki callback `onClick`, sehingga pengguna yang menekan harga/tombol beli di banner promosi Peny Store tidak mendapatkan respons apa pun.

2. **Gating Kerapatan Layar (Density) Terlalu Agresif**:
   - `CompositingCapability.kt` sebelumnya menetapkan `MinCompositingDensity = 2.5f`.
   - Akibatnya, jutaan perangkat Android entry-level populer (seperti seri Redmi 9A/A3, resolusi 720p dengan density ~2.0f) dipaksa masuk ke mode "perangkat terbatas". Fitur stiker/gambar di compose terkunci, surat tidak di-composite, dan hanya mengirimkan teks mentah.
   - Setelah diuji langsung di perangkat fisik Redmi 9A, gambar surat hasil compositing terbukti **sangat tajam, tidak pecah, dan terbaca dengan jelas**.

3. **Tekstur Kertas Acak pada Surat Non-Composite (PenPals Feed)**:
   - Pada surat tanpa composite image di `OpenLetterOverlay.kt`, background kertas dipilih menggunakan `TextModePaperTemplates.random()`. Hal ini menyebabkan latar belakang surat berganti-ganti setiap kali surat yang sama ditutup dan dibuka kembali.

4. **Ketiadaan Field `paper_url`**:
   - Berbeda dengan amplop dan prangko yang memiliki URL dan ID katalog dinamis, kertas surat sebelumnya hanya disimpan sebagai metadata enum lokal (`paper_template`).
   - Jika pengguna membeli kertas eksklusif dari Peny Store atau jika surat dibuka dalam mode teks fallback, penerima tidak bisa melihat kertas asli pengirim.

---

## 2. Rincian Solusi & Implementasi

### A. Perbaikan Interaksi Showcase Carousel
- **File**: `app/src/main/java/com/apps/unsealed/ui/screens/stamps/widgets/ShowcaseBannerCarousel.kt`
- Menambahkan parameter `onBuyClick: (ShowcaseItemUi) -> Unit`, `isPurchasing: Boolean`, dan `purchasingItemId: String?`.
- Mengaktifkan `onClick = { onBuyClick(currentItem) }` pada `PriceTag` dengan visual loading spinner saat item tersebut sedang diproses.
- Auto-advance carousel otomatis dijeda (*paused*) saat proses transaksi berlangsung.
- Disambungkan melalui `PenyStoreSection.kt` ke `StampsScreen.kt` yang memanggil `initiateStorePurchase(item.id, item.tier)`.

---

### B. Penyesuaian `MinCompositingDensity` & Hashing Deterministik
1. **Penurunan Ambang Batas Compositing**:
   - **File**: `app/src/main/java/com/apps/unsealed/core/util/CompositingCapability.kt`
   - Mengubah `MinCompositingDensity` dari `2.5f` menjadi **`1.7f`**.
   - Perangkat 720p/HD+ kini dapat menikmati fitur stiker, penyisipan gambar, dan compositing penuh tanpa degradasi visual.

2. **Kertas Deterministik di Feed Penpals**:
   - **File**: `app/src/main/java/com/apps/unsealed/ui/screens/penpals/widgets/OpenLetterOverlay.kt`
   - Mengganti `TextModePaperTemplates.random()` dengan hashing berbasis `letter.id`:
     ```kotlin
     val templateIndex = (letter.id.hashCode() and Int.MAX_VALUE) % TextModePaperTemplates.size
     TextModePaperTemplates[templateIndex]
     ```
   - Latar belakang surat kini konsisten setiap kali dibuka.

---

### C. Arsitektur End-to-End Field `paper_url`

#### 1. Kontrak Backend (Telah Diterapkan oleh Tim BE)
- **Migration**: `041_letters_paper_url.sql`:
  ```sql
  ALTER TABLE letters ADD COLUMN IF NOT EXISTS paper_url TEXT NULL;
  ```
- **Entity & Repository**: Struct `Letter` ditambahkan field `PaperURL *string db:"paper_url"`.
- **Endpoints**:
  - `POST /api/v1/letters`: Menerima opsional `paper_url` (JSON: `paper_url`).
  - `GET /api/v1/penpals/feed`: Mengembalikan `paper_url` di setiap item.
  - `GET /api/v1/letters/{id}`: Mengembalikan `paper_url` di respon detail.

#### 2. Implementasi Android Client

| Layer | File Terkait | Rincian Perubahan |
|---|---|---|
| **Data (DTO)** | `feature/penpals/data/PenpalsDto.kt`<br>`feature/letters/data/LettersDto.kt`<br>`feature/mailbox/data/MailboxDto.kt` | Menambahkan `@Json(name = "paper_url") val paperUrl: String? = null` pada `FeedItemDto`, `SendLetterRequest`, `LetterDetailDto`, dan `MailboxLetterDto`. |
| **Domain / State** | `ui/screens/penpals/state/PenpalLetter.kt`<br>`ui/screens/inbox/state/ThreadLetterItem.kt`<br>`ui/screens/inbox/state/MailboxThreadMapping.kt` | Menambahkan `val paperUrl: String? = null` dan memetakannya dari DTO ke UI state model. |
| **Draft & Compose** | `core/data/ComposeDraftHolder.kt`<br>`ui/screens/compose/state/ComposeUiState.kt`<br>`ui/screens/compose/viewmodel/ComposeViewModel.kt` | Menyimpan `paperUrl` pada in-memory draft holder (`ComposeDraft`), UI state (`selectedPaperUrl`), dan mempassing nilai saat compositing. |
| **Canvas Render** | `ui/screens/compose/widgets/LetterCanvas.kt` | `Crossfade` pada layer background kertas kini mendukung Coil 3 `AsyncImage` jika `selectedPaperUrl` terisi. |
| **Pengiriman** | `ui/screens/selectrecipient/viewmodel/SelectRecipientViewModel.kt` | Mengisi `paperUrl = draft.paperUrl` ke `SendLetterRequest` pada cabang pengiriman direct (`KNOWN_USER`) maupun public feed (`PENPAL_REGION`). |
| **Penerimaan (Penpals)** | `ui/screens/penpals/widgets/OpenLetterOverlay.kt` | Pada mode fallback teks, jika `letter.paperUrl` ada, me-render `AsyncImage(model = letter.paperUrl)`. Jika kosong, menggunakan template deterministik. |
| **Penerimaan (Inbox)** | `ui/screens/inbox/widgets/EnvelopePaperReveal.kt`<br>`ui/screens/inbox/widgets/MailboxFullLetterViewer.kt` | Melewatkan `paperUrl` ke `PlainTextLetterPaper` yang me-render kertas via `AsyncImage`. |

---

## 3. Kompatibilitas & Safety

- **Backward & Forward Compatibility**:
  - Semua penambahan field bersifat opsional / nullable (`String? = null`).
  - Moshi JSON adapter pada Android secara otomatis mengabaikan field yang tidak dideklarasikan, sehingga tidak akan memicu crash jika backend atau client dideploy secara bertahap.
  - Surat-surat lama yang tidak memiliki `paper_url` tetap menggunakan mekanisme template fallback lokal secara transparan.

---

## 4. Hasil Pengujian & Verifikasi

- **Verifikasi Build**:
  - Task: `./gradlew :app:compileDevDebugKotlin`
  - Hasil: **`BUILD SUCCESSFUL in 21s`** (zero errors).
- **Verifikasi Perangkat Fisik**:
  - Perangkat: Xiaomi Redmi 9A (Android 10 / MIUI, Screen Density 2.0x, 720x1600).
  - Hasil: Fitur compositing berjalan lancar, teks dan stiker pada gambar surat tampak tajam tanpa blur atau pikselasi.
