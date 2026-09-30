# Letterly — TODO / Next Up
*Daftar kerjaan yang masih terbuka, biar tinggal pilih nomor pas mulai sesi baru.*

> Sumber kebenaran status per-fitur tetap `compose-screen-spec.md` §"Status Implementasi" — dokumen ini cuma nyaring baris yang masih 🔜 (atau ✅ sebagian) jadi daftar kerjaan konkret, plus alasan/prioritas kenapa.
>
> Sumber kebenaran status **backend** (Auth/Profile/PenPals/Letters/dst.) ada di `docs/backend-roadmap.md` — dokumen ini fokus ke kerjaan frontend.

---

## P1 — kemungkinan besar lanjut duluan

### 0. Trust & Safety (Play Store compliance) — Report/Block/TOS/Delete Account/Moderasi
**Baru dimulai 2026-08-11.** Age Gate ✅ selesai (`OnboardingGenderBirthdayScreen.kt`, `MinAgeYears = 13`, Skip dihapus khusus di step ini biar nggak bisa dilewatin). Spec lengkap + endpoint yang dibutuhkan ada di `docs/be/trust_and_safety_api.md`. Delete Account link masih nunggu keputusan bentuk deliverable (HTML statis vs. copy doang) — belum diputuskan.

**Update 2026-08-12 — PenPals Report client-side ✅ selesai:** `OpenLetterOverlay.kt`'s "···" (`SenderInfoBar`) diganti dari `ComingSoonBottomSheet` generik jadi dropdown beneran (`AnchoredDropdownMenu`) dengan 4 item — Start Conversation (buka `LetterlyCenterDialog` konfirmasi dulu, sama-sama dipakai action-rail comment icon, baru navigate ke compose reply setelah confirm), View Profile (masih `ComingSoonBottomSheet` — app ini belum punya layar profil publik user lain sama sekali, di luar scope sesi ini), Share Mail (native `Intent.ACTION_SEND`, ga butuh backend), Report Mail (`ReportMailDialog` baru di `ui/components/` — 4 alasan: spam/harassment/inappropriate_content/empty_mail + catatan opsional, styling reason-row mirip `RegionPickerSheet`'s `RegionRow`, bukan Material `RadioButton`). `feature/reports/data/` (Api/Dto/Repository) baru, mirror pola `PenpalsRepository`. Sukses report → `PenPalsViewModel.reportLetter` filter surat itu keluar dari feed (optimistic client-side hide, pola sama kayak `toggleLike`) + overlay ditutup. **Backend `POST /api/v1/reports` sendiri belum live** (client dapat error sampai di-deploy — sesuai konvensi "FE duluan, backend nyusul" di `docs/be/trust_and_safety_api.md`). Sisanya masih 🔜: Inbox's `LetterMoreMenu` (`OpenLetterScreen.kt`) report entry point masih stub, dan Block (PenPals + Inbox) belum ada entry point sama sekali — lihat detail di `docs/be/trust_and_safety_api.md` §1/§2.

### 1. Sticker Picker sungguhan
Belum dibangun sama sekali. `StickerInstance` udah ada shape-nya di `ComposeUiState.kt` (stub, unused), tapi belum ada:
- Sheet grid stiker (built-in Procreate stickers + emoji besar)
- Layer draggable di atas kertas (drag pindah, pinch resize, double-tap hapus)

**Catatan:** gambar yang di-insert dari Overflow Menu sekarang (`ComposeUiState.images: List<ImageInstance>`, `EditableImage.kt`) udah jadi layer editable penuh — drag (diclamp ke canvas), pinch resize + rotate, ganti gambar/duplikat/reorder/hapus lewat menu "···", boleh nambah banyak (lihat `docs/image-edit-mode-handoff.md`). Crop juga udah diimplementasi (`ImageCropOverlay`/`CropFrame`) tapi sengaja disembunyikan (`CropFeatureEnabled = false`) sambil nunggu polish lanjutan — worth direvisit. Ini masih jalur **terpisah** dari sistem sticker yang belum dibangun. Kalau Sticker Picker beneran dibangun, worth dipikirin apakah `ImageInstance`/`StickerInstance` di-unify jadi satu sistem overlay (biar drag/resize/rotate/delete-nya satu implementasi), bukan dua sistem yang beda.

### 2. Overflow Menu — 2 item stub yang tersisa
"Ganti Kertas" dan "Sisipkan Gambar" udah fungsional. Yang belum:
- **Simpan ke Foto** — export surat (teks + annotate + template) jadi satu PNG, simpan ke galeri
- **Bagikan Surat** — share sebagai gambar atau PDF (spec bilang "gambar/link", implementasi asli mungkin beda — worth diskusi ulang)

Berat di composite rendering (nge-flatten TextField + AnnotateCanvas + template + inserted image jadi satu Bitmap) — kemungkinan dependency baru (bukan cuma UI).

**Update — rich text nambah alasan lagi buat item ini:** sejak Bold/Italic/Text Size/Indent jadi per-seleksi (§7 di bawah, `docs/compose-screen-spec.md` §4/§9), `body_text` yang dikirim ke `POST /letters` cuma plain string polos — styling per-rentang di-strip, ga ada field terstruktur baru ditambahin ke API (keputusan: daripada bikin skema span/markup baru di backend, surat yang punya rich formatting nanti disalurin lewat `composite_image_url` yang sama, sama kayak annotate/gambar — lihat `docs/be/letters_api.md`). Jadi compositing di item ini sekarang juga jadi prasyarat biar rich formatting keliatan di surat yang diterima, bukan cuma buat annotate/gambar/stiker lagi.

### 3. Flow Kirim Surat
**Update 2026-08-02:** Backend beneran udah live di dev (`docs/be/api_contract.md`) — `POST /letters` sudah diwire lewat `feature/letters/data/LettersRepository`, tapi **cuma jalur reply** yang functional (Open Letter → Write → Select Recipient, `recipient_id` dari surat yang dibales — lihat `docs/backend-roadmap.md` M4). Tombol Send di `SelectRecipientScreen` (`ui/screens/selectrecipient/`) masih dummy front-end buat jalur "mulai percakapan baru" (Add Recipient dropdown → Share Contact stub / Penpal by region sheet dengan 7 region dummy data), karena belum ada endpoint yang bisa resolve pilihan region jadi `recipient_id` konkret (nunggu PenPals feed atau Address Book). Envelope picker (5 `envelope_N.webp`) dan Stamp picker (6 desain vector-drawn) sudah fungsional di kedua jalur. Yang masih kurang:
- Validasi (`dearName` kosong, `bodyText` < 20 / > 2000 char — sudah ada aturan di spec §10, belum diimplementasi di client sama sekali, baik jalur reply maupun region-picker)
- Jalur "mulai percakapan baru" (region-picker) masih 100% mock — bukan lagi soal storage/backend (itu udah kelar via Postgres, ~~Firebase Storage/Firestore~~ sudah gak relevan lagi, lihat `docs/backend-roadmap.md`), tapi soal belum ada endpoint buat resolve region → recipient_id
- `composite_image_url` (upload composite image kalau ada annotasi, gambar, atau sekarang juga rich text formatting — lihat update di item #2) selalu dikirim `null` — nunggu item #2 (composite rendering) beneran ada
- Real "Share Contact" (device contact picker) — sekarang masih `ComingSoonBottomSheet` stub
- Prangko asli (PNG/vector Procreate) — sekarang masih 6 swatch warna polos dengan border scalloped, belum ada desain custom

Bergantung ke item #2 (composite rendering) kalau surat punya annotasi.

**Update 2026-08-13 — Reply flow bug fixes + halaman konfirmasi kirim ✅ selesai:**
- **Fix bug:** `EnvelopeCard.kt` (`SelectRecipientScreen`) sebelumnya tetap nampilin "+ Add Recipient" pas reply (`RecipientMode.KNOWN_USER`) karena kondisi tampilannya cuma ngecek `selectedRegion` — field itu emang gak pernah keisi buat reply. Efeknya: kalau user gak sadar dan tap "Browse Penpals" di kartu itu, reply-nya diam-diam berubah jadi kirim ke penpal random, bukan balas ke pengirim asli. Sekarang mode `KNOWN_USER` nampilin "Replying to {nama}" sebagai teks statis non-interactive — gak ada jalan balik ke region picker.
- `SendLetterResponse.estimatedArrivalAt` (udah ada dari awal di kontrak, cuma dibuang di `SelectRecipientViewModel.onSendClick`) sekarang dialirin ke `SelectRecipientUiState.sentEstimatedArrivalAt` → layar konfirmasi baru. `formatExpectedDelivery()` di-expose publik dari `LetterMapping.kt` (sebelumnya private) biar format tanggalnya sama persis kayak yang dipakai Inbox — gak ada pattern `DateTimeFormatter` kedua.
- Halaman baru `LetterSentScreen.kt` (`ui/screens/selectrecipient/screen/`), muncul setelah `SendSuccessOverlay` kelar, buat SEMUA jalur kirim (reply & fresh): nampilin estimasi tiba ASLI dari backend (same-continent ~6h / cross-continent ~24h, `docs/be/letters_api.md`), bukan hardcode "1 hari" kayak asumsi awal. Background custom (gradient `BrandInkMid`→`BrandInkDeep`, bukan `bg_texture` bersama — di-skip via `isLetterSentRoute` di `MainActivity.kt`) + mascot bobbing/wobble loop pakai `rememberInfiniteTransition` (Lottie sempat dicoba, dijatohin karena ribet, diganti animasi Compose sederhana) + copy ber-suara-Peni baru (`letter_sent_body`).
- Dua tombol di bawah layar: **"Back to PenPal Feed"** (primary/filled, selalu `popUpTo(0)` balik ke tab PenPals — sekalian nge-fix bug lama: `popBackStack(Destinations.Inbox.route)` diam-diam gagal/no-op kalau reply mulai dari PenPals feed, soalnya Inbox emang gak pernah ada di back stack jalur itu) dan **"Write Another Letter"** (secondary/outlined, teks putih, langsung `popUpTo(0)` ke tab Write kosong buat user yang mau kirim lagi).
- Route baru `LetterSentRoute` (`navigation/UnsealedNavHost.kt`, arg `estimatedArrivalAt`) — bottom nav disembunyikan sama kayak `SelectRecipientRoute` (`isLetterSentRoute` di `MainActivity.kt`).
- **Tidak ada perubahan backend** — `estimated_arrival_at` udah lama ada di response `POST /letters`, cuma belum dipakai FE.
- **Belum di-test manual di device sesi ini** — cuma `./gradlew :app:assembleDevDebug` yang di-verify bersih tiap perubahan, testing live jadi tanggung jawab manual berikutnya (user).

### 4. Inbox Mode 2 + Real Data
Inbox Mode 1 (received letters) ✅ selesai. Open Letter Screen ✅ juga udah kelar (nav route `open_letter/{letterId}`, envelope↔paper reveal, Incoming Mail state, 3-dot menu, Write Letter CTA — detail di `docs/inbox-screen-spec.md`).

**Update 2026-08-02 — Real data ✅ dan Reply pre-fill ✅ selesai:** `InboxViewModel`/`OpenLetterViewModel` (baru, `ui/screens/inbox/`) manggil `GET /letters/inbox`/`GET /letters/{id}` beneran lewat `LettersRepository` — `dummyInboxItems` sudah dihapus dari codebase. Write Letter CTA sekarang navigate ke `write/reply/{id}/{name}` bawa `recipient_id`/nama pengirim, diteruskan ke `select_recipient/reply/{id}/{name}` sampai `SelectRecipientViewModel` beneran manggil `POST /letters`. Detail: `docs/backend-roadmap.md` M4.

Yang masih kurang:
- **Mode 2:** Tab switcher di bawah judul — "Received" / "Sent" (atau Drafts), dengan list surat terkirim. Backend `GET /letters/sent` udah ada (`docs/be/api_contract.md`), tinggal UI tab-switcher-nya aja yang belum
- **Report/Add Friend/Block/Open Early:** masih `ComingSoonBottomSheet` stub, belum ada backend (Add Friend ini juga yang jadi jalur seharusnya buat dapetin recipient valid di luar reply — lihat `docs/backend-roadmap.md` M4/M5)
- **Letter image asli:** `InboxItem.letterPaper` masih placeholder `PaperTemplate` — backend belum return `paper_template` di response `GET /letters/inbox`/`{id}` juga (gap kecil, lihat `docs/be/letters_api.md`)
- **Pull to refresh:** Swipe down untuk fetch surat baru
- **Empty state:** Tampilkan `inbox_empty_label` saat list kosong
- **Pagination:** `InboxViewModel` baru initial load (`limit=20`), belum ada cursor/infinite-scroll
- **Stamp asset asli:** Sekarang masih `StampSwatch` placeholder — swap guide ada di `docs/inbox-screen-spec.md`

Detail arsitektur dan swap guide: lihat `docs/inbox-screen-spec.md`.

### ~~5. Draft Auto-Save~~ ✅ selesai (jadi "Draft-on-Exit")
**Update (2026-08-09):** diimplementasi, tapi mekanisme trigger-nya diganti dari rencana awal —
bukan autosave debounce berkala di background, melainkan **dialog konfirmasi saat user coba keluar
dari Write** ("Simpan Suratnya Dulu? 💌", copy Peni-voice di `docs/copywriting/cp.md`). Keputusan
produk: lebih simpel, dan draft tetap **teks-only** (gambar/annotate/stiker sengaja ga ikut disimpan,
persis kayak batas `ComposeDraftHolder` sekarang) jadi ga ada alasan kuat buat autosave berkala —
cukup tangkap momen user mau pergi. Trade-off yang diterima: app di-force-kill/crash di tengah nulis
(bukan lewat back/tab-switch) bikin teks yang belum di-exit-dialog-in hilang.

Yang kebangun:
- [x] Room (`androidx.room` + KSP) — DB pertama di project ini. `DraftEntity`/`DraftDao`/`UnsealedDatabase` + Hilt `DatabaseModule` (`core/database/`, ikut pola `NetworkModule`/`FirebaseModule`) + `DraftRepository` (`feature/draft/data/`)
- [x] `WriteExitCoordinator` (`core/data/`, `@Singleton`) — jembatan cross-scope: `ComposeViewModel` register status "ada konten belum disimpan" pas `init`, `MainActivity`'s bottom-nav tab-tap handler & `WriteRouteContent`'s `BackHandler` (system back) sama-sama baca lewat coordinator ini buat munculin dialog yang sama sebelum benar-benar pindah/keluar
- [x] `LetterlyCenterDialog` (`ui/components/`) — center dialog pertama di codebase ini (sebelumnya semua `ModalBottomSheet`), motion per `docs/motion-rules.md`. Dipakai buat dialog exit (`SaveDraftDialog`) dan konfirmasi hapus draft manual
- [x] `ComposeViewModel`: `SavedStateHandle` (pertama kali dipakai di ViewModel ini), load draft on-init dari nav-arg `draftId`, save/discard eksplisit (bukan debounce) dipicu tombol di dialog exit, hapus row draft saat send sukses (nyentuh `SelectRecipientViewModel` juga, bareng `composeDraftHolder.clear()` yang sudah ada)
- [x] `UnsealedNavHost.kt`: route baru `write/draft/{draftId}` (mirror pola `WriteReplyRoute`), `ProfileDraftScreen.onDraftClick` navigate ke situ dengan pre-fill penuh
- [x] `ProfileDraftScreen`: `dummyDrafts` diganti `DraftDao.observeAll()` beneran lewat `ProfileDraftViewModel` baru (ViewModel pertama buat sub-screen Profile — lihat `docs/profile-screen-spec.md`) + affordance hapus draft (trash icon + dialog konfirmasi, copy "Yakin Mau Dibuang? 🗑️")

**Scope v1 = teks-only** (dearName, bodyText + rich-text styling, sign-off, font, ukuran, alignment, warna tinta, warna/template kertas, visibility, crisis tag) — gambar (`ImageInstance`), Annotate strokes, dan stiker **sengaja ga ikut** (keputusan produk, bukan gap sementara), nunggu fase 2 dibundel bareng composite-image pipeline di item #2 di atas.

**Verifikasi:** `./gradlew :app:assembleDevDebug` + `:app:lintDevDebug` bersih (0 finding baru di file yang disentuh). **Belum di-test manual di device** — build-only verified sesi ini, testing live jadi tanggung jawab manual berikutnya.

Detail desain lengkap: `docs/compose-screen-spec.md` §11 (sekarang judulnya "Draft-on-Exit").

---

## P2 — lanjutan / polish

### 5. Batch template dekoratif kedua
`PaperTemplate` batch pertama (8 tekstur solid kayak Grid/Lined/Indigo) udah live. Batch kedua dari draft spec awal (`floral_corner`, `minimal_border`, `washi_tape`, `vintage_stamp_frame`, `starry_margin`, `lined_dot`) itu **beda jenis** — overlay dekoratif transparan di ATAS warna/tekstur yang udah dipilih, bukan full-bleed pengganti. Butuh asset baru (PNG transparan) + sedikit kerja UI (toggle overlay terpisah dari template picker, karena sifatnya "tambahan" bukan "pilih salah satu").

### 6. Undo/Redo — Eraser belum masuk sistem undo
Undo/Redo teks & stroke annotate udah jalan (lihat `docs/compose-screen-spec.md` §1). Yang **sengaja** belum: hapusan dari Eraser tool ga bisa di-undo/redo — sistem sekarang cuma nge-track commit stroke baru (`onAnnotatePathCommit`), bukan semua mutasi `annotateState.paths`. Kalau mau Eraser juga undoable, perlu ubah approach dari "push on commit" jadi "push on every paths mutation" (termasuk erase), sedikit lebih kompleks state-wise.

### 7. ~~Indent (Formatting Bar)~~ ✅ selesai
Tombol `►≡` di `FormattingBar.kt` sekarang fungsional — indent per-paragraf, cycle 0→4→0. Dikerjakan bareng bikin Bold/Italic/Text Size jadi per-seleksi (dulu whole-letter, bug: ganti ukuran font pas cuma select sebagian teks ikut ngubah semua teks lain). Detail model: `docs/compose-screen-spec.md` §4/§9, implementasi di `TextStyleRuns.kt` (`StyleRun`). **Belum nyambung ke backend** — lihat catatan baru di item #2/#3 di bawah.

### 8. Profile — sub-screen backend yang masih stub
UI penuh sudah dibangun (`ui/screens/profile/`, detail di `docs/profile-screen-spec.md`). **Draft** ✅ sudah bukan dummy lagi (lihat item #5 di atas — real Room persistence lewat `ProfileDraftViewModel`); 3 sub-screen lain masih dummy in-memory:
- **Address Book** — tap kontak masih `ComingSoonBottomSheet`, belum ada detail screen/CRUD kontak sungguhan
- **Stamp Book** — `dummyStampBook` (koleksi/lock state) belum terhubung ke pembelian sungguhan di tab Stamps
- **Settings** — Subscription/Cache/Contact Us/Privacy Policy/Report Bug/Love Us semuanya `ComingSoonBottomSheet`; Subscription sengaja **tidak** deep-link ke tab Stamps (keputusan produk, lihat spec)

### 9. PenPals header — state-aware greeting (cuma default state yang jalan)
**Update (2026-08-09):** Header PenPals (`PenPalsHeader` di `PenPalsScreen.kt`) diganti dari judul statis "PenPals" + search bar dummy jadi greeting ber-suara-Peni — baris 1 `"Halo, {Nama}! 👋"` (nama dari `AuthUiState.Authenticated`/`NeedsOnboarding` → `UserDto.nickname`, fallback `"Tamu"`/`"Guest"` kalau belum login) + baris 2 title/body kondisi inbox. Search bar dihapus total (dummy, ga pernah ada filtering logic beneran).

**Yang masih kurang:** baris 2 sekarang **selalu** nampilin state "Inbox Kosong / Default", padahal desain aslinya state-aware — beda copy tergantung kondisi surat user. Belum diimplementasi karena `PenPalsScreen` belum punya sinyal inbox real (unread count / status pengiriman) — perlu expose dari `InboxViewModel`/`LettersRepository` atau query kecil terpisah dulu sebelum state lain bisa jalan. Copy 2 state yang belum kepakai (biar ga hilang):

| Kondisi | Title | Body |
|---|---|---|
| Ada Surat Baru (Unread) | Ada Surat Mendarat, [Nama]! 💌 | Wusss! Peni bawa kabar hangat dari jauh buatmu, nih. Buka yuk! |
| Surat dalam Perjalanan | Suratmu Sedang Meluncur! 🌊 | Peni lagi berjuang menembus ombak bawa cerita hangatmu. |

Detail implementasi header saat ini: `docs/penpals-screen-spec.md` §2.3.

### 10. Lint debt — `MissingTranslation` pre-existing
`./gradlew :app:lintDebug` gagal dengan 47 `MissingTranslation` error yang semuanya sudah ada sebelum fitur Profile (mis. `select_recipient_*`, `penpals_*`, `stamp_design_*`, `open_letter_*` — belum pernah ditambahkan ke `values-id/strings.xml`). Semua string baru dari fitur Stamps dan Profile sudah sinkron EN/ID sejak awal; item ini murni bersih-bersih backlog lama, bukan regresi baru.

### 11. Pilihan Kota Pengguna & Rute Asli Antar-Kota (City-to-City Delivery Routing)
**Ide Improvisasi:**
- Saat ini rute pengiriman di `DeliveryRouteMap.kt` menggunakan sepasang titik representatif wilayah benua (`ContinentCentroids` dan `IntraContinentRoutePairs`) + widget privasi (`PrivacyRouteBadge`).
- **Roadmap Peningkatan:** Tambahkan pilihan **Kota Asal (City/Region)** di halaman Onboarding / Edit Profil (misal autocomplete: *Jakarta, Surabaya, Bandung, Tokyo, Seoul, London, dsb.*).
- Aplikasi memiliki tabel koordinat titik tengah kota (*City Centroid Lookup Table*).
- **Manfaat:**
  - Tanpa izin GPS / sensor lokasi (0 permission, 100% ramah privasi).
  - Rute di peta menjadi nyata dan personal (misal: *Jakarta ➔ Tokyo* atau *Bandung ➔ Paris*).
  - Waktu tempuh pengiriman surat (*transit duration*) dapat dihitung secara realistis berdasarkan jarak antar kota.

---

## Sudah aman, ga perlu disentuh lagi kecuali ada bug baru

Toolbar Atas (Undo/Redo/Send/More), Letter Canvas (paper + tekstur + editable inserted image layer), Font Picker, Formatting Bar (size + ink color + alignment + bold/italic/indent, semuanya per-seleksi sekarang), Annotate Mode penuh, Paper Picker (8 tekstur), UiState/ViewModel shape. Detail lengkap ada di `compose-screen-spec.md`.

**Inbox Mode 1** (`InboxScreen.kt`, `InboxItem.kt`, `SoftwareBlur.kt`) — bokeh backdrop full-screen (cross-API), collapsing title, 10 dummy cards, stagger entrance, frosted glass cards dengan stamp placeholder. **Open Letter Screen** (`OpenLetterScreen.kt`) — nav route pushed dari tap kartu, envelope↔paper reveal, Incoming Mail state, 3-dot menu, Write Letter CTA. Detail: `docs/inbox-screen-spec.md`.

**Stamps Store** (`StampsScreen.kt` + `ui/screens/stamps/`) — 4 section (Claim Rewards/Buy Stamps/Buy Paper/Subscribe) UI penuh, `LetterlyStoreCard` shared widget. Backend RevenueCat belum ada (disengaja, seam-nya sudah siap) — lihat komentar di `StampsViewModel.kt`.

**Draft-on-Exit** (`core/database/`, `core/data/WriteExitCoordinator.kt`, `ui/components/{LetterlyCenterDialog,WriteExitGuard}.kt`, `ProfileDraftViewModel.kt`) — Room (DB pertama di project ini), dialog "Simpan Suratnya Dulu?" di back-press/tab-switch keluar dari Write, Continue Editing (`write/draft/{draftId}`), delete affordance di Draft list. Detail: `docs/compose-screen-spec.md` §11.

**Profile Redesain & Auth Integration** (`ui/screens/profile/ProfileScreen.kt`, `UserUtils.kt`) — Tampilan persona UI penuh sesuai mockup (`profil.png`) dengan foto/avatar, Nickname, Continent/Lokasi, Usia | Gender | Zodiak (dihitung dari `birthday`), `🟢 Last active today` badge, `INTERESTS` flow chips, `LANGUAGES` level dots, `STAMP COLLECTION` horizontal preview, `BIO`, `Envelopes`/`Lettres` segmented toggle, serta 5 sub-screen menu (`LetterlyMenuRow`). Terkoneksi dengan `AuthViewModel` (`GET /api/v1/auth/me`).

**Firebase Crashlytics** (`FirebaseModule.kt`, `AuthRepository.kt`, `build.gradle.kts`) — Terpasang penuh dengan `@Provides FirebaseCrashlytics`, otomatis mencatat `setUserId` saat auth sukses/logout, dan mengkonfigurasi `recordException` untuk non-fatal errors.

**Perbaikan Auth & Onboarding Flow** (`RegisterScreen.kt`, `LoginScreen.kt`, `OnboardingPhotoScreen.kt`, dll.):
- Fix tombol registrasi stuck dengan menambahkan state collection (`collectAsState()`) dan handling `LaunchedEffect` navigasi.
- Fix tombol onboarding klik 2x dengan standardisasi `LetterlyButton` dan pembersihan fokus keyboard otomatis (`clearFocus()`).

---

*Update terakhir: 2026-08-13 — item #3 (Flow Kirim Surat): fix bug `EnvelopeCard` yang bisa diam-diam ngubah reply jadi kirim-ke-random-penpal, thread `estimated_arrival_at` asli dari backend (dulu dibuang), halaman baru `LetterSentScreen` (custom bg + animasi mascot bobbing + copy Peni-voice + 2 tombol: balik ke PenPal Feed / tulis surat baru) gantiin navigasi post-send lama yang buggy (reply dari PenPals feed nyangkut karena target `popBackStack` gak pernah ada di stack). Build-only verified, belum di-test manual di device. Sebelumnya: PenPals header diganti jadi greeting ber-suara-Peni (nama user via `AuthUiState`, fallback Tamu/Guest) + search bar dummy dihapus total; baru default-state copy yang jalan, 2 state lain (unread/in-transit) jadi tech debt item #9. Sebelumnya lagi: Draft Auto-Save selesai diimplementasi sebagai "Draft-on-Exit" (Room pertama di project, dialog exit ganti rencana autosave-berkala, Continue Editing, delete affordance); build+lint bersih, belum di-test manual di device. Sebelumnya lagi: Inbox real data (`GET /letters/inbox`/`{id}`) & kirim surat jalur reply (`POST /letters`) diwire ke backend dev; `dummyInboxItems` dihapus. Sebelumnya lagi: Firebase Crashlytics, Profile Redesign (GET /auth/me), PenPals Header Avatar Navigation, & Auth/Onboarding Fixes.*

