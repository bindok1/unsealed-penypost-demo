# Rewards & Daily Check-In API

Mekanisme gamifikasi & retensi harian Unsealed:
1. **Daily Check-In / Streak** (Siklus 7 Hari untuk mendapatkan Energy & Prangko eksklusif).
2. **Daily Quests / Misi Harian** (Reward Energy dari aktivitas kirim/balas surat).
3. **Stamp Exchange** (Membuka prangko baru dari Stamp Book menggunakan Energy).

Semua endpoint berada di bawah `/api/v1` dan memerlukan header:
`Authorization: Bearer <firebase_id_token>`

---

## 1. Status Reward Harian (Daily Check-In)

Mengambil status check-in hari ini, streak saat ini, sisa waktu hingga reset harian, dan kalender reward 7 hari.

### Tabel Milestone Siklus 7 Hari (Milestone Schedule):

| Hari (Day) | Tipe Reward | Jumlah Energi | Item Spesial / Prangko | Keterangan |
|---|---|---|---|---|
| **Hari 1** | ⚡ Energi Peni | `+10` | - | *Starter Boost* |
| **Hari 2** | ⚡ Energi Peni | `+15` | - | *Streak 2 Hari* |
| **Hari 3** | ⚡ Energi + Segel | `+20` | `seal_wax_gold` | **Milestone 1**: Aksesoris Segel Lilin Emas |
| **Hari 4** | ⚡ Energi Peni | `+25` | - | *Streak 4 Hari* |
| **Hari 5** | ⚡ Energi Peni | `+30` | - | *Streak 5 Hari* |
| **Hari 6** | ⚡ Energi Peni | `+40` | - | *Streak 6 Hari (Super Boost)* |
| **Hari 7** | ⚡ Energi + 👑 Prangko | `+50` | `stamp_peni_aurora` | **Grand Prize**: Prangko Eksklusif Peni Aurora |

`GET /api/v1/rewards/daily`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "can_claim_today": true,
    "current_streak": 3,
    "longest_streak": 14,
    "last_claimed_at": "2026-08-16T08:30:00Z",
    "next_reset_at": "2026-08-18T00:00:00Z",
    "schedule": [
      { "day": 1, "energy": 10, "stamp_id": null, "is_claimed": true, "is_current": false },
      { "day": 2, "energy": 15, "stamp_id": null, "is_claimed": true, "is_current": false },
      { "day": 3, "energy": 20, "stamp_id": "seal_wax_gold", "is_claimed": false, "is_current": true },
      { "day": 4, "energy": 25, "stamp_id": null, "is_claimed": false, "is_current": false },
      { "day": 5, "energy": 30, "stamp_id": null, "is_claimed": false, "is_current": false },
      { "day": 6, "energy": 40, "stamp_id": null, "is_claimed": false, "is_current": false },
      { "day": 7, "energy": 50, "stamp_id": "stamp_peni_aurora", "is_claimed": false, "is_current": false }
    ]
  }
}
```

---

## 2. Klaim Reward Harian (Claim Daily Check-In)

Mengklaim reward check-in hari ini. Backend akan:
1. Menambahkan `energy` ke saldo user.
2. Menambah `current_streak` (+1). Jika kemarin tidak check-in (streak putus), reset streak kembali ke Day 1.
3. Jika reward hari tersebut menyertakan `stamp_id`, otomatis membuka prangko tersebut di koleksi user (`user_stamps`).
4. Mencatat `last_claimed_at` menjadi waktu sekarang.

`POST /api/v1/rewards/daily/claim`

### Request Body
Kosong `{}`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "streak": 3,
    "energy_granted": 20,
    "current_energy_balance": 140,
    "unlocked_stamp": null,
    "claimed_at": "2026-08-17T09:35:00Z",
    "next_reset_at": "2026-08-18T00:00:00Z"
  }
}
```

### Error Cases:
- `409 Conflict`: `{ "success": false, "error": { "code": "ALREADY_CLAIMED_TODAY", "message": "Reward harian sudah diklaim hari ini. Silakan kembali besok!" } }`

---

## 3. Misi Harian (Daily Quests)

Daftar quest harian yang dapat diselesaikan pengguna untuk mendapatkan tambahan Energy (misal: "Kirim 1 surat hari ini", "Beri makan Peni", "Balas surat penpal").

`GET /api/v1/rewards/quests`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "quest_send_letter",
        "title": "Tulis Surat",
        "description": "Kirim 1 surat ke sahabat pena baru atau lama",
        "target_count": 1,
        "current_count": 1,
        "energy_reward": 15,
        "is_completed": true,
        "is_claimed": false
      },
      {
        "id": "quest_feed_peni",
        "title": "Beri Makan Peni",
        "description": "Percepat pengantaran surat dengan memberi makan Peni",
        "target_count": 1,
        "current_count": 0,
        "energy_reward": 10,
        "is_completed": false,
        "is_claimed": false
      }
    ]
  }
}
```

---

## 4. Klaim Reward Misi (Claim Quest)

`POST /api/v1/rewards/quests/{quest_id}/claim`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "quest_id": "quest_send_letter",
    "energy_granted": 15,
    "current_energy_balance": 155,
    "claimed_at": "2026-08-17T09:36:00Z"
  }
}
```

### Error Cases (belum pernah didokumentasikan sebelumnya):
- `409 Conflict`: `{ "success": false, "error": { "code": "ALREADY_CLAIMED_TODAY", "message": "Reward misi ini sudah diklaim hari ini." } }` (`app/module/reward/service/reward_service.go:236`)
- `400 Bad Request`: `{ "success": false, "error": { "code": "QUEST_NOT_COMPLETED", "message": "Misi ini belum selesai." } }` (`app/module/reward/service/reward_service.go:270`)

---

## 5. Buka Prangko dengan Energy (Unlock Stamp)

Memungkinkan pengguna menukarkan Energy yang telah dikumpulkan dari Daily Reward untuk membuka prangko di Stamp Book.

`POST /api/v1/stamps/{stamp_id}/unlock`

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "stamp_id": "stamp_pack_rose",
    "energy_spent": 50,
    "current_energy_balance": 105,
    "unlocked_at": "2026-08-17T09:37:00Z"
  }
}
```

### Error Cases:
- `400 Bad Request`: `{ "success": false, "error": { "code": "INSUFFICIENT_ENERGY", "message": "Energi kamu tidak cukup untuk membuka prangko ini." } }` — perhatikan `"Energi"` (Indonesia), bukan `"Energy"` seperti yang sempat ditulis di sini; pesan asli dari `app/module/stamp/repository/stamp_repository.go:297`, kalau FE match string literal harus pakai ejaan ini
- `409 Conflict`: `{ "success": false, "error": { "code": "ALREADY_UNLOCKED", "message": "Prangko ini sudah kamu miliki." } }`

---

## 6. Rancangan Tabel Database (Reference for Backend Team)

```sql
-- Status daily check-in per user
CREATE TABLE IF NOT EXISTS user_daily_rewards (
    user_id VARCHAR(128) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Riwayat klaim check-in
CREATE TABLE IF NOT EXISTS daily_claim_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_number INT NOT NULL,
    energy_granted INT NOT NULL,
    stamp_granted VARCHAR(64),
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Prangko yang dimiliki user
CREATE TABLE IF NOT EXISTS user_stamps (
    user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stamp_id VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(32) NOT NULL DEFAULT 'daily_reward', -- 'daily_reward' | 'energy_purchase' | 'iap'
    PRIMARY KEY (user_id, stamp_id)
);
```
