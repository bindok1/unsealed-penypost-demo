# Welcome Letter Config — Webadmin Integration Guide

Panduan buat bikin satu halaman settings di webadmin (proyek terpisah, lihat `docs/be/admin_dashboard_web.md`) yang mengontrol fitur **Welcome Letter** (`docs/be/welcome_letter_api.md`). Backend-nya **sudah selesai dan live** — dokumen ini murni kontrak buat integrasi FE, gak ada endpoint baru yang perlu diminta ke backend.

**Kenapa halaman ini penting:** fitur welcome letter secara default **off**. Selama config di bawah belum diisi (`sender_id` kosong), gak ada satupun user baru yang dapet surat welcome. Halaman ini adalah satu-satunya cara nyalain fitur ini di production tanpa redeploy.

---

## Auth

Sama seperti semua endpoint `/api/v1/admin/*` lain (`docs/be/admin_dashboard_web.md` §Auth):
- Header `X-Admin-Key: <ADMIN_API_KEY>`, bukan Bearer token Firebase.
- Simpan key di `localStorage` (internal tool, gak perlu OAuth beneran).
- Response envelope standar: `{ "success": true, "data": {...} }` atau `{ "success": false, "error": "..." }`.

---

## Endpoint utama: baca & update config

### `GET /api/v1/admin/welcome-letter-config`

Baca config saat ini. Panggil ini pas halaman settings di-load, buat isi form.

Response `200`:
```json
{
  "success": true,
  "data": {
    "sender_id": "seed_abc123",
    "body_text": "Halo! Selamat datang di Unsealed...",
    "letter_image_url": "https://.../welcome-letter.webp",
    "envelope_image_url": null,
    "envelope_id": "ENVELOPE_2",
    "stamp_id": "STAMP_ROSE",
    "updated_at": "2026-08-19T10:00:00Z"
  }
}
```
Baris pertama kali (belum pernah di-setup): semua field `null` kecuali `body_text` (`""`) dan `updated_at`.

### `PATCH /api/v1/admin/welcome-letter-config`

**Partial update** — cuma kirim field yang mau diubah, field yang di-omit/`null` **gak berubah** (bukan di-reset ke null). Pola sama seperti `PATCH /admin/seed-users/{id}`.

Request body (semua optional per field):
```json
{
  "sender_id": "seed_abc123",
  "body_text": "Halo! Selamat datang di Unsealed, semoga kamu betah nulis surat di sini 🩵",
  "letter_image_url": "https://.../welcome-letter.webp",
  "envelope_image_url": null,
  "envelope_id": "ENVELOPE_2",
  "stamp_id": "STAMP_ROSE"
}
```
Response `200`: shape sama persis dengan `GET` di atas (config yang udah ke-update).

| Field | Type | Wajib diisi buat "nyalain" fitur? | Catatan |
|---|---|---|---|
| `sender_id` | string, nullable | **Ya** — ini switch utama | Id persona seed (`seed_<uuid>`), dari `POST /admin/seed-users`. **Kosong/null = fitur off**, register jalan normal tanpa kirim apa-apa. |
| `body_text` | string | Ya, min 20 karakter | Divalidasi backend **saat surat benar-benar dikirim** (`POST /letters` rule, min 20 char), **bukan** saat `PATCH` config ini — kalau isi kurang dari 20 char, config tersimpan tanpa error, tapi tiap user baru register bakal silently gagal (cuma ke-log, register tetap sukses, user gak dapet surat). Validasi ini **di sisi FE aja**, backend gak nolak. |
| `letter_image_url` | string, nullable | Ya (efektifnya wajib walau nullable di DB) | Public URL gambar surat, dari flow presign (§ di bawah). |
| `envelope_image_url` | string, nullable | Tidak, opsional | Kalau `null`, amplop pakai desain katalog biasa dari `envelope_id`. |
| `envelope_id` | string, nullable | Ya | Id valid dari `GET /envelopes`. |
| `stamp_id` | string, nullable | Ya | Id valid dari `GET /stamps`. |
| `updated_at` | string (ISO8601) | — | Read-only, buat tampilin "last edited" di UI. |

**⚠️ Gotcha penting — gak ada validasi referential di backend:** `sender_id`, `envelope_id`, `stamp_id` **tidak** di-FK-check ke tabel `users`/`envelopes`/`stamps` (`db/migrations/028_welcome_letter_config.sql` — kolomnya `TEXT` polos, gak ada constraint). Kalau salah ketik id yang gak eksis, `PATCH` tetap `200`, tapi tiap registrasi user baru bakal gagal kirim surat secara diam-diam (cuma ke-log di server, `POST /auth/register` tetap sukses dari sisi user). **FE wajib validasi id-id ini di client** — dropdown/select yang isinya dari `GET /admin/seed-users`, `GET /envelopes`, `GET /stamps` (bukan free-text input), biar gak mungkin salah ketik.

---

## Endpoint pendukung yang dibutuhkan buat isi form ini

Form settings-nya butuh 3 sumber data lain buat populate pilihan (dropdown), bukan free text:

### 1. Pilih persona pengirim — dari daftar seed users

- **List (buat dropdown):** `GET /api/v1/admin/seed-users?cursor=&limit=` → `{ items: SeedUserResponse[], next_cursor }`. Tampilin `nickname` + `photo_url` tiap opsi, value-nya `id`.
- **Bikin baru kalau belum ada persona yang cocok:** `POST /api/v1/admin/seed-users`
  ```json
  { "nickname": "Peny", "continent": "Asia", "bio": "Founder Unsealed", "photo_url": "https://.../peny.jpg" }
  ```
  Response `201` → `SeedUserResponse.id` (`seed_<uuid>`) inilah yang dipakai sebagai `sender_id`.
  
  Detail lengkap: `docs/be/seed_accounts_api.md` §`POST /api/v1/admin/seed-users`.

### 2. Upload gambar surat & amplop — presign flow

Sama seperti upload foto profil biasa (`docs/be/profile_api.md` §upload flow), **bukan endpoint baru**:
1. `POST /api/v1/storage/presign` dengan `{ "file_name": "welcome-letter.webp", "content_type": "image/webp" }` → response `{ "upload_url", "public_url", "key" }`.
2. `PUT` file bytes langsung ke `upload_url` (langsung ke R2, gak lewat backend Go).
3. Simpan `public_url` dari step 1 — itu yang dikirim sebagai `letter_image_url` (dan `envelope_image_url` kalau amplopnya juga custom) di `PATCH /admin/welcome-letter-config`.

**⚠️ Beda skema auth — dikonfirmasi ke kode (`package/middleware/auth.go:18-44`):** `POST /api/v1/storage/presign` **wajib** `Authorization: Bearer <firebase_id_token>`, **bukan** `X-Admin-Key`. Endpoint ini gak berada di bawah prefix `/api/v1/admin` (yang di-bypass dari `FirebaseAuth`) dan gak masuk `publicGetPaths`, jadi `X-Admin-Key` doang **tidak cukup** buat manggil endpoint ini dari webadmin.

### 3. Pilih envelope & stamp — dari katalog

- `GET /api/v1/envelopes` → `[{ "id": "ENVELOPE_2", "name": "Envelope 2", "image_url": "...", "is_premium": false }]` (`docs/be/envelopes_api.md`)
- `GET /api/v1/stamps` → shape serupa (`docs/be/stamps_api.md`)

Tampilin sebagai grid pilihan visual (thumbnail `image_url` + `name`), value yang dikirim ke `PATCH` adalah `id`.

**⚠️ Sama kasusnya dengan §2** — dua endpoint ini juga **wajib** Bearer token Firebase, bukan `X-Admin-Key` (sama-sama gak ada di `publicGetPaths` maupun prefix `/admin`).

**Implikasi buat webadmin:** ini internal tool tanpa login user biasa (cuma input `X-Admin-Key`, `docs/be/admin_dashboard_web.md` §Auth), jadi gak ada Firebase ID token yang bisa dipakai buat 3 panggilan di atas (`storage/presign`, `GET /envelopes`, `GET /stamps`). Pilihan yang realistis, urut dari paling gampang:
1. **Paling simpel:** minta ops login sekali pakai akun Firebase manapun di Android/tool terpisah, ambil ID token-nya manual, tempel sebagai env var/secret di webadmin (token Firebase expire ~1 jam, jadi ini cuma workaround jangka pendek buat testing, bukan solusi produksi).
2. **Lebih tahan lama:** minta backend expose 3 endpoint ini juga di bawah prefix `/api/v1/admin/*` (atau bikin admin-key bypass tambahan khusus buat ketiganya) — ini butuh perubahan kode backend, bukan cuma FE. Diskusikan ke tim backend sebelum mulai build kalau mau jalur ini.
3. **Alternatif tercepat buat MVP:** skip UI upload/browse di webadmin buat ketiga hal ini — isi `letter_image_url`/`envelope_id`/`stamp_id` manual via `curl`/Bruno pakai token sekali pakai, form webadmin cukup terima `sender_id` + `body_text` (yang beneran cuma butuh `X-Admin-Key`) sebagai MVP pertama, field gambar/katalog nyusul kalau opsi 2 sudah kelar.

---

## Alur form yang disarankan

1. Load halaman → `GET /admin/welcome-letter-config` buat isi initial state, sekaligus fetch `GET /admin/seed-users`, `GET /envelopes`, `GET /stamps` buat isi opsi dropdown.
2. Section "Pengirim" — dropdown seed users (+ tombol "buat persona baru" yang buka form mini `POST /admin/seed-users` inline).
3. Section "Isi Surat" — textarea `body_text` dengan character counter, tandai merah kalau < 20 karakter (soft-fail warning, jelasin konsekuensinya kalau tetap di-save — lihat gotcha di atas).
4. Section "Gambar Surat" — image uploader yang jalanin presign flow (§2), preview hasil upload.
5. Section "Amplop" — pilihan: (a) gambar custom (presign flow yang sama) atau (b) pilih dari katalog `GET /envelopes` + `GET /stamps` (grid visual).
6. Tombol "Simpan" → `PATCH /admin/welcome-letter-config` dengan field yang berubah aja.
7. **Indikator status jelas di UI**: badge besar "🟢 Aktif" / "🔴 Nonaktif" berdasarkan `sender_id != null` dari response terakhir — supaya ops langsung sadar kalau belum ke-setup lengkap, gak nebak-nebak dari field kosong satu-satu.

---

## Yang **tidak** perlu dikerjain di webadmin

- Gak ada tombol "kirim ke semua user existing" (broadcast) — ini **bukan** scope fitur ini secara sengaja (`docs/be/welcome_letter_api.md` §6). User yang udah register sebelum config diisi **tidak akan** dapet surat ini secara retroactive, dan tidak ada endpoint buat itu.
- Gak perlu preview "surat gimana kalau dibuka user" — kalau dibutuhin, itu murni render `letter_image_url` + `body_text` pakai komponen surat yang sama kayak yang dipakai buat preview surat lain di dashboard (kalau ada), bukan endpoint baru.

---

## Testing checklist

1. `PATCH` config dengan `sender_id` valid + semua field lain → `GET` balikin nilai yang sama persis.
2. Register akun baru (real flow atau lewat endpoint test kalau ada) → cek `GET /mailbox` akun itu punya room baru dari `sender_id` yang di-set, surat langsung `status = delivered` (gak ada badge "In Transit").
3. Kosongin `sender_id` (`PATCH { "sender_id": "" }` atau lewat DB langsung kalau API gak terima empty-string-sebagai-unset) → register akun baru lagi → pastikan **tidak** ada surat welcome terkirim, dan `POST /auth/register` tetap sukses normal.
4. Register akun yang **sama** dua kali (retry/reinstall+login) → pastikan cuma **satu** surat welcome yang masuk (idempotency guard `xmax = 0`, `docs/be/welcome_letter_api.md` §5).
