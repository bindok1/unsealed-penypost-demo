# iOS MVP Spec — Unsealed Companion App (Internal, 1 User)

Bukan port Android → iOS. Ini aplikasi terpisah, SwiftUI, dibangun cuma supaya kamu (tanpa HP Android) bisa ikutan pakai fitur penpal dari akun kamu sendiri lewat backend yang **sama persis** (`api.unsealed.app`, Go/Postgres/Firebase Auth) — zero perubahan backend. Sifatnya internal/personal, jadi banyak hal di Android **sengaja** dipotong di sini. Baca §1 dulu sebelum baca fitur apa aja yang masuk.

Rujukan kontrak API dipakai apa adanya dari `docs/be_updet/{letters_api,penpals_api,daily_penpal_stack,letter_send_flow,send_and_envelope_handoff,profile_api,device_capability_tiering}.md` — dokumen ini gak mengulang detail request/response lengkap, cuma nunjuk endpoint mana yang dipakai screen mana.

---

## 0. Realita deployment yang membentuk scope ini

- **Free Apple ID signing, bukan paid Developer Program ($99/tahun).** Build dari Xcode ke iPhone kamu pakai personal team gratis → app itu **expire 7 hari**, harus di-run ulang dari Xcode (kabel atau WiFi sama) buat lanjut jalan. Ini bukan bug yang perlu "difix" — kamu sendiri yang sebut ini di awal ("hrus refresh"), jadi anggap given.
- **Konsekuensi dari itu:** gak ada TestFlight, gak ada push notification production-grade (APNs remote push butuh paid account buat setup yang reliable). MVP ini pull-refresh only — buka app / tarik-refresh buat lihat surat baru, bukan notifikasi otomatis. Ini juga cocok sama constraint "cuma 1 minggu" — gak worth invest ke push infra buat app yang mati tiap 7 hari.
- **Firebase project sama dengan Android** — perlu register 1 iOS app baru di Firebase Console yang sudah ada (bundle id baru, misal `com.apps.unsealed.ios`), lalu download `GoogleService-Info.plist`. Ini **tidak** bikin akun baru — kalau login pakai kredensial yang sama (Google atau email/password yang sama), kamu masuk ke UID/data yang sama persis kayak yang kepake di Android.
- **Base URL:** pakai prod (`https://api.unsealed.app/api/v1`) langsung — biar surat yang kamu kirim/terima nyambung ke user asli/seed yang sama kayak yang Android pakai. Gak ada alasan pakai `dev` flavor di sini karena gak ada backend lokal yang perlu ditest dari iOS.

---

## 1. Yang sengaja DIPOTONG dari Android (baca ini duluan)

**`composite_image_url` boleh `null`** di kontrak backend (`device_capability_tiering.md`) — tapi itu eksis buat nampung surat **masuk** dari Android device low-density/seed admin lama, **bukan** sesuatu yang iOS pilih pakai buat surat **keluar**. iOS-nya sendiri gak punya masalah density (1 device, iPhone kamu), jadi `ComposeView` iOS **selalu** flatten+kirim `composite_image_url`, gak pernah `null` — detail lengkap di §2a.

**Letter compositor gak dipotong** — lihat §2a: pakai `PencilKit` (native iOS, bukan custom canvas dari nol kayak Android) buat tulisan tangan/gambar, **plus** paper template picker (8 asset asli) dan image insert (full parity `EditableImage.kt` Android, termasuk drag/pinch/rotate/crop-hidden/duplicate/z-order). Ketiganya masuk MVP karena PencilKit gantiin custom Annotate Mode toolbar Android (`AnnotateToolbar.kt`) dengan `PKToolPicker` bawaan sistem — budget kerjaan yang di Android abis buat bikin tool-select UI sendiri, di iOS dialihkan ke bikin paper picker + image insert biar surat iOS match visualnya sama Android. Sisanya di tabel bawah tetap dipotong:

| Fitur Android | Kenapa dipotong di MVP iOS |
|---|---|
| Sticker (badan & amplop) | Secara teknis Android sendiri cuma manggil sistem insert-image yang sama (§2a) buat sticker — begitu image insert jadi, nambah sticker relatif murah. Tapi tetap gak diminta buat MVP ini, jadi dipotong dulu. |
| Rich-text formatting per-rentang (bold/italic/size beda-beda dalam satu surat) | `body_text` iOS tetap 1 gaya seragam se-surat — beda dari Android yang punya `bodyStyleRuns` per index karakter. |
| Amplop custom (sticker di amplop, `envelope_composite_image_url`) | Beda sistem dari image-insert badan surat di atas — pilih dari katalog id polos aja (`GET /envelopes`), tanpa compositing visual amplop. |
| Font picker Google Fonts (`FontPickerSheet.kt`, download-on-demand dari katalog Google Fonts buat body surat) | Dua alasan: (1) teknis — `font_id` di-set ke default tetap, dikirim sebagai metadata doang, backend gak pakai buat render ulang; (2) brand — beda dari `paper_template`/envelope yang katalognya **fixed & sama persis di kedua platform**, Google Fonts itu katalog terbuka gede, jadi kalau dibuka bebas surat-surat bisa keluar pakai font apa aja dan lepas dari identitas visual app (`NunitoFontFamily`/`CaveatFontFamily`). Font surat tetap 1 default yang sama biar "brand style" surat konsisten, sama kayak `paper_template`/envelope yang katalognya dikunci. |
| Postal Collection Showcase, bookmarks, address book, stamp collection screen | Bukan jalur kritis buat "ikut jadi penpal" — cuma nice-to-have yang bisa nyusul kalau app ini ternyata dipakai lebih dari seminggu. |
| Block/Report UI | Kalau kamu cuma testing sama seed accounts / akun kamu sendiri, resiko rendah. Kalau kamu berinteraksi sama user asli lain, ini perlu di-reconsider — lihat §8. |
| Energy spend UI (`unlock` instant-delivery) | `energy` tetap ditampilkan read-only (dari `GET /auth/me`), tapi tombol "unlock" gak dibangun — kamu tunggu delay biasa (24j/72j) selama minggu itu. |
| Push notification (`daily_stack_ready`, `letter_delivered`, dst.) | Butuh APNs + paid account. Ganti: pull-to-refresh + refresh otomatis tiap app dibuka foreground. |
| ~~Delivery tracking map (peta dunia + animasi jalan/berenang)~~ | **Supersede:** bukan dipotong total lagi — masuk MVP sebagai versi standar tanpa map engine (no MapKit/Mapbox), lihat `docs/ios-recipient-mailbox-tracking-flow.md` §3. |
| Onboarding lengkap (gender, birthday, foto, interests, bahasa, notify hour) | Cuma `nickname` + `continent` (`POST /auth/register`) yang wajib buat mulai pakai app. Field lain boleh nganggur default — bisa diisi dari Android kalau suatu saat kamu pegang lagi. |
| Loose-desk drag/scatter envelope interaction di PenPals | Itu murni Android flourish (physics drag, scatter offset) — iOS cukup `List`/`LazyVGrid` biasa, fungsinya sama (lihat surat, like, buka, reply). |

---

## 2. Fitur yang MASUK MVP

Kriteria masuk: dibutuhkan supaya kamu bisa **login, lihat surat penpal baru, balas, dan cek mailbox** — siklus inti "ikut jadi penpal".

1. **Login** — Firebase Auth, Google Sign-In *dan* Email/Password (lihat §8, perlu kamu konfirmasi kredensial akun existing kamu yang mana).
2. **Register/onboarding minimal** — `nickname` + `continent` sekali jalan (`POST /auth/register`), lompat ke app kalau `GET /auth/me` sudah `200`.
3. **PenPals feed (stack harian)** — `GET /penpals/feed` (tanpa `region` = stack harian ≤20 surat), like/unlike (`POST /penpals/feed/{id}/like`), buka detail surat, tombol Reply.
4. **Compose & kirim** — form: `dear_name`, `body_text` (textarea, min 20 karakter), pilih paper template (8 tekstur asli, match Android — §2a), insert 0+ foto yang bisa di-drag/pinch/rotate (§2a), pilih envelope id & stamp id dari katalog (`GET /envelopes`, `GET /stamps`, fallback ke id `ENVELOPE_1..5` kalau endpoint gagal), toggle `visibility` private/public, plus kanvas PencilKit opsional buat tulisan tangan/coretan (lihat §2a). `envelope_composite_image_url` tetap selalu `null` (amplop custom dipotong, §1); `composite_image_url` **selalu di-flatten+upload di-Send**, gak pernah `null` — gak ada konsep "device gak capable" kayak Android (`MinCompositingDensity`/`device_capability_tiering.md`) di iOS, karena cuma jalan di 1 device (iPhone kamu sendiri), jadi gak perlu tier fallback sama sekali. Lihat §2a.
   - **Mode A** (post publik ke region): `recipient_id = "penpal:<region>"`, `visibility: "public"`.
   - **Mode B1** (reply dari feed/mailbox): `recipient_id` = `sender_id`/`correspondent_id` yang sudah di tangan, `visibility: "private"`.
   - **Mode B2** (mulai obrolan baru random di region): `POST /penpals/match` dulu → dapat `recipient_id`, baru `POST /letters` dengan `visibility: "private"`.
5. **Mailbox** — `GET /mailbox` (list room), `GET /mailbox/{threadId}` (detail chat-style), pull-to-refresh manual + auto-refresh saat tab dibuka/app masuk foreground.
6. **Profile minimal** — tampilkan `nickname`, `continent`, `energy` (read-only) dari `GET /auth/me`, tombol Sign Out.

---

## 2a. Compose screen — kanvas, PencilKit, image insert & paper template (lengkap)

Ini gantiin `ComposeScreen.kt`/`ComposeViewModel.kt`/`LetterCanvas.kt` Android (`ui/screens/compose/`). Prinsip utamanya: `PencilKit` (`import PencilKit`, framework bawaan iOS, **bukan** dependency SPM tambahan) udah nyediain kanvas gambar/tulisan-tangan + Apple Pencil + tool picker out of the box — jadi budget kerjaan yang di Android abis buat bikin custom Annotate Mode (`AnnotateToolbar.kt`, `AnnotateCanvas.kt`, stroke-rendering sendiri) di iOS gak perlu sama sekali, dan bisa dialihkan buat bangun **image insert** + **paper template picker** biar surat iOS match visualnya sama Android.

### Kenapa gak perlu custom Annotate toolbar

Android's `AnnotateToolbar.kt` punya 5 tool button (Pen, Fine, Wave, Deco, Eraser) + color spectrum picker sendiri, semua state (`AnnotateTool`, warna, stroke width) dikelola manual di `ComposeUiState`. Di iOS, `PKToolPicker` (bagian dari PencilKit) **muncul otomatis** begitu `PKCanvasView` jadi first responder — udah ada pen/pencil/marker/eraser/color picker/undo-redo built-in dari sistem, gak perlu bikin `AnnotateTool` enum atau toolbar sendiri sama sekali. Cukup attach `PKToolPicker.shared(for: window)` ke canvas.

### Layer kanvas (bawah ke atas, `ZStack`)

1. **Paper template** — background kertas asli (lihat "Paper template picker" di bawah), bukan flat color.
2. **`body_text`** — text editor di atas paper.
3. **Image layer** — 0+ foto yang di-insert user, tiap foto instance independen (lihat "Image insert" di bawah).
4. **`PKCanvasView`** — transparan, nutupin seluruh kanvas, buat gambar/tulisan tangan.

### Paper template picker — bawa 8 asset asli Android, jangan bikin ulang

Android drop opsi flat-color paper dan cuma pakai 8 tekstur asli (`ui/theme/PaperTemplate.kt`). Convert tiap `.webp` → `.png` (`sips -s format png in.webp --out out.png`), masukin ke `Assets.xcassets`:

| Enum Android | Asset (`app/src/main/res/drawable/`) |
|---|---|
| `GRID` | `kertas_kraft.webp` |
| `LINED` | `kertas_putih_garis.webp` |
| `INDIGO` | `blue_paper_texture.webp` |
| `AGED_KRAFT` | `kertas_kraft_tua.webp` |
| `SOFT_BEIGE` | `kertas_buram_halus.webp` |
| `DOTTED` | `kertas_six.webp` |
| `PLAIN_WHITE` | `kertas_seven.webp` |
| `BLUSH_MARBLE` | `kertas_eight.webp` |

Picker: sheet dengan scroll horizontal 8 thumbnail (mirror `PaperPickerSheet.kt`'s `LazyRow`, 64×90pt, checkmark bulat di pojok kanan-bawah buat yang lagi dipilih). Nama enum di atas dikirim sebagai string `paper_template` ke `POST /letters` — metadata doang (`letters_api.md:42`, backend gak render ulang pakai ini), tapi kirim tetap penting: karena backend **gak pernah** balikin `paper_template` ke recipient di response manapun, satu-satunya cara recipient beneran lihat kertas yang kamu pilih adalah lewat `composite_image_url` (makanya flatten selalu jalan, lihat "Alur render" di bawah) — kalau `composite_image_url` sampai kosong, pilihan paper kamu gak akan pernah kelihatan di sisi penerima.

### Image insert — full parity sama `EditableImage.kt` Android

Multi-image (bukan cuma 1), tiap foto adalah instance independen:

```swift
struct ImageInstance: Identifiable {
    let id: String
    var uri: URL
    var offsetX: CGFloat = 0
    var offsetY: CGFloat = 0
    var scale: CGFloat = 1.0        // clamp 0.3...4.0 — ImageMinScale/ImageMaxScale Android
    var rotation: Double = 0        // derajat
    var cropRect: CGRect = .init(x: 0, y: 0, width: 1, height: 1)  // FullCropRect Android — uncropped
    var baseWidthPx: CGFloat = 0
    var baseHeightPx: CGFloat = 0
}
```

Interaksi (mirror `EditableImage.kt` + `ComposeViewModel.kt`'s `onImage*` functions 1:1):
- **Insert** — `PhotosPicker` (native, ganti `PickVisualMedia` Android) → foto baru masuk scale `1.0`, auto-selected, posisi tengah kanvas.
- **Drag** — `DragGesture`, geser `offsetX`/`offsetY`, di-clamp ke `±halfCanvasWidth/Height` (gak bisa didorong keluar kertas sepenuhnya) — sama kayak `onImageTransform` Android.
- **Pinch + rotate** — `MagnificationGesture` + `RotationGesture` digabung `.simultaneously(with:)`; scale clamp `0.3...4.0`, rotasi bebas.
- **Tap select** — dashed border putih muncul (`.dashedSelectionBorder()` Android) + corner resize handle (bulat gold, drag 1-jari) di pojok kanan-bawah sebagai alternatif pinch.
- **"···" action menu** (muncul cuma pas selected, mirror `ImageActionsMenu`):
  - Replace foto (`PhotosPicker` lagi, ganti `uri` di instance yang sama, posisi/scale/rotasi gak berubah)
  - Reset scale (balik ke `1.0`, posisi/rotasi gak kesentuh)
  - Duplicate (copy baru disisipkan tepat di atas z-order aslinya di array, offset +24pt biar keliatan sebagai layer baru, auto-selected)
  - Send to back / Bring to front (pindahin instance ke awal/akhir array — urutan array = urutan render `ZStack`)
  - Delete
  - Crop — ada di Android (`ImageCropOverlay`/`CropFrame`, drag 4 pojok live-resize) tapi **disembunyikan dari menu** di app yang jalan sekarang (`CropFeatureEnabled = false`, `EditableImage.kt:439`). Buat parity sama yang kamu **benar-benar lihat** di Android hari ini, skip nge-build crop dulu — nyusul kalau Android sendiri nyalain fiturnya.

Semua foto (bukan cuma yang lagi diedit) otomatis ke-capture pas flatten (lihat "Alur render"), jadi gak ada logic ekspor terpisah dari `PKCanvasView`.

**Yang tetap dipotong** (gak berubah dari revisi §1): sticker badan/amplop, rich-text per-rentang, amplop custom. Sticker secara teknis murah nyusul nanti karena Android sendiri cuma manggil `onImageInsert(catalogUrl)` — sistem yang sama persis kayak di atas — tapi tetap gak diminta buat MVP ini sekarang.

### Alur render & kirim

1. Pas user tekan Kirim: flatten seluruh tumpukan (paper + teks + foto + goresan PencilKit) jadi satu `UIImage` — pakai `ImageRenderer` (SwiftUI, iOS 16+) yang capture root `View` gabungan itu, konsepnya sama persis kayak `GraphicsLayer.captureIntoGraphicsLayer` Android (`core/util/captureIntoGraphicsLayer`), cuma API-nya native SwiftUI.
2. `UIImage` → `Data` (PNG/WebP) → **pakai ulang flow presign yang sudah ada**: `POST /storage/presign` (`send_and_envelope_handoff.md` §5) → `PUT` bytes ke `upload_url` → `public_url` hasilnya dikirim sebagai `composite_image_url` di `POST /letters`. Endpoint yang **sama persis** dipakai Android, gak ada kerjaan backend tambahan.
3. **Flatten ini selalu jalan, gak pernah di-skip** — beda dari draft sebelumnya yang nganggep ini opsional/eksperimental. Alasannya: paper template kamu pilih **selalu** ada (bukan opsional), dan backend gak pernah balikin `paper_template` mentah ke recipient, jadi tanpa flatten pilihan kertas/foto kamu gak akan pernah kelihatan di sisi penerima. Gak ada konsep "device gak capable, kirim `null`" di sini kayak Android (`device_capability_tiering.md`) — itu murni buat nutup gap density fragmentation Android low-end, gak relevan buat app yang cuma jalan di iPhone kamu sendiri.

**Kenapa AsyncImage penting di `LetterDetailView`:** surat yang kamu kirim dari iOS (compositing `ImageRenderer`) dan surat yang kamu terima dari Android (compositing `GraphicsLayer`) **sama-sama** cuma berupa `composite_image_url` biasa di response API — gak ada bedanya di kontrak. `LetterDetailView`'s `AsyncImage` (§3) itu satu render path yang dipakai buat nampilin surat dari platform manapun, konsisten dua arah.

---

## 2b. Alur login & routing state (baca ini sebelum bikin `LoginView` — ini sumber gap-nya)

**Gak ada endpoint `POST /auth/login` sama sekali di backend.** "Login" itu murni Firebase Auth SDK (client-side — Google Sign-In atau Email/Password lewat `FirebaseAuth`), gak ada REST call buat itu. Begitu Firebase bilang sukses, itu **cuma berarti kamu punya Firebase user** — belum tentu ada profil di backend. Yang mutusin kamu lanjut ke mana itu satu panggilan `GET /auth/me` sesudahnya, persis pola `AuthViewModel.checkAuthState()` di Android (`feature/auth/AuthViewModel.kt:111-153`). `POST /auth/register` juga baru dipanggil kalau `/auth/me` bilang belum ada record — bukan alternatif dari "login", tapi lanjutan sesudahnya.

### State machine (mirror `AuthUiState` Android 1:1)

```swift
enum AuthState {
    case loading
    case unauthenticated
    case needsRegister              // Firebase user ada, backend record belum ada (404)
    case needsOnboarding(User)      // backend record ada, nickname/continent masih kosong
    case authenticated(User)
    case error(String)
}
```

Bedanya dari Android: `needsOnboarding` Android ngecek `tosAcceptedAt`/`gender`/`birthday`/`interests` juga (`feature/auth/data/AuthRepository.kt:49-50`, fungsi `needsOnboarding()`); di iOS MVP cukup cek `nickname`/`continent` kosong, karena field lain sengaja dibiarkan default tanpa gate apapun (§1).

### Urutan panggilan yang benar — dari `LoginScreen.kt`/`RegisterScreen.kt`/`OnboardingScaffold.kt` sampai masuk feed

1. **App start** (setara `SplashScreen.kt` Android, walau di iOS gak perlu layar splash custom — cukup native launch screen, §3a) → cek `Auth.auth().currentUser` (lokal, **bukan** network call). `nil` → `.unauthenticated` → tampilkan `LoginView`.
2. Ada Firebase user → panggil `GET /auth/me` **satu kali**:
   - `404` → `.needsRegister` → tampilkan `OnboardingView` (form nickname + continent — ini yang gabungin peran `RegisterScreen.kt` **dan** `OnboardingScaffold.kt` Android jadi satu layar, sesuai §2 item 2).
   - `200` tapi `nickname`/`continent` kosong → `.needsOnboarding` → `OnboardingView` yang sama persis, cuma trigger-nya beda state.
   - `200` lengkap → `.authenticated` → masuk `TabView`, tab default **PenPals** (`GET /penpals/feed` — ini "penpal feed"-nya, §3 `PenPalsFeedView`).
   - `401` → sign-out diam-diam (token Firebase basi), balik ke `.unauthenticated`.
   - error lain → `.error(message)`, tampilkan banner, tetap di `LoginView`.
3. **Google/Email sign-in atau sign-up sukses** → ulangi langkah 2 (satu `GET /auth/me`). Firebase sign-in sendiri **gak pernah** langsung set `.authenticated` — selalu lewat `/auth/me` dulu buat konfirmasi backend record-nya ada.
4. **`POST /auth/register` sukses** (dipanggil dari `OnboardingView` saat state `.needsRegister`) → **jangan** fetch ulang `/auth/me`. Response body register-nya sendiri sudah `User` lengkap — pakai langsung buat mutusin next state (kalau nickname+continent yang dikirim selalu terisi, ini otomatis jadi `.authenticated`, karena di iOS gak ada field wajib lain). Ini sama persis pola Android — `register()` gak manggil `checkAuthState()` lagi (`AuthViewModel.kt:221-238`).

### Kesalahan Android yang JANGAN diulang di iOS

Di Android, `PenPalsViewModel`, `StampsViewModel`, `ProfileSettingsViewModel`, dan `DeliveryTrackingViewModel` **masing-masing manggil ulang `GET /auth/me` sendiri-sendiri** cuma buat baca field kayak `energy`/`notifyHourPref` — padahal `AuthViewModel.uiState` yang sudah di-cache punya data yang sama. Efeknya satu sesi app bisa ngehit `/auth/me` 4-5 kali padahal usernya gak berubah. Ini kemungkinan besar sumber kesan "ngaco ngehit auth me terus".

Di iOS: taruh hasil `GET /auth/me` di **satu** `AuthViewModel: ObservableObject` (`@Published var state: AuthState`), inject sebagai `@EnvironmentObject` ke seluruh `TabView`. `ProfileView` baca `nickname`/`continent`/`energy` dari `state`-nya situ, **bukan** manggil `/auth/me` lagi sendiri. Yang boleh nge-refetch cuma pull-to-refresh manual (§2 item 5) atau balik ke foreground — bukan tiap kali pindah tab/layar.

### Struktur tab (SwiftUI `TabView`, 3 tab — bukan 5 kayak Android)

```
[ PenPals ]   [ Mailbox ]   [ Profile ]
```

`Write`/Compose dan `Stamps` di Android jadi tab tersendiri — di iOS MVP, **Compose** cukup jadi tombol "+" di toolbar PenPals & Mailbox (buka sebagai sheet), dan **Stamps** (katalog koleksi) dipotong total (lihat §1) karena bukan jalur kritis.

---

## 3. Screens (SwiftUI, MVVM — `ObservableObject` + `@Published`, mirror pola `StateFlow` Android tapi lebih flat)

| View | Endpoint dipakai | Catatan |
|---|---|---|
| `LoginView` | Firebase Auth SDK (bukan REST) → lalu `GET /auth/me` (lihat §2b, **bukan** endpoint login) | Google Sign-In button + Email/Password form, mirror `LoginScreen.kt` tapi tanpa animasi custom (Canvas sparkle, dsb) — cukup 1 layout statis. Sukses sign-in **tidak** langsung masuk main app — nunggu hasil `GET /auth/me` dari `AuthViewModel` shared (§2b) buat tau lanjut ke `OnboardingView` atau `TabView`. |
| `OnboardingView` | `POST /auth/register` (state `.needsRegister`) — dipakai juga buat state `.needsOnboarding`, gak perlu 2 layar terpisah | Gabungan peran `RegisterScreen.kt` + `OnboardingScaffold.kt` Android jadi 1 layar, 2 field (nickname text field, continent picker 7 opsi). Trigger & routing sesudahnya lihat §2b. |
| `PenPalsFeedView` | `GET /penpals/feed`, `POST /penpals/feed/{id}/like` | `List` biasa, satu row per surat: nama pengirim, region, snippet `body_text`, like count/heart button. Tap → `LetterDetailView` sebagai sheet/push. |
| `LetterDetailView` | (data sudah di tangan dari item feed/mailbox, gak perlu fetch ulang) | `composite_image_url` **hampir selalu ada** (§2a — iOS selalu flatten di-Send), render pakai `AsyncImage`. `composite_image_url == null` cuma buat surat seed admin lama atau surat lama Android dari device low-density (`device_capability_tiering.md`) — fallback-nya tampilkan `body_text` di atas 1 tekstur `paper_template` default (bukan yang dipilih sender asli, karena field itu emang gak dibalikin ke recipient manapun, §2a). Tombol Reply → buka `ComposeView` dengan `recipient_id` prefilled. |
| `ComposeView` | `GET /envelopes`, `GET /stamps`, `POST /penpals/match` (Mode B2), `POST /storage/presign` (selalu, tiap Send — §2a), `POST /letters` | Form sheet: dear name, body text editor, paper template picker (8 tekstur asli match Android), 0+ foto insertable (drag/pinch/rotate/z-order/duplicate — full parity `EditableImage.kt`), `PKCanvasView` transparan di atas semuanya (tool picker native, gak perlu UI sendiri), envelope picker (thumbnail asli, lihat §3a — bagian penting dari alur balas, bukan dekorasi), stamp picker (6 color swatch, sama pola fallback kayak Android), segmented control private/public. Detail lengkap kanvas & image insert di §2a. |
| `MailboxListView` | `GET /mailbox` | `List` per `correspondent_id`, badge `unread_count`, preview `last_body_text`, status pill IN_TRANSIT/DELIVERED. |
| `MailboxThreadView` | `GET /mailbox/{threadId}` | Chat bubble kiri/kanan berdasar `sender_id == viewer`, urut `sent_at` ASC. **Known limitation** (bukan sesuatu yang perlu kamu fix di iOS): endpoint ini belum mark-as-read di backend (`letters_api.md` §"Bug 2026-08-25") — badge unread mailbox mungkin gak turun walau kamu udah baca. Bukan bug iOS, sama kayak Android sekarang. |
| `ProfileView` | Firebase `signOut()` saja — `nickname`/`continent`/`energy` dibaca dari `AuthViewModel.state` yang sudah di-cache (§2b), **bukan** manggil `GET /auth/me` sendiri | Read-only info + sign out. |

---

## 3a. Assets yang dibawa dari Android

Sumbernya `app/src/main/res/drawable/`. Keputusan: **envelope + paper template + mascot + region icon** yang dibawa — sisanya sengaja ditinggal, konsisten sama §1.

| Asset Android | Dibawa? | Kenapa |
|---|---|---|
| `envelope_1.webp` ... `envelope_5.webp` | ✅ **Ya, wajib** | Ini yang dilihat di envelope picker `ComposeView` — bagian dari alur **balas surat**, bukan dekorasi. `envelope` id tetap dikirim ke `POST /letters` walaupun `envelope_composite_image_url` di-null-in (§1), jadi picker-nya perlu keliatan kayak amplop beneran, gak cukup ikon generik. Convert `.webp` → `.png` (`sips -s format png in.webp --out out.png`) lalu drag ke `Assets.xcassets` sebagai image set biasa. |
| `kertas_kraft.webp`, `kertas_putih_garis.webp`, `blue_paper_texture.webp`, `kertas_kraft_tua.webp`, `kertas_buram_halus.webp`, `kertas_six.webp`, `kertas_seven.webp`, `kertas_eight.webp` | ✅ **Ya, wajib** (revisi — sebelumnya dipotong) | 8 asset ini persis `PaperTemplate` enum Android (`ui/theme/PaperTemplate.kt`) — dipakai buat paper picker `ComposeView`, lihat §2a. Convert `.webp` → `.png` sama kayak envelope. `bg_texture.webp` (wood-desk background di luar kanvas, bukan kertas) **tetap tidak dibawa**. |
| Stamp images | ❌ Gak ada yang perlu dibawa | Android sendiri gak punya bundled stamp image — fallback-nya cuma **6 warna hardcoded** (`StampPickerOverlay.kt`) kalau `GET /stamps` gagal. iOS tiru pola yang sama: 6 `Color` swatch sebagai fallback, sisanya render `image_url` dari API lewat `AsyncImage`. |
| `docs/design-assets/mascot-source.svg` | ✅ Ya | Master vector Peni — generate `AppIcon.appiconset` dari sini (bukan gambar ulang), biar app icon iOS konsisten sama identitas Android. Bisa dipakai juga buat launch screen kalau mau. |
| `ic_region_*.xml` (7 file) | Opsional | Kecil & murah buat dibawa (convert vector → SF Symbol-style asset), tapi SF Symbols bawaan iOS (`globe.asia.australia`, dst.) juga cukup buat continent picker MVP — gak wajib convert kalau males. |
| `device.webp`, `ic_fish`, `ic_instagram`, `ic_whatsapp`, `ic_menu_book`, `ic_privacy_shield`, `ic_notification` | ❌ Tidak | Di luar scope fitur MVP (device-capability sheet, share icon, dsb — semuanya dipotong). |

---

## 4. Networking layer

- **`URLSession` + `async/await`** langsung, gak perlu Alamofire/library eksternal tambahan — cuma nambah beban dependency buat app 1-user.
- Satu `APIClient` yang inject `Authorization: Bearer <firebase_id_token>` per-request via `Auth.auth().currentUser?.getIDToken()` (refresh token otomatis ditangani Firebase SDK, sama prinsipnya kayak `AuthInterceptor` di Android).
- `Codable` DTOs, mirror shape JSON yang didokumentasikan di `letters_api.md`/`penpals_api.md`/`profile_api.md` — bukan didesain ulang. Minimal set:
  - `UserResponse` (register/me)
  - `FeedItemDto`, `LikeResponse`
  - `SendLetterRequest`
  - `MailboxRoomDto`, `MailboxMessageDto`
  - `MatchPenpalResponse`
  - `CatalogItemDto` (envelope & stamp, shape identik)
- Base URL konstan `https://api.unsealed.app/api/v1`, gak perlu flavor dev/prod switching kayak Android (cuma ada 1 target: kamu, prod).

---

## 5. Dependencies (SPM, minim sengaja)

- `firebase-ios-sdk` (`FirebaseAuth` module aja — gak butuh Firestore/Storage/Analytics/Crashlytics buat MVP ini).
- `GoogleSignIn-iOS` — **cuma kalau** akun existing kamu login via Google (lihat §8). Kalau ternyata cukup email/password, skip dependency ini sepenuhnya buat setup yang lebih cepat.
- `PencilKit` — framework bawaan iOS (`import PencilKit`), bukan SPM package, dipakai buat kanvas compositing §2a.
- Gak ada networking library tambahan, gak ada DI framework (`@EnvironmentObject`/manual init cukup buat app sekecil ini).

---

## 6. Build & signing checklist

1. Xcode project baru, bundle id unik (mis. `com.apps.unsealed.ios.internal`), target iOS 16+.
2. Firebase Console → tambah iOS app baru ke project existing → download `GoogleService-Info.plist` → drop ke project.
3. Kalau pakai Google Sign-In: tambah URL scheme `REVERSED_CLIENT_ID` (ada di plist) ke `Info.plist`.
4. Signing: Xcode → target → Signing & Capabilities → pilih personal team (Apple ID gratis kamu) → automatic signing.
5. Run ke device fisik via kabel/WiFi (bukan simulator, karena kamu perlu akun asli tersambung + push permission dialog dsb walau push gak dipakai).
6. **Reminder mingguan:** app berhenti jalan 7 hari dari tanggal install — buka Xcode, run ulang ke device buat lanjut. Gak perlu automasi apapun buat kasus 1 user ini.

---

## 7. Urutan build yang disarankan (solo, AI-assisted)

1. Xcode scaffold + Firebase SDK + registrasi iOS app di console.
2. Login (mulai dari Email/Password — paling cepat diverifikasi ujung ke ujung), lanjut Google Sign-In kalau memang dibutuhkan (§8).
3. `GET/POST /auth/me`, `POST /auth/register` + `OnboardingView` 1 layar.
4. `PenPalsFeedView` read-only dulu (list + buka detail) — pastikan surat asli dari Android (`composite_image_url != null`) render dengan benar sebelum lanjut ke fitur tulis.
5. Like button.
6. `ComposeView`, dikerjain bertahap (§2a):
   1. Mode B1 (reply, paling simpel karena `recipient_id` sudah di tangan) dengan paper template default + `ImageRenderer` flatten dari langkah awal (**bukan** `composite_image_url: null` — itu udah gak jadi jalur normal, lihat §2a) → verifikasi kirim/terima jalan end-to-end dulu pakai kombinasi paling simpel ini.
   2. Tambah paper template picker (8 asset, §2a) — paling murah karena cuma background layer + 1 field string.
   3. Tambah `PKCanvasView` + `PKToolPicker` buat tulisan tangan/gambar — gak perlu bikin tool-select UI sendiri (§2a).
   4. Tambah image insert (`PhotosPicker` → drag/pinch/rotate → "···" menu replace/duplicate/z-order/delete, §2a) — paling banyak kerjaan gesture-nya, kerjain terakhir dari 4 ini.
   5. Baru lanjut Mode A (public post) dan Mode B2 (`/penpals/match`).
7. `MailboxListView` + `MailboxThreadView`.
8. `ProfileView` + sign out.
9. Polish: pull-to-refresh, empty states, error banner sederhana (samain pola `LetterlyTopSnackbar` Android — satu banner generik di atas, bukan alert per-error).

---

## 8. Hal yang perlu kamu putuskan (bukan keputusan teknis, tapi mempengaruhi scope)

- **Metode login akun existing kamu apa — Google atau Email/Password?** Kalau akun test kamu didaftarkan lewat Google Sign-In di Android dan belum pernah set password, iOS **wajib** dukung Google Sign-In (gak bisa login pakai email/password kalau passwordnya emang gak pernah ada). Kalau kamu OK bikin akun baru khusus testing iOS pakai email/password, ini bisa disederhanain — skip Google Sign-In sepenuhnya, hemat 1 dependency + 1 setup step (§5, §6.3).
- **Target datanya prod asli (ketemu user/seed account yang sama kayak Android) atau bikin akun terpisah?** Kalau prod asli dan kamu akan reply surat dari user sungguhan (bukan cuma seed), pertimbangkan ulang apakah Block/Report beneran boleh dipotong (§1) — resikonya bukan ke kamu doang kalau kamu jadi gak bisa moderate interaksi yang gak nyaman.
- **Berapa lama sebenarnya app ini bakal dipakai?** Kalau ternyata bakal dipakai lebih dari beberapa minggu, worth reconsider beli Apple Developer Program ($99/tahun) buat dapet TestFlight (gak perlu re-run Xcode tiap minggu) — tapi itu keputusan biaya, bukan sesuatu yang perlu diputuskan sekarang buat mulai build MVP ini.
