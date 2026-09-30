# Implementation Plan — Peny Store (Creator Marketplace)

> Berdasarkan spec: `docs/backend-spec_peny_store.md`
> Stack: Go · Fiber · pgx · Cloudflare R2 · RevenueCat · existing conventions

---

## Prinsip Eksekusi

- Setiap milestone **tidak merusak fitur yang sudah live** — additive only kecuali M3 (extend webhook)
- Ikuti konvensi yang sudah ada: `package/apperr`, `package/response`, `package/cursor`, atomic tx dengan `SELECT ... FOR UPDATE`
- Magic token creator = **random hex token, disimpan di DB** — tidak perlu JWT, tidak ada middleware baru, cukup `WHERE magic_token = $1` di repo
- Semua tabel baru pakai UUID PK (`gen_random_uuid()`) kecuali ada alasan lain, sesuai pola `invites`
- Migration numbering lanjut dari `036_` → mulai dari `037_`

---

## Milestone 1 — Foundation: Skema DB + Module Shell + Catalog API

**Goal:** Item store bisa dibrowse dari app. Tidak ada transaksi dulu.

### 1.1 Migration `037_peny_store_foundation.sql`

Tabel yang dibuat:

**`creators`**
```sql
CREATE TABLE IF NOT EXISTS creators (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    magic_token     TEXT NOT NULL UNIQUE, -- random hex, generate saat INSERT
    payout_country  TEXT NOT NULL DEFAULT '',
    payout_method   TEXT NOT NULL DEFAULT '',
    payout_details  TEXT NOT NULL DEFAULT '', -- sensitif, tidak pernah masuk public API
    unpaid_balance_idr  BIGINT NOT NULL DEFAULT 0,
    unpaid_balance_usd  BIGINT NOT NULL DEFAULT 0, -- dalam sen USD
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**`items`**
```sql
CREATE TABLE IF NOT EXISTS items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id          UUID NOT NULL REFERENCES creators(id),
    category            TEXT NOT NULL,         -- 'stamp_pack' | 'sticker_pack' | 'paper_pack' | 'envelope_pack' | 'bundle'
    tier                TEXT NOT NULL,         -- 'tier_1'..'tier_5'
    title               TEXT NOT NULL,
    description         TEXT NOT NULL DEFAULT '',
    sub_description     TEXT NOT NULL DEFAULT '',
    thumbnail_url       TEXT NOT NULL DEFAULT '',
    showcase_banner_url TEXT NOT NULL DEFAULT '',
    is_showcase_banner  BOOLEAN NOT NULL DEFAULT false,
    banner_order        INT NOT NULL DEFAULT 0,
    stamps_count        INT NOT NULL DEFAULT 0,
    stickers_count      INT NOT NULL DEFAULT 0,
    papers_count        INT NOT NULL DEFAULT 0,
    envelopes_count     INT NOT NULL DEFAULT 0,
    views_count         BIGINT NOT NULL DEFAULT 0,
    likes_count         BIGINT NOT NULL DEFAULT 0,
    sales_count         INT NOT NULL DEFAULT 0,
    publication_status  TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT | PENDING_REVIEW | PUBLISHED | REJECTED | ARCHIVED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_items_creator_id ON items(creator_id);
CREATE INDEX IF NOT EXISTS idx_items_publication_status ON items(publication_status);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category);
```

**`item_assets`**
```sql
CREATE TABLE IF NOT EXISTS item_assets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id     UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    asset_type  TEXT NOT NULL,  -- STAMP | STICKER | PAPER | ENVELOPE
    asset_url   TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_item_assets_item_id ON item_assets(item_id);
```

**`item_likes`**
```sql
CREATE TABLE IF NOT EXISTS item_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id     UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (item_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_item_likes_item_id ON item_likes(item_id);
```

### 1.2 Module Baru: `app/module/shop`

```
app/module/shop/
  ├── dto/
  │   └── shop_dto.go         ← ItemListItem, ItemDetail, ShowcaseItem, tier price map
  ├── entity/
  │   └── item.go             ← Item, ItemAsset (public fields only)
  ├── handler/
  │   └── shop_handler.go     ← Showcase, ListItems, GetItem
  ├── repository/
  │   └── shop_repository.go  ← GetShowcase, ListItems (cursor), GetItemByID
  ├── service/
  │   └── shop_service.go
  └── shop.go                 ← wire + RegisterRoutes
```

### 1.3 Endpoints M1

| Method | Path | Deskripsi |
|---|---|---|
| `GET` | `/api/v1/shop/showcase` | Banner showcase items (published + `is_showcase_banner = true`), sorted by `banner_order` |
| `GET` | `/api/v1/items` | Catalog items, filter `?category=`, cursor paginated, sorted by `created_at DESC` |
| `GET` | `/api/v1/items/:id` | Detail item + assets + creator display name + tier price + like count + `viewer_has_liked` + `viewer_owns` |

**Tier price map** — konstanta di `shop/service/shop_service.go`:
```go
var TierPriceIDR = map[string]int64{
    "tier_1": 17000, "tier_2": 35000, "tier_3": 88000,
    "tier_4": 59000, "tier_5": 149000,
}
var TierPriceUSD = map[string]float64{
    "tier_1": 0.99, "tier_2": 1.99, "tier_3": 4.99,
    "tier_4": 3.99, "tier_5": 9.99,
}
```

### 1.4 Wire ke Bootstrap & Router

Tambah `ShopModule *shop.Module` ke `router.Modules`, register di `api` group (dalam FirebaseAuth middleware — user harus login untuk dapat `viewer_has_liked` & `viewer_owns`).

---

## Milestone 2 — Inventory: Transactions + Grant + Inventory API

**Goal:** Setelah beli via RevenueCat, item masuk inventory user. User bisa lihat inventory untuk Letter Composer.

### 2.1 Migration `038_peny_store_transactions.sql`

**`payouts`** (shell disiapkan sekarang karena di-FK dari `creator_earnings`)
```sql
CREATE TABLE IF NOT EXISTS payouts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id  UUID NOT NULL REFERENCES creators(id),
    amount_idr  BIGINT,
    amount_usd  BIGINT, -- dalam sen
    currency    TEXT NOT NULL DEFAULT 'IDR',
    receipt_url TEXT,
    paid_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**`purchase_transactions`**
```sql
CREATE TABLE IF NOT EXISTS purchase_transactions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              TEXT NOT NULL REFERENCES users(id),
    item_id              UUID NOT NULL REFERENCES items(id),
    tier                 TEXT NOT NULL,
    revenuecat_event_id  TEXT NOT NULL UNIQUE, -- idempotency key
    store_transaction_id TEXT,
    gross_amount_idr     BIGINT,
    gross_amount_usd     BIGINT,
    currency             TEXT NOT NULL DEFAULT 'IDR',
    status               TEXT NOT NULL DEFAULT 'COMPLETED', -- COMPLETED | REVERSED
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_purchase_transactions_user_id ON purchase_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_purchase_transactions_item_id ON purchase_transactions(item_id);
```

**`creator_earnings`**
```sql
CREATE TABLE IF NOT EXISTS creator_earnings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id              UUID NOT NULL REFERENCES creators(id),
    item_id                 UUID NOT NULL REFERENCES items(id),
    user_id                 TEXT NOT NULL REFERENCES users(id),
    purchase_transaction_id UUID NOT NULL REFERENCES purchase_transactions(id),
    tier                    TEXT NOT NULL,
    royalty_amount_idr      BIGINT,
    royalty_amount_usd      BIGINT,
    currency                TEXT NOT NULL DEFAULT 'IDR',
    status                  TEXT NOT NULL DEFAULT 'UNPAID', -- UNPAID | PAID | REVERSED
    payout_id               UUID REFERENCES payouts(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at                 TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_creator_earnings_creator_id ON creator_earnings(creator_id);
CREATE INDEX IF NOT EXISTS idx_creator_earnings_status ON creator_earnings(status);
```

**`user_inventories`**
```sql
CREATE TABLE IF NOT EXISTS user_inventories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id      UUID NOT NULL REFERENCES items(id),
    unlocked_via TEXT NOT NULL DEFAULT 'IAP', -- IAP | ENERGY | REDEEM
    unlocked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, item_id)
);
CREATE INDEX IF NOT EXISTS idx_user_inventories_user_id ON user_inventories(user_id);
```

### 2.2 Extend RevenueCat Webhook — Dual Path

**File diubah:** `app/module/revenuecat/service/revenuecat_service.go`

Tambah product→item mapping sebagai konstanta (bisa di-move ke DB nanti):
```go
// PenyStoreProductMap maps RevenueCat product_id → item UUID.
// Populate setelah item pertama dibuat via admin endpoint.
var PenyStoreProductMap = map[string]string{
    "penypost_item_tier1_001": "uuid-item-xxx",
}
```

Logic routing di `HandleWebhook`:
1. `EnergyForProduct(productID) > 0` → flow lama (kredit energy)
2. `PenyStoreProductMap[productID]` ada → flow baru (grant inventory + creator earning)
3. Selainnya → no-op, return 200

Tambah `ProcessStoreItemPurchase` di repository — satu atomic transaction:
1. `INSERT INTO purchase_transactions ... ON CONFLICT (revenuecat_event_id) DO NOTHING` → idempotency
2. `INSERT INTO user_inventories ... ON CONFLICT DO NOTHING`
3. `INSERT INTO creator_earnings ...`
4. `UPDATE creators SET unpaid_balance_idr = unpaid_balance_idr + $royalty`
5. `UPDATE items SET sales_count = sales_count + 1`

### 2.3 Module Baru: `app/module/inventory`

```
app/module/inventory/
  ├── dto/
  │   └── inventory_dto.go    ← InventoryResponse, OwnedItem, AssetItem
  ├── handler/
  │   └── inventory_handler.go
  ├── repository/
  │   └── inventory_repository.go
  ├── service/
  │   └── inventory_service.go
  └── inventory.go
```

### 2.4 Endpoints M2

| Method | Path | Deskripsi |
|---|---|---|
| `GET` | `/api/v1/me/inventory` | Semua item milik user beserta assets (stamps, stickers, papers, envelopes) |

Response shape:
```json
{
  "items": [
    {
      "id": "uuid",
      "title": "Rose Pack",
      "thumbnail_url": "...",
      "unlocked_via": "IAP",
      "unlocked_at": "ISO8601",
      "assets": [
        { "asset_type": "STAMP", "asset_url": "https://...", "sort_order": 0 },
        { "asset_type": "STICKER", "asset_url": "https://...", "sort_order": 1 }
      ]
    }
  ]
}
```

---

## Milestone 3 — Interaksi: Like + View

**Goal:** User bisa like item. View counter naik saat buka detail.

### 3.1 Endpoints M3

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/api/v1/items/:id/like` | Toggle like |
| `POST` | `/api/v1/items/:id/view` | Increment view count (non-blocking) |

**Like toggle** — atomic, pola sama seperti penpal like:
```sql
-- Like: INSERT ON CONFLICT DO NOTHING + UPDATE likes_count + 1
-- Unlike: DELETE + UPDATE likes_count - 1 (GREATEST 0)
```

**View** — fire-and-forget goroutine di handler, tidak block response:
```go
go func() { _ = h.svc.IncrementView(context.Background(), itemID) }()
```
Tidak pakai Redis — `UPDATE items SET views_count = views_count + 1` sudah cukup.

---

## Milestone 4 — Creator Portal

**Goal:** Creator bisa lihat produk dan earnings mereka lewat magic token.

### 4.1 Magic Token — Fast Path

| Keputusan | Pilihan |
|---|---|
| Format token | `crypto/rand` 32 bytes → hex string (64 char) |
| Penyimpanan | Kolom `creators.magic_token` (sudah ada dari M1) |
| Auth mechanism | Query `WHERE magic_token = $1` di repo, compare dengan `subtle.ConstantTimeCompare` |
| Middleware baru | ❌ Tidak ada — token di-resolve langsung di handler/service |
| Route group | Terpisah dari group `/api/v1` Firebase — daftarkan di `router.go` sebagai `app.Group("/api/v1/creators")` tanpa middleware |

### 4.2 Admin: Buat & Kelola Creator

Ditambahkan ke `app/module/admin`:

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/api/v1/admin/creators` | Buat creator, generate `magic_token`, return token (satu-satunya saat terekspos) |
| `GET` | `/api/v1/admin/creators` | List semua creator |
| `PATCH` | `/api/v1/admin/creators/:id` | Update nama/email/payout info |

### 4.3 Module Baru: `app/module/creator`

```
app/module/creator/
  ├── dto/
  │   └── creator_dto.go       ← PortalResponse (no payout_details raw)
  ├── handler/
  │   └── creator_handler.go
  ├── repository/
  │   └── creator_repository.go ← GetByMagicToken, GetPortalData
  ├── service/
  │   └── creator_service.go
  └── creator.go
```

### 4.4 Endpoints M4

| Method | Path | Auth | Deskripsi |
|---|---|---|---|
| `GET` | `/api/v1/creators/portal/:token` | Magic token (no Firebase) | Profil, saldo, eligibility payout, list produk |

Response:
```json
{
  "creator": {
    "name": "Studio Bunga",
    "email": "studio@bunga.id",
    "payout_method": "BANK_TRANSFER",
    "payout_country": "ID"
  },
  "financial": {
    "unpaid_balance_idr": 95000,
    "payout_currency": "IDR",
    "minimum_threshold_idr": 25000,
    "is_eligible_for_payout": true
  },
  "products": [
    {
      "id": "uuid",
      "title": "Rose Pack",
      "thumbnail_url": "...",
      "publication_status": "PUBLISHED",
      "views_count": 1240,
      "likes_count": 88,
      "sales_count": 12
    }
  ]
}
```

> [!IMPORTANT]
> Route `/api/v1/creators/portal/:token` **tidak** masuk group FirebaseAuth — daftarkan sebagai group terpisah di `router.go`, sama seperti `/api/webhooks/*`.

---

## Milestone 5 — Admin: Item Management + Asset Upload

**Goal:** Admin bisa CRUD item dan attach assets via R2.

### 5.1 Endpoints M5 (tambah ke `app/module/admin`)

| Method | Path | Deskripsi |
|---|---|---|
| `POST` | `/api/v1/admin/items` | Buat item baru (default status `DRAFT`) |
| `PATCH` | `/api/v1/admin/items/:id` | Update metadata item |
| `PATCH` | `/api/v1/admin/items/:id/status` | Ubah `publication_status` |
| `POST` | `/api/v1/admin/items/:id/assets` | Tambah asset (pakai R2 presign yang sudah ada) |
| `DELETE` | `/api/v1/admin/items/:id/assets/:assetId` | Hapus asset |
| `GET` | `/api/v1/admin/items` | List semua item (semua status) |

**Asset upload flow** — pakai existing `POST /storage/presign`:
1. Admin → `POST /storage/presign` → dapat `upload_url` + `public_url`
2. Admin upload file langsung ke R2
3. Admin → `POST /admin/items/:id/assets { asset_type, asset_url, sort_order }`

**Review state machine:**
```
DRAFT → PENDING_REVIEW → PUBLISHED
                      → REJECTED → DRAFT (revisi)
PUBLISHED → ARCHIVED
```

---

## Milestone 6 — Admin: Payout Flow

**Goal:** Admin proses payout ke creator, settle earnings, attach receipt.

### 6.1 Endpoints M6

| Method | Path | Deskripsi |
|---|---|---|
| `GET` | `/api/v1/admin/payout/eligible` | Creator yang balance ≥ threshold |
| `GET` | `/api/v1/admin/creators/:id/earnings` | Detail earnings unpaid |
| `POST` | `/api/v1/admin/creators/:id/payouts` | Buat payout, lock earnings, nol-kan balance |
| `PATCH` | `/api/v1/admin/payouts/:id/receipt` | Attach `receipt_url` setelah transfer |

**Anti-race payout** — `SELECT ... FOR UPDATE` di creator row:
```sql
BEGIN;
  SELECT unpaid_balance_idr FROM creators WHERE id = $1 FOR UPDATE;
  -- validasi masih eligible
  INSERT INTO payouts (...) RETURNING id;
  UPDATE creator_earnings SET status='PAID', payout_id=$id, paid_at=now()
    WHERE creator_id=$1 AND status='UNPAID';
  UPDATE creators SET unpaid_balance_idr=0 WHERE id=$1;
COMMIT;
```

---

## Milestone 7 — Purchase Reversal

**Goal:** Handle RevenueCat refund/revoke dengan benar tanpa duplicate.

### 7.1 Extend Webhook

Event types: `CANCELLATION`, `REFUND`

Logic `ProcessReversal` (atomic):
1. `INSERT INTO revenuecat_webhook_events ... ON CONFLICT DO NOTHING` → idempotency
2. Cari `purchase_transactions` by original event ID
3. Kalau sudah `REVERSED` → no-op, return 200
4. `UPDATE purchase_transactions SET status='REVERSED'`
5. `DELETE FROM user_inventories WHERE user_id=X AND item_id=Y`
6. Earning masih `UNPAID`:
   - `UPDATE creator_earnings SET status='REVERSED'`
   - `UPDATE creators SET unpaid_balance_idr = unpaid_balance_idr - royalty`
7. Earning sudah `PAID`:
   - `INSERT INTO creator_earnings (status='REVERSED', royalty=-original)` — negative adjustment
   - `UPDATE creators SET unpaid_balance_idr = unpaid_balance_idr - royalty` (bisa negatif sementara)
8. `UPDATE items SET sales_count = GREATEST(0, sales_count - 1)`

---

## Ringkasan Semua Endpoint

| M | Method | Path | Auth |
|---|---|---|---|
| 1 | GET | `/api/v1/shop/showcase` | Firebase |
| 1 | GET | `/api/v1/items` | Firebase |
| 1 | GET | `/api/v1/items/:id` | Firebase |
| 2 | GET | `/api/v1/me/inventory` | Firebase |
| 3 | POST | `/api/v1/items/:id/like` | Firebase |
| 3 | POST | `/api/v1/items/:id/view` | Firebase |
| 4 | GET | `/api/v1/creators/portal/:token` | Magic token (no Firebase) |
| 4 | POST | `/api/v1/admin/creators` | X-Admin-Key |
| 4 | GET | `/api/v1/admin/creators` | X-Admin-Key |
| 4 | PATCH | `/api/v1/admin/creators/:id` | X-Admin-Key |
| 5 | POST | `/api/v1/admin/items` | X-Admin-Key |
| 5 | PATCH | `/api/v1/admin/items/:id` | X-Admin-Key |
| 5 | PATCH | `/api/v1/admin/items/:id/status` | X-Admin-Key |
| 5 | POST | `/api/v1/admin/items/:id/assets` | X-Admin-Key |
| 5 | DELETE | `/api/v1/admin/items/:id/assets/:assetId` | X-Admin-Key |
| 5 | GET | `/api/v1/admin/items` | X-Admin-Key |
| 6 | GET | `/api/v1/admin/payout/eligible` | X-Admin-Key |
| 6 | GET | `/api/v1/admin/creators/:id/earnings` | X-Admin-Key |
| 6 | POST | `/api/v1/admin/creators/:id/payouts` | X-Admin-Key |
| 6 | PATCH | `/api/v1/admin/payouts/:id/receipt` | X-Admin-Key |
| 7 | (extend) | `/api/webhooks/revenuecat` | RC secret |

---

## Royalty Reference

| Tier | IDR Price | Creator Royalty IDR | USD Price | Creator Royalty USD |
|---|---:|---:|---:|---:|
| tier_1 | Rp17.000 | Rp9.000 | $0.99 | $0.60 |
| tier_2 | Rp35.000 | Rp19.000 | $1.99 | $1.20 |
| tier_3 | Rp88.000 | Rp48.000 | $4.99 | $3.00 |
| tier_4 | Rp59.000 | Rp31.000 | $3.99 | $2.00 |
| tier_5 | Rp149.000 | Rp80.000 | $9.99 | $5.00 |

Minimum payout threshold:
- Indonesia (IDR): Rp25.000
- Global (USD): $3.00

---

## Files yang Dibuat / Diubah per Milestone

### M1
| Aksi | File |
|---|---|
| Baru | `db/migrations/037_peny_store_foundation.sql` |
| Baru | `app/module/shop/**` |
| Ubah | `app/bootstrap/app.go` |
| Ubah | `app/router/router.go` |

### M2
| Aksi | File |
|---|---|
| Baru | `db/migrations/038_peny_store_transactions.sql` |
| Baru | `app/module/inventory/**` |
| Ubah | `app/module/revenuecat/service/revenuecat_service.go` |
| Ubah | `app/module/revenuecat/repository/revenuecat_repository.go` |
| Ubah | `app/bootstrap/app.go` |
| Ubah | `app/router/router.go` |

### M3
| Aksi | File |
|---|---|
| Ubah | `app/module/shop/handler/shop_handler.go` |
| Ubah | `app/module/shop/service/shop_service.go` |
| Ubah | `app/module/shop/repository/shop_repository.go` |

### M4
| Aksi | File |
|---|---|
| Baru | `app/module/creator/**` |
| Ubah | `app/module/admin/**` (creator CRUD) |
| Ubah | `app/bootstrap/app.go` |
| Ubah | `app/router/router.go` (group creator portal di luar Firebase) |

### M5
| Aksi | File |
|---|---|
| Ubah | `app/module/admin/**` (item management) |

### M6
| Aksi | File |
|---|---|
| Ubah | `app/module/admin/**` (payout flow) |

### M7
| Aksi | File |
|---|---|
| Ubah | `app/module/revenuecat/service/revenuecat_service.go` |
| Ubah | `app/module/revenuecat/repository/revenuecat_repository.go` |
