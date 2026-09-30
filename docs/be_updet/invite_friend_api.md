# Invite Friend API

**Status: BE implemented (2026-08-31).** Endpoint §2 + migrasi §4 udah masuk (`app/module/invite/**`, `db/migrations/030_invites.sql`) — Opsi A (§3) dipakai (draft surat murni client-side, gak ada tabel baru). Bagian Android (§5) belum dikerjain — itu di luar scope repo BE ini. Sisanya di bawah tetap dipertahankan sebagai referensi desain buat fitur growth "invite teman → begitu dia daftar, langsung connect + surat yang udah disiapin kekirim otomatis". Tujuannya naikin jumlah installer lewat referral, bukan cuma nambah kontak.

Berbeda dari `address_book_api.md` (yang butuh `penpal_user_id` ke user **yang sudah ada**) dan `penpals_api.md` `POST /penpals/match` (random match ke user **yang sudah ada**) — invite ini satu-satunya alur di app yang ngalamatin surat ke orang yang **belum jadi user**.

Semua endpoint di bawah `/api/v1`, butuh `Authorization: Bearer <firebase_id_token>` kecuali disebut lain.

---

## 1. Kenapa gak bisa full-frontend

Dua alasan wajib backend:

1. **Attribution lintas-install.** Deep link (App Link) cuma nyala kalau app-nya udah ke-install. Buat kasus "belum punya app", satu-satunya cara nyambungin klik-link → install → buka-app-pertama-kali adalah **Play Install Referrer API** — string referrer yang nempel pas user klik link Play Store, dibaca ulang sama app begitu pertama dibuka. Kode invite disisipkan ke referrer string ini, jadi harus ada endpoint buat resolve `code → inviter_user_id`.
2. **Race/abuse control.** Redeem cuma boleh sekali per user baru, gak boleh self-invite, dan connect address-book harus dilakuin server-side biar konsisten sama constraint `chk_address_book_not_self` / `uq_address_book_user_penpal` yang udah ada di `address_book_entries`.

---

## 2. Endpoints

### `POST /api/v1/invites`

Generate invite code buat viewer yang login.

Response `201`:
```json
{
  "success": true,
  "data": {
    "code": "K7XQ2A",
    "deep_link_url": "https://peny-dashboard-xi.vercel.app/invite/K7XQ2A"
  }
}
```
*Domain:* pakai Vercel project yang udah ada (`peny-dashboard-xi.vercel.app`, sama domain yang di-pakai admin dashboard — lihat `admin_dashboard_web.md`), gak perlu beli domain custom, lihat §5.1. Kalau mau pisah dari admin tool secara branding/separation, boleh juga bikin project Vercel baru khusus public-facing — pilihan implementasi, gak ngubah kontrak endpoint ini.
- `code`: short unique token (6-8 char, alfanumerik, gampang diketik manual sebagai fallback kalau link putus).
- Satu user boleh punya banyak `invites` row aktif sekaligus (tiap share = code baru, atau reuse code yang belum redeemed — pilihan implementasi, gak ngubah kontrak).

### `POST /api/v1/invites/{code}/redeem`

Dipanggil sekali oleh user **baru** abis selesai signup, bawa `code` yang udah di-extract dari Install Referrer (app belum ke-install) atau dari App Link intent (app udah ke-install).

Response `200`:
```json
{
  "success": true,
  "data": {
    "inviter_user_id": "usr_abc123",
    "inviter_name": "kirana_senja"
  }
}
```
Efek samping saat sukses:
- Bikin `address_book_entries` **dua arah** (inviter↔invitee), reuse logic yang sama kayak `POST /address-book`.
- Tandain `invites.redeemed_by_user_id` + `redeemed_at`, code jadi gak bisa dipakai ulang.
- Kirim push notification ke inviter (pola sama kayak `address_book_added` di `address_book_api.md`), event baru mis. `invite_redeemed`, Body: *"{Nama} baru gabung lewat undangan kamu! ✉️"*.

**Error `400`:**
- `invite code not found`
- `invite already redeemed`
- `cannot redeem your own invite code` (kalau somehow code kepake sama akun yang sama, mis. re-login/testing)

---

## 3. Alur "surat langsung kekirim begitu temen join"

Ini bagian yang bikin fitur ini beda dari sekadar "invite generik" — surat yang udah ditulis user di `SelectRecipientScreen` **tidak** langsung `POST /letters` (karena `recipient_id` belum ada, invitee belum jadi user). Dua opsi implementasi, **direkomendasikan opsi A** karena gak nambah state baru di backend:

**Opsi A — draft disimpan di client, kirim on-redeem (direkomendasikan)**
1. User A nulis surat di compose screen tapi target-nya "invite" bukan recipient existing → surat disimpan sebagai local draft (Room/DataStore, pola yang sama kemungkinan udah ada buat draft biasa).
2. User A generate invite (`POST /invites`), share link.
3. User B install & redeem → response kasih `inviter_user_id`.
4. Client A **tidak** perlu polling — begitu redeem sukses, backend push notif ke A; A buka app, draft yang nunggu otomatis di-`POST /letters` pakai `recipient_id = inviter_user_id`/`invitee_user_id` yang baru valid.
5. Kalau A gak pernah buka app lagi, surat ya tetap draft lokal — gak ada surat "hilang" di server, karena emang belum pernah keupload. Trade-off: gak ada guarantee delivery kalau device A ganti/uninstall sebelum draft terkirim.

**Opsi B — pending letter tersimpan di server (lebih kompleks, TIDAK direkomendasikan buat MVP)**
- Butuh tabel `pending_letters` (mirror kolom `letters` tapi `recipient_id` nullable + `invite_code` FK), worker yang resolve begitu invite redeemed, dan ubah `letters_api.md` buat terima recipient yang lagy "pending". Cuma worth dibangun kalau riset user nunjukin banyak yang nutup app sebelum draft sempet ke-flush (opsi A gak reliable).

MVP: bangun **opsi A dulu**. Gak nyentuh skema `letters` sama sekali — endpoint `POST /letters` yang ada di `letters_api.md` dipanggil apa adanya begitu `recipient_id` valid.

---

## 4. Desain Database

Migrasi baru, pola penomoran nerusin yang terakhir (`013_address_book.sql` → cek nomor migrasi terbaru sebelum nentuin nomor final):

```sql
CREATE TABLE IF NOT EXISTS invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    inviter_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    redeemed_by_user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    redeemed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_invite_not_self CHECK (inviter_user_id <> redeemed_by_user_id)
);
CREATE INDEX IF NOT EXISTS idx_invites_inviter_user_id ON invites(inviter_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_invites_code ON invites(code);
```

Gak perlu tabel baru buat "pending letter" kalau pakai Opsi A (§3) — draft-nya murni client-side.

---

## 5. Yang perlu dibangun di Android (bukan cuma BE)

- **Play Install Referrer API** (`com.android.installreferrer:installreferrer`): baca referrer string pas first-launch, extract invite code, simpan sementara sampai user selesai onboarding/signup, baru call redeem.
- **App Links** (`https://peny-dashboard-xi.vercel.app/invite/{code}`) buat kasus app udah ke-install — intent-filter di manifest, extract `code` dari `Uri`.
- UI generate/share invite link (kemungkinan tempatnya di `SelectRecipientScreen` `onShareContactClick` yang sekarang cuma buka address book sheet — lihat `SelectRecipientViewModel.kt:184` — atau entry point baru).
- Local draft storage buat surat yang nunggu invite redeemed (§3 Opsi A).

### 5.1 Setup domain (Vercel, gak perlu beli apa-apa)

Dua jalur share link, beda kebutuhan hosting:

**A. App belum ke-install** — gak butuh domain sama sekali. Link share-nya langsung ke Play Store, invite code nempel di parameter `referrer`:
```
https://play.google.com/store/apps/details?id=com.apps.unsealed&referrer=invite_code%3DK7XQ2A
```
Play Store nyimpen referrer string ini; app baca balik lewat Install Referrer API pas first-launch (§5).

**B. App udah ke-install** — App Links butuh file verifikasi di-host di domain yang match `android:host`:
1. Tambahin static file di project Vercel (`peny-dashboard-xi.vercel.app`) — kalau Next.js, taruh di `public/.well-known/assetlinks.json`:
   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": {
       "namespace": "android_app",
       "package_name": "com.apps.unsealed",
       "sha256_cert_fingerprints": ["<SHA256 dari keystore signing, ambil via `keytool -list -v` atau Play Console > App integrity>"]
     }
   }]
   ```
   Harus bisa diakses publik di `https://peny-dashboard-xi.vercel.app/.well-known/assetlinks.json` (no auth — beda dari route `/api/v1/admin/*` yang dashboard ini punya).
2. Tambahin route `/invite/[code]` (halaman ringan/redirect — kalau user buka link ini dari browser bukan dari app, arahkan ke Play Store dengan link dari §5.1.A).
3. Intent-filter di `AndroidManifest.xml`:
   ```xml
   <intent-filter android:autoVerify="true">
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="https" android:host="peny-dashboard-xi.vercel.app" android:pathPrefix="/invite" />
   </intent-filter>
   ```

Vercel subdomain gratis valid buat App Links verification — Google cuma cek exact host match + cert fingerprint, gak peduli domain custom atau bukan.

---

## 6. Growth tracking (nice-to-have, bukan blocker MVP)

Metrics buat admin dashboard (`admin_dashboard_web.md`) kalau mau ukur efektivitas:
- Jumlah `invites` dibuat vs `redeemed` (conversion rate).
- Top inviter (leaderboard, opsional buat gamifikasi lanjutan).

Gak masuk scope MVP — endpoint di §2 udah cukup buat fitur jalan, dashboard bisa nyusul.

---

## 7. Roadmap (belum dibangun / di luar scope MVP)

- Opsi B (§3) — pending letter di server, kalau data nunjukin opsi A gak cukup reliable.
- ~~Reward/insentif buat invite sukses (mis. energy/stamp gratis) — belum ada keputusan produk soal ini.~~ **Sudah ada keputusan & spec-nya** — lihat `docs/be_updet/invite_friend_reward_api.md` (request baru, belum dibangun): reward energi buat inviter+invitee, cair begitu invitee kirim surat pertama, lewat sistem quest yang udah ada. Precondition-nya (§3 di dokumen itu) nambah satu unique index ke `invites` table di sini.
- Rate limit generate invite (cegah spam) — MVP cukup andalkan auth + unique code, bisa nyusul kalau kejadian abuse beneran.
