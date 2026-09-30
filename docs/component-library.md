# Letterly — Component Library / Frontend Spec
*Reusable Compose UI · Android*

---

## Kenapa dokumen ini ada

Sebelumnya beberapa layar bikin styling sendiri-sendiri untuk kontrol yang sama secara konsep. Contoh nyata yang jadi pemicu dokumen ini: `AnnotateColorPicker.kt` (RGB channel sliders + opacity slider) sudah punya slider custom — no ripple, thumb bulat berwarna dengan border putih, scale feedback saat ditekan — tapi `FontPickerSheet.kt` (slider ukuran teks) masih pakai `Slider` bawaan Material3 apa adanya. Hasilnya: dua slider dengan visual dan feel yang beda di app yang sama, dan yang bawaan Material3 terasa "kayak slider Android" alih-alih Letterly.

**Aturan:** begitu sebuah kontrol UI dipakai di lebih dari satu tempat (atau kemungkinan besar akan dipakai lagi), ia jadi kandidat komponen shared di `ui/components/`, didokumentasikan di sini, dan setiap layar baru wajib pakai versi shared-nya — bukan menulis ulang styling-nya sendiri.

Lokasi source: `app/src/main/java/com/apps/unsealed/ui/components/`.
Motion/animation rules yang mendasari semua komponen di sini: `docs/motion-rules.md`.

---

## LetterlyButton & LetterlyOutlinedButton

`ui/components/LetterlyButton.kt`

Pengganti `androidx.compose.material3.Button` dan `OutlinedButton` di seluruh app.
Dipakai di: `RegisterScreen.kt` (tombol "Daftar"), dan **semua** CTA utama di onboarding / auth flow ke depannya.

### Kenapa bukan Button bawaan Material3

| | Material3 `Button` default | `LetterlyButton` |
|---|---|---|
| Shape | `RoundedCornerShape(50%)` bawaan M3 | `RoundedCornerShape(20.dp)` — rounded tapi tidak pill, kawai & konsisten |
| Press feedback | Ripple Material default | Scale 1.0 → 0.94 (Snappy) → 1.0 (Bouncy) via `rememberPressScale`, tanpa ripple |
| Warna | Bergantung `MaterialTheme.colorScheme.primary` dan bisa ditimpa M3 internal | `BrandGold` fill + `BrandInk` teks secara default, override via param |
| Loading state | Tidak ada — caller harus kelola sendiri | Built-in: `isLoading = true` menampilkan `CircularProgressIndicator` kecil |
| Font | Default typography Material | Selalu `NunitoFontFamily` Bold — tidak bisa meleset ke font fallback |

### API

```kotlin
@Composable
fun LetterlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = BrandGold,
    contentColor: Color = BrandInk,
    shape: Shape = RoundedCornerShape(20.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
)

@Composable
fun LetterlyOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = BrandGold,
    contentColor: Color = BrandGold,
    shape: Shape = RoundedCornerShape(20.dp),
)
```

| Param | Default | Catatan |
|---|---|---|
| `isLoading` | `false` | Saat `true`: teks diganti spinner, tombol tidak clickable. `enabled` tidak perlu di-set false secara terpisah. |
| `containerColor` | `BrandGold` | Override untuk varian permukaan gelap (misal: tombol di atas `BrandInkDeep`, pakai `BrandGold`). |
| `contentColor` | `BrandInk` | Warna teks, spinner, dan ikon. Wajib kontras dengan `containerColor`. |
| `shape` | `RoundedCornerShape(20.dp)` | Override hanya kalau konteks menuntut shape beda (mis. pill = `CircleShape`). Jangan ad-hoc per screen. |
| `leadingIcon` | `null` | Slot composable untuk ikon kiri (misal logo Google di `GoogleSignInButton`). |

### Contoh pakai

Tombol CTA utama (RegisterScreen):

```kotlin
LetterlyButton(
    text = stringResource(R.string.register_cta),
    onClick = { viewModel.register(nickname, continent) },
    enabled = nickname.isNotBlank() && continent.isNotEmpty(),
    isLoading = isLoading,
    modifier = Modifier.fillMaxWidth(),
)
```

Tombol dengan ikon (misal sign-in):

```kotlin
LetterlyButton(
    text = "Masuk dengan Google",
    onClick = { viewModel.signInWithGoogle(context) },
    isLoading = isLoading,
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = {
        Image(painterResource(R.drawable.ic_google_logo), null, Modifier.size(20.dp))
    },
)
```

Tombol sekunder (outlined):

```kotlin
LetterlyOutlinedButton(
    text = stringResource(R.string.onboarding_skip),
    onClick = onSkip,
    modifier = Modifier.fillMaxWidth(),
)
```

### Checklist sebelum nambah tombol baru

- [ ] Jangan import `androidx.compose.material3.Button` atau `OutlinedButton` di layar baru — pakai `LetterlyButton` / `LetterlyOutlinedButton`.
- [ ] Jangan set `shape = RoundedCornerShape(...)` inline di layar — itu tugas `LetterlyButton`.
- [ ] Gunakan `isLoading` bukan mengganti konten tombol sendiri.
- [ ] Kalau tombol butuh warna yang beda dari default (mis. tombol error/danger), override `containerColor` + `contentColor` — jangan buat komponen baru.

---

## LetterlySlider

`ui/components/LetterlySlider.kt`

Pengganti `androidx.compose.material3.Slider` di seluruh app. Dipakai oleh:

- `AnnotateColorPicker.kt` — 3× RGB channel slider (R/G/B) + 1× opacity slider
- `FontPickerSheet.kt` — slider ukuran teks ("Ukuran Teks")

### Kenapa bukan Slider bawaan Material3

| | Material3 `Slider` default | `LetterlySlider` |
|---|---|---|
| Ripple/state-layer saat ditekan | Ada | Tidak ada (dilarang, motion-rules.md §4) |
| Thumb | Lingkaran abu-abu polos | Lingkaran terisi warna (`trackColor` by default) + border putih 2dp |
| Feedback saat drag/press | Ripple + elevation bawaan | Scale 1.0 → 0.94 (Snappy) → kembali (Bouncy), lewat `rememberPressScale` |
| Reduced motion | Tidak disadari | Otomatis snap ke state akhir kalau reduced motion aktif |

### API

```kotlin
@Composable
fun LetterlySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.15f),
    thumbColor: Color = trackColor,
    thumbBorderColor: Color = Color.White,
)
```

| Param | Default | Catatan |
|---|---|---|
| `trackColor` | `Color.White` | Warna track aktif + warna isi thumb secara default. Biasanya diisi warna yang relevan secara semantik — warna channel RGB, warna base color untuk opacity, warna aksen tema untuk kontrol lain. |
| `inactiveTrackColor` | `White @ 15%` | Cocok untuk surface gelap (mis. popup color picker `Color(0xFF2A241C)`). **Di surface terang, override ini** — lihat contoh `FontPickerSheet` di bawah, pakai `trackColor.copy(alpha = 0.15f)` supaya track kosong tetap kebaca. |
| `thumbColor` | = `trackColor` | Override kalau warna thumb harus beda dari warna track (jarang dipakai sejauh ini). |
| `thumbBorderColor` | `Color.White` | Border tipis di sekeliling thumb supaya thumb tetap kebaca di atas track warna apa pun. |

### Contoh pakai

RGB channel slider (surface gelap, `AnnotateColorPicker.kt`):

```kotlin
LetterlySlider(
    value = value.toFloat(),
    onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
    valueRange = 0f..255f,
    trackColor = trackColor, // mis. Color(red = red, green = 0, blue = 0)
)
```

Slider ukuran teks (surface terang, `FontPickerSheet.kt`):

```kotlin
LetterlySlider(
    value = fontSize,
    onValueChange = onFontSizeChange,
    valueRange = ComposeFontSizeMin..ComposeFontSizeMax,
    trackColor = FontCardBorderSelected,
    inactiveTrackColor = FontCardBorderSelected.copy(alpha = 0.15f),
    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
)
```

### Checklist sebelum nambah slider baru

- [ ] Jangan import `androidx.compose.material3.Slider` langsung di layar baru — pakai `LetterlySlider`.
- [ ] Tentukan `trackColor` yang masuk akal secara semantik untuk konteksnya (bukan asal warna).
- [ ] Kalau slider ada di surface terang, override `inactiveTrackColor` — default-nya dioptimalkan untuk surface gelap.
- [ ] Jangan bikin `MutableInteractionSource` atau custom thumb sendiri — itu sudah ditangani di dalam `LetterlySlider`.

---

## rememberPressScale

`core/util/MotionUtils.kt`

```kotlin
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource): Float
```

Scale feedback standar (1.0 → 0.94 saat pressed via Snappy spring, kembali ke 1.0 via Bouncy spring) yang dipakai di **semua** elemen tappable di app — bukan cuma slider thumb. Sadar reduced-motion secara otomatis (lihat `docs/motion-rules.md` §5).

Awalnya ini private di `AnnotateColorPicker.kt` dan cuma dipakai di file itu (tombol warna, tab toggle, hex-copy row, grid swatch). Dipromosikan jadi public di `core/util/MotionUtils.kt` supaya `LetterlySlider` bisa pakai versi yang sama, dan supaya layar/komponen baru lain nggak perlu nulis ulang logic yang sama.

**Pakai ini, bukan nulis `animateFloatAsState` manual,** setiap kali sebuah elemen butuh feedback "ditekan" — tombol custom, card, chip, swatch, dll.

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val scale = rememberPressScale(interactionSource)

Box(
    modifier = Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null, onClick = { ... }),
)
```

---

## AnchoredDropdownMenu

`ui/components/AnchoredDropdownMenu.kt`

Shell dropdown gelap (dark chrome) yang anchored ke sebuah trigger — awalnya dibangun sekali untuk `OverflowMenuButton.kt` (compose-screen-spec.md §6), lalu dipakai lagi untuk menu aksi per-gambar di `LetterCanvas.kt` (`ImageActionsMenu`, lihat `docs/image-edit-mode-handoff.md`). Begitu dipakai dua kali, diekstrak ke sini sesuai aturan di atas.

Isi: `Popup` + animasi scale/alpha masuk (`LetterlySpring.Gentle`) dan keluar (`LetterlySpring.Stiff`/`tween`, lebih cepat) mengikuti motion-rules.md §3.4, `Surface` dark chrome (`Color(0xFF2A241C)`, `RoundedCornerShape(16.dp)`, `shadowElevation = 12.dp`) supaya semua floating dropdown di app ini terasa satu keluarga.

### API

```kotlin
@Composable
fun AnchoredDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    transformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
    width: Dp = 220.dp,
    content: @Composable () -> Unit,
)

@Composable
fun DropdownMenuAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier)
```

Caller cuma pegang satu boolean `expanded` (true = buka, false = minta tutup — misal dari `onClick` sebuah item, atau dari tap di luar via `onDismissRequest`). Mount/animate-in dan animate-out/unmount ditangani sepenuhnya di dalam komponen, jadi animasi keluar selalu selesai dulu sebelum `Popup`-nya benar-benar hilang dari composition — caller nggak perlu nge-track `isVisible` terpisah dari `isPresent` sendiri.

### Contoh pakai (`OverflowMenuButton.kt`)

```kotlin
var expanded by remember { mutableStateOf(false) }

IconButton(onClick = { expanded = true }) { /* ikon trigger */ }

AnchoredDropdownMenu(
    expanded = expanded,
    onDismissRequest = { expanded = false },
    offset = with(density) { IntOffset(0, (ButtonSize + DropdownGap).roundToPx()) },
) {
    DropdownMenuAction(Icons.Filled.Description, stringResource(R.string.overflow_menu_change_paper)) {
        expanded = false
        onChangePaperClick()
    }
    // ...item lain
}
```

---

## LetterlyStoreCard

`ui/components/LetterlyStoreCard.kt`

Kartu "judul + subjudul + gambar" untuk `StampsScreen.kt` (klaim hadiah, beli prangko, beli kertas, subscribe). Ini adalah generalisasi dari tiga implementasi kartu privat yang nyaris identik yang sudah ada duluan: `FontPickerCard` (`FontPickerSheet.kt`), `StampCard` (`StampPickerOverlay.kt`), dan `EnvelopeCard.kt` — begitu kebutuhan ke-4 muncul (kartu toko), sesuai aturan di atas, ditarik jadi satu komponen shared alih-alih nulis kartu privat ke-4.

Dua varian bentuk:

- **`StoreCardShape.Square`** — grid item (paket prangko, paket kertas). Bisa dipilih (`isSelected` menganimasikan border + check badge, sama seperti `FontPickerCard`/`StampCard`).
- **`StoreCardShape.Wide`** — hero banner full-bleed (klaim hadiah, subscribe). Rasio sama dengan `EnvelopeCard` (`EnvelopeAspectRatio`, 1720:900) supaya semua "kartu hero" di app terasa satu keluarga. Tidak selectable — komunikasikan status (mis. "Diklaim") lewat slot `badge`, bukan border.

### API

```kotlin
enum class StoreCardShape { Square, Wide }

@Composable
fun LetterlyStoreCard(
    shape: StoreCardShape,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isSelected: Boolean = false,          // cuma berlaku visual untuk Square
    badge: (@Composable BoxScope.() -> Unit)? = null,
    image: @Composable BoxScope.() -> Unit,
)
```

`image` sengaja berupa content-slot (bukan parameter `imageRes`/`imageUrl`) supaya caller sekarang (drawable bundled lewat `Image(painterResource(...))`, atau gambar prosedural macam `StampSwatch`) dan caller masa depan (`AsyncImage` Coil untuk gambar produk dari RevenueCat) sama-sama jalan tanpa komponen ini perlu diubah.

### Contoh pakai (`StampsScreen.kt`)

```kotlin
// Square — grid paket prangko
LetterlyStoreCard(
    shape = StoreCardShape.Square,
    title = stringResource(pack.titleRes),
    subtitle = stringResource(pack.subtitleRes, pack.priceLabel),
    onClick = { viewModel.onBuyStampPackClick(pack.id) },
    modifier = Modifier.width(116.dp),
    image = { StampSwatch(pack.stampDesign, Modifier.fillMaxSize()) },
)

// Wide — banner klaim hadiah, status lewat badge
LetterlyStoreCard(
    shape = StoreCardShape.Wide,
    title = stringResource(reward.titleRes),
    subtitle = stringResource(reward.subtitleRes),
    onClick = { if (!isClaimed) onClaimClick() },
    image = { /* ikon di atas gradient */ },
    badge = { /* pill "Klaim" / "Diklaim" */ },
)
```

### Catatan asimetri

`isSelected`/border cuma berlaku kalau `shape == Square` — untuk `Wide`, parameter itu diabaikan secara visual. Ini disengaja (bukan bug): ~90% resep kartu (clip, background/image, kolom judul/subjudul, press feedback, slot badge) tetap sama di kedua varian, jadi tidak perlu dipecah jadi dua komponen terpisah — cukup didokumentasikan di sini.

---

## LetterlyMenuRow

`ui/components/LetterlyMenuRow.kt`

Row ikon + judul (+ subjudul opsional) + trailing slot (default chevron kanan) — dipakai berulang kali di seluruh fitur Profile (`ProfileScreen.kt`'s 5-menu list, `ProfileSettingsScreen.kt`'s 6 baris pengaturan). Ini adalah "list item" generik pertama di app yang dipromosikan jadi shared component sejak awal (bukan diekstrak belakangan dari duplikasi), karena polanya sudah jelas bakal dipakai berkali-kali dalam satu fitur.

Badge ikon lingkaran 40dp (`Color.White @ 12%`), row background `Color.White @ 8%`, `RoundedCornerShape(16.dp)`, scale-only press feedback via `rememberPressScale` — tanpa ripple, sama seperti komponen shared lain.

### API

```kotlin
@Composable
fun LetterlyMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = Color.White,
    trailing: @Composable () -> Unit = { /* chevron-right, White @ 40% */ },
)
```

### Contoh pakai (`ProfileScreen.kt`)

```kotlin
LetterlyMenuRow(
    icon = Icons.Filled.Drafts,
    title = stringResource(R.string.profile_menu_draft),
    onClick = onDraftClick,
)
```

`trailing` bisa di-override kalau baris butuh sesuatu selain chevron (belum ada contoh pakai sejauh ini, tapi disiapkan supaya baris seperti toggle switch di masa depan tidak perlu komponen baru).

---

## LetterlyScreenHeader

`ui/components/LetterlyScreenHeader.kt`

Back arrow + judul, dipakai di kelima stack-only sub-screen Profile (`ProfileDraftScreen.kt`, `ProfileAddressBookScreen.kt`, `ProfileStampBookScreen.kt`, `ProfileTipsScreen.kt`, `ProfileSettingsScreen.kt`). Generalisasi dari resep `Header` privat di `SelectRecipientScreen.kt` (back arrow dengan scale-only press feedback, tanpa ripple) — dipotong jadi versi minimal (cuma back + judul, tanpa action button/subtitle) karena kelima sub-screen Profile nggak butuh elemen ekstra itu.

### API

```kotlin
@Composable
fun LetterlyScreenHeader(title: String, onBackClick: () -> Unit, modifier: Modifier = Modifier)
```

### Contoh pakai

```kotlin
LetterlyScreenHeader(title = stringResource(R.string.profile_draft_title), onBackClick = onBackClick)
```

Pakai satu string bersama `profile_back_desc` ("Back"/"Kembali") untuk `contentDescription` back arrow di semua caller.

---

## staggerEntrance

`core/util/MotionUtils.kt`

```kotlin
@Composable
fun Modifier.staggerEntrance(index: Int): Modifier
```

Modifier stagger-entrance per motion-rules.md §3.7: translateX -20dp → 0 + fade, 40ms per running `index`, sadar reduced-motion. Awalnya private di `StampsScreen.kt`, dipromosikan jadi public di sini begitu `ProfileScreen.kt` dan sub-screen Profile butuh recipe yang sama persis — sesuai aturan ekstraksi di atas (2+ pemakai). Index yang dipakai biasanya index item dalam list/grid (bukan running counter lintas section, kecuali screen itu memang ingin satu cascade berkelanjutan seperti `StampsScreen.kt`).

```kotlin
items(menuEntries.size) { index ->
    LetterlyMenuRow(..., modifier = Modifier.staggerEntrance(index))
}
```

---

## Komponen lain di `ui/components/`

| File | Isi |
|---|---|
| `BottomNavBar.kt` | Bottom navigation bar (PenPals/Inbox/Write/Stamps/Profile) — iOS-style tab bar. |
| `LetterlySlider.kt` | Lihat di atas. |
| `AnchoredDropdownMenu.kt` | Lihat di atas. |
| `LetterlyStoreCard.kt` | Lihat di atas. |
| `LetterlyMenuRow.kt` | Lihat di atas. |
| `LetterlyScreenHeader.kt` | Lihat di atas. |
| `LetterlyCenterDialog.kt` | Center `Dialog` pertama di codebase ini (semua modal lain sebelumnya `ModalBottomSheet`) — mascot + title bold + body abu-abu + `LetterlyButton`/`LetterlyOutlinedButton` pair, `LetterlySpring.Gentle` masuk / `Stiff` keluar (150ms delay biar exit animation kelar dulu sebelum callback beneran jalan). Dipakai di `SaveDraftDialog` (Draft-on-Exit), konfirmasi hapus draft (`ProfileDraftScreen.kt`), dan `OpenLetterOverlay.kt`'s reply-confirm dialog. |
| `ReportMailDialog.kt` | Reason-picker center dialog buat report konten — reuse resep `Dialog`/`Surface(20.dp)`/motion `LetterlyCenterDialog`, tapi body-nya reason list (styling mirip `RegionPickerSheet`'s `RegionRow`, bukan Material `RadioButton`) + `OutlinedTextField` catatan opsional, bukan cuma string. Dipakai pertama kali di PenPals (`OpenLetterOverlay.kt`'s "···" → Report Mail); Inbox's `OpenLetterScreen.kt` rencananya reuse yang sama (`docs/be/trust_and_safety_api.md` §1). |

---

## Prinsip umum komponen shared

1. **Satu slider, satu tombol, satu style feedback press** — lihat `docs/motion-rules.md` §1 ("Consistent weight") dan §4 ("Hal yang Dilarang": jangan pakai ripple Material default, jangan pakai animasi beda-beda untuk komponen sejenis).
2. Kalau nemu dua tempat yang re-implement kontrol yang sama (kayak kasus slider ini), extract ke `ui/components/`, hapus implementasi lokal yang lama, dan update dokumen ini.
3. String yang user-facing tetap wajib lewat `strings.xml` (lihat `docs/string-resources.md`) — bukan hardcoded, termasuk di komponen shared.
4. Semua parameter warna/style di komponen shared punya default yang masuk akal buat use case pertama, tapi tetap bisa di-override — jangan hardcode warna yang cuma cocok untuk satu context tanpa jalan keluar.

---

*Letterly Component Library v1.0 · Jetpack Compose*
