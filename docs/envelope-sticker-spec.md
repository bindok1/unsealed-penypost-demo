# Envelope Sticker + Composite Cover — Implementation Spec (FE)
*Pre-implementation draft · Android · Jetpack Compose · pairs with `docs/be/envelope_customization_api.md`*

Belum ada kode ditulis untuk fitur ini — dokumen ini spec buat mulai implementasi, bukan catatan retroaktif kayak `docs/penpals-screen-spec.md`. Update status table di bawah begitu tiap bagian selesai.

---

## Status

| Bagian | Status | Catatan |
|---|---|---|
| Sticker catalog (`GET /stickers`) fetch | 🟢 | `CatalogApi`/`CatalogRepository` punya `getStickers()`, di-fetch paralel di `SelectRecipientViewModel`, `OpenLetterViewModel`, `PenPalsViewModel` |
| `EnvelopeEditToolbar.onStickerClick` jadi picker beneran | 🟢 | `onStickerToggleRequest()` — fallback ke `ComingSoonBottomSheet` selama catalog kosong (backend belum live), switch otomatis begitu ada isinya |
| `StickerPickerOverlay.kt` | 🟢 | Baru, mirror `StampPickerOverlay.kt`, deselect via badge "X" |
| `EnvelopeCard.kt` sticker slot | 🟢 | Anchor `BottomEnd` selalu ada (shared-bounds `StickerSheetSharedKey`), `EnvelopeStickerBadge` mirror `StampArrival` |
| Shared compositing constant extraction | 🟢 | Pindah ke `core/util/CompositingCapability.kt`, `LetterCompositor.kt` dihapus, `ComposeScreen.kt` diupdate importnya |
| Capture + upload di Send flow | 🟢 | `SelectRecipientScreen` capture `GraphicsLayer` + `SelectRecipientViewModel.onSendClick(bytes)` presign/upload lalu kirim `envelope_sticker_id`/`envelope_composite_image_url` |
| Render sisi penerima (Inbox + PenPals feed) | 🟢 | `OpenLetterScreen`/`DeskCard` — pakai composite kalau ada, else overlay sticker di atas envelope art mentah |
| BE fields (`envelope_sticker_id`, `envelope_composite_image_url`) | 🔴 | FE sudah kirim/terima field ini di semua DTO terkait, tapi backend (`GET /stickers`, penyimpanan field di `POST /letters`) belum dibangun — lihat `docs/be/envelope_customization_api.md` |

---

## Context

Rangkuman keputusan produk (riwayat lengkap ada di percakapan/commit sebelumnya, bukan diulang di sini):

- Envelope sekarang cuma bisa pilih desain envelope + stamp dari katalog. `onTextClick`/`onStickerClick` di `EnvelopeEditToolbar` masih stub. Fitur ini bikin **sticker** beneran jalan — `envelope_caption` (teks bebas) **dicoret dari scope**, tidak dikerjakan.
- Sticker envelope **cuma 1 slot, posisi preset** (bukan drag/multi) — keputusan produk buat kecepatan rilis.
- Cover amplop di PenPals feed mau jadi `AsyncImage` dari backend (`envelope_composite_image_url`), bukan direkonstruksi dari layer lokal tiap render — motivasi utama kenapa fitur ini butuh compositing, bukan cuma data terstruktur murni.
- Risiko `GraphicsLayer.record()` capture gagal/blur di device density rendah (`docs/be/device_capability_tiering.md`) **berlaku juga** di sini — reuse `MinCompositingDensity` yang sama. Tapi beda dari letter body: kalau capture di-skip, fallback rekonstruksi dari `envelope`/`stamp`/`envelope_sticker_id` (id-id yang tetap dikirim apa adanya) menghasilkan tampilan **identik**, bukan rusak. Makanya sticker envelope **tidak perlu di-dim/di-gate** kayak Annotate/Insert Image/Add Sticker letter body — semua device tetap bisa pasang sticker penuh, yang di-skip cuma optimisasi "kirim 1 gambar pre-baked"-nya doang.

---

## 1. Reuse checklist (jangan bikin ulang)

- `CatalogItemDto`/`CatalogRepository` (`feature/catalog/data/`) — pola cache-in-memory + fallback yang sudah ada buat `getEnvelopes()`/`getStamps()`, di-mirror buat `getStickers()` (§2).
- `StampPickerOverlay.kt` — struktur panel (shared-bounds container-transform dari corner box kecil ke panel penuh, horizontal scroll row, `StampCard` select-bounce animation) jadi template `StickerPickerOverlay.kt` (§4). **Jangan** re-derive pattern-nya dari nol.
- `StampImage.kt` — komponen ini sebenarnya generik atas `CatalogItemDto` (namanya kebetulan "Stamp" tapi nggak ada logic stamp-specific selain nama fallback design-nya). Reuse langsung buat render sticker art juga, jangan bikin `StickerImage.kt` duplikat kecuali butuh fallback vector khusus sticker (kemungkinan nggak butuh — sticker nggak punya bundled fallback asset seperti `StampDesign`, lihat §2).
- `LetterCompositor.kt`'s `rememberGraphicsLayer()`/`Modifier.captureIntoGraphicsLayer()`/`GraphicsLayer.toCompositeBytes()` — semuanya generik, sudah nggak spesifik letter body. **Tidak perlu bikin `EnvelopeCompositor.kt` terpisah** — reuse tiga fungsi ini apa adanya buat capture `EnvelopeCard`, cuma dipindah ke lokasi shared dulu (§7).
- `StorageRepository.presign()`/`uploadToPresignedUrl()` — jalur upload yang sama persis dipakai `ComposeViewModel.compositeAndSaveDraftForSend()`, reuse apa adanya buat upload envelope composite juga (§9).
- `ComingSoonBottomSheet` — tetap dipakai sebagai fallback kalau sticker catalog kosong (§2), bukan dibuang.

---

## 2. Prasyarat: sticker catalog (`GET /stickers`)

Belum ada file sticker data sama sekali (`feature/catalog/`, cek — tidak ada `feature/stickers/`). Backend-nya sendiri juga belum dibangun (`docs/be/stickers_api.md` §Roadmap: "seluruh isi dokumen ini belum dikerjakan").

- `CatalogApi.kt` — tambah:
  ```kotlin
  @GET("api/v1/stickers")
  suspend fun getStickers(): ApiResponse<List<CatalogItemDto>>
  ```
  Reuse `CatalogItemDto` apa adanya (`id`/`name`/`image_url`/`is_premium`) — abaikan field `pack` yang ada di spec BE (`stickers_api.md`), nggak perlu buat picker single-slot flat-grid ini. Kalau nanti sticker letter body (`docs/todo.md` #1) butuh grouping per-pack, baru bikin DTO terpisah saat itu.
- `CatalogRepository.kt` — tambah `getStickers()`, pola identik `getStamps()` (cache in-memory, `AuthResult` wrapper, `crashlytics.recordException`). **Tidak ada bundled fallback** (`localStickerCatalog(context)`) seperti envelope/stamp yang punya 5-6 asset di APK — sticker murni CMS-driven dari hari pertama.
- **Degradasi graceful kalau backend belum live / catalog kosong**: `EnvelopeEditToolbar.onStickerClick` tetap fallback ke `ComingSoonBottomSheet` selama `stickerCatalog.isEmpty()` — baru switch ke `StickerPickerOverlay` beneran begitu catalog punya isi. Ini bukan flag terpisah, cukup `if (stickerCatalog.isEmpty()) onComingSoonRequest(STICKER) else isStickerPickerOpen = true` di ViewModel. Artinya FE bisa rilis duluan tanpa nunggu BE — otomatis "nyala" begitu `GET /stickers` mulai balikin data.

---

## 3. Data model changes

### 3.1 `SelectRecipientUiState.kt`
```kotlin
val selectedSticker: CatalogItemDto? = null,
val isStickerPickerOpen: Boolean = false,
```
(Pola identik `selectedStamp`/`isStampPickerOpen`, bukan field baru yang beda bentuk.)

### 3.2 `SendLetterRequest` / `LetterDetailDto` (`feature/letters/data/LettersDto.kt`)
```kotlin
@Json(name = "envelope_sticker_id") val envelopeStickerId: String? = null,
@Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String? = null,
```
Detail kontrak lengkap: `docs/be/envelope_customization_api.md`.

### 3.3 `FeedItemDto` (`feature/penpals/data/PenpalsDto.kt`)
```kotlin
@Json(name = "stamp") val stamp: String? = null, // belum ada sama sekali sekarang
@Json(name = "envelope_sticker_id") val envelopeStickerId: String? = null,
@Json(name = "envelope_composite_image_url") val envelopeCompositeImageUrl: String? = null,
```

---

## 4. `StickerPickerOverlay.kt` (baru) — mirror `StampPickerOverlay.kt`

Sama struktur persis dengan `StampPickerOverlay`/`StampCard` (§1), beda 3 hal:

1. **Shared-bounds key baru**, `StickerSheetSharedKey`, terhubung ke slot sticker baru di `EnvelopeCard` (§5) — bukan reuse `StampSheetSharedKey`. `SelectRecipientScreen.kt` perlu `AnimatedVisibilityScope` kedua (`stickerAnimatedVisibilityScope`) di samping `stampAnimatedVisibilityScope` yang sudah ada, pola `remember { mutableStateOf<AnimatedVisibilityScope?>(null) }` yang sama.
2. **Selectable jadi deselectable** — karena cuma 1 slot dan bukan wajib (beda dari stamp yang gating "Ready to Send"), tap sticker yang lagi `isSelected` = pilih ulang deselect (`onStickerSelect(null)`), bukan no-op. Tambah "X" kecil di `StickerCard` yang lagi selected, atau baris "Hapus Sticker" terpisah di atas grid — pilih salah satu saat implementasi, keduanya valid.
3. Grid/row-nya render dari `stickerCatalog` (state baru di ViewModel, §2), bukan `stampCatalog`.

String resources baru: `sticker_picker_title`, `sticker_picker_subtitle`, `sticker_picker_selected_desc`, (opsional) `sticker_picker_remove_desc` — pola sama persis `stamp_picker_*`.

---

## 5. `EnvelopeCard.kt` — slot sticker

Posisi preset baru — **bukan** di corner stamp yang sudah ada (`EnvelopeHeaderRow`, kanan atas), biar nggak numpuk. Rekomendasi: pojok **kanan bawah** badan amplop (di luar `EnvelopeHeaderRow`/`RecipientAddressLabel` yang sudah pakai top+center), ukuran kecil ~40-48dp, `Alignment.BottomEnd` di dalam `Box` utama `EnvelopeCard`.

```kotlin
Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
    uiState.selectedSticker?.let { sticker ->
        EnvelopeStickerBadge(
            item = sticker,
            sharedTransitionScope = sharedTransitionScope,
            stickerAnimatedVisibilityScope = stickerAnimatedVisibilityScope,
        )
    }
}
```
`EnvelopeStickerBadge` = composable baru, mirror `StampArrival` persis (drop-in bounce entrance lewat `LetterlySpring.Bouncy`, keyed ke `item.id`) — bedanya cuma posisi & size, animasinya reuse.

---

## 6. `EnvelopeEditToolbar.kt`

```kotlin
onStickerClick: () -> Unit, // sudah ada parameternya, tinggal caller-nya diganti
```
Nggak ada perubahan di file ini — cukup `SelectRecipientScreen.kt`'s `onStickerClick = { viewModel.onStickerToggleRequest() }` (ganti dari `onComingSoonRequest(STICKER)`, lihat §2 buat fallback logic).

---

## 7. Shared compositing constant — pindahkan dari `LetterCompositor.kt`

`MinCompositingDensity`, `rememberGraphicsLayer()`, `Modifier.captureIntoGraphicsLayer()`, `GraphicsLayer.toCompositeBytes()` sudah generik (tidak menyentuh apa pun spesifik letter body) tapi sekarang tinggal di file bernama letter-spesifik. Pindahkan ke lokasi netral, mis. `ui/core/compositing/CompositingCapability.kt` (atau `core/util/` kalau mau konsisten sama `LetterlySpring`/`rememberPressScale` yang sudah di situ). `LetterCompositor.kt` di ComposeScreen tinggal import dari lokasi baru — tidak ada perubahan behavior, murni pemindahan file supaya nama filenya nggak menyesatkan begitu envelope compositing juga pakai fungsi yang sama.

---

## 8. Capture envelope — tidak butuh compositor baru

Karena §7 sudah generik, `SelectRecipientScreen.kt` tinggal:
```kotlin
val envelopeGraphicsLayer = rememberGraphicsLayer()
val isCompositingSupported = LocalDensity.current.density >= MinCompositingDensity
// ...
EnvelopeCard(
    ...,
    modifier = Modifier
        .padding(top = 12.dp)
        .let { if (isCompositingSupported) it.captureIntoGraphicsLayer(envelopeGraphicsLayer) else it },
)
```
Sama persis pola `ComposeScreen.kt` capture `LetterCanvas`. Kalau device di bawah threshold, modifier capture nggak dipasang sama sekali (bukan dipasang-lalu-diabaikan) — konsisten sama "nggak ada cost tambahan buat device rendah" yang sudah jadi prinsip sejak `device_capability_tiering.md`.

---

## 9. `SelectRecipientViewModel.kt` — wiring Send

`StorageRepository` perlu ditambah ke constructor (belum ada di sana sekarang, cuma ada di `ComposeViewModel`).

`onSendClick()` (§ kedua cabang `KNOWN_USER`/`PENPAL_REGION`) butuh ubah signature jadi terima composite bytes opsional dari caller, karena capture (`GraphicsLayer.toCompositeBytes()`) itu suspend function yang harus dipanggil dari coroutine di sisi Composable (nggak bisa dari ViewModel langsung — `GraphicsLayer` terikat ke composition):

```kotlin
// SelectRecipientScreen.kt
onSendClick = {
    coroutineScope.launch {
        val envelopeBytes = if (isCompositingSupported) {
            envelopeGraphicsLayer.toCompositeBytes()
        } else null
        viewModel.onSendClick(envelopeBytes)
    }
}
```

```kotlin
// SelectRecipientViewModel.kt
fun onSendClick(envelopeCompositeBytes: ByteArray?) {
    // ...
    viewModelScope.launch {
        val envelopeCompositeImageUrl = envelopeCompositeBytes?.let { bytes ->
            val fileName = "envelope_${UUID.randomUUID()}.webp"
            val presign = when (val r = storageRepository.presign(fileName, "image/webp")) {
                is AuthResult.Success -> r.data
                is AuthResult.Error -> { _uiState.update { it.copy(sendError = r.message) }; return@launch }
            }
            val upload = storageRepository.uploadToPresignedUrl(presign.uploadUrl, bytes, "image/webp")
            if (upload is AuthResult.Error) { _uiState.update { it.copy(sendError = upload.message) }; return@launch }
            presign.publicUrl
        }
        val request = SendLetterRequest(
            ...,
            envelopeStickerId = state.selectedSticker?.id,
            envelopeCompositeImageUrl = envelopeCompositeImageUrl,
        )
        // ... sisanya sama seperti sekarang
    }
}
```
Duplikat logic presign/upload ini persis sama shape-nya dengan `ComposeViewModel.compositeAndSaveDraftForSend` — kalau mau, bisa diekstrak jadi satu suspend helper `StorageRepository.uploadComposite(bytes): AuthResult<String>` biar nggak ada 2 salinan try/presign/upload yang identik. Opsional, bukan blocking buat v1.

---

## 10. Render sisi penerima (Inbox + PenPals feed)

Dua tempat render envelope cover saat ini, keduanya cuma `AsyncImage(model = envelopeImageUrl, placeholder = enum.drawableRes, error = enum.drawableRes)` — **tidak ada** stamp/apa pun di-overlay (gap yang sudah ada dari sebelum fitur ini, di luar scope buat diperbaiki sekarang):

- `DeskCard.kt` (PenPals feed) — line ~183.
- `OpenLetterScreen.kt`'s `EnvelopePaperReveal` (Inbox, sebelum surat dibuka) — line ~403.

Ubah keduanya jadi:
```kotlin
if (item.envelopeCompositeImageUrl != null) {
    AsyncImage(model = item.envelopeCompositeImageUrl, ...) // sama seperti sekarang, cuma ganti source
} else {
    Box {
        AsyncImage(model = item.envelopeImageUrl, ...) // persis seperti sekarang
        item.envelopeStickerId?.let { id ->
            stickerCatalog.find { it.id == id }?.let { sticker ->
                StampImage(item = sticker, modifier = Modifier.align(Alignment.BottomEnd).padding(...).size(40.dp))
            }
        }
    }
}
```
**Sengaja cuma overlay sticker, bukan stamp** — supaya fallback tetap "identik" dengan composite (janji di `envelope_customization_api.md`), tapi nggak memperbaiki gap stamp-nggak-kelihatan yang sudah ada sebelum fitur ini (itu perubahan terpisah, di luar scope).

Butuh sticker catalog tersedia di kedua screen ini juga (`PenPalsViewModel`/`InboxViewModel` inject `CatalogRepository`, panggil `getStickers()` sama seperti `SelectRecipientViewModel` — reuse cache in-memory yang sama, jadi ini nggak nge-refetch kalau user udah pernah buka Select Recipient di sesi yang sama).

### 10.1 Mapping
- `LetterMapping.kt` — `mapToInboxItem` tambah param `envelopeStickerId: String?`, `envelopeCompositeImageUrl: String?`; `InboxItem.kt` tambah dua field yang sama.
- `PenpalLetter.kt`'s `toPenpalLetter()` — tambah dua field yang sama dari `FeedItemDto`.

---

## 11. File checklist

**Baru:**
- `ui/screens/selectrecipient/widgets/StickerPickerOverlay.kt`

**Diubah:**
- `feature/catalog/data/CatalogApi.kt`, `CatalogRepository.kt` — `getStickers()`
- `ui/screens/selectrecipient/state/SelectRecipientUiState.kt` — `selectedSticker`, `isStickerPickerOpen`
- `ui/screens/selectrecipient/widgets/EnvelopeCard.kt` — slot sticker + `EnvelopeStickerBadge`
- `ui/screens/selectrecipient/screen/SelectRecipientScreen.kt` — wiring picker + capture + send
- `ui/screens/selectrecipient/viewmodel/SelectRecipientViewModel.kt` — sticker catalog fetch, `StorageRepository` injected, `onSendClick` terima composite bytes
- `ui/screens/compose/widgets/LetterCompositor.kt` → pecah, constant/helper generik pindah ke lokasi shared baru (§7)
- `feature/letters/data/LettersDto.kt` — `SendLetterRequest`/`LetterDetailDto`
- `feature/penpals/data/PenpalsDto.kt` — `FeedItemDto`
- `ui/screens/inbox/{InboxItem,LetterMapping,OpenLetterScreen}.kt`
- `ui/screens/penpals/{state/PenpalLetter,widgets/DeskCard}.kt`
- `res/values/strings.xml` + `res/values-id/strings.xml` — `sticker_picker_*`

---

## 12. Di luar scope

- `envelope_caption` — dicoret total, tidak ada di spec ini.
- Overlay stamp di sisi penerima — gap lama, nggak diperbaiki di sini (§10).
- Multi-sticker / posisi bebas / drag — sengaja dihindari, lihat `docs/be/envelope_customization_api.md`.
- Ekstraksi `uploadComposite()` helper bersama antara `ComposeViewModel`/`SelectRecipientViewModel` — opsional cleanup, bukan blocking (§9).
