**Status: implemented (2026-08-19).** Kode udah masuk (`app/module/auth/service/auth_service.go`, `app/module/letter/service/letter_service.go`, `config/env.go`) — sisanya di bawah tetap dipertahankan sebagai referensi desain, ditambah catatan implementasi real di tiap section.

# Welcome Letter dari Admin — Backend Handoff

Fitur baru: setiap user yang baru **register** (bukan "first install" — lihat catatan di bawah kenapa bedanya penting) otomatis dapet satu surat welcome dari persona admin/founder, gambarnya sudah di-desain manual sebelumnya (bukan hasil compositing per-user). Dokumen ini entry point buat kerjaan backend-nya — **tidak ada perubahan Android sama sekali**, karena surat ini cuma insert row `letters` biasa dan `GET /mailbox` (`docs/be/letters_api.md` §Mailbox) sudah otomatis nampilin room baru begitu ada surat, tidak peduli siapa pengirimnya.

---

## Kenapa hook-nya di `POST /auth/register`, bukan "first install"

`POST /auth/register` (`docs/be/profile_api.md` §"`POST /api/v1/auth/register`") cuma kejadian **sekali per akun** — beda dari "first install" yang bisa kejadian berkali-kali (user uninstall lalu install ulang & login akun yang sama). Trigger di titik ini otomatis benar secara semantik: satu welcome letter per akun, seumur hidup akun itu, gak peduli device/install keberapa. `nickname` & `continent` juga sudah tersedia langsung dari body request ini — gak perlu nunggu onboarding profiling (`PATCH /auth/me` dkk) selesai duluan.

---

## Yang perlu di-prepare

### 1. Persona pengirim ("Admin"/founder account)

Reuse endpoint yang sudah ada, **tidak butuh endpoint baru**: `POST /api/v1/admin/seed-users` (`docs/be/seed_accounts_api.md` §"`POST /api/v1/admin/seed-users`"). Dipanggil **sekali** secara manual oleh ops — nickname, continent, foto profil, bio persona ini. Hasil `id`-nya (`seed_<uuid>`) diisi ke `sender_id` lewat `PATCH /api/v1/admin/welcome-letter-config` (§6 — DB-backed, bukan env var lagi, lihat revisi 2026-08-19).

Persona ini tetap seed account normal — kalau product decision-nya user boleh reply (lihat §4 di bawah), gak perlu kerjaan tambahan, `SelectRecipientScreen`/Compose di client sudah bisa target `correspondent_id` mana pun tanpa tahu itu seed atau bukan (`is_seed` sengaja tidak pernah keekspos ke client, `docs/be/seed_accounts_api.md` §"Data model").

### 2. Asset gambar surat (fixed, bukan per-user)

Gambarnya **sama buat semua user** — beda dari flow compositing normal (`LetterCompositor.kt` di client, per-surat). Jadi cukup upload **sekali**:
- Lewat `POST /storage/presign` yang sudah ada (`docs/be/api_contract.md`) — panggil manual sekali, dapet `public_url` tetap, simpan sebagai konstanta.
- Kalau amplopnya juga custom-desain (bukan pakai desain katalog biasa), siapin juga satu `envelope_composite_image_url` tetap dengan cara yang sama.

Hasil akhir: dua URL tetap (`letter_image_url`, opsional `envelope_image_url`) yang diisi lewat `PATCH /api/v1/admin/welcome-letter-config` (§6) dan dipakai berulang di setiap kirim, bukan generate baru tiap user.

### 3a. Keputusan produk yang sudah diambil (2026-08-19)

- **Instant delivery**: dipilih. Welcome letter langsung `status = "delivered"`, gak lewat delay continent seperti surat biasa — user bisa buka begitu masuk mailbox pertama kali. Diimplementasi via method baru `LetterService.SendInstantAsSeed` (`app/module/letter/service/letter_service.go`), yang skip perhitungan `EstimatedArrivalAt` dan langsung set `DeliveredAt = now()`.
- **Boleh di-reply**: tidak ada kerjaan tambahan diambil (default), persona tetap discoverable seperti seed persona biasa.

### 3. Trigger otomatis di alur register

Tepat setelah row `users` baru **berhasil di-insert** (bukan di path lookup user lama yang sudah ada), panggil langsung function internal — karena instant delivery dipilih (§3a), yang dipanggil adalah **`letter/service.LetterService.SendInstantAsSeed(ctx, WELCOME_SENDER_ID, req)`** (bukan `SendAsSeed` biasa, yang tetap kena delay continent). Ini **function call langsung**, bukan HTTP round-trip ke endpoint admin.

**Implementasi real (2026-08-19):** trigger-nya ada di `AuthService.Register` → `AuthService.sendWelcomeLetter` (`app/module/auth/service/auth_service.go`), dipanggil sebagai `go s.sendWelcomeLetter(user.ID, user.Nickname)` — goroutine terpisah dengan `context.Background()` (bukan context request, biar gak ke-cancel begitu HTTP response udah balik). `AuthService` sekarang depends langsung ke `*letterservice.LetterService` (constructor param baru, diwire di `app/bootstrap/app.go` — `letterModule` dikonstruksi sebelum `authModule` supaya `letterModule.Service()` bisa dipass masuk) dan ke `*config.Env` (buat baca semua `WELCOME_*` env var, lihat §6 baru di bawah).

**Koreksi (2026-08-19, tervalidasi ke kode):** `sender_id` **bukan field di request struct** (`SendLetterRequest`, `app/module/letter/dto/letter_dto.go:3-18`) — sender diterima sebagai parameter fungsi terpisah (`senderID string`, argumen ke-2 di `SendAsSeed`), bukan bagian dari JSON body. Baris `sender_id` di tabel di bawah maksudnya "nilai yang dipass sebagai `personaID` ke `SendAsSeed`", bukan field body.

Field request (`SendLetterRequest`):

| Field | Nilai |
|---|---|
| *(param fungsi, bukan field body)* `personaID` | `WELCOME_SENDER_ID` |
| `recipient_id` | id user yang baru saja register |
| `dear_name` | nickname dari body `POST /auth/register` |
| `body_text` | **tetap wajib**, min 20 karakter — lihat catatan di bawah |
| `composite_image_url` | `WELCOME_LETTER_IMAGE_URL` (konstanta dari §2) |
| `envelope_composite_image_url` | `WELCOME_ENVELOPE_IMAGE_URL` kalau ada, else `null` |
| `envelope` / `stamp` | id katalog yang dipilih buat surat ini (lihat "Open questions" di bawah) |
| `visibility` | `"private"` — **jangan** `"public"`, biar gak nongol di Postal Collection Showcase siapa pun |
| `crisis_tag` | `null` |
| `sticker_data` | `[]` |
| `envelope_sticker_id` | `null` |

Struct-nya juga punya `paper_template` (nullable, boleh `null`), `paper_color`, `font_id` — gak divalidasi wajib-isi (`validation.go` cuma cek `envelope`/`stamp` non-empty), jadi aman dibiarin default/string kosong kalau gak ada desain spesifik buat welcome letter.

**Catatan penting soal `body_text`:** validasi `POST /letters` (`docs/be/letters_api.md` §"`POST /api/v1/letters`") gak bedain berdasarkan ada-tidaknya `composite_image_url` — `body_text` tetap wajib min 20 karakter walau gambarnya sudah final. Perlu satu caption pendek yang disiapin sekali (misal isi ringkas dari pesan yang ada di gambar), bukan string kosong/placeholder asal-asalan karena field ini juga dipakai buat search/notifikasi-preview.

### 4. Keputusan produk (sudah diputuskan, lihat §3a — bagian ini historis)

- ~~Instant delivery vs slow-delivery normal?~~ **Diputuskan: instant** (§3a). Diimplementasi via `SendInstantAsSeed`, bukan percabangan di `Send()`.
- ~~Boleh di-reply?~~ **Diputuskan: ya, default** (§3a) — gak ada kerjaan tambahan.
- **`envelope`/`stamp` id apa** — **masih open, ini yang bikin fitur belum aktif di production.** Harus id valid dari `GET /envelopes`/`GET /stamps` (`docs/be/envelopes_api.md`, `docs/be/stamps_api.md`), diisi ke env var `WELCOME_ENVELOPE_ID`/`WELCOME_STAMP_ID` (§6). Bisa reuse desain katalog yang sudah ada, atau bikin desain khusus "Official"/"Welcome".

### 5. Idempotency & keandalan

- **Idempotency guard — implemented (2026-08-19).** Dokumen awal sempat asumsi `/auth/register` "return existing user, bukan create baru" kalau akun udah ada — **itu keliru**. Implementasi aslinya (`app/module/auth/repository/auth_repository.go`) pakai `INSERT INTO users (...) VALUES (...) ON CONFLICT (id) DO UPDATE SET nickname=EXCLUDED.nickname, continent=EXCLUDED.continent, ... RETURNING`, yaitu **upsert yang selalu overwrite** kolom itu tiap dipanggil, bukan no-op kalau row udah ada.
  **Sudah ditambahin**: `Upsert` sekarang `RETURNING ..., (xmax = 0) AS inserted` dan return `(*entity.User, bool, error)` — bool-nya `true` cuma kalau row itu baru aja di-insert (trik standar Postgres: `xmax = 0` berarti fresh insert, bukan hasil update). `AuthService.Register` cuma manggil `sendWelcomeLetter` kalau `inserted == true`, jadi aman dari duplikasi walau `/auth/register` dipanggil ulang (retry, reinstall+login, dsb).
- **Non-blocking — implemented.** `AuthService.Register` manggil `go s.sendWelcomeLetter(...)` (goroutine terpisah, `context.Background()`, bukan context request) — kegagalan (storage down, config belum lengkap, dst) cuma di-`log.Printf`, gak pernah bikin `POST /auth/register` gagal/lambat.
- **Scope**: trigger ini **hanya** untuk user asli yang lewat `POST /auth/register` (`AuthService.Register`), **bukan** untuk `POST /admin/seed-users` (beda code path, `AdminService` gak manggil `sendWelcomeLetter`) — seed persona lain gak akan saling kirim welcome letter.

### 6. Config — DB-backed, bukan env var (revisi 2026-08-19)

**Keputusan produk (2026-08-19):** awalnya config ini disimpan sebagai env var Railway (§6 versi lama). Diputuskan pindah ke tabel DB + endpoint admin, biar bisa diedit dari webadmin tanpa redeploy — sekalian jadi pola dasar buat CMS-lite config lain ke depannya kalau perlu. **Bukan fitur broadcast** (kirim ke semua user existing) — itu keputusan sadar buat scope kecil dulu (lihat diskusi 2026-08-19); broadcast beneran, kalau nanti dibutuhin, bisa dibangun di atas tabel yang sama tapi itu kerjaan terpisah (job/batch processing, target audience, dst).

- **Migration**: `db/migrations/028_welcome_letter_config.sql` — tabel singleton `welcome_letter_config` (pola `id BOOLEAN PRIMARY KEY DEFAULT true CHECK (id)`, jamin cuma 1 row), di-seed 1 row kosong otomatis saat migration jalan.
- **Package baru**: `app/module/welcomeletter/` (`entity.Config`, `repository.WelcomeLetterRepository`) — leaf package, di-import langsung oleh `auth` (baca, di setiap send — bukan cache dari boot, jadi edit dari webadmin langsung kepakai tanpa restart) dan `admin` (baca+tulis, buat endpoint di bawah).
- **Endpoint admin baru**:

| Method | Path | Fungsi |
|---|---|---|
| `GET` | `/api/v1/admin/welcome-letter-config` | Baca config saat ini |
| `PATCH` | `/api/v1/admin/welcome-letter-config` | Update partial — field yang di-omit/`null` gak berubah (pola sama kayak `PATCH /admin/seed-users/{id}`) |

Body/response fields: `sender_id`, `body_text`, `letter_image_url`, `envelope_image_url`, `envelope_id`, `stamp_id` (semua nullable kecuali `body_text`).

- **Kosong = fitur no-op** — kalau `sender_id` belum diisi (default row baru), `AuthService.sendWelcomeLetter` langsung return tanpa ngirim apa-apa, register tetap jalan normal. Ini switch utama buat "nyalain" fitur ini di production, sekarang lewat `PATCH /api/v1/admin/welcome-letter-config` alih-alih Railway dashboard.

---

## Yang backend **tidak** perlu bikin baru

~~Tidak ada endpoint baru, tidak ada tabel baru, tidak ada migration baru~~ — **ini berubah setelah keputusan §6 direvisi (2026-08-19)**: sekarang ada 1 tabel baru (`welcome_letter_config`) + 1 migration + 2 endpoint admin, supaya config-nya editable dari webadmin. Tetap full reuse infrastruktur `letters`/`mailbox` yang sudah ada buat pengiriman surat itu sendiri. Kerjaan kode-nya **sudah selesai** (§3, §3a, §5, §6); yang tersisa murni ops/produk: bikin persona (§1) via `POST /admin/seed-users`, upload dua URL gambar (§2) via `POST /storage/presign`, pilih `envelope`/`stamp` id (§4), lalu isi semuanya lewat `PATCH /api/v1/admin/welcome-letter-config` (§6) — dari webadmin, bukan Railway dashboard.

## Android: tidak ada perubahan

`MailboxScreen.kt`/`MailboxViewModel`/`GET /mailbox` sudah generic — begitu ada row `letters` baru dengan `recipient_id` = user itu, room-nya otomatis muncul di list mailbox user tersebut tanpa perlu tahu sender-nya admin atau user biasa. Kalau keputusan §4 "instant delivery" diambil, `MailboxRoomItem.isLastLetterInTransit` otomatis `false` dan surat langsung bisa dibuka tanpa badge "In Transit" — juga tanpa perubahan kode client.
