# Stamps, Paper & Store API

Dokumentasi lengkap API Toko Prangko (*Stamps*), Kertas Surat (*Letter Papers*), dan *Supporter Club*.
Sistem ini menggunakan **Energi Peni** (yang didapat dari Daily Check-in / Misi Harian) atau IAP untuk membuka koleksi prangko dan kertas surat.

Semua endpoint berada di bawah `/api/v1` dan memerlukan header:
`Authorization: Bearer <firebase_id_token>`

---

## 0. Referensi Sederhana (`GET /api/v1/stamps`)

Sudah live tapi belum pernah didokumentasikan di sini — **jangan disamakan** dengan katalog toko di §1 (endpoint ini shape-nya lebih sederhana, dipakai sebagai referensi ringan, mirror pola `GET /envelopes` & `GET /stickers`).

`GET /api/v1/stamps`

Response `200`: array `StampResponse` (`app/module/stamp/dto/stamp_dto.go`):
```json
[{ "id": "OCEAN", "name": "Ocean", "image_url": "https://...", "orientation": "portrait", "is_premium": false }]
```
`id` di sini yang dikirim sebagai field `stamp` di `POST /letters` (`docs/be/letters_api.md`).
`orientation`: `'portrait'` (default, mis. 384×512) atau `'landscape'` (mis. 512×384) — ditambahkan via Migration 039.

---

## 1. Katalog Prangko (Stamps Catalog)

Mengambil daftar semua prangko yang tersedia di toko beserta harga energi dan status kepemilikannya oleh pengguna.

`GET /api/v1/stamps/catalog`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "stamp_pack_rose",
        "design": "ROSE",
        "name": "Paket Rose",
        "description": "Prangko bunga mawar merah klasik.",
        "energy_price": 50,
        "image_url": "https://cdn.unsealed.app/stamps/rose.webp",
        "is_unlocked": false,
        "is_exclusive": false
      },
      {
        "id": "stamp_pack_ocean",
        "design": "OCEAN",
        "name": "Paket Ocean",
        "description": "Nuansa laut biru nan tenang.",
        "energy_price": 50,
        "image_url": "https://cdn.unsealed.app/stamps/ocean.webp",
        "is_unlocked": true,
        "is_exclusive": false
      },
      {
        "id": "stamp_peni_aurora",
        "design": "AURORA",
        "name": "Peni Aurora",
        "description": "Prangko legendaris dari siklus 7-Day Streak.",
        "energy_price": 100,
        "image_url": "https://cdn.unsealed.app/stamps/peni_aurora.webp",
        "is_unlocked": false,
        "is_exclusive": true
      }
    ]
  }
}
```

---

## 2. Beli / Buka Prangko dengan Energi (Unlock Stamp)

> [!NOTE]
> **Status Frontend**: Endpoint ini belum akan di-hit oleh Frontend untuk saat ini (fitur pembelian/toko prangko disiapkan untuk update berikutnya).

Membeli atau membuka prangko menggunakan saldo Energi Peni milik pengguna.
Backend akan memeriksa kecukupan saldo `users.energy`, memotong energi, dan mencatat kepemilikan prangko ke tabel `user_stamps`.

`POST /api/v1/stamps/{stamp_id}/unlock`


### Request Body
Kosong `{}`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "stamp_id": "stamp_pack_rose",
    "energy_spent": 50,
    "current_energy_balance": 90,
    "unlocked_at": "2026-08-17T10:00:00Z"
  }
}
```

### Error Cases:
- `400 Bad Request`: `{ "success": false, "error": { "code": "INSUFFICIENT_ENERGY", "message": "Energi kamu tidak cukup untuk membuka prangko ini." } }`
- `404 Not Found`: `{ "success": false, "error": { "code": "STAMP_NOT_FOUND", "message": "Prangko tidak ditemukan dalam katalog." } }`
- `409 Conflict`: `{ "success": false, "error": { "code": "ALREADY_UNLOCKED", "message": "Prangko ini sudah kamu miliki di Stamp Book." } }`

---

## 3. Prangko Saya (My Owned Stamps)

Mengambil daftar ID dan kode prangko yang telah dimiliki pengguna untuk digunakan pada layar penulisan surat (*Compose Letter*) dan Buku Prangko Profil (*Stamp Book*).

`GET /api/v1/stamps/me`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "stamps": [
      {
        "id": "stamp_pack_ocean",
        "design": "OCEAN",
        "unlocked_at": "2026-08-15T07:12:00Z",
        "source": "energy_purchase"
      },
      {
        "id": "stamp_peni_aurora",
        "design": "AURORA",
        "unlocked_at": "2026-08-17T09:00:00Z",
        "source": "daily_reward"
      }
    ]
  }
}
```

---

## 4. Katalog Kertas Surat (Paper Catalog)

Mengambil daftar jenis kertas surat (*Paper Template*) yang tersedia di toko.

`GET /api/v1/papers/catalog`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "paper_pack_grid",
        "template": "GRID",
        "name": "Kertas Kotak-Kotak",
        "energy_price": 40,
        "texture_url": "https://cdn.unsealed.app/papers/grid.webp",
        "is_unlocked": false
      },
      {
        "id": "paper_pack_aged_kraft",
        "template": "AGED_KRAFT",
        "name": "Kertas Kraft Tua",
        "energy_price": 40,
        "texture_url": "https://cdn.unsealed.app/papers/aged_kraft.webp",
        "is_unlocked": true
      },
      {
        "id": "paper_pack_blush_marble",
        "template": "BLUSH_MARBLE",
        "name": "Kertas Marmer Pink",
        "energy_price": 60,
        "texture_url": "https://cdn.unsealed.app/papers/blush_marble.webp",
        "is_unlocked": false
      }
    ]
  }
}
```

---

## 5. Beli / Buka Kertas Surat (Unlock Paper)

> [!NOTE]
> **Status Frontend**: Endpoint ini belum akan di-hit oleh Frontend untuk saat ini (fitur pembelian/toko kertas surat disiapkan untuk update berikutnya).

`POST /api/v1/papers/{paper_id}/unlock`


### Request Body
Kosong `{}`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "paper_id": "paper_pack_grid",
    "energy_spent": 40,
    "current_energy_balance": 50,
    "unlocked_at": "2026-08-17T10:05:00Z"
  }
}
```

### Error Cases:
- `400 Bad Request`: `{ "success": false, "error": { "code": "INSUFFICIENT_ENERGY", "message": "Energi kamu tidak cukup untuk membuka kertas surat ini." } }`
- `409 Conflict`: `{ "success": false, "error": { "code": "ALREADY_UNLOCKED", "message": "Kertas surat ini sudah kamu miliki." } }`

---

## 6. Kertas Surat Saya (My Owned Papers)

`GET /api/v1/papers/me`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "papers": [
      {
        "id": "paper_pack_aged_kraft",
        "template": "AGED_KRAFT",
        "unlocked_at": "2026-08-01T00:00:00Z"
      }
    ]
  }
}
```

---

## 7. Supporter Club (Patron Membership — *Deferred Post-Launch with RevenueCat*)

> **Catatan Implementasi UI & IAP**:
> Bagian Supporter Club dan sistem pembelian berbayar (fiat) di-hide terlebih dahulu pada rilis awal aplikasi (v1.0). Pembelian paket Supporter Club dan IAP nyata (Google Play Billing / Apple In-App Purchase) akan diintegrasikan secara penuh menggunakan **RevenueCat SDK** setelah rilis.
>
> Endpoint `GET /api/v1/supporter/tiers` saat ini disiapkan sebagai placeholder (`is_active: false`), dan UI aplikasi menyembunyikan section ini sampai konfigurasi RevenueCat siap di tahap post-launch.

`GET /api/v1/supporter/tiers`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "tiers": [
      {
        "id": "supporter_club_monthly",
        "title": "Peny Supporter",
        "subtitle": "Dukung Peny langsung dan dapatkan lencana supporter di profilmu.",
        "price_label": "Rp 25.000/bulan",
        "is_active": false
      }
    ]
  }
}
```


---

## 8. Skema Tabel Database PostgreSQL (DDL)

```sql
-- Katalog prangko toko
CREATE TABLE IF NOT EXISTS stamp_catalog (
    id VARCHAR(64) PRIMARY KEY,
    design VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    energy_price INT NOT NULL DEFAULT 50,
    image_url TEXT NOT NULL,
    is_exclusive BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Prangko yang dimiliki user
CREATE TABLE IF NOT EXISTS user_stamps (
    user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stamp_id VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(32) NOT NULL DEFAULT 'energy_purchase', -- 'daily_reward' | 'energy_purchase' | 'iap'
    PRIMARY KEY (user_id, stamp_id)
);

-- Katalog kertas surat toko
CREATE TABLE IF NOT EXISTS paper_catalog (
    id VARCHAR(64) PRIMARY KEY,
    template VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    energy_price INT NOT NULL DEFAULT 40,
    texture_url TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Kertas surat yang dimiliki user
CREATE TABLE IF NOT EXISTS user_papers (
    user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paper_id VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, paper_id)
);
```
