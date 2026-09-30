# iOS Flow Spec — Select Recipient, Mailbox List, Delivery Tracking (Standard/No-Map)

*Companion doc untuk `docs/ios-mvp-spec.md` (app iOS personal, SwiftUI, backend sama persis `api.unsealed.app`). Dokumen ini nge-detail-in 3 screen yang di draft awal cuma disebut singkat/dipotong: `SelectRecipientScreen.kt`, `MailboxScreen.kt`, `DeliveryTrackingScreen.kt` (Android). Ditulis supaya AI coding agent di Xcode bisa langsung bikin View + ViewModel + networking-nya tanpa harus buka source Android.*

---

## 0. Soal MapKit — jawaban singkat sebelum masuk spec

**MapKit sendiri gratis** — framework bawaan iOS SDK, gak butuh Apple Developer Program berbayar ($99/tahun) buat dipakai. Bisa langsung `import MapKit` dan jalan di device fisik pakai personal team gratis (sama seperti constraint yang sudah didokumentasikan di `ios-mvp-spec.md` §0 — app expire 7 hari, itu soal *code signing*, bukan soal MapKit). Jadi kalau nanti mau upgrade ke peta beneran, gak ada blocker teknis/biaya dari sisi MapKit-nya.

**Tapi keputusan di dokumen ini: skip map engine sama sekali** (MapKit maupun Mapbox/MapLibre yang dipakai Android, lihat `docs/message-tracking.md`) — bukan karena gak bisa, tapi karena "bikin standar aja" lebih cocok buat scope app personal 1-user ini:
- Mapbox/MapLibre butuh API key/token + GeoJSON asset (`world_countries.geojson`, ~820KB) yang gak ada gunanya buat app yang cuma jalan di 1 iPhone.
- MapKit walau gratis tetap nambah kerjaan (custom overlay buat great-circle route, custom annotation icon yang switch darat/laut, dst — itu semua logic custom Android di `DeliveryRouteMap.kt`/`GreatCircle.kt`, bukan sesuatu yang MapKit kasih out-of-the-box).
- Hasil akhir yang dibutuhkan cuma "progress surat dalam perjalanan" — bisa dicapai 100% pakai `Shape`/`Path` SwiftUI biasa (progress track + icon berjalan), zero dependency, zero entitlement, zero network call tambahan.

Bagian §3 di bawah nge-supersede baris di `ios-mvp-spec.md` §1 yang bilang delivery tracking map "dipotong, cukup teks countdown" — sekarang statusnya **masuk MVP tapi versi standar (non-map)**, bukan full dipotong.

---

## 1. Select Recipient & Envelope Flow

### 1.1 Posisi di alur kirim surat

Di Android ini **layar terpisah** (`SelectRecipientScreen.kt`), dibuka setelah user selesai nulis isi surat di `ComposeScreen` dan tap "Send". Di `ios-mvp-spec.md` §2a/§3, langkah ini sempat digabung jadi bagian dari `ComposeView` yang sama — dokumen ini **merekomendasikan tetap dipisah jadi step ke-2** dalam alur compose (bukan digabung 1 layar), karena state-nya beda konteks (nulis isi surat vs. pilih amplop+perangko+penerima) dan Android sendiri membuktikan pemisahan ini bikin flow lebih jelas (`SelectRecipientHeader`'s title berubah "Select Recipient" → "Ready to Send" begitu semua terisi).

```
ComposeView (tulis surat, PencilKit, paper picker)
        │  tap "Next" / "Kirim"
        ▼
SelectRecipientView (envelope + stamp + recipient + Send)
        │  tap "Send" (canSend == true)
        ▼
LetterSentView (opsional, lihat §1.9) atau langsung balik ke Mailbox/Feed
```

### 1.2 State model

```swift
enum RecipientMode { case none, penpalRegion, knownUser }

struct SelectRecipientState {
    var recipientMode: RecipientMode = .none
    var selectedRegion: PenpalRegion?          // 7 region enum, lihat §1.8
    var recipientId: String?                    // set kalau .knownUser (reply flow)
    var recipientName: String?
    var recipientContinent: String?
    var isReplyFlow: Bool = false

    var selectedEnvelope: CatalogItem            // dari GET /envelopes, fallback bundled
    var selectedStamp: CatalogItem?              // dari GET /stamps
    var selectedSticker: CatalogItem?            // opsional — lihat catatan §1.5

    var isRecipientPickerOpen = false
    var initialPickerTab = 0                     // 0 = Regions, 1 = Address Book (kalau dibawa)
    var isEnvelopePickerOpen = false
    var isStampPickerOpen = false

    var isSending = false
    var sendError: String?
    var showPublicShowcaseDialog = false

    // Hasil sukses kirim — dibawa ke LetterSentView/DeliveryTrackingView
    var sentLetterId: String?
    var sentEstimatedArrivalAt: String?
    var sentVisibility: String = "private"
    var sentCompositeImageUrl: String?
}

extension SelectRecipientState {
    var hasRecipient: Bool { recipientMode != .none }
    var isReadyToSend: Bool { selectedStamp != nil }   // drives title "Select Recipient" -> "Ready to Send"
    var canSend: Bool { hasRecipient && isReadyToSend } // drives header action: "Next" pill -> highlighted "Send" button
}
```

Field `selectedSticker`/sticker picker **boleh di-skip** kalau mengikuti `ios-mvp-spec.md` §1 (sticker dipotong dari MVP) — cukup selalu kirim `envelopeStickerId: nil`. Cantumkan di sini karena `EnvelopeCard`/`EnvelopeEditToolbar` Android tetap punya slot buat itu; keputusan cut/tidaknya sama seperti keputusan compose canvas di §1 dokumen MVP, bukan keputusan baru.

### 1.3 UI layout (mirror `SelectRecipientScreen.kt`)

Top-to-bottom, `VStack` full screen, background gelap konsisten (`bg_texture`-style atau flat dark color, sesuaikan tema app):

1. **Header** — back button (leading), title (`"Select Recipient"` / `"Ready to Send"`, ganti teks begitu `isReadyToSend` flip — animasi typewriter opsional, boleh cukup crossfade biasa), trailing action:
   - kalau `!canSend`: pill "Next" (disabled/dim kalau `!hasRecipient`) → tap buka Stamp picker langsung (bukan recipient picker — di Android, "Next" muncul begitu recipient udah dipilih tapi stamp belum, jadi tap-nya nuntun ke langkah berikutnya yang belum lengkap).
   - kalau `canSend`: tombol "Send" ter-highlight (warna aksen/gold).
   - Subtitle kecil di bawah title: hint text yang juga berubah sesuai state (`"Tap to add a recipient"` vs `"Ready to send!"`).
2. **Envelope Card** (tengah, ambil sisa ruang) — preview visual amplop:
   - Background = `selectedEnvelope.imageUrl` (`AsyncImage`).
   - Kalau `hasRecipient`: tampilkan info penerima (nama + region) di badan amplop; kalau belum, tampilkan CTA kosong "Tap to select recipient" — tap di mana pun pada card (selain area stamp/sticker) → buka Recipient Picker.
   - Corner kiri-atas: alamat pengirim (nama user login + region-nya sendiri, dari `AuthViewModel.state` yang sudah di-cache — **jangan** fetch ulang `/auth/me`).
   - Stamp slot (pojok kanan-atas) — tampilkan `selectedStamp.imageUrl` kalau ada, placeholder kalau belum; tap → buka Stamp Picker.
   - Sticker slot (opsional, lihat §1.2) — tap → Sticker Picker atau `ComingSoon`.
3. **Edit Toolbar** (row bawah, selalu terlihat) — 3-4 tombol ikon: Envelope (buka Envelope Picker), Sticker (opsional), Stamp (buka Stamp Picker). Font/Text button boleh dianggap "coming soon" konsisten dengan cut compose-formatting di `ios-mvp-spec.md` §1.

### 1.4 Recipient Picker (sheet, 2 tab)

Dibuka dari tap Envelope Card kosong / tap ikon dedicated. `TabView` atau segmented control dengan 2 tab:

- **Tab "Regions"** (default, index 0) — grid/list 7 `PenpalRegion` (Africa, Antarctica, Asia, Europe, North America, Oceania, South America), tiap row: emoji + nama + jumlah penpal live dari `GET /penpals/regions` (fallback ke angka dummy per-region kalau call gagal — lihat tabel §1.8). Tap satu region:
  1. Set `recipientMode = .penpalRegion`, `selectedRegion = region`.
  2. Kalau user **belum pernah** opt-in public showcase sebelumnya (state dari draft compose): tampilkan `showPublicShowcaseDialog = true` (lihat §1.6) — auto-prompt saat pertama kali pilih region, bukan nunggu user cari toggle publish sendiri.
  3. Tutup sheet.
- **Tab "Address Book"** (index 1) — kalau dibawa ke MVP (opsional, lihat `ios-mvp-spec.md` §1 "address book" masuk daftar yang dipotong sebagai nice-to-have): list kontak yang pernah ditukar (`GET /address-book`), tap kontak → `recipientMode = .knownUser`, isi `recipientId`/`recipientName`/`recipientContinent` dari item. Kalau address book di-skip, tab ini boleh dihilangkan total dan Recipient Picker cuma jadi 1 layar Regions saja.

Reply flow (dari `LetterDetailView`'s tombol Reply) langsung set `recipientMode = .knownUser` + field terkait **sebelum** screen ini muncul (nav-arg, sama seperti Android's `SelectRecipientUserIdArg`/`SelectRecipientNameArg`/`SelectRecipientContinentArg`) — user reply gak perlu buka Recipient Picker sama sekali, cukup langsung liat nama penerima udah terisi di Envelope Card.

### 1.5 Envelope / Stamp Picker (sheet)

Sederhana — horizontal/grid scroll dari katalog:
- **Envelope**: `GET /envelopes` → list `CatalogItem` (fallback ke 5 asset bundled `envelope_1..5` kalau call gagal, lihat `ios-mvp-spec.md` §3a — asset yang sama persis harus dibawa). Tap → `selectedEnvelope = item`, tutup sheet.
- **Stamp**: `GET /stamps` → fallback 6 warna solid (`Color` swatch) kalau call gagal (pola persis Android's `StampPickerOverlay`, bukan bundled image). Tap → `selectedStamp = item`, tutup sheet — ini yang men-trigger `isReadyToSend` jadi `true`.

### 1.6 Public Showcase confirm dialog

Muncul otomatis (bukan cuma dari toggle tersembunyi) begitu user pilih region pertama kali di sesi compose ini:

```
Title: "Post this letter publicly?"
Body: penjelasan bahwa surat bakal muncul di Postal Collection Showcase publik & PenPals feed
      (lihat docs/be_updet/letters_api.md "Postal Collection Showcase")
Primary CTA "Yes, make it public" -> set draft.isPublicShowcase = true, visibility nanti "public"
Secondary CTA "No, keep private"  -> visibility tetap "private"
```

Pilihan ini tersimpan di level draft compose (persist across dialog dismiss), dipakai buat nentuin `visibility` di request kirim (§1.7).

### 1.7 Send flow — dua cabang

Tombol "Send" cuma aktif kalau `canSend == true`. Logic-nya cabang dua sesuai `recipientMode`:

**Cabang A — `.knownUser` (reply / address book):**
1. `visibility` **selalu** `"private"` — gak peduli toggle showcase (surat langsung ke satu orang gak pernah bisa jadi bentuk `"penpal:<region>"` yang disyaratkan backend buat post publik).
2. Langsung `POST /letters` dengan `recipient_id = recipientId`, `dear_name = recipientName`.

**Cabang B — `.penpalRegion`:**
1. Kalau `draft.isPublicShowcase == true`: skip resolve penpal — kirim langsung `recipient_id = "penpal:<region.apiLabel>"`, `dear_name = ""`, `visibility = "public"`.
2. Kalau `false`: resolve dulu penerima nyata:
   - Coba `POST /penpals/match` (body `{ "region": region.apiLabel }`) → dapat `recipient_id`/`recipient_name`.
   - Kalau match gagal, fallback `GET /penpals/feed?region=<region>&limit=1` → ambil `sender_id`/`sender_name` dari item pertama.
   - Kalau dua-duanya gagal (region kosong penpal-nya): tampilkan `sendError`, jangan lanjut kirim.
   - `visibility = "private"`.
3. `POST /letters` dengan hasil resolve di atas.

Kedua cabang sama-sama:
- Ambil `bodyText`/`paperTemplate`/`paperColor`/`fontId`/`compositeImageUrl` dari draft compose yang sudah diisi `ComposeView` (§2a `ios-mvp-spec.md`).
- `envelope = selectedEnvelope.id`, `stamp = selectedStamp.id`, `envelope_sticker_id = selectedSticker?.id` (atau `nil`).
- `envelope_composite_image_url` — **boleh selalu `nil`** kalau amplop custom/sticker-di-amplop di-skip (konsisten `ios-mvp-spec.md` §1's "Amplop custom ... dipotong").
- Sukses (`201`) → simpan `sentLetterId`, `sentEstimatedArrivalAt`, `sentVisibility`, `sentCompositeImageUrl` dari response; `isSending = true` (dipakai buat trigger transisi ke §1.9); clear draft compose.
- Gagal → `sendError = message`, tampilkan banner/alert, jangan clear draft.

### 1.8 Data contracts dipakai screen ini

| Panggilan | Kapan | Catatan |
|---|---|---|
| `GET /penpals/regions` | saat screen muncul | `[{region, count}]` → live count per region, fallback dummy kalau gagal (lihat tabel 7 region di `PenpalRegion` bawah) |
| `GET /address-book` | saat screen muncul / buka Recipient Picker tab kedua | opsional, lihat §1.4 |
| `GET /envelopes`, `GET /stamps` | saat screen muncul | `CatalogItemDto { id, name, image_url, is_premium }` — fallback bundled kalau gagal |
| `POST /penpals/match` | saat Send, cabang B non-public | body `{ "region": "Asia" }` → `{ recipient_id, recipient_name }` |
| `GET /penpals/feed?region=X&limit=1` | fallback kalau match gagal | ambil item pertama |
| `POST /storage/presign` + `PUT` bytes | sebelum Send, kalau ada flatten envelope composite (skip kalau amplop custom di-cut) | flow presign yang sama dipakai compose canvas |
| `POST /letters` | tap Send | lihat §1.7 buat body-nya; response `201` → `{ id, status, estimated_arrival_at }` |

7 region (`PenpalRegion`) — `apiLabel` harus persis string ini (case-sensitive) tiap dipakai di query/body:

| Region | apiLabel | Emoji |
|---|---|---|
| Africa | `Africa` | 🌍 |
| Antarctica | `Antarctica` | ❄️ |
| Asia | `Asia` | 🌏 |
| Europe | `Europe` | 🏰 |
| North America | `North America` | 🌲 |
| Oceania | `Oceania` | 🏝️ |
| South America | `South America` | 🦜 |

### 1.9 Setelah sukses kirim

Android transisi ke `SendSuccessOverlay` lalu (via callback nav) ke `LetterSentScreen` (peta 0% + entrance) sebelum balik ke Mailbox. Untuk versi iOS standar (no-map, §3), cukup:
1. Tampilkan overlay sukses singkat ("Letter sent! 💌", auto-dismiss ~1.5 detik atau tombol "Done").
2. Dismiss sheet compose sepenuhnya, kembali ke `PenPalsFeedView`/`MailboxListView` (tab asal).
3. Room baru otomatis muncul di Mailbox berikutnya kali `GET /mailbox` di-refresh (lihat §2) — gak perlu navigate paksa ke `DeliveryTrackingView` dari sini; user cukup buka room-nya dari Mailbox kalau mau lihat progress (§3.1).

---

## 2. Mailbox List Flow (`MailboxListView`)

### 2.1 Purpose & data

Pengganti Inbox lama — satu row per **lawan bicara** (`correspondent_id`), bukan per surat. Room langsung muncul di kedua sisi begitu ada 1 surat terkirim (searah manapun), termasuk yang masih `IN_TRANSIT` — user gak perlu nunggu balasan buat lihat room-nya.

```swift
struct MailboxRoom: Identifiable {
    let id: String              // correspondentId
    let correspondentName: String
    let correspondentContinent: String
    let lastActivityLabel: String   // format lokal dari last_sent_at
    let stamp: CatalogItem          // resolve last_stamp lewat stamp catalog
    let unreadCount: Int
    let lastStatus: String          // "IN_TRANSIT" | "DELIVERED"
}
extension MailboxRoom {
    var isUnread: Bool { unreadCount > 0 }
    var isLastLetterInTransit: Bool { lastStatus == "IN_TRANSIT" }
}
```

### 2.2 UI layout

`NavigationStack` + `List` (native, gak perlu custom scatter/collapse title kayak PenPals) — tapi kalau mau match nuansa Android (large title collapse ke compact-centered saat scroll), pakai `.navigationBarTitleDisplayMode(.large)` bawaan SwiftUI, itu sudah otomatis kasih behavior collapse yang sama tanpa custom scroll-offset math (`TitleCollapseThresholdPx` di Android murni workaround karena Compose gak punya native large-title, **iOS gak butuh reimplement ini**).

Tiap row (`MailboxRoomCard` equivalent):
- Leading: stamp/envelope thumbnail kecil (dari `stamp.imageUrl`, atau avatar placeholder bulat kalau mau tambah).
- Title: `correspondentName`.
- Subtitle: `correspondentContinent` + `lastActivityLabel` (mis. "Asia · 2h ago").
- Trailing: badge count bulat (`unreadCount`) kalau `isUnread`; pill kecil "In Transit" (warna gold/amber) kalau `isLastLetterInTransit`.
- Empty state: teks "No letters yet — send your first one!" kalau list kosong.
- Loading state: `ProgressView` tengah layar (skip shimmer custom, native spinner cukup buat MVP).
- Error state: teks error + tombol Retry.

### 2.3 Refresh behavior

- Fetch sekali saat `onAppear` pertama.
- Refresh silent (gak nge-reset ke Loading state) tiap kali tab ini balik ke foreground / `scenePhase == .active` — mirror Android's `DisposableEffect(lifecycleOwner)` yang manggil `loadMailbox(isSilent = true)` di `ON_RESUME`.
- `.refreshable { }` native SwiftUI buat pull-to-refresh manual — Android gak punya ini (gak ada gesture pull-refresh di Compose versionnya), tapi ini idiom iOS standar yang sepadan, **tambahkan** meski gak ada versi Android-nya.
- **Tidak perlu polling terus-menerus** di layar list ini — status per-room cukup ke-refresh pas foreground/pull-refresh, bukan tiap beberapa detik (beda dari `DeliveryTrackingView` di §3 yang emang butuh live polling karena user lagi nunggu 1 surat spesifik).

### 2.4 Navigasi dari row tap

Tap satu room → push/present `MailboxThreadView` (`GET /mailbox/{threadId}`, `threadId = correspondentId`) — chat-bubble style, `sender_id == viewer` nentuin kiri/kanan, urut `sent_at` ASC. Di dalam thread, surat yang **viewer kirim** dan masih `IN_TRANSIT` menampilkan row kecil "On its way to {continent} — Track" (mirror `OutgoingTrackingContent.kt`) → tap ini yang navigate ke `DeliveryTrackingView` (§3), bukan dari list Mailbox langsung. Detail penuh `MailboxThreadView` di luar scope dokumen ini (gak diminta), tapi wiring nav-nya penting supaya `DeliveryTrackingView` bisa dijangkau — butuh diteruskan: `letterId`, `otherPartyContinent`, dan (opsional, buat nutup gap di §3.3) `correspondentName` sebagai fallback nama.

### 2.5 Data contract

`GET /mailbox` → `data: [MailboxRoomDto]`, array langsung (bukan wrapped `items`/`next_cursor`, gak ada pagination):

```json
{
  "correspondent_id": "uid",
  "correspondent_name": "Kirana",
  "correspondent_continent": "Asia",
  "last_body_text": "...",
  "last_sent_at": "2026-08-13T09:00:00Z",
  "last_status": "IN_TRANSIT",
  "last_estimated_arrival_at": "2026-08-13T15:00:00Z",
  "last_stamp": "OCEAN",
  "last_envelope": "ENVELOPE_2",
  "last_composite_image_url": "https://...",
  "unread_count": 2
}
```

`last_stamp` di-resolve ke `CatalogItem` lewat katalog stamp yang sudah di-fetch (`GET /stamps`, fallback bundled/color swatch) — sama seperti `MailboxMapping.kt`'s `resolveItem()` Android, jangan render `last_stamp` string mentah.

---

## 3. Delivery Tracking Flow — versi standar (no map)

### 3.1 Entry point

Satu-satunya jalan masuk: row "On its way — Track" di dalam `MailboxThreadView` (§2.4), untuk surat yang **viewer kirim sendiri** dan `status == "IN_TRANSIT"`. Nav-arg yang dibawa: `letterId`, `otherPartyContinent` (continent lawan bicara di thread itu), dan sebaiknya `correspondentName` sebagai fallback nama (lihat gap §3.3).

### 3.2 Kenapa versi ini gak butuh map sama sekali

Semua yang direpresentasikan peta Android (`DeliveryRouteMap.kt`) sebenarnya cuma 3 angka + 1 enum:
- `progress: Float` (0...1, linear time-based, lihat §3.5)
- `transportMode: .walk | .swim` (WALK kalau `senderContinent == recipientContinent`, SWIM kalau beda — dihitung di client, **bukan** dari backend)
- `senderContinent` / `recipientContinent` (buat label 2 endpoint)
- `isDelivered: Bool`

Versi standar cukup render ini sebagai **progress track horizontal** — 2 lingkaran endpoint (sender/recipient) dihubungkan garis putus-putus, bagian garis yang sudah "dilewati" terisi warna aksen, dan 1 icon (🚶/🏊, atau SF Symbol `figure.walk`/`figure.pool.swim`) diposisikan di titik `progress` sepanjang garis pakai `GeometryReader` + `.offset(x:)`. Nol dependency, nol API key, nol permission (`NSLocationWhenInUseUsageDescription` dkk **tidak diperlukan** — ini bukan lokasi asli, cuma visualisasi progress relatif).

```swift
struct RouteProgressTrack: View {
    let senderLabel: String       // e.g. "Asia"
    let recipientLabel: String
    let progress: Double          // 0...1
    let transportSymbol: String   // "figure.walk" atau "figure.pool.swim"

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                // dashed track (background)
                Rectangle().fill(Color.white.opacity(0.16))
                    .frame(height: 3)
                    .overlay(DashedLine())
                // filled portion
                Rectangle().fill(Color.accentColor)
                    .frame(width: geo.size.width * progress, height: 3)
                // moving icon
                Image(systemName: transportSymbol)
                    .foregroundStyle(Color.accentColor)
                    .background(Circle().fill(.black.opacity(0.6)).frame(width: 32, height: 32))
                    .offset(x: geo.size.width * progress - 16)
            }
        }
        .frame(height: 32)
    }
}
```

Label `senderLabel`/`recipientLabel` cukup ditaruh di atas masing-masing ujung track (`HStack` terpisah di luar `GeometryReader`), animasikan posisi icon pakai `.animation(.easeInOut, value: progress)` biar tiap kali polling update progress-nya kerasa gerak halus, bukan loncat kasar.

### 3.3 State model

Mirror `DeliveryTrackingUiState.Success` Android 1:1:

```swift
enum DeliveryTrackingState {
    case loading
    case error(String)
    case success(Success)

    struct Success {
        let senderContinent: PenpalRegion
        let recipientContinent: PenpalRegion
        let transportMode: TransportMode        // .walk | .swim
        let progress: Double                     // 0...1
        let sentAtLabel: String
        let estimatedArrivalAtLabel: String
        let isDelivered: Bool
        let senderName: String?                  // nil kalau viewer = sender & BE gak expose nama lawan (lihat gap di bawah)
        let recipientName: String?
        let letterId: String
        let isViewerRecipient: Bool               // gates tombol Boost
        let viewerEnergy: Int?
    }
}

enum TransportMode { case walk, swim }
func transportMode(sender: PenpalRegion, recipient: PenpalRegion) -> TransportMode {
    sender == recipient ? .walk : .swim
}
```

**Gap yang perlu diketahui (bukan bug, dari `docs/be_updet/letters_api.md`'s roadmap):** `GET /letters/{id}` **tidak** expose `recipient_name` — cuma `sender_name`. Efeknya, kalau **viewer adalah sender** yang lagi ngetrack surat yang dia kirim sendiri, `recipientName` gak akan pernah ke-isi dari response ini (`nil`). Workaround yang sama dipakai Android: thread nav-arg `correspondentName` (dari `MailboxThreadView`, §2.4) sebagai fallback `recipientName` kalau `isFromViewer == true` — kalau nav-arg ini gak diteruskan, fallback ke label continent-only (`recipientContinent.label`) di UI, jangan crash/kosongkan section.

### 3.4 UI layout lengkap

`VStack` full screen (skip full-bleed map background — cukup warna solid gelap konsisten tema app):

1. **Header row** — back button, pill judul "Tracking", dan (kondisional):
   - Kalau `isViewerRecipient && !isDelivered`: pill "⚡ {energy}" (Boost) → tap buka confirm dialog (§3.6).
   - Kalau `isViewerRecipient && isDelivered`: pill "✉️ Open Letter" → tap buka celebration modal (§3.7).
2. **Card utama** (tengah/atas, `RoundedRectangle` background semi-transparan gelap):
   - `RouteProgressTrack` (§3.2) — label continent kiri = `senderName ?? senderContinent.label`, kanan = `recipientName ?? recipientContinent.label`.
   - Flavor text dinamis di bawah track, dari tabel §3.5.2 (pilih berdasar `transportMode` + rentang `progress`).
   - Row bawah: `sentAtLabel` (kiri) — `estimatedArrivalAtLabel` (kanan, bold aksen).
3. Kalau `isDelivered`: track penuh 100%, icon berhenti di ujung kanan, flavor text ganti jadi "Delivered!" statis, badge lock terbuka.

Gak ada draggable/collapsible widget kayak Android (itu murni existing buat gak nutupin peta full-screen) — karena di sini kartu **adalah** seluruh konten layar, gak perlu draggable/minimize sama sekali.

### 3.5 Progress & polling

**3.5.1 Kalkulasi progress** — linear time interpolation, dihitung ulang tiap kali data baru masuk (bukan timer terpisah):

```swift
func calculateProgress(sentAt: Date, estimatedArrivalAt: Date, now: Date) -> Double {
    let total = estimatedArrivalAt.timeIntervalSince(sentAt)
    guard total > 0 else { return 1.0 }
    let elapsed = now.timeIntervalSince(sentAt)
    return min(max(elapsed / total, 0), 1)
}
```

**Polling** — panggil `GET /letters/{id}` berulang selama screen ini aktif (`Task` yang di-cancel saat `onDisappear`), bukan sekali doang:
```swift
while !Task.isCancelled {
    let delivered = await refresh()
    if delivered { break }
    let interval = currentProgress >= 0.9 ? 3.0 : 10.0   // makin dekat ETA, makin sering cek
    try? await Task.sleep(for: .seconds(interval))
}
```
`isDelivered` juga dianggap `true` kalau `now >= estimatedArrivalAt` secara lokal, gak perlu nunggu response server bilang `"DELIVERED"` eksplisit (client-side time math sudah cukup, backend cuma dikonfirmasi ulang tiap poll).

Sekali di awal (bukan tiap poll): `GET /auth/me` buat ambil `viewerName`/`viewerContinent`/`viewerEnergy` — cache di memory, cuma di-refresh lokal setelah unlock sukses (§3.6), **jangan** re-fetch tiap 10 detik bareng polling letter.

**3.5.2 Flavor text** (string lokal, taruh di `Localizable.strings`, bukan hasil hardcode di kode):

| Transport | Progress | Teks |
|---|---|---|
| Walk (`same continent`) | 0–20% | "Just set off, full of energy! 🚶" |
| Walk | 20–50% | "Walking along, letter tucked in safe" |
| Walk | 50–80% | "Halfway there, taking a quick break ☕" |
| Walk | 80–100% | "Almost there! Can see the street now 👀" |
| Swim (`different continent`) | 0–20% | "Dove in, letter staying dry, bon voyage! 🏊" |
| Swim | 20–40% | "Swimming steady, strong strokes" |
| Swim | 40–60% | "Waves picking up, getting a bit tired 🥱" |
| Swim | 60–80% | "Land in sight in the distance!" |
| Swim | 80–100% | "Washed up on shore, almost there! 🏖" |

Durasi total: **24 jam** same-continent (WALK), **72 jam** beda-continent (SWIM) — ini dihitung backend (`estimated_arrival_at`), client cuma perlu tau buat pilih flavor text yang tepat dari rentang `progress`, bukan menghitung durasi sendiri.

### 3.6 Boost (unlock instant-delivery) flow

Cuma muncul kalau `isViewerRecipient == true` (recipient boleh bayar energy buat skip sisa waktu, sender gak bisa boost surat sendiri):

1. Tap pill "⚡ Boost" → confirm dialog ("Use 20 Energy to open this letter now?").
2. Confirm → `POST /letters/{id}/unlock`.
3. Sukses `200` → simpan `energy_remaining` lokal, `refresh()` ulang (letter sekarang `DELIVERED`), tampilkan celebration modal (§3.7).
4. Gagal `400` dengan `meta.required`/`meta.available` (energy kurang) → tutup confirm dialog, langsung tampilkan dialog kedua "Not enough Energy — need {required}, you have {available}" dengan CTA ke halaman beli/dapetin Energy (kalau ada di scope iOS; kalau belum ada, cukup dismiss). Pola "jemput bola" ini disengaja — jangan cuma tampilkan toast generic.
5. Gagal lain → toast error generic.

### 3.7 Delivered celebration

State terpisah (bukan bagian dari `DeliveryTrackingState`), muncul otomatis (dengan delay ~650ms setelah progress kebaca 100% biar user sempat lihat track penuh dulu) kalau `isDelivered && isViewerRecipient`, atau manual dari tap pill "Open Letter":

```
Modal fullscreen/sheet — "🎉 Letter Delivered!"
  envelope preview (dari envelopeCompositeImageUrl kalau ada, atau fallback envelope id)
  sender name
  CTA "Open Letter" -> dismiss modal, navigate ke LetterDetailView (fetch GET /letters/{id} lagi buat full body)
```

### 3.8 Data contracts dipakai screen ini

| Panggilan | Frekuensi | Catatan |
|---|---|---|
| `GET /auth/me` | 1× di awal | ambil `viewerContinent`/`viewerName`/`viewerEnergy`, cache lokal |
| `GET /letters/{id}` | polling 3-10 detik (§3.5.1) sampai delivered | field dipakai: `sender_id`, `sender_name`, `posted_at`, `estimated_arrival_at`, `delivery_status`, `envelope`, `envelope_composite_image_url` |
| `POST /letters/{id}/unlock` | sekali per tap confirm Boost | body kosong; sukses → `{ energy_remaining, ...LetterDetailResponse }`; gagal energy → `{ success:false, error, meta:{ required, available } }` |

---

## 4. Ringkasan navigasi lintas-screen

```
PenPalsFeedView / MailboxListView
        │
        ▼ (tap "+"/compose atau Reply)
ComposeView  ──"Next"──▶  SelectRecipientView (§1)
        │                        │ tap Send (sukses)
        │                        ▼
        │                 overlay sukses singkat ──▶ balik ke tab asal
        ▼
MailboxListView (§2) ──tap room──▶ MailboxThreadView ──tap "Track" (surat outgoing in-transit)──▶ DeliveryTrackingView (§3)
                                          │ tap surat delivered
                                          ▼
                                    LetterDetailView (full body)
```

---

## 5. Checklist verifikasi buat AI agent

- [ ] `SelectRecipientView`: title berubah "Select Recipient" ↔ "Ready to Send" seiring `isReadyToSend` (stamp dipilih).
- [ ] Header action pill jadi "Next" (disabled kalau belum ada recipient) lalu jadi "Send" ter-highlight begitu `canSend == true`.
- [ ] Recipient Picker: pilih region → auto-munculin Public Showcase dialog (cuma sekali per sesi compose, cuma dari jalur region — bukan reply/address book).
- [ ] Cabang kirim `.knownUser` selalu `visibility: "private"`; cabang `.penpalRegion` publik pakai `recipient_id: "penpal:<region>"` literal, non-publik resolve lewat `/penpals/match` → fallback `/penpals/feed`.
- [ ] `MailboxListView`: room baru muncul tanpa nunggu delivered (in-transit sudah kelihatan), badge unread & pill "In Transit" tampil sesuai `unread_count`/`last_status`.
- [ ] Mailbox refresh silent saat app/tab kembali foreground, plus `.refreshable` pull-to-refresh manual.
- [ ] `DeliveryTrackingView`: **tidak** ada import `MapKit`/Mapbox SDK sama sekali — cuma `Shape`/`GeometryReader` SwiftUI polos.
- [ ] Progress track terisi sesuai `progress` (0...1) dan icon transport (`figure.walk`/`figure.pool.swim`) bergerak halus (animated) tiap poll update.
- [ ] Polling `GET /letters/{id}` berhenti otomatis begitu `isDelivered == true` atau screen di-dismiss (`Task` di-cancel, gak bocor di background).
- [ ] Boost pill cuma muncul kalau `isViewerRecipient == true`; insufficient-energy dialog kasih `required`/`available` yang benar dari response `meta`, bukan pesan generic.
- [ ] `recipientName` fallback ke `correspondentName` nav-arg (bukan crash/blank) saat viewer = sender & backend gak expose nama lawan (§3.3).
