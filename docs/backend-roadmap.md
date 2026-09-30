# Unsealed — Backend Roadmap

*Source of truth "sudah sampai mana backend-nya" — untuk fitur, lihat `docs/todo.md` (yang menunjuk ke `compose-screen-spec.md` §"Status Implementasi"). Update dokumen ini tiap milestone berubah status.*

> Backend Go/PostgreSQL (Railway) ada di **repo terpisah** dari project Android ini (`app/router/router.go` dst., lihat `docs/architecture.md`). Dokumen-dokumen di `docs/be/` adalah spesifikasi untuk dikerjakan di repo backend itu.

---

## Status Audit (per Agustus 2026)

**Sudah live & tersambung penuh ke Android:**
- Auth (Firebase ID token verification, tidak ada `/login` sendiri)
- Profile (`GET/PATCH /auth/me`, `POST /auth/register`)
- Interests (`GET /interests`, `PUT /auth/me/interests`)
- Photo upload (generic presign flow `POST /storage/presign` → R2)
- **Letters Core** (`POST /letters`, `GET /letters/inbox`, `GET /letters/{id}`) — lihat detail di bawah, "Letters — wiring pertama" (2026-08-02)

Kontrak lengkap semua endpoint yang sudah live (bukan cuma draft): `docs/be/api_contract.md`. Detail rationale per-milestone tetap di `docs/be/profile_api.md`, `letters_api.md`, dst.

**Letters — wiring pertama (2026-08-02):** backend Go real sudah di-push ke dev, client Android mulai diwire — bukan cuma spec lagi:
- `feature/letters/data/` (`LettersApi`/`LettersDto`/`LettersRepository`) dibuat, ikut pola `feature/auth/data/`.
- **Inbox (list + detail) pakai data real** — `InboxViewModel`/`OpenLetterViewModel` manggil `GET /letters/inbox`/`GET /letters/{id}`, `dummyInboxItems` sudah dihapus dari codebase.
- **Kirim surat jalan untuk jalur reply saja** — Open Letter → Write (`write/reply/{id}/{name}`) → Select Recipient (`select_recipient/reply/{id}/{name}`) → `POST /letters` beneran, `recipient_id` diambil dari `sender_id` surat yang dibales. End-to-end functional.
- **Jalur "pilih region baru" (`SelectRecipientScreen` region-picker) masih mock** — belum ada endpoint yang bisa resolve pilihan region jadi `recipient_id` konkret; nunggu M3 (PenPals feed, reply ke `sender_id` feed item) atau M5 (Address Book/Add-Friend). Lihat §"Gap yang ditemukan" di bawah kalau mau history keputusannya.
- `composite_image_url` selalu dikirim `null` untuk sekarang — compositing pipeline (`docs/todo.md` #2, flatten teks+annotate+gambar jadi satu PNG) belum dibangun, jadi baru surat plain-text yang bisa dikirim.
- Arsitektur baru yang diperkenalkan buat nutup gap ini: `core/data/ComposeDraftHolder.kt` — singleton in-memory yang jembatani `body_text`/`paper_template`/`font_id`/`paper_color` dari `ComposeViewModel` (Write tab) ke `SelectRecipientViewModel` (Send), karena dua screen itu memang gak pernah share ViewModel di codebase ini (pertama kalinya kebutuhan lintas-screen kayak gini muncul). Stepping-stone alami ke arah Draft Auto-Save (`docs/todo.md` #5, Room-backed) — in-memory sekarang, tinggal ganti backing store-nya nanti.

**Belum ada backend sama sekali — 100% dummy/in-memory di client:**
- PenPals feed (`PenpalLetter`, 20 dummy item)
- Address Book, Stamp Book (dummy list di sub-screen Profile)
- Stamps Store / RevenueCat (disengaja ditunda, seam UI siap)
- Push Notifications (field `fcm_token` sudah ada di schema, belum ada trigger/worker)
- Sticker Catalog — `StickerInstance` stub sudah ada di client, belum dirender/belum ada picker; lihat `docs/be/stickers_api.md` (M8, independen dari RevenueCat)

**✅ Kontradiksi lama sudah diselesaikan:** `compose-screen-spec.md` §10 dan `docs/todo.md` #3 dulu masih menyebut "Firebase Storage + Firestore" sebagai rencana penyimpanan surat — itu asumsi lama sebelum backend Go/Postgres/Railway ada. Sudah di-supersede: body surat & metadata sekarang beneran masuk Postgres lewat `LettersRepository`, gambar composite (kalau ada) tetap lewat presign→R2 yang sudah ada, bukan Firestore/Firebase Storage. `docs/todo.md` #3 sudah diupdate mengikuti ini.

---

## Milestone Table

| # | Milestone | Spec | Endpoint inti | Status |
|---|---|---|---|---|
| M0 | Envelope & Error Standard | (bagian dokumen ini, di bawah) | `{success,data}` / `{success,error:string}` | ✅ Selesai — live di backend + `core/network/ApiResponse.kt` generic wrapper sudah dipakai `AuthApi`/`LettersApi` |
| M1 | Auth/Profile — tutup gap | `docs/be/profile_api.md` (§ Follow-up gaps) | tambah `last_active_at`; putuskan arah `language` vs "LANGUAGES" level-dots | 🔜 Perlu keputusan produk dulu sebelum dikerjakan |
| M2 | **Seed/Admin Account** (jawaban chicken-egg) | `docs/be/seed_accounts_api.md` | `POST/PATCH/GET/DELETE /api/v1/admin/seed-users`, `POST .../letters`, `POST .../penpal-posts` | ✅ Live di backend; 10 seed persona + ~11 penpal post + 2 surat masuk DB Railway (2026-08-02) |
| M3 | PenPal Feed | `docs/be/penpals_api.md` | `GET /penpals/feed`, `POST /penpals/feed/{id}/like`, `GET /penpals/regions` | ✅ Backend live; Android client diwire — feed real data, optimistic like toggle, Reply → Compose flow, region counts live di RegionPickerSheet, PENPAL_REGION send pakai real recipient_id (2026-08-02) |
| M4 | Letters/Messaging Core | `docs/be/letters_api.md`, `docs/be/api_contract.md` | `POST /letters` (hitung `estimated_arrival_at`), `GET /letters/inbox`, `/letters/sent`, `/letters/{id}` | 🔶 Backend live di dev; client: Inbox (list+detail) ✅ real data, kirim surat ✅ jalur reply saja, jalur region-picker/Sent-tab UI 🔜 belum |
| M5 | Address Book | `docs/be/address_book_api.md` | CRUD kontak via `penpal_user_id` | 🔜 Belum dibangun |
| M6 | Stamps & Store | `docs/be/stamps_api.md` | katalog stamp/paper, webhook RevenueCat | 🔜 Sengaja ditunda (lihat `todo.md`) |
| M7 | Push Notifications | `docs/be/notifications_api.md` | worker sweep `IN_TRANSIT`→`DELIVERED`, kirim FCM | 🔜 Belum dibangun, perlu FCM SDK di Android juga |
| M8 | Sticker Catalog | `docs/be/stickers_api.md` | `GET /stickers`, validasi premium di `POST /letters` | 🔜 Belum dibangun — **independen dari M6**, gate premium cukup `is_premium` boolean (bukan RevenueCat), bisa dikerjakan duluan |

**Urutan dependency:** M0 selesai → M1 butuh keputusan produk dulu tapi tidak blocking milestone lain → **M2 dan M3 bisa paralel** (M2 = akun seed, M3 = tempat kontennya muncul + jalur kirim-ke-orang-baru) → M5/M6/M7 masing-masing bergantung M4 (letters harus ada dulu sebelum address-book-dari-surat, notifikasi-surat-tiba, dst). **M8 cuma bergantung M4** (butuh `POST /letters` buat validasi `sticker_data`, sudah live) — tidak perlu nunggu M6.

**Rekomendasi immediate next**: lanjut **M2 (seed accounts)** dan **M3 (PenPals feed)** di backend — M4 (Letters Core) sisi client sekarang sudah cukup buat jalur reply, tapi app masih butuh cara kirim surat ke orang **baru** (bukan cuma balas), dan itu nunggu M2 (biar ada siapa yang dibalas dari awal) + M3 (biar ada UI buat mulai percakapan baru).

---

## M0 — Envelope & Error Standard

Kontrak response yang **sudah live** di backend (dikonfirmasi dari `profile_api.md`):

```json
// sukses
{ "success": true, "data": { ... } }

// gagal
{ "success": false, "error": "human-readable message string" }
```

Ini jadi standar wajib untuk **semua** endpoint baru di M1–M7 — jangan bikin bentuk envelope baru per fitur. Error tetap string tunggal (bukan object `{code, message}`) supaya konsisten dengan yang sudah di-ship, bukan breaking change.

Sisi Android: generic `ApiResponse<T>` (`core/network/ApiResponse.kt`) + helper `.unwrap()` sudah dibuat, menggantikan duplikasi `UserResponse`/`InterestsResponse` ad-hoc — dipakai `AuthApi` dan `LettersApi` (`feature/letters/data/`).

---

## Cara pakai dokumen ini

Tiap kali sebuah milestone mulai/selesai dikerjakan di backend, update kolom **Status** di tabel di atas (🔜 belum mulai / 🔶 sebagian / ✅ selesai) dan tanggal di footer.

*Update terakhir: 2026-08-04 — M8 (Sticker Catalog) ditambahkan, spec di `docs/be/stickers_api.md`; belum dibangun, independen dari M6/RevenueCat. Sebelumnya: 2026-08-02 — M2 (seed data) ✅ live di Railway; M3 (PenPals feed) ✅ backend live + Android diwire: feed real data, optimistic like toggle, Reply → Compose flow, live region counts di RegionPickerSheet, PENPAL_REGION send path pakai real recipient_id dari /penpals/feed. Sebelumnya: M0 selesai; M4 Letters Core mulai diwire di client.*
