iya 
# PenPals Home Screen — Implementation Spec
*Dummy front-end pass · Android · Jetpack Compose*

---

## Status

| Bagian | Status | Catatan |
|---|---|---|
| Nav wiring | ✅ | `Destinations.PenPals` (route `"penpals"`, icon `Icons.Filled.Public`, first di `entries`), `UnsealedNavHost.kt` route ke `PenPalsScreen()`. `startDestination` tetap `Inbox`. |
| Real data | ✅ | `ui/screens/penpals/PenpalLetter.kt` — `PenpalLetter` domain model, `PenPalsViewModel` fetch dari backend (`FeedItemDto`→`toPenpalLetter()`), reuse `PenpalRegion`/`EnvelopeDesign` dari `.selectrecipient`. |
| Strings | ✅ | Block "PenPals" di `strings.xml` (lihat §5). |
| `PenPalsScreen.kt` | ✅ | **Hybrid Adaptive View** — default Loose Desk (API ≥ 28) atau Grid (API ≤ 27), user-toggleable via header switch. Capped 6 surat terbaru. Detail di §2 & §2.6. |
| Paywall Prompt | ✅ | `ArchiveBottomCard` ("Lihat Semua Surat Lama 🔒") + `PeniPremiumBottomSheet.kt` pas surat > 6. |

**Catatan penting — riwayat arsitektur:** dokumen ini sudah beberapa kali ganti arsitektur, semuanya hasil iterasi live bareng user, bukan draft awal:
1. **v1.0** — desain awal "scrollable `LazyColumn` overlap statis", dibuang karena scrollable range-nya kebatas total content height, nggak bisa jamin "selalu ada tepat satu kartu fokus" kalau item-nya dikit.
2. **v2.x** — "custom drag-driven focus stack" (`Modifier.draggable` 1D + `Animatable<Float>` sebagai "continuous focus index", satu kartu fokus/full-expand, sisanya antri collapsed di belakang).
3. **v3.0** — **"loose desk cards"**: v2's linear stack diganti total jadi postcard yang berserakan bebas (2D) di atas meja kerja Peni, tiap kartu independen posisinya, draggable ke mana aja, lift-to-top-of-pile saat digrab. Nggak ada lagi konsep "focus index" — setiap kartu punya posisi X/Y/rotasi sendiri yang persist sampai user drag lagi. Background layar sempat diganti lokal jadi gradient terang (`IceSkyBlue`→`SurfaceCream`) — **ini keliru dan sudah di-revert di v3.1** (lihat poin 4), karena ternyata dobel-nutupin `bg_texture` meja hijau yang MainActivity udah render secara global.
4. **v3.1** — dua perubahan: (a) **background lokal dihapus**, balik pakai `bg_texture` global dari `MainActivity.kt` (meja hijau) — teks/pill balik ke skema putih-di-atas-gelap yang lama; (b) **focus mode ditambahkan** — tap kartu nggak langsung buka surat lagi, tapi "diangkat" ke tengah layar dulu (scale up, rotasi diluruskan, sisanya di-dim), baru tap kedua yang beneran buka `OpenLetterOverlay`. Detail di §2.5.
5. **v3.2 (sekarang)** — **Hybrid Adaptive View + Paywall Limit**: (a) Mode render adaptif OS: API ≤ 27 default ke 2-column `Grid`, API ≥ 28 default ke `LooseDesk`. Header menyediakan `ViewModeToggle` agar user bisa bebas beralih mode. (b) Feed dibatasi maksimal 6 surat terbaru (`postedAt` desc). Jika total surat > 6, `ArchiveBottomCard` ("Lihat Semua Surat Lama 🔒") dirender sebagai kartu terkunci (peeking out di LooseDesk, trailing row di Grid) yang memicu `PeniPremiumBottomSheet`.

Dokumen ini sekarang jadi source-of-truth yang match kode aktual v3.2, bukan pre-implementation draft.

---

## Context

Dua mockup referensi awal (`app/src/main/java/com/apps/unsealed/ui/screens/home.png`, `home_versi_2.png`) — masih relevan buat `OpenLetterOverlay` (§3), tapi **bagian stack amplop di `home.png` sudah nggak dipakai** (diganti loose desk cards, lihat riwayat arsitektur di atas):
- **home.png** — background wood texture, title "PenPals" + avatar bulat kanan atas, search bar pill, dan stack amplop bertumpuk gaya iOS Wallet. *(Stack + search bar bagian ini sudah digantikan seluruhnya — lihat §2/§2.3.)*
- **home_versi_2.png** — tap amplop → full-screen kertas bergaris, tombol close (X), body teks (scrollable), rail ikon vertikal (like/comment/bookmark/share) di kanan, bottom bar translucent isi avatar+nama+dot online+region+"Posted on {tanggal}"+"···". *(Masih akurat, lihat §3.)*

Keputusan final v3.1:
- Tab "PenPals" di bottom nav (bukan ganti tab `Inbox`).
- **Loose desk cards** — tiap `PenpalLetter` jadi satu postcard 240dp lebar yang di-scatter otomatis (posisi + rotasi acak tapi stabil per id) di area meja di bawah header, bisa di-drag bebas ke mana aja, dan naik ke lapisan paling atas begitu digrab (lihat §2).
- **Search bar dihapus total** (dummy, nggak pernah ada filtering logic beneran).
- **Background layar** = `bg_texture` global dari `MainActivity.kt` (meja hijau, `R.drawable.device`), **tidak di-override lokal di layar ini** — sempat dicoba pakai gradient terang lokal, tapi itu cuma dobel-nutupin background meja yang udah ada, jadi di-revert (lihat §2.3).
- **Diralat dari draft awal:** kartu envelope **bukan** murni gambar tanpa teks. Nama pengirim + "A Penpal in {region}" dirender sebagai bagian dari `EnvelopeCard` (`EnvelopeHeaderRow`/`RecipientAddressLabel`) dan ikut ke-composite jadi satu WebP bareng background/stamp/sticker (`envelope_composite_image_url`, lihat `docs/envelope-sticker-spec.md`/`docs/be/envelope_customization_api.md`) — ini disengaja, bukan bug: kalau kartu cuma gambar amplop polos, susah nentuin posisi X/Y elemen (nama, stamp, sticker) yang konsisten antara preview compose vs. hasil composite. Kalau device pengirim di bawah `MinCompositingDensity` (composite `null`), sisi penerima **reconstruct teks yang sama** dari field terstruktur (`senderName`, `region`, `stamp`, `envelope_sticker_id`) yang tetap dikirim apa adanya — lihat `DeskCard.kt`. `OpenLetterOverlay`'s `SenderInfoBar` tetap render info sender lagi (dengan tambahan avatar/dot online/tanggal) begitu surat dibuka — bukan pengganti, pelengkap dengan detail lebih lengkap.
- **Focus mode** — tap kartu yang belum fokus nggak langsung buka surat; kartu itu "diangkat" ke tengah layar (scale 1→1.1, rotasi diluruskan ke 0°), sisanya + header di-dim di belakang scrim. Tap lagi di kartu yang lagi fokus baru beneran buka `OpenLetterOverlay`; tap di luar kartu (scrim) atau tombol back ngembaliin kartu ke posisi semula di meja. Detail di §2.5.
- Transisi amplop → surat = `OpenLetterOverlay`, slide-up-from-bottom + fade, ikutin §3 di bawah — **tidak berubah** dari versi sebelumnya, termasuk kalau kartunya sekarang scattered bukan stacked. Bedanya cuma trigger-nya sekarang tap kedua (dari state focus), bukan tap pertama langsung.
- **Mode surat terbuka bisa di-scroll ke surat berikutnya** — begitu satu surat kebuka, user tinggal scroll/drag vertikal buat pindah ke surat lain. Detail di §3. *(Catatan v3.0: dulu paging ini juga men-sync balik posisi "fokus" di stack lewat `onPageChanged` — di loose desk nggak ada lagi konsep fokus stack, jadi sync itu dihapus, lihat §3. Catatan: "focus" di sini beda konsep dari "focus mode" v3.1 — yang lama itu index fokus di linear stack, yang baru itu state zoom-preview per kartu.)*
- Nggak pakai ViewModel buat state kartu — posisi/rotasi/z-index/focus tetap `remember { ... }` di level `PenPalsScreen`, sama seperti sebelumnya.

---

## 1. Reuse checklist (jangan bikin ulang)

- `EnvelopeDesign` — `ui/screens/selectrecipient/EnvelopeDesign.kt`. `EnvelopeAspectRatio` (1720:900, rasio asli asset) dipakai buat nurunin tinggi kartu desk dari `DeskCardWidth` (§2) — **jangan diubah**, dipakai juga di `EnvelopeCard.kt`/`SendSuccessOverlay.kt`/`EnvelopePickerSheet.kt`.
- **`DeskCardWidth = 240.dp`** — bukan asset/util baru, tapi angka yang sengaja disamain dengan konvensi lebar kartu amplop yang sudah dipakai di `EnvelopeCard.kt` (`ui/screens/selectrecipient/`), biar postcard di desk ini konsisten ukurannya dengan kartu amplop di tempat lain.
- `PenpalRegion` — `ui/screens/selectrecipient/PenpalRegion.kt`
- `PenpalLetter` — `ui/screens/penpals/PenpalLetter.kt`, dipetakan dari `FeedItemDto` (backend) lewat `toPenpalLetter()`.
- `LetterlySpring`, `reducedMotionSpring`, `rememberIsReducedMotion`, `rememberPressScale` — `core/util/`
- `CaveatFontFamily`, `InkDefault`, `ToolbarFrostedDark`, `LetterBodyStyle` — `ui/theme/`, dipakai di `OpenLetterOverlay` **dan** sekarang lagi di `PenPalsHeader`/error state juga (§2.3) — dark chrome di atas `bg_texture` global, sama seperti sebelum v3.0.
- `androidx.compose.ui.util.lerp` — dipakai buat interpolasi posisi/rotasi kartu antara posisi meja dan posisi tengah layar pas focus mode (§2.5), bukan util custom.
- `PaperTemplate.LINED` (drawable `kertas_putih_garis`) — `ui/theme/PaperTemplate.kt`, background kertas bergaris di `OpenLetterOverlay`.
- `ComingSoonBottomSheet(title, onDismiss)` — `ui/screens/compose/ComingSoonBottomSheet.kt`, dipakai buat semua aksi dummy (like/comment/bookmark/share/···) di dalam `OpenLetterOverlay`.
- Asset amplop `envelope_1..5.webp` — udah ada di `res/drawable/` + `res/drawable-widecg/`.

---

## 2. `ui/screens/penpals/PenPalsScreen.kt`

### Arsitektur: loose desk cards (2D freeform, bukan stack)

Setiap `PenpalLetter` dapet satu `DeskCardState` — posisi X/Y (`Animatable<Float>`), rotasi stabil (`Float`, di-seed sekali), dan z-index (`MutableFloatState`, naik tiap digrab):

```kotlin
private class DeskCardState(
    val offsetX: Animatable<Float, AnimationVector1D>,
    val offsetY: Animatable<Float, AnimationVector1D>,
    val rotationZ: Float,
    val zIndex: MutableFloatState,
)
```

**Kenapa `mutableStateMapOf` yang cuma nambah entry, bukan `remember(letters) { letters.map { ... } }`:** kalau `cardStates` dibangun ulang tiap `letters` ganti instance list (misalnya abis `toggleLike()` dari dalam `OpenLetterOverlay`, yang emit list baru dengan `likeCount` ke-update), posisi kartu yang udah di-drag user bakal ke-reset balik ke posisi scatter awal. Solusinya: map persist selama layar hidup, `LaunchedEffect(letters, ...)` cuma nambah entry buat id yang belum ada, nggak pernah nge-replace entry yang udah ada.

```kotlin
val cardStates = remember { mutableStateMapOf<String, DeskCardState>() }
val topZIndex = remember { mutableFloatStateOf(letters.size.toFloat() + 1f) }

LaunchedEffect(letters, deskWidthPx, deskHeightPx, scatterTopPx) {
    letters.forEachIndexed { index, letter ->
        if (!cardStates.containsKey(letter.id)) {
            val seed = letter.id.hashCode()
            val offset = deskScatterOffset(seed, deskWidthPx, scatterAreaHeightPx, cardWidthPx, cardHeightPx)
            cardStates[letter.id] = DeskCardState(
                offsetX = Animatable(offset.x),
                offsetY = Animatable(scatterTopPx + offset.y),
                rotationZ = deskScatterRotation(seed),
                zIndex = mutableFloatStateOf(index.toFloat() + 1f),
            )
        }
    }
}
```

### 2.1 Scatter formula — `deskScatterOffset`/`deskScatterRotation`

Posisi & rotasi awal tiap kartu **deterministik dari `letter.id.hashCode()`** (bukan `Random` global tanpa seed) — jadi re-populate map setelah recompose yang nggak terkait tetap menghasilkan posisi identik buat kartu yang belum pernah disentuh user. Distribusi uniform-disk (`sqrt(random)` buat radius fraction, bukan radius linear) biar kartu nggak numpuk ke tengah:

```kotlin
internal fun deskScatterOffset(
    seed: Int, areaWidthPx: Float, areaHeightPx: Float,
    cardWidthPx: Float, cardHeightPx: Float,
): Offset {
    val rnd = Random(seed)
    val angle = rnd.nextFloat() * (2 * PI).toFloat()
    val radiusFraction = sqrt(rnd.nextFloat())
    val maxRx = ((areaWidthPx - cardWidthPx) / 2f).coerceAtLeast(0f)
    val maxRy = ((areaHeightPx - cardHeightPx) / 2f).coerceAtLeast(0f)
    // ... center + cos/sin(angle) * maxR * radiusFraction, coerced ke area
}

internal fun deskScatterRotation(seed: Int): Float =
    Random(seed).nextFloat() * 12f - 6f // -6f..6f
```

Kedua fungsi ini **dipakai bareng oleh `DeskCard` (kartu asli) dan `EnvelopeStackShimmer` (loading skeleton)** — seed shimmer = index kartu (belum ada id asli saat loading) — supaya posisi shimmer dan kartu asli identik persis, zero layout shift (lihat §2.4).

Area scatter: dari `headerHeightDp + DeskScatterTopGap (24.dp)` sampai bawah layar. Nggak perlu reserve manual buat bottom nav — `MainActivity.kt` udah `Modifier.padding(innerPadding)` ke seluruh `NavHost`, jadi `fillMaxSize()` layar ini emang udah area yang aman dari nav bar.

### 2.2 `DeskCard` — gesture, z-index-on-grab, dan draw-phase perf

**Constants (top of file):**
```kotlin
private val DeskCardWidth = 240.dp
private val DeskScatterTopGap = 24.dp
private val DeskCardRestElevation = 2.dp
private val DeskCardLiftedElevation = 14.dp
private val DeskCardFocusedElevation = 28.dp
private const val DeskCardRestScale = 1f
private const val DeskCardLiftedScale = 1.03f
private const val DeskCardFocusedScale = 1.1f
private const val DeskRotationRangeDeg = 6f

// Focus mode (§2.5)
private const val DeskDimmedAlpha = 0.4f
private const val DeskScrimAlpha = 0.4f
private const val DeskHeaderZIndex = 1000f
private const val DeskFocusScrimZIndex = 1100f
private const val DeskFocusedCardZIndex = 1200f
// OpenLetterOverlay pakai zIndex(2000f) — di atas semuanya, lihat §3.
```

Semua nilai yang berubah tiap frame drag (`translationX/Y`, `rotationZ`, `scaleX/Y` dari lift feedback, `shadowElevation`) dibaca **di dalam `graphicsLayer {}`** (Draw phase, RenderThread) — bukan di Layout phase kayak `.offset { IntOffset(...) }` yang dipakai `EditableImage.kt` (`ui/screens/compose/`, layer draggable buat gambar yang di-insert user). Beda pendekatan ini disengaja: `EditableImage` cuma pernah punya SATU foto yang lagi di-gesture dalam satu waktu, jadi Layout-phase read-nya nggak masalah; layar PenPals ini sebelumnya (v2, stack) udah pernah diprofilingdan Layout-phase read jadi sumber lag nyata di device API 27 fisik (lihat §7 lama, prinsipnya masih dipertahankan meski detail masalahnya udah nggak relevan di v3). Praktiknya di v3 ini jauh lebih longgar dari v2 — cuma SATU kartu yang lagi di-drag yang mutasi tiap frame, kartu lain diem statis — tapi disiplin Draw-phase-only tetap dipertahankan.

```kotlin
Box(
    modifier = Modifier
        .size(cardWidth, cardHeight)
        .graphicsLayer {
            // focusProgress 0→1 interpolates position/rotation from the desk
            // spot to screen center — see §2.5.
            translationX = lerp(state.offsetX.value, focusCenterX, focusProgress)
            translationY = lerp(state.offsetY.value, focusCenterY, focusProgress) + entranceOffsetYPx
            rotationZ = lerp(state.rotationZ, 0f, focusProgress)
            scaleX = liftScale; scaleY = liftScale       // rest/lifted/focused, see below
            alpha = entranceAlpha * dimAlpha              // dimAlpha < 1 when another card is focused
            shadowElevation = elevationPx
            shape = cardShape; clip = true
        }
        .zIndex(if (isFocused) DeskFocusedCardZIndex else state.zIndex.floatValue)
        .then(
            if (!isAnyFocused) {
                Modifier.pointerInput(letter.id) {
                    detectDragGestures(
                        onDragStart = { isDragging = true; onDragStart() },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (state.offsetX.value + dragAmount.x)
                                .coerceIn(0f, (deskWidthPx - cardWidthPx).coerceAtLeast(0f))
                            val newY = (state.offsetY.value + dragAmount.y)
                                .coerceIn(scatterTopPx, (deskHeightPx - cardHeightPx).coerceAtLeast(scatterTopPx))
                            scope.launch { state.offsetX.snapTo(newX) }
                            scope.launch { state.offsetY.snapTo(newY) }
                        },
                    )
                }
            } else Modifier, // dragging disabled entirely while any card is focused
        )
        .then(
            if (canInteract) { // !isAnyFocused || isFocused — dimmed cards ignore taps
                Modifier.clickable(interactionSource, indication = null) {
                    if (isFocused) onRequestOpen() else onRequestFocus()
                }
            } else Modifier,
        ),
)
```

`liftScale`/`elevationPx` sekarang 3-arah (`when { isFocused -> ...Focused; isDragging -> ...Lifted; else -> ...Rest }`), bukan cuma 2-arah kayak sebelum ada focus mode — detail lengkap di §2.5.

**Clamp saat drag** (`deskWidthPx`/`deskHeightPx`/`scatterTopPx` dari `BoxWithConstraints` parent, `cardWidthPx`/`cardHeightPx` dari `DeskCardWidth`/`EnvelopeAspectRatio`): pojok kiri-atas kartu nggak pernah bisa didrag keluar area meja (nggak bisa ke bawah header, nggak bisa keluar bawah/samping layar). Ini beda dari sketsa awal yang nggak punya bound sama sekali — di layar ini nggak ada scroll, jadi kartu yang ke-drag keluar viewport bakal ilang selamanya tanpa clamp. Prinsipnya sama kayak `EditableImage.kt`'s clamp-ke-canvas (clamp titik acuan, biarin tepi kartu boleh sedikit nongol), cuma di sini yang di-clamp pojok kiri-atas (bukan center) karena representasi posisinya emang top-left offset, bukan center offset.

**z-index-on-grab** (dari `PenPalsScreen`, di-pass sebagai `onDragStart` callback ke `DeskCard`):
```kotlin
onDragStart = {
    topZIndex.value += 1f
    state.zIndex.value = topZIndex.value
    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
}
```
`zIndex` disimpan sebagai field per-kartu (`MutableFloatState`), bukan `Map<String, Float>` level-screen yang di-`toMutableMap().apply{}` ulang tiap grab — biar cuma kartu yang digrab yang recompose, bukan semua reader dari satu map bersama.

**Lift feedback saat drag** (`isDragging: Boolean` lokal per kartu → `animateFloatAsState(LetterlySpring.Snappy)` buat `scale` 1↔1.03 dan `shadowElevation` 2dp↔14dp) — pola yang sama persis kayak `pressScale` di kartu versi lama, cuma triggernya sekarang `isDragging` bukan `isPressed`.

**Posisi setelah dilepas:** dibiarkan apa adanya (nggak ada spring-settle tambahan) — opsi paling simpel yang emang udah cukup, konsisten sama clamp yang udah jalan duluan waktu masih di-drag.

### 2.3 Header & background — balik ke `bg_texture` global (v3.1)

**Riwayat singkat:** waktu loose desk cards pertama kali dibikin (v3.0), layar ini sempat dikasih background lokal sendiri (`Brush.verticalGradient(IceSkyBlue→SurfaceCream)`, child pertama di root `Box`) dengan asumsi butuh tampilan "meja terang" yang beda dari wood texture gelap `MainActivity.kt`. **Ini keliru** — `MainActivity.kt` udah render `bg_texture` (`R.drawable.device`, meja **hijau**, bukan wood gelap seperti asumsi awal) secara global buat semua route in-app, termasuk PenPals. Background gradient lokal itu cuma dobel-nutupin meja hijau yang udah ada. Di v3.1, background lokal itu **dihapus total** — layar ini sekarang murni transparan, `bg_texture` global yang kelihatan di belakangnya, sama kayak semua layar in-app lain (Inbox/Write/Stamps/Profile).

Karena background balik gelap, teks/pill header & error state juga **balik ke skema putih-di-atas-gelap** yang lama (bukan `BrandInk`/`SurfaceCardLight` yang sempat dipakai di v3.0):

`PenPalsHeader(userName: String, onProfileClick, modifier)` — `Column` dua baris:
- **Baris 1**: `Text("Halo, {Nama}! 👋", headlineMedium, Bold, Color.White)` + `Spacer(weight(1f))` + avatar bulat 40dp — pill `ToolbarFrostedDark`, ikon `Icons.Filled.Person` tint `Color.White`.
- **Baris 2**: title state (`penpals_greeting_default_title`, `titleLarge`, Bold, `Color.White`) + body state (`penpals_greeting_default_body`, `bodyMedium`, `Color.White.copy(alpha = 0.75f)`).

Error/retry state di bawah shimmer/kartu: teks error → `Color.White.copy(alpha = 0.8f)`, pill Retry → background `ToolbarFrostedDark` + teks `Color.White`.

`userName` resolve-nya nggak berubah dari sebelumnya: `authViewModel.uiState` → `AuthUiState.Authenticated`/`NeedsOnboarding` → `user.nickname`, selain itu → `R.string.penpals_greeting_guest_name` ("Tamu"/"Guest").

**Tech debt — state-aware greeting belum lengkap:** baris 2 di atas selalu nampilin state "Inbox Kosong / Default" (`Siap Berseluncur Hari Ini? 🐧`). Desain lengkap punya 2 state lain (surat baru mendarat / surat dalam perjalanan) yang butuh sinyal inbox real yang belum di-wire ke screen ini — detail & copy lengkap ada di `docs/todo.md`.

### 2.4 `EnvelopeStackShimmer` (`EnvelopeShimmer.kt`) — scattered, bukan stack lagi

Loading skeleton di-render pakai **`deskScatterOffset`/`deskScatterRotation` yang sama** dengan kartu asli (seed = index kartu, karena belum ada `letter.id` beneran saat loading), jadi begitu data asli datang, kartu asli langsung nempatin diri di posisi yang sama persis kayak shimmer-nya — zero layout shift, prinsip yang sama kayak versi stack lama, cuma geometrinya sekarang scatter bukan ramp/step vertikal. Warna shimmer (`SurfaceCardLight`/`SurfaceCream`/sepia edge) tetap dipakai murni sebagai palet shimmer itu sendiri (nggak terkait background layar).

### 2.5 Focus mode (v3.1) — tap-to-zoom sebelum buka surat

**Kenapa ditambahkan:** di loose desk, kartu bisa ada di mana aja dan saling numpuk — user gampang salah tap kartu yang ketiban terus langsung nyasar buka `OpenLetterOverlay` yang salah. Focus mode kasih "jeda konfirmasi" visual: tap pertama cuma ngangkat kartu ke tengah biar user bisa lihat jelas kartu mana yang kepencet, baru tap kedua yang beneran commit buka surat.

**State**, di level `PenPalsScreen` (bukan per-kartu — cuma boleh ada 1 kartu fokus dalam satu waktu):
```kotlin
var focusedLetterId by remember { mutableStateOf<String?>(null) }
BackHandler(enabled = focusedLetterId != null && !isLetterOpen) { focusedLetterId = null }
```
`BackHandler` ini terpisah dari `BackHandler(enabled = isLetterOpen) { closeLetter() }` yang udah ada — kalau overlay surat lagi kebuka, back nutup overlay dulu (fokusnya tetap seperti apa adanya, kartu balik ke desk kalau overlay-nya ditutup terus di-scrim/back lagi).

**Tiap `DeskCard` nerima** `isFocused: Boolean` (`letter.id == focusedLetterId`), `isAnyFocused: Boolean` (`focusedLetterId != null`), dan dua callback baru (`onClick` lama dipecah jadi dua karena tap punya arti beda tergantung state):
- `onRequestFocus: () -> Unit` → `{ focusedLetterId = letter.id }` — tap kartu yang belum fokus.
- `onRequestOpen: () -> Unit` → `{ authGuard.guard { openLetter(index) } }` — tap kartu yang **lagi** fokus. **Auth guard sekarang di tap kedua ini**, bukan di tap pertama — jadi guest tetap bisa preview/zoom kartu manapun, baru diminta login pas beneran mau baca isinya.

**Visual di dalam `DeskCard`** — 3 nilai animasi baru, semua pakai `LetterlySpring.Bouncy` ("efek muncul yang playful", cocok buat "kartu keangkat" ini) kecuali dim yang pakai `Gentle` (dokumentasi presetnya sendiri: "modal, bottom sheet"):
```kotlin
val focusProgress by animateFloatAsState(if (isFocused) 1f else 0f, LetterlySpring.Bouncy) // 0..1
val dimAlpha by animateFloatAsState(
    if (isAnyFocused && !isFocused) DeskDimmedAlpha else 1f, LetterlySpring.Gentle,
) // kartu lain meredup ke 0.4f
val focusCenterX = (deskWidthPx - cardWidthPx) / 2f
val focusCenterY = (deskHeightPx - cardHeightPx) / 2f
```
`focusProgress` dipakai buat **interpolasi** (`androidx.compose.ui.util.lerp`, bukan animasi posisi absolut terpisah) antara posisi meja kartu (`state.offsetX/Y.value`, yang nggak berubah selama fokus karena drag di-disable) dan titik tengah layar — jadi begitu fokus dilepas, kartu otomatis animasi balik ke posisi meja yang PERSIS sama, nggak perlu nyimpen "posisi sebelum fokus" terpisah:
```kotlin
translationX = lerp(state.offsetX.value, focusCenterX, focusProgress)
translationY = lerp(state.offsetY.value, focusCenterY, focusProgress) + entranceOffsetYPx
rotationZ = lerp(state.rotationZ, 0f, focusProgress) // rotasi diluruskan pas fokus
```
`liftScale`/`elevationPx` (dipakai bareng sama drag-lift yang udah ada) jadi 3 tingkat: `isFocused → 1.1f/28.dp`, `isDragging → 1.03f/14.dp`, rest `1f/2.dp`. Alpha akhir kartu = `entranceAlpha * dimAlpha` (staggered entrance tetap jalan independen dari dim).

**Gating interaksi** — `canInteract = !isAnyFocused || isFocused`:
- Drag (`pointerInput`/`detectDragGestures`) cuma di-attach kalau `!isAnyFocused` — begitu ada kartu fokus (termasuk kartu itu sendiri), drag mati total buat semua kartu.
- `clickable` cuma di-attach kalau `canInteract` — kartu yang lagi di-dim (bukan yang fokus, tapi ada kartu lain yang fokus) nggak nerima tap sama sekali.

**Scrim**, di-render di `PenPalsScreen` (bukan di dalam `DeskCard`) — full-screen, di atas header (jadi seluruh layar termasuk header ikut meredup), di bawah kartu yang fokus:
```kotlin
if (focusedLetterId != null) {
    Box(
        Modifier.fillMaxSize().zIndex(DeskFocusScrimZIndex)
            .background(Color.Black.copy(alpha = DeskScrimAlpha))
            .clickable(interactionSource, indication = null) { focusedLetterId = null },
    )
}
```

**zIndex band** (lihat konstanta §2.2): kartu desk biasa pakai `state.zIndex.floatValue` (kecil, naik tiap digrab) → header `1000f` → scrim `1100f` → kartu yang lagi fokus `1200f` → `OpenLetterOverlay` `2000f` (paling atas, nggak berubah).

---

## 3. `ui/screens/penpals/OpenLetterOverlay.kt`

**Tidak berubah oleh refactor v3** — overlay ini persis sama seperti sebelum loose desk cards ada, sesuai keputusan produk untuk cuma ganti stack-nya. Satu-satunya perubahan: `onPageChanged` (sync balik ke `PenPalsScreen`) sekarang nggak dipanggil sama sekali dari `PenPalsScreen` (fallback ke default no-op-nya sendiri) — dulu dipakai buat `focusProgress.snapTo(index)` supaya stack di belakang overlay nunjukin kartu yang sama kayak yang terakhir dibuka pas overlay ditutup; di loose desk nggak ada konsep "kartu yang lagi fokus" buat di-sync, jadi plumbing itu dibuang, bukan diganti sesuatu yang lain.

Slide up/down, `VerticalPager` buat scroll-to-next-letter, `CloseButton`/`ActionRail`/`SenderInfoBar`, dan semua sub-composable-nya — semua detail di bawah ini masih akurat persis seperti implementasi aktual.

**Beda dari draft v1.0:** bukan scale-in (`0.85f → 1f`) — implementasi aktual **slide up dari bawah layar** (translationY 100% viewport height → 0), lebih match "surat ditarik keluar dari amplop" secara fisik.

**Scroll-to-next-letter (paging):** overlay nerima **seluruh list surat**, bukan cuma satu, dan nampilinnya lewat `VerticalPager` — satu page per surat. Gesture drag vertikal yang sama yang dipakai buat buka surat juga dipakai buat pindah ke surat berikutnya/sebelumnya, jadi user nggak perlu tap close → cari kartu lain di desk → tap lagi.

```kotlin
@Composable
fun OpenLetterOverlay(
    letters: List<PenpalLetter>,
    initialIndex: Int,
    isOpen: Boolean,
    onClose: () -> Unit,
    onFullyClosed: () -> Unit,
    onPageChanged: (Int) -> Unit = {}, // v3: PenPalsScreen tidak lagi override ini
    modifier: Modifier = Modifier,
) {
    // slideOffsetFraction: 0 = full visible, 1 = full di bawah layar
    //   OPEN  → SlideUpEnterSpec  (dampingRatio 0.78, stiffness 280 — gentle w/ slight overshoot)
    //   CLOSE → SlideDownExitSpec (DampingRatioNoBouncy, StiffnessMedium — cepat, no bounce)
    // BoxWithConstraints buat dapetin screenHeightPx real, translationY = screenHeightPx * fraction
    // onFullyClosed() dipanggil dari finishedListener pas fraction >= 0.98f (bukan langsung pas tap X)

    // overlayAlpha: fade cepat (tween 200ms), independen dari slide
    // Stagger isi: showBody di +100ms, showFooter di +200ms setelah isOpen — masing-masing
    // pasangan animateFloatAsState (alpha) + animateDpAsState (offsetY 20dp→0dp, GentleDpSpring).
    // Stagger ini GLOBAL ke overlay (nggak re-trigger tiap pindah page), biar swipe cepat
    // antar surat nggak keliatan kedip-kedip fade-in ulang.

    val pagerState = rememberPagerState(initialPage = initialIndex) { letters.size }
    val currentLetter = letters[pagerState.currentPage]

    // Haptic tick + lapor balik (opsional) tiap kali settled page berubah.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { page -> onPageChanged(page); haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    }
}
```

Layout dalam `BoxWithConstraints`:
- **Body** — `VerticalPager(state = pagerState, userScrollEnabled = isOpen, key = { letters[it].id })`. Background kertas sekarang **per-page di dalam pager** (bukan satu `Image(PaperTemplate.LINED)` statis di luar pager lagi — itu versi lama, diganti karena tiap surat bisa punya kertas beda): kalau `letters[page].compositeImageUrl != null`, render `AsyncImage(compositeImageUrl)` full-bleed (`placeholder`/`error` = `PaperTemplate.LINED`, sama pola dengan `envelopeCompositeImageUrl` di `DeskCard.kt`) — ini flattened letter paper asli dari sender (teks + tekstur kertas + annotasi, lihat `docs/be/penpals_api.md`). Kalau `null` (surat lama / device sender di bawah `MinCompositingDensity`, atau backend belum kirim field ini), fallback: `Image(PaperTemplate.LINED)` generik + `Column` scrollable (`verticalScroll`, state baru per page) dengan padding `start=24 end=76 top=88 bottom=120`, `Text(letters[page].bodyText, CaveatFontFamily, 20sp, lineHeight 30sp, InkDefault)` — kertas asli pengirim (`paper_template`/`paper_color`) tetap nggak diketahui di jalur fallback ini, sama gap-nya dengan `InboxItem.letterPaper`. `userScrollEnabled = isOpen` biar drag nggak nyangkut di tengah animasi close.
- Gradient scrim atas 120dp (`Brush.verticalGradient`, hitam 53%→transparent) dirender **di atas** pager (bukan di bawah lagi) biar `CloseButton` tetap kebaca terlepas dari background per-page yang sekarang dinamis.
- `CloseButton` — top-end, `statusBarsPadding()`, **di luar pager** (posisi tetap, nggak ikut swipe).
- `ActionRail` — center-end, data dari `currentLetter` (jadi `likeCount` update reaktif tiap pindah page), **di luar pager**.
- `SenderInfoBar` — bottom-start full width, data dari `currentLetter`, **di luar pager**.
- `ComingSoonBottomSheet` dirender **di luar** `Box` yang slide, biar posisinya nggak ikut ke-translate pas overlay slide turun.

**Kenapa cuma body yang di dalam `VerticalPager`, bukan seluruh layout (close/rail/sender bar):** elemen-elemen itu posisinya tetap di layar yang sama buat surat manapun, cuma datanya yang beda — nggak ada gunanya di-duplikat 20× di dalam tiap page. Ini juga otomatis ngasih behavior yang tepat lewat nested scroll bawaan Compose: kalau body surat cukup pendek buat muat di layar (nggak butuh scroll), drag langsung diteruskan ke pager → langsung pindah surat. Kalau suratnya panjang, `verticalScroll` di dalam page nyerap drag dulu buat scroll teks, baru pas udah mentok atas/bawah sisa drag-nya diteruskan ke pager. Nggak butuh custom `NestedScrollConnection`.

### Sub-composables (tidak berubah)

**`CloseButton(onClick, modifier)`** — `Box` 40dp `CircleShape` `ToolbarFrostedDark`, `rememberPressScale`, isi `Icon(Icons.Filled.Close, tint=White)`.

**`ActionRail(likeCount, onLikeClick, onCommentClick, onBookmarkClick, onShareClick, modifier)`** — `Column(spacedBy(24.dp))`, 4× `ActionRailItem`: Like (`FavoriteBorder`, label=`likeCount`), Comment (`ChatBubbleOutline`), Bookmark (`BookmarkBorder`), Share (`Share`).

**`SenderInfoBar(letter, onMoreClick, modifier)`** — `Row` (`RoundedCornerShape(20.dp)`, `ToolbarFrostedDark`, padding 16dp/12dp): avatar placeholder bulat 40dp → nama + dot online hijau (`Color(0xFF34C759)`, cuma kalau `letter.isOnline`) → region → `"Posted on {tanggal}"` → `Icons.Filled.MoreVert`.

**`ShareLetterSheet(letter, graphicsLayer, onDismiss, onSaveImageRequested, modifier)`** (`ui/screens/penpals/widgets/ShareLetterSheet.kt`) — hand-rolled slide-up sheet dibuka dari `ActionRail`'s Share icon dan `SenderInfoBar`'s "Share Mail" menu item. Sumber byte-nya: kalau `letter.compositeImageUrl != null`, download langsung (kualitas seragam berapa pun density HP viewer); kalau `null` (surat mode-text), fallback capture kartu on-screen lewat `graphicsLayer.toCompositeBytes()`.

> **Bug (fixed):** capture fallback di atas dipanggil tanpa cek density HP **viewer** (device yang lagi buka share sheet, bukan device pengirim surat) — padahal `CompositingCapability.kt`'s `MinCompositingDensity` sudah mendokumentasikan `GraphicsLayer.toCompositeBytes()` menghasilkan composite yang "too soft (at worst, illegible noise)" di bawah threshold itu, dan `ComposeScreen.kt` sudah gating semua pemakaian lain fungsi ini pakai `density.density >= MinCompositingDensity`. `ShareLetterSheet.kt` melewatkan gating yang sama. Gejalanya: share surat yang **mode-text** (bukan mode-image/`compositeImageUrl == null`) dari HP dengan density rendah ("kentang") menghasilkan gambar rusak/noise — mode-image nggak kena karena bytes-nya didownload, bukan di-capture on-device.
>
> **Fix:** `ShareLetterSheet.kt` sekarang skip capture kalau `LocalDensity.current.density < MinCompositingDensity` dan `compositeImageUrl == null` — semua share target (`Instagram`/`WhatsApp`/`Save`/`More`) tetap disabled seperti sebelumnya (`targetsEnabled = shareBytes != null`), tapi preview box nampilin pesan penjelas (`share_letter_device_unsupported`) alih-alih diem kosong atau ngirim gambar pecah.

---

## 4. Import list

**`PenPalsScreen.kt`:** `androidx.activity.compose.BackHandler`, `androidx.compose.animation.core.{Animatable,AnimationVector1D,animateFloatAsState}`, `androidx.compose.foundation.{Image,background,clickable}`, `androidx.compose.foundation.gestures.detectDragGestures`, `androidx.compose.foundation.interaction.MutableInteractionSource`, `androidx.compose.foundation.layout.*`, `androidx.compose.foundation.shape.{RoundedCornerShape,CircleShape}`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Person`, `androidx.compose.material3.{Icon,MaterialTheme,Text}`, `androidx.compose.runtime.*` (termasuk `MutableFloatState`/`mutableFloatStateOf`/`mutableStateMapOf`), `androidx.compose.ui.Alignment`, `androidx.compose.ui.Modifier`, `androidx.compose.ui.draw.clip`, `androidx.compose.ui.geometry.Offset`, `androidx.compose.ui.graphics.{Color,graphicsLayer}`, `androidx.compose.ui.hapticfeedback.HapticFeedbackType`, `androidx.compose.ui.input.pointer.pointerInput`, `androidx.compose.ui.layout.{ContentScale,onGloballyPositioned}`, `androidx.compose.ui.platform.{LocalContext,LocalDensity,LocalHapticFeedback}`, `androidx.compose.ui.res.{painterResource,stringResource}`, `androidx.compose.ui.text.font.FontWeight`, `androidx.compose.ui.unit.{Dp,dp}`, `androidx.compose.ui.util.lerp`, `androidx.compose.ui.zIndex`, `com.apps.unsealed.R`, `com.apps.unsealed.core.util.{LetterlySpring,reducedMotionSpring,rememberIsReducedMotion,rememberPressScale}`, `com.apps.unsealed.feature.auth.AuthUiState`, `com.apps.unsealed.ui.screens.selectrecipient.EnvelopeAspectRatio`, `com.apps.unsealed.ui.theme.ToolbarFrostedDark`, `kotlinx.coroutines.{delay,launch}`, `kotlin.math.{PI,cos,sin,sqrt}`, `kotlin.random.Random`. (v3.1: `Brush`/`IceSkyBlue`/`SurfaceCardLight`/`SurfaceCream`/`BrandInk`/`BrandGoldDim` dibuang bareng local background revert §2.3; `Color`/`androidx.compose.ui.util.lerp` ditambah buat focus mode §2.5.)

**`OpenLetterOverlay.kt`:** tidak berubah — `androidx.compose.foundation.layout.BoxWithConstraints`, `androidx.compose.foundation.pager.{VerticalPager,rememberPagerState}`, `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`, `androidx.compose.material.icons.filled.{Close,FavoriteBorder,ChatBubbleOutline,BookmarkBorder,Share,MoreVert}`, `androidx.compose.runtime.snapshotFlow`, `androidx.compose.ui.graphics.{Brush,vector.ImageVector}`, `androidx.compose.ui.hapticfeedback.HapticFeedbackType`, `androidx.compose.ui.platform.LocalHapticFeedback`, `com.apps.unsealed.core.util.rememberPressScale`, `com.apps.unsealed.ui.screens.compose.ComingSoonBottomSheet`, `com.apps.unsealed.ui.theme.PaperTemplate`, `kotlinx.coroutines.flow.{distinctUntilChanged,drop}`.

---

## 5. Strings (sudah ada di `strings.xml`, referensi cepat)

```xml
<string name="nav_penpals">PenPals</string>
<string name="penpals_greeting_hello">Hi, %1$s! 👋</string>
<string name="penpals_greeting_guest_name">Guest</string>
<string name="penpals_greeting_default_title">Ready to Slide In Today? 🐧</string>
<string name="penpals_greeting_default_body">Peni's all geared up. Where should we send a letter today?</string>
<string name="penpals_avatar_desc">Profile</string>
<string name="penpals_card_region_label">A Penpal in %1$s</string>
<string name="open_letter_close_desc">Close</string>
<string name="open_letter_like_desc">Like</string>
<string name="open_letter_comment_desc">Comment</string>
<string name="open_letter_bookmark_desc">Bookmark</string>
<string name="open_letter_share_desc">Share</string>
<string name="open_letter_more_desc">More</string>
<string name="open_letter_posted_on">Posted on %1$s</string>
```

---

## 6. Verifikasi manual (checklist)

- Build/run app, cek tab "PenPals" (globe icon) muncul paling kiri di bottom nav.
- Masuk tab: background = `bg_texture` global (meja hijau), sama seperti tab lain — bukan gradient terang lokal. Header + avatar pill kebaca jelas (putih di atas gelap).
- Kartu-kartu surat berserakan (bukan tersusun stack) di bawah header, muncul dengan stagger entrance.
- Drag satu kartu (belum ada yang fokus): gerak bebas ngikutin jari, lepas = kartu diem di posisi terakhir.
- Grab kartu manapun: kartu itu langsung naik ke lapisan paling atas (nutupin kartu lain yang ketiban), haptic tick pas mulai drag.
- Coba drag kartu ke arah header / ke luar tepi layar: nggak bisa (clamped), kartu tetap reachable.
- **Focus mode — tap kartu yang belum fokus:** kartu meluncur/membesar ke tengah layar (rotasi jadi lurus), kartu lain + header meredup di belakang scrim gelap. Kartu itu nggak lagi bisa di-drag selama fokus.
- **Tap kartu lain / area kosong (scrim) saat ada yang fokus:** kartu fokus balik ke posisi & rotasi semula di meja (persis, bukan posisi baru), scrim hilang.
- **Tombol back saat ada kartu fokus** (dan `OpenLetterOverlay` belum kebuka): sama efeknya kayak tap scrim — kartu balik ke meja.
- **Tap kedua di kartu yang lagi fokus:** baru ini yang buka `OpenLetterOverlay`. Kalau belum login, di titik ini baru muncul `LoginPromptBottomSheet` (bukan pas tap pertama/fokus) — coba di akun guest.
- Di dalam `OpenLetterOverlay`: like sebuah surat (ikon hati di rail), lalu close overlay — pastikan posisi kartu-kartu di desk di belakangnya **tidak** ke-reset/acak ulang.
- Loading state (throttle network / restart tab pas belum ke-cache): shimmer muncul scattered, begitu data asli datang kartu asli nongol persis di posisi shimmer (nggak ada "lompat").
- `OpenLetterOverlay` sendiri (paging antar surat, reply, like, bookmark/share stub, sender bar, close) — semua persis seperti sebelumnya, tidak ada regresi.
- Tab lain (`Inbox`/`Write`/`Stamps`/`Profile`) nggak kesenggol.

---

## 7. Catatan performa (v3 vs v2)

*v2 (stack 1D) sempat diprofiling di device fisik Android 8.1 (API 27) setelah lag ketara — root cause & fix lengkapnya waktu itu: Composition-phase read tiap frame (semua kartu recompose bareng), Layout-phase modifier dinamis (`.padding`/`.height`), semua kartu selalu di-render tanpa culling, dan `shadowElevation` dinamis di banyak layer sekaligus.*

**Kenapa v3 (loose desk) secara struktural jauh lebih longgar soal ini, meski tetap pakai disiplin yang sama:**
- **Cuma satu kartu yang mutasi per frame** — kartu yang lagi di-drag. Di v2, SEMUA kartu recompute posisi tiap frame dari satu `focusProgress` bersama (karena posisi tiap kartu adalah fungsi dari jarak ke fokus); di v3, posisi tiap kartu independen (`Animatable` masing-masing), jadi drag satu kartu nggak nyentuh kartu lain sama sekali.
- Karena itu, **culling (v2 §7.1.C, §7.2.C) nggak diperlukan lagi** — nggak ada cost "semua kartu ikut kena kalkulasi tiap frame" yang perlu dibatasi, jadi seluruh `letters` di-render langsung tanpa windowing.
- **Prinsip Draw-phase-only tetap dipertahankan** (translationX/Y, rotationZ, scale, shadowElevation semua di dalam `graphicsLayer {}`, bukan `.offset{}`/`.padding()` dinamis) — bukan karena krusial kayak di v2 (cuma 1 kartu yang gerak, bukan 20), tapi karena nggak ada alasan buat regress dari disiplin yang udah ada, dan tetap paling murah buat device kelas bawah.
- **Nggak ada lagi konsep "snap ke index terdekat"** (v2 §7.3) — kartu di loose desk cuma "dilepas di posisi manapun terakhir", nggak ada target diskrit buat di-snap ke sana.
- **Focus mode (v3.1, §2.5)** ikut prinsip yang sama: cuma kartu yang lagi di-tap yang punya `focusProgress`/`dimAlpha` animasi berjalan; kartu-kartu lain nilainya konstan (`0f`/`1f`) begitu animasi settle, jadi nggak ada cost tambahan yang signifikan dibanding sebelum ada focus mode.

---

*PenPals Screen Spec v3.1 · Loose desk cards (2D freeform drag) + focus zoom (tap-to-preview) + scroll-to-next-letter overlay + Peni-voice greeting header · Android · Jetpack Compose*
