# Sticker Catalog API

Backend untuk fitur Sticker Picker di Compose (`ui/screens/compose/`). `ComposeUiState.stickers: List<StickerInstance>` sudah ada shape-nya di client (`id`, `stickerId`, `offsetX`, `offsetY`, `scale`, `rotation`) tapi **stub, belum dirender, belum ada picker UI** — ini masih di `docs/todo.md` #1. Dokumen ini spec backend yang dibutuhkan buat mulai bangun fitur itu di client.

**Beda dari Stamps (`docs/be/stamps_api.md`): sticker TIDAK bergantung RevenueCat.** Gate premium cukup boolean `is_premium` yang sudah ada & live di `UserDto` (`GET /auth/me`), sama pola yang sudah dipakai `POST /suggest`. Nggak ada ownership/pembelian per-item, jadi milestone ini bisa dikerjakan independen dari M6 (Stamps & Store) yang sengaja ditunda.

Semua endpoint di bawah `/api/v1`, butuh `Authorization: Bearer <firebase_id_token>` kecuali disebutkan admin-only.

---

## `GET /api/v1/stickers`

Katalog sticker, mirror shape `GET /stamps` yang sudah live (`docs/be/api_contract.md`):

```json
[
  { "id": "sticker_heart", "name": "Heart", "image_url": "https://...", "is_premium": false, "pack": "basic" },
  { "id": "sticker_stamp_pilo", "name": "Pilo Stamp", "image_url": "https://...", "is_premium": true, "pack": "premium_pilo" }
]
```

| Field | Type | Catatan |
|---|---|---|
| `id` | string | Stabil, dipakai sebagai referensi di `sticker_data` saat kirim surat — jangan pernah diubah/reuse setelah publish |
| `name` | string | Label internal/aksesibilitas, tidak selalu ditampilkan di grid picker |
| `image_url` | string | PNG transparan, di-host di R2 lewat jalur `POST /storage/presign` yang sudah ada (lihat §Admin CRUD di bawah) |
| `is_premium` | bool | Gate akses — lihat §Validasi di bawah |
| `pack` | string | Grouping buat UI (mis. `basic`, `seasonal`, `premium_pilo`). Spec asli (`compose-screen-spec.md` §8) sebut dua sumber sticker (built-in Procreate + emoji besar) — field ini kasih ruang nambah drop baru tanpa ubah schema |

List penuh, tidak dipaginate (jumlah sticker realistis masih kecil di awal). Endpoint ini publik untuk semua user yang login (gratis maupun premium) — user free tetap bisa **lihat** sticker premium di grid (ditandai locked di UI), cuma nggak bisa **pakai**. Pola browse-then-upgrade ini standar buat paywall, dan konsisten sama `is_premium` yang juga sudah muncul apa adanya di `GET /stamps`.

---

## Validasi di `POST /api/v1/letters` — sudah live

Berlaku saat `sticker_data` tidak kosong (`app/module/letter/service/letter_service.go`):

1. Setiap ID di `sticker_data` harus ada di katalog sticker (`GET /stickers`) — kalau ada ID tidak dikenal → `400 { "error": "unknown sticker id: ..." }`.
2. Kalau ada ID yang `is_premium == true` dan user pengirim (`sender.is_premium != true`) → `403 { "error": "premium sticker requires subscription" }`. Pola identik `POST /suggest` yang sudah live (`docs/be/api_contract.md`).

## Klarifikasi semantik `sticker_data: List<String>`

Field ini **sudah ada** di kontrak `SendLetterRequest` (`docs/be/letters_api.md`) tapi belum pernah didokumentasikan isinya secara eksplisit — client juga belum pernah mengisinya. Tegaskan di sini:

- `sticker_data` = **daftar ID sticker yang dipakai** di surat (dari katalog `GET /stickers`), dipakai untuk validasi premium di atas + analytics/moderasi konten ke depan.
- **Bukan** data posisi/skala/rotasi per sticker. Sticker itu "elemen posisi bebas" persis kayak `ImageInstance` (gambar yang di-insert & di-drag) — per aturan yang sudah ada di `letters_api.md` §"Kapan `body_text` polos vs `composite_image_url`", posisi visual elemen bebas selalu di-flatten jadi satu PNG (`composite_image_url`), bukan dikirim sebagai koordinat terstruktur (supaya WYSIWYG konsisten lintas ukuran layar). Jadi: **surat yang punya sticker otomatis wajib isi `composite_image_url`** — ini bukan aturan baru, cuma penegasan bahwa sticker tunduk ke aturan yang sama dengan gambar/annotate yang sudah didokumentasikan.

---

## Admin CRUD — kelola katalog (opsional, buat staff)

Mirror pola `docs/be/seed_accounts_api.md` — pakai header `X-Admin-Key` (bukan Firebase Bearer), **tidak pernah dipanggil dari Android**:

- `GET /api/v1/admin/stickers` — list semua sticker (termasuk yang sudah di-archive), buat layar kelola katalog di webadmin
- `POST /api/v1/admin/stickers` — tambah sticker baru (`name`, `image_url`, `is_premium`, `pack`)
- `PATCH /api/v1/admin/stickers/{id}` — update field (mis. pindah pack, toggle premium)
- `DELETE /api/v1/admin/stickers/{id}` — soft-delete (`archived_at`, bukan hard delete — sticker lama yang sudah kepakai di surat terkirim harus tetap valid secara referential, sama alasan seed account di `seed_accounts_api.md`)

Asset PNG-nya sendiri **tidak butuh endpoint upload baru** — staff upload lewat jalur `POST /storage/presign` yang sudah ada (sama seperti foto profil), baru daftarkan `public_url` hasilnya ke tabel sticker lewat `POST /admin/stickers` di atas.

---

## Di luar scope dokumen ini

Supaya tidak ada kerja ganda/miskomunikasi — dua fitur lain yang sempat dibahas bareng sticker **tidak butuh backend sama sekali**:

- **Save to Photos** (export surat jadi PNG ke galeri) — murni `MediaStore` Android di sisi client, tidak ada network call.
- **Draft** (simpan surat belum terkirim) — per keputusan produk, cukup Room lokal di device (sesuai spec awal `compose-screen-spec.md` §11, debounce 3 detik), bukan disinkronkan ke backend. Bisa direvisit kalau nanti ada kebutuhan draft lintas-device.

**Put Image** (sisip foto ke surat) juga tidak butuh endpoint baru — sudah jalan penuh sebagai layer lokal di client (`ImageInstance`), dan upload composite PNG-nya reuse `POST /storage/presign` → R2 yang sudah live (dipakai juga untuk foto profil). Yang belum selesai di situ murni kerjaan client (compositing/flatten layer jadi satu Bitmap, lihat `docs/todo.md` #2), bukan gap backend.

## Status: sudah live

Seluruh isi dokumen ini **sudah diimplementasi** di backend (`app/module/sticker/` untuk katalog + admin CRUD, validasi di `app/module/letter/service/letter_service.go`). Bukan lagi roadmap.
