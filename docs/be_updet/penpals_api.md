# PenPal Feed API

`PenPalsScreen.kt` saat ini render `PenpalLetter` dummy (20 item) sebagai stack kartu amplop yang bisa di-drag/dibuka — bentuknya "postcard wall publik" (like + reply), **bukan** friend-request/matching klasik. Spec ini didesain mengikuti bentuk UI yang sudah ada, bukan bikin sistem connect/follow baru yang belum ada UI-nya.

Semua endpoint di bawah `/api/v1` dan butuh `Authorization: Bearer <firebase_id_token>` seperti biasa.

---

## `GET /api/v1/penpals/feed`

Query params: `cursor` (opsional, keyset pagination), `limit` (default mis. 20), `region` (opsional, filter salah satu dari 7 value `PenpalRegion`: `Africa, Antarctica, Asia, Europe, North America, Oceania, South America`).

Response `200`:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "letter_abc123",
        "sender_id": "firebase-uid-or-seed-id",
        "sender_name": "kirana_senja",
        "sender_photo_url": "https://.../photo.jpg",
        "region": "Asia",
        "envelope": "ENVELOPE_2",
        "stamp": "STAMP_ROSE",
        "envelope_sticker_id": null,
        "envelope_composite_image_url": "https://.../key-from-presign-flow",
        "composite_image_url": "https://.../key-from-presign-flow",
        "paper_url": "https://.../paper-texture.webp",
        "body_text": "...",
        "posted_at": "2026-07-13T09:00:00Z",
        "like_count": 18,
        "viewer_has_liked": false,
        "is_online": true
      }
    ],
    "next_cursor": "opaque-string-or-null"
  }
}
```

Catatan field:
- `sender_id` **wajib** ada (dummy `PenpalLetter` cuma punya `senderName`) — dibutuhkan supaya tap "reply" bisa langsung `POST /letters` dengan `recipient_id` terisi, tanpa lookup tambahan.
- `sender_photo_url` — nullable, sama semantiknya dengan `photo_url` di `profile_api.md` (`GET /users/{id}`, `PATCH /auth/me`). Diisi dari join ke `users.photo_url` di query feed/stack yang sudah ada (`app/module/penpal/repository/penpal_repository.go`), tidak butuh lookup tambahan per sender.
- `stamp`, `envelope_sticker_id`, `envelope_composite_image_url`, `composite_image_url`, `paper_url` — sama persis semantiknya dengan field bernama sama di `letters_api.md` (`POST /letters` request & `GET /letters/{id}` response); dikirim apa adanya dari row letter yang bersangkutan, tidak ada transformasi khusus buat feed ini. `paper_url` nullable, dipakai client buat render kertas pada mode fallback teks.
- `posted_at` dikirim mentah (ISO 8601), bukan pre-formatted string (`postedDateLabel` dummy) — biarkan client yang format sesuai locale.
- `is_online` = derived dari `last_active_at` user — `now() - last_active_at < 5 menit`, dihitung langsung di query (`app/module/penpal/repository/penpal_repository.go`). Gap M1 di `profile_api.md` sudah **resolved**: `last_active_at` di-update otomatis tiap request authenticated lewat middleware (`package/middleware/auth.go`), jadi field ini bukan lagi hardcode `false`.
- **(Baru)** Tanpa `region`, `items` isinya stack harian milik viewer (≤20 surat, di-refresh tiap hari), bukan lagi chronological semua surat — lihat section "Stack harian" di bawah dan detail lengkap di `daily_penpal_stack.md`.
- Campuran seed vs real content diatur lewat lever di `seed_accounts_api.md` — endpoint ini tidak butuh logika khusus, seed letters cuma baris biasa di tabel yang sama.

## `POST /api/v1/penpals/feed/{id}/like`

Toggle like (kalau viewer sudah like → unlike, dan sebaliknya) — idempoten per user per letter (unique constraint `(letter_id, user_id)` di tabel `likes`).

Response `200`:
```json
{ "success": true, "data": { "like_count": 19, "viewer_has_liked": true } }
```

## `GET /api/v1/penpals/regions`

Ganti angka dummy `PenpalRegion.dummyPenpalCount` (dipakai di `SelectRecipientScreen` "Penpal by region" sheet) dengan data real.

Response `200`:
```json
{ "success": true, "data": [ { "region": "Asia", "count": 480 }, { "region": "Antarctica", "count": 6 } ] }
```
`count` = jumlah user (real + seed) yang `continent == region`.

---

## Integrasi reply

Tap kartu di feed → buka detail → tombol "Reply" navigate ke Compose screen dengan `recipient_id` = `sender_id` dari item feed. Ini murni pemakaian `POST /api/v1/letters` (M4) — tidak ada endpoint reply terpisah.

## Stack harian (`GET /api/v1/penpals/feed` tanpa `region`)

Behavior baru — lihat `daily_penpal_stack.md` untuk detail lengkap (skema, worker, notifikasi, `notify_hour_pref`):
- **Tanpa `region`** dan viewer signed-in → `items` diambil dari stack harian personal milik viewer (≤20 surat, di-generate ulang tiap hari kalender UTC, "hilang" dari feed utama begitu stack besok datang — surat lama tetap ada di DB, like/reply riwayat tetap jalan).
- **Dengan `region`** (browsing eksplisit per region, termasuk fallback `POST /penpals/match` di bawah) → behavior lama tidak berubah, tetap chronological semua surat penpal, **tidak** dibatasi ke stack harian.
- Anonymous (soft-paywall preview, tanpa token) → tetap chronological lama, karena tidak ada "viewer" untuk digenerate stack-nya.
- Implementasi: `app/module/dailystack/` (worker generate + notif) dan `app/module/penpal/service/penpal_service.go` (`GetFeed`, lazy-generate on-demand kalau worker belum sempat jalan).
- **(Baru)** `POST /api/v1/penpals/feed/refresh` — reroll stack hari ini lebih awal, bayar `20` energy, tanpa limit harian. Detail lengkap di `daily_penpal_stack.md` §6.

## `POST /api/v1/penpals/match`

**Status: sudah live** — bukan lagi "belum dibangun" (`app/module/penpal/service/penpal_service.go` `MatchPenpal`, `app/module/penpal/repository/penpal_repository.go` `MatchPenpal`).

Endpoint buat fitur Send (`docs/be/send_and_envelope_handoff.md`) — "carikan satu penpal buat mulai obrolan baru" di suatu region, dipanggil dari `SelectRecipientScreen`'s alur "kirim ke penpal baru via region" (bukan reply). **Sengaja dipisah** dari `GET /penpals/feed`: feed itu buat browsing publik (paginated, sorted by recency/likes, nampilin banyak surat sekaligus), sedangkan endpoint ini tujuannya beda — "pilihkan satu orang", bukan "kasih daftar surat buat di-scroll".

Request:
```json
{ "region": "Asia" }
```
`region` = salah satu 7 value enum `continent` (sama seperti `GET /penpals/feed`'s query `region`).

Response `200`:
```json
{ "recipient_id": "firebase-uid-or-seed-id", "recipient_name": "kirana_senja" }
```
Response `404` kalau tidak ada user eligible di region itu:
```json
{ "error": "no penpals available in this region" }
```

Response `403` kalau viewer belum accept Terms of Service (`tos_accepted_at IS NULL`):
```json
{ "error": "please accept the Terms of Service before matching with a penpal" }
```

**Implementasi MVP yang cukup:** pilih satu user (real atau seed) secara random dari `continent == region`, exclude diri sendiri (`recipient_id != viewer_id`). Tidak perlu logic anti-repeat/interest-matching dulu — itu bisa nyusul kalau dirasa perlu, tanpa ubah kontrak endpoint ini (client cuma terima `recipient_id`/`recipient_name` jadi, bukan tahu algoritmanya).

**Fallback yang FE masih boleh pakai:** `SelectRecipientViewModel` coba panggil endpoint ini duluan; kalau error, fallback ke `GET /penpals/feed?region=X&limit=1`. Endpoint ini sekarang sudah live, jadi fallback ini idealnya jarang kepakai, tapi tetap aman dibiarkan sebagai safety net (mis. kalau region itu genuinely tidak ada eligible user, keduanya sama-sama akan 404/empty).

## Roadmap (belum dibangun)

- Ranking/recommendation berbasis interest-overlap — MVP cukup reverse-chronological atau random-shuffle per region. Algoritma pencocokan yang lebih pintar nyusul setelah ada cukup data engagement.
- Anti-repeat untuk stack harian — surat yang sama bisa saja ke-random-pick lagi di hari lain (lihat `daily_penpal_stack.md` §Known limitations). Belum masalah kalau supply surat penpal cukup banyak, tapi perlu diawasi.
- Report/Block **khusus dari UI feed** (mis. tombol report langsung di kartu surat) — endpoint generiknya (`POST /reports`, `POST/DELETE/GET /blocks`, module `trust`) **sudah ada** dan blocking sudah otomatis nyaring feed (`sender_id NOT IN blocks...`), tinggal FE wiring tombolnya di kartu feed kalau belum ada.
