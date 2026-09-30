# Panduan Scaffold Fitur/Screen Baru
*Baca dokumen ini PERTAMA kali sebelum mulai nulis kode untuk fitur/screen baru — human atau AI agent.*

> Dokumen ini bukan pengganti `docs/architecture.md` atau `docs/api-integration-guide.md` — dokumen ini adalah
> **peta jalan** yang nyambungin semuanya, plus ngisi satu gap yang belum ada di manapun: kapan struktur folder
> screen baru harus flat vs kapan harus dipecah ke `screen/`, `state/`, `viewmodel/`, `widgets/`, `constants/`.

**Baca juga (jangan duplikasi isinya, cukup rujuk):**

| Dokumen | Isinya |
|---|---|
| `docs/architecture.md` | Gambaran arsitektur besar, layer, network, DI, design system, "Aturan Wajib untuk AI Agent" |
| `docs/api-integration-guide.md` | Checklist step-by-step nambah 1 endpoint baru: DTO → Api → Repository → ViewModel → Composable → NavHost → strings |
| `docs/component-library.md` | Katalog `ui/components/` yang wajib dipakai ulang, plus aturan ekstraksi (2+ pemakaian) |
| `docs/motion-rules.md` | Spring preset, aturan press-feedback, timing animasi |
| `docs/string-resources.md` | Aturan wajib: semua string user-facing lewat `strings.xml`, tidak pernah hardcode |

---

## 1. Langkah pertama: flat atau subfolder?

Setiap fitur baru mulai di `app/src/main/java/com/apps/unsealed/ui/screens/<feature>/`. Ada dua pola yang sudah dipakai
di codebase ini — **jangan pilih sembarangan**, ikuti kriteria ini:

| Pola | Kapan dipakai | Contoh nyata |
|---|---|---|
| **Flat** — `<Feature>Screen.kt`, `<Feature>ViewModel.kt`, `<Feature>UiState.kt` langsung di root folder fitur, tanpa subfolder | State lokal trivial, satu screen saja, tidak ada widget screen-local yang perlu dipisah | `stamps/` (`StampsScreen.kt`, `StampsViewModel.kt`, `StampsUiState.kt`, `StampsCatalog.kt`), `welcome/`, `onboarding/` |
| **Subfolder** — `screen/`, `state/`, `viewmodel/`, `widgets/`, (opsional) `constants/` | Multi-screen stack, state punya beberapa data class + mapping, atau screen-nya sudah dipecah jadi 3+ composable lokal | `inbox/`, `penpals/`, `profile/`, `selectrecipient/`, `tracking/`, `compose/` |

**Aturan praktis:** mulai flat. Begitu salah satu dari ini kejadian, pindahkan ke subfolder (jangan biarkan menumpuk di
root):
- State-nya butuh lebih dari 1 file (`UiState` + `Mapping` + `Item` data class) → buat `state/`
- Screen-nya dipecah jadi 3+ composable privat yang panjang → pindah ke `widgets/`, `<Feature>Screen.kt` masuk `screen/`
- Ada 2+ screen dalam satu fitur (misal fitur Profile yang punya 5 sub-screen) → wajib `screen/`
- Ada katalog/konfigurasi statis yang bukan network state (misal daftar interest, daftar warna, daftar planet)
  → `constants/`

Kalau fiturnya jelas dari awal bakal kompleks (multi-screen, butuh network), langsung mulai dengan struktur
subfolder — tidak perlu nunggu sampai file-nya menumpuk baru dipecah.

### Template subfolder lengkap

```
ui/screens/<feature>/
├── constants/            ← opsional, katalog statis (mis. PenPalsConstants.kt)
├── screen/
│   └── <Feature>Screen.kt        ← satu atau lebih, kalau multi-screen stack
├── state/
│   ├── <Feature>UiState.kt       ← sealed Loading/Success/Error
│   ├── <Feature>Mapping.kt       ← opsional, extension fn Dto → Item
│   └── <Feature>Item.kt          ← data class row/list model
├── viewmodel/
│   └── <Feature>ViewModel.kt
└── widgets/
    └── <WidgetName>.kt           ← composable privat fitur ini, BUKAN reusable lintas fitur
```

---

## 2. State layer — pola yang harus diikuti persis

Contoh nyata: `inbox/state/MailboxThreadUiState.kt`, `inbox/state/MailboxThreadMapping.kt`,
`inbox/state/ThreadLetterItem.kt`.

1. **Sealed `UiState`** — untuk kode baru pakai `sealed interface` (idiom lebih baru dari `sealed class`, sudah dipakai
   di `tracking/state/DeliveryTrackingUiState.kt`):
   ```kotlin
   sealed interface FeatureUiState {
       data object Loading : FeatureUiState
       data class Success(val items: List<FeatureItem>) : FeatureUiState
       data class Error(val message: String) : FeatureUiState
   }
   ```
   Tambahkan varian domain-specific kalau perlu (contoh `NotFound` di `PublicProfileUiState` — lihat
   `docs/api-integration-guide.md` §4).

2. **`<Feature>Mapping.kt`** (opsional, file terpisah) — extension function `DtoType.toXItem(...)` yang mengubah DTO
   API jadi UI state. Pisahkan ke file sendiri **hanya kalau** logic mapping-nya panjang/ada business rule (contoh:
   `MailboxThreadMapping.kt` menghitung `LetterDeliveryStatus` dari beberapa field DTO). Kalau mapping-nya sepele,
   taruh langsung di file `Item` yang sama (contoh: `PenpalLetter.kt` punya `fun FeedItemDto.toPenpalLetter(...)` di
   file yang sama dengan data class-nya) — jangan bikin file `Mapping.kt` kosongan kalau isinya cuma satu baris.

3. **`<Feature>Item.kt`** — `data class` polos untuk satu baris/kartu di list, setiap field dikomentari (lihat
   `ThreadLetterItem.kt`/`MailboxRoomItem.kt` sebagai contoh gaya komentar). Boleh punya computed property
   (`val isUnread: Boolean get() = ...`) untuk derived state yang dipakai UI.

Kalau screen-nya tidak punya lifecycle Loading/Success/Error yang ketat (misal banyak toggle/picker independen),
`data class` flat dengan default value juga valid — lihat `selectrecipient/state/SelectRecipientUiState.kt`.

---

## 3. ViewModel — pola ringkas

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val featureRepository: FeatureRepository,   // inject repository langsung, TIDAK ada use-case layer di codebase ini
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeatureUiState>(FeatureUiState.Loading)
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = FeatureUiState.Loading   // selalu reset dulu, cegah data lama kekilat
        viewModelScope.launch {
            _uiState.value = when (val result = featureRepository.getSomething()) {
                is AuthResult.Success -> FeatureUiState.Success(result.data.map { it.toFeatureItem() })
                is AuthResult.Error -> FeatureUiState.Error(result.message)
            }
        }
    }
}
```

Butuh nav-arg (misal `threadId` dari route)? Inject `SavedStateHandle savedStateHandle` di constructor, baca dengan
`savedStateHandle.get<String>(ArgKey)` — lihat `MailboxThreadViewModel.kt`. Butuh beberapa panggilan paralel? Bungkus
dengan `coroutineScope { async {...}; async {...} }.awaitAll()` (lihat `MailboxThreadViewModel.kt`).

**Kalau ViewModel ini juga butuh endpoint API baru** (DTO/Api/Repository baru), jangan improvisasi — ikuti
`docs/api-integration-guide.md` step 1-4 persis. Tidak perlu bikin Hilt module baru untuk fitur baru — Retrofit
sudah singleton di `NetworkModule`, repository baru cukup `@Singleton @Inject constructor(retrofit: Retrofit)`.

---

## 4. Sebelum nulis UI baru: cek komponen yang sudah ada

**Jangan langsung nulis `Button`/`Slider`/`Dialog` Material3 baru.** Urutan cek:

1. Buka `docs/component-library.md` — cek tabel komponen (`LetterlyButton`, `LetterlySlider`, `LetterlyMenuRow`,
   `LetterlyStoreCard`, `LetterlyScreenHeader`, `AnchoredDropdownMenu`, `LetterlyCenterDialog`, `ReportMailDialog`,
   dll).
2. **Dokumen itu bisa saja belum ter-update** — kalau ragu, langsung `ls app/src/main/java/com/apps/unsealed/ui/components/`
   untuk lihat isi folder yang sebenarnya, jangan cuma percaya daftar di doc.
3. Kalau kontrol yang kamu butuh sudah ada versi shared-nya → pakai itu, jangan tulis ulang styling sendiri.
4. Kalau kontrol ini kemungkinan besar dipakai di 2+ tempat dan belum ada shared version → tulis dulu di
   `ui/screens/<feature>/widgets/` sebagai kebutuhan lokal, baru **begitu ada pemakai kedua**, ekstrak ke
   `ui/components/` dan update `docs/component-library.md` (aturan resmi ada di dokumen itu §"Prinsip umum").
5. Kalau cuma dipakai sekali dan spesifik untuk fitur ini → biarkan di `widgets/`, tidak perlu dipaksa jadi shared
   component.

---

## 5. Warna — selalu dari `ui/theme/Color.kt`, jangan hex baru

Semua warna UI **harus** lewat token yang sudah ada di `ui/theme/Color.kt`. Ringkasan token utama (lihat file untuk
daftar lengkap + komentar fungsinya):

| Token | Fungsi |
|---|---|
| `BrandGold` / `BrandGoldDim` / `BrandGoldDeep` | CTA utama, subtitle/hint, pressed state |
| `BrandInk` / `BrandInkDeep` / `BrandInkMid` / `BrandInkAccent` | Teks di atas gold, dark bg (splash, inbox bg), gradient stop |
| `BrandCardDark` / `BrandCardStroke` | Frosted card bg + border di surface gelap |
| `SurfaceCream` / `SurfaceCardLight` / `SurfaceBorderLight` | Light-mode bg, panel, divider |
| `IceSkyBlue` / `IceSkyAccent` | Aksen tema Antarctic/ice (badge, accent) |
| `PaperCream`/`PaperStrawberry`/`PaperLavender`/`PaperMint`/`PaperSky` (via `PaperColor` enum) | Warna kertas surat, bukan brand chrome |
| `LemonYellow`, `ToolbarFrostedDark`, `InkDefault` | Token khusus canvas/annotate — cek komentarnya sebelum dipakai di konteks lain |

**Kalau butuh warna yang belum ada tokennya:**
1. Cek dulu apakah salah satu token yang ada (dengan `.copy(alpha = ...)`) sudah cukup — kebanyakan kebutuhan
   "warna lebih terang/transparan" cukup di-`copy(alpha = ...)`, tidak perlu token baru.
2. Kalau memang butuh warna baru (misal aksen semantik baru), tambahkan ke `Color.kt` dengan nama semantik
   (`BrandXxx`/`SurfaceXxx`/`PaperXxx`, bukan `Blue1`/`Color2`) + komentar 1 baris yang jelasin fungsinya, ikuti gaya
   komentar yang sudah ada di file itu — bukan file/objek warna terpisah per screen.
3. Jangan hardcode `Color(0xFF...)` langsung di composable screen kecuali untuk kasus yang memang didokumentasikan
   sebagai pengecualian (contoh: rose accent unread-dot di `MailboxRoomCard.kt` yang secara eksplisit dikomentari
   "matches StampDesign.ROSE" — itu pengecualian terdokumentasi, bukan pola default).

---

## 6. Motion — ikuti `docs/motion-rules.md`, jangan improvisasi animasi

Ringkasan yang paling sering kepake (detail lengkap di dokumen itu):

- Semua elemen tappable **wajib** `rememberPressScale(interactionSource)` + `.clickable(indication = null, ...)` —
  tidak ada ripple Material default di app ini sama sekali.
- Pilih spring preset dari `LetterlySpring` sesuai bobot elemen (`Snappy` tombol kecil, `Bouncy` FAB/card/stamp,
  `Gentle` modal/sheet, `Stiff` error/exit cepat, `Envelope` animasi amplop/kertas) — jangan hardcode
  `spring(dampingRatio=..., stiffness=...)` manual per screen.
- Selalu bungkus animation spec dengan `reducedMotionSpring(spec, rememberIsReducedMotion())` supaya user reduced-motion
  dapat `snap()` otomatis.
- List item masuk → `Modifier.staggerEntrance(index)` (40ms per item), jangan tulis ulang `LaunchedEffect(delay...)`
  manual kalau kasusnya sama persis.

---

## 7. Navigasi — daftarkan route di `UnsealedNavHost.kt`

Detail lengkap ada di `docs/api-integration-guide.md` §6, ringkasan cepat:

```kotlin
// Route + arg constants — di UnsealedNavHost.kt, dekat composable{} block-nya
internal const val FeatureRoute = "feature"
internal const val FeatureIdArg = "featureId"
private const val FeatureRoutePattern = "$FeatureRoute/{$FeatureIdArg}"
internal fun featureRoute(id: String) = "$FeatureRoute/${Uri.encode(id)}"   // selalu Uri.encode tiap segment

// Registrasi destination
composable(
    route = FeatureRoutePattern,
    arguments = listOf(navArgument(FeatureIdArg) { type = NavType.StringType }),
) { backStackEntry ->
    val id = backStackEntry.arguments?.getString(FeatureIdArg) ?: return@composable
    FeatureScreen(
        featureId = id,
        onBackClick = { navController.popBackStack() },
        onSomeAction = { arg -> navController.navigate(otherRoute(arg)) },
    )
}
```

- Tab utama (bottom nav) → route didaftarkan di `Destinations.kt` sebagai `sealed class Destinations`.
- Screen stack-only (bukan tab) → const route langsung di `UnsealedNavHost.kt`, dekat `composable{}`-nya.
- Screen baru yang bottom-nav-nya harus disembunyikan (mis. full-screen overlay/sub-screen) → tambahkan ke
  `HideBottomNavRoutes` set (lihat `docs/architecture.md` §7).
- Back selalu `navController.popBackStack()`; replace stack (auth/onboarding) pakai `popUpTo(...) { inclusive = true }`.

---

## 8. String resources

Semua teks user-facing **wajib** lewat `strings.xml` — tidak ada pengecualian, termasuk di komponen shared maupun
widget lokal. Detail penuh: `docs/string-resources.md`. Konvensi nama: `<feature>_<context>_<label>` (contoh:
`mailbox_full_view_envelope_desc`). Tambahkan ke **kedua** file locale: `res/values/strings.xml` (EN, default) dan
`res/values-id/strings.xml` (ID).

---

## 9. Checklist sebelum dianggap selesai

- [ ] Struktur folder sesuai §1 (flat kalau sederhana, subfolder kalau kompleks — bukan campuran acak)
- [ ] `UiState` sealed interface dengan minimal Loading/Success/Error, atau alasan jelas kalau tidak dipakai
- [ ] ViewModel: `@HiltViewModel`, inject repository langsung, `MutableStateFlow` privat + `StateFlow` publik
- [ ] Endpoint API baru (kalau ada) mengikuti `docs/api-integration-guide.md` persis
- [ ] Cek `ui/components/` dulu sebelum nulis kontrol UI baru (§4)
- [ ] Warna dari `Color.kt`, tidak ada hex baru tanpa alasan terdokumentasi (§5)
- [ ] Semua tappable pakai `rememberPressScale` + `indication = null`, spring preset dari `LetterlySpring` (§6)
- [ ] Route terdaftar di `UnsealedNavHost.kt`, `Uri.encode` untuk arg, bottom-nav visibility diatur kalau perlu (§7)
- [ ] Semua string lewat `strings.xml` di **kedua** locale (§8)
- [ ] Kalau ada komponen UI baru yang dipakai 2+ tempat → sudah diekstrak ke `ui/components/` + `docs/component-library.md` di-update

---

*Unsealed — Panduan Scaffold Fitur Baru · rujuk `docs/architecture.md` untuk gambaran besar, dokumen ini untuk langkah konkret.*
