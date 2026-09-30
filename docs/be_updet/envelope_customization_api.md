# Envelope Customization (Sticker) — Backend Handoff

Backend buat variasi tampilan amplop (`SelectRecipientScreen`/`EnvelopeCard.kt`) — sekarang cuma bisa pilih desain envelope + stamp dari katalog, `onStickerClick` di `EnvelopeEditToolbar` masih stub `ComingSoonBottomSheet`. Dokumen ini spec backend buat bikin tombol itu beneran jalan, **plus** bikin cover amplop di PenPals feed jadi gambar asli dari backend (bukan reconstruct dari asset lokal + id).

> ⚠️ **Revisi dari draft pertama dokumen ini:** draft awal sengaja menghindari compositing sama sekali (sticker cukup dikirim sebagai id, direkonstruksi ulang client-side di kedua sisi). Itu **masih benar** buat alasan WYSIWYG (envelope emang nggak punya elemen posisi bebas seperti letter body). Tapi ada kebutuhan produk terpisah yang mengubah keputusan: PenPals feed mau nampilin cover amplop sebagai `AsyncImage` dari backend (persis pola letter body), bukan rebuild dari layer lokal tiap kali — jadi kita tetap composite, tapi dengan fallback yang jauh lebih ringan daripada letter body (lihat di bawah).
>
> **Ralat kedua: `envelope_caption` (teks bebas di amplop) dicoret dari scope.** Cuma sticker yang dikerjain.

## Kenapa compositing dipakai lagi, dan kenapa ini AMAN (beda dari letter body)

`docs/be/device_capability_tiering.md` sudah buktikan: `GraphicsLayer.record()` capture bisa gagal (blank/blur) di device dengan screen density rendah. Risiko teknis itu **sama** kalau kita capture `EnvelopeCard` jadi WebP — nggak spesifik ke letter body, ini masalah di level API Compose-nya, bukan konten yang di-capture.

Tapi konsekuensinya **jauh lebih ringan** buat envelope, karena beda sifat kontennya:

| | Letter body | Envelope |
|---|---|---|
| Konten | Elemen posisi bebas (annotate stroke, gambar di-drag/resize, rich text per-rentang) | Data terstruktur (`envelope` id, `stamp` id, `envelope_sticker_id` id di slot preset — lihat draft pertama dokumen ini) |
| Bisa direkonstruksi ulang tanpa image? | **Tidak** — koordinat absolut ke canvas pengirim, reflow beda di layar lain | **Ya, selalu** — semua sumber render-nya cuma id, deterministic di device manapun |
| Kalau capture gagal di device density rendah | Surat jadi nggak kebaca sama sekali kalau dipaksa kirim gambar itu → makanya di-skip total, fallback ke `body_text` polos | Fallback-nya **sama bagusnya** dengan hasil capture — reconstruct dari `envelope`/`stamp`/`envelope_sticker_id` yang tetap dikirim apa adanya |

Konsekuensi desain: **fitur sticker envelope TIDAK perlu di-gate/di-dim** di device density rendah (beda dari Annotate/Insert Image/Add Sticker letter body yang didim + `DeviceCapabilityBottomSheet`). User di device manapun tetap bisa pasang sticker penuh. Yang di-skip cuma optimisasi "kirim satu gambar pre-baked" — kalau device-nya di bawah threshold, `envelope_composite_image_url` dikirim `null`, dan penampil (feed/inbox) otomatis reconstruct dari field id yang tetap lengkap terkirim. Hasil akhir yang dilihat orang lain **identik** baik dari composite maupun dari reconstruction — cuma beda "sumber render", bukan beda tampilan.

## Perubahan kontrak `POST /api/v1/letters`

```json
{
  "body_text": "...",
  "envelope": "ENVELOPE_2",
  "stamp": "OCEAN",
  "envelope_sticker_id": "sticker_heart",
  "envelope_composite_image_url": "https://.../key-from-presign-flow",
  ...
}
```

| Field | Type | Catatan |
|---|---|---|
| `envelope_sticker_id` | string? | Opsional. Id dari `GET /stickers` (katalog sama dengan letter body, `docs/be/stickers_api.md`) — validasi sama seperti di sana (unknown id → `400`, premium tanpa akses → `403`). Selalu dikirim apa adanya, terlepas device-nya kompositing atau nggak — ini satu-satunya sumber render fallback. |
| `envelope_composite_image_url` | string? | **Baru.** Opsional/nullable — flatten `EnvelopeCard` (background + stamp + sticker) jadi satu WebP, upload lewat `POST /storage/presign` yang sudah ada (jalur sama persis dengan letter body `composite_image_url`, endpoint upload tidak baru). `null` kalau device pengirim di bawah `MinCompositingDensity` (`LetterCompositor.kt`) — backend terima `null` apa adanya, tidak perlu tau alasannya, sama pola seperti letter body. |

Backend **tidak perlu** requirement silang antara dua field ini (mis. tidak boleh maksa `envelope_composite_image_url` wajib kalau `envelope_sticker_id` terisi) — beda dari aturan letter body (`sticker_data` → wajib `composite_image_url`). Di sini semua kombinasi valid: composite ada atau `null`, dengan atau tanpa sticker, bebas.

### Validasi

1. `envelope_sticker_id` — reuse pola validasi `docs/be/stickers_api.md` §"Validasi tambahan" (unknown id → `400`, premium check → `403`), diterapkan juga ke field ini (bukan cuma `sticker_data` letter body).
2. `envelope_composite_image_url` — tidak ada validasi konten (sama seperti `composite_image_url` letter body, backend nggak perlu render-ulang buat verifikasi).

## Render di sisi penerima (Inbox + PenPals feed)

Field baru yang sama (`envelope_sticker_id`, `envelope_composite_image_url`) perlu dibalikin di:

- `GET /letters/{id}` (`LetterDetailDto`) — buat Inbox.
- `GET /api/v1/penpals/feed` (`FeedItemDto`) — buat cover amplop di feed, ini motivasi utama field `envelope_composite_image_url`. **Update:** sudah live (`stamp`, `envelope_sticker_id`, `envelope_composite_image_url`, `composite_image_url` semua sudah ada di response, lihat `docs/be/penpals_api.md`) — catatan "belum punya stamp" di atas sudah tidak berlaku.

Aturan render di client (kedua tempat, pola sama):
- `envelope_composite_image_url != null` → tampilkan langsung sebagai `AsyncImage`, tidak rebuild layer apa pun.
- `envelope_composite_image_url == null` → reconstruct dari `envelope` + `stamp` + `envelope_sticker_id` (lookup ke katalog `GET /envelopes`/`GET /stickers` yang sudah di-cache client), persis seperti `EnvelopeCard.kt` yang sudah ada sekarang — bukan tampilan kosong/placeholder.

`GET /letters/inbox` (list endpoint) — opsional nambahin field ini juga kalau list view nanti mau nampilin cover amplop (saat ini list view nggak nampilin envelope card sama sekali, cuma dipakai kalau ada kebutuhan baru).

## File client terkait (belum dikerjakan, ini scope-nya)

- `ui/screens/selectrecipient/widgets/EnvelopeCard.kt` — tambah slot render sticker; jadi target capture (`captureIntoGraphicsLayer`) buat compositor baru
- `ui/screens/compose/widgets/LetterCompositor.kt` — `MinCompositingDensity` sebaiknya dipindah ke file/objek shared (bukan spesifik letter body lagi) karena sekarang dipakai 2 fitur — sarankan ekstrak ke `ui/screens/compose/widgets/CompositingCapability.kt` atau sejenis
- **Baru**: `EnvelopeCompositor.kt` — mirror `LetterCompositor.kt`, capture `EnvelopeCard` jadi WebP lewat `toCompositeBytes()`-style helper
- `ui/screens/selectrecipient/screen/SelectRecipientScreen.kt` — `onStickerClick` (ganti dari `ComingSoonBottomSheet` jadi picker beneran, **tidak** di-gate density seperti letter body — lihat rationale di atas)
- `ui/screens/selectrecipient/state/SelectRecipientUiState.kt` — tambah `envelopeStickerId: String?`
- `ui/screens/selectrecipient/viewmodel/SelectRecipientViewModel.kt` — `onSendClick()`: capture (kalau device support) → presign/upload → forward dua field baru ke `SendLetterRequest`
- `feature/letters/data/LettersDto.kt` — `SendLetterRequest`/`LetterDetailDto` tambah dua field
- `feature/penpals/data/PenpalsDto.kt` — `FeedItemDto` tambah dua field (+ `stamp` kalau mau lengkap) buat render cover asli di feed
- `docs/be/stickers_api.md` — katalog sticker yang di-reuse (prasyarat kalau belum dikerjain)

## Di luar scope dokumen ini

- `envelope_caption` (teks bebas di amplop) — dicoret, tidak jadi dikerjakan.
- Multi-sticker atau posisi bebas (drag/rotate) — tetap dihindari sama seperti draft pertama, alasan sama (balik butuh flatten-wajib + kena masalah density beneran, karena kalau posisinya bebas, reconstruction-nya nggak lagi identik dengan capture).
- `GET /letters/inbox` (list) ikut nampilin cover — opsional, nyusul kalau dibutuhkan.
