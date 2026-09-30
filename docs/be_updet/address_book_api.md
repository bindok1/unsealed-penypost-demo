# Address Book API

Backend untuk `ProfileAddressBookScreen.kt` (`dummyContacts: AddressBookEntry(id, name, country)`).

Semua endpoint `/api/v1`, butuh Bearer token.

---

## 1. Endpoints

### `GET /api/v1/address-book`
Mengambil daftar kontak tersimpan milik viewer. Menyaring otomatis user yang diblokir (dua arah).

**Response `200 OK`:**
```json
{
  "success": true,
  "data": [
    {
      "id": "c1f7b03b-1b15-465d-9c3f-4b067d0cf03e",
      "penpal_user_id": "usr_penpal123",
      "name": "Sahabat Pena",
      "nickname": "JaneDoe",
      "country": "Asia",
      "photo_url": "https://...",
      "bio": "Menulis surat dari Tokyo",
      "label": "Sahabat Pena",
      "created_at": "2026-08-16T15:30:00Z"
    }
  ]
}
```
*Catatan:* `name` mengembalikan `label` jika diisi kustom oleh viewer, atau fallback ke `nickname` asli dari profil penpal.

---

### `POST /api/v1/address-book`
Menambahkan kontak ke address book. Jika kontak sudah ada, label akan diperbarui (upsert).

**Request Body:**
```json
{
  "penpal_user_id": "usr_penpal123",
  "label": "Sahabat Pena"
}
```

**Response `201 Created`:**
```json
{
  "success": true,
  "data": {
    "id": "c1f7b03b-1b15-465d-9c3f-4b067d0cf03e",
    "penpal_user_id": "usr_penpal123",
    "name": "Sahabat Pena",
    "nickname": "JaneDoe",
    "country": "Asia",
    "photo_url": "https://...",
    "bio": "Menulis surat dari Tokyo",
    "label": "Sahabat Pena",
    "created_at": "2026-08-16T15:30:00Z"
  }
}
```

*Push Notification:* Ketika sukses ditambahkan, backend otomatis mengirimkan push notification via FCM ke `penpal_user_id` yang ditambahkan (`event: address_book_added`, Title: *"Ditambahkan ke Buku Alamat! 📖"*, Body: *"{Nickname} baru saja menambahkanmu ke buku alamat mereka!"*).

**Error Responses:**
- `400 Bad Request`:
  - `penpal_user_id is required`
  - `cannot add yourself to address book`
  - `user not found` (jika user tidak ada di tabel `users` atau terblokir)

---

### `DELETE /api/v1/address-book/:id`
Menghapus kontak dari address book berdasarkan `id` entri address book.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "deleted": true
  }
}
```

**Error `400`:** `contact not found` — kalau `id` entri address book tidak ada/bukan milik viewer (`app/module/addressbook/repository/address_book_repository.go:141-143`).

---

## 2. Desain Database & Relasi

**Migrasi:** `db/migrations/013_address_book.sql`

```sql
CREATE TABLE IF NOT EXISTS address_book_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    penpal_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_address_book_user_penpal UNIQUE (user_id, penpal_user_id),
    CONSTRAINT chk_address_book_not_self CHECK (user_id <> penpal_user_id)
);
CREATE INDEX IF NOT EXISTS idx_address_book_entries_user_id ON address_book_entries(user_id);
```

**Desain penting:** simpan `penpal_user_id` (foreign key), **jangan** denormalisasi `name`/`country` ke tabel address book — resolve lewat join ke `users` tiap fetch, supaya kalau kontak ganti nickname/continent, address book otomatis ikut update.

---

## 3. Status Implementasi

Backend selesai (2026-08-16):
- [x] Migrasi `013_address_book.sql`
- [x] Module `app/module/addressbook` (Entity, DTO, Repository, Service, Handler)
- [x] Routing & Module Wiring di `app/router/router.go` dan `app/bootstrap/app.go`
- [x] Unit test di `app/module/addressbook/service/address_book_service_test.go`

