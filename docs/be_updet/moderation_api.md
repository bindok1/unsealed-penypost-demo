# Moderation & Reporting API — Spec

> **Status (update 2026-08-18): sudah diimplementasi.** Doc ini awalnya ditulis sebagai spec sebelum dibangun (`Status: Belum diimplementasi` di bawah ini basi, dibiarkan buat histori) — implementasinya sudah masuk (`829cbf7 add fitur block and bug letters`, `78c7c3c impelemnt moderasi systme`). `trust` module (`app/module/trust`) live buat `POST /reports`, `POST/DELETE/GET /blocks`; `admin` module live buat suspend/ban/`moderation-action`/list-detail-update report/delete content. Sisa doc di bawah tetap valid sebagai referensi kontrak/schema, cuma framing "belum dibangun"-nya yang perlu diabaikan.
>
> **Update (2026-08-20): beberapa detail kontrak di bawah sudah drift dari implementasi aktual** (ditemukan lewat audit `docs/be/*.md` vs kode) — dikoreksi inline di section terkait (§DB Schema, §1, §5). Kalau butuh ground truth: `app/module/trust/service/validation.go` (reason enum) dan `app/module/admin/dto/admin_dto.go` (response shapes).
>
> ~~**Status:** Belum diimplementasi. Ini adalah spec/desain endpoint yang perlu dibangun di BE (Unsealed Go backend) dan di-integrate ke web admin dashboard.~~
>
> **Scope:** Report, Suspend/Ban user, Soft-delete letter yang melanggar, sistem notifikasi moderasi.

---

## Latar Belakang

Berdasarkan gap dari `letters_api.md` (line 109) dan `penpals_api.md` (line 75), fitur Report & Block masih `ComingSoonBottomSheet` di sisi mobile client dan belum ada tabel/endpoint di backend **(saat doc ini ditulis — sudah tidak akurat, lihat status update di atas)**. Dokumen ini mendefinisikan kontrak penuh yang perlu diimplementasi.

---

## Database Schema (New Tables)

> **Skema aktual berbeda dari draft di bawah** — khususnya `target_type` (`user`/`letter` saja, tidak ada `penpal_post`) dan `reason` (`harassment`, `spam`, `inappropriate_content`, `underage`, `impersonation`, `other`, `empty_mail` — bukan set di bawah ini). SQL draft dipertahankan buat histori desain awal, tapi jangan dipakai sebagai ground truth; ground truth-nya `app/module/trust/service/validation.go` (`ValidTargetTypes`, `ValidReportReasons`) dan migration aktual di `db/migrations/`.

### Tabel `user_reports`
```sql
CREATE TABLE user_reports (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id   TEXT NOT NULL REFERENCES users(id),  -- Firebase UID pelapor
  reported_id   TEXT NOT NULL REFERENCES users(id),  -- Firebase UID yang dilaporkan
  target_type   TEXT NOT NULL CHECK (target_type IN ('letter', 'penpal_post', 'user')),
  target_id     TEXT NOT NULL,                        -- ID surat / post / uid user
  reason        TEXT NOT NULL CHECK (reason IN (
                  'harassment', 'hate_speech', 'sexual_content',
                  'spam', 'fraud', 'violence', 'other'
                )),
  body_snapshot TEXT,                                 -- Snapshot isi konten saat dilaporkan
  status        TEXT NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending', 'reviewed', 'actioned', 'dismissed')),
  admin_note    TEXT,                                 -- Catatan internal admin
  reviewed_by   TEXT,                                 -- Admin UID/identifier yang meninaju
  reviewed_at   TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_reports_reported_id ON user_reports(reported_id);
CREATE INDEX idx_user_reports_status ON user_reports(status);
CREATE INDEX idx_user_reports_created_at ON user_reports(created_at DESC);
```

### Kolom tambahan di tabel `users` (Migration)
```sql
-- Tambahkan ke tabel users yang sudah ada
ALTER TABLE users
  ADD COLUMN account_status TEXT NOT NULL DEFAULT 'active'
    CHECK (account_status IN ('active', 'suspended', 'banned')),
  ADD COLUMN suspended_at   TIMESTAMPTZ,
  ADD COLUMN suspended_reason TEXT,
  ADD COLUMN suspend_expires_at TIMESTAMPTZ;  -- NULL = permanent ban
```

---

## Endpoint Baru

### 1. Laporan dari User (Mobile App)

**Auth:** Firebase Bearer Token  
**Path:** `POST /api/v1/reports`

> **Request/response aktual beda dari draft di bawah** — lihat catatan setelah tiap blok.

```json
// Request Body (draft — lihat kontrak aktual di bawah)
{
  "target_type": "letter | penpal_post | user",
  "target_id": "string",
  "reason": "harassment | hate_speech | sexual_content | spam | fraud | violence | other",
  "description": "string? (max 500 karakter, opsional)"
}
```

**Kontrak aktual** (`app/module/trust/dto/report_dto.go`, `app/module/trust/service/validation.go`):
```json
{
  "target_type": "letter | user",
  "target_id": "string",
  "reason": "harassment | spam | inappropriate_content | underage | impersonation | other | empty_mail",
  "note": "string? (opsional — nama field-nya `note`, bukan `description`)",
  "letter_id": "string? (opsional)"
}
```
`reason: "empty_mail"` cuma valid kalau `target_type == "letter"` (`400 "empty_mail reason is only valid for target_type=letter"` kalau tidak). `target_type: "penpal_post"` tidak didukung.

```json
// Response 201 aktual
{
  "success": true,
  "data": { "report_id": "uuid" }
}
```
Tidak ada field `message` di response aktual — `CreateReportResponse` cuma punya `report_id`.

- **Anonimitas**: `reporter_id` disimpan di DB tapi **tidak pernah** dikirim ke endpoint admin GET (gunakan aggregate saja).
- **Dedup, bukan hard rate-limit**: request duplikat — sama `(reporter_id, target_type, target_id)` dalam rolling 24 jam — **tidak** ditolak, tapi di-short-circuit balikin `report_id` yang sudah ada tanpa insert baru (`ReportService.CreateReport`, idempotent). Beda dari framing "rate limit" di draft awal.
- **Validasi**: `target_id` **tidak** dicek eksis di tabel yang sesuai — cuma dicek non-blank. Existence check yang didraft di atas tidak diimplementasikan.

---

### 2. Admin: List Semua Laporan

**Auth:** `X-Admin-Key`  
**Path:** `GET /api/v1/admin/reports`

Query params:
| Param | Default | Deskripsi |
|---|---|---|
| `status` | — | Filter: `pending` / `reviewed` / `actioned` / `dismissed` |
| `reason` | — | Filter by reason |
| `target_type` | — | Filter: `letter` / `penpal_post` / `user` |
| `cursor` | — | Cursor pagination (opaque base64) |
| `limit` | 20 | Max 100 |

```json
// Response 200
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "reported_user": {
          "id": "string",
          "nickname": "string",
          "continent": "Asia",
          "photo_url": "string | null",
          "account_status": "active | suspended | banned",
          "total_reports_received": 3
        },
        "target_type": "letter",
        "target_id": "string",
        "body_snapshot": "string | null",
        "reason": "harassment",
        "status": "pending",
        "admin_note": "string | null",
        "reviewed_by": "string | null",
        "reviewed_at": "ISO8601 | null",
        "created_at": "ISO8601",
        "report_count_same_target": 2
      }
    ],
    "next_cursor": "string | null",
    "total_pending": 14
  }
}
```

> **Privacy**: `reporter_id` **tidak pernah** dimasukkan ke response ini. Admin hanya tahu siapa yang dilaporkan dan konten laporan.

---

### 3. Admin: Detail Satu Laporan

**Auth:** `X-Admin-Key`  
**Path:** `GET /api/v1/admin/reports/:id`

Response sama dengan item di list, ditambah field `related_reports` (laporan lain ke user/target yang sama dalam 30 hari terakhir):
```json
{
  "success": true,
  "data": {
    // ...same as list item...
    "related_reports": [
      { "id": "uuid", "reason": "spam", "created_at": "ISO8601", "status": "pending" }
    ]
  }
}
```

---

### 4. Admin: Update Status Laporan (Review / Dismiss)

**Auth:** `X-Admin-Key`  
**Path:** `PATCH /api/v1/admin/reports/:id`

```json
// Request Body
{
  "status": "reviewed | actioned | dismissed",
  "admin_note": "string? (max 1000 karakter)"
}
```

```json
// Response 200 (draft — beda dari aktual, lihat di bawah)
{
  "success": true,
  "data": { "id": "uuid", "status": "actioned", "reviewed_at": "ISO8601" }
}
```
**Aktual:** handler (`UpdateReportStatus`, `admin_handler.go:277-286`) balikin `response.OK(c, nil)` — body sukses cuma `{"success": true}`, tidak ada `data` sama sekali.

---

### 5. Admin: Suspend / Ban / Unsuspend User

**Auth:** `X-Admin-Key`

> **Path aktual beda dari draft.** Ada 3 route paralel di kode (`app/module/admin/admin.go:59-62`):
> - `POST /api/v1/admin/users/:id/suspend` — **legacy**, action-nya di-hardcode `"suspend"` server-side (`SuspendUserRequest{Action:"suspend"}`), body cuma `{reason, expires_at?, duration_days?}`.
> - `POST /api/v1/admin/users/:id/ban` — **legacy**, action di-hardcode `"ban"`, body cuma `{reason}`.
> - `POST /api/v1/admin/users/:uid/moderation-action` — **endpoint unified yang aktif dipakai**, ini yang sesuai draft di bawah (support `action: "suspend"|"ban"|"unsuspend"` dalam satu body). Pakai ini buat integrasi baru — dua di atas dipertahankan cuma buat backward-compat.

```json
// Request Body — POST /api/v1/admin/users/:uid/moderation-action
{
  "action": "suspend | ban | unsuspend",
  "reason": "string (wajib buat suspend/ban, muncul di notif ke user)",
  "expires_at": "ISO8601 | null",  // null = permanent ban / indefinite suspend
  "duration_days": "int? (alternatif expires_at, khusus suspend)"
}
```

```json
// Response 200
{
  "success": true,
  "data": {
    "uid": "string",
    "account_status": "suspended | banned | active",
    "suspended_at": "ISO8601 | null",
    "suspend_until": "ISO8601 | null"
  }
}
```
Field terakhir namanya **`suspend_until`**, bukan `suspend_expires_at` seperti draft awal (`ModerationActionResponse`, `app/module/admin/dto/admin_dto.go:139-145`).

**Side effects server (otomatis):**
1. Update `account_status` di tabel `users`.
2. Kirim **FCM push notification** ke device pengguna (kalau ada `fcm_token`) dengan pesan:
   > *"Akun Anda telah ditangguhkan karena pelanggaran Pedoman Komunitas. Hubungi dukungan jika ini adalah kekeliruan."*
3. **Revoke Firebase refresh tokens** pengguna (via Firebase Admin SDK `RevokeRefreshTokens(uid)`) agar sesi langsung expired.

---

### 6. Admin: Hapus Konten (Soft-delete Letter/Post)

**Auth:** `X-Admin-Key`  
**Path:** `DELETE /api/v1/admin/content/:type/:id`

`:type` = `letter` atau `penpal_post`

```json
// Response 200
{
  "success": true,
  "data": { "id": "string", "deleted_at": "ISO8601" }
}
```

**Side effects:**
1. Soft-delete record (set `deleted_at`, jangan hard delete).
2. Kirim **FCM push notification** ke `sender_id` konten:
   > *"Salah satu surat atau postingan kamu telah dihapus karena melanggar Pedoman Komunitas Penypost."*

---

### 7. Auth Middleware: Cek Account Status

Middleware `FirebaseAuth` yang sudah ada perlu diupdate untuk:
1. Setelah verifikasi token berhasil, query `account_status` dari tabel `users` berdasarkan `uid`.
2. Kalau `account_status == 'suspended'` atau `'banned'` → return `403`:
```json
{
  "success": false,
  "error": "account_suspended",
  "message": "Akun Anda telah ditangguhkan karena pelanggaran Pedoman Komunitas. Hubungi dukungan jika ini adalah kekeliruan."
}
```

---

## Event/Notifikasi Constants (Tambah di `app/constant/event.go`)

```go
const (
  // existing...
  EventLetterDelivered = "letter_delivered"
  EventNewLetter       = "new_letter"

  // NEW: Moderation events
  EventAccountSuspended   = "account_suspended"
  EventAccountBanned      = "account_banned"
  EventContentRemoved     = "content_removed"
  EventReportReceived     = "report_received"  // khusus internal, gak ke user
)
```

---

## Alur Moderasi End-to-End

```
[Mobile User]                  [Backend]                [Admin Dashboard]
     │                              │                           │
     │── POST /reports ────────────>│                           │
     │   (anonim, tersimpan)        │                           │
     │<─ 201 report_id ────────────│                           │
     │                              │                           │
     │                              │<── GET /admin/reports ────│
     │                              │── return list (no reporter_id) ──>│
     │                              │                           │
     │                              │<── PATCH /admin/reports/:id (actioned) + DELETE /admin/content/... ──│
     │                              │── soft-delete content     │
     │                              │── FCM: "konten dihapus" ──>│ (ke sender)
     │                              │                           │
     │                              │<── POST /admin/users/:uid/suspend ──│
     │                              │── update account_status   │
     │                              │── revoke Firebase tokens  │
     │                              │── FCM: "akun ditangguhkan"──>│ (ke user)
     │                              │                           │
     │── any API call ─────────────>│                           │
     │<─ 403 account_suspended ─────│                           │
```

---

## Web Admin Dashboard CMS (Frontend)

Path di dashboard: `/moderation`

**Tab 1 — Laporan Masuk (Pending)**
- Tabel laporan dengan kolom: Tanggal, Alasan, Konten ter-snapshot, Akun Terlapor, Jumlah Laporan, Aksi.
- Tombol aksi per baris: `Hapus Konten`, `Suspend User`, `Ban Permanen`, `Dismiss`.

**Tab 2 — Semua Laporan (History)**
- Filter by status, reason, date range.

**Tab 3 — Akun Tersuspend**
- List user dengan `account_status = suspended | banned`.
- Tombol `Unsuspend`.

---

*Moderation API Spec v1.0 · Penypost — Perlu diimplementasi di BE Unsealed Go sebelum fitur report diluncurkan.*
