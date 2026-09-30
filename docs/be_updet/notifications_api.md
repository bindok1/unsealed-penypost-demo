# Push Notifications — Backend Spec

Status: **sudah jalan end-to-end.** Worker `app/module/delivery/service/delivery_service.go` dan `app/module/dailystack/service/dailystack_service.go` berjalan in-process dengan `time.Ticker`. Firebase Admin SDK terpasang dan di-inject ke 5 modul: `delivery`, `admin`, `addressbook`, `dailystack`, serta `letter`. `fcm_token` disimpan lewat `PATCH /auth/me` / `POST /auth/register` (§1), dan penanganan token invalidation (§5) sudah aktif di seluruh modul pengirim notifikasi.

---

## 1. Data Model & Preferensi
- `users.fcm_token` (nullable, `db/migrations/001_init.sql`) — di-update lewat `AuthRepository.Update` (`app/module/auth/repository/auth_repository.go`), menggunakan `COALESCE($4, fcm_token)` agar field yang di-omit pada request tidak menimpa token lama.
- Satu kolom = satu token per user (device terakhir yang login menang). Cukup untuk MVP.
- `users.notify_hour_pref` (`"morning"`, `"afternoon"`, `"evening"`) & `users.utc_offset_minutes` (int) — preferensi jadwal pengiriman notifikasi stack harian per user (lihat `daily_penpal_stack.md`).
- Token bisa menjadi tidak valid sewaktu-waktu (uninstall, clear data, reinstall, rotasi FCM) — dibersihkan secara otomatis saat pengiriman gagal dengan status `UNREGISTERED` (§5).

### 1.1 Sinkronisasi Token dari Client
FE mengirim token melalui:
1. `FirebaseMessagingService.onNewToken()` saat token baru digenerate oleh Firebase.
2. `AuthViewModel.syncFcmToken()` setiap kali status user terautentikasi.

---

## 2. Trigger Points & Event Matrix

| Event Key | Trigger | Modul / Service | Payload Data |
|---|---|---|---|
| `letter_delivered` | Surat tiba di penerima (`in_transit` → `delivered`) | `delivery` ([delivery_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/delivery/service/delivery_service.go)) | `{"letter_id": "<id>", "event": "letter_delivered"}` |
| `daily_stack_ready` | Tumpukan surat harian (≤20 surat) siap dibaca pada jam preferensi user | `dailystack` ([dailystack_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/dailystack/service/dailystack_service.go)) | `{"event": "daily_stack_ready"}` |
| `address_book_added` | User ditambahkan ke address book user lain | `addressbook` ([address_book_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/addressbook/service/address_book_service.go)) | `{"event": "address_book_added", "user_id": "<adder_id>"}` |
| `account_suspended` | Moderasi: Akun ditangguhkan sementara | `admin` ([admin_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/admin/service/admin_service.go)) | `{"event": "account_suspended", "reason": "<alasan>"}` |
| `account_banned` | Moderasi: Akun dinonaktifkan permanen | `admin` ([admin_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/admin/service/admin_service.go)) | `{"event": "account_banned", "reason": "<alasan>"}` |
| `content_removed` | Moderasi: Surat atau postingan dihapus oleh admin | `admin` ([admin_service.go](file:///Users/niozihni/IdeaProjects/unsealed/app/module/admin/service/admin_service.go)) | `{"event": "content_removed", "content_id": "<id>"}` |

*Catatan:*
- Surat baru yang dikirim (`POST /letters`) **tidak** mengirim notifikasi instan karena surat berstatus `in_transit` (memerlukan waktu perjalanan sesuai jarak). Notifikasi baru dikirim saat status berubah menjadi `delivered`.
- Surat dari seed persona (M2) mengikuti alur yang sama persis dengan surat biasa (`letter_delivered`).

---

## 3. Background Workers

Semua background worker berjalan sebagai goroutine in-process dengan `time.Ticker` (diinisialisasi pada `app/bootstrap/app.go` saat startup):

### 3.1 Delivery Worker (`delivery_service.go`)
- **Interval:** 30 detik (`pollInterval = 30 * time.Second`).
- **Tugas:** Mengecek surat dengan status `in_transit` dan `estimated_arrival_at <= now()`, mengubah statusnya menjadi `delivered`, dan mengirimkan push notification `letter_delivered` ke penerima.

### 3.2 Daily Stack Worker (`dailystack_service.go`)
- **Interval:** 5 menit (`pollInterval = 5 * time.Minute`).
- **Tugas:** Mengecek user yang sudah jatuh tempo berdasarkan kombinasi `notify_hour_pref` dan `utc_offset_minutes`, meng-generate stack harian hari tersebut jika belum ada, dan mengirimkan push notification `daily_stack_ready`.
- *Catatan:* Jika user sudah membuka aplikasi dan melihat feed hari itu sebelum jam jadwal notifikasinya tiba, stack sudah digenerate secara on-the-fly (`notify=false`), sehingga notifikasi push tidak akan dikirim lagi untuk hari itu.

---

## 4. Format Payload Notifikasi (Firebase Admin SDK)

Inisialisasi Firebase Messaging Client (`*messaging.Client`) dilakukan di `app/bootstrap/firebase.go` dari kredensial environment variable `FIREBASE_CREDENTIALS`.

### 4.1 Letter Delivered (`letter_delivered`)
```go
Notification: &messaging.Notification{
    Title: "New Letter Arrived! 💌",
    Body:  fmt.Sprintf("Peny just landed with a warm letter from %s. Open it now!", senderNickname),
},
Data: map[string]string{
    "letter_id": letterID,
    "event":     "letter_delivered",
},
```

### 4.2 Daily Stack Ready (`daily_stack_ready`)
```go
Notification: &messaging.Notification{
    Title: "Your Daily Stack is Ready! 📬",
    Body:  "New letters from fellow penpals are waiting to be read. Open them now!",
},
Data: map[string]string{
    "event": "daily_stack_ready",
},
```

### 4.3 Address Book Added (`address_book_added`)
```go
Notification: &messaging.Notification{
    Title: "Ditambahkan ke Buku Alamat! 📖",
    Body:  fmt.Sprintf("%s baru saja menambahkanmu ke buku alamat mereka!", adderNickname),
},
Data: map[string]string{
    "event":   "address_book_added",
    "user_id": adderID,
},
```

### 4.4 Moderation Notifications
```go
// Account Suspended
Notification: &messaging.Notification{
    Title: "Akun Ditangguhkan",
    Body:  "Akun Anda telah ditangguhkan karena pelanggaran Pedoman Komunitas. Hubungi dukungan jika ini adalah kekeliruan.",
},
Data: map[string]string{
    "event":  "account_suspended",
    "reason": req.Reason,
}

// Account Banned
Notification: &messaging.Notification{
    Title: "Akun Dinonaktifkan",
    Body:  "Akun Anda telah dinonaktifkan permanen karena pelanggaran Pedoman Komunitas. Hubungi dukungan jika ini adalah kekeliruan.",
},
Data: map[string]string{
    "event":  "account_banned",
    "reason": req.Reason,
}

// Content Removed
Notification: &messaging.Notification{
    Title: "Konten Dihapus",
    Body:  "Salah satu surat atau postingan kamu telah dihapus karena melanggar Pedoman Komunitas Penypost.",
},
Data: map[string]string{
    "event":      "content_removed",
    "content_id": id,
}
```

---

## 5. Token Invalidation

FCM mengembalikan `messaging.IsUnregistered(err)` apabila token sudah tidak valid (aplikasi di-uninstall, clear storage, dll.).

Penanganan token invalidation dilakukan di modul-modul berikut:
1. **`admin` service:** Memanggil `s.authRepo.ClearFCMToken(ctx, token)` (`UPDATE users SET fcm_token = NULL WHERE fcm_token = $1`).
2. **`delivery` service:** Menjalankan direct SQL `UPDATE users SET fcm_token = NULL WHERE id = $1` berdasarkan recipient ID.
3. **`addressbook` service:** Memanggil `s.repo.ClearFCMToken(ctx, targetID)`.
4. **`dailystack` service:** Menjalankan direct SQL `UPDATE users SET fcm_token = NULL WHERE id = $1` berdasarkan user ID.

*Catatan:* Error sementara lainnya (koneksi jaringan, kuota) **tidak** menghapus token, hanya status `IsUnregistered` yang memicu pembersihan token.

---

## 6. Scope & Batasan MVP
- **Single token per user:** Menggunakan model 1 user = 1 `fcm_token` (login terakhir menang). Multi-device tidak diperlukan untuk MVP.
- **In-App Notification Center:** Saat ini push notification langsung diarahkan ke handler client / deep link, belum ada tabel history in-app notification center.
