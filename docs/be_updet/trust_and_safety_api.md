# Trust & Safety — Report / Block / TOS / Delete Account / Moderation

*Play Store compliance batch (UGC + messaging policy) — spec buat kerjaan yang belum dibangun. Endpoint di bawah masih rencana (belum ada implementasi backend), ditulis di sini dulu supaya kerjaan client bisa nyambung begitu backend-nya ada, sama pola kayak `docs/be/penpals_api.md` §`POST /penpals/match` ("FE duluan, backend nyusul").*

Semua endpoint di bawah `/api/v1` dan butuh `Authorization: Bearer <firebase_id_token>` seperti biasa, kecuali disebut lain.

---

## Status

| Bagian | Status | Catatan |
|---|---|---|
| Age Gate | ✅ selesai | `OnboardingGenderBirthdayScreen.kt` — `MinAgeYears = 13`, submit di-disable + inline error kalau di bawah umur, **Skip dihapus khusus di step ini** (step onboarding lain tetap skippable) biar age gate ga bisa dilewatin. Selaras sama jawaban kuisioner IARC yang harus diisi di Play Console (App Content → Target audience). |
| Pelaporan (Report) | 🟡 backend ✅, client PenPals ✅, client Inbox 🔜 | Lihat §1. **Backend selesai** (2026-08-12): `app/module/trust/**` — `POST /api/v1/reports`, idempoten 24 jam via `ReportRepository.FindRecentDuplicate`. **PenPals client selesai** (2026-08-12): `OpenLetterOverlay.kt`'s `SenderInfoBar` "···" sekarang `AnchoredDropdownMenu` beneran (Start Conversation/View Profile/Share Mail/Report Mail), Report Mail buka `ReportMailDialog` (`ui/components/`, reason list + note opsional) → `PenPalsViewModel.reportLetter` → `ReportsRepository`/`ReportsApi` → endpoint ini, sekarang sudah live. Sukses = optimistic client-side hide (surat hilang dari `_letters` feed reporter) + overlay ditutup. Inbox's `OpenLetterScreen.kt`'s `LetterMoreMenu` (`onReportUserClick`) masih stub ke `ComingSoonBottomSheet`. |
| Pemblokiran (Block) | 🟡 backend ✅, client 🔜 | Lihat §2. **Backend selesai** (2026-08-12): `POST/DELETE/GET /api/v1/blocks` + enforcement dua-arah di query (`GET /letters/inbox`, `GET /penpals/feed` exclude symmetric block; `POST /letters` private → 403 kalau ada block relationship). Titik masuk UI **sudah ada** — `LetterMoreMenu`'s `onBlockUserClick` (Inbox), stub yang sama, tinggal disambungkan ke endpoint ini. PenPals overlay belum ada tombol Block sama sekali (cuma Report via "···" generik) — perlu ditambah. |
| TOS/EULA persetujuan | 🟡 backend ✅, client 🔜 | Lihat §3. **Backend selesai** (2026-08-12): `tos_accepted_at` di `PATCH /api/v1/auth/me` (`UpdateProfileRequest`, bukan `PatchMeRequest` — nama di spec ini keliru, tipe asli di kode `UpdateProfileRequest`), server-side gate di `POST /letters` (private) dan `POST /penpals/match` → 403 kalau `tos_accepted_at` null. Belum ada checkbox di client — `RegisterScreen.kt` (auth) dan step onboarding pertama sama-sama belum nanya persetujuan. |
| Hapus Akun (in-app, self-service) | ✅ selesai | Lihat §4a. **Backend selesai** (2026-08-17): `DELETE /api/v1/auth/me` — set `users.status = 'deleted'`, digate lewat mekanisme suspend/ban yang sudah ada di `package/middleware/auth.go` (request authenticated apapun sesudahnya, termasuk `GET /auth/me` pas "coba login lagi", langsung `403` dengan pesan jelas + `meta.account_status`). Firebase refresh token di-revoke best-effort. Client: belum disambungkan. |
| Tautan Hapus Akun (web) | 🔜 belum dibangun — **di luar scope** | Lihat §4b. Ini halaman publik di luar app (nggak butuh install), buat Play Store compliance yang mewajibkan opsi hapus akun **tanpa perlu install app** — beda dari §4a di atas yang cuma bisa dipakai user yang masih bisa buka app. Masih di luar scope kerjaan Android maupun backend Fiber batch ini, diputuskan untuk di-skip di batch trust & safety ini. |
| Moderasi backend (suspend/ban) | ✅ selesai | Lihat §5. **Backend selesai** (2026-08-12): `POST /api/v1/admin/users/:id/suspend` (`duration_days` null = indefinite) dan `/ban` (`app/module/admin/**`, kolom `users.status`/`suspended_until`/`moderation_reason`), plus review queue `GET/PATCH /api/v1/admin/reports`. Middleware `FirebaseAuth` (`package/middleware/auth.go`) sekarang nolak 403 semua request dari user `suspended`/`banned` (fail-open kalau DB error). |

---

## 1. Pelaporan (Report)

`POST /api/v1/reports`

Request:
```json
{
  "target_type": "user",
  "target_id": "firebase-uid-or-seed-id",
  "reason": "harassment",
  "letter_id": null,
  "note": "opsional, teks bebas dari pelapor"
}
```

| Field | Tipe | Catatan |
|---|---|---|
| `target_type` | enum | `"user"` atau `"letter"` — laporan bisa soal profil orangnya, atau satu surat spesifik |
| `target_id` | string | `sender_id` (kalau `target_type=user`) atau `letter_id` (kalau `target_type=letter`) |
| `reason` | enum | `harassment`, `spam`, `inappropriate_content`, `underage`, `impersonation`, `other`, `empty_mail` — daftar awal, gampang nambah tanpa breaking change (client render sebagai list pilihan, `other` munculin field teks). `empty_mail` khusus letter-report (`target_type=letter`) — surat kosong/isinya cuma spasi, ga relevan buat `target_type=user`. |
| `letter_id` | string? | isi kalau laporan dipicu dari dalam surat tertentu (`target_type=letter` **atau** `target_type=user` yang originnya dari surat spesifik) — biar reviewer punya konteks langsung, nggak cuma "user X dilaporkan" tanpa tau surat mana |
| `note` | string? | opsional, dari `TextField` bebas di bottom sheet report |

Response `201` (bukan `200` — handler pakai `response.Created`, `report_handler.go:32`):
```json
{ "success": true, "data": { "report_id": "report_xyz789" } }
```

Idempoten per `(reporter_id, target_type, target_id)` dalam 24 jam terakhir — reviewer nggak perlu liat laporan duplikat kalau user spam-tap tombol report yang sama.

**Client-side:**
- ✅ **PenPals selesai** (`OpenLetterOverlay.kt` + `ui/components/ReportMailDialog.kt`, 2026-08-12) — "···" di `SenderInfoBar` sekarang dropdown beneran (`AnchoredDropdownMenu`) dengan 4 item: Start Conversation, View Profile (backend-nya sekarang sudah live — `GET /api/v1/users/{id}`, lihat `docs/be/profile_api.md` — client masih `ComingSoonBottomSheet`, tinggal disambungkan), Share Mail (native `Intent.ACTION_SEND`, ga butuh backend), Report Mail (`ReportMailDialog` — 4 alasan yang dipakai di sini: `spam`/`harassment`/`inappropriate_content`/`empty_mail`, plus catatan opsional). `ReportsApi`/`ReportsRepository` baru di `feature/reports/data/` mirror pola `PenpalsRepository`. `PenPalsViewModel.reportLetter` filter surat yang dilaporkan keluar dari `_letters` begitu server bilang sukses (optimistic client-side hide, pola yang sama kayak `toggleLike`'s optimistic update tapi buat penghapusan). String baru (`report_reason_*`, `report_mail_*`, `reply_confirm_dialog_*`, `open_letter_menu_*`) — EN & ID, ikutin aturan `docs/string-resources.md`.
- 🔜 Ganti `ComingSoonBottomSheet` di `LetterMoreMenu`'s `onReportUserClick` (`OpenLetterScreen.kt`, Inbox) jadi `ReportMailDialog` yang sama (butuh reason list lebih lengkap — `target_type=user` juga butuh `underage`/`impersonation`/`other` yang PenPals-nya sengaja ga expose) → panggil endpoint ini.
- Block belum ada entry point sama sekali di kedua tempat (lihat §2) — di luar scope batch PenPals ini.

---

## 2. Pemblokiran (Block)

`POST /api/v1/blocks`
```json
{ "blocked_user_id": "firebase-uid-or-seed-id" }
```
Response `201` (bukan `200` — handler pakai `response.Created`, `block_handler.go:32`): `{ "success": true, "data": { "blocked_user_id": "..." } }`

`DELETE /api/v1/blocks/{blocked_user_id}` — unblock. Response `200` no body.

`GET /api/v1/blocks` — list blocked users (buat halaman "Blocked Users" di Settings):
```json
{ "success": true, "data": { "items": [ { "user_id": "...", "user_name": "...", "blocked_at": "2026-08-11T09:00:00Z" } ] } }
```

**Efek blokir (dua arah, backend yang harus enforce, bukan cuma client filter):**
- `GET /penpals/feed` dan `GET /letters/inbox` **tidak** boleh nampilin konten dari user yang di-blok (baik arah block maupun di-blok — symmetric hide, standar block behavior).
- `POST /letters` (kirim surat) ke `recipient_id` yang lagi blocked-relationship (dua arah) → `403`.
- Client **jangan** cuma filter list di sisi app (gampang di-bypass, dan tetep nge-fetch data orang yang harusnya nggak keliatan) — filtering harus di query backend.

**Client-side (belum dikerjakan):**
- `LetterMoreMenu`'s `onBlockUserClick` (`OpenLetterScreen.kt`) — sekarang stub, perlu confirm dialog ("Blokir {nama}? Kalian nggak akan bisa saling kirim surat atau lihat profil.") → panggil `POST /blocks`.
- Halaman baru "Blocked Users" di Profile → Settings (`ProfileSettingsScreen.kt` sudah punya slot `LetterlyMenuRow` buat item baru — natural tempatnya di situ, sebelahan sama Privacy Policy row yang juga masih stub) — list dari `GET /blocks` + tombol Unblock per-row.
- PenPals: dropdown "···" udah ada sekarang (lihat §1, dibangun buat Report), tapi belum ada item "Block" di dalamnya — perlu ditambah sebagai item ke-5 begitu endpoint ini live.

---

## 3. Persetujuan TOS/EULA

Bukan endpoint baru — nambah field ke `PATCH /api/v1/auth/me` yang udah ada (`PatchMeRequest`, dipanggil dari `OnboardingGenderBirthdayScreen.kt` §langkah 1):

```json
{ "tos_accepted_at": "2026-08-11T09:00:00Z" }
```

Backend nolak `POST /letters` (dan idealnya juga `POST /penpals/match`) kalau `tos_accepted_at` null di user profile — bukan cuma UI gate, harus dicek server-side juga (checkbox doang di client gampang di-bypass kalau nggak divalidasi ulang di backend).

**Client-side (belum dikerjakan):**
- Checkbox "Saya menyetujui [Ketentuan Layanan] & [Kebijakan Privasi]" (dua link, buka WebView/Custom Tab ke halaman TOS/Privacy — dua-duanya juga belum ada URL publiknya, sama kayak §4b) — taruh di `RegisterScreen.kt` (paling awal, sebelum akun kebuat) **atau** step pertama onboarding (`OnboardingGenderBirthdayScreen.kt`, bareng age gate) — dua-duanya valid, tinggal pilih satu titik biar nggak dobel tanya. Kalau ditaruh bareng age gate, `PatchMeRequest` di step itu tinggal nambahin `tos_accepted_at` bareng `gender`/`birthday`.
- Submit (Register atau step onboarding itu) di-disable sampai checkbox dicentang — pola yang sama kayak age-gate CTA disable yang udah ada sekarang.

---

## 4a. Hapus Akun (In-App, Self-Service)

*Status: ✅ dibangun (2026-08-17).* Ini beda dari §4b di bawah — §4b itu halaman **web publik** buat orang yang **nggak bisa/nggak mau install app** (syarat Play Store yang lebih ketat). Ini versi simpelnya: user yang lagi login di app, sadar mau hapus akun, tap tombol di Settings.

`DELETE /api/v1/auth/me`

No body. Response `200`:
```json
{ "success": true, "data": { "account_status": "deleted" } }
```

Efek:
- `users.status` di-set ke `"deleted"` (reason `"self-deleted"` di `moderation_reason`, buat bedain dari admin suspend/ban di kolom status yang sama).
- **Reuse langsung** mekanisme suspend/ban yang sudah ada di `package/middleware/auth.go` (`checkModerationAndTouchActivity`) — begitu status jadi `"deleted"`, **request authenticated apapun sesudahnya** (bukan cuma login flow) otomatis kena `403`:
  ```json
  { "success": false, "error": "Akun ini sudah dihapus.", "meta": { "account_status": "deleted" } }
  ```
  Ini yang bikin skenario "user delete akun → FE lempar ke login screen → user coba login lagi (Firebase sign-in-nya sendiri masih sukses karena kita nggak hapus akun Firebase-nya) → app manggil `GET /auth/me` seperti biasa → kena block di sini dengan pesan jelas" jalan otomatis, tanpa endpoint/logic baru di sisi client-detection. `meta.account_status` disediakan biar client bisa branch (misal ke layar "Akun ini sudah dihapus" yang beda dari layar suspended/banned) tanpa string-match pesan yang di-localize.
- Firebase refresh token di-revoke best-effort (`firebaseAuth.RevokeRefreshTokens`, sama pola kayak admin suspend/ban di `admin_service.go`) — bukan yang utama nge-block (gate di atas yang utama, jalan di request pertama walau token lama masih teknis valid), tapi mempercepat expiry sesi yang lagi aktif.
- **Dampak ke endpoint publik & interaksi pengguna lain:**
  - `GET /api/v1/users/{id}` (Public Profile) → `404 {"error": "user not found"}` (sama persis seperti non-existent user atau relasi block dua arah, tidak membocorkan status akun).
  - `POST /api/v1/letters` (Kirim Surat) → `400 {"error": "user not found"}` jika ditujukan ke `recipient_id` yang statusnya `deleted` (mencegah buang-buang waktu/resource mengirim ke akun yang tidak bisa diakses lagi). Sender yang berstatus `deleted` juga di-reject sebagai defense-in-depth.
  - `GET /api/v1/users/{id}/showcase` (Showcase) → `200` dengan `{ "items": [], "next_cursor": null }` (array kosong, bukan 404, konsisten dengan filosofi list endpoint).

**Sengaja minimal scope — yang TIDAK dilakukan endpoint ini:**
- **Tidak** hard-delete row `users` atau data terkait (`letters`, `user_stamps`, dst) — surat yang sudah dikirim/diterima tetap ada apa adanya (termasuk `sender_nickname` yang ke-denormalize di tiap `letters` row, jadi nickname lama tetap kelihatan di surat lama, nggak ikut ke-scrub).
- **Tidak** menghapus akun Firebase Auth itu sendiri — cuma DB row kita yang ditandai deleted. User secara teknis masih bisa sign-in ke Firebase, tapi diblok di layer aplikasi (lihat gate di atas).
- **Tidak** re-usable id — kalau user daftar ulang, Firebase kasih UID baru (akun Google/Firebase lama nggak reset), jadi nggak ada risiko "akun deleted ke-reuse otomatis".

Kalau nanti butuh full data erasure (buat kepatuhan yang lebih ketat), itu kerjaan terpisah — lihat §4b buat versi yang eksplisit soal apa yang dihapus/disimpan.

**Client-side (belum dikerjakan):** row baru "Delete Account" di `ProfileSettingsScreen.kt`, dengan confirm dialog yang jelas ("Semua surat & profil kamu tetap tersimpan tapi akun ini nggak bisa dipakai lagi" atau copy final terserah produk) → panggil endpoint ini → sukses → clear local session/token → navigate ke Login screen. Kalau habis itu user coba login lagi, `GET /auth/me` bakal balikin `403` dengan `meta.account_status: "deleted"` seperti di atas — client tinggal tangkep itu dan tampilin pesan yang sesuai (jangan treat kayak error generik/network error).

---

## 4b. Tautan Hapus Akun (Web Delete Account Link)

**Di luar scope kerjaan Android app** — ini halaman publik (`https://domain-kamu.com/delete-account` atau semacamnya) yang Play Store wajibkan buat app apa pun yang punya akun user, **terlepas dari app-nya diinstall atau enggak**. Nggak bisa di-deploy dari sesi ini (nggak ada akses domain/hosting kamu) — dicatat di sini biar nggak lupa pas lanjut besok.

**Yang dibutuhkan:**
1. **Halaman itu sendiri** — form/instruksi buat submit permintaan hapus akun (biasanya: email + alasan opsional, atau login-lite kalau mau otomatis). Kontennya perlu jelasin: apa yang dihapus (profil, surat terkirim/diterima, foto), berapa lama prosesnya, ada data yang tetap disimpan untuk keperluan legal/anti-fraud atau nggak.
2. **Endpoint pendukung** (kalau mau otomatis, bukan email manual) — `POST /api/v1/account/delete-request` (public, no-auth kalau formnya cuma email; atau butuh auth kalau ada login-lite di halaman itu) yang masuk ke antrian moderasi/admin buat diproses.
3. **URL-nya sendiri** perlu ditaruh di Play Console (Data Safety section) dan idealnya juga ada link-out dari dalam app (`ProfileSettingsScreen.kt` → row baru "Delete Account", buka browser eksternal ke URL itu — sama pola kayak link TOS/Privacy di §3).

**Belum diputuskan (nunggu kamu lanjut):** bentuk deliverable-nya — HTML statis siap-deploy, atau cukup copy/instruksi doang, atau endpoint dulu baru halamannya nyusul. Placeholder di sini sampai keputusan itu dibuat.

---

## 5. Mekanisme Moderasi Backend (suspend/ban)

Bergantung ke §1 (Report) — laporan yang masuk perlu alur buat ditindaklanjuti:

- **Antrian review** — tabel `reports` (dari §1) + status (`pending`/`reviewed`/`actioned`/`dismissed`), kemungkinan besar butuh admin dashboard sederhana (di luar scope app Android — kemungkinan internal tool terpisah, bukan bagian dari app ini).
- `POST /api/v1/admin/users/{id}/suspend` — suspend sementara (user nggak bisa login/kirim surat, tapi akun & data tetap ada). Request: `{ "reason": "...", "duration_days": 7 }` (null = indefinite sampai di-review ulang).
- `POST /api/v1/admin/users/{id}/ban` — permanent, biasanya dipicu manual dari review queue setelah beberapa report/pelanggaran berat.
- Auth middleware (semua endpoint biasa, bukan cuma admin) perlu nolak request dari user yang lagi `suspended`/`banned` — `403` dengan pesan yang jelas, bukan generic error.
- **MVP yang cukup buat mulai:** nggak perlu bot/auto-moderation dulu — manual review dari report queue udah cukup buat batch pertama, sama prinsipnya kayak `docs/be/penpals_api.md`'s `POST /penpals/match` MVP note ("implementasi sederhana dulu, kompleksitas nyusul kalau perlu").

---

## Ringkasan kerjaan tersisa (buat lanjut besok)

- [x] Age Gate (client, `OnboardingGenderBirthdayScreen.kt`)
- [~] §1 Report — **backend selesai** (`app/module/trust/**`, 2026-08-12); client selesai buat PenPals (`OpenLetterOverlay.kt` + `ReportMailDialog.kt` + `ReportsRepository`, 2026-08-12); Inbox (`OpenLetterScreen.kt`) masih stub
- [~] §2 Block — **backend selesai** (`app/module/trust/**` + enforcement di letter/penpal repo, 2026-08-12); client confirm dialog + item dropdown PenPals + halaman "Blocked Users" di Settings masih belum
- [~] §3 TOS/EULA — **backend selesai** (`tos_accepted_at` di `UpdateProfileRequest`/`PATCH /auth/me` + gate di `POST /letters` & `POST /penpals/match`, 2026-08-12); checkbox client (Register atau onboarding step 1) + link ke halaman TOS/Privacy (yang juga belum ada URL-nya) masih belum
- [x] §4a Hapus Akun (in-app, self-service) — **backend selesai** (`DELETE /api/v1/auth/me`, `app/module/auth/**` + gate di `package/middleware/auth.go`, 2026-08-17); client (row "Delete Account" di Settings) belum
- [ ] §4b Tautan Hapus Akun (web) — **di-skip dari batch ini**, di luar scope backend Fiber
- [x] §5 Moderasi backend — selesai (`app/module/admin/**` suspend/ban + review queue reports + gate di `package/middleware/auth.go`, 2026-08-12)
