# Letterly — Compose Screen Feature Spec
*Typed + Annotate Mode · Android · Jetpack Compose*

---

## Status Implementasi

| Bagian | Status | Catatan |
|---|---|---|
| 1. Toolbar Atas | ✅ P0 | Semua ikon tampil (`ComposeToolbar.kt`) dan fungsional, termasuk **Undo/Redo** (history stack asli, lihat §1 detail di bawah). More buka dropdown menu asli (`OverflowMenuButton.kt`), Aa buka Font Picker asli. Send dipisah jadi lingkaran solid sendiri di luar pill (`SendButton`), bukan ikon terakhir di dalam grup. |
| 1a. Sticky Reply Banner | ✅ P1 | `StickyReplyBanner.kt`, muncul di bawah toolbar saat `uiState.replyRecipientName` tidak null — lihat §1 detail "Reply Context" di bawah. Dulu **bocor** (bug): banner masih nongol di surat baru yang nggak ada hubungannya sama reply manapun karena `ReplyLetterContextHolder` (singleton one-shot hand-off) nggak pernah di-`clear()`. Sudah diperbaiki — lihat catatan di bawah. |
| 2. Kertas Surat (Letter Canvas) | ✅ P0 (sebagian) | `LetterCanvas.kt` — teks diketik + 5 warna paper solid + gambar yang di-insert sebagai layer editable (`EditableImage.kt`, lihat §8 & `docs/image-edit-mode-handoff.md`: drag/pinch-resize/rotate/duplicate/reorder, ganti gambar, hapus, boleh lebih dari satu, ga bisa digeser keluar canvas). Template PNG dan texture PNG (`BlendMode.Multiply`) **belum ada** (butuh asset / scope lanjutan). |
| 3. Font Picker | ✅ P1 | Sheet asli sudah jalan (`FontPickerSheet.kt`) — grid 6 font (semua punya asset nyata sekarang) + "More Fonts" (Google Fonts on-demand download), live update ke canvas. Text Size **pindah ke Formatting Bar** (§4) — nggak lagi di sheet ini. Detail: `docs/annotate-mode-and-font-picker.md`, `docs/font-completion-bold-italic-google-fonts.md`. |
| 4. Formatting Bar | ✅ P0 | `FormattingBar.kt` — alignment toggle, hide-keyboard, Aa shortcut, **Text Size** (popup slider, `LetterlySlider`, lihat `docs/component-library.md`), dan **Warna Tinta teks** (reuse `AnnotateColorButton`) semua fungsional. Bold/Italic/Text Size/**Indent** sekarang **per-seleksi** (rich text), bukan whole-letter lagi — select sebagian teks lalu ubah size/bold/italic cuma restyle rentang itu; tanpa seleksi (kursor doang) jadi "pending style" buat karakter berikutnya yang diketik. Indent (`►≡`) sekarang fungsional, berlaku per-paragraf, cycle 0→4→0. Model datanya: `StyleRun` per-rentang di `TextStyleRuns.kt` (lihat §9). |
| 5. Annotate Mode | ✅ P2 | Canvas gambar asli jalan (`AnnotateCanvas.kt`/`AnnotateToolbar.kt`): Pen/Fine/Wave/Deco/Eraser + color picker custom (Spektrum/Grid/Penggeser + opacity + hex copy, lihat `AnnotateColorPicker.kt`) — sliders-nya pakai `LetterlySlider` shared component. Stroke persist setelah keluar mode. Undo/Redo stroke sekarang fungsional lewat toolbar atas (lihat §1). Detail: `docs/annotate-mode-and-font-picker.md`, `docs/component-library.md`. |
| 6. Overflow Menu | ✅ P1 (sebagian) | `OverflowMenuButton.kt` — dropdown icon+text asli, anchored ke ikon `···`. "Ganti Kertas" dan "Sisipkan Gambar" fungsional; "Pasang Stiker"/"Simpan ke Foto"/"Bagikan Surat" masih routing ke `ComingSoonBottomSheet`. |
| 7. Paper & Template Picker | ✅ P1 | `PaperPickerSheet.kt` — `LazyRow` 8 tekstur `PaperTemplate` (`ui/theme/PaperTemplate.kt`), fungsional & live-update ke canvas (`LetterCanvas.kt` render full-bleed via `ContentScale.Crop`). Flat-color `PaperColor` swatches sengaja **tidak** dijadikan opsi picker — produk mau canvas selalu terasa seperti kertas surat sungguhan, bukan background warna polos. `PaperColor` masih dipakai sebagai fallback background non-UI. Asset: 1240×1754px WEBP (rasio A4) di `res/drawable/`. |
| 8. Sticker Picker | 🔜 P1 | Belum dibangun sama sekali (sticker asset asli + grid picker). Gambar dari Overflow Menu ("Sisipkan Gambar") sudah jadi layer editable penuh (`ComposeUiState.images: List<ImageInstance>`, `EditableImage.kt`) — drag (diclamp ke dalam canvas), pinch resize + rotate, ganti gambar/duplikat/urutan depan-belakang/hapus lewat menu "···", boleh nambah banyak. Crop sudah diimplementasi tapi sengaja disembunyikan dari menu (`CropFeatureEnabled = false`) sambil nunggu polish lanjutan. Ini tetap jalur terpisah dari sistem sticker yang belum dibangun, bukan jenis "sticker" yang sama. Detail: `docs/image-edit-mode-handoff.md`. |
| 9. UiState & ViewModel | ✅ P0 | `ComposeUiState.kt` + `ComposeViewModel.kt` — shape data class lengkap sesuai spec, tapi state in-memory saja (tidak ada Room/Firebase-backed field yang benar-benar tersambung). |
| 10. Flow Kirim Surat | ✅ P1 (sebagian) | Tombol Send sekarang navigate ke `SelectRecipientScreen` (`ui/screens/selectrecipient/`) — dummy front-end: pilih recipient (Share Contact stub / Penpal by region, 7 region dummy data) + pilih stamp (6 desain vector, no asset), judul berubah "Select Recipient" → "Ready to Send" begitu stamp dipilih, tap Send munculin dummy success overlay lalu navigate ke Inbox. Belum ada validasi bodyText/dearName dari spec asli, dan belum ada Firebase Storage/Firestore — recipient/stamp semuanya in-memory mock, bukan data sungguhan. |
| 11. Draft-on-Exit | ✅ | Room (DB pertama di project ini) + dialog exit "Simpan sebagai Draft?" (trigger di back-press/tab-switch, bukan autosave berkala) — lihat §11 di bawah. |
| 12. Checklist Implementasi | — | Lihat tabel di atas per item; P0 checklist asli (ComposeScreen/LetterCanvas/ComposeToolbar/FormattingBar/ViewModel+UiState) selesai dengan catatan di atas. |

DI note: project sekarang pakai **Hilt** (`@HiltAndroidApp` di `UnsealedApplication`, `@AndroidEntryPoint` di `MainActivity`, `@HiltViewModel` di `ComposeViewModel`) — baru diperkenalkan bareng fitur ini, sebelumnya tidak ada DI framework sama sekali.

---

## Gambaran Umum

Compose screen adalah **jantung dari Letterly**. User menghabiskan paling banyak waktu di sini. Experience-nya harus terasa seperti benar-benar duduk di meja dan menulis surat fisik — bukan seperti nulis di notes app.

**Referensi visual:** Navi (iOS) — aesthetic kertas legal pad kuning di atas meja kayu gelap.

**Keputusan platform:**
- Tidak ada full drawing mode (kanvas kosong)
- Drawing hadir sebagai **Annotate Mode** — lapisan coretan tangan DI ATAS teks yang sudah diketik
- Ini lebih unik dari iOS dan lebih personal dari sekadar typed text

---

## Struktur Layar

```
┌─────────────────────────────────────┐
│  TOOLBAR ATAS (floating, frosted)   │
│  ← Undo  Redo →  Aa  ⌨  ✏  ···  ✈ │
├─────────────────────────────────────┤
│                                     │
│                                     │
│       KERTAS SURAT (canvas)         │
│                                     │
│  [Template PNG rendered di sini]    │
│  [Teks diketik di atasnya]          │
│  [Annotate layer paling atas]       │
│                                     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  FORMATTING BAR (muncul saat       │
│  keyboard aktif, di atas keyboard) │
│  Aa  ≡  →≡  ⌨↓                    │
├─────────────────────────────────────┤
│        KEYBOARD                     │
└─────────────────────────────────────┘
```

---

## 1. Toolbar Atas

Selalu visible. Frosted glass dark (seperti referensi — `Color(0xB3140C04)`).

### Komponen Toolbar

```
[←] [→]  [Aa]  [⌨]  [✏]  [···]          ( ✈ )
Undo Redo Font  KB  Annot  More          Send
└──────── pill blur ─────────┘         lingkaran solid terpisah
```

| Icon | Label | Aksi |
|---|---|---|
| `←` | Undo | Undo aksi terakhir (teks atau annotasi) |
| `→` | Redo | Redo aksi yang di-undo |
| `Aa` | Font | Buka Font Picker bottom sheet |
| `⌨` | Keyboard | Toggle keyboard muncul/sembunyi |
| `✏` | Annotate | Toggle Annotate Mode ON/OFF |
| `···` | More | Buka dropdown overflow menu |
| `✈` | Send | Kirim surat (confirm dialog) |

**States toolbar icon `✏` (Annotate):**
- OFF: icon normal, opacity 60%
- ON: icon highlight dengan `LemonYellow` background pill, opacity 100%

**Transition:** saat Annotate Mode ON, toolbar sedikit shift — icon `⌨` fade out (keyboard disembunyikan), annotate toolbar muncul di bawah kertas.

> **Catatan implementasi (Send):** tombol Send **sengaja dikeluarkan** dari pill blur utama — dia hidup sebagai lingkaran solid terpisah (`SendButton`, private composable di `ComposeToolbar.kt`), dipisahkan dari grup Undo/Redo/Font/Keyboard/Annotate/More oleh `Spacer(Modifier.weight(1f))`. Alasannya: Send itu final action yang beda kelas fungsinya dari tombol-tombol toggle/formatting di sebelahnya (bukan "satu lagi ikon di grup yang sama"), jadi dipisah biar keliatan beda secara UX — solid `ToolbarFrostedDark` (bukan blur transparan seperti pill), shadow sendiri, scale feedback lewat `rememberPressScale` (motion-rules.md §3.1/§4, no ripple).

> **Catatan implementasi (Undo/Redo):** history stack asli, disimpan sebagai buffer privat di `ComposeViewModel` (bukan di `ComposeUiState` — supaya state yang di-expose ke UI tetap ringkas, cuma expose 4 boolean turunan: `canUndoText`/`canRedoText`/`canUndoAnnotate`/`canRedoAnnotate`). Dua stack independen:
> - **Teks** (`textUndoStack`/`textRedoStack`, tipe `String` snapshot penuh dari `bodyText`): di-push dengan debounce 600ms sejak jeda ngetik terakhir (`TextUndoDebounceMillis`) — bukan per-keystroke, biar satu Undo membalikkan satu "ledakan ngetik", bukan satu huruf. Dibatasi `MaxUndoDepth = 50` snapshot biar ga numpuk terus di sesi nulis yang panjang.
> - **Annotate** (`annotateRedoStack`, tipe `AnnotatePath`): undo = pop stroke terakhir dari `annotateState.paths`, redo = push balik. Hanya nge-track commit stroke (`onAnnotatePathCommit`) — hapusan dari Eraser sengaja **tidak** masuk sistem undo/redo ini (`onAnnotateErase` cuma update `canUndoAnnotate` biar tombol tetap akurat, tapi ga bisa di-undo/redo), sesuai kalimat spec "undo stroke terakhir" yang secara literal soal stroke, bukan semua mutasi paths.
>
> Tombol Undo/Redo di toolbar selalu satu pasang, tapi `ComposeUiState.canUndo`/`canRedo` (computed property) otomatis milih stack yang relevan berdasar `isAnnotateMode` — jadi `ComposeViewModel.onUndoClick()`/`onRedoClick()` juga branch berdasar mode yang sama, persis sesuai spec: "Jika Annotate Mode OFF → undo teks; Jika Annotate Mode ON → undo stroke terakhir."

### Sticky Reply Banner (Reply Context)

Saat user membalas surat (tap "Balas Surat" dari PenPals feed/Mailbox), `StickyReplyBanner` muncul melayang tepat di bawah toolbar (di dalam `Column` floating yang sama, `ComposeScreen.kt`) menampilkan "Membalas [Nama]". Tap banner buka `OriginalLetterBottomSheet` (kalau surat aslinya ada, `uiState.replyLetter != null`) supaya user bisa baca ulang surat yang dibalas; kalau tidak ada body surat (`hasLetterBody = false`), tap-nya jadi `onBackClick` biasa.

**Dua jalur reply context masuk ke `ComposeUiState.replyRecipientName`/`replyLetter`, di-resolve bareng di `ComposeViewModel.init`:**
1. **Nav-arg langsung** (`WriteReplyUserIdArg`/`WriteReplyNameArg` dari route `write/reply/{userId}/{name}`) — dipakai saat entry point-nya sudah tau siapa recipient dari awal (mis. dari `MailboxThreadScreen`'s "Write Letter" atau `writeReplyRoute(...)` lain).
2. **`ReplyLetterContextHolder`** (`ui/screens/compose/state/ReplyLetterContextHolder.kt`, `@Singleton`) — dipakai saat entry point-nya lewat tab bottom-nav plain `write` (nggak bawa nav-arg), khususnya dari `PenPalsViewModel.prepareReplyLetter()` yang set `PenpalLetter` lengkap (bukan cuma nama) ke holder sebelum navigate, supaya `OriginalLetterBottomSheet` punya isi surat asli untuk ditampilkan, bukan cuma nama pengirim.

> **Bug (fixed):** `ReplyLetterContextHolder` didesain sebagai **one-shot hand-off** — nilainya seharusnya cuma "diminum" sekali oleh `ComposeViewModel` berikutnya yang dibuka. Tapi `clear()`-nya (sudah ada dari awal di class-nya) **tidak pernah dipanggil di mana pun**, jadi holder-nya nyangkut selamanya di memory (singleton, survive selama proses app hidup). Gejalanya: user balas surat A → kirim → dari `LetterSentScreen` tap "Tulis Surat Baru" (`onWriteNewLetterClick`, `UnsealedNavHost.kt`) yang navigate ke `Destinations.Write.route` dengan `popUpTo(0){inclusive=true}` (backstack di-reset total, jadi `ComposeViewModel` lama di-destroy dan instance **baru** dibuat) — instance baru ini nggak punya nav-arg reply (route-nya plain `write`), tapi `init`-nya tetap baca `replyLetterContextHolder.activeReplyLetter.value` yang masih nyimpen surat A dari sesi sebelumnya, jadi banner "Membalas [Nama A]" nongol lagi di surat yang seharusnya kosong/baru.
>
> **Fix:** `ComposeViewModel.init` sekarang manggil `replyLetterContextHolder.clear()` tepat setelah baca `activeReplyLetter.value`, jadi begitu satu `ComposeViewModel` "mengonsumsi" reply context itu (baik lewat holder atau nav-arg — clear-nya nggak bersyarat), holder balik ke `null` dan `ComposeViewModel` instance berikutnya yang dibuka tanpa nav-arg reply selalu mulai bersih.

> **Bug (fixed):** Tap banner buka `OriginalLetterBottomSheet` dengan benar (`replyLetter != null`), tapi sheet itu sendiri cuma render `letter.bodyText` — nggak pernah cek `letter.compositeImageUrl`. Kalau surat asli yang dibalas dibuat pakai **mode gambar** (compose image, bukan mode text — lihat komentar di `PenpalLetter.kt`'s `compositeImageUrl`), backend ngirim `bodyText` kosong, jadi sheet cuma nampilin placeholder `"(A letter with no text content)"` alih-alih isi surat asli. Dari sudut pandang user kesannya banner "nggak ngapa-ngapain" pas di-tap, padahal secara teknis dia jalan dan sheet-nya kebuka — cuma isinya kosong. Ini inkonsisten sama `OpenLetterOverlay.kt` (reader utama di feed PenPals) yang sudah lebih dulu bener: cek `compositeImageUrl != null` dulu baru fallback ke `bodyText`.
>
> **Fix:** `OriginalLetterBottomSheet.kt` sekarang niru fallback order yang sama persis dengan `OpenLetterOverlay` — render `AsyncImage(letter.compositeImageUrl)` kalau ada (mode gambar), baru fallback ke `Text(letter.bodyText)` kalau nggak (mode text).

---

## 2. Kertas Surat (Letter Canvas)

Area utama tempat user berinteraksi. Bukan `TextField` biasa — ini adalah `Box` dengan beberapa layer yang ditumpuk.

### Layer Stack (urutan dari bawah ke atas)

```
Layer 1: Paper Background PNG       ← template / warna kertas
Layer 2: Template Decoration PNG    ← ilustrasi hiasan (opsional)
Layer 3: TextField (teks surat)     ← typed content
Layer 4: Annotate Canvas            ← DrawBox layer, hanya aktif saat Annotate Mode ON
Layer 5: Sticker Overlay            ← stiker yang ditempel, draggable
```

### Paper Background

5 warna kertas default + custom templates dari Procreate:

| ID | Nama | Warna |
|---|---|---|
| `cream` | Ivory | `#F5EDD8` |
| `pink` | Strawberry | `#F7E0E8` |
| `lavender` | Lavender | `#E8E0F0` |
| `mint` | Mint | `#D8EDE4` |
| `blue` | Sky | `#D8E8F2` |

Paper texture PNG di-overlay dengan `BlendMode.Multiply` untuk kesan kertas fisik.

### TextField Styling

```kotlin
BasicTextField(
    value = text,
    onValueChange = { text = it },
    textStyle = LetterBodyStyle.copy(color = inkColor),
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 20.dp),
    decorationBox = { innerTextField ->
        // Placeholder "Dear ___," di baris pertama
        // Line guide horizontal (opsional, toggle)
        innerTextField()
    }
)
```

**Aturan teks:**
- Font default: Caveat Regular 15sp, line height 28sp
- User bisa ganti font via Font Picker
- Teks selalu di atas paper layer, di bawah annotate layer
- Placeholder awal: `"Dear ___,"` dengan opacity 40%
- Keyboard type: `KeyboardType.Text`, `ImeAction.Default` (enter = newline)

---

## 3. Font Picker (Bottom Sheet)

Muncul saat tap icon `Aa` di toolbar.

### Font yang Tersedia

| Font | Karakter/Vibe |
|---|---|
| Caveat | Expressive, dewasa — **default** |
| Kalam | Hangat, diary |
| Architects Daughter | Clean, school |
| Indie Flower | Playful, bubbly |
| Nothing You Could Do | Elegan, tipis |
| Shadows Into Light | Dramatis, emosional |

### UI Font Picker

```
┌──────────────────────────────────────┐
│ ▬▬▬  (drag handle)                  │
│                                      │
│  Pilih Gaya Tulisan                  │
│                                      │
│  ┌────────┐  ┌────────┐  ┌────────┐ │
│  │Caveat  │  │ Kalam  │  │Arch.D  │ │
│  │"Halo"  │  │"Halo"  │  │"Halo"  │ │
│  │✓ aktif │  │        │  │        │ │
│  └────────┘  └────────┘  └────────┘ │
│                                      │
│  ┌────────┐  ┌────────┐  ┌────────┐ │
│  │ Indie  │  │Nothing │  │Shadows │ │
│  │"Halo"  │  │"Halo"  │  │"Halo"  │ │
│  └────────┘  └────────┘  └────────┘ │
└──────────────────────────────────────┘
```

Setiap card font preview menampilkan kata "Halo," dengan font tersebut. Item masuk dengan stagger animation 40ms (lihat Motion Rules).

> **Catatan implementasi:** slider "Ukuran Teks" di mockup awal di atas **sudah dipindah ke Formatting Bar** (§4) — dulu ada di sini, tapi karena user sering ganti-ganti ukuran teks sambil ngetik, menguburnya satu sheet lagi (harus buka Font Picker dulu) bikin susah dilihat/diakses. Sekarang sheet ini murni soal pilih font family + download Google Fonts tambahan.

---

## 4. Formatting Bar (Di Atas Keyboard)

Muncul otomatis saat keyboard aktif. Warna matching dengan paper (`PaperCream`, dll).

```
┌──────────────────────────────────────────────────┐
│  Aa   Tt   ◉   ≡◄   ►≡                  ⌨↓       │
│ Font Size Ink Align Indent              HideKB    │
└──────────────────────────────────────────────────┘
```

| Kontrol | Fungsi |
|---|---|
| `Aa` | Shortcut ke Font Picker (grid font + Google Fonts) |
| `Tt` | Ukuran teks — popup slider kecil (lihat catatan implementasi) |
| `◉` | Warna tinta teks — popup color picker (spektrum/grid/slider + opacity, sama seperti Annotate Mode) |
| `≡◄` | Alignment toggle: Left / Center / Right |
| `►≡` | Indent: tambah indent baris |
| `⌨↓` | Sembunyikan keyboard |

Bar ini menggunakan `WindowInsets` untuk tidak tertabrak keyboard. Background: `paper color + 0.9 alpha` agar terasa menyatu dengan kertas.

> **Catatan implementasi:** `Tt` (`TextSizeButton`, private composable di `FormattingBar.kt`) dan `◉` (`AnnotateColorButton`, di-reuse langsung dari Annotate Mode's ink picker — lihat `AnnotateColorPicker.kt`) keduanya baru, dipindah/ditambah ke sini supaya user bisa lihat & ubah ukuran/warna teks sambil ngetik tanpa buka sheet terpisah. Keduanya popup anchored ke ikon trigger-nya (pola yang sama dengan `OverflowMenuButton`/`AnnotateColorButton`: `Popup` + scale/alpha `LetterlySpring.Gentle`/`Stiff`, dark chrome `Surface`), tumbuh ke ATAS karena bar ini ada di bawah layar (di atas keyboard).

> **Catatan implementasi — rich text per-seleksi:** `Tt`, Bold, Italic, dan `►≡` (Indent) semuanya **selection-aware**, bukan whole-letter lagi. Kalau ada teks yang di-select, restyle cuma berlaku ke rentang itu (fix dari bug awal: dulu ganti ukuran font ikut ngubah semua teks lain padahal cuma sebagian yang di-select). Kalau kursor doang tanpa seleksi, toggle-nya jadi "pending style" — berlaku ke karakter berikutnya yang diketik, lalu otomatis clear begitu selection pindah tanpa ngetik (biar toolbar-nya nggak nunjukin state basi dari posisi kursor sebelumnya). Detail model data: `ComposeUiState.bodyStyleRuns`/`bodySelection`/`pendingBold`/`pendingItalic`/`pendingFontSize` (§9) + `TextStyleRuns.kt` (`StyleRun`, `applyOverride`, `afterTextChange`, `buildStyledBody`). Indent berlaku per-**paragraf** penuh (bukan per-karakter — `ParagraphStyle.textIndent` Compose bakal motong satu baris jadi dua kalau cuma sebagian yang di-style), cycle 0→1→2→3→4→0 tiap ditekan (nggak ada tombol outdent terpisah).

---

## 5. Annotate Mode

Ini **fitur pembeda Letterly di Android**. Bukan kanvas kosong — tapi lapisan coretan tangan di atas surat yang sudah diketik.

### Konsep

```
SEBELUM ANNOTATE:
┌─────────────────────┐
│ Dear Karin,         │
│                     │
│ Aku mau cerita...   │
│                     │
└─────────────────────┘

SETELAH ANNOTATE (user coret-coret):
┌─────────────────────┐
│ Dear Karin,         │ ← user tambah underline manual
│           ~~~       │ ← user gambar gelombang kecil
│ Aku mau cerita...   │
│              ♡      │ ← user gambar hati di pojok
│ P.S. kangen!        │ ← user tulis tangan di bawah
└─────────────────────┘
```

### Toolbar Annotate (muncul di bawah kertas saat mode ON)

Menggantikan keyboard saat Annotate Mode aktif.

```
┌──────────────────────────────────────────────┐
│  🖊  🖋  〰  ✱  [eraser]     ◉ color picker  │
│ Pen Fine Wave Deco  Hapus       Warna         │
└──────────────────────────────────────────────┘
```

| Tool | Fungsi | Stroke style |
|---|---|---|
| `🖊` Pen | Tulisan tangan umum | Smooth, 3px |
| `🖋` Fine | Detail halus, coretan kecil | Thin, 1.5px |
| `〰` Wave | Underline dekoratif | Wavy line preset |
| `✱` Deco | Stamp kecil (hati, bintang, bunga) | Tap = stamp, bukan stroke |
| Eraser | Hapus annotasi | Area erase |
| Color | Pilih warna tinta annotasi | Color wheel |

**Warna tinta annotasi default:**
- Merah (`#E53935`) — paling umum, seperti koreksi
- Biru (`#1E40AF`) — tinta pen klasik
- Hijau (`#065F46`) — catatan samping
- Hitam (`#1F2937`) — bold annotation
- Pink (`#9D174D`) — sweet, personal

### State Management Annotate

```kotlin
data class AnnotateState(
    val isActive: Boolean = false,
    val selectedTool: AnnotateTool = AnnotateTool.PEN,
    val inkColor: Color = Color(0xFFE53935),
    val strokeWidth: Float = 3f,
    val paths: List<AnnotatePath> = emptyList(), // tersimpan terpisah dari teks
)

enum class AnnotateTool { PEN, FINE, WAVE, DECO, ERASER }
```

Annotate paths disimpan **terpisah** dari body text di Room DB. Saat render final (kirim/simpan), keduanya di-composite jadi satu gambar.

### Undo/Redo untuk Annotate

Undo di toolbar atas berlaku untuk kedua mode:
- Jika Annotate Mode OFF → undo teks
- Jika Annotate Mode ON → undo stroke terakhir

Stack undo dipisah per mode, tapi tombol UI-nya sama.

---

## 6. Overflow Menu (`···`)

Floating menu dark (seperti referensi iOS), muncul dari kanan toolbar.

```
┌──────────────────────────┐
│  📄  Ganti Kertas        │
│  ─────────────────────── │
│  🖼  Sisipkan Gambar     │
│  🎨  Pasang Stiker       │
│  ─────────────────────── │
│  💾  Simpan ke Foto      │
│  📤  Bagikan Surat...  › │
└──────────────────────────┘
```

| Menu Item | Aksi |
|---|---|
| Ganti Kertas | Buka Paper Picker (warna + template) |
| Sisipkan Gambar | Buka galeri, gambar ditempel sebagai layer draggable |
| Pasang Stiker | Buka Sticker Picker bottom sheet |
| Simpan ke Foto | Export surat sebagai PNG ke galeri |
| Bagikan Surat | Sub-menu: share sebagai gambar / link |

Menu ini dismiss saat tap di luar atau tap item.

> **Catatan implementasi:** dibangun sebagai floating dropdown (`OverflowMenuButton.kt`), persis seperti mockup di atas — bukan `ModalBottomSheet`. Anchored `Popup` yang unfurl dari ikon `···` (bukan slide dari bawah layar), dengan dark chrome `Surface` yang sama persis dengan `AnnotateColorPickerPopup` (`Color(0xFF2A241C)`, `RoundedCornerShape(16.dp)`, `shadowElevation = 12.dp`) supaya semua floating picker di layar ini terasa satu keluarga. Animasi masuk/keluar pakai `LetterlySpring.Gentle`/`Stiff` (scale + alpha, `TransformOrigin` di pojok kanan-atas dekat ikon trigger) mengikuti motion-rules.md §3.4, dan tiap item pakai `rememberPressScale` (no ripple) per §3.1/§4 — pola yang sama dengan `AnnotateColorButton`. Tanpa sub-menu/divider — 5 item flat. "Ganti Kertas" dan "Sisipkan Gambar" fungsional (Sisipkan Gambar buka Android Photo Picker via `ActivityResultContracts.PickVisualMedia`, tidak perlu permission storage). "Pasang Stiker"/"Simpan ke Foto"/"Bagikan Surat" masih `ComingSoonBottomSheet` stub.

---

## 7. Paper & Template Picker

Buka dari "Ganti Kertas" di overflow menu.

> **Catatan implementasi:** dibangun sebagai satu `LazyRow` (`PaperPickerSheet.kt`), bukan dua tab terpisah — sesuai keputusan produk untuk versi awal ini. Baris ini berisi 8 thumbnail tekstur `PaperTemplate` (`ui/theme/PaperTemplate.kt`) — **tanpa** swatch warna solid `PaperColor`, dibuang sengaja karena produk mau canvas selalu terasa seperti kertas surat sungguhan, bukan flat-color background. Thumbnail tekstur dirender rounded-rect 64×90dp, `ContentScale.Crop` dari asset asli 1240×1754px (rasio A4) di `res/drawable/`. `LetterCanvas.kt` merender tekstur terpilih full-bleed di atas warna dasar `PaperColor` (warna tetap jadi fallback background non-UI kalau asset punya transparansi) — lihat `ComposeViewModel.onPaperTemplateSelect`.
>
> 🚀 **Next Upgrade (Peny Store Collectibles)**: Lihat [docs/plan_composer_collectibles_picker.md](plan_composer_collectibles_picker.md) — sheet akan dikelompokkan menjadi **"Koleksi Dasar"** (8 template klasik) dan **"Koleksi Saya"** (kertas dari paket kreator yang dibeli). Jika user belum memiliki item kreator, bagian "Koleksi Saya" menampilkan preview showcase & tombol **"Beli Koleksi"** yang pre-filter ke Toko Kertas.

### Tab 1 — Warna Kertas

5 warna solid + preview real-time di thumbnail kertas kecil.

### Tab 2 — Template

Grid template ilustrasi Procreate. Setiap template adalah PNG transparent yang di-overlay di atas warna kertas.

**Kategori template batch pertama (6 template):**

| Template | Deskripsi | Cocok untuk |
|---|---|---|
| `floral_corner` | Bunga di sudut kiri atas | Surat personal, cinta |
| `minimal_border` | Border garis tipis elegan | Formal, korporat |
| `washi_tape` | Strip washi tape di tepi | Casual, scrapbook |
| `vintage_stamp_frame` | Frame cap pos vintage | Nostalgia, pen pal |
| `starry_margin` | Bintang-bintang kecil di margin | Malam hari, dreamy |
| `lined_dot` | Kombinasi garis + dot grid | Clean, modern |

Template bertambah terus — ini eventual template marketplace.

> **Catatan implementasi:** batch pertama yang benar-benar terpasang berbeda dari daftar di atas — 8 tekstur kertas penuh (bukan overlay dekoratif transparan): Grid, Lined, Indigo, Aged Kraft, Soft Beige, Dotted, Plain White, Blush Marble (lihat enum `PaperTemplate`). Daftar `floral_corner`/`minimal_border`/dst di atas tetap jadi referensi untuk batch template dekoratif berikutnya (overlay tipis di atas warna, bukan pengganti full-bleed).

---

## 8. Sticker Picker

Bottom sheet dengan grid stiker. Dua sumber:

1. **Built-in stickers** — PNG Procreate buatan Nio (Pilo si prangko, ekspresi, dekorasi)
2. **Emoji stickers** — emoji di-render sebagai stiker besar (64x64dp)
3. **Koleksi Kreator (Peny Store)** — stiker eksklusif dari paket kreator yang telah dibeli (lihat [docs/plan_composer_collectibles_picker.md](plan_composer_collectibles_picker.md)). Menyediakan showcase preview + CTA "Beli Koleksi" jika user belum memiliki stiker kreator.

Stiker yang ditempel jadi layer draggable di atas kertas. User bisa:
- **Drag** untuk pindah posisi
- **Pinch** untuk resize
- **Double tap** untuk hapus

```kotlin
data class StickerInstance(
    val id: String,
    val stickerId: String,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)
```

---

## 9. UiState & ViewModel

```kotlin
// feature/compose/ui/ComposeUiState.kt

data class ComposeUiState(
    // Konten surat
    val dearName: String = "",
    val bodyText: String = "",
    val signOff: String = "",

    // Styling
    val selectedFont: LetterFont = LetterFont.CAVEAT,
    val fontSize: Float = 15f,
    val textAlignment: TextAlign = TextAlign.Start,
    val inkColor: Color = Color(0xFF1F2937),

    // Paper & Template
    val paperColor: PaperColor = PaperColor.CREAM,
    val selectedTemplateId: String? = null,

    // Stickers
    val stickers: List<StickerInstance> = emptyList(),

    // Annotate
    val annotateState: AnnotateState = AnnotateState(),

    // Visibility
    val visibility: LetterVisibility = LetterVisibility.OPEN,
    val crisisTag: CrisisTag = CrisisTag.NONE,

    // UI state
    val isKeyboardVisible: Boolean = false,
    val isAnnotateMode: Boolean = false,
    val isOverflowMenuOpen: Boolean = false,
    val isFontPickerOpen: Boolean = false,
    val isTemplatePicker: Boolean = false,
    val isStickerPickerOpen: Boolean = false,
    val isSending: Boolean = false,
    val isDraft: Boolean = false,
    val error: String? = null,
)

enum class LetterFont(val fontFamily: FontFamily) {
    CAVEAT(CaveatFamily),
    KALAM(KalamFamily),
    ARCHITECTS_DAUGHTER(ArchitectsDaughterFamily),
    INDIE_FLOWER(IndieFlowerFamily),
    NOTHING_YOU_COULD_DO(NothingYouCouldDoFamily),
    SHADOWS_INTO_LIGHT(ShadowsIntoLightFamily),
}
```

> **Catatan implementasi P0:** file sebenarnya ada di `app/src/main/java/com/apps/unsealed/ui/screens/compose/ComposeUiState.kt` (bukan `feature/compose/ui/`) — mengikuti struktur package yang sudah ada di project ini (`ui/screens/*`), bukan struktur `feature/*` yang disebut di draft spec awal. Shape data class-nya sama persis, hanya lokasi file yang menyesuaikan konvensi repo.

> **Catatan implementasi — rich text (P0 lanjutan):** `isBold`/`isItalic` whole-letter di atas sudah **diganti** jadi per-seleksi (lihat §4). Field sebenarnya di `ComposeUiState` sekarang:
> - `bodyText: String` — tetap plain text aja, semua styling pindah keluar dari string ini.
> - `bodyStyleRuns: List<StyleRun>` — daftar rentang non-overlapping (selalu nutup penuh `[0, bodyText.length)`) yang bawa bold/italic/fontSize/indentLevel masing-masing. Didefinisikan di file baru `TextStyleRuns.kt` (package sama), yang juga isi fungsi-fungsi pure buat manipulasinya: `applyOverride` (restyle satu rentang, dipakai tombol Bold/Italic/size/indent), `afterTextChange` (reshuffle offset run pas teks berubah karena ngetik/hapus/paste), dan `buildStyledBody` (render `bodyText` + `bodyStyleRuns` jadi `AnnotatedString` asli yang di-render `BasicTextField`).
> - `bodySelection: TextRange` — posisi kursor/seleksi saat ini, di-hoist ke sini (bukan state lokal Compose) supaya ViewModel tau rentang mana yang harus di-restyle.
> - `pendingBold`/`pendingItalic`/`pendingFontSize: Boolean?/Float?` — kalau user toggle style pas kursor doang (nggak ada seleksi), nilainya nempel di sini dan berlaku ke karakter berikutnya yang diketik, bukan langsung ubah teks yang udah ada.
> - `fontSize: Float` masih ada, tapi sekarang cuma dipakai buat seed ukuran default karakter pertama yang diketik di surat kosong — begitu ada teks, ukuran per-karakter selalu lewat `bodyStyleRuns`/`pendingFontSize`.
> - `bodyAnnotated: AnnotatedString` (derived property) — `bodyText`+`bodyStyleRuns` yang udah di-render lewat `buildStyledBody`, ini yang langsung dikonsumsi `LetterCanvas`'s `BasicTextField`.
> - `effectiveBold`/`effectiveItalic`/`effectiveFontSize` (derived property) — style yang harus ditampilkan aktif di FormattingBar: `pending*` kalau ada, atau style di rentang seleksi/kursor saat ini.
>
> Indent (`indentLevel` di `StyleRun`) sengaja **bukan** field paragraf terpisah — dia numpang di sistem run per-karakter yang sama, cuma tombol Indent-nya yang selalu meluaskan rentang restyle ke satu paragraf penuh (`paragraphRangesTouching`) biar nggak motong satu baris jadi dua render paragraph terpisah (lihat `buildStyledBody`'s catatan soal `ParagraphStyle`).
>
> **Belum nyambung ke backend:** `bodyText` yang dikirim ke `POST /letters` masih plain string aja (styling di-strip) — lihat `docs/be/letters_api.md` dan `docs/todo.md` #2/#3 soal rencana nyalurin surat yang punya rich formatting lewat `composite_image_url` (flatten ke gambar), bukan nambah field terstruktur baru ke API.

---

## 10. Flow Kirim Surat

```
User tap ✈ SEND
    ↓
Validasi: bodyText.isNotBlank() && dearName.isNotBlank()
    ↓ (jika valid)
Confirm dialog: "Kirim surat ini?"
    ↓ (user konfirmasi)
Jika ada annotate paths → composite teks + annotasi → upload PNG ke Firebase Storage
Jika tidak ada annotate → simpan bodyText langsung ke Firestore
    ↓
Navigate ke Home + success animation (amplop terbang)
```

**Validasi error:**
- `dearName` kosong → shake field + toast "Surat untuk siapa nih?"
- `bodyText` terlalu pendek (< 20 char) → toast "Cerita dikit dong, jangan cuma salam 😄"
- `bodyText` terlalu panjang (> 2000 char) → realtime counter merah + disable send

---

## 11. Draft-on-Exit (dulu "Draft Auto-Save")

**Status: ✅ selesai diimplementasi.** Room jadi database lokal pertama di project ini (sebelumnya cuma ada satu `DataStore` sempit, `WelcomePreferences`, untuk satu boolean). Nama bagian ini diganti dari "Draft Auto-Save" — desain awal (debounce autosave berkala di background) **diganti** jadi trigger eksplisit saat user keluar dari Write (lihat "Trigger: exit dialog, bukan autosave berkala" di bawah), lebih simpel dan menghindari kompleksitas simpan koordinat gambar/annotate di background.

### Scope v1: teks-only

Draft v1 cuma nyimpen **teks + styling + pilihan font/kertas** — bukan gambar (`ImageInstance`), goresan Annotate (`AnnotateState`), atau stiker. Ini konsisten sama batas yang sudah dipakai `ComposeDraftHolder` (jembatan Compose → Send yang sudah ada sekarang, `core/data/ComposeDraftHolder.kt`) yang juga cuma bawa `bodyText`/`paperTemplate`/`fontId`/`paperColor`. Gambar/annotate/stiker nunggu fase 2, dibundel bareng kerjaan composite-image pipeline yang sudah di-track terpisah (`docs/todo.md` #2) — karena persist `ImageInstance` butuh persistable URI permission (`ContentResolver.takePersistableUriPermission`) dan `AnnotateState.paths` butuh serialisasi list `Offset` per goresan, kompleksitas yang sama yang bikin item #2 juga belum kelar.

### Data model — `DraftEntity` (Room)

| Field | Sumber (`ComposeUiState`) | Catatan |
|---|---|---|
| `draftId` | — | UUID, primary key |
| `recipientId` / `recipientName` | nav-arg `write/reply/{id}/{name}` | nullable — cuma keisi kalau draft dimulai dari jalur reply |
| `dearName` | `dearName` | |
| `bodyText` | `bodyText` | |
| `bodyStyleRunsJson` | `bodyStyleRuns: List<StyleRun>` | JSON via **Moshi** (`bodyStyleRunsJson: String`) — reuse dependency yang sudah dipakai buat network DTOs, bukan library JSON baru |
| `signOff` | `signOff` | |
| `selectedFont` | `selectedFont` | disimpan sebagai nama enum |
| `selectedGoogleFontName` | `selectedGoogleFontName` | nullable |
| `fontSize` | `fontSize` | |
| `textAlignment` | `textAlignment` | disimpan sebagai nama enum |
| `inkColor` | `inkColor` | packed `Long` (ARGB) |
| `paperColor` | `paperColor` | nama enum |
| `selectedPaperTemplate` | `selectedPaperTemplate` | nullable, nama enum |
| `visibility` | `visibility` | nama enum |
| `crisisTag` | `crisisTag` | nama enum |
| `createdAt` / `updatedAt` | — | epoch millis — `updatedAt` yang dipakai buat label "diedit X lalu" di `ProfileDraftScreen` dan urutan list |

### Trigger: exit dialog, bukan autosave berkala

**Keputusan produk (bukan lagi debounce background):** daripada autosave otomatis tiap beberapa detik, draft cuma ke-save saat user secara eksplisit memilih "Simpan sebagai Draft" di dialog center yang muncul pas user coba keluar dari Write dengan konten yang "berarti" (`bodyText.isNotBlank() || dearName.isNotBlank()`). Trade-off yang diterima: kalau app di-force-kill/crash di tengah nulis (bukan lewat back/tab-switch), teks yang belum sempat di-exit-dialog-in hilang — diterima demi kesederhanaan (ga perlu debounce watcher, ga ada write ke DB tiap beberapa detik).

**Copy dialog** ("Simpan Suratnya Dulu? 💌" / id `save_draft_dialog_*`, lihat `docs/copywriting/cp.md`): dua pilihan — **Simpan sebagai Draft** (upsert row) atau **Buang Suratnya** (no-op, ga nulis apa-apa ke DB). Tap di luar dialog / back saat dialog kebuka = batal, balik nulis lagi (tanpa tombol ketiga).

**Dua titik trigger, satu dialog** (implementasi: `WriteExitCoordinator` — `core/data/WriteExitCoordinator.kt`, `@Singleton`):
1. **System back press** saat di Write (tab-root atau `write/reply/{...}`/`write/draft/{...}`) — `BackHandler` lokal di `WriteRouteContent` (`UnsealedNavHost.kt`), karena `ComposeScreen` masih ke-compose pas back ditekan.
2. **Tap tab bottom-nav lain saat masih di Write** — trigger dari `MainActivity.kt`'s `bottomBar` lambda, scope composition yang beda sama `ComposeViewModel`. `WriteExitCoordinator` jadi jembatan cross-scope: `ComposeViewModel` register snapshot provider-nya saat `init`/unregister saat `onCleared()`, `MainActivity` baca state yang sama lewat `hiltViewModel<WriteExitViewModel>()` (thin wrapper buat narik singleton itu ke Compose scope manapun). Dialog-nya (`SaveDraftDialog`, wrapping `LetterlyCenterDialog` — center dialog pertama di codebase ini, sebelumnya semua `ModalBottomSheet`) di-mount sekali aja di `MainActivity.kt`'s `UnsealedApp`, karena itu satu-satunya scope yang pasti survive navigasi tab-switch.

### Lifecycle

1. **Belum ada row** sampai user pilih "Simpan sebagai Draft" di exit dialog — bukan lagi dicek terus-menerus lewat debounce, cuma dicek sekali saat dialog itu trigger (`hasUnsavedContent()` di `WriteExitCoordinator.Session`).
2. Save pertama yang lolos syarat #1 **create** row baru (UUID baru, di-generate di `ComposeViewModel.buildDraftEntity()`); save berikutnya di sesi yang sama **update** row yang sama (`currentDraftId` di-cache di ViewModel).
3. Saat surat berhasil terkirim (`SelectRecipientViewModel`'s send-success path, bareng `composeDraftHolder.clear()` yang sudah ada) → row draft-nya **dihapus** (kalau ada, dicek dari `ComposeDraft.draftId`), biar surat yang sama ga nyangkut jadi "draft" sekaligus "sudah terkirim".
4. **Continue Editing**: route `write/draft/{draftId}` (pola sama kayak `write/reply/{userId}/{name}`). `ComposeViewModel` pakai `SavedStateHandle` (pertama kali dipakai di ViewModel ini), baca `draftId`, load + deserialize row itu pas `init`, save berikutnya nulis ke row yang sama (bukan bikin baru).
5. **Hapus manual** dari Draft screen (§ di `docs/profile-screen-spec.md`) langsung hapus row-nya via `DraftRepository.delete()` — tanpa undo, dengan dialog konfirmasi terpisah (copy "Yakin Mau Dibuang? 🗑️" dari `docs/copywriting/cp.md`, reuse `LetterlyCenterDialog` yang sama dengan warna destructive).

**File-file kunci:** `core/database/{DraftEntity,DraftDao,UnsealedDatabase,DatabaseModule}.kt`, `feature/draft/data/DraftRepository.kt`, `core/data/WriteExitCoordinator.kt`, `ui/components/{LetterlyCenterDialog,WriteExitGuard}.kt`, `ui/screens/profile/ProfileDraftViewModel.kt`.

---

## 12. Checklist Implementasi

| Priority | Task |
|---|---|
| 🔴 P0 | `ComposeScreen.kt` — scaffold + layer stack |
| 🔴 P0 | `LetterCanvas.kt` — paper + text layer |
| 🔴 P0 | `ComposeToolbar.kt` — toolbar atas |
| 🔴 P0 | `FormattingBar.kt` — bar di atas keyboard |
| 🔴 P0 | `ComposeViewModel.kt` + `ComposeUiState.kt` |
| ✅ P1 | `FontPickerSheet.kt` — bottom sheet font |
| ✅ P1 | `OverflowMenuButton.kt` — dropdown icon+text anchored ke ikon `···` (Ganti Kertas + Sisipkan Gambar fungsional, sisanya stub) |
| ✅ P1 | `PaperPickerSheet.kt` — `LazyRow` warna kertas + 8 tekstur `PaperTemplate` |
| 🟢 P2 | Batch template dekoratif berikutnya (`floral_corner`/dst, overlay transparan bukan full-bleed) |
| 🟡 P1 | `StickerPickerSheet.kt` + sticker drag layer |
| 🟡 P1 | Auto-save draft ke Room |
| ✅ P2 | `AnnotateCanvas.kt` — DrawBox layer |
| ✅ P2 | `AnnotateToolbar.kt` — toolbar alat annotasi + color picker (`AnnotateColorPicker.kt`) |
| 🟢 P2 | Composite annotate + teks saat kirim |
| 🟢 P2 | Export surat sebagai PNG |

---

*Letterly Compose Spec v1.0 · Typed + Annotate Mode · Android · Jetpack Compose*
