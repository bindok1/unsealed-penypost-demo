 # Device Capability Tiering — Backend Handoff

Amandemen buat `docs/be/send_and_envelope_handoff.md` §1 ("`composite_image_url` selalu wajib") — bagian itu **dibatalkan**, jangan dikerjain kalau belum sempat. Dokumen ini alasannya + kontrak baru yang perlu backend ikutin.

## Kenapa berubah

Compositing (`GraphicsLayer` capture di `LetterCanvas` → flatten jadi satu WebP) ternyata device-dependent: di device dengan screen density rendah (umum di Android kelas menengah/lawas), hasil capture-nya soft sampai ke titik nggak kebaca (bukan cuma "kurang tajam"). Percobaan bikin resolusi tetap/supersampling gagal (blank output) dan belum ada cara aman buat fix tanpa on-device test loop — jadi solusinya sementara: **device di bawah threshold tertentu nggak pernah kompositing surat sama sekali**, bukan dipaksa dan hasilnya rusak.

Client-side sudah mengimplementasikan ini (`MinCompositingDensity` di `LetterCompositor.kt`, dicek terhadap `LocalDensity.current.density` device pengirim):
- Device density **cukup** → alur sama seperti sebelumnya, `composite_image_url` selalu terisi.
- Device density **di bawah threshold** → tombol Annotate/Insert Image/Add Sticker di compose toolbar tetap kelihatan tapi di-dim + tap-nya nampilin bottom sheet penjelasan (bukan fitur beneran) — jadi surat dari tier ini nggak akan pernah punya gambar/annotate/sticker. `POST /letters` dikirim dengan `composite_image_url: null`.

## Perubahan kontrak `POST /api/v1/letters`

- **Batalkan** rencana validasi `composite_image_url == null → 400`. Field ini **tetap conditionally required** seperti sebelum `send_and_envelope_handoff.md` ditulis — boleh `null`.
- Tidak ada field baru buat nandain "surat ini dari device tier rendah" — backend nggak perlu tau alasannya, cukup terima `null` apa adanya seperti dulu.
- `body_text` tetap selalu wajib & dikirim seperti biasa (tidak berubah) — ini yang jadi satu-satunya konten buat surat dari tier density rendah.

## Kenapa `GET /letters/{id}` sudah cukup, nggak butuh field baru

`LetterDetailDto` yang sudah ada (`docs/be/api_contract.md`) sudah punya `body_text` DAN `composite_image_url` sekaligus — client Inbox (`OpenLetterScreen.kt`) sekarang render salah satu:
- `composite_image_url != null` → tampilin gambar composite seperti biasa.
- `composite_image_url == null` → tampilin `body_text` sebagai teks native di atas tekstur kertas placeholder (bukan lagi blank/kosong kayak sebelumnya).

Jadi **tidak ada perubahan skema/response** yang dibutuhkan di endpoint ini — behavior lama (`compositeImageUrl` nullable, `bodyText` selalu ada) sudah persis pas buat kebutuhan baru ini.

## Update Status (26 September 2026) — RESOLVED

1. **Threshold `MinCompositingDensity`**:
   - Diturunkan dari `2.5f` ke **`1.7f`** di `CompositingCapability.kt` setelah pengujian langsung di Redmi 9A (density ~2.0f) membuktikan hasil compositing tajam dan terbaca jelas. Device 720p/HD+ kini dapat menggunakan full compositing.
2. **Dukungan `paper_url`**:
   - Backend menambahkan kolom `paper_url` (`migration 041_letters_paper_url.sql`).
   - `POST /api/v1/letters`, `GET /api/v1/penpals/feed`, dan `GET /api/v1/letters/{id}` sekarang mendukung `paper_url`.
   - Client Android sekarang me-render `AsyncImage(model = paperUrl)` pada fallback teks, baik di feed PenPals maupun Inbox/Thread viewer.

## File client terkait

- `core/util/CompositingCapability.kt` — `MinCompositingDensity = 1.7f` threshold
- `ui/screens/compose/widgets/DeviceCapabilityBottomSheet.kt` — penjelasan ke user pas fitur di-tap di device unsupported
- `ui/screens/compose/screen/ComposeScreen.kt` / `viewmodel/ComposeViewModel.kt` — `isCompositingSupported` gating + `compositeAndSaveDraftForSend`
- `ui/screens/inbox/widgets/{EnvelopePaperReveal,MailboxFullLetterViewer}.kt` — fallback kertas dinamis `paperUrl`
- `ui/screens/penpals/widgets/OpenLetterOverlay.kt` — fallback kertas dinamis `paperUrl` & deterministik template
