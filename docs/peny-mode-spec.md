# Mode Peny — Mobile Implementation Spec
*Android · Jetpack Compose · nempel di Compose Screen yang sudah ada*

---

## Status Implementasi

| Bagian | Status | Catatan |
|---|---|---|
| 1. Network layer (`PenyApi`/`PenyRepository`/DTO) | 🔜 P0 | Belum ada sama sekali. BE sudah ready (`docs/be_updet/peny_mode_mobile_integration.md`, `docs/api_contract.md` §`peny`). |
| 2. `ComposeUiState` — field mode Peny | 🔜 P0 | Belum ada. Lihat §3. |
| 3. `ComposeViewModel` — trigger 3-lapis + panggilan API | 🔜 P0 | Belum ada. Lihat §4. |
| 4. Trigger sihir di kertas | 🔜 P0 | Bentuk visual & posisi belum didesain final — lihat §5.1, masih ada keputusan terbuka. |
| 5. Glow indicator (3 state) | 🔜 P1 | Lihat §5.2. |
| 6. Reveal tinta-merembes (balasan Peny) | 🔜 P0 | MVP: fade-in opacity, bukan shader "tinta basah". Lihat §5.3. |
| 7. Kunci ornamen selama mode aktif | 🔜 P0 | Disable Annotate/Overflow/FormattingBar controls. Lihat §5.4. |
| 8. Respons lokal (Lapis 2 — sapaan & kata ambigu) | 🔜 P1 | Daftar variasi belum ditulis. Lihat §4.2. |
| 9. Gating (`is_enabled` + `isPremium`) | 🔜 P0 | Lihat §6. |
| 10. Error handling per skenario | 🔜 P0 | Lihat §7. |
| 11. String resources | 🔜 P0 | Lihat §9. |

Legenda status sama seperti `compose-screen-spec.md`: 🔴/🔜 belum dibangun, 🟡 sebagian, ✅ selesai.

---

## 1. Ringkasan & Referensi

Mode Peny **bukan screen baru** — dia adalah state tambahan di `ComposeScreen.kt` yang sudah ada (`app/src/main/java/com/apps/unsealed/ui/screens/compose/`). Dokumen ini adalah breakdown teknis mobile-side, turunan dari dua dokumen lain:

| Dokumen | Isinya |
|---|---|
| Draft konsep produk (percakapan yang menghasilkan dokumen ini) | Rationale desain: kenapa state-toggle bukan layer, kenapa stateless, persona Peny, boundary rules, threshold 3-lapis, visual & animasi. **Rujuk balik ke situ untuk "kenapa"** — dokumen ini fokus ke "bagaimana implementasinya di kode yang sudah ada". |
| `docs/be_updet/peny_mode_mobile_integration.md` | Kontrak alur API dari sisi client: kapan panggil apa, apa yang disimpan di mana, error handling per case. **Sumber kebenaran untuk network layer.** |
| `docs/api_contract.md` §`peny` | Shape DTO mentah, final. |
| `docs/compose-screen-spec.md` | Spec Compose Screen yang sudah ada — Mode Peny numpang di struktur ini (toolbar, `LetterCanvas`, `FormattingBar`, dst). |

**Prinsip implementasi yang paling penting untuk dijaga saat coding:** hampir seluruh fitur ini murni client-side (lihat tabel §1 di `peny_mode_mobile_integration.md`) — cuma **satu** titik nyentuh network (`POST /peny/reply`). Jangan overengineer network layer untuk kasus yang sebenarnya harus zero-network (Lapis 1 & 2 di §4).

---

## 2. Struktur Folder

Ikuti `docs/new-feature-guide.md` §1 — fitur ini numpang di `compose/` yang sudah subfolder, jadi tambahan file mengikuti pola yang sama, bukan bikin top-level feature folder baru:

```
app/src/main/java/com/apps/unsealed/
├── feature/peny/data/                      ← baru, mengikuti pola feature/rewards/data
│   ├── PenyApi.kt                          ← Retrofit interface, 2 endpoint
│   ├── PenyDto.kt                          ← request/response DTO
│   └── PenyRepository.kt                   ← @Singleton, pola sama RewardsRepository
│
└── ui/screens/compose/
    ├── state/
    │   ├── ComposeUiState.kt               ← tambah field di §3
    │   └── PenyTurn.kt                     ← baru — data class 1 giliran percakapan
    ├── viewmodel/
    │   └── ComposeViewModel.kt             ← tambah handler di §4
    └── widgets/
        ├── PenyTriggerGlyph.kt             ← baru — trigger sihir + glow, lihat §5.1/5.2
        └── PenyReplyOverlay.kt             ← baru — balasan di zona tengah, lihat §5.3
```

`feature/peny/` (bukan `feature/compose/`) karena network layer ini mandiri secara domain (bisa saja dipakai ulang di luar compose screen suatu saat) — sama alasannya kenapa `feature/rewards/` terpisah dari screen yang memakainya (`ui/screens/stamps/`).

---

## 3. `ComposeUiState` — Field Baru

Nambah ke `ComposeUiState.kt` (`app/src/main/java/com/apps/unsealed/ui/screens/compose/state/ComposeUiState.kt`), pola penamaan & komentar mengikuti gaya field yang sudah ada di file itu (lihat `annotateState`, `isAnnotateMode` sebagai preseden field mode serupa):

```kotlin
data class ComposeUiState(
    // ...existing fields...

    /** True selama Mode Peny aktif — TextField "diambil alih" jadi
     * percakapan sementara (lihat draft produk §3). [bodyText] TIDAK
     * disentuh selama true; kertas menampilkan [penyHistory]/[penyDraftInput]
     * alih-alih body surat. Hanya bisa true kalau [isPenyModeAvailable]. */
    val isPenyModeActive: Boolean = false,

    /** `GET /peny/status`'s `is_enabled` AND `isPremium` dari `GET /auth/me`
     * (§6) — dua gate independen di backend, tapi satu boolean di UI:
     * kalau false, trigger sihir sama sekali tidak dirender (bukan
     * render-lalu-disable, lihat peny_mode_mobile_integration.md §2). */
    val isPenyModeAvailable: Boolean = false,

    /** Percakapan sesi ini — **in-memory saja**, tidak pernah ditulis ke
     * Room/DataStore, tidak termasuk field yang di-snapshot Draft-on-Exit
     * (§11 compose-screen-spec.md). Di-drop total begitu [isPenyModeActive]
     * kembali false (§4.4) — "rahasia yang lenyap, bukan yang dikoleksi". */
    val penyHistory: List<PenyTurn> = emptyList(),

    /** Draft mentah yang sedang diketik user di TextField saat Mode Peny
     * aktif — analog ke [bodyText] tapi untuk percakapan Peny, terpisah
     * biar [bodyText] surat asli benar-benar tidak tersentuh (draft
     * produk §3 "batasnya otomatis lewat state"). Direset kosong begitu
     * turn ini selesai diserap (lihat §4.3 animasi absorb). */
    val penyDraftInput: String = "",

    /** True sejak `POST /peny/reply` dikirim sampai response/error datang —
     * drives glow's "processing" state (§5.2) dan disable input sementara. */
    val isPenyReplyLoading: Boolean = false,

    /** Kalau non-null, balasan Peny (atau chunk pertama dari multi-part
     * fallback, §5.3 catatan "safety net") sedang tampil di zona tengah
     * lewat reveal fade-in. Null = zona balasan kosong. */
    val penyVisibleReply: String? = null,

    /** Sisa chunk dari multi-part fallback (§5.3) — biasanya kosong.
     * Kalau tidak kosong, begitu [penyVisibleReply] fade-out, item
     * pertama di list ini dipop jadi [penyVisibleReply] berikutnya. */
    val penyPendingReplyChunks: List<String> = emptyList(),

    /** Pesan error ramah-Peny (bukan raw API error) untuk kasus
     * `INSUFFICIENT_ENERGY` — lihat §7. Null = tidak ada error aktif. */
    val penyEnergyErrorMessage: String? = null,
)
```

`PenyTurn.kt` (file baru, `ui/screens/compose/state/`):

```kotlin
package com.apps.unsealed.ui.screens.compose.state

/** Satu giliran percakapan Mode Peny — dikirim ulang penuh tiap request
 * (`peny_mode_mobile_integration.md` §2 langkah 4), jadi shape-nya harus
 * match persis `history` array yang diterima backend (`api_contract.md`
 * §peny), bukan model UI bebas. */
data class PenyTurn(
    val role: PenyRole,
    val text: String,
)

enum class PenyRole { USER, PENY }
```

**Kenapa bukan expose `AuthResult`/loading sealed state seperti fitur lain:** compose-screen-spec.md §9 sudah menetapkan `ComposeUiState` sebagai satu `data class` flat (bukan sealed Loading/Success/Error) karena screen ini banyak toggle/picker independen, bukan satu lifecycle linear. Mode Peny ikut pola yang sama — `isPenyReplyLoading` sebagai boolean terpisah, bukan membungkus seluruh `ComposeUiState` jadi sealed state.

---

## 4. `ComposeViewModel` — Logic Threshold 3-Lapis

Ini bagian paling substansial. Rujuk `peny_mode_mobile_integration.md` §1 untuk tabel lapisnya; berikut breakdown implementasi tiap lapis di `ComposeViewModel.kt`.

### 4.1 Lapis 1 — Animasi serap (selalu jalan, zero-network)

Handler baru `onPenyDraftInputChange(text: String)`, dipanggil dari `LetterCanvas`'s TextField saat `isPenyModeActive == true` (ganti `onBodyTextChange` yang biasa dipakai). Update `penyDraftInput` di state, murni — tidak ada logic threshold di sini, cuma nyalain efek visual "diserap" (state `isPenyModeActive` yang sudah true sudah cukup buat `PenyTriggerGlyph`/glow merender state aktif, lihat §5.2).

### 4.2 Lapis 2 — Respons lokal (hardcode, tanpa API)

Object baru `PenyLocalResponses` (taruh di `ui/screens/compose/constants/PenyLocalResponses.kt`, ikuti pola `constants/` yang sudah ada seperti `GoogleFontCatalog.kt` — daftar statis, bukan network state):

```kotlin
object PenyLocalResponses {
    private val Greetings = listOf(/* 5-8 variasi, lihat catatan §8 soal isi copy */)
    private val AmbiguousInvite = listOf(/* 5-8 variasi */)

    private val GreetingPattern = Regex("^(hai|halo|hello|hi|woy|yo)[\\s!.,]*$", RegexOption.IGNORE_CASE)

    /** Null kalau input tidak match pola manapun — caller lanjut ke Lapis 3. */
    fun matchLocal(input: String): String? {
        val trimmed = input.trim()
        return when {
            GreetingPattern.matches(trimmed) -> Greetings.random()
            trimmed.split(Regex("\\s+")).size in 1..3 && !looksLikeSentence(trimmed) -> AmbiguousInvite.random()
            else -> null
        }
    }

    private fun looksLikeSentence(s: String) = s.endsWith(".") || s.endsWith("?") || s.endsWith("!")
}
```

`looksLikeSentence` mengimplementasikan catatan draft produk §9: tanda baca penutup ("capek.") jadi sinyal "cukup bermakna" meski di bawah threshold kata — kalau true, **jangan** match ke `AmbiguousInvite`, biarkan lanjut ke Lapis 3.

### 4.3 Lapis 3 — Panggilan AI (debounce + threshold)

Ada preseden persis untuk pola "debounce sejak jeda ngetik terakhir" di file yang sama: `TextUndoDebounceMillis = 600L` (§1 Undo/Redo, `compose-screen-spec.md`). Pakai pola `Job` yang sama, cuma constant beda:

```kotlin
private const val PenyReplyDebounceMillis = 1_800L  // draft produk: 1.5-2 detik
private const val PenyMinWordThreshold = 4

private var penyDebounceJob: Job? = null

fun onPenyDraftInputChange(text: String) {
    _uiState.update { it.copy(penyDraftInput = text) }
    penyDebounceJob?.cancel()
    penyDebounceJob = viewModelScope.launch {
        delay(PenyReplyDebounceMillis)
        handlePenyTurnSubmit(text)
    }
}

private fun handlePenyTurnSubmit(message: String) {
    if (message.isBlank()) return
    val local = PenyLocalResponses.matchLocal(message)
    if (local != null) {
        appendPenyTurn(message, local)
        return
    }
    val wordCount = message.trim().split(Regex("\\s+")).size
    if (wordCount < PenyMinWordThreshold) return  // belum cukup bermakna, tunggu ketikan lanjutan
    requestPenyReply(message)
}
```

> **Catatan implementasi:** kode di atas contoh dari draft awal — implementasi aktual di `PenyLocalResponses.kt`/`ComposeViewModel.kt` sudah berubah dari beberapa hal yang ditulis di sini: `AmbiguousInvite` match cuma untuk **satu kata bare** (bukan rentang 1-3 kata seperti contoh §4.2), karena frasa 2 kata kayak "kamu siapa" ternyata pertanyaan asli yang harus nyampe ke AI, bukan pembuka ambigu — jadi `PenyMinWordThreshold` juga diturunin dari `4` jadi `2` biar tetep nutup celah pesan 2-3 kata yang sekarang lolos dari Lapis 2.
>
> **Bug (fixed):** baik `PenyLocalResponses.matchLocal` maupun `PenyMinWordThreshold` gate di atas awalnya berlaku di **setiap** turn, bukan cuma pembuka percakapan. Gejalanya: pas lagi di tengah percakapan dan Peny nanya sesuatu, jawaban satu kata dari user ("iya", "boleh", "nggak") ke-treat sama kayak kalimat pembuka yang vague ("capek", "libur") — kalau nggak diakhiri tanda baca, langsung dibales canned response `AmbiguousInvite` yang gak nyambung sama sekali; kalau di bawah `PenyMinWordThreshold`, malah hilang diam-diam (nggak dibales apa-apa, karena nggak ada re-trigger sampai user ngetik lagi). Dari sisi user kerasa kayak Mode Peny random ngabaikan jawabannya.
>
> **Fix:** `matchLocal` sekarang nerima parameter `isFirstTurn` (dari `uiState.value.penyHistory.isEmpty()` di `handlePenyTurnSubmit`), dan baik cabang `AmbiguousInvite` maupun gate `PenyMinWordThreshold` cuma aktif kalau `isFirstTurn == true`. Begitu percakapan udah jalan, pesan sependek apa pun (termasuk satu kata) langsung diteruskan ke Lapis 3 apa adanya — sesuai maksud awal heuristik ini yang cuma buat disambiguasi kalimat *pembuka*, bukan buat nyaring tiap balasan di tengah obrolan.

`requestPenyReply` yang manggil network — **satu-satunya** tempat di seluruh fitur ini yang bikin `viewModelScope.launch { penyRepository.reply(...) }`:

```kotlin
private fun requestPenyReply(message: String) {
    _uiState.update { it.copy(isPenyReplyLoading = true, penyEnergyErrorMessage = null) }
    viewModelScope.launch {
        val historyBeforeThisTurn = uiState.value.penyHistory
        when (val result = penyRepository.reply(historyBeforeThisTurn, message)) {
            is AuthResult.Success -> {
                appendPenyTurn(message, result.data.reply)
                // current_energy_balance: update cache lokal saldo Energy di sini
                // (lihat repo yang sudah nyimpen saldo Energy dari unlockStamp/claimQuest,
                // reuse sumber yang sama — jangan bikin cache energy kedua).
            }
            is AuthResult.Error -> handlePenyError(result)
        }
        _uiState.update { it.copy(isPenyReplyLoading = false) }
    }
}

private fun appendPenyTurn(userMessage: String, penyReply: String) {
    _uiState.update {
        it.copy(
            penyDraftInput = "",  // "diserap" — TextField kosong lagi
            penyHistory = it.penyHistory + PenyTurn(PenyRole.USER, userMessage) + PenyTurn(PenyRole.PENY, penyReply),
            penyVisibleReply = penyReply,  // trigger reveal, §5.3
        )
    }
}
```

**Catatan penting soal `penyHistory` yang dikirim:** `peny_mode_mobile_integration.md` §2 langkah 4 eksplisit — `history` di body request **belum** termasuk giliran yang baru ini, makanya `historyBeforeThisTurn` diambil sebelum `appendPenyTurn` dipanggil.

### 4.4 Keluar dari Mode Peny

```kotlin
fun onPenyModeToggle() {
    val exiting = uiState.value.isPenyModeActive
    penyDebounceJob?.cancel()
    _uiState.update {
        it.copy(
            isPenyModeActive = !exiting,
            penyHistory = emptyList(),          // drop total, baik masuk maupun keluar
            penyDraftInput = "",
            penyVisibleReply = null,
            penyPendingReplyChunks = emptyList(),
            isPenyReplyLoading = false,
            penyEnergyErrorMessage = null,
        )
    }
}
```

`penyHistory = emptyList()` dipanggil baik saat **masuk** maupun **keluar** — bukan cuma keluar. Ini sengaja: kalau sesi sebelumnya sempat pop-up lagi (misal user toggle off lalu on lagi dalam satu kunjungan screen), sesi baru harus mulai bersih, bukan nyambung history lama (stateless per aktivasi, bukan cuma stateless per screen — konsisten dengan "rahasia yang lenyap" tapi diperketat ke level toggle, bukan cuma level screen-lifecycle).

`bodyText` (draft surat asli) **tidak disentuh sama sekali** di handler ini — itu poin utama kenapa ini state toggle, bukan layer baru.

---

## 5. Visual & Animasi

### 5.1 Trigger Sihir — Keputusan Terbuka

Draft produk §10 eksplisit belum memutuskan bentuk konkretnya. Untuk implementasi awal, opsi paling murah untuk dibangun & dites (bisa diganti tanpa mengubah arsitektur state di §3/§4):

- **Small persistent glyph** di pojok kertas (mirip posisi elemen dekoratif lain di `LetterCanvas`) — tap toggle `onPenyModeToggle()`. Tidak butuh keyboard/teks trigger apa pun, cuma satu tap target. **Direkomendasikan untuk P0** karena tidak butuh parsing teks tambahan di `onBodyTextChange` yang sudah ada (kalau triggernya berupa teks yang diketik di draft asli, itu perlu regex-watch tambahan yang mengganggu jalur `bodyText` yang sudah kompleks — lihat rich-text run system di §9 compose-screen-spec.md, menambah side-channel parsing di situ berisiko).
- Alternatif teks-trigger (misal user mengetik `":peny"` atau semacamnya di draft) ditunda ke P2 kalau glyph-based terasa kurang "magis" setelah dites — bisa ditambah sebagai trigger kedua tanpa mengubah `isPenyModeActive` toggle yang sudah ada.

Widget baru `PenyTriggerGlyph.kt` — composable kecil, `rememberPressScale` + `LetterlySpring.Snappy` untuk tap feedback (motion-rules.md §3.1, bukan tombol besar jadi bukan `Bouncy`), ditempatkan di `LetterCanvas.kt`'s layer stack (lihat komentar layer di file itu) sebagai layer baru di atas Layer 1 (paper), sejajar posisi elemen dekoratif lain.

### 5.2 Glow Indicator — 3 State

Bukan komponen shared baru di `ui/components/` (dipakai sekali, spesifik fitur ini — ikuti aturan `new-feature-guide.md` §4 poin 5), taruh di `PenyTriggerGlyph.kt` atau file terpisah kalau logic-nya besar.

| State | Kondisi | Visual |
|---|---|---|
| Nonaktif | `!isPenyModeActive` | Glow mati/tidak dirender |
| Aktif | `isPenyModeActive && !isPenyReplyLoading` | Glow menyala steady |
| Memproses | `isPenyModeActive && isPenyReplyLoading` | Glow pulse/breathing loop |

Warna: turunan `BrandGold`/`LemonYellow` (sudah dipakai untuk state "aktif" di toolbar Annotate — lihat `ComposeToolbar.kt`'s `LemonYellow` background pill untuk `isAnnotateMode`, preseden warna "mode ON" yang sama persis bisa dipakai ulang) dengan alpha/blur, **bukan** token warna baru — ikuti `new-feature-guide.md` §5.

Pulse loop pakai `infiniteRepeatable` — satu-satunya kasus di motion-rules.md yang eksplisit boleh linear/infinite (lihat §3.10 "Mood Delivery Banner" icon rotation sebagai preseden loading-loop), bukan pelanggaran aturan "no LinearEasing" karena itu aturannya untuk animasi respons-tap, bukan indikator loading state.

### 5.3 Reveal Tinta-Merembes (Balasan Peny)

Widget baru `PenyReplyOverlay.kt`. Diposisikan **tengah kertas**, terpisah dari zona tulis (draft produk §7) — di `LetterCanvas.kt`, ini masuk sebagai layer baru di antara paper background dan TextField (di bawah TextField secara z-order tidak masalah karena TextField-nya sendiri disembunyikan/diganti saat `isPenyModeActive`, lihat §5.4).

```kotlin
@Composable
fun PenyReplyOverlay(
    reply: String?,
    modifier: Modifier = Modifier,
) {
    val isReducedMotion = rememberIsReducedMotion()
    AnimatedVisibility(
        visible = reply != null,
        enter = fadeIn(reducedMotionSpring(tween(700), isReducedMotion)),
        exit = fadeOut(reducedMotionSpring(tween(400), isReducedMotion)),
        modifier = modifier.align(Alignment.Center),
    ) {
        Text(
            text = reply.orEmpty(),
            style = LetterBodyStyle,  // font surat yang sedang aktif, konsisten visual
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = LocalCanvasWidth.current * 0.75f), // §7 draft produk: ±70-80%
        )
    }
}
```

MVP: `fadeIn`/`fadeOut` `tween` polos (draft produk §7 eksplisit memilih ini di atas shader bleeding-effect untuk MVP, alasan performa di range device Android luas — sama alasan device-capability tiering yang sudah ada, lihat `docs/be_updet/device_capability_tiering.md` dan `MinCompositingDensity` yang sudah dipakai `ComposeScreen.kt` untuk gating fitur compositing berat). **Jangan** implementasi custom shader/`RenderEffect` blur-edge di P0 — itu upgrade path eksplisit, bukan requirement awal.

Durasi 700ms enter sedikit di atas batas motion-rules.md §4 ("Max 350ms untuk feedback langsung") — ini bukan pelanggaran karena reveal balasan bukan feedback-tap instan, dia lebih dekat ke kategori "Sheet masuk" (350-400ms, `Gentle`) yang di §6 motion-rules.md eksplisit boleh lebih lambat. Sesuaikan ke rentang ±500-800ms sesuai draft produk §7's "±0.5-1 detik" saat playtesting nyata, jangan angka final di kode ini.

**Multi-part fallback** (safety net, draft produk §7): kalau `response.reply` di-split jadi >1 chunk (deteksi panjang kata di client, atau backend sudah split — cek `api_contract.md` untuk final shape, dokumen ini asumsikan client yang split), `penyPendingReplyChunks` dipop begitu `PenyReplyOverlay`'s `AnimatedVisibility` `exit` selesai (`onExitFinished` callback Compose) — bukan pola utama, jangan desain animasi tambahan untuk transisi antar-chunk, cukup reuse reveal yang sama.

### 5.4 Kunci Ornamen Selama Mode Aktif

Di `ComposeScreen.kt`, titik-titik yang perlu digate dengan `!uiState.isPenyModeActive`:

```kotlin
onAnnotateClick = {
    if (uiState.isPenyModeActive) return@ComposeToolbar  // atau disable ikonnya, bukan cuma no-op
    // ...existing logic...
}
```

Sama untuk `onChangePaperClick`, `onInsertImageClick`, `onAddStickerClick`, dan `FormattingBar` (jangan dirender sama sekali saat `isPenyModeActive`, sama pola `if (uiState.isAnnotateMode) ... else if (uiState.isKeyboardVisible) ...` yang sudah ada di baris terakhir `ComposeScreen.kt` — tambah satu cabang `if (uiState.isPenyModeActive) { /* tidak render FormattingBar/AnnotateToolbar */ }` di depan cabang yang sudah ada).

Lebih rapi: tambahkan `disabled` param eksplisit ke `ComposeToolbar` (`isPenyModeActive: Boolean`) daripada mengulang `if` di tiap `onXClick` — icon-icon yang di-disable turunkan opacity (pola yang sama seperti Annotate OFF state di §1 compose-screen-spec.md, opacity 60%) dan `clickable(enabled = false)`.

---

## 6. Gating: `is_enabled` + `isPremium`

`ComposeViewModel.init` (tempat resolve reply-context yang sudah ada, lihat compose-screen-spec.md §1's "Sticky Reply Banner" — pola inisialisasi serupa) tambah:

```kotlin
init {
    // ...existing reply-context resolution...
    viewModelScope.launch {
        val statusEnabled = when (val result = penyRepository.getStatus()) {
            is AuthResult.Success -> result.data.isEnabled
            is AuthResult.Error -> false  // fail-closed: network error = trigger disembunyikan, bukan crash
        }
        val isPremium = authRepository.currentUser.value?.isPremium ?: false  // cek yang mana sumber cache-nya
        _uiState.update { it.copy(isPenyModeAvailable = statusEnabled && isPremium) }
    }
}
```

Sesuai `peny_mode_mobile_integration.md` §2 langkah 2: **jangan** buat dua flag terpisah di UI (`isEnabled`/`isPremium`) — gabung jadi satu `isPenyModeAvailable`, karena dari sisi UI keduanya sama-sama alasan untuk **tidak menampilkan** trigger, user tidak perlu tau bedanya. `PenyTriggerGlyph` di `LetterCanvas` cuma dirender kalau `uiState.isPenyModeAvailable` — bukan dirender-lalu-disabled.

Cek dulu nama field/method persis di `AuthRepository`/`authRepository.currentUser` sebelum implementasi — grep `isPremium` di `AuthDto.kt` untuk shape yang sudah ada (`GET /auth/me` sudah punya field ini, tidak perlu request baru, sesuai instruksi eksplisit di `peny_mode_mobile_integration.md` §2).

---

## 7. Error Handling

Tabel lengkap sudah ada di `peny_mode_mobile_integration.md` §3 — breakdown implementasi per case:

| Case | Implementasi client |
|---|---|
| `400 message is required/too long` | Tidak butuh UI — kalau kejadian berarti bug di threshold client (§4.3). Log via Crashlytics (`crashlytics.recordException`, pola sama semua repository lain), jangan tampilkan apa pun ke user. |
| `400 INSUFFICIENT_ENERGY` | Set `penyEnergyErrorMessage` ke copy hangat ala-Peny (bukan toast generik) — **jangan** append giliran user ke `penyHistory` (biarkan `penyDraftInput` tetap ada/reset sesuai desain "diserap", user retry setelah dapat Energy). Render sebagai bagian dari `PenyReplyOverlay` (pakai jalur reveal yang sama, bukan komponen error terpisah — supaya tidak merusak ilusi "kertas hidup", persis prinsip §8 draft produk). |
| `403 premium only` | Seharusnya tidak kejadian (§6 sudah gate). Fallback: `isPenyModeAvailable = false`, tampilkan paywall/upgrade prompt yang sudah ada di app (cek pola existing, mis. `showPublicShowcaseDialog`-style dialog atau redirect ke halaman upgrade). |
| `403 unavailable` | Fallback: set `isPenyModeAvailable = false`, `isPenyModeActive = false`, pesan singkat. |
| `500` | Energy tidak terpotong — retry aman. Animasi "gagal" halus: reuse `PenyReplyOverlay`'s fade tapi dengan copy netral ("...sepertinya kertasnya lagi diem, coba lagi?" — gaya tetap Peny, bukan dialog error teknis, sesuai §8 draft produk prinsip umum "tidak lewat penolakan eksplisit"). |

Semua pesan (energy error, 500 fallback, paywall) lewat `strings.xml` (§9) — **jangan** hardcode string Kotlin (aturan project, lihat memory `string-resources.md`).

---

## 8. Persona & Konten (Bukan Kode)

Bagian ini di luar scope kode client — tapi ada dua daftar yang **harus** ada sebelum P0 bisa selesai, dan keduanya hidup di client (bukan server):

1. **`PenyLocalResponses`** (§4.2) — isi lengkap daftar sapaan (5-8 variasi) & respons generik-mengundang (5-8 variasi), gaya bicara Peny sesuai kerangka karakter draft produk §5. Ini bukan keputusan teknis, tapi harus ditulis manual (bukan AI-generated on-the-fly) karena §4 `peny_mode_mobile_integration.md` menegaskan Lapis 2 sengaja zero-network.
2. **Copy error/fallback** (§7 di atas) — 2-3 variasi tiap kategori (energy habis, 500 gagal) supaya tidak berulang, ditulis dengan gaya yang sama seperti `docs/copywriting/cp.md` (lihat contoh `save_draft_dialog_*` di compose-screen-spec.md §11 sebagai referensi format).

System prompt/persona/few-shot examples untuk sisi AI (server) **bukan** tanggung jawab dokumen/kode ini — itu di `peny_config` sisi backend (`peny_mode_mobile_integration.md` §4), client tidak pernah kirim/lihat system prompt.

---

## 9. String Resources

Semua string baru masuk `res/values/strings.xml` (EN) + `res/values-id/strings.xml` (ID), prefix `compose_peny_*` (konsisten `<feature>_<context>_<label>` dari `new-feature-guide.md` §8):

- `compose_peny_trigger_content_desc` — accessibility label trigger glyph
- `compose_peny_energy_insufficient_*` (beberapa variasi, §8)
- `compose_peny_reply_failed_*` (beberapa variasi, §8)
- `compose_peny_unavailable_message`
- `compose_peny_local_greeting_*` / `compose_peny_local_invite_*` — kalau daftar §4.2/§8 ditaruh di `strings.xml` alih-alih hardcode array Kotlin (lebih disukai — konsisten dengan aturan string project, memudahkan translator/copywriter edit tanpa sentuh kode)

**Keputusan implementasi:** daftar respons lokal (§4.2) sebaiknya diambil dari `strings.xml` via `stringArrayResource` atau daftar `stringResource` individual di composable, bukan `object PenyLocalResponses` dengan string Kotlin literal seperti contoh kode di §4.2 — contoh kode di situ disederhanakan untuk fokus ke logic matching-nya, implementasi aktualnya harus baca dari resource. Update §4.2 saat implementasi nyata supaya konsisten.

---

## 10. Checklist Implementasi (Urutan Disarankan)

| Urutan | Task | Kenapa urutan ini |
|---|---|---|
| 1 | `PenyApi`/`PenyDto`/`PenyRepository` (§ new-feature-guide.md `api-integration-guide.md` step 1-4) | Fondasi — semua layer di atasnya butuh ini |
| 2 | `ComposeUiState` field + `PenyTurn.kt` (§3) | Shape data dulu sebelum logic |
| 3 | Gating init logic (§6) | Supaya bisa ditest "trigger muncul/tidak" sebelum bangun UI kompleks |
| 4 | `ComposeViewModel` Lapis 3 (API call) tanpa UI dulu (§4.3-4.4) — test lewat logcat/breakpoint | Validasi kontrak network sebelum invest waktu di animasi |
| 5 | `PenyTriggerGlyph` + toggle wiring di `ComposeScreen.kt` (§5.1, §5.4 ornament lock) | UI minimal buat bisa masuk/keluar mode |
| 6 | `PenyReplyOverlay` reveal (§5.3), sambungkan ke Lapis 3 | Baru di titik ini fitur end-to-end bisa ditest manual |
| 7 | Lapis 2 lokal (§4.2, §8 isi konten) | Optimasi biaya — bisa nyusul setelah Lapis 3 kebukti jalan |
| 8 | Glow 3-state (§5.2) | Polish visual, bukan blocker fungsional |
| 9 | Error handling lengkap semua case (§7) | Setelah happy path stabil |
| 10 | String resources final kedua locale (§9) | Terakhir, setelah copy final |

---

## 11. Pertanyaan Terbuka (Belum Bisa Diputuskan dari Dokumen Ini Saja)

- Bentuk final trigger sihir (§5.1) — glyph vs teks-trigger, butuh keputusan desain/produk sebelum P0 dianggap selesai, bukan cuma keputusan teknis.
- Apakah `history` yang dikirim ke `POST /peny/reply` dibatasi panjangnya (mis. cuma N giliran terakhir) kalau sesi ngobrol panjang — `api_contract.md` perlu dicek untuk ada/tidaknya limit server-side; kalau tidak ada, perlu diputuskan apakah client harus truncate sendiri sebelum kirim (biar payload tidak membengkak tanpa batas dalam satu sesi panjang).
- Nama/lokasi pasti method `authRepository`/state holder untuk `isPremium` cache saat ini (§6) — perlu dicek langsung di kode `feature/auth/` saat implementasi, jangan asumsikan nama dari dokumen ini.
- Apakah multi-part fallback split (§5.3) dilakukan di client atau backend sudah mengirim array chunk — `api_contract.md` §peny final harus dicek, dokumen ini asumsikan client-side split sebagai default aman.

---

*Mode Peny Mobile Spec v1.0 · Android · Jetpack Compose · rujuk `docs/compose-screen-spec.md` untuk struktur screen yang ditempeli, `docs/be_updet/peny_mode_mobile_integration.md` untuk kontrak API.*
