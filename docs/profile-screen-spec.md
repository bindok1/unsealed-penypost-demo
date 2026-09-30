# Letterly — Profile Screen Spec
*5 menu utama + 5 sub-screen · Android · Jetpack Compose*

---

## Status Implementasi

| Bagian | Status | Catatan |
|---|---|---|
| ProfileScreen (redesain & auth integration) | ✅ | Tampilan persona lengkap (Avatar, Nickname, Continent, Age/Gender/Zodiac, Interests, Languages, Stamp Collection preview, Bio, Envelopes/Lettres toggle) + 5 `LetterlyMenuRow` (Draft/Address Book/Stamp Book/Tips/Settings) |
| Draft sub-screen | ✅ | `ProfileDraftViewModel` (ViewModel pertama di sub-tree Profile) + `DraftDao.observeAll()` — real Room persistence, delete affordance, tap → `write/draft/{draftId}` pre-fill penuh. Lihat §"Draft" |
| Address Book sub-screen | ✅ | Dummy `AddressBookEntry` list, tap → `ComingSoonBottomSheet` |
| Stamp Book sub-screen | ✅ | Grid 3-kolom, `LetterlyStoreCard(Square)`, entry terkunci didim + lock badge |
| Tips sub-screen | ✅ | 4 tip, expand-in-place (bukan navigasi), chevron rotate + `animateContentSize` |
| Settings sub-screen | ✅ | 6 baris hardcoded, semuanya → `ComingSoonBottomSheet` (termasuk Subscription — lihat catatan produk di bawah) |
| Navigation wiring | ✅ | 5 route stack-only baru di `UnsealedNavHost.kt`, bottom nav disembunyikan di semua-nya (`MainActivity.kt`) |
| String resources | ✅ | EN + ID, semua key ke-28 cocok di kedua file |
| RevenueCat / backend sungguhan | 🔜 | Semua data dummy in-memory — lihat §"Yang Belum" per sub-screen |

---

## Gambaran Umum

Profile adalah tab ke-5 (terakhir) di bottom nav. Tidak ada state screen-level-nya sendiri —
murni daftar menu yang fan-out ke 5 sub-screen stack-only (bukan tab), masing-masing punya
`LetterlyScreenHeader` (back arrow + judul) sendiri dan disembunyikan dari bottom nav bar selagi aktif.

**Keputusan produk penting:** item **Subscription** di Settings sengaja **tidak** deep-link ke tab
Stamps — dia membuka `ComingSoonBottomSheet` yang sama seperti item Settings lain, karena flow
subscription yang sebenarnya (RevenueCat paywall) belum tentu sama dengan UI Stamps tab.

---

## Arsitektur

### File yang terlibat

| File | Keterangan |
|---|---|
| `ui/screens/profile/ProfileScreen.kt` | Menu utama — 5 `LetterlyMenuRow`, terima 5 callback navigasi |
| `ui/screens/profile/ProfileDraftScreen.kt` | `DraftEntry` model + screen (data dari `ProfileDraftViewModel`, bukan dummy lagi) |
| `ui/screens/profile/ProfileDraftViewModel.kt` | ViewModel pertama di sub-tree Profile — `DraftRepository.observeAll()` → `List<DraftEntry>`, `onDeleteDraft` |
| `ui/screens/profile/ProfileAddressBookScreen.kt` | `AddressBookEntry` model + `dummyAddressBook` (6 item) + screen |
| `ui/screens/profile/ProfileStampBookScreen.kt` | `StampBookEntry` model + `dummyStampBook` (6 item, reuse `StampDesign`) + screen |
| `ui/screens/profile/ProfileTipsScreen.kt` | `TipEntry` model + dummy list (4 item) + screen dengan expand-in-place |
| `ui/screens/profile/ProfileSettingsScreen.kt` | Screen dengan 6 baris hardcoded (bukan data-driven — tiap baris punya copy/behavior beda) |
| `ui/components/LetterlyMenuRow.kt` | Shared row ikon+judul(+subjudul)+trailing — lihat `component-library.md` |
| `ui/components/LetterlyScreenHeader.kt` | Shared back-arrow+judul header — lihat `component-library.md` |
| `core/util/MotionUtils.kt` | `staggerEntrance` modifier (dipromosikan public dari `StampsScreen.kt` saat Profile jadi pemakai ke-2) |
| `navigation/UnsealedNavHost.kt` | 5 route stack-only (`ProfileDraftRoute`, dst.) + `ProfileStackRoutes` list |
| `MainActivity.kt` | `bottomBar` visibility check meng-exclude `ProfileStackRoutes` |
| `res/values/strings.xml`, `values-id/strings.xml` | `profile_*` keys (28 key, EN+ID sinkron) |

### Dependency graph

```
UnsealedNavHost
├── Destinations.Profile → ProfileScreen(onDraftClick, onAddressBookClick, onStampBookClick, onTipsClick, onSettingsClick)
├── ProfileDraftRoute → ProfileDraftScreen(uiState dari ProfileDraftViewModel, onBackClick, onDraftClick → navigate ke write/draft/{draftId}, onDeleteDraft)
├── ProfileAddressBookRoute → ProfileAddressBookScreen(onBackClick)
├── ProfileStampBookRoute → ProfileStampBookScreen(onBackClick)
├── ProfileTipsRoute → ProfileTipsScreen(onBackClick)
└── ProfileSettingsRoute → ProfileSettingsScreen(onBackClick)
```

`ProfileDraftViewModel` adalah satu-satunya `ViewModel`/`UiState` di sub-tree Profile ini — 5 screen
lain (Address Book/Stamp Book/Tips/Settings/menu utama) tetap composable biasa + `remember` lokal
(untuk `ComingSoonBottomSheet` visibility di Address Book/Settings, dan expand state di Tips). Draft
butuh ViewModel karena datanya sekarang live dari Room (`DraftDao.observeAll()`, Flow-backed) —
beda dari `StampsScreen.kt` yang pakai `ViewModel+UiState` karena state lintas-recomposition yang
lebih kompleks (klaim reward) dan seam siap-RevenueCat, tapi motivasinya sama: begitu ada sumber
data sungguhan (bukan dummy in-memory), `remember` lokal ga cukup lagi.

---

## Draft

Menampilkan draft surat yang belum terkirim, live dari Room (`ProfileDraftViewModel` →
`DraftRepository.observeAll()`). Tiap baris (`DraftCard`, private composable — cuma dipakai sekali,
jadi tidak diekstrak ke `ui/components/`) menampilkan thumbnail `PaperTemplate` 48dp, nama penerima,
cuplikan isi (1 baris, ellipsis), label "diedit X lalu" (`DateUtils.getRelativeTimeSpanString`,
locale-aware), trailing trash `IconButton`, dan chevron. Tap kartu → navigate ke
`write/draft/{draftId}` (pola sama kayak `write/reply/{userId}/{name}`) — `ComposeViewModel` load +
restore isi surat penuh dari row Room-nya lewat `SavedStateHandle`.

**Scope v1 cuma teks-only** (`docs/compose-screen-spec.md` §11): dearName, body text + rich-text
styling per-seleksi, sign-off, font, warna tinta, warna/template kertas, alignment, visibility,
crisis tag — restore penuh. Gambar yang disisipkan, goresan Annotate, dan stiker **sengaja tidak
ikut di-restore** (keputusan produk, bukan keterbatasan sementara — ribet nyimpen koordinat gambar/
annotate, dan dialog exit sudah bilang eksplisit ke user "gambar/coretan/stiker bakal ikut hilang"
saat pilih Simpan sebagai Draft).

Tap trash icon → dialog konfirmasi terpisah ("Yakin Mau Dibuang? 🗑️", `docs/copywriting/cp.md`,
reuse `LetterlyCenterDialog` dengan warna destructive) → `onDeleteDraft` → `DraftRepository.delete()`,
list update otomatis (Flow-backed).

---

## Address Book

Daftar kontak penpal tersimpan (`AddressBookEntry(id, name, country)`), dirender pakai
`LetterlyMenuRow` (ikon `Person`, subjudul = nama negara). Tap baris mana pun → `ComingSoonBottomSheet`
judul "Address Book" — belum ada detail-screen kontak sungguhan.

---

## Stamp Book

Galeri pasif (bukan interaktif — `onClick = {}`) semua `StampDesign` yang sudah/belum dikoleksi
user. `LazyVerticalGrid(GridCells.Fixed(3))` dari `LetterlyStoreCard(shape = Square)`, reuse
`StampSwatch` sebagai `image`. Entry terkunci (`isCollected = false`) di-dim (`alpha = 0.35f`) dan
dapat lock-badge bulat di pojok kanan-atas. Dummy state: Rose/Ocean/Marigold/Sage terkumpul,
Plum/Ink terkunci.

---

## Tips

How-to guide, 4 entry dummy (`TipEntry(id, titleRes, bodyRes, icon)`): "Why sign in?", "How do
stamps work?", "Writing your first letter", "Staying safe with penpals". Beda dari 4 sub-screen
lain — baris **expand in-place** saat di-tap (bukan navigasi), lewat `animateContentSize` +
chevron yang rotate 0°→180° (`LetterlySpring.Gentle`, sadar reduced-motion).

`GentleSizeSpring` (private, `ProfileTipsScreen.kt`) adalah mirror `IntSize`-typed dari
`LetterlySpring.Gentle` — sama seperti `BouncyDpSpring` di `LetterlyStoreCard.kt`, dibutuhkan
karena `LetterlySpring` cuma expose varian `Float` dan `animateContentSize` butuh spec `IntSize`.

---

## Settings

6 baris **hardcoded** (bukan data-driven lewat list+enum) karena tiap baris punya copy dan tujuan
yang benar-benar beda, jadi abstraksi list-nya cuma nambah indirection tanpa reuse nyata:

| Baris | Ikon | Perilaku |
|---|---|---|
| Subscription | `WorkspacePremium` | `ComingSoonBottomSheet` — **sengaja tidak** deep-link ke tab Stamps, lihat §"Gambaran Umum" |
| Cache | `Storage` | `ComingSoonBottomSheet`, subjudul dummy "128 MB used" (Cache-clearing sungguhan sengaja tidak diimplementasi — di luar scope) |
| Contact Us | `Email` | `ComingSoonBottomSheet` |
| Privacy Policy | `PrivacyTip` | `ComingSoonBottomSheet` |
| Report a Bug | `BugReport` | `ComingSoonBottomSheet` |
| Love Us | `Favorite` | `ComingSoonBottomSheet` |

---

## Navigation & Bottom Nav Visibility

5 route baru di `UnsealedNavHost.kt`, dikumpulkan di `internal val ProfileStackRoutes` supaya
`MainActivity.kt` bisa exclude semuanya dari `bottomBar` dengan satu `currentRoute !in ProfileStackRoutes`
check — pola yang sama seperti `SelectRecipientRoute`/`OpenLetterRoute` yang sudah ada duluan.

```kotlin
internal val ProfileStackRoutes = listOf(
    ProfileDraftRoute, ProfileAddressBookRoute, ProfileStampBookRoute,
    ProfileTipsRoute, ProfileSettingsRoute,
)
```

---

## Checklist verifikasi (sudah dijalankan)

- [x] `./gradlew :app:assembleDebug` — build bersih
- [x] Lint: 0 finding baru dari kode/string Profile (47 error lint yang ada semuanya pre-existing,
      `MissingTranslation` dari fitur-fitur sebelum Profile — lihat catatan di `docs/todo.md`)
- [x] Emulator: ProfileScreen → semua 5 baris nav ke sub-screen yang benar
- [x] Draft → tap entry → navigate ke tab Write *(checklist lama, sebelum Draft-on-Exit — behavior
      sekarang berubah jadi navigate ke `write/draft/{draftId}` dengan pre-fill penuh; build+lint
      bersih tapi belum di-verifikasi manual di device, lihat `docs/todo.md` #5)*
- [x] Address Book → tap entry → `ComingSoonBottomSheet` judul "Address Book"
- [x] Stamp Book → grid render, entry terkunci ter-dim + lock badge
- [x] Tips → tap "Why sign in?" → expand in-place, chevron rotate, body muncul
- [x] Settings → 6 baris render, "Subscription" → `ComingSoonBottomSheet` (bukan navigasi Stamps)
- [x] Bottom nav bar tersembunyi di semua 5 sub-screen, muncul lagi begitu kembali ke tab manapun

---

*Letterly Profile Screen Spec v1.0 · Jetpack Compose*
