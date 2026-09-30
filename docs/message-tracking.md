# Unsealed — Letter Delivery Tracking

*"In Transit" map · Peni + peta dunia · Jetpack Compose + MapLibre*

---

## Status: Implemented (client-side), map pivoted to MapLibre

Dokumen ini adalah catatan **as-built**, bukan spec aspirasional lagi — versi lama (Canvas + PNG statis) sudah disupersede sepenuhnya. Kalau ada mismatch sama kode, kode yang benar.

- **Tidak ada endpoint/field BE baru** untuk fitur ini sendiri. `TransportMode` (WALK/SWIM, cuma 2 mode) dihitung 100% di client dari continent yang sudah ada di tiap DTO letter/mailbox — lihat `TransportMode.kt`. Satu-satunya perubahan BE yang diminta: formula durasi `estimated_arrival_at` jadi 2-tier (24h/72h) — lihat `docs/be/letters_api.md`.
- **Progress bar murni client-side time math**, bukan websocket — `DeliveryProgress.kt`'s `calculateProgress()`. Status `IN_TRANSIT → DELIVERED` dicek via **REST polling ringan** (`GET /letters/{id}` tiap 60 detik, hanya selagi `DeliveryTrackingScreen` di foreground — lihat `DeliveryTrackingViewModel`), bukan push/websocket.
- **Peta dunia sekarang real MapLibre map** (`org.maplibre.gl:android-sdk`), bukan `Canvas` + PNG statis seperti draft awal — lihat bagian "Peta: MapLibre + GeoJSON lokal" di bawah. User bisa pan/zoom peta sendiri.
- **Rute = great-circle asli**, bukan garis lurus atau bow Bezier palsu — dihitung dari lat/long continent centroid sungguhan (`GreatCircle.kt`).
- **Garis rute dashed** (`lineDasharray`), bukan solid — kesan "jejak", bukan garis peta biasa.
- **Pin start/end punya label teks** (nama + continent, mis. "Karin · Europe"), bukan cuma titik polos — `SymbolLayer` per pin, baca properti `label` dari `GeoJsonSource` yang sama dengan pin-nya. Fallback ke continent-text-only kalau nama nggak diketahui (lihat gap di bawah). Belum ada foto profil di pin — diputuskan scope-nya text-only dulu karena data foto recipient belum konsisten tersedia.
- **Dua entry point**, satu shared composable (`DeliveryRouteMap.kt`):
  - `LetterSentScreen` (baru dikirim) — 0% progress, entrance animation penuh, tanpa network fetch (pakai `estimated_arrival_at` dari `SendLetterResponse` + `sentAt = now()`).
  - `DeliveryTrackingScreen` (dari Mailbox thread, lihat `OutgoingTrackingContent.kt`) — full-screen map, live progress, polling, draggable collapsible info card, entrance sudah settled (tidak diulang).
- **Marker punya custom art & dinamis** — `peni_mail_walk.xml`/`peny_swim.xml`, di-render sebagai `SymbolLayer` icon yang otomatis switch tergantung marker lagi di darat atau laut (real hit-test ke peta yang di-render, bukan cuma tinting warna lagi). Caption roaming di atas marker (nama negara real kalau di darat, cute line random kalau di laut) — lihat bagian "Marker dinamis" di bawah. Peni-expression-swap (mabuk laut dll, beda dari ini) belum dikerjakan.
- **World Activity Map** (agregat global, lihat bagian Post-MVP) belum dikerjakan.

---

## Peta: MapLibre + GeoJSON lokal (bukan tile server)

MapLibre Native cuma *rendering engine* — dia butuh data buat digambar, tapi Unsealed cuma butuh bentuk benua level-kasar (bukan navigasi/jalanan), jadi **tidak pakai tile server / API key sama sekali**:

- `Style.Builder()` kosong dibangun runtime di `DeliveryRouteMap.kt`, layer-nya semua kita definisikan sendiri di atas satu asset lokal.
- Bentuk benua: `app/src/main/assets/world_countries.geojson` — Natural Earth 110m (`ne_110m_admin_0_countries.geojson`), **public domain**, ~820KB, dibundle di APK, di-load via `asset://world_countries.geojson`. Tiap feature punya properti `CONTINENT` yang cocok persis sama `PenpalRegion.apiLabel` (Africa/Asia/Europe/North America/Oceania/South America/Antarctica).
- Kenapa bukan `georgique/world-geojson` (opsi awal yang di-link user): itu GPL-3.0 (risiko lisensi buat app komersial) dan granularitas per-negara, bukan per-continent — Natural Earth dipilih setelah opsi ini di-flag & disetujui.
- Layer yang di-render (semua dari `GeoJsonSource` lokal, zero network call):
  - `BackgroundLayer` — warna laut flat.
  - `FillLayer` + `LineLayer` dari `world_countries.geojson` — isi benua + border tipis, warna Letterly (cream fill, gold-tan border).
  - `LineLayer` rute — `GeoJsonSource` kosong di awal, di-update tiap frame dari `sampleGreatCircle()` sesuai reveal animation / live progress.
  - `CircleLayer` × 3 — start pin, end pin, marker posisi berjalan. Warna marker ikut `TransportMode` (gold/ice-blue/gold-deep).
- Kamera: `LatLngBounds.Builder().include(start).include(end)` di-fit otomatis begitu style selesai load. Saat `animateEntrance = true` (LetterSentScreen), kamera mulai zoom-in di titik pengirim lalu `easeCamera` melebar ke fit-bounds — saat `animateEntrance = false` (DeliveryTrackingScreen) langsung ke fit-bounds view. Gesture pan/zoom default MapLibre **tidak dimatikan** — user bebas geser/zoom sendiri.
- Reduced-motion (`rememberIsReducedMotion()`): kamera langsung `moveCamera` (no ease), garis rute langsung final state (no reveal animation), idle pulse dimatikan.

### Rute: great-circle interpolation asli

`GreatCircle.kt` — `greatCircleInterpolate(start, end, fraction)` pakai formula spherical-interpolation standar (haversine central angle + intermediate-point formula), bukan Bezier bow yang di-fake. `sampleGreatCircle()` sampling ~48 titik dari start sampai fraction tertentu buat bikin `LineString` yang melengkung beneran secara geodesik — ini alasan rute pesawat kelihatan melengkung di peta datar, dihitung sungguhan karena bumi bulat.

### Continent centroid

`ContinentCentroids` di `DeliveryRouteMap.kt` — lat/long approximate per `PenpalRegion`, dipakai sebagai titik start/end (bukan lokasi user sebenarnya, cuma representasi visual per-continent).

### Marker dinamis: icon walk/swim + caption roaming, berdasarkan posisi real

Marker (titik yang bergerak sesuai progress) sekarang pakai `SymbolLayer` dengan icon asli (`drawable/peni_mail_walk.xml` / `drawable/peny_swim.xml`, di-register ke style sebagai bitmap via `Style.Builder.withImage`), bukan `CircleLayer` polos — dan iconnya **berubah otomatis** tergantung marker lagi di darat atau laut:

- Tiap kali posisi marker berubah (`updateMarkerPosition` di `DeliveryRouteMap.kt`), posisinya di-project ke screen point (`map.projection.toScreenLocation`), lalu di-query beneran ke `WorldFillLayerId` yang lagi di-render (`map.queryRenderedFeatures`) — bukan hitung ulang geometri sendiri, tapi nanya langsung ke peta "ada daratan di titik ini nggak".
- Kalau kena daratan → icon `peni_mail_walk`, caption di atas marker = nama negaranya (ambil dari properti `ADMIN` bawaan Natural Earth di `world_countries.geojson`, mis. "Lagi lewat Jepang").
- Kalau nggak kena apa-apa (laut) → icon `peny_swim`, caption = random dari `string-array letter_tracking_ocean_captions` (5 baris cute, teks lokal Indonesia di `values-id`, English di `values`).
- Caption-nya sendiri `SymbolLayer` teks terpisah (`CaptionLayerId`), nempel di source yang sama dengan marker, fade in/out bareng animasi pop-in marker yang sudah ada.
- **Catatan keterbatasan**: karena progress di `DeliveryTrackingScreen` cuma di-update tiap polling 60 detik (bukan animasi kontinu), marker + caption-nya juga cuma "loncat" tiap update itu, bukan bergerak halus terus-menerus. Ini bukan bug baru, cuma konsekuensi dari desain progress yang sudah ada.
- **Belum tervalidasi visual** — build-only per instruksi user, jadi rendering asli icon/caption di device belum dicek langsung.

---

## Transportasi per Route

Cuma 2 mode — nggak ada pesawat kertas lagi, biar sesuai sama 2 asset custom yang ada (`drawable/peni_mail_walk.xml`, `drawable/peny_swim.xml`):

| Dari → Ke | Transportasi | Durasi (diminta ke BE) | Vibe |
|---|---|---|---|
| Same continent | Peni jalan kaki nenteng amplop 🚶 | 24 jam | Lucu, dekat banget |
| Beda continent | Peni berenang 🏊 | 72 jam | Klasik, effort nyebrang |

Logic pemilihan transportasi — `TransportMode.kt`, `transportModeFor(sender, recipient)`:

```kotlin
enum class TransportMode { WALK, SWIM }

fun transportModeFor(sender: PenpalRegion, recipient: PenpalRegion): TransportMode =
    if (sender == recipient) TransportMode.WALK else TransportMode.SWIM
```

**BE change yang masih pending** (lihat `docs/be/letters_api.md`): formula `estimated_arrival_at` masih flat 2-tier (+6h same continent / +24h beda continent) — perlu diganti jadi 24h/72h sesuai tabel di atas biar transport mode kerasa masuk akal. Ini murni ganti angka, bukan perubahan schema.

---

## Progress Calculation

`DeliveryProgress.kt` — linear interpolation antara `sent_at` dan `estimated_arrival_at`, dihitung ulang tiap kali dibutuhkan (tidak perlu timer terus-menerus, cukup wall-clock math):

```kotlin
fun calculateProgress(sentAtMillis: Long, estimatedArrivalAtMillis: Long, nowMillis: Long): Float {
    val total = estimatedArrivalAtMillis - sentAtMillis
    if (total <= 0L) return 1f
    val elapsed = nowMillis - sentAtMillis
    return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}
```

`DeliveryTrackingViewModel` manggil ini tiap kali polling (60 detik sekali, hanya saat layar foreground) berbarengan dengan cek status `IN_TRANSIT → DELIVERED` dari `GET /letters/{id}`.

---

## Flavor Text Dinamis

String resource: `letter_tracking_flavor_*` di `strings.xml`/`values-id/strings.xml`. Dipilih via `flavorTextFor(transportMode, progress)`.

### Jalan kaki — Walk (same continent)
| Progress | Teks |
|---|---|
| 0–20% | Peni baru berangkat, semangatnya penuh! 🚶 |
| 20–50% | Jalan sambil nenteng amplop, humming |
| 50–80% | Setengah jalan, Peni mampir dulu ☕ |
| 80–100% | Hampir sampai! Peni udah keliatan gang-nya 👀 |

### Berenang — Swim (beda continent)
| Progress | Teks |
|---|---|
| 0–20% | Peni nyebur, amplopnya dijagain tetap kering, bon voyage! 🏊 |
| 20–40% | Berenang santai, gerakannya mantap |
| 40–60% | Ombak lumayan besar, Peni mulai capek 🥱 |
| 60–80% | Sudah kelihatan daratan di kejauhan! |
| 80–100% | Terdampar di pantai, hampir sampai! 🏖 |

---

## File Map

| File | Peran |
|---|---|
| `ui/screens/tracking/state/TransportMode.kt` | `TransportMode` enum + `transportModeFor()` |
| `ui/screens/tracking/state/DeliveryProgress.kt` | `calculateProgress()`, `flavorTextFor()`, ISO-8601 parsing |
| `ui/screens/tracking/state/GreatCircle.kt` | Geodesic interpolation buat garis rute melengkung |
| `ui/screens/tracking/state/DeliveryTrackingUiState.kt` | Sealed state buat `DeliveryTrackingScreen` |
| `ui/screens/tracking/viewmodel/DeliveryTrackingViewModel.kt` | Polling `GET /letters/{id}` tiap 60 detik |
| `ui/screens/tracking/screen/DeliveryTrackingScreen.kt` | Full-screen map + draggable info card, entry dari Mailbox |
| `ui/screens/tracking/widgets/DeliveryRouteMap.kt` | Shared MapLibre map — dipakai `LetterSentScreen` & `DeliveryTrackingScreen` |
| `ui/screens/selectrecipient/screen/LetterSentScreen.kt` | Post-send confirmation, map di 0% + entrance animation |
| `ui/screens/inbox/widgets/OutgoingTrackingContent.kt` | Row "on its way, tap to track" di Mailbox thread buat surat yang viewer kirim |
| `ui/screens/inbox/state/LetterDeliveryStatus.kt` | `OUTGOING_IN_TRANSIT` — status baru buat bedain arah surat (dulu semua "IN_TRANSIT" ketimpa jadi satu) |
| `app/src/main/assets/world_countries.geojson` | Bentuk benua, Natural Earth 110m, public domain |

---

## Cara Akses Screen

1. **Setelah kirim surat** — `LetterSentScreen` muncul otomatis (nav route `LetterSentRoute`, bawa `estimatedArrivalAt` + `recipientContinent`).
2. **Dari Mailbox thread** — row surat yang viewer kirim & masih `IN_TRANSIT` menampilkan `OutgoingTrackingContent` ("On its way to {continent}", tombol "Track") → navigate ke `DeliveryTrackingRoute(letterId, otherPartyContinent)`.
3. **Badge di Mailbox room list** — `MailboxRoomCard` menampilkan badge "In Transit" kalau surat terakhir di room itu masih `IN_TRANSIT`.

---

## Known gaps / belum diputuskan

- **Foto profil pengirim/penerima belum ditampilkan di peta** — pin label sekarang text-only (nama + continent), diputuskan begini karena data foto nggak konsisten tersedia di semua flow:
  - Sender di `LetterSentScreen` = user login sendiri → nama/`photoUrl` sudah ada via `AuthViewModel`, tapi cuma nama yang dipakai (`senderName` param di `DeliveryRouteMap`).
  - Recipient di `LetterSentScreen` (alur reply, `RecipientMode.KNOWN_USER`) → `recipientName` dari `SelectRecipientUiState` **sudah dipakai** (nav-arg baru `LetterSentRecipientNameArg`). Alur region-match (`RecipientMode.PENPAL_REGION`) tidak pernah tahu nama recipient sebelum surat terkirim → fallback continent-only.
  - `DeliveryTrackingScreen` (nge-track surat yang viewer kirim/terima) → `senderName`/`recipientName` di `DeliveryTrackingUiState.Success` diisi dari `letter.senderName` (selalu ada di DTO) buat sisi sender, dan dari profil viewer sendiri (`AuthRepository.fetchMe()`) buat sisi manapun yang merupakan viewer. **Satu-satunya kasus yang genuinely unknown:** viewer adalah sender (nge-track surat yang dia kirim sendiri) → nama recipient `null`, fallback continent-only, karena `GET /letters/{id}` cuma expose `sender_*`, bukan `recipient_*` — sudah dicatat sebagai gap di `docs/be/letters_api.md` Roadmap ("`recipient_id`/`recipient_name` belum ke-expose").
  - Foto profil (di semua kasus di atas) tetap belum di-fetch/dipakai sama sekali — kalau nanti mau avatar penuh simetris di kedua pin, butuh BE nambah `recipient_photo_url` (atau publik-profile fetch tambahan pakai `recipient_id` begitu itu ada) plus MapLibre `SymbolLayer` icon (`style.addImage` dari bitmap Coil-loaded) buat render-nya — belum dikerjakan, di luar scope pass ini.
- **Belum ada custom Peni sprite / transport icon** — marker peta masih `CircleLayer` polos, bukan ilustrasi.
- **World Activity Map** (agregat global lintas semua user) masih di tahap ide, belum ada endpoint atau UI.

---

## [Post-MVP] World Activity Map

Screen terpisah, bisa dibuka dari Home — menampilkan **aktivitas global Unsealed** (jumlah surat in-transit per pasangan continent), bukan posisi surat individual (private + berat).

Query BE (ringan, ~7×7 = 49 rows max, cache-able 5 menit):
```sql
SELECT sender_continent, recipient_continent, COUNT(*) as total
FROM letters
WHERE status = 'in_transit'
GROUP BY sender_continent, recipient_continent;
```

Endpoint baru (belum dibangun): `GET /api/v1/world/activity` →
```json
{
  "total_in_transit": 312,
  "routes": [
    { "from": "Asia", "to": "Europe", "count": 47, "transport": "swim" }
  ]
}
```

Polling 5 menit, bukan realtime websocket — data stale 5 menit masih terasa "live".

---

*Unsealed Delivery Tracking — as-built doc, update terakhir: MapLibre pivot + great-circle route.*
