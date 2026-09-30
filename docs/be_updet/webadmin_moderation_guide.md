# Web Admin Integration Guide — Moderation & Reporting API

Dokumen ini berisi panduan teknis dan spesifikasi endpoint backend untuk pengembangan **Web Admin Dashboard CMS** pada fitur **Moderasi & Pelaporan (Moderation & Reporting)**.

---

## 1. Autentikasi & Header

Seluruh endpoint admin **tidak menggunakan Firebase Token**, melainkan menggunakan Admin Key tersendiri.

* **Header Wajib:** `X-Admin-Key: <ADMIN_SECRET_KEY>`
* **Content-Type:** `application/json`

```http
GET /api/v1/admin/reports HTTP/1.1
Host: api.unsealed.app
X-Admin-Key: your_admin_secret_key_here
Content-Type: application/json
```

---

## 2. Struktur Halaman Dashboard `/moderation`

Halaman moderasi dibagi menjadi **3 Tab Utama**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Moderation Dashboard                                                    │
├──────────────────┬──────────────────────┬───────────────────────────────┤
│ Tab 1: Pending   │ Tab 2: History       │ Tab 3: Suspended Users        │
│ (Laporan Masuk)  │ (Semua Laporan)      │ (Manajemen Suspend/Ban)       │
└──────────────────┴──────────────────────┴───────────────────────────────┘
```

---

## 3. Fitur & Integrasi API per Tab

### Tab 1 — Laporan Masuk (`Pending Queue`)

#### A. Load Daftar Laporan Pending
* **HTTP Method:** `GET`
* **Path:** `/api/v1/admin/reports?status=pending&limit=20`
* **Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "report_123abc",
        "target_type": "letter",
        "target_id": "letter_456def",
        "body_snapshot": "Isi surat yang dilaporkan...",
        "reason": "harassment",
        "status": "pending",
        "admin_note": null,
        "reviewed_by": null,
        "reviewed_at": null,
        "created_at": "2026-08-13T10:00:00Z",
        "report_count_same_target": 3,
        "reported_user": {
          "id": "user_789ghi",
          "nickname": "Budi",
          "continent": "Asia",
          "photo_url": "https://...",
          "account_status": "active",
          "total_reports_received": 5
        }
      }
    ],
    "next_cursor": "eyJjcmVhdGVkX2F0Ij...",
    "total_pending": 14
  }
}
```
* **UI Notes:**
  * Gunakan `total_pending` untuk menampilkan badge counter di header/tab.
  * Tampilkan kolom: Tanggal, Alasan, Preview Konten (`body_snapshot`), User Terlapor (`reported_user.nickname`), Jumlah Laporan Konten ini (`report_count_same_target`), dan Total Laporan User ini (`reported_user.total_reports_received`).

---

#### B. Detail Laporan + Riwayat Laporan Terkait (Modal / Drawer)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/admin/reports/:id`
* **Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "id": "report_123abc",
    "target_type": "letter",
    "target_id": "letter_456def",
    "body_snapshot": "Isi surat yang dilaporkan...",
    "reason": "harassment",
    "status": "pending",
    "admin_note": null,
    "reviewed_by": null,
    "reviewed_at": null,
    "created_at": "2026-08-13T10:00:00Z",
    "report_count_same_target": 3,
    "reported_user": {
      "id": "user_789ghi",
      "nickname": "Budi",
      "continent": "Asia",
      "photo_url": "https://...",
      "account_status": "active",
      "total_reports_received": 5
    },
    "related_reports": [
      {
        "id": "report_000old",
        "reason": "spam",
        "created_at": "2026-08-01T12:00:00Z",
        "status": "actioned"
      }
    ]
  }
}
```

---

#### C. Aksi 1: Hapus Konten (Soft-Delete)
Panggil 2 API berurutan saat admin menekan tombol **"Hapus Konten"**:

1. **Soft-delete Konten:**
   * **HTTP Method:** `DELETE`
   * **Path:** `/api/v1/admin/content/:type/:id` (`:type` = `letter` atau `penpal_post`)
   * **Response `200 OK`:**
     ```json
     {
       "success": true,
       "data": { "id": "letter_456def", "deleted_at": "2026-08-13T16:00:00Z" }
     }
     ```
   * *Side-effect:* Server otomatis mengirim push notification FCM ke sender surat bahwa kontennya dihapus.

2. **Update Status Laporan jadi Actioned:**
   * **HTTP Method:** `PATCH`
   * **Path:** `/api/v1/admin/reports/:id`
   * **Request Body:**
     ```json
     {
       "status": "actioned",
       "admin_note": "Konten dihapus karena melanggar Pedoman Komunitas."
     }
     ```

---

#### D. Aksi 2: Suspend atau Ban User
Panggil 2 API berurutan saat admin menekan tombol **"Suspend User"** atau **"Ban Permanen"**:

1. **Jalankan Aksi Moderasi User:**
   * **HTTP Method:** `POST`
   * **Path:** `/api/v1/admin/users/:uid/moderation-action`
   * **Request Body (Suspend Sementara):**
     ```json
     {
       "action": "suspend",
       "reason": "Pelanggaran berulang terkait pesan ketidakpantasan.",
       "duration_days": 7
     }
     ```
   * **Request Body (Ban Permanen):**
     ```json
     {
       "action": "ban",
       "reason": "Pelanggaran berat Pedoman Komunitas."
     }
     ```
   * **Response `200 OK`:**
     ```json
     {
       "success": true,
       "data": {
         "uid": "user_789ghi",
         "account_status": "suspended",
         "suspended_at": "2026-08-13T16:00:00Z",
         "suspend_until": "2026-08-20T16:00:00Z"
       }
     }
     ```
   * *Side-effect:* Server otomatis memutus sesi pengguna (Revoke Firebase Token) dan mengirim push notification FCM berisi alasan penangguhan.

2. **Update Status Laporan jadi Actioned:**
   * **HTTP Method:** `PATCH`
   * **Path:** `/api/v1/admin/reports/:id`
   * **Request Body:**
     ```json
     {
       "status": "actioned",
       "admin_note": "User ditangguhkan selama 7 hari."
     }
     ```

---

#### E. Aksi 3: Dismiss (Abaikan Laporan)
* **HTTP Method:** `PATCH`
* **Path:** `/api/v1/admin/reports/:id`
* **Request Body:**
  ```json
  {
    "status": "dismissed",
    "admin_note": "Laporan diverifikasi dan tidak ditemukan pelanggaran."
  }
  ```

---

### Tab 2 — Riwayat & Filter Laporan (`History`)

Tampilkan tabel laporan dengan filter dinamis di bagian atas.

* **HTTP Method:** `GET`
* **Path:** `/api/v1/admin/reports`
* **Query Parameters:**
  | Param | Type | Pilihan Value |
  |---|---|---|
  | `status` | string | `pending`, `reviewed`, `actioned`, `dismissed` |
  | `reason` | string | `harassment`, `spam`, `inappropriate_content`, `underage`, `impersonation`, `other`, `empty_mail` (`app/module/trust/service/validation.go`'s `ValidReportReasons` — beda dari draft lama di `moderation_api.md`) |
  | `target_type` | string | `letter`, `user` (bukan `penpal_post` — target itu tidak pernah tersimpan di data report) |
  | `cursor` | string | Nilai `next_cursor` dari response sebelumnya |
  | `limit` | number | Default `20`, Max `100` |

* **Contoh Request:**
  `GET /api/v1/admin/reports?status=actioned&reason=harassment&limit=20`

---

### Tab 3 — Manajemen Akun Tersuspend (`Suspended Users`)

#### Unsuspend / Aktifkan Kembali Akun User
* **HTTP Method:** `POST`
* **Path:** `/api/v1/admin/users/:uid/moderation-action`
* **Request Body:**
  ```json
  {
    "action": "unsuspend"
  }
  ```
* **Response `200 OK`:**
  ```json
  {
    "success": true,
    "data": {
      "uid": "user_789ghi",
      "account_status": "active",
      "suspended_at": null,
      "suspend_until": null
    }
  }
  ```

---

## 4. Format Respon Error

Jika terjadi kesalahan (misal key salah atau parameter tidak valid):

* **401 Unauthorized / 403 Forbidden:**
  ```json
  { "success": false, "error": "unauthorized" }
  ```
* **400 Bad Request:**
  ```json
  { "success": false, "error": "reason is required for suspend" }
  ```
