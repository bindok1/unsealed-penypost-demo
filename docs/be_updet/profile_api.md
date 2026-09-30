# Profile Enrichment API

User onboarding/profiling: interests, gender, birthday, spoken languages, profile photo, and bio.
All endpoints below are under `/api/v1` and require `Authorization: Bearer <firebase_id_token>` (same as every other route — see `app/router/router.go`).

---

## Auth Flow (Firebase — tidak ada /login endpoint di backend)

Backend tidak punya endpoint `/login`. Seluruh autentikasi dilakukan di client via Firebase Auth, backend hanya memverifikasi token.

1. **Firebase SDK** sudah terpasang via `google-services.json` (project `unsealed`).
2. **Google Sign-In** via `CredentialManager` + `GetGoogleIdOption` — lihat `AuthRepository.signInWithGoogle()`.
3. Setelah sign-in sukses, ambil ID token: `user.getIdToken()` — disimpan otomatis oleh `AuthInterceptor` dan dikirim ke setiap request sebagai `Authorization: Bearer <token>`.
4. `BASE_URL` diambil dari `BuildConfig.BASE_URL` — berbeda per flavor:
   - `dev`: `http://10.0.2.2:8080/` (emulator → localhost host machine)
   - `prod`: `https://api.unsealed.app/`
5. **Cek user baru**: `GET /api/v1/auth/me`
   - `200` → user lama → cek onboarding completeness → main app
   - `404` → user baru → arahkan ke `RegisterScreen` → `POST /api/v1/auth/register`
6. **Onboarding**: setelah register, jalankan flow multi-step (lihat endpoint di bawah).

---

## When to call this

Right after first login, the client already calls `POST /api/v1/auth/register` (unchanged — only `nickname` + `continent`). The profiling step below happens **after** that, as one or more follow-up calls — it does not need to happen in a single request. Build it as a multi-step onboarding flow (one screen per field, or one combined form) and call `PATCH /auth/me` / `PUT /auth/me/interests` / `PUT /auth/me/languages` whenever the user submits a step.

## Fresh user defaults

Immediately after `POST /auth/register`, before any profiling step runs, `GET /auth/me` returns:

```json
{
   "id": "firebase-uid",
   "nickname": "...",
   "continent": "...",
   "is_premium": false,
   "energy": 100,
   "gender": null,
   "birthday": null,
   "photo_url": null,
   "bio": "",
   "interests": [],
   "languages": [],
   "last_active_at": null,
   "tos_accepted_at": null,
   "notify_hour_pref": "morning",
   "utc_offset_minutes": 0
}
```
(`utc_offset_minutes` default `0` = UTC when omitted at register — see `POST /auth/register` below. `tos_accepted_at`/`last_active_at` are `null` until the client sends them / the user makes an authenticated call, matching `dto.UserResponse` in `app/module/auth/dto/auth_dto.go`.)

`energy` (int, default `100` — `db/migrations/018_user_energy.sql`) is the spendable balance for `POST /letters/{id}/unlock` (`docs/be/letters_api.md`). Private to the owner — deliberately **not** exposed on `GET /users/{id}` (public profile) below, same treatment as `gender`/`birthday`. How users top it up (daily claim, IAP, ...) is not decided yet.

**Note (2026-08-17):** there used to be a `language` field here (`"id"`/`"en"` only). It's **removed** — the backend never has a reason to know which UI locale the app is rendering in (that's a pure client-side i18n concern, string resources bundled in the APK, nothing to sync server-side). What actually needed persisting was "languages the user speaks, for pen pal matching" — see `PUT /auth/me/languages` below, which replaces it.

## `POST /api/v1/auth/register`

First-time registration. Call this once, right after Google Sign-In, when `GET /auth/me` returns 404.

Request body:
```json
{ "nickname": "matahari_senja", "continent": "Asia" }
```

| Field | Type | Rules |
|---|---|---|
| `nickname` | string | required, max 32 chars |
| `continent` | string | one of: Africa, Antarctica, Asia, Europe, North America, Oceania, South America |

Both rules are enforced server-side in `AuthService.Register` (`app/module/auth/service/auth_service.go`) — `400` on blank/`>32`-char nickname or an unrecognized continent. Same rules apply (as optional format checks, not required-ness) if `nickname`/`continent` are sent to `PATCH /auth/me` below.

Response `201`: full profile (same shape as `GET /auth/me`).

## `PATCH /api/v1/auth/me`

Partial update — send only the fields you want to change. All fields are optional.

Request body:

```json
{
   "nickname": "optional",
   "continent": "optional",
   "fcm_token": "optional",
   "gender": "m",
   "birthday": "2000-01-31",
   "photo_url": "https://.../key",
   "bio": "up to 300 characters"
}
```

| Field | Type | Rules |
|---|---|---|
| `gender` | string | `"m"` or `"f"` only |
| `birthday` | string | `"YYYY-MM-DD"`, cannot be a future date |
| `photo_url` | string | public URL of an already-uploaded photo (see upload flow below) |
| `bio` | string | max 300 **characters** (not bytes — emoji/diacritics count as 1 each) |

Spoken languages are **not** part of this endpoint — use `PUT /auth/me/languages` below.

**(Baru)** `notify_hour_pref` — `"morning" | "afternoon" | "evening"`, default `"morning"`. Preferensi jam untuk notifikasi "stack surat penpal baru" (fixed-UTC approximation, bukan timezone asli). Full detail (mapping jam, onboarding UX, payload notif): `daily_penpal_stack.md`.

Response `200`: full updated profile, same shape as `GET /auth/me` above.

Response `400` (validation failure), e.g.:
```json
{ "success": false, "error": "gender must be \"m\" or \"f\"" }
```

## Profile photo upload flow

There is no dedicated photo-upload endpoint — reuse the existing generic presign flow:

1. `POST /api/v1/storage/presign` with `{ "file_name": "...", "content_type": "image/jpeg" }` → returns `{ "upload_url", "public_url", "key" }`.
2. Client `PUT`s the raw file bytes directly to `upload_url` (goes straight to R2, not through this backend).
3. Client calls `PATCH /api/v1/auth/me` with `{ "photo_url": "<public_url from step 1>" }` to attach it to the profile.

## `GET /api/v1/interests`

Returns the full interest catalog (no auth-specific filtering — same list for everyone). Use this to render the multi-select UI.

Response `200`:
```json
{
   "success": true,
   "data": [
      { "id": "anime", "name": "Anime", "emoji": "🎌" },
      { "id": "cats", "name": "Cats", "emoji": "🐱" }
   ]
}
```

## `PUT /api/v1/auth/me/interests`

Replaces the user's **entire** set of selected interests in one call — this is not additive.

Request body:
```json
{ "interest_ids": ["anime", "cats", "crafting"] }
```

- Always send the **full** desired set. Omitting the field or sending `[]` clears all of the user's interests.
- Unknown `interest_id`s (not in the catalog) return `400`.

Response `200`: full updated profile (same shape as `GET /auth/me`), with `interests` reflecting the new set.

## `PUT /api/v1/auth/me/languages`

Replaces the user's **entire** set of spoken languages in one call — not additive, same full-replace semantics as `PUT /auth/me/interests`. This is "languages the user speaks, for pen pal matching" — **not** the app's UI locale, which the backend doesn't track at all (see note under "Fresh user defaults" above).

Request body:
```json
{ "languages": [{ "code": "id", "level": 5 }, { "code": "en", "level": 2 }] }
```

| Field | Type | Rules |
|---|---|---|
| `code` | string | required, non-blank. **Free-form** — not restricted to a curated catalog, any language the user types (`"fr"`, `"French"`, whatever the client's language picker sends) is accepted as-is. |
| `level` | int | required, `1`–`5` (1 = beginner, 5 = fluent/native — matches the level-dots UI in `ProfileScreen.kt`) |

- Always send the **full** desired set. Omitting the field or sending `[]` clears all of the user's languages.
- Duplicate `code`s in the same request (case-insensitive) return `400`.
- `level` outside `1`–`5` returns `400`.

Response `200`: full updated profile (same shape as `GET /auth/me`), with `languages` reflecting the new set.

## `GET /api/v1/users/{id}` — view another user's public profile

*Status: ✅ selesai (2026-08-12).* Dibangun buat nutup gap "View Profile" yang disebut di `docs/be/trust_and_safety_api.md` §1 — PenPals overlay's "···" dropdown sudah punya item View Profile sejak awal, tapi masih stub `ComingSoonBottomSheet` karena belum ada layar profil publik user lain sama sekali. Sekarang endpoint-nya sudah live, tinggal client-nya nyambungin.

Response `200` — subset field yang aman diekspos ke user lain (**bukan** shape yang sama dengan `GET /auth/me`, sengaja exclude `gender`/`birthday`/`fcm_token` yang privat dan `tos_accepted_at`/status moderasi yang internal):
```json
{
   "id": "uid_...", "nickname": "...", "continent": "Asia", "is_premium": false,
   "photo_url": "https://...", "bio": "...", "interests": ["reading", "coffee"],
   "languages": [{ "code": "id", "level": 5 }, { "code": "en", "level": 2 }],
   "is_online": false, "last_active_at": "2026-08-10T09:00:00Z"
}
```
- `is_online` / `last_active_at` — pola sama persis kayak `GET /penpals/feed`'s `is_online` (threshold 5 menit) dan `GET /auth/me`'s `last_active_at` (RFC3339 mentah, client yang format jadi relative time kayak "aktif 2 hari lalu").
- `404 {"error": "user not found"}` — baik kalau id-nya memang nggak ada, kalau akun target berstatus dihapus (`users.status == "deleted"`), **maupun** kalau ada relasi block (dua arah) antara viewer & target (lihat `docs/be/trust_and_safety_api.md` §2 & §4a) — sengaja disamain biar response-nya nggak bocorin status akun atau status block ke siapapun.

**Terkait:** `GET /api/v1/users/{id}/showcase` (surat & amplop publik yang ditampilkan di "Postal Collection" profile ini) didokumentasikan di `docs/be/letters_api.md` §"Postal Collection Showcase" — bukan di sini karena entity-nya letter, tapi path-nya satu namespace sama endpoint ini. (Catatan: untuk akun yang berstatus `deleted`, endpoint showcase mengembalikan array kosong `items: []` dengan `200 OK`, bukan `404`).

## `DELETE /api/v1/auth/me` — self-service delete account

*Status: ✅ selesai (2026-08-17).* Detail lengkap ada di `docs/be/trust_and_safety_api.md` §4a.
- Mengubah status akun menjadi `users.status = 'deleted'`.
- Request terautentikasi selanjutnya akan ditolak dengan `403 {"success": false, "error": "Akun ini sudah dihapus.", "meta": {"account_status": "deleted"}}`.
- Menyembunyikan profil publik dari `GET /users/{id}` (404), menolak surat baru ke user ini di `POST /letters` (400 "user not found"), dan mengosongkan showcase di `GET /users/{id}/showcase` (`items: []`).

Response sukses `200` (sebelumnya tidak didokumentasikan — `auth_handler.go:69-76`):
```json
{ "success": true, "data": { "account_status": "deleted" } }
```

## Roadmap (not built yet)

The following are planned as follow-up work after this profiling foundation lands, out of scope for now:
- Stamp collection (tracking which catalog stamps a user owns/has collected)
- Envelope customization
- Letter-based profiling (deriving interests/preferences from letter activity)
- Profile photo upload (presign flow UI — backend ready, client placeholder screen exists)

### ✅ resolved: `last_active_at`
Sempat ditandai gap (badge "🟢 Last active today" di `ProfileScreen.kt` masih hardcoded) — sudah **kebangun**: kolom `users.last_active_at` di-`UPDATE ... = now()` otomatis tiap request authenticated (`package/middleware/auth.go` — `checkModerationAndTouchActivity`, dijalanin dari `FirebaseAuth` middleware yang bungkus semua route `/api/v1/*`), bukan cuma pas hit `GET /auth/me`. Sudah diekspos di `GET /auth/me`, `GET /users/{id}`, dan `GET /penpals/feed`'s `is_online`.

### ✅ resolved: `language` (single field) vs UI "LANGUAGES" level-dots (jamak)
Sempat jadi pertanyaan terbuka: apakah `language` itu bahasa antarmuka app atau bahasa yang dikuasai user. **Keputusan produk (2026-08-17): yang kedua** — app nggak perlu tahu/nyimpen preferensi locale UI user di backend sama sekali (itu murni client-side, string resource yang di-bundle di APK). Field `language` lama (`users.language`, enum `"id"`/`"en"`) sudah **dihapus** (`db/migrations/019_user_languages.sql`), diganti tabel `user_languages(user_id, language_code, level)` + endpoint `PUT /auth/me/languages` di atas — bebas bahasa apa aja (bukan enum tertutup), mendukung banyak bahasa per user dengan level 1-5, sesuai section "LANGUAGES" level-dots yang ada di `ProfileWidgets.kt`'s `UnsealedProfileContent` (bukan `ProfileScreen.kt` — level-dots-nya live di widget, `ProfileScreen.kt` cuma pemanggil).

**Client sudah nyusul (2026-08-17):** `UserDto.language: String` di Android diganti `UserDto.languages: List<LanguageProficiencyDto>` (`AuthDto.kt`), `PatchMeRequest.language` dihapus total (settings language-picker di `ProfileSettingsScreen.kt` sekarang murni `AppCompatDelegate.setApplicationLocales(...)`, nggak lagi PATCH ke backend). Onboarding step 4 (`OnboardingLanguageBioScreen.kt`) sekarang manggil `PUT /auth/me/languages` beneran (dulu comma-joined hack lewat `PATCH /auth/me`'s `language` field). **Catatan:** picker onboarding-nya masih cuma milih bahasa, belum ada slider per-bahasa — semua bahasa yang dipilih dikirim dengan `level: 5` (fluent/native) hardcoded. Kalau mau level yang beneran akurat per bahasa, itu perlu UI baru di client, belum ada.
