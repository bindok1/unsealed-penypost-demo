# Daily Penpal Stack + Notification Preference

Spec ini untuk FE (Android/Kotlin) — dokumentasi perubahan `GET /api/v1/penpals/feed` jadi "stack harian" per user, field onboarding baru `notify_hour_pref`, dan notifikasi push yang menyertainya. Lihat juga `penpals_api.md` (kontrak feed lengkap) dan `notifications_api.md` (mekanisme push umum).

---

## 1. Ringkasan

Sebelumnya `GET /penpals/feed` murni infinite-scroll kronologis atas semua surat penpal — user bisa scroll terus ke masa lalu. Sekarang (tanpa `region` filter) feed itu jadi **stack harian personal**: tiap user dapat set ≤20 surat sendiri, digenerate ulang tiap hari, dan begitu hari berikutnya datang stack lama "hilang" dari feed utama (kayak Stories) — bukan lagi ditambah/di-append. Tujuannya: terasa seperti "dapat tumpukan surat baru tiap hari", bukan feed yang bisa di-scroll tanpa habis.

Surat lama **tidak dihapus** dari database — cuma tidak lagi ditampilkan di feed utama. Like/reply ke surat lama tetap berfungsi normal kalau user masih punya akses ke item itu (mis. lewat address book atau riwayat reply).

## 2. `GET /api/v1/penpals/feed` — behavior berubah

| Kondisi request | Behavior |
|---|---|
| Tanpa `region`, signed-in | **Baru**: baca stack harian milik viewer (≤20 item, `stack_date` = tanggal kalender UTC hari ini). Kalau stack hari ini belum ada (worker belum sempat jalan), digenerate on-the-fly saat request ini — user tidak perlu nunggu. |
| Dengan `region` | **Tidak berubah** — tetap chronological semua surat penpal di region itu (dipakai untuk browsing eksplisit per region & fallback `POST /penpals/match`). |
| Anonymous (belum login) | **Tidak berubah** — tetap chronological (tidak ada "viewer" untuk digenerate stack-nya). |

Shape response JSON **tidak berubah** (`items`, `next_cursor`, semua field per item sama seperti sebelumnya). Yang berubah:
- Jumlah item dibatasi ke ≤20/hari untuk mode stack (bukan lagi bisa di-page mundur tanpa batas).
- `next_cursor` akan jadi `null` lebih cepat — begitu stack hari ini habis di-scroll, bukan berarti error, itu memang akhir dari stack hari ini.
- Isi feed bisa berubah total kalau user buka app lagi besok (stack baru) — jangan asumsikan surat yang sudah pernah muncul akan tetap ada di sesi berikutnya.

**Implikasi UI:** kalau FE punya "pull to refresh" atau caching lokal terhadap isi feed, pastikan cache di-invalidate per hari (atau langsung percaya response terbaru dari server tiap kali dibuka), supaya tidak nampilin stack kemarin yang sudah stale.

## 3. Field baru `notify_hour_pref` + `utc_offset_minutes`

Dua field baru di `PATCH /api/v1/auth/me` (dan `POST /api/v1/auth/register` untuk `utc_offset_minutes`), muncul juga di response `GET /auth/me` / `POST /auth/register` (shape sama seperti field lain di `profile_api.md`).

| Field | Type | Rules |
|---|---|---|
| `notify_hour_pref` | string | salah satu dari `"morning"`, `"afternoon"`, `"evening"`. Default `"morning"` untuk user baru & user existing (belum pernah set). |
| `utc_offset_minutes` | int (nullable) | menit **bertanda**, timur dari UTC (mis. WIB/+07:00 → `420`, PST/-08:00 → `-480`). Range valid: `-720` s.d. `840`. Default `0` (UTC) kalau belum pernah dikirim client. |

Contoh request:
```json
PATCH /api/v1/auth/me
{ "notify_hour_pref": "evening", "utc_offset_minutes": 420 }
```

Response `400` kalau value di luar pilihan/range:
```json
{ "success": false, "error": "notify_hour_pref must be one of: morning, afternoon, evening" }
{ "success": false, "error": "utc_offset_minutes must be between -720 and 840" }
```

**FE perlu update:** kirim `utc_offset_minutes` device saat register (`POST /auth/register`) dan idealnya setiap kali app dibuka via `PATCH /auth/me` (device bisa pindah timezone). Di Android: `TimeZone.getDefault().getRawOffset() / 60000` (atau pakai `OffsetDateTime.now().offset.totalSeconds / 60` kalau mau ikut DST otomatis). Field ini **opsional** — kalau tidak dikirim, backend fallback ke offset yang tersimpan sebelumnya (atau `0`/UTC untuk user baru), jadi app lama tidak akan error, cuma jamnya kurang akurat seperti sebelumnya.

**Bagaimana jam dihitung sekarang:** `notify_hour_pref` dipetakan ke jam **lokal** target (bukan UTC tetap lagi), lalu backend hitung jam UTC aktualnya pakai `utc_offset_minutes` user:

| Pilihan | Jam lokal target |
|---|---|
| `morning` | `08:00` waktu lokal user |
| `afternoon` | `14:00` waktu lokal user |
| `evening` | `20:00` waktu lokal user |

Ini menggantikan approksimasi lama (§7 versi sebelumnya) yang cuma mengandalkan `continent` dan jam UTC tetap 00:00/08:00/16:00 — sekarang akurat ke menit sesuai offset device asli, bukan cuma benua.

## 4. Onboarding UX

Tambahkan step baru di flow onboarding (setelah step interests/languages di `profile_api.md`'s Auth Flow, urutan pastinya fleksibel):
- Style pertanyaan: **3 tombol pilihan** (pagi/siang/malam), **bukan** time picker bebas — backend cuma terima 3 enum tetap, bukan jam arbitrary.
- Copy yang disarankan: "Kapan kamu mau dapat tumpukan surat baru?" dengan 3 opsi berlabel bahasa Indonesia (`"Pagi"`, `"Siang"`, `"Malam"`) yang di-map ke value enum Inggris (`"morning"`, `"afternoon"`, `"evening"`) saat dikirim ke `PATCH /auth/me` — sama seperti `gender` (`"m"`/`"f"`) yang label UI-nya beda dari value API.
- Tidak wajib diisi di onboarding — kalau di-skip, user tetap dapat default `"morning"` dan bisa diubah nanti dari halaman settings.

## 5. Notifikasi push

Payload FCM saat stack harian siap:
```json
{
  "notification": { "title": "Your Daily Stack is Ready! 📬", "body": "New letters from fellow penpals are waiting to be read. Open them now!" },
  "data": { "event": "daily_stack_ready" }
}
```
- Event key baru: `daily_stack_ready` (`app/constant/event.go`) — tambahkan handling-nya di sisi client kalau ada logic per-`event` (mengikuti pola event lain: `letter_delivered`, `address_book_added`, dst., lihat `notifications_api.md`).
- **Kapan dikirim:** pada jam UTC sesuai `notify_hour_pref` user (§3), begitu worker background berhasil generate stack hari itu.
- **Kapan TIDAK dikirim:** kalau user sudah buka feed sendiri sebelum jam preferensinya tiba (lazy-generate saat request `GET /penpals/feed`) — user sudah lihat stack barunya secara langsung di app, jadi push dianggap tidak perlu lagi untuk hari itu.

## 6. `POST /api/v1/penpals/feed/refresh` — reroll manual pakai energy

Endpoint baru: user bisa refresh stack hari ini lebih awal (gak perlu nunggu besok) dengan bayar `energy` — ikut pola `POST /letters/{id}/unlock` & unlock stamp/paper (`docs/be/letters_api.md`, `docs/be/rewards_api.md`). **Gak ada limit harian** — selama energy cukup, boleh dipanggil berkali-kali (energy itu sendiri yang jadi pembatas alami).

- **Cost**: `20` energy (`RefreshFeedEnergyCost`, `app/module/penpal/service/penpal_service.go`) — placeholder, sama kayak semua harga energy lain di codebase ini, belum keputusan produk final.
- **Efek**: stack hari ini (`user_daily_stacks` row buat `CURRENT_DATE`) di-reroll — isi `user_daily_stack_letters`-nya diganti. Kalau user belum pernah punya stack hari ini (misal refresh dipanggil sebelum pernah buka feed), stack-nya dibuatkan dulu.
- **"Fresh" tapi gak pernah lebih sedikit**: reroll **mengutamakan** surat yang belum pernah muncul di stack hari ini, tapi kalau pool surat yang tersedia tipis, sisanya di-backfill pakai surat yang sama seperti sebelumnya — supaya user yang udah bayar energy gak malah dapat stack yang lebih kosong dari sebelumnya.
- **Atomic**: satu transaction — lock saldo energy (`FOR UPDATE`), cek cukup, potong, baru reroll surat. Kalau salah satu gagal, semuanya rollback (gak ada kejadian energy kepotong tapi stack gagal di-reroll, atau sebaliknya).

Request: tidak ada body.

Response `200` — sama shape kayak `GET /penpals/feed` (stack mode) ditambah 2 field:
```json
{
  "success": true,
  "data": {
    "items": [ /* sama shape seperti item GET /penpals/feed */ ],
    "next_cursor": null,
    "energy_spent": 20,
    "energy_remaining": 80
  }
}
```

Response `400` kalau energy kurang:
```json
{ "success": false, "error": { "code": "INSUFFICIENT_ENERGY", "message": "Energi kamu tidak cukup untuk refresh feed." } }
```

## 7. Known limitations (disengaja untuk MVP, bukan bug)

- **Bukan IANA timezone, cuma UTC offset mentah** — `utc_offset_minutes` (§3) tidak otomatis handle DST (daylight saving) kayak `Asia/Jakarta` atau `America/New_York` akan. Kalau device pindah DST, app perlu kirim ulang `utc_offset_minutes` terbaru (mis. saat app dibuka) supaya tetap akurat — backend tidak tahu kapan DST berubah dengan sendirinya.
- **Durasi stack tetap bisa asimetris** — `stack_date` masih dihitung dari kalender UTC, bukan kalender lokal user, jadi window antara "stack baru muncul" dan "stack berganti besok" masih bisa kurang dari 24 jam tergantung kombinasi offset + pref jam. Trade-off kesederhanaan skema yang belum diubah di iterasi ini.
- **Tidak ada anti-repeat antar hari** — random-pick surat per hari bisa saja mengambil surat yang sama seperti hari sebelumnya, terutama kalau supply surat penpal di suatu region masih sedikit. Belum ada logic exclude "surat yang sudah pernah masuk stack user ini".
- **Region-filtered feed tidak ikut mode stack** — `GET /penpals/feed?region=X` sengaja tetap chronological (lihat §2), supaya fallback `POST /penpals/match` (`penpals_api.md`) tidak berubah behavior-nya.
