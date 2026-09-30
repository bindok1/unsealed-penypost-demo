# Letters / Messaging Core API

Fitur inti app (slow-messaging). Menggantikan `dummyInboxItems`/`InboxItem` di `InboxScreen.kt` dan flow dummy di `SelectRecipientScreen`.

**Supersede catatan lama:** `compose-screen-spec.md` §10 dan `docs/todo.md` #3 masih menyebut "Firebase Storage + Firestore" untuk kirim surat — itu asumsi lama sebelum backend Go/Postgres/Railway ada. Spec ini **mengganti** asumsi itu: body surat & metadata masuk Postgres lewat API di bawah; gambar composite (kalau ada annotasi) tetap lewat flow presign yang **sudah ada** (`POST /api/v1/storage/presign` → R2), sama seperti upload foto profil — tidak butuh endpoint upload baru.

Semua endpoint di bawah `/api/v1`, butuh `Authorization: Bearer <firebase_id_token>`.

---

## `POST /api/v1/letters`

Kirim surat. Server yang hitung waktu tiba (slow-delivery), client cuma kirim isi surat.

Request:
```json
{
  "recipient_id": "firebase-uid-or-seed-id",
  "dear_name": "Kirana",
  "body_text": "...",
  "envelope": "ENVELOPE_2",
  "stamp": "OCEAN",
  "paper_template": "AGED_KRAFT",
  "paper_color": "CREAM",
  "font_id": "CAVEAT",
  "composite_image_url": "https://.../key-from-presign-flow",
  "crisis_tag": null,
  "visibility": "private",
  "sticker_data": [],
  "envelope_sticker_id": null,
  "envelope_composite_image_url": "https://.../key-from-presign-flow",
  "paper_url": "https://.../paper-texture.webp"
}
```

| Field | Type | Rules |
|---|---|---|
| `recipient_id` | string | Formatnya **beda tergantung `visibility`**. Kalau `visibility != "public"` (surat langsung ke satu user): required, harus user valid **dan belum delete akun** — `400 "user not found"` kalau id-nya gak ada ATAU `users.status == "deleted"` (sengaja disamain pesannya, gak bocorin beda antara "emang gak ada" vs "sengaja dihapus", pola sama kayak block-hide di `GET /users/{id}`). Berlaku juga buat sender (`senderID`) sebagai defense-in-depth — walau praktiknya gak akan kejadian karena akun deleted udah diblok total di `package/middleware/auth.go` sebelum bisa manggil endpoint manapun. Kalau `visibility == "public"`: **wajib** literal `"penpal:<region>"` (mis. `"penpal:Asia"`), bukan UID — lihat "Public penpal posts" di bawah. |
| `dear_name` | string | required, non-blank (spec `compose-screen-spec.md` §11) |
| `body_text` | string | required, minimal 20 karakter, **tanpa batas atas** (user bisa mengecilkan font di client buat muat lebih banyak teks — lihat "Update" di bawah, `POST /letters` §10 lama sempat nyebut maks 2000 karakter, itu **sudah dicabut**) |
| `envelope` | string | id dari `GET /envelopes` (`docs/be/envelopes_api.md`) — bukan lagi enum tertutup, lihat catatan di bawah |
| `stamp` | string | id dari `GET /stamps` |
| `paper_template` / `paper_color` / `font_id` | string | **metadata saja** sejak update di bawah — tidak dipakai buat render ulang di recipient (composite image sudah mencakup semuanya), disimpan buat analitik/kemungkinan re-edit surat oleh pengirim sendiri nanti |
| `paper_url` | string, nullable | **(Baru)** URL aset kertas surat jika berasal dari kertas kreator/remote collectible. Dipakai recipient buat render latar saat mode fallback teks |
| `composite_image_url` | string | **wajib diisi, non-null**, lihat "Update" di bawah |
| `crisis_tag` | string, nullable | **belum dipakai** — reserved buat fitur crisis-support/moderasi yang belum dirancang, backend cukup terima & simpan apa adanya, jangan validasi ketat dulu |
| `visibility` | string | `"private"` (default) atau `"public"` — lihat "Postal Collection Showcase" di bawah buat makna & konsekuensinya, **jangan** samain sama field lama yang sempat di-hardcode berdasarkan recipient mode (itu bug, sudah diperbaiki di client). `"public"` juga ngubah aturan `recipient_id`/`dear_name` — lihat "Public penpal posts" di bawah |
| `sticker_data` | array of string | stiker yang ditempel di badan surat (bukan di amplop) — belum ada validasi khusus, simpan apa adanya |
| `envelope_sticker_id` | string, nullable | id stiker yang ditempel di amplop, kalau ada |
| `envelope_composite_image_url` | string, nullable | hasil flatten tampilan amplop (beda dari `composite_image_url` yang isinya surat/kertas) — dipakai buat sisi "amplop" di Postal Collection Showcase |

### Public penpal posts: `recipient_id` = `"penpal:<region>"`, bukan UID

Surat `visibility: "public"` (post ke penpal feed global) **tidak** punya satu recipient tertentu, jadi `recipient_id` bukan UID — harus literal `"penpal:<region>"`, dengan `<region>` salah satu dari 7 continent yang valid (`app/constant/continent.go`): `Africa`, `Antarctica`, `Asia`, `Europe`, `North America`, `Oceania`, `South America` (persis case-nya). Salah satu contoh: `"penpal:Asia"`.

`400 'recipient_id must be "penpal:<region>" for public letters'` kalau formatnya nggak cocok (mis. masih ngirim UID/seed-id asli sambil `visibility: "public"`) atau region-nya bukan salah satu dari 7 di atas. Endpoint block-check (`IsBlockedEitherDirection`) juga di-skip buat kasus ini karena nggak ada recipient spesifik yang bisa nge-block.

Kebalikannya: `visibility != "public"` (default, direct letter) **wajib** `recipient_id` = UID/seed-id user asli, ikut aturan di tabel field di atas — **jangan** kirim `"penpal:<region>"` di kasus ini.

Contoh request public:
```json
{
  "recipient_id": "penpal:Asia",
  "body_text": "...",
  "envelope": "ENVELOPE_2",
  "stamp": "OCEAN",
  "paper_template": "AGED_KRAFT",
  "paper_color": "CREAM",
  "font_id": "CAVEAT",
  "composite_image_url": "https://.../key-from-presign-flow",
  "visibility": "public",
  "sticker_data": [],
  "envelope_sticker_id": null,
  "envelope_composite_image_url": "https://.../key-from-presign-flow"
}
```
`dear_name` boleh kosong di kasus ini (lihat aturan di tabel field) — nggak ada penerima spesifik buat disapa.

### Update: selalu `composite_image_url`, bukan hybrid lagi

**Keputusan produk baru — supersede seksi hybrid yang lama di bawah ini:** setiap surat, sesimpel apapun isinya, sekarang **selalu** di-flatten jadi satu PNG dulu (lewat `POST /storage/presign` yang sudah ada) sebelum dikirim — bukan cuma surat yang punya elemen bebas-posisi (annotate/gambar/rich-text). Client sudah beneran bikin compositor-nya (`LetterCompositor.kt`, `GraphicsLayer` capture) dan menjalankan presign→upload sebelum `POST /letters` — lihat `ComposeViewModel.compositeAndSaveDraftForSend()`.

**Perubahan validasi yang backend perlu terapkan:** `composite_image_url` berubah dari *conditionally required* (per aturan hybrid di bawah) jadi **selalu required, tidak boleh `null`**. `400` kalau field ini kosong pada request manapun (bukan cuma yang "kompleks"). `paper_template`/`paper_color`/`font_id` tetap boleh dikirim tapi backend **tidak perlu lagi pakai buat re-render** — anggap metadata, bukan sumber render.

**Trade-off yang disadari & diterima:** `body_text` tetap dikirim (buat search/notifikasi-preview/moderasi) tapi bukan lagi sumber render utama; ini artinya alur `POST /admin/seed-users/{id}/letters` (seed surat massal via teks polos, `docs/be/seed_accounts_api.md`) **tetap boleh** kirim `composite_image_url: null` khusus dari jalur admin — pengecualian ini hanya untuk endpoint seed, bukan `POST /letters` biasa, karena ops butuh generate surat seed lewat script tanpa compositing manual.

**Render di sisi recipient:** selalu tampilkan `composite_image_url` apa adanya — tidak ada lagi fallback ke `body_text` + `paper_template` untuk surat yang dikirim user asli (client `OpenLetterScreen.kt` sudah pakai `composite_image_url` sebagai satu-satunya sumber gambar surat, dengan `paper_template` cuma jadi placeholder loading/error). Untuk surat seed admin (`composite_image_url: null`), client fallback ke texture `paper_template` seperti sebelumnya.

Detail lengkap DTO/endpoint pendukung (envelope/stamp catalog, presign, penpals match) ada di `docs/be/send_and_envelope_handoff.md` — dokumen itu jadi entry point buat kerjaan backend fitur Send + Get Envelope ini.

---

<details>
<summary>Seksi lama (hybrid text-vs-image) — <strong>sudah disupersede</strong> di atas, dipertahankan sebagai riwayat keputusan</summary>

### Kapan `body_text` polos vs `composite_image_url` (hybrid, bukan selalu-image)

Surat yang **cuma** teks + atribut simpel (font, ukuran/warna/bold/italic **seragam se-surat**, alignment, tanpa elemen posisi bebas) cukup dikirim sebagai `body_text` + atribut terstruktur — `composite_image_url` dikosongkan, client re-render pakai `paper_template` yang dipilih.

Begitu surat punya **elemen posisi bebas** — annotate strokes, gambar yang disisipkan & di-drag/resize (`ImageInstance`), stiker, **atau rich text formatting per-rentang** (Bold/Italic/Text Size/Indent yang beda-beda di dalam satu surat, bukan seragam — lihat `ComposeUiState.bodyStyleRuns`, `docs/compose-screen-spec.md` §4/§9) — `composite_image_url` **wajib** diisi (hasil flatten semua layer jadi satu PNG, upload dulu lewat `POST /storage/presign` yang sudah ada, baru URL-nya dikirim di sini). Alasannya bukan cuma soal simpel: posisi elemen-elemen itu koordinat absolut di canvas si pengirim (dan buat rich text, rentang stylingnya terikat ke index karakter tertentu) — kalau dikirim sebagai data terstruktur lalu di-render ulang di device recipient yang beda ukuran layar, teks bisa reflow beda dan annotate/gambar/formatting jadi ga nempel lagi di tempat yang dimaksud. Flatten ke image adalah satu-satunya cara jamin WYSIWYG persis.

**Kenapa bukan selalu-image untuk semua surat (alasan ini sudah tidak berlaku, lihat "Update" di atas):** bakal nyusahin flow `POST /admin/seed-users/{id}/letters` (`docs/be/seed_accounts_api.md`) — ops nulis surat seed lewat teks polos biar gampang di-script/di-generate massal buat ngatasi chicken-egg problem. Plain-text juga masih kebuka buat fitur nanti kayak "letter-based profiling" yang sudah di-flag di roadmap (`profile_api.md`) — ga bisa search/analisis konten kalau semuanya flat image. Keputusan produk terbaru menerima trade-off ini demi satu code path yang lebih simpel (lihat "Update" di atas) — seed letters jadi satu-satunya pengecualian yang tetap boleh `null`, bukan lagi aturan umum.

</details>

**Slow-delivery calculation** — server hitung `estimated_arrival_at` dari `sender.continent` vs `recipient.continent`:
- Continent sama → **WALK** → +24 jam
- Continent beda → **SWIM** → +72 jam

Ini disesuaikan dengan UI *letter tracking* di client (peta dunia + animasi Peni jalan kaki/berenang). `transport_mode` dihitung di client dari perbandingan `sender_continent` vs `recipient_continent`, backend cukup mengirimkan `estimated_arrival_at` yang sesuai.

Response `201`:
```json
{ "success": true, "data": { "id": "letter_xyz", "status": "IN_TRANSIT", "estimated_arrival_at": "2026-08-03T09:00:00Z" } }
```

## `GET /api/v1/letters/inbox`

Query: `status=incoming|delivered` (opsional filter, default semua), `cursor`, `limit`.

Response item shape (pengganti `InboxItem` dummy):
```json
{
  "id": "letter_xyz",
  "sender_id": "...",
  "sender_name": "...",
  "sender_continent": "Asia",
  "posted_at": "2026-07-27T06:28:00Z",
  "delivered_at": "2026-07-27T18:28:00Z",
  "estimated_arrival_at": "2026-07-27T18:28:00Z",
  "stamp": "OCEAN",
  "envelope": "ENVELOPE_2",
  "is_unread": true,
  "delivery_status": "DELIVERED"
}
```

**Gap yang perlu diputuskan:** dummy `InboxItem.country` pakai granularitas negara ("Japan", "Spain"), sementara `UserDto` cuma punya `continent`. Dua opsi: (a) turunkan UI supaya tampilkan continent aja (paling simpel, konsisten sama data yang ada), atau (b) tambah field `country` opsional di `UserDto`/register flow kalau granularitas negara memang mau dipertahankan. Belum diputuskan di sini — flag ke product decision sebelum M4 mulai dikerjakan backend-nya.

`time_label`/`meta_label`/`expected_delivery_label` dari dummy **sengaja tidak** direplikasi sebagai string pre-formatted dari server — kirim timestamp mentah (`posted_at`, `delivered_at`, `estimated_arrival_at`), format di client (lebih gampang di-i18n-kan, konsisten sama pola `posted_at` di `penpals_api.md`).

Delivery status transition (`IN_TRANSIT` → `DELIVERED`) dihitung **lazy** saat endpoint ini dipanggil (`now >= estimated_arrival_at`) — tidak butuh cron/background worker untuk read-path. (Proaktif push notification saat surat tiba butuh worker terpisah — lihat `docs/be/notifications_api.md`, M7.)

> **Catatan scope:** filter di endpoint ini selalu `recipient_id = viewer` — surat yang viewer **kirim sendiri** tidak akan pernah muncul di sini, ini bukan bug, itu memang makna "inbox" (surat diterima, bukan dikirim). Kalau butuh screen gabungan yang langsung menampilkan room percakapan ke kedua sisi (sender tidak perlu menunggu recipient balas), pakai `/mailbox` di bawah, bukan endpoint ini.

## `GET /api/v1/letters/sent`

Sama shape kayak `/inbox`, tapi filter `sender_id == viewer`. Ini yang isi tab "Sent" di Inbox Mode 2 (`docs/todo.md` #4).

## `GET /api/v1/letters/{id}`

Detail penuh: semua field di atas + `body_text`, `dear_name`, `composite_image_url`. Side-effect: kalau viewer == recipient dan `is_unread == true`, server set `is_unread = false` (mark-as-read otomatis saat dibuka, tidak perlu endpoint PATCH terpisah).

### Gap (2026-08-18): `recipient_name` gak ada di response — masalah buat delivery tracking pas viewer = sender

Response cuma denormalize `sender_name` (buat kasus viewer = recipient, lihat field list di atas `/letters/inbox`), **tidak ada** `recipient_name`/`recipient_nickname` sama sekali. Ini baru kelihatan masalahnya di alur *delivery tracking* Android:

- `DeliveryTrackingViewModel` manggil endpoint ini buat ambil detail surat yang mau di-track. Waktu viewer = sender (kasus "Track" dari `OutgoingTrackingContent` di mailbox thread — beda dari `LetterSentScreen` yang nama recipient-nya udah ada di tangan client langsung dari compose flow, gak lewat endpoint ini), VM eksplisit hardcode `recipientName = null` (`DeliveryTrackingViewModel.kt:114-115`) karena API memang gak expose nama itu.
- Efeknya `DeliveryRouteMap` jatuh ke fallback label continent doang (mis. cuma "Asia") dan nggak nunjukin nama penerima di peta, beda dari alur `LetterSentScreen` yang labelnya lengkap.

Ini root cause yang sama kayak item roadmap `recipient_id`/`recipient_name` di bawah (`GET /letters/sent`), tapi call-site-nya beda — di sini yang kena `GET /letters/{id}` (dipakai tracking), bukan list "Sent" tab. Kalau mau fix tanpa nyentuh backend dulu: thread `correspondentName` yang udah ada di scope `MailboxThreadScreen` (dipakai buat teks dialog konfirmasi) lewat nav-arg ke `DeliveryTrackingViewModel`, dipakai sebagai fallback `recipientName` pas `isFromViewer == true` — persis pola yang sudah dipakai `LetterSentScreen`. Fix backend yang proper: denormalize `recipient_nickname` di `entity.Letter` (sama kayak `sender_nickname`) lalu expose di response ini juga, bukan cuma `/letters/sent`.

---

## Mailbox (Room/Thread View)

Beda dari `/letters/inbox` & `/letters/sent` (flat list, satu row per surat, cursor-paginated) — `/mailbox` mengelompokkan surat per lawan bicara (`correspondent_id`), model room/thread kayak chat app. **Ini endpoint yang dipakai buat screen "list percakapan" utama** — begitu satu surat terkirim (dari sisi manapun), room-nya langsung muncul di `/mailbox` **kedua belah pihak**, sender tidak perlu menunggu recipient balas, dan (sejak 2026-08-13) tidak perlu menunggu `estimated_arrival_at` lewat juga — room sudah kelihatan walau surat masih `IN_TRANSIT`.

Semua endpoint di bawah butuh `Authorization: Bearer <firebase_id_token>`. Response array langsung di `data` — belum ada pagination/cursor (beda dari `/letters/inbox`).

### `GET /api/v1/mailbox`

List semua room milik viewer, diurutkan `last_sent_at` terbaru dulu.

Response `200`:
```json
{
  "success": true,
  "data": [
    {
      "correspondent_id": "firebase-uid-or-seed-id",
      "correspondent_name": "Kirana",
      "correspondent_continent": "Asia",
      "last_body_text": "isi surat terakhir di thread ini...",
      "last_sent_at": "2026-08-13T09:00:00Z",
      "last_status": "IN_TRANSIT",
      "last_estimated_arrival_at": "2026-08-13T15:00:00Z",
      "last_stamp": "OCEAN",
      "last_envelope": "ENVELOPE_2",
      "last_composite_image_url": "https://.../key-from-presign-flow",
      "unread_count": 2
    }
  ]
}
```

| Field | Tipe | Catatan |
|---|---|---|
| `correspondent_id` | string | UID lawan bicara di room ini |
| `correspondent_name` / `correspondent_continent` | string | nickname & continent lawan bicara (JOIN ke `users`), buat header row list — sama data yang dipakai `sender_name`/`sender_continent` di `/letters/inbox` |
| `last_body_text` | string | isi surat paling baru di thread — sudah termasuk yang masih `IN_TRANSIT` |
| `last_sent_at` | string (RFC3339) | dipakai buat sort urutan room |
| `last_status` | enum | `"IN_TRANSIT"` \| `"DELIVERED"` — status surat **paling baru** di thread, ini yang nentuin blur/tidaknya preview di row list |
| `last_estimated_arrival_at` | string (RFC3339) | dipakai buat hitung countdown kalau `last_status == "IN_TRANSIT"` |
| `last_stamp` / `last_envelope` | string | id stamp/envelope surat terakhir — buat render preview visual di row list |
| `last_composite_image_url` | string? \| null | gambar surat terakhir (flattened PNG dari compositor client) — **ini** sumber render utama, bukan `last_body_text` (lihat catatan `composite_image_url` di `POST /letters` di atas). `null` cuma buat surat seed admin |
| `unread_count` | int | jumlah surat `read_at IS NULL` & `recipient_id == viewer`, termasuk yang masih `in_transit` |

### `GET /api/v1/mailbox/{threadId}`

Detail satu room — `threadId` = `correspondent_id`. Semua surat (in_transit + delivered) antara viewer & correspondent, urut `sent_at ASC` (chronological, cocok buat render chat bubble dari atas ke bawah).

Response `200`:
```json
{
  "success": true,
  "data": [
    {
      "id": "letter_xyz",
      "sender_id": "firebase-uid-or-seed-id",
      "body_text": "...",
      "status": "DELIVERED",
      "sent_at": "2026-08-12T09:00:00Z",
      "estimated_arrival_at": "2026-08-12T15:00:00Z",
      "stamp": "OCEAN",
      "envelope": "ENVELOPE_2",
      "composite_image_url": "https://.../key-from-presign-flow",
      "delivered_at": "2026-08-12T15:00:03Z",
      "read_at": "2026-08-12T16:10:00Z"
    }
  ]
}
```

| Field | Tipe | Catatan |
|---|---|---|
| `id` | string | letter id |
| `sender_id` | string | bandingkan sama UID viewer buat nentuin bubble kiri/kanan |
| `body_text` | string | full isi surat — sudah keikut walau `status == "IN_TRANSIT"` (lihat catatan produk di bawah) |
| `status` | enum | `"IN_TRANSIT"` \| `"DELIVERED"`, per-surat — satu room bisa berisi campuran keduanya |
| `sent_at` / `estimated_arrival_at` | string (RFC3339) | |
| `stamp` / `envelope` | string | id stamp/envelope surat ini |
| `composite_image_url` | string? \| null | gambar surat ini (flattened PNG) — sumber render utama, tampilkan apa adanya. `null` cuma buat surat seed admin (fallback ke texture `paper_template` di client, sama pola kayak `/letters/{id}`) |
| `delivered_at` | string? \| null | `null` kalau masih `IN_TRANSIT` |
| `read_at` | string? \| null | `null` kalau belum dibaca |

`threadId` yang salah/belum ada histori → array kosong `[]`, bukan `404`.

**Keputusan produk (2026-08-13):** `body_text`/`composite_image_url` sengaja **tidak** di-gate server-side berdasarkan `status` — konten surat yang masih `IN_TRANSIT` tetap ke-expose apa adanya di response ini, blur envelope murni cosmetic di client (`ComingSoonBottomSheet`-style UX, bukan proteksi data). Diterima sebagai trade-off karena app ini bukan messaging sensitif.

**⚠️ Known trade-off, perlu ditinjau ulang (2026-08-17):** `POST /letters/{id}/unlock` di bawah sekarang sudah dibangun (energy-gated instant delivery), jadi trade-off di atas sekarang punya konsekuensi ekonomi, bukan cuma UX — konten mentah tetap ke-expose apa adanya di response ini walau `status == "IN_TRANSIT"`, jadi proteksi "bayar energy buat lihat lebih cepat" gampang di-bypass lewat proxy (Proxyman/Charles/mitmproxy) tanpa keluar energy sama sekali. Belum di-fix sekarang (keputusan sadar, biar nggak nambah kompleksitas sebelum unlock endpoint kepakai beneran) — revisit kalau economy/energy ini sudah jalan produksi dan bypass-nya kelihatan dieksploitasi.

### 🐛 Bug (2026-08-25): `GET /mailbox/{threadId}` gak mark-as-read — badge unread di nav bar stuck

**Simptom di client:** user buka thread yang ada surat unread-nya, baca isinya, balik ke Mailbox list — badge unread di bottom nav (`Destinations.Inbox`) tetap nunjukin angka lama (mis. stuck di `3`) padahal user udah jelas-jelas baca semua surat di thread itu. Cuma ilang kalau app di-restart penuh (state di memory ke-reset, bukan beneran fixed).

**Root cause:** `GET /letters/{id}` (section di atas) punya side-effect mark-as-read (`read_at = now()` kalau `viewer == recipient_id` dan masih `NULL`) — **tapi `GET /mailbox/{threadId}` tidak**. Sejak Mailbox jadi primary read path (bukan lewat `/letters/inbox` → `/letters/{id}` lagi, lihat section "Mailbox (Room/Thread View)" di atas), gak ada satupun call yang beneran nge-set `read_at` pas user baca surat dari Mailbox thread. Android client sempat nyoba workaround refetch `GET /mailbox` abis buka thread buat resync badge count — tapi karena `read_at` di server emang gak pernah keubah, refetch itu cuma dapet `unread_count` yang sama persis. Bukan bug di client, bug-nya di server yang gak pernah nandain apa-apa.

**Fix yang dibutuhkan:** tambahin side-effect yang sama kayak `GET /letters/{id}` ke `GET /mailbox/{threadId}` — begitu endpoint ini dipanggil dan berhasil return data:

```
UPDATE letters
SET read_at = now()
WHERE (sender_id = :correspondentId OR recipient_id = :correspondentId)  -- surat di thread ini
  AND recipient_id = :viewerId                                          -- viewer adalah penerimanya
  AND read_at IS NULL
```

Catatan implementasi:
- **Scope-nya per-thread, bukan global** — cuma surat yang `recipient_id == viewer` **dan** lawan bicaranya == `threadId` yang di-mark, surat unread di thread lain harus tetap unread.
- **Termasuk surat yang masih `IN_TRANSIT`**, bukan cuma `DELIVERED` — konsisten sama keputusan produk 2026-08-13 di atas (konten `IN_TRANSIT` udah di-expose apa adanya di response ini, jadi kalau kontennya udah "kebaca" pas thread dibuka, `read_at`-nya juga harus ikut ke-set, sama kayak gimana `unread_count` di `/mailbox` list ngitung surat `IN_TRANSIT` sebagai unread juga — lihat field table `unread_count` di atas).
- Idempotent by design lewat `read_at IS NULL` guard — dipanggil berkali-kali (client polling thread tiap 5-20 detik selama ada surat in-transit, lihat `MailboxThreadViewModel.kt`) gak akan re-trigger apa-apa buat surat yang udah pernah di-mark.
- Gak butuh perubahan response shape — field `read_at` di response `GET /mailbox/{threadId}` (lihat tabel di atas) udah ada dari sebelumnya, cuma nilainya yang sekarang harus beneran keisi begitu di-update.

**FE-side (Android), tinggal implement pas ini sudah deploy:** `MailboxThreadViewModel.kt` udah punya resync call `mailboxRepository.getMailbox()` abis `loadThread()` sukses (buat refresh badge count di nav bar) — itu **sudah ada di client sekarang**, cuma percuma karena BE-nya belum nge-set apa-apa. Begitu fix di atas deploy, gak perlu perubahan kode Android tambahan — resync call yang sudah ada bakal otomatis mulai dapet `unread_count` yang benar dari server. Cukup manual-test ulang: buka thread yang punya unread, balik ke Mailbox list, cek badge-nya turun tanpa perlu restart app.

---

## `POST /api/v1/letters/{id}/unlock`

Instant-delivery: skip sisa `estimated_arrival_at` dan langsung set `delivered`, dibayar pakai `energy` (lihat `GET /auth/me`, field `energy`) — bukan `users.is_premium` seperti draft roadmap lama, ini keputusan sadar biar unlock jadi resource-sink yang bisa di-refill (reward harian/beli paket), bukan subscription gate.

- Validasi: viewer harus `recipient_id` letter ini, letter harus masih `status == "IN_TRANSIT"`, dan `energy` viewer harus cukup (`UnlockEnergyCost`, saat ini `20` — placeholder, belum ada keputusan produk soal harga final).
- Efek: `energy` viewer dikurangi, `status` jadi `"DELIVERED"`, `delivered_at = now()`.
- Atomic: row letter **dan** row user di-lock (`SELECT ... FOR UPDATE`) dalam transaction yang sama dengan deduct energy + set delivered, jadi double-tap (2x request nyaris bersamaan) nggak bakal ke-charge dua kali — request kedua akan lihat letter sudah `DELIVERED` dan gagal sebelum nyentuh energy sama sekali.
- Response sukses `200`: sama seperti `GET /letters/{id}` (`LetterDetailResponse`) plus `energy_remaining`.
- Response `400` kalau energy kurang — bukan cuma pesan string, ada `meta.required`/`meta.available` biar client nggak perlu re-fetch `/auth/me` buat nampilin "butuh 20, sisa 10":
  ```json
  { "success": false, "error": "not enough energy", "meta": { "required": 20, "available": 10 } }
  ```
- **Belum diselesaikan (produk, bukan teknis):** cara user dapetin energy (daily claim? beli paket via RevenueCat?) belum diputuskan — endpoint ini cuma nyediain sisi "spend"-nya. Starting balance sementara `100` (migration `018_user_energy.sql`), murni biar bisa di-test, bukan angka final.

---

## Postal Collection Showcase

*Status: ✅ dibangun (2026-08-17).* Ganti section "Postal Collection" lama di `ProfileWidgets.kt` (`UnsealedProfileContent`) yang selama ini nampilin katalog statis (`StampDesign`/`PaperTemplate` enum lokal, bukan data user) — sekarang beneran nampilin amplop & surat yang **sudah dikirim** user, dan bisa dilihat orang lain juga di public profile mereka.

### Kenapa `visibility` jadi gerbang privasi

`composite_image_url` itu gambar surat yang isinya **sudah ke-render** (bukan desain kosong) — kalau surat otomatis publik, isi korespondensi pribadi bocor ke siapapun yang buka profil. Makanya:

- `visibility` **selalu default `"private"`** di `POST /letters` — surat cuma jadi kandidat showcase kalau sender secara sadar toggle publik di compose screen (client sudah bikin toggle-nya, ada dialog konfirmasi yang jelasin konsekuensinya sebelum nyala).
- Surat `visibility == "private"` **tidak pernah** muncul di showcase manapun, titik.

### State kedua: `showcase_status` (independen dari `visibility`)

```
showcase_status: "visible" | "paused" | "deleted"   -- default "visible"
```

Cuma relevan (dan cuma valid diubah) kalau `visibility == "public"`. Ini yang dikontrol lewat menu "···" di tiap item showcase (Pause / Delete) — **terpisah** dari `visibility` supaya user bisa pause/resume tanpa harus ubah keputusan publik/privat surat itu sendiri.

Migration: `db/migrations/020_letters_showcase.sql` — `ALTER TABLE letters ADD COLUMN showcase_status VARCHAR(10) NOT NULL DEFAULT 'visible'`, plus index `(sender_id, visibility, showcase_status)` buat query showcase di bawah.

### `GET /api/v1/users/{id}/showcase`

Dipanggil dari profile sendiri **maupun** public profile orang lain. Cursor-paginated, pola sama kayak `GET /letters/sent`.

Filter: `sender_id = {id} AND visibility = 'public' AND showcase_status = 'visible'`.

Kalau `{id}` itu akun yang udah **delete akun** (`users.status == "deleted"`) — balikin `items: []` (200 kosong), **bukan** `404`. Beda perlakuan dari `GET /users/{id}` (yang 404, lihat `docs/be/profile_api.md`) karena ini list endpoint, sama filosofinya kayak `threadId` yang gak ada histori di `/mailbox/{threadId}` (array kosong, bukan error) — di praktiknya juga gak akan pernah kepanggil sendirian karena FE bakal 404 duluan pas fetch profil sebelum sempat manggil showcase-nya.

Response — sengaja **exclude** `recipient_id`/`dear_name`/`body_text` mentah (identitas penerima bukan urusan publik; `body_text` sudah terwakili lewat gambar kalau sender emang setuju publish):
```json
{
  "items": [
    {
      "id": "letter_...",
      "envelope": "ENVELOPE_2",
      "envelope_composite_image_url": "https://...",
      "paper_template": "AGED_KRAFT",
      "composite_image_url": "https://...",
      "stamp": "OCEAN",
      "posted_at": "2026-08-10T09:00:00Z"
    }
  ],
  "next_cursor": "..."
}
```

### Update (2026-08-18): `body_text` perlu ditambahkan, conditional kalau `composite_image_url` null

**Gap yang ditemukan:** kebijakan exclude di atas asumsinya `composite_image_url` **selalu** ada buat surat `public` — asumsi itu keliru buat sender di device low-density (lihat catatan hybrid `composite_image_url` di `POST /letters` atas, & `docs/be/device_capability_tiering.md`), yang skip compositing sama sekali (`composite_image_url` **dan** `envelope_composite_image_url` dua-duanya `null`). Buat sender kayak gitu, item showcase-nya gak punya gambar apapun buat direpresentasiin — client (`PostalCollectionCard`) jatuh ke fallback art generik (`envelope`/`paper_template` bundled drawable) yang sama persis buat SEMUA surat sender itu, jadi gak ada cara bedain surat satu sama lain atau nunjukin isinya sama sekali.

**Perubahan yang diminta:** include `body_text` di response `GET /users/{id}/showcase` **dan** `GET /letters/me/showcase`, tapi **cuma** saat `composite_image_url == null` (bukan wholesale ke semua item) — kebijakan lama ("gambar udah cukup representasi, gak perlu bocorin teks mentah") tetap berlaku buat kasus normal (composite ada), ini cuma nutup gap fallback-nya. Client bakal render `body_text` di atas `paper_template` texture pas `composite_image_url` null, sama pola kayak `OpenLetterOverlay`'s fallback buat surat seed admin.

Response shape (kasus `composite_image_url` null):
```json
{
  "id": "letter_...",
  "envelope": "ENVELOPE_2",
  "envelope_composite_image_url": null,
  "paper_template": "AGED_KRAFT",
  "composite_image_url": null,
  "body_text": "...",
  "stamp": "OCEAN",
  "posted_at": "2026-08-10T09:00:00Z"
}
```
`body_text` **tidak usah** dikirim (atau `null`/di-omit) kalau `composite_image_url` non-null — tetap ikut kebijakan lama buat kasus itu.

### `GET /api/v1/letters/me/showcase?status=all|visible|paused|deleted`

Owner-only (token harus match `sender_id`). Beda dari endpoint di atas: **tidak** difilter `showcase_status = 'visible'` — dipakai buat layar "kelola showcase" biar user bisa lihat & unpause item yang lagi disembunyikan.

### `PATCH /api/v1/letters/{id}/showcase`

Action dari menu "···" di tiap item showcase.
```json
{ "status": "paused" }
```

Validasi:
- `403` kalau requester bukan `sender_id` surat itu.
- `400` kalau `visibility != "public"` (nggak ada showcase buat surat private).
- `status` harus salah satu dari `visible|paused|deleted`.

Response `200`: item showcase yang sudah diupdate (shape sama kayak item `GET /users/{id}/showcase` + `showcase_status`).

**Penting — "Delete" di sini bukan hard-delete:** cuma ubah `showcase_status = 'deleted'`. Surat asli & thread mailbox (termasuk salinan si penerima) **tetap utuh** — keputusan produk biar aman, nggak destructive ke korespondensi yang sudah ada. Jangan bikin ini beneran `DELETE FROM letters`.

**Implementasi:** `app/module/letter/{repository,service,handler}` — `GetShowcase`/`GetMyShowcase`/`SetShowcaseStatus`. `GET /users/{id}/showcase` di-mount dari **letter module**, bukan auth module (entity-nya letter), lewat `router.Group("/users")` kedua yang di-daftarkan terpisah di `letter.go` — jalan berdampingan sama `GET /users/{id}` (auth module) tanpa konflik karena path segment-nya beda (`:id` vs `:id/showcase`). `error` shape validasi (403/400) ikut pola `apperr`/`response.Error` yang sama kayak endpoint lain.

---

## Roadmap (belum dibangun)

- **🐛 Bug, bukan roadmap sebenernya, tapi butuh BE fix:** `GET /mailbox/{threadId}` gak mark-as-read (`read_at` gak pernah keubah), akibatnya badge unread di Android nav bar stuck gak turun walau surat udah dibaca. Spec lengkap fix-nya ada di section "Bug (2026-08-25)" di atas, tepat di bawah dokumentasi `GET /mailbox/{threadId}`. FE (Android) udah siap, tinggal nunggu BE deploy.
- Reply pre-fill nav-arg plumbing dari Open Letter Screen ke Compose — ini kerjaan Android murni (ViewModel/nav-arg), bukan backend.
- Report / Add Friend / Block / Open Early — masih `ComingSoonBottomSheet` di client, butuh tabel & endpoint baru kalau mau diimplementasi (di luar scope M4).
- Read-receipt granular (kapan dibaca, bukan cuma unread/read binary).
- Cara dapetin `energy` (daily reward claim / beli paket RevenueCat) — lihat catatan di `POST /letters/{id}/unlock` di atas.
- `recipient_id`/`recipient_name` belum ke-expose di `LetterListItemResponse` (`GET /letters/sent`) **maupun** `LetterDetailResponse` (`GET /letters/{id}`) — struct-nya cuma punya `sender_id`/`sender_name`. Buat tab "Sent" ini nggak guna (sender = viewer sendiri), tapi buat `GET /letters/{id}` ini beneran motong fitur: delivery-tracking map jadi gak bisa nunjukin nama penerima pas viewer = sender (lihat gap detail di section `GET /letters/{id}` di atas). Perlu denormalize `recipient_nickname` di `entity.Letter` (kayak `sender_nickname`) & expose di kedua response kalau mau dibenerin di backend.
