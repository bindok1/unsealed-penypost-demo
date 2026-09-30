# Peny Store (Creator Marketplace) — Backend API Specification & Integration Guide

Dokumen ini adalah spesifikasi backend lengkap dan panduan integrasi untuk fitur **Peny Store (Creator Marketplace)**, disusun agar tim **Mobile Apps (Flutter/Android/iOS)** dan **Web Admin** dapat mengonsumsi API secara akurat, konsisten, dan efisien.

---

## 1. Overview & Autentikasi

### Base URL
- **Development**: `https://api.unsealed.app/`
- **Local**: `http://localhost:8080`

### Skema Autentikasi Berdasarkan Konsumen

| Konsumen | Mekanisme Auth | Header / Param | Catatan |
|---|---|---|---|
| **Mobile App** | Firebase Auth | `Authorization: Bearer <firebase_id_token>` | Wajib untuk catalog, detail item (agar `viewer_has_liked` & `viewer_owns` terisi), like, view, dan user inventory. |
| **Web Admin** | Admin Secret / Session | `X-Admin-Key: <ADMIN_API_KEY>` **atau** `Authorization: Bearer <session_token>` | Header yang sama seperti dashboard admin lainnya. |
| **Creator Portal** | Magic Token (No Auth) | URL Param `:token` pada path `/api/v1/creators/portal/:token` | Didesain fast-path, langsung dibuka via link tanpa Firebase auth. |
| **RevenueCat** | Webhook Shared Secret | `Authorization: <revenuecat_webhook_secret>` | Konsumen sistem server-to-server. |

### Format Standar Response Envelope

Semua endpoint mengembalikan envelope standar:

```json
// Sukses
{
  "success": true,
  "data": { ... }
}

// Error
{
  "success": false,
  "error": "human readable error message",
  // or structured error if error code is present:
  // "error": { "code": "EMAIL_EXISTS", "message": "creator email is already registered" }
}
```

---

## 2. Reference Enum, Tier Pricing & Royalty

### Category Enum
- `stamp_pack`
- `sticker_pack`
- `paper_pack`
- `envelope_pack`
- `bundle`

### Tier, Harga & Royalti Creator

| Tier | Harga IDR | Royalti Creator IDR | Harga USD | Royalti Creator USD |
|---|---:|---:|---:|---:|
| `tier_1` | Rp17.000 | Rp9.000 | $0.99 | $0.60 |
| `tier_2` | Rp35.000 | Rp19.000 | $1.99 | $1.20 |
| `tier_3` | Rp88.000 | Rp48.000 | $4.99 | $3.00 |
| `tier_4` | Rp59.000 | Rp31.000 | $3.99 | $2.00 |
| `tier_5` | Rp149.000 | Rp80.000 | $9.99 | $5.00 |

### Minimum Payout Threshold
- **Indonesia (IDR)**: Rp25.000
- **Global (USD)**: $3.00 (300 sen USD)

### Publication Status Enum (Review State Machine)
- `DRAFT` (Awal pembuatan item)
- `PENDING_REVIEW` (Diajukan untuk review admin)
- `PUBLISHED` (Live dan dapat dilihat/dibeli user)
- `REJECTED` (Ditolak admin, dapat direvisi kembali ke `DRAFT`)
- `ARCHIVED` (Diturunkan dari etalase publik)

### Asset Type Enum
- `STAMP` (Perangko digital)
- `STICKER` (Stiker surat)
- `PAPER` (Kertas surat)
- `ENVELOPE` (Amplop surat)

---

## 3. Mobile Apps Integration Guide

### 3.1 Showcase Banner Etalase
Mengambil banner promosi utama toko yang di-pin oleh admin (`is_showcase_banner = true`), diurutkan berdasarkan `banner_order ASC`.

- **Method**: `GET`
- **Path**: `/api/v1/shop/showcase`
- **Auth**: `Bearer <firebase_id_token>`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
        "title": "Autumn Botanical Collection",
        "sub_description": "Exclusive fall illustrations by Studio Kembara",
        "description": "A warm autumn pack featuring vintage botanical stamps and matching envelopes.",
        "showcase_banner_url": "https://pub-r2.penypost.me/banners/autumn_showcase.webp",
        "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
        "tier": "tier_2",
        "category": "bundle",
        "creator_name": "Studio Kembara",
        "price_idr": 35000,
        "price_usd": 1.99
      }
    ]
  }
}
```

---

### 3.2 Katalog Toko (Catalog with Keyset Pagination)
Menampilkan daftar semua item berstatus `PUBLISHED`. Mendukung filter kategori dan pagination menggunakan cursor.

- **Method**: `GET`
- **Path**: `/api/v1/items`
- **Auth**: `Bearer <firebase_id_token>`
- **Query Params**:
  - `category` *(optional)*: Filter kategori item. Mendukung alias pendek atau nama pack: `stamp` / `stamp_pack`, `paper` / `paper_pack`, `sticker` / `sticker_pack`, `envelope` / `envelope_pack`, `bundle`, atau `all` (tanpa filter).
  - `type` *(optional)*: Filter item yang memuat jenis aset tertentu: `stamp` (item dengan `stamps_count > 0`), `paper` (`papers_count > 0`), `sticker` (`stickers_count > 0`), `envelope` (`envelopes_count > 0`), `bundle`. Berguna jika user ingin mencari semua produk yang memuat perangko/kertas meskipun dikemas dalam bentuk bundle.
  - `creator_id` *(optional)*: Filter karya berdasarkan ID kreator (untuk fitur Fase 2: *"More by this artist"*).
  - `after` *(optional)*: Nilai `next_cursor` dari response sebelumnya untuk keyset pagination.
  - `limit` *(optional, default: 20, max: 50)*: Jumlah item per halaman.

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
        "title": "Autumn Botanical Collection",
        "sub_description": "Exclusive fall illustrations",
        "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
        "category": "bundle",
        "tier": "tier_2",
        "price_idr": 35000,
        "price_usd": 1.99,
        "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
        "creator_name": "Studio Kembara",
        "stamps_count": 4,
        "stickers_count": 8,
        "papers_count": 2,
        "envelopes_count": 1,
        "likes_count": 128,
        "viewer_has_liked": true,
        "created_at": "2026-09-25T07:00:00Z"
      }
    ],
    "next_cursor": "MjAyNi0wOS0yNVQwNzowMDowMFp8ZTZhMWU5NDQtNzdhOC00OGI0LTkyNzEtZThkMzU2OGM4NTUx"
  }
}
```
> **Tips Client**: Jika `next_cursor` bernilai `null`, berarti sudah mencapai akhir katalog (halaman terakhir).

---

### 3.3 Detail Produk (Item Detail)
Menampilkan informasi lengkap item, aset-aset di dalamnya, status like user, status kepemilikan user (`viewer_owns`), serta objek `creator` lengkap untuk Bottom Sheet / Detail Card (Fase 1 MVP).

- **Method**: `GET`
- **Path**: `/api/v1/items/:id`
- **Auth**: `Bearer <firebase_id_token>`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
    "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
    "creator_name": "Studio Kembara",
    "creator": {
      "id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
      "name": "Studio Kembara",
      "avatar_url": "https://pub-r2.penypost.me/avatars/kembara.webp",
      "bio": "Illustrator & botanical artist based in Bandung.",
      "external_link": "https://instagram.com/studiokembara"
    },
    "category": "bundle",
    "tier": "tier_2",
    "price_idr": 35000,
    "price_usd": 1.99,
    "title": "Autumn Botanical Collection",
    "description": "Full collection containing 4 stamps, 8 stickers, 2 paper textures, and 1 custom envelope.",
    "sub_description": "Exclusive fall illustrations",
    "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
    "showcase_banner_url": "https://pub-r2.penypost.me/banners/autumn_showcase.webp",
    "stamps_count": 4,
    "stickers_count": 8,
    "papers_count": 2,
    "envelopes_count": 1,
    "views_count": 1540,
    "likes_count": 128,
    "viewer_has_liked": true,
    "viewer_owns": false,
    "assets": [
      {
        "asset_type": "STAMP",
        "asset_url": "https://pub-r2.penypost.me/stamps/autumn_leaf.webp",
        "sort_order": 0
      },
      {
        "asset_type": "STICKER",
        "asset_url": "https://pub-r2.penypost.me/stickers/mushroom.webp",
        "sort_order": 1
      },
      {
        "asset_type": "PAPER",
        "asset_url": "https://pub-r2.penypost.me/papers/kraft_vintage.webp",
        "sort_order": 2
      },
      {
        "asset_type": "ENVELOPE",
        "asset_url": "https://pub-r2.penypost.me/envelopes/maple_wood.webp",
        "sort_order": 3
      }
    ],
    "created_at": "2026-09-25T07:00:00Z"
  }
}
```

---

### 3.4 Toggle Like Item
Mengubah status like user untuk item tertentu. Jika belum like, akan like dan `likes_count` bertambah; jika sudah like, akan unlike dan `likes_count` berkurang.

- **Method**: `POST`
- **Path**: `/api/v1/items/:id/like`
- **Auth**: `Bearer <firebase_id_token>`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "has_liked": true,
    "likes_count": 129
  }
}
```

---

### 3.5 Increment View Count
Dipanggil saat layar detail item dibuka secara bermakna oleh user. Endpoint ini beroperasi non-blocking di backend.

- **Method**: `POST`
- **Path**: `/api/v1/items/:id/view`
- **Auth**: `Bearer <firebase_id_token>`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "status": "ok"
  }
}
```

---

### 3.6 User Inventory (Jembatan ke Letter Composer)
Menampilkan seluruh item marketplace yang telah dimiliki secara permanen oleh user beserta aset-aset di dalamnya, untuk digunakan saat menyusun surat (composer).

- **Method**: `GET`
- **Path**: `/api/v1/me/inventory`
- **Auth**: `Bearer <firebase_id_token>`

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
        "title": "Autumn Botanical Collection",
        "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
        "category": "bundle",
        "unlocked_via": "IAP",
        "unlocked_at": "2026-09-25T07:30:00Z",
        "assets": [
          {
            "asset_type": "STAMP",
            "asset_url": "https://pub-r2.penypost.me/stamps/autumn_leaf.webp",
            "sort_order": 0
          },
          {
            "asset_type": "STICKER",
            "asset_url": "https://pub-r2.penypost.me/stickers/mushroom.webp",
            "sort_order": 1
          }
        ]
      }
    ]
  }
}
```

---

### 3.8 Profil Publik Kreator (Bottom Sheet / Detail Card)
Mengambil data profil publik kreator untuk ditampilkan di Bottom Sheet / Detail Card saat pengguna mengetuk prangko atau nama kreator.

- **Method**: `GET`
- **Path**: `/api/v1/creators/:id/profile`
- **Auth**: None (Public)

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
    "name": "Studio Kembara",
    "avatar_url": "https://pub-r2.penypost.me/avatars/kembara.webp",
    "bio": "Illustrator & botanical artist based in Bandung.",
    "external_link": "https://instagram.com/studiokembara"
  }
}
```

---

### 3.9 Alur Pembelian IAP (RevenueCat) dari Mobile
```text
Mobile App                  RevenueCat              Backend (Webhook)
    │                            │                          │
    ├─ In-App Purchase Flow ────►│                          │
    │  (Google Play/App Store)   │                          │
    │                            ├─ Webhook Event ─────────►│
    │                            │  NON_RENEWING_PURCHASE   ├─ Validasi Idempotency
    │                            │                          ├─ Grant User Inventory
    │                            │                          ├─ Catat Creator Royalty
    │                            │                          └─ Increment Sales Count
    │◄─ Purchase Succeeded ──────┤                          │
    │                            │                          │
    ├─ Refetch GET /me/inventory ──────────────────────────►│
    │  atau GET /items/:id (viewer_owns = true)             │
```

---

## 4. Web Admin Integration Guide

Semua endpoint admin berada di `/api/v1/admin/*` dan membutuhkan header:
```http
X-Admin-Key: <ADMIN_API_KEY>
```
atau
```http
Authorization: Bearer <session_token>
```

---

### 4.1 Manajemen Creator

#### A. Buat Creator Baru
Membuat entitas creator baru dan menghasilkan token akses unik (Magic Token).
- **Method**: `POST`
- **Path**: `/api/v1/admin/creators`

**Request Body**:
```json
{
  "name": "Studio Kembara",
  "email": "kembara@studio.id",
  "payout_country": "ID",
  "payout_method": "BANK_TRANSFER",
  "payout_details": "BCA 1234567890 a/n Studio Kembara"
}
```

**Response `201 Created`**:
```json
{
  "success": true,
  "data": {
    "id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
    "name": "Studio Kembara",
    "email": "kembara@studio.id",
    "magic_token": "a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0",
    "payout_country": "ID",
    "payout_method": "BANK_TRANSFER",
    "payout_details": "BCA 1234567890 a/n Studio Kembara",
    "unpaid_balance_idr": 0,
    "unpaid_balance_usd": 0,
    "created_at": "2026-09-25T06:00:00Z",
    "updated_at": "2026-09-25T06:00:00Z"
  }
}
```
> **Penting**: Simpan atau bagikan `magic_token` ke creator saat pembuatan ini. Magic token adalah URL akses creator portal: `https://penypost.me/creator/portal/<magic_token>`.

#### B. Daftar Creator
- **Method**: `GET`
- **Path**: `/api/v1/admin/creators`

#### C. Update Creator
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/creators/:id`

**Request Body** *(partial, kirim field yang ingin diubah saja)*:
```json
{
  "name": "Studio Kembara Utama",
  "payout_method": "BANK_TRANSFER",
  "payout_details": "Mandiri 9876543210 a/n Studio Kembara Utama"
}
```

---

### 4.2 Manajemen Item Catalog

#### A. Buat Item Baru
Item yang baru dibuat akan otomatis memiliki status `DRAFT`.
- **Method**: `POST`
- **Path**: `/api/v1/admin/items`

**Request Body**:
```json
{
  "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
  "category": "bundle",
  "tier": "tier_2",
  "title": "Autumn Botanical Collection",
  "description": "Full collection containing autumn stamps, stickers, and envelopes.",
  "sub_description": "Exclusive fall illustrations",
  "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
  "showcase_banner_url": "https://pub-r2.penypost.me/banners/autumn_showcase.webp",
  "is_showcase_banner": true,
  "banner_order": 1
}
```

**Response `201 Created`**:
```json
{
  "success": true,
  "data": {
    "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
    "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
    "category": "bundle",
    "tier": "tier_2",
    "title": "Autumn Botanical Collection",
    "publication_status": "DRAFT",
    "stamps_count": 0,
    "stickers_count": 0,
    "papers_count": 0,
    "envelopes_count": 0,
    "created_at": "2026-09-25T07:00:00Z"
  }
}
```

#### B. Update Metadata Item
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/items/:id`

**Request Body** *(partial)*:
```json
{
  "title": "Autumn Botanical Collection (Rev 2)",
  "banner_order": 2,
  "is_showcase_banner": false
}
```

#### C. Update Status Publikasi (Review Workflow)
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/items/:id/status`

**Request Body**:
```json
{
  "status": "PUBLISHED"
}
```
> **Aturan Transisi Status**:
> - `DRAFT` → `PENDING_REVIEW` atau `PUBLISHED`
> - `PENDING_REVIEW` → `PUBLISHED`, `REJECTED`, atau `DRAFT`
> - `REJECTED` → `DRAFT`
> - `PUBLISHED` → `ARCHIVED`
> - `ARCHIVED` → `DRAFT`

#### D. Tambah Asset ke Item
Upload file gambar terlebih dahulu ke Cloudflare R2 menggunakan `POST /api/v1/storage/presign`, lalu pasang URL-nya ke endpoint ini:
- **Method**: `POST`
- **Path**: `/api/v1/admin/items/:id/assets`

**Request Body**:
```json
{
  "asset_type": "STAMP",
  "asset_url": "https://pub-r2.penypost.me/stamps/autumn_leaf.webp",
  "sort_order": 0
}
```
*(Menambahkan asset otomatis menambah `stamps_count` / `stickers_count` / `papers_count` / `envelopes_count` pada item).*

#### E. Hapus Asset dari Item
- **Method**: `DELETE`
- **Path**: `/api/v1/admin/items/:id/assets/:assetId`

#### F. List Semua Item di Admin
- **Method**: `GET`
- **Path**: `/api/v1/admin/items`
- **Query Params**:
  - `creator_id` *(optional)*
  - `publication_status` *(optional)*
  - `category` *(optional)*

#### G. Detail Item di Admin
Mengambil detail satu item lengkap dengan daftar karya/aset (`assets`) dan nama kreator (`creator_name`) tanpa memerlukan Firebase auth (menggunakan Admin Key/Session).
- **Method**: `GET`
- **Path**: `/api/v1/admin/items/:id`

---

### 4.3 Payout Flow (Pembayaran Royalti ke Creator)

#### A. Daftar Creator yang Berhak Menerima Payout
Menampilkan creator yang memiliki saldo unpaid di atas batas minimum (IDR ≥ Rp25.000 atau USD ≥ $3.00).
- **Method**: `GET`
- **Path**: `/api/v1/admin/payout/eligible`

**Response `200 OK`**:
```json
{
  "success": true,
  "data": [
    {
      "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
      "name": "Studio Kembara",
      "email": "kembara@studio.id",
      "payout_country": "ID",
      "payout_method": "BANK_TRANSFER",
      "payout_details": "BCA 1234567890 a/n Studio Kembara",
      "unpaid_balance_idr": 95000,
      "unpaid_balance_usd": 0
    }
  ]
}
```

#### B. Lihat Detail Royalti Unpaid per Creator
- **Method**: `GET`
- **Path**: `/api/v1/admin/creators/:id/earnings`

#### C. Proses Eksekusi Payout
Secara atomic: mengunci baris creator (`FOR UPDATE`), membuat rekaman `payouts`, menandai semua record `creator_earnings` yang berstatus `UNPAID` menjadi `PAID` terhubung ke `payout_id`, dan mengenolkan saldo `unpaid_balance_idr` & `unpaid_balance_usd`.
- **Method**: `POST`
- **Path**: `/api/v1/admin/creators/:id/payouts`

**Response `201 Created`**:
```json
{
  "success": true,
  "data": {
    "id": "7b8e1f02-a1b2-4c3d-8e9f-0123456789ab",
    "creator_id": "c1f7a228-4f24-4f81-807d-cf5d50693a12",
    "amount_idr": 95000,
    "amount_usd": 0,
    "currency": "IDR",
    "receipt_url": null,
    "paid_at": null,
    "created_at": "2026-09-25T08:00:00Z"
  }
}
```

#### D. Lampirkan Bukti Transfer (Receipt)
Setelah admin mentransfer dana ke rekening creator, lampirkan screenshot/PDF bukti transfer.
- **Method**: `PATCH`
- **Path**: `/api/v1/admin/payouts/:id/receipt`

**Request Body**:
```json
{
  "receipt_url": "https://pub-r2.penypost.me/payout_receipts/bca_transfer_95000.pdf"
}
```

---

## 5. Creator Portal (Magic Token)

Creator dapat memantau produk dan penghasilannya tanpa perlu registrasi atau login akun Google/Firebase, cukup melalui link magic token unik mereka.

- **Method**: `GET`
- **Path**: `/api/v1/creators/portal/:token`
- **Auth**: **Tanpa Auth** (Bypass Firebase Auth)

#### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "creator": {
      "name": "Studio Kembara",
      "email": "kembara@studio.id",
      "payout_method": "BANK_TRANSFER",
      "payout_country": "ID"
    },
    "financial": {
      "unpaid_balance_idr": 95000,
      "unpaid_balance_usd": 0,
      "payout_currency": "IDR",
      "minimum_threshold_idr": 25000,
      "minimum_threshold_usd": 300,
      "is_eligible_for_payout": true
    },
    "products": [
      {
        "id": "e6a1e944-77a8-48b4-9271-e8d3568c8551",
        "title": "Autumn Botanical Collection",
        "thumbnail_url": "https://pub-r2.penypost.me/thumbnails/autumn_thumb.webp",
        "publication_status": "PUBLISHED",
        "views_count": 1540,
        "likes_count": 128,
        "sales_count": 5
      }
    ]
  }
}
```

> **Catatan Privasi**: Kolom sensitif seperti nomor rekening bank (`payout_details`) secara sengaja disaring dan **tidak pernah ditampilkan** pada response creator portal publik ini.

---

## 6. Error Response Reference

| HTTP Code | Error Code / Message | Kondisi Terjadinya |
|---|---|---|
| `400` | `"title is required"` / `"creator_id is required"` | Payload request tidak lengkap atau tidak valid. |
| `400` | `"invalid tier"` / `"invalid category"` | Value enum tidak sesuai dengan daftar yang didukung. |
| `400` | `"invalid status transition from X to Y"` | Melakukan transisi status review yang dilarang state machine. |
| `400` | `"creator has no unpaid balance"` | Menjalankan payout untuk creator yang saldonya 0. |
| `401` | `{"error": "unauthorized"}` | Firebase ID token atau Admin Key tidak dikirimkan atau salah. |
| `404` | `"item not found"` | Item ID tidak ditemukan atau belum berstatus `PUBLISHED` (di sisi app). |
| `404` | `"creator not found"` | Token magic atau ID creator tidak ada di database. |
| `404` | `"asset not found"` | Asset ID tidak ditemukan pada item terkait. |
| `404` | `"payout not found"` | Payout ID tidak ditemukan di database. |
| `409` | `{"code": "EMAIL_EXISTS", "message": "creator email is already registered"}` | Email creator sudah pernah didaftarkan sebelumnya. |
