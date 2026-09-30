# FE Handoff — Letter Send Flow Fix

> Backend sudah difix. Ada **satu hal yang perlu diubah di FE** supaya alur kirim surat jalan dengan benar.

---

## Root Cause

Backend sebelumnya **hardcode `recipient_type = "user"` untuk semua surat**. Efeknya: surat yang harusnya muncul di PenPals feed tidak pernah muncul karena feed query filter `WHERE recipient_type = 'penpal'`.

Backend sekarang sudah diperbaiki: backend **derive `recipient_type` dari field `visibility`** yang dikirim FE.

---

## Aturan Baru (sudah live di backend)

| `visibility` yang FE kirim | Yang backend lakukan | Surat muncul di |
|---|---|---|
| `"public"` | set `recipient_type = 'penpal'`, `estimated_arrival_at = now()` | `GET /penpals/feed` — **langsung** |
| `"private"` | set `recipient_type = 'user'`, `estimated_arrival_at = +6h / +24h` | `GET /letters/inbox` penerima — **dengan delay** |

FE **tidak perlu kirim `recipient_type`** — backend handle sendiri dari `visibility`.

---

## Dua Mode Kirim — Flow yang Benar

### Mode A: Kirim ke Penpal Feed (Global Post)

Pengirim pilih region → surat muncul di feed publik, bisa di-like + di-reply siapapun.

```
1. POST /api/v1/penpals/match
   Body: { "region": "Asia" }
   → dapat { "recipient_id": "...", "recipient_name": "..." }

2. POST /api/v1/letters
   Body: {
     "recipient_id": "<dari step 1>",
     "visibility": "public",      ← WAJIB "public"
     "dear_name": "...",
     "body_text": "...",
     "envelope": "ENVELOPE_1",
     "stamp": "MARIGOLD",
     ... field lainnya seperti biasa
   }
```

**Response:** `status: "IN_TRANSIT"` — tapi surat **langsung** muncul di `GET /penpals/feed` tanpa menunggu.

---

### Mode B: Kirim Surat Langsung ke Inbox Seseorang

Pengirim tahu siapa yang dituju (balas dari feed, atau dari address book).

```
POST /api/v1/letters
Body: {
  "recipient_id": "<uid orang yang dituju>",
  "visibility": "private",     ← WAJIB "private"
  "dear_name": "...",
  "body_text": "...",
  "envelope": "ENVELOPE_1",
  "stamp": "MARIGOLD",
  ... field lainnya seperti biasa
}
```

**Response:** `status: "IN_TRANSIT"`, `estimated_arrival_at` = +6 jam (same continent) atau +24 jam (cross continent). Surat masuk inbox penerima setelah waktu itu, **tidak muncul di feed**.

> 💡 Untuk reply dari feed: `recipient_id` = `sender_id` dari item feed yang di-tap. Tidak perlu panggil `POST /penpals/match` lagi.

---

## Bug di Request yang Dikirim Sebelumnya

```json
{
  "recipient_id": "seed_343c9253-decc-4485-862c-33821c1db69d",
  "visibility": "private",   ← ❌ harusnya "public" untuk Mode A
  ...
}
```

FE memanggil `POST /penpals/match` (benar untuk Mode A), tapi mengirim `visibility: "private"` sehingga surat masuk sebagai private letter ke inbox seed account — bukan ke feed.

---

## Checklist FE

- [ ] **Mode A (global post):** pastikan kirim `visibility: "public"` bukan `"private"`
- [ ] **Mode B (reply/direct):** kirim `visibility: "private"`, **jangan** panggil `POST /penpals/match`
- [ ] Tidak perlu kirim field `recipient_type` — backend handle otomatis
- [ ] Tidak ada perubahan shape request lainnya, semua field tetap sama

---

## Tidak Ada Perubahan di Endpoint Lain

| Endpoint | Status |
|---|---|
| `POST /api/v1/penpals/match` | Tidak berubah |
| `GET /api/v1/penpals/feed` | Tidak berubah |
| `GET /api/v1/letters/inbox` | Tidak berubah |
| `GET /api/v1/letters/{id}` | Tidak berubah |
| `POST /api/v1/letters` | **Shape request tidak berubah** — hanya pastikan `visibility` benar |
