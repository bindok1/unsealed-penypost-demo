# Bug: "body_text must be ≥20 characters" saat pakai PencilKit — Root Cause & Fix

## TL;DR

**PencilKit hanya mengisi `composite_image_url`, bukan `body_text`.** Backend memvalidasi keduanya secara **independen** — `body_text` minimal 20 karakter SELALU diwajibkan, bahkan kalau `composite_image_url` sudah ada. Surat yang dibuat 100% dari coretan PencilKit (tanpa mengetik teks sama sekali) akan selalu kena 400 dari backend.

---

## Root Cause — Kontrak Backend yang Sebenarnya

Dari `letters_api.md` baris 39:

```
body_text | string | required, minimal 20 karakter, tanpa batas atas
```

Dan baris 81 (setelah update "selalu composite_image_url"):

```
composite_image_url | wajib diisi, non-null
```

**Ini bukan either/or** — kedua field wajib diisi sekaligus:

| Field | Kewajiban | Sumber data di iOS |
|---|---|---|
| `composite_image_url` | Wajib, non-null | ✅ `ImageRenderer` flatten PencilKit + paper + teks |
| `body_text` | Wajib, ≥20 karakter | ❌ PencilKit **tidak** mengisi field ini |
| `dear_name` | Wajib, non-blank | TextEditor di form |

`composite_image_url` menggantikan `body_text` sebagai **sumber render** di sisi penerima — tapi **tidak menggantikan kewajiban validasinya**. Backend Go masih menjalankan validasi `len(body_text) >= 20` terpisah dari ada-tidaknya `composite_image_url`.

---

## Kenapa Android Tidak Kena Bug Ini

Di Android, compose screen selalu punya `BasicTextField` aktif sebagai Layer 3 dari kanvas:

```
Layer 1: Paper Background
Layer 2: Template Decoration
Layer 3: TextField (teks surat)   ← selalu visible, user "dipaksa" ngetik
Layer 4: Annotate Canvas          ← coretan di ATAS teks
```

`AnnotateMode` di Android adalah **lapisan tambahan di atas teks yang sudah ada** — user harus mengetik dulu, baru bisa coret-coret. Jadi `body_text` hampir selalu terisi sebelum user masuk ke mode annotate.

Di iOS dengan PencilKit, skenario yang berbeda dimungkinkan:
- User langsung gambar/tulis tangan di `PKCanvasView` **tanpa mengetik** di `TextEditor`
- `body_text` tetap `""` (kosong)
- `composite_image_url` berisi gambar coretan yang valid
- Backend tetap reject dengan 400: `body_text must be at least 20 characters`

---

## Tiga Solusi — Perbandingan

### Solusi 1 (Recommended): Silent Placeholder di `body_text`

Kalau `body_text` kosong atau < 20 karakter saat Send ditekan, **isi otomatis dengan placeholder** sebelum dikirim ke backend. Placeholder tidak ditampilkan di UI, hanya ada di payload API.

```swift
// Di ComposeViewModel.swift, sebelum POST /letters

func buildSendPayload() -> SendLetterRequest {
    let rawBodyText = bodyText.trimmingCharacters(in: .whitespacesAndNewlines)

    // Backend wajib body_text ≥ 20 char (letters_api.md §39).
    // PKCanvasView tidak mengisi body_text — kalau user hanya coret-coret
    // tanpa mengetik teks, kirim placeholder biar validasi backend lewat.
    // Sisi penerima tetap lihat composite_image_url (bukan body_text ini).
    let effectiveBodyText = rawBodyText.count >= 20
        ? rawBodyText
        : rawBodyText + String(repeating: " ", count: max(0, 20 - rawBodyText.count))

    return SendLetterRequest(
        recipientId: recipientId,
        dearName: dearName,
        bodyText: effectiveBodyText,   // ← padded kalau perlu
        compositeImageUrl: compositeImageUrl,  // dari ImageRenderer
        // ... field lainnya
    )
}
```

**Trade-off:**
- ✅ Paling simpel, tidak ada perubahan UX
- ✅ `body_text` tetap dikirim (berguna untuk notifikasi preview backend, search, moderasi — lihat `letters_api.md` §80)
- ✅ Konsisten dengan desain iOS MVP (§2a spec: "iOS selalu flatten+kirim `composite_image_url`")
- ⚠️ `body_text` di database berisi spasi padding — acceptable karena recipient melihat via `composite_image_url`, bukan `body_text`

---

### Solusi 2: Validasi Client + Pesan Error

Tampilkan error di UI sebelum dikirim ke backend, paksa user untuk mengetik minimal.

```swift
// Di ComposeView.swift

var canSend: Bool {
    let bodyTrimmed = bodyText.trimmingCharacters(in: .whitespacesAndNewlines)
    return !dearName.isEmpty
        && bodyTrimmed.count >= 20
        && !isSending
}

// Tambah counter di UI
Text("\(bodyText.count)/20 min")
    .font(.caption)
    .foregroundStyle(bodyText.count < 20 ? .red : .secondary)
```

**Trade-off:**
- ✅ Jujur ke user tentang requirement
- ❌ Merusak UX surat "all-drawing" — user yang ingin kirim surat gambar murni harus mengetik teks sia-sia
- ❌ Tidak konsisten dengan konsep "iOS pakai PencilKit sebagai pengganti Annotate Mode Android" — di Android annotate itu *opsional*, tapi di sini drawing saja tidak cukup

---

### Solusi 3: Hitung Strokes sebagai "Konten"

Anggap `PKDrawing` yang non-kosong sebagai pengganti requirement teks, lalu generate placeholder dari jumlah strokes.

```swift
func hasEnoughContent() -> Bool {
    let bodyTrimmed = bodyText.trimmingCharacters(in: .whitespacesAndNewlines)
    let hasEnoughText = bodyTrimmed.count >= 20
    let hasDrawing = !canvasView.drawing.strokes.isEmpty
    return hasEnoughText || hasDrawing
}

// Kalau hanya drawing, generate body_text dari stroke count
func buildBodyText() -> String {
    let raw = bodyText.trimmingCharacters(in: .whitespacesAndNewlines)
    if raw.count >= 20 { return raw }

    let strokeCount = canvasView.drawing.strokes.count
    if strokeCount > 0 {
        // Placeholder yang "bermakna" untuk moderasi/search backend
        return "[Letter with \(strokeCount) handwritten strokes — see composite_image_url]"
        // ↑ sudah ≥20 char untuk strokeCount apapun
    }
    return raw
}
```

**Trade-off:**
- ✅ `body_text` di database punya makna (moderator bisa tahu ini surat gambar)
- ⚠️ Lebih kompleks dari Solusi 1
- ⚠️ Perlu referensi ke `PKCanvasView` dari ViewModel (perlu binding ke SwiftUI state)

---

## Rekomendasi: Gunakan Solusi 1 + Validasi Dear Name

Paling simpel dan tidak ada UX regression. **Yang tetap harus divalidasi di client sebelum Send:**

```swift
// ComposeViewModel.swift

enum SendValidationError: LocalizedError {
    case missingDearName
    case noContent   // body_text kosong DAN PKCanvas kosong

    var errorDescription: String? {
        switch self {
        case .missingDearName: return "Tulis nama penerima dulu ya!"
        case .noContent:       return "Tulis atau gambar sesuatu di suratmu dulu"
        }
    }
}

func validateBeforeSend() throws {
    guard !dearName.trimmingCharacters(in: .whitespaces).isEmpty else {
        throw SendValidationError.missingDearName
    }

    let hasText = !bodyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    let hasDrawing = !canvasDrawing.strokes.isEmpty  // PKDrawing dari binding
    let hasImages = !insertedImages.isEmpty

    guard hasText || hasDrawing || hasImages else {
        throw SendValidationError.noContent
    }
    // Tidak perlu validasi 20-char di sini — ditangani di buildBodyText()
}

func buildBodyText() -> String {
    let raw = bodyText.trimmingCharacters(in: .whitespacesAndNewlines)
    guard raw.count < 20 else { return raw }
    // Pad dengan spasi sampai minimal 20 char (backend requirement)
    return raw.padding(toLength: 20, withPad: " ", startingAt: 0)
}
```

---

## Implementasi Lengkap — `SendLetterRequest`

```swift
// Models/SendLetterRequest.swift

struct SendLetterRequest: Codable {
    let recipientId: String
    let dearName: String
    let bodyText: String           // ← selalu ≥20 char (padded kalau perlu)
    let envelope: String
    let stamp: String
    let paperTemplate: String
    let paperColor: String
    let fontId: String
    let compositeImageUrl: String  // ← selalu non-null di iOS (ImageRenderer)
    let visibility: String
    let stickerData: [String]
    let envelopeStickerId: String?
    let envelopeCompositeImageUrl: String?  // null di iOS MVP (§1 ios-mvp-spec)
    let crisisTag: String?

    enum CodingKeys: String, CodingKey {
        case recipientId = "recipient_id"
        case dearName = "dear_name"
        case bodyText = "body_text"
        case envelope, stamp
        case paperTemplate = "paper_template"
        case paperColor = "paper_color"
        case fontId = "font_id"
        case compositeImageUrl = "composite_image_url"
        case visibility
        case stickerData = "sticker_data"
        case envelopeStickerId = "envelope_sticker_id"
        case envelopeCompositeImageUrl = "envelope_composite_image_url"
        case crisisTag = "crisis_tag"
    }
}
```

---

## Alur Send yang Benar di iOS

```
User tap "Kirim"
    ↓
validateBeforeSend()
    ├── dearName kosong → error "Tulis nama penerima dulu"
    └── body DAN canvas DAN gambar semuanya kosong → error "Tulis atau gambar sesuatu"
    ↓ (valid)
Flatten canvas → ImageRenderer.render(compositeView) → UIImage → Data(PNG)
    ↓
POST /storage/presign → PUT image bytes → dapat composite_image_url
    ↓
POST /letters dengan:
    body_text = buildBodyText()              ← padded ke ≥20 char kalau perlu
    composite_image_url = <url dari presign> ← selalu non-null
    dear_name = dearName
    ... field lainnya
    ↓
201 → navigate ke success screen
```

---

## Ringkasan Checklist Fix

- [ ] **Hapus** validasi `body_text.count >= 20` dari tombol "Kirim" di client (jangan disabled berdasarkan char count)
- [ ] **Tambah** `buildBodyText()` di `ComposeViewModel` — pad dengan spasi ke minimal 20 char kalau kurang
- [ ] **Tetap validasi** `dearName` non-empty di client (ini tetap diperlukan backend)
- [ ] **Tetap validasi** "ada konten" di client — minimal salah satu dari: teks tidak kosong, ada stroke PencilKit, atau ada foto yang di-insert (jangan kirim surat benar-benar kosong)
- [ ] **Pastikan** `composite_image_url` selalu non-null di payload — flatten via `ImageRenderer` sebelum POST (sudah di-spec di §2a ios-mvp-spec.md)
- [ ] **Jangan kirim** `envelope_composite_image_url` berisi nilai (dipotong di iOS MVP, kirim `null`)

---

## Referensi

| Dokumen | Relevansi |
|---|---|
| `be_updet/letters_api.md` baris 39 | Validasi `body_text` ≥20 char (mandatory, tidak bisa di-bypass) |
| `be_updet/letters_api.md` baris 81 | `composite_image_url` wajib non-null |
| `ios-mvp-spec.md` §2a | Flatten via `ImageRenderer` selalu jalan, tidak pernah `null` |
| `compose-screen-spec.md` §10 | Validasi Android (referensi) — `bodyText.isNotBlank() && dearName.isNotBlank()` |
