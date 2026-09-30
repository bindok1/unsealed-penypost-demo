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
| `"private"` | set `recipient_type = 'user'`, `estimated_arrival_at = +24h (same continent/WALK) / +72h (cross continent/SWIM)` | `GET /letters/inbox` penerima — **dengan delay** |

FE **tidak perlu kirim `recipient_type`** — backend handle sendiri dari `visibility`.

---

## Dua Mode Kirim — Flow yang Benar

> ⚠️ **Update (2026-08-19, commit `bf8fbc9`):** section "Mode A" di bawah ini sempat salah sejak `recipient_id` untuk surat publik diwajibkan berformat literal `"penpal:<region>"` (lihat `docs/be/letters_api.md` §"Public penpal posts"). Versi lama Mode A (pakai `recipient_id` hasil `POST /penpals/match`) **tidak akan pernah cocok** dengan format itu dan akan selalu kena `400`. Sudah diperbaiki di bawah — dan sekaligus diluruskan: `POST /penpals/match` sebenarnya bukan untuk Mode A sama sekali, lihat catatan di Mode A yang baru.

### Mode A: Kirim ke Penpal Feed (Global Post)

Pengirim pilih region → surat muncul di feed publik, bisa di-like + di-reply siapapun. **Tidak perlu panggil `POST /penpals/match`** — `recipient_id` dibangun langsung dari region yang dipilih user, tanpa lookup ke satu user tertentu (post publik tidak punya recipient tunggal).

```
POST /api/v1/letters
Body: {
  "recipient_id": "penpal:Asia",   ← literal "penpal:<region>", BUKAN hasil /penpals/match
  "visibility": "public",          ← WAJIB "public"
  "dear_name": "...",              ← opsional untuk public, tidak ada penerima spesifik
  "body_text": "...",
  "envelope": "ENVELOPE_1",
  "stamp": "MARIGOLD",
  ... field lainnya seperti biasa
}
```

`<region>` harus salah satu dari 7 continent valid (case-sensitive): `Africa`, `Antarctica`, `Asia`, `Europe`, `North America`, `Oceania`, `South America`.

**Response:** `status: "IN_TRANSIT"` — tapi surat **langsung** muncul di `GET /penpals/feed` tanpa menunggu.

---

### Mode B: Kirim Surat Langsung ke Inbox Seseorang

Dua sub-kasus, keduanya pakai `visibility: "private"` dan `recipient_id` = UID orang yang dituju (bukan `"penpal:<region>"`):

**B1 — balas dari feed atau address book:** `recipient_id` = `sender_id` dari item feed yang di-tap, atau id kontak dari address book. Tidak perlu panggil endpoint lain.

**B2 — mulai obrolan baru dengan penpal yang di-random-kan dari suatu region:** ini yang sebenarnya makai `POST /penpals/match` (bukan Mode A) — endpoint ini balikin **satu UID user asli** hasil matching di region tersebut, dipakai sebagai `recipient_id` untuk surat **private**, bukan public.

```
1. POST /api/v1/penpals/match
   Body: { "region": "Asia" }
   → dapat { "recipient_id": "<uid asli>", "recipient_name": "..." }

2. POST /api/v1/letters
   Body: {
     "recipient_id": "<uid dari step 1>",
     "visibility": "private",     ← WAJIB "private", bukan "public"
     "dear_name": "...",
     "body_text": "...",
     "envelope": "ENVELOPE_1",
     "stamp": "MARIGOLD",
     ... field lainnya seperti biasa
   }
```

**Response (B1 & B2):** `status: "IN_TRANSIT"`, `estimated_arrival_at` = +24 jam (same continent / WALK) atau +72 jam (cross continent / SWIM). Surat masuk inbox penerima setelah waktu itu, **tidak muncul di feed**.

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

- [ ] **Mode A (global post):** kirim `recipient_id: "penpal:<region>"` (literal, bukan hasil `/penpals/match`) dan `visibility: "public"`
- [ ] **Mode B1 (reply/address book):** kirim `recipient_id` = UID orang yang dituju, `visibility: "private"`
- [ ] **Mode B2 (obrolan baru via region):** panggil `POST /penpals/match` dulu buat dapat UID, lalu kirim `visibility: "private"` (**bukan** `"public"`)
- [ ] Tidak perlu kirim field `recipient_type` — backend handle otomatis
- [ ] Tidak ada perubahan shape request lainnya, semua field tetap sama

---

## Status Endpoint Lain

| Endpoint | Status |
|---|---|
| `POST /api/v1/penpals/match` | Kontraknya sendiri tidak berubah — tapi pemakaiannya di flow ini pindah dari Mode A ke Mode B2, lihat di atas |
| `GET /api/v1/penpals/feed` | Tidak berubah |
| `GET /api/v1/letters/inbox` | Tidak berubah |
| `GET /api/v1/letters/{id}` | Tidak berubah |
| `POST /api/v1/letters` | Shape request tidak berubah, tapi **kontrak `recipient_id` untuk `visibility: "public"` berubah** sejak commit `bf8fbc9` (2026-08-19) — wajib `"penpal:<region>"`, lihat `docs/be/letters_api.md` |
