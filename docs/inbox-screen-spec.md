# Letterly — Inbox Screen Spec
*Mode 1: Received Letters · Android · Jetpack Compose*

---

## Status Implementasi

| Bagian | Status | Catatan |
|---|---|---|
| Scaffold & Background | ✅ | Full-screen bokeh backdrop di `MainActivity`, cross-API (API 24+) via software blur. Sempat ada bug overlay-terlalu-gelap (gradient+graphicsLayer alpha) yang bikin blur-nya nggak keliatan — sudah di-fix, lihat §"Dark Overlay" |
| Collapsing Title | ✅ | Large title kiri atas → compact center saat scroll |
| Letter List (Mode 1) | ✅ | `LazyColumn` 10 dummy `InboxItem`, stagger entrance animation |
| InboxItemCard | ✅ | Stamp placeholder (`StampSwatch`), sender name, country · time, unread dot |
| Open Letter Screen | ✅ | `OpenLetterScreen.kt` — nav route pushed on card tap, envelope↔paper reveal, Incoming Mail state, 3-dot menu, Write Letter CTA. Detail di §"Open Letter Screen" di bawah |
| Mode 2 (Sent/Drafts) | 🔜 | Belum didesain — scope session berikutnya |
| Navigation wiring | ✅ | Route `Destinations.Inbox` → `InboxScreen(onItemClick)`; tap kartu → `open_letter/{letterId}` (`OpenLetterScreen`) |
| String resources | ✅ | EN + ID, semua terlokalisasi |

---

## Gambaran Umum

Inbox adalah tab pertama yang dilihat user setelah masuk Penpals.
Menampilkan daftar surat masuk yang sudah diterima dari penpal.

**Aesthetic:** Dark, moody, seperti meja surat malam hari — bokeh blur dari bg_texture
di balik kartu frosted glass. Beda tone dari ComposeScreen yang terang dan hangat.

---

## Arsitektur

### File yang terlibat

| File | Keterangan |
|---|---|
| `ui/screens/inbox/InboxScreen.kt` | Screen composable + `InboxItemCard` private composable |
| `ui/screens/inbox/InboxItem.kt` | Data model + `dummyInboxItems` list (10 item) |
| `core/util/SoftwareBlur.kt` | `Context.softwareBlurredBitmap()` — cross-API blur helper |
| `navigation/Destinations.kt` | `Destinations.Inbox` route; `entries by lazy` (fix init ordering) |
| `MainActivity.kt` | Bokeh background dikelola di sini untuk semua route |
| `res/values/strings.xml` | Inbox strings (EN) |
| `res/values-id/strings.xml` | Inbox strings (ID) |

### Dependency graph

```
MainActivity
├── softwareBlurredBitmap()  [SoftwareBlur.kt]  — hanya aktif saat isInboxRoute
└── UnsealedNavHost
    └── InboxScreen
        ├── InboxItem + dummyInboxItems
        └── StampSwatch  [SelectRecipientScreen — reuse]
```

---

## Background Bokeh

### Kenapa di `MainActivity`, bukan di `InboxScreen`

Background bokeh harus terlihat **full screen** — termasuk di area status bar dan navigation bar.
`InboxScreen` di-embed di dalam `Scaffold` dengan `padding(innerPadding)`, sehingga apapun
yang digambar di dalam screen tidak bisa menembus ke luar Scaffold content area.

Solusi: background dikelola di `MainActivity.kt` di `Box` root yang benar-benar full screen,
di bawah `Scaffold`.

```
MainActivity root Box (fillMaxSize)
├── [Layer 1] Image bg_texture — software-blurred (hanya saat Inbox route)
│             ATAU Image bg_texture biasa + tilt parallax (route lain)
├── [Layer 2] Dark gradient overlay — animasi fade in saat masuk Inbox
└── [Layer 3] Scaffold
    ├── BottomNavBar
    └── InboxScreen (transparan, tidak punya background sendiri)
```

### Cross-API Software Blur

`Modifier.blur()` hanya tersedia di API 31+ (Android 12).
Project ini `minSdk = 24`, jadi dipakai teknik **downscale + upscale** via `Canvas` + `Paint`:

```kotlin
// SoftwareBlur.kt
fun Context.softwareBlurredBitmap(
    @DrawableRes resId: Int,
    scaleFactor: Float = 0.06f,  // 6% = strong bokeh
): Bitmap? {
    val source = BitmapFactory.decodeResource(resources, resId, options)
    val small  = Bitmap.createScaledBitmap(source, w * scaleFactor, h * scaleFactor, filter = true)
    // Stretch back up dengan isFilterBitmap = true → bilinear interpolation → blur
    ...
}
```

`scaleFactor = 0.025f` (2.5% dari ukuran asli) menghasilkan blur kuat/lembut — wood grain
sepenuhnya larut jadi soft bokeh blob, bukan sekadar sedikit soft. Bitmap di-cache via
`remember {}` — hanya dihitung sekali per composition lifecycle.

> **Riwayat tuning:** nilai awal `0.06f` ("6% = strong bokeh" di komentar lama) sebenarnya
> masih cukup detail/kurang blur untuk mood yang dicari — diturunkan ke `0.025f` supaya
> background beneran "gak jelas" (indistinct), bukan cuma soft-focus ringan. Semakin kecil
> `scaleFactor`, semakin blur.

### Dark Overlay

Overlay tint gelap solid (`Color.Black.copy(alpha = overlayAlpha)`) yang **fade in dengan animasi**
saat user berpindah ke tab Inbox:

```kotlin
val overlayAlpha by animateFloatAsState(
    targetValue = if (isInboxRoute) 0.25f else 0f,
    label = "inbox_overlay_alpha",
)

Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = overlayAlpha)),
)
```

Tujuan: memastikan teks kartu tetap terbaca di atas bokeh backdrop,
sekaligus memberikan transisi yang smooth saat berpindah tab.

> **Bug fix — kenapa bukan `Brush.verticalGradient` lagi:** implementasi awal pakai
> gradient dua warna semi-transparan (`0xD9100C08` → `0xF0080502`, alpha ~85-94%) yang
> di-fade lewat `Modifier.graphicsLayer { alpha = overlayAlpha }` terpisah. Kombinasi ini
> ternyata compositing jauh lebih gelap dari alpha nominalnya — `overlayAlpha = 0.25f`
> secara visual terbaca hampir solid hitam (background bokeh sama sekali gak keliatan),
> padahal targetnya cuma tint tipis. Dikonfirmasi lewat `Log.d` bahwa `overlayAlpha`
> benar-benar settle di `0.25`, jadi bug-nya di compositing gradient-alpha-berlapis, bukan
> di angka target. Fix: ganti ke `Color.Black.copy(alpha = overlayAlpha)` polos (satu warna
> flat, alpha di-bake langsung ke `Color`, tanpa `graphicsLayer` terpisah) — komposit persis
> sesuai alpha yang diminta, matematisnya cocok (`bg × (1-alpha)`). Konsekuensi: overlay
> sekarang rata (dulu ada sedikit gradasi vertikal atas→bawah), tapi bedanya nggak kentara
> secara visual dan worth ditukar demi background beneran nunjukkin blur-nya.

---

## Collapsing Title

Teknik: `LazyListState.firstVisibleItemScrollOffset` + `firstVisibleItemIndex`
untuk menginterpolasi antara dua state title dalam rentang 140px scroll.

```
titleCollapseFraction: Float  →  0f (atas) ... 1f (sudah scroll)

Large title (kiri atas):
  alpha    = 1f - titleCollapseFraction
  translateY = -16dp * titleCollapseFraction

Compact title (center, pinned di top):
  alpha = titleCollapseFraction
  scale = 0.92f + 0.08f * titleCollapseFraction  (scale-up entrance)
```

---

## InboxItemCard

Layout per kartu:

```
Row (frosted glass: 0x28FFFFFF, RoundedCornerShape(20dp))
├── Box 52×72dp (clip 8dp)
│   └── StampSwatch(design = item.stamp)   ← placeholder vertikal
├── Column (weight 1f)
│   ├── Text: senderName  (SemiBold, 15sp, White)
│   └── Text: "country · time"  (12sp, White 60%)
└── Box 8dp (CircleShape, 0xFFE07A9A)  — hanya jika isUnread = true
```

### Stamp Placeholder → Swap ke Asset Asli

Sekarang stamp dirender pakai `StampSwatch` (vector-drawn colored rectangle dari `SelectRecipientScreen`).
Saat asset bitmap perangko sudah siap, ganti block di `InboxItemCard`:

```kotlin
// Sekarang:
Box(Modifier.width(52.dp).height(72.dp).clip(RoundedCornerShape(8.dp))) {
    StampSwatch(design = item.stamp, modifier = Modifier.fillMaxSize())
}

// Setelah ada asset:
Image(
    painter = painterResource(R.drawable.nama_asset_perangko),
    contentDescription = stringResource(R.string.inbox_stamp_desc, item.senderName),
    contentScale = ContentScale.Crop,
    modifier = Modifier.width(52.dp).height(72.dp).clip(RoundedCornerShape(8.dp)),
)
```

Tambahkan field `@DrawableRes val stampImageRes: Int` ke `InboxItem` kalau mau per-item
asset yang berbeda.

### Motion

Mengikuti `motion-rules.md`:

| Behavior | Nilai |
|---|---|
| Press feedback | scale 0.97f + rotationZ -1.5° |
| Animation spec | `LetterlySpring.Snappy` |
| Ripple | ❌ Tidak ada — `indication = null` |
| Stagger entrance | delay `index × 40ms`, offset `-24dp → 0dp` + alpha 0→1, duration 220ms |
| Reduced motion | `rememberIsReducedMotion()` → `tween(0)` |

---

## Data Model

```kotlin
data class InboxItem(
    val id: String,
    val senderName: String,
    val country: String,
    val timeLabel: String,       // e.g. "Jul 27, 6:28"
    val stamp: StampDesign,      // placeholder — swap ke Image() saat asset siap
    val isUnread: Boolean = false,
)
```

`dummyInboxItems` (10 item) ada di `InboxItem.kt` untuk development.
Akan diganti dengan data real dari Firestore saat backend diintegrasikan.

---

## String Resources

| Key | EN | ID |
|---|---|---|
| `inbox_title` | Inbox | Kotak Masuk |
| `inbox_stamp_desc` | Stamp from %1$s | Perangko dari %1$s |
| `inbox_empty_label` | No letters yet | Belum ada surat |
| `inbox_sender_country` | %1$s · %2$s | %1$s · %2$s |
| `inbox_unread_dot_desc` | Unread | Belum dibaca |

---

## Bug Fix: `Destinations.entries` NPE

Saat `Destinations.Inbox.route` diakses dari `MainActivity` (untuk cek `isInboxRoute`),
JVM memulai inisialisasi `Destinations$Inbox` class **sebelum** companion object selesai.
Ketika companion object mencoba membuat list `entries = listOf(PenPals, Inbox, ...)`,
`Destinations$Inbox.INSTANCE` masih `null` → list berisi null → NPE di `BottomNavBar`.

**Fix di `Destinations.kt`:**

```kotlin
// Sebelum (broken kalau Destinations.Inbox diakses lebih dulu):
val entries: List<Destinations> = listOf(PenPals, Inbox, Write, Stamps, Profile)

// Sesudah (aman — list dibuat saat pertama kali dibaca, semua INSTANCE sudah set):
val entries: List<Destinations> by lazy { listOf(PenPals, Inbox, Write, Stamps, Profile) }
```

---

## Open Letter Screen

*`ui/screens/inbox/OpenLetterScreen.kt` — full-screen detail pushed from `InboxItemCard`'s tap.*

### Arsitektur

Dibangun sebagai **nav route beneran** (`open_letter/{letterId}`, di-push ke back stack), bukan overlay lokal seperti `OpenLetterOverlay` di fitur PenPals — ini jadi route pertama di codebase ini yang bawa nav argument. Konstanta route (`OpenLetterRoute`, `openLetterRoute(id)`) ada di `UnsealedNavHost.kt`, mengikuti pola `SelectRecipientRoute` (stack-only, bukan `Destinations` entry). Di dalam `composable()`, letter di-lookup dari `dummyInboxItems` berdasarkan id — belum ada ViewModel/repository (konsisten dengan `InboxScreen`/`PenPalsScreen` yang juga masih dummy-data-only).

`MainActivity.kt` meng-extend `isInboxRoute` (jadi `isInboxRoute || isOpenLetterRoute`) supaya background bokeh gelap yang sama ikut terbawa dari Inbox, dan meng-extend kondisi hide-bottom-nav (sebelumnya cuma `SelectRecipientRoute`) supaya bottom nav juga hilang di layar ini. Karena `NavBackStackEntry.destination.route` resolve ke **pattern** (`"open_letter/{letterId}"`), bukan nilai terisi, perbandingannya pakai `startsWith(OpenLetterRoute)`, bukan `==`.

### Data model tambahan (`InboxItem.kt`)

```kotlin
enum class LetterDeliveryStatus { DELIVERED, INCOMING }
```

`InboxItem` nambah `envelope: EnvelopeDesign` (reuse dari `selectrecipient`), `letterPaper: PaperTemplate`, `metaLabel: String`, `deliveryStatus`, dan `expectedDeliveryLabel: String?` (cuma dipakai saat `INCOMING`). 2 dari 10 dummy item di-set `INCOMING` biar kedua state bisa didemoin.

**Placeholder letter image:** belum ada asset "foto surat tulisan tangan" — `letterPaper` reuse salah satu dari 8 tekstur `PaperTemplate` yang sudah ada sebagai stand-in, pola yang sama dengan `StampSwatch` yang jadi placeholder buat stamp. Swap ke asset foto surat asli (atau URL dari backend) kalau pipeline-nya udah ada — ganti tipe field jadi image resource/URL yang sesuai.

### Layar

- **Header:** back button (frosted circle, sama persis shape-nya dengan `CloseButton` di `OpenLetterOverlay`) + nama pengirim & negara + trigger 3-dot yang buka `AnchoredDropdownMenu` (Report User / Add Friend / Block User — ketiganya masih stub ke `ComingSoonBottomSheet`, pola yang sama seperti action rail PenPals).
- **`INCOMING`:** judul "Incoming Mail" + subtext perkiraan tanggal tiba + amplop redup (non-interaktif) + tombol "Open Early" (pill, ikon gembok — paywall stub, `ComingSoonBottomSheet`).
- **`DELIVERED`:** reveal amplop → kertas (motion-rules.md §3.6) — satu `Box` yang `aspectRatio`-nya dianimasikan dari `EnvelopeAspectRatio` (landscape) ke rasio portrait kertas (`1240f/1754f`, sama seperti asset `PaperTemplate`) via `LetterlySpring.Envelope`, dua layer `Image` crossfade (amplop scale-down+fade-out, kertas scale-up+fade-in). Tap di amplop (belum dibuka) memicu reveal. Di bawahnya, ikon kecil (amplop↔kertas, ikut state) + `item.metaLabel` sebagai caption — **posisi/slot captionnya sama persis di kedua mode**, cuma teksnya per-item (bukan diturunkan dari mode).
- **Write Letter:** pill icon+text (gaya sama seperti `NextButton` di `SelectRecipientScreen.kt` — `ToolbarFrostedDark` stadium + border + shadow), navigate ke tab Write (`ComposeScreen`) yang sudah ada. **Belum ada pre-fill reply context** (nama penerima/dearName) — `ComposeViewModel` belum punya nav-arg plumbing sama sekali, jadi ini scope lanjutan.

## Next Steps (Mode 2 & Beyond)

- **Mode 2:** Sent letters / Drafts tab di dalam Inbox screen (tab switcher di bawah title)
- **Real data:** Koneksi ke Firestore untuk fetch surat masuk asli
- **Reply pre-fill:** `ComposeViewModel` belum bisa menerima "replying to X" — Write Letter sekarang cuma navigate ke tab Write kosong
- **Report/Add Friend/Block/Open Early sungguhan:** masih `ComingSoonBottomSheet` stub, belum ada backend
- **Letter image asli:** ganti `InboxItem.letterPaper` (placeholder `PaperTemplate`) dengan asset/URL foto surat sungguhan
- **Pull to refresh:** Swipe down untuk fetch surat baru
- **Empty state:** Tampilkan `inbox_empty_label` saat list kosong
