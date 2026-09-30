# Admin Dashboard (Web) — Spec buat Proyek Terpisah

*Status: 🔜 belum dibangun di sini — dulu sempat ada prototype cepat (`webadmin/index.html`, vanilla JS single-file) yang dipakai buat verifikasi endpoint pas development, tapi sudah dihapus dari repo ini (2026-08-12) karena diputuskan dashboard-nya dikerjakan sebagai proyek/repo terpisah, bukan bagian dari backend Go ini. Dokumen ini spec kontrak API-nya biar proyek terpisah itu bisa langsung nyambung — pola yang sama kayak `docs/be/penpals_api.md` §`POST /penpals/match` ("FE duluan, backend nyusul"), cuma di sini arahnya kebalik: backend udah selesai duluan, tinggal dashboard-nya nyusul di tempat lain.*

Semua endpoint di bawah **backend-nya sudah live** di `app/module/admin/**` — tidak ada endpoint baru yang perlu ditambahkan ke backend Go untuk dashboard ini kecuali disebutkan di §Belum Ada.

---

## Auth: `X-Admin-Key`

- Semua route `/api/v1/admin/*` divalidasi lewat header `X-Admin-Key` yang dicocokkan ke env var `ADMIN_API_KEY` (`package/middleware/admin.go`) — **bukan** Firebase Bearer token seperti route lain.
- Key ini didapat dari tim ops (env var Railway), **jangan** pernah di-hardcode/commit ke repo dashboard.
- Dashboard perlu satu input field buat masukin key ini (simpan di `localStorage` cukup untuk internal tool), dan kirim di header `X-Admin-Key` di setiap request ke `/api/v1/admin/*`.
- Base URL: sama seperti backend API biasa, mis. `https://<railway-domain>/api/v1/admin/...`. CORS sudah di-allow global (`cors.New()` di `app/bootstrap/fiber.go`) jadi dashboard boleh di-host di domain/port manapun (Vite dev server, Vercel, dsb).

Response envelope standar di semua endpoint (`package/response/response.go`):
```json
{ "success": true, "data": { ... } }
```
atau kalau error:
```json
{ "success": false, "error": "pesan error" }
```

---

## 1. Seed Accounts (persona internal)

Lihat detail lengkap di `docs/be/seed_accounts_api.md`. Ringkasan endpoint:

| Method | Path | Body / Query | Catatan |
|---|---|---|---|
| `POST` | `/seed-users` | `{nickname, continent, gender?, birthday?, bio?, photo_url?, interest_ids?}` | 201, return `SeedUserResponse` |
| `PATCH` | `/seed-users/:id` | field sama, semua optional | 200 |
| `GET` | `/seed-users?cursor=&limit=` | — | `{items: SeedUserResponse[], next_cursor}` |
| `DELETE` | `/seed-users/:id` | — | soft-delete (`archived_at`), 200 no body |
| `POST` | `/seed-users/:id/letters` | sama seperti `POST /letters` biasa | kirim surat sebagai persona |
| `POST` | `/seed-users/:id/penpal-posts` | `{region, envelope, body_text}` | post ke penpal feed sebagai persona |

`SeedUserResponse`:
```json
{
  "id": "seed_...", "nickname": "...", "continent": "Asia", "is_premium": false,
  "gender": "f", "birthday": "1998-04-12", "photo_url": "...",
  "bio": "...", "is_seed": true, "interests": ["reading", "coffee"]
}
```

**UI yang masuk akal:** grid card per persona (foto, nama, continent, bio, interest pills), form create/edit di modal, tombol archive, dan idealnya tombol quick-action "Send Letter as..." / "Post to Feed as..." yang buka form terpisah.

---

## 2. Sticker Catalog

| Method | Path | Body | Catatan |
|---|---|---|---|
| `GET` | `/stickers` | — | return array `Sticker` entity langsung (bukan `StickerResponse` DTO — includes `created_at`/`updated_at`/`archived_at`) |
| `POST` | `/stickers` | `{id?, name, image_url, is_premium, pack}` | `id` optional, auto-generate kalau kosong |
| `PATCH` | `/stickers/:id` | semua field optional | partial update |
| `DELETE` | `/stickers/:id` | — | soft-delete (`archived_at`), 200 no body |

Entity `Sticker`:
```json
{ "id": "sticker_sparkles", "name": "Sparkles", "image_url": "https://...", "is_premium": false, "pack": "basic", "created_at": "...", "updated_at": "...", "archived_at": null }
```

**UI:** grid preview image + nama, filter by pack & premium/free, badge ARCHIVED kalau `archived_at` ada, toggle premium quick-action, form create/edit.

---

## 3. Stamp Catalog

| Method | Path | Body | Catatan |
|---|---|---|---|
| `GET` | `/stamps` | — | array `StampResponse` |
| `POST` | `/stamps` | `{id, name, image_url, is_premium}` | |
| `PATCH` | `/stamps/:id` | semua field optional | |
| `DELETE` | `/stamps/:id` | — | soft-delete |
| `POST` | `/users/:id/grant-stamp` | `{stamp_id}` | kasih stamp ke user manapun (real/seed), buat reward manual/compensate |

`StampResponse`: `{ "id": "...", "name": "...", "image_url": "...", "is_premium": false }`

**UI:** mirip sticker grid, plus form terpisah "Grant Stamp to User" (input user id + pilih stamp).

---

## 4. Trust & Safety (§1/§2/§5 dari `docs/be/trust_and_safety_api.md`)

Ini bagian yang paling penting buat dashboard — **tanpa UI ini, laporan yang masuk dari app nggak ada yang bisa ditindaklanjuti**.

### Review queue laporan
| Method | Path | Query/Body | Catatan |
|---|---|---|---|
| `GET` | `/reports?status=&reason=&target_type=&cursor=&limit=` | `status` optional: `pending`\|`reviewed`\|`actioned`\|`dismissed`; `reason`/`target_type` optional exact-match filter (kosong = semua) | `{items: ReportResponse[], next_cursor, total_pending}` |
| `GET` | `/reports/:id` | — | detail satu laporan, response sama seperti item list + `related_reports` (laporan lain ke target yang sama dalam 30 hari terakhir) |
| `PATCH` | `/reports/:id` | `{status, admin_note?}` | update status laporan, response sukses `{"success": true}` tanpa `data` |
| `DELETE` | `/content/:type/:id` | — | soft-delete konten yang dilaporkan (`:type` = `letter`\|`penpal_post`), response `{data: {id, deleted_at}}` — dipakai buat tombol "Hapus Konten" |

`ReportResponse`:
```json
{
  "id": "report_xyz789", "reporter_id": "uid_...", "target_type": "user",
  "target_id": "uid_atau_letter_id", "reason": "harassment",
  "letter_id": "uuid_atau_null", "note": "catatan bebas dari pelapor atau null",
  "status": "pending", "created_at": "2026-08-12T09:00:00Z"
}
```
`reason` enum: `harassment`, `spam`, `inappropriate_content`, `underage`, `impersonation`, `other`, `empty_mail`.

### Suspend / Ban / Unsuspend user
| Method | Path | Body | Catatan |
|---|---|---|---|
| `POST` | `/users/:uid/moderation-action` | `{action: "suspend"\|"ban"\|"unsuspend", reason, expires_at?, duration_days?}` | **endpoint yang dipakai buat dashboard baru** — satu endpoint unified buat ketiga aksi, termasuk unsuspend (set balik `account_status = "active"`). Response `{data: {uid, account_status, suspended_at, suspend_until}}` |
| `POST` | `/users/:id/suspend` | `{reason, duration_days}` | **legacy**, action-nya di-hardcode `"suspend"` server-side — dipertahankan buat backward-compat, gak perlu dipakai di dashboard baru |
| `POST` | `/users/:id/ban` | `{reason}` | **legacy**, action di-hardcode `"ban"` — sama, gak perlu dipakai di dashboard baru |

Efek langsung: user yang di-suspend/ban akan dapat `403` di **semua** endpoint biasa (bukan cuma admin) begitu request berikutnya masuk — dicek di `package/middleware/auth.go`.

**UI yang direkomendasikan** (ini yang sempat dibangun di prototype yang dihapus, jadi referensi kalau mau dibangun ulang):
- Filter dropdown status laporan (default `pending`, biar reviewer langsung lihat yang perlu ditindaklanjuti) + search box (target_id/reporter_id/report_id/letter_id).
- Tiap laporan ditampilkan sebagai card: `target_type`+`target_id`, badge status berwarna, `reason` + `letter_id` kalau ada, `note` kalau ada, `reporter_id`, timestamp.
- Tombol per-card: "Mark Reviewed" / "Dismiss" / "Mark Actioned" → `PATCH /reports/:id`.
- Tombol "🚫 Suspend/Ban" per-card (auto-prefill user id kalau `target_type == "user"`, kosong kalau `target_type == "letter"` karena `target_id`-nya letter id bukan user id — reviewer perlu cari tau sender letternya dulu lewat tool lain) yang buka modal terpisah: input User ID, pilihan Suspend/Ban, Reason wajib, Duration Days (kosong = indefinite, cuma relevan buat Suspend) → `POST /users/:id/suspend` atau `/ban`.
- Modal konfirmasi sebelum submit suspend/ban (aksi langsung berefek, user ke-lock keluar app seketika).

---

## 5. Yang Belum Ada di Backend (kalau dashboard butuh)

- **Lookup user by id** (buat validasi User ID sebelum suspend/ban, atau buat resolve `sender_id` dari sebuah `letter_id` pas ada laporan `target_type=letter`) — belum ada endpoint admin buat "get user detail" atau "get letter detail" generik. Kalau dashboard butuh ini, perlu endpoint baru semacam `GET /api/v1/admin/users/:id` dan `GET /api/v1/admin/letters/:id` — belum di-spec, koordinasikan dulu sebelum diimplementasikan.
- **`admin_audit_log` read endpoint** — tabel audit log (`action`, `target_id`, `created_at`) sudah diisi tiap aksi admin (termasuk suspend/ban/update-report-status), tapi belum ada endpoint buat baca isinya dari dashboard. Kalau mau ada halaman "Activity Log", perlu endpoint baru dulu.

> ~~Unsuspend / un-ban manual~~ — **sudah ada** (update 2026-08-20): `POST /api/v1/admin/users/:uid/moderation-action` dengan `action: "unsuspend"` — lihat tabel "Suspend / Ban / Unsuspend user" di atas. Klaim lama di sini (belum ada endpoint eksplisit) sudah basi, dibiarkan tercoret buat histori.

Item di atas di luar scope backend yang sudah dibangun — catat di sini biar kelihatan kalau dashboard butuh salah satunya, baru dikoordinasikan mau nambah endpoint atau nggak.
