# Send Letter + Get Envelope — Backend Handoff

Entry point buat kerjaan backend fitur **Send** (surat asli — teks+styling+gambar+annotate — sampai ke recipient persis seperti yang dilihat pengirim) dan **Get Envelope** (recipient bisa buka surat itu dan lihat gambar aslinya, bukan placeholder). Client Android sudah selesai dikerjakan mengikuti kontrak di bawah — dokumen ini yang backend perlu ikutin supaya nyambung.

Detail rationale lengkap tiap bagian ada di file terpisah (link di tiap seksi) — dokumen ini isinya ringkasan + urutan kerja + status.

---

## Status ringkas

| # | Perubahan | Status client | Status backend butuh |
|---|---|---|---|
| 1 | `POST /letters` — `composite_image_url` selalu wajib (bukan hybrid lagi) | ✅ Selesai | 🔴 Perlu ubah validasi (lihat §1) |
| 2 | `GET /api/v1/envelopes` — catalog amplop CMS-driven | ✅ Selesai (fallback lokal sementara) | 🔴 Endpoint baru (lihat §2) |
| 3 | `GET /api/v1/stamps` — catalog stamp CMS-driven | ✅ Selesai | ✅ Sudah live, tidak berubah |
| 4 | `POST /api/v1/penpals/match` — resolusi region → recipient | ✅ Selesai (fallback ke feed hack sementara) | 🔴 Endpoint baru (lihat §4) |
| 5 | `POST /storage/presign` — upload gambar composite | ✅ Selesai, sudah dipakai | ✅ Sudah live, tidak berubah |

Client-side "selesai" di atas artinya: kode sudah ditulis dan compile sukses (`./gradlew :app:compileDevDebugKotlin`/`compileProdDebugKotlin`). Belum di-test end-to-end lewat app beneran (belum di-run/build APK) — begitu backend bagian 🔴 di atas live, perlu satu putaran testing manual bareng.

---

## 1. `POST /api/v1/letters` — `composite_image_url` jadi selalu wajib

**Perubahan validasi paling penting.** Detail penuh + alasan produk di `docs/be/letters_api.md` (seksi "Update: selalu `composite_image_url`, bukan hybrid lagi"). Ringkasnya:

- Field `composite_image_url` berubah dari *conditionally required* (dulu: cuma wajib kalau surat punya annotate/gambar/rich-text) jadi **selalu wajib, tidak boleh `null`** — untuk request dari `POST /letters` biasa (bukan dari `POST /admin/seed-users/{id}/letters`, itu tetap boleh `null`).
- Client sudah selalu flatten surat ke satu PNG lewat `GraphicsLayer` capture (`ui/screens/compose/LetterCompositor.kt`) dan upload lewat `POST /storage/presign` yang **sudah ada** (tidak ada endpoint upload baru) sebelum manggil `POST /letters` — lihat `ComposeViewModel.compositeAndSaveDraftForSend()`.
- `body_text` **tetap dikirim** (buat search/notifikasi-preview/moderasi), tapi bukan lagi sumber render — jangan drop field ini dari request meskipun sekarang bukan render-critical.
- Batas atas `body_text` 2000 karakter yang lama **dicabut** — client sekarang biarkan user ngetik sepanjang apapun (mereka bisa mengecilkan font sendiri). Minimal 20 karakter tetap berlaku.
- `paper_template`/`paper_color`/`font_id` tetap dikirim tapi sekarang murni metadata, backend tidak perlu pakai buat re-render apapun.

Action item backend: ubah validasi `POST /letters` supaya `composite_image_url == null` → `400` (kecuali dari jalur admin seed), dan boleh drop pengecekan panjang maksimal `body_text` kalau ada.

## 2. `GET /api/v1/envelopes` — endpoint baru

Spesifikasi lengkap: `docs/be/envelopes_api.md`. Shape-nya **identik** dengan `GET /api/v1/stamps` yang sudah live (`docs/be/api_contract.md`):
```json
[{ "id": "ENVELOPE_2", "name": "Envelope 2", "image_url": "https://...", "is_premium": false }]
```
Wajib seed 5 baris dengan id `ENVELOPE_1`..`ENVELOPE_5` (cocok sama enum lama di client) supaya backward-compatible. Id baru bebas string apa aja buat desain yang ditambah lewat CMS nanti.

Client: `feature/catalog/data/CatalogApi.kt` sudah manggil endpoint ini; kalau belum live, `SelectRecipientViewModel` diam-diam fallback ke 5 desain bundled di APK — jadi endpoint ini **tidak blocking**, tapi begitu live, admin bisa nambah desain amplop tanpa client perlu update.

## 3. `GET /api/v1/stamps` — tidak berubah, cuma dikonfirmasi kepake

Sudah live, sudah didokumentasikan di `docs/be/api_contract.md`. Tidak ada perubahan kontrak — cuma catatan bahwa client **sekarang** benar-benar manggilnya (sebelumnya cuma didokumentasikan, belum pernah dipanggil dari Android). `StampPickerOverlay.kt` sekarang render dari sini, fallback ke 6 warna hardcoded kalau gagal, pola sama seperti envelope.

## 4. `POST /api/v1/penpals/match` — endpoint baru

Spesifikasi lengkap: `docs/be/penpals_api.md` (seksi "`POST /api/v1/penpals/match` (baru, belum dibangun)"). Ringkasnya:
```json
// request
{ "region": "Asia" }
// response 200
{ "recipient_id": "firebase-uid-or-seed-id", "recipient_name": "kirana_senja" }
// response 404
{ "error": "no penpals available in this region" }
```
MVP: random user eligible di region itu, exclude diri sendiri. Tidak blocking — client fallback ke `GET /penpals/feed?region=X&limit=1` kalau endpoint ini error/belum ada.

## 5. `POST /api/v1/storage/presign` — tidak berubah

Sudah live (`docs/be/api_contract.md`), dipakai ulang apa adanya — tidak ada endpoint upload baru buat compositing surat. Client baru sekarang benar-benar manggilnya (`feature/storage/data/StorageApi.kt`/`StorageRepository.kt`): `POST /storage/presign` → dapat `upload_url` → `PUT` byte PNG langsung ke situ (bukan lewat backend) → `public_url` dikirim sebagai `composite_image_url`.

---

## Urutan kerja disarankan

1. **§1 (validasi `composite_image_url`)** — paling penting, tanpa ini surat kiriman real user selalu ke-reject. Tidak butuh tabel/skema baru, cuma ubah rule validasi.
2. **§2 (`GET /envelopes`)** dan **§4 (`POST /penpals/match`)** — independen satu sama lain, bisa paralel. Keduanya tidak blocking buat app tetap jalan (client sudah punya fallback), tapi tanpa ini fitur "CMS bisa nambah amplop" dan "match penpal beneran" belum hidup.
3. **§3 (`GET /stamps`)** dan **§5 (presign)** — tidak ada kerjaan backend, cuma konfirmasi kedua endpoint existing ini tetap seperti yang didokumentasikan.

## File client terkait (buat referensi timbal-balik)

- `feature/storage/data/{StorageApi,StorageDto,StorageRepository}.kt` — presign + upload
- `feature/catalog/data/{CatalogApi,CatalogDto,CatalogRepository}.kt` — envelope/stamp catalog + fallback
- `feature/letters/data/LettersDto.kt` — `SendLetterRequest` (shape tidak berubah, cuma `composite_image_url` sekarang selalu terisi)
- `feature/penpals/data/{PenpalsApi,PenpalsDto,PenpalsRepository}.kt` — `matchPenpal()`
- `ui/screens/compose/{LetterCompositor,ComposeViewModel,ComposeScreen}.kt` — compositing pipeline
- `ui/screens/inbox/{LetterMapping,InboxItem,OpenLetterScreen}.kt` — render `composite_image_url` di sisi recipient
