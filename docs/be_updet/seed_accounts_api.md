# Seed / Admin Account API

> **Status: ✅ Implemented (2026-08-02)** — modul baru `app/module/admin/**` + `package/middleware/admin.go`. Semua endpoint di bawah sudah jalan sesuai spec ini; `admin_audit_log` (rekomendasi opsional di dokumen ini) juga sudah dibuat dan diisi tiap aksi admin.

Solusi untuk masalah *chicken-egg* (network effect): app pen-pal butuh kepadatan konten & user lain supaya terasa hidup, tapi di awal belum ada real user yang cukup. Milestone ini membangun jalur **internal-only** untuk membuat & mengoperasikan akun "seed" (persona yang dikontrol tim sendiri) tanpa perlu login Google/Firebase per akun.

Semua endpoint di bawah ini **terpisah total** dari auth flow biasa — tidak pakai `Authorization: Bearer <firebase_id_token>`, dan **tidak pernah dipanggil dari client Android**. Ini murni tool ops (dipanggil lewat Postman/script/internal dashboard).

---

## Auth: `X-Admin-Key`

- Semua route `/api/v1/admin/*` divalidasi middleware terpisah yang cek header `X-Admin-Key` terhadap secret di environment variable (mis. `ADMIN_API_KEY`), bukan Firebase token.
- Key ini **tidak boleh** pernah masuk ke `BuildConfig`/APK Android — hanya dipegang tim ops, dipakai dari luar app.
- Rotate key via env var kalau bocor; tidak ada mekanisme rotasi otomatis di MVP.
- Rekomendasi: log setiap call admin (timestamp + action + target id) ke tabel `admin_audit_log` sederhana untuk akuntabilitas — bukan hard requirement MVP, tapi murah untuk ditambahkan sejak awal.

> **Implementasi — gotcha routing:** Fiber mencocokkan middleware `Group()`/`Use()` berdasarkan **prefix path**, bukan boundary grup yang eksak. Karena semua route lain sudah di-mount di bawah prefix `/api/v1` dengan middleware `FirebaseAuth`, dan `/api/v1/admin/*` juga diawali `/api/v1`, `FirebaseAuth` ikut tereksekusi untuk request admin juga (menolaknya karena tidak ada Bearer token) — route admin jadi tidak pernah ke-reach oleh `AdminKey` sama sekali kalau tidak ditangani. Fix-nya: `FirebaseAuth` (`package/middleware/auth.go`) punya bypass eksplisit di awal untuk path berprefix `/api/v1/admin`, membiarkan request lanjut ke middleware `AdminKey` yang memang bertugas menjaga prefix itu. Kalau ke depan ada prefix baru lain di bawah `/api/v1` yang juga perlu bypass Firebase, pola yang sama perlu diikuti.

---

## Data model

Tambah kolom `is_seed BOOLEAN DEFAULT false` ke tabel `users`.

**Aturan paling penting:** `is_seed` **tidak pernah** muncul di response yang dilihat client manapun (`GET /auth/me`, `GET /penpals/feed`, `GET /letters/inbox`, dst). Akun seed harus 100% tidak bisa dibedakan dari akun asli oleh user biasa — shape `UserDto` yang dikembalikan sama persis.

---

## `POST /api/v1/admin/seed-users`

Bikin satu persona baru. Bypass Firebase — backend generate `id` sendiri (mis. `seed_<uuid>`).

Request body (superset dari `RegisterRequest` + field profil, semua langsung di-set di satu call, beda dari flow onboarding real user yang bertahap):
```json
{
  "nickname": "kirana_senja",
  "continent": "Asia",
  "gender": "f",
  "birthday": "1998-04-12",
  "bio": "Suka nulis surat sambil dengerin hujan.",
  "photo_url": "https://.../seed/kirana.jpg",
  "interest_ids": ["reading", "coffee", "music"]
}
```

Response `201`: full `UserDto` (shape identik `GET /auth/me`, `is_seed` tidak ikut serialize).

> **Implementasi:** `interest_ids` divalidasi (harus semua ada di katalog `interests`) **sebelum** row user dibuat — urutan ini sengaja, supaya `interest_id` yang salah gagal cepat tanpa meninggalkan persona "yatim" (row user ter-create tapi request tetap balas error) di database.

## `PATCH /api/v1/admin/seed-users/{id}`

Update sebagian field persona, path pakai `id` eksplisit (bukan dari token, karena tidak ada token per-persona). **Bukan field yang sama persis seperti `PATCH /auth/me`** — `UpdateSeedUserRequest` (`app/module/admin/dto/admin_dto.go:13-20`) cuma subset: `nickname`, `continent`, `gender`, `birthday`, `photo_url`, `bio`. Field yang ada di `PATCH /auth/me` tapi **tidak ada** di sini (karena gak relevan buat persona tanpa sesi login sendiri): `fcm_token`, `tos_accepted_at`, `notify_hour_pref`, `utc_offset_minutes`.

## `GET /api/v1/admin/seed-users`

List semua akun seed (paginated: `?cursor=&limit=`) — untuk dashboard ops lihat persona mana aja yang aktif. Response includes `is_seed: true` di sini karena ini endpoint admin-only, bukan dilihat client.

## `DELETE /api/v1/admin/seed-users/{id}`

Retire persona (soft-delete / set `archived_at`, jangan hard delete supaya surat lama yang pernah dikirim persona ini tetap valid secara referential).

> **Implementasi:** kolom `users.archived_at TIMESTAMPTZ` ditambahkan sesuai rekomendasi di atas. Belum ada logic tambahan yang menyembunyikan persona ter-archive dari `GET /admin/seed-users` atau dari surat lama yang pernah mereka kirim — sesuai spec, arsip murni menandai "retired", tidak menghapus jejak apapun.

## `POST /api/v1/admin/seed-users/{id}/letters`

Kirim surat **sebagai** persona ini ke user tertentu (real atau seed lain). Bergantung pada M4 (`docs/be/letters_api.md`) — tabel `letters` harus sudah ada; endpoint ini cuma varian `POST /letters` yang `sender_id` di-override manual dari path, bukan dari token.

```json
{ "recipient_id": "firebase-uid-or-seed-id", "dear_name": "...", "body_text": "...", "envelope": "ENVELOPE_2", "stamp": "OCEAN", "paper_template": "AGED_KRAFT" }
```

Response `201`: letter object, sama seperti `POST /letters` biasa.

> **Implementasi:** endpoint ini memanggil `letter/service.LetterService.SendAsSeed(ctx, personaID, req)` (`app/module/letter/service/letter_service.go:55-57`) — bukan `Send` biasa seperti yang sempat ditulis di sini sebelumnya (nama fungsinya sudah usang). `SendAsSeed` secara fungsional identik dengan `Send` saat ini (parameter `isSeed` yang membedakannya tidak dipakai di `ValidateSendRequest`), jadi aturan validasi, delay per-continent, dan shape response tetap sama seperti `POST /letters` biasa — cuma method call-nya yang beda nama.

## `POST /api/v1/admin/seed-users/{id}/penpal-posts`

Post satu "kartu pos" ke PenPal feed atas nama persona ini. Bergantung pada M3 (`docs/be/penpals_api.md`). Body cuma `{region, envelope, body_text}`.

> **Field yang di-hardcode server-side, tidak ada di request body** (`AdminService.PostToFeedAsSeed`, `app/module/admin/service/admin_service.go:264-298`): `paper_color: "white"`, `stamp: "INK"`, `font_id: "default"`, `sticker_data: []`. Post seed selalu langsung `status: "delivered"` (bukan `in_transit`) dengan `delivered_at = now()`. Kalau butuh variasi stamp/warna kertas per post, endpoint ini perlu diupdate dulu — saat ini semua post seed keluar dengan kombinasi hardcode itu.

---

## Ops lever: rasio seed vs real di feed/matching

`GET /api/v1/penpals/feed` (M3) dan rekomendasi ke depan sebaiknya punya knob (env var atau kolom config, mis. `SEED_MIX_RATIO` per region) buat ngatur proporsi konten seed vs real yang muncul. Di awal (real user dikit) rasio bisa tinggi ke seed; seiring user asli bertambah, turunin manual/berkala. Tidak perlu algoritma otomatis di MVP — ini tuas manual yang di-tweak ops.

## Roadmap (belum dibangun)

- Dashboard internal (UI) — MVP cukup Postman/script, dashboard nyusul kalau volume seed content makin banyak.
- Scheduler otomatis buat posting konten seed berkala (cron "posting N kartu pos/hari").
- Analytics: seed vs real engagement (buat tahu kapan rasio bisa diturunin).
