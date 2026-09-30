# 🐧 Penny (Peni) - Character Persona & UI Microcopy Rules

Document status: Active  
App Type: Penpal & Slow Messaging  
Mascot: Penny / Peni (Kawaii Flat Vector Penguin)

---

## 1. Character Identity & Tone of Voice

### Persona Profile
* **Name:** Penny (Peni)
* **Role:** Kurir Pos Antartika & Duta Penpal
* **Traits:** Ramah, antusias, sedikit konyol/komedis, gigih, dan hangat.
* **Motto:** *"Sayap tak bisa terbang, tapi perut jago seluncuran!"*

### Tone Guidelines
* **Tone:** Warm, Playful, Friendly, & Reassuring.
* **Perspective:** First-person ("Aku Peni", "Peni", "Suratmu").
* **Key Vocabulary:** Seluncuran, wusss, surat hangat, sahabat pena, Antartika, mendarat, kepingan es.
* **Emoji Usage:** Gunakan maksimal 1-2 emoji per pesan (`🐧`, `✉️`, `❄️`, `💌`, `💨`).

---

## 2. Copywriting & Asset Rules (Rulebook for Improvement)

Gunakan aturan ini setiap kali menambah UI microcopy, pesan notifikasi, atau mengarahkan desainer/AI untuk membuat gambar aset baru.

### Rule A: Copywriting Rules
1. **Always Human-First, Never Technical:**
    * ❌ *Bad:* "Error 500: Server connection failed."
    * ✅ *Good:* "Aduh, Peni Tergelincir! Sinyal internetmu terputus, nih."
2. **Keep It Short & Scannable:**
    * **Title:** Maksimal 4-6 kata. Harus langsung menangkap perhatian.
    * **Body:** Maksimal 1-2 kalimat (terutama untuk *push notification* & *empty states*).
3. **Include Active Call-to-Action (CTA):**
    * Gunakan kata kerja positif pada tombol action (e.g., *"Siap Berseluncur!"*, *"Tulis Surat Pertama"*).

### Rule B: Visual Asset & Image Rules
1. **Style Consistency:**
    * **Style:** Kawaii Flat Vector (*lineless*, tanpa *outline* tebal).
    * **Shape Foundation:** Geometri dasar murni (*Capsule*, *Circle*, *Rounded Triangle*).
2. **Color Palette & Skeuomorphic Design Enforcement:**
    * Wajib pakai token warna dari `Color.kt` (Cute Polar Post Office & Tactile Paper palette).
    * **Background & Canvas (Default Light Cozy):** `SurfaceCream` (`#FFFDF9`) dipadu gradasi lembut `IceSkyBlue` (`#E5F3F9`) — bersih, hangat, dan bernuansa Antartika ramah (hindari background espresso gelap untuk layar pembuka/onboarding).
    * **Tactile Envelope Cards (Skeuomorphism):** `SurfaceCardLight` (`#F6EFE3`) atau `PaperCream` (`#FFF8E7`) dengan border `SurfaceBorderLight` (`#E6DAC8`) dan bayangan fisik lembut (*soft drop shadow* 4-6dp) bergaya kertas taktil.
    * **Primary CTA & Wax Seal:** `BrandGold` (`#E8984E`) — warm honey gold untuk tombol aksi utama & segel lilin, selaras dengan paruh & kaki Peni.
    * **Text & Typography:** `BrandInk` (`#2C221E`) — soft espresso untuk header/judul, dan `BrandGoldDim` (`#9E6B43`) untuk subtitle/body.
    * **Peni Mascot Body Colors:**
      * **Primary Body:** `BrandInk` (`#2C221E`) — soft espresso.
      * **Belly & Face:** `White` (`#FFFFFF`) atau `SurfaceCream` (`#FFFDF9`).
      * **Beak & Feet:** `BrandGold` (`#E8984E`).
      * **Cheek Blush:** `PaperStrawberry` (`#FCE4EC`, Opacity 70%).
      * **Polar Accents (opsional):** `IceSkyBlue` (`#E5F3F9`) atau `IceSkyAccent` (`#5CA3D0`) untuk aksen es Antartika.
      * **Outline/Detail (opsional):** `BrandGoldDeep` (`#73441B`) untuk shading atau garis tipis.
3. **Expression Matching (State Alignment):**
    * **Success State:** Peni memberi hormat (*salute*) atau melempar topi ke atas.
    * **Loading State:** Peni melakukan *belly slide* (tiarap berseluncur).
    * **Error State:** Peni terduduk miring, menggaruk kepala, atau kebingungan.
    * **Empty State:** Peni melambaikan satu sayap dengan wajah penasaran/kesepian.

---

## 3. UI Microcopy Mapping

### 0. Splash Screen (App Open / Loading)
Beberapa opsi tone untuk copy splash — pilih sesuai durasi tampil & nuansa yang mau ditonjolkan. Semua tetap dalam suara Peni (lihat §1).

| Opsi | Title | Subtitle | Kapan Dipakai |
| :--- | :--- | :--- | :--- |
| **A — Cute & Expressive** (Rekomendasi default) | Paket Surat Datang! 💌 | Peni lagi ngebut biar suratmu gak terlambat! | Splash super singkat (< 2 detik), auto-routing cepat ke login/main. |
| **B — Relaxed & Emotional** | Rehat Sejenak, Ada Cerita Baru ☕ | Peni baru saja mendarat membawa kabar dari jauh. | Kalau mau menonjolkan slow-messaging vibe app. |
| **C — Interactive / Micro-Story** | Tunggiiin Peni! 🐧💨 | Agak licin nih, tapi suratmu pasti Peni antarkan sampai tujuan! | Splash yang durasinya agak lebih lama (nunggu auth/network check). |
| **D — Clean & Simple** | Wusss! Surat Hangat Mendarat ❄️✉️ | *(tanpa subtitle)* | Kalau layout splash minimalis, cuma satu baris teks di bawah logo Peni. |

> ⚠️ **Gap implementasi saat ini:** `SplashScreen.kt` masih pakai tagline hardcoded berbahasa Inggris *"Write letters. Connect the world."* — tidak pakai suara Peni sama sekali dan beda bahasa dari seluruh copy lain di app. Perlu diganti ke salah satu opsi di atas (string resource, bukan hardcoded) saat splash di-touch lagi.

### A. Onboarding & Permissions
| Screen / Action | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **Welcome** | Halo! Aku Peni! 🐧✉️ | Penguin biasa yang nekat jadi kurir penpal. Sayapku memang gak bisa terbang, tapi perutku jago seluncuran buat nganterin suratmu sampai ke ujung dunia! | Siap Berseluncur! |
| **Permissions (gabungan)** | Biarkan Peni Ketuk Pintumu 🔔 | Biar Peni bisa jaga suratmu dengan baik, izinkan dua hal kecil ini ya. | Boleh, Peni! |
| ↳ Notifikasi (row) | Notifikasi surat masuk | Nyalakan notifikasi supaya Peni bisa langsung kasih tahu saat surat dari sahabat penamu mendarat! | *(bagian dari CTA gabungan di atas)* |
| ↳ Penyimpanan (row) | Simpan & bagikan surat 🖼️ | Izinkan akses galeri biar Peni bisa simpan suratmu yang berkesan atau bagikan ke teman-temanmu! | *(bagian dari CTA gabungan di atas)* |

> ℹ️ **Implementasi saat ini:** `OnboardingPermissionsScreen.kt` menggabungkan permintaan notifikasi (`POST_NOTIFICATIONS`, Android 13+) dan penyimpanan (`WRITE_EXTERNAL_STORAGE`, hanya API <29 — scoped storage di atasnya tidak butuh izin) dalam satu step onboarding (step 2 dari 6), bukan dua dialog terpisah. Satu tombol "Boleh, Peni!" memicu kedua system dialog secara berurutan; step ini juga punya tombol Skip karena kedua izin bersifat opsional (fitur terkait degrade dengan baik kalau ditolak).

### A2. Onboarding Carousel (3-Step Intro)
Dipakai sebelum login/register — memperkenalkan konsep app ke user baru, 3 slide singkat.

| Step | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **1/3 — Konsep** | Tulis Surat, Bukan Chat Buru-Buru 🖋️ | Di sini kamu nulis surat asli ke sahabat pena — pelan-pelan, penuh cerita, gak perlu terburu-buru. | Lanjut |
| **2/3 — Koneksi Global** | Ketemu Sahabat dari Seluruh Dunia 🌍 | Peni bakal antarkan suratmu ke penpal baru di negara lain. Siapa tahu jadi teman seumur hidup! | Lanjut |
| **3/3 — Notifikasi Balasan** | Balasan Datang, Peni yang Kabari 🔔 | Begitu surat balasan mendarat, Peni langsung kasih tahu kamu. Yuk mulai petualangannya! | Mulai Sekarang |

### B. Empty States
| Screen / Action | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **Empty Inbox** | Sepi Banget, Kayak Antartika ❄️ | Belum ada surat yang masuk, nih. Yuk, tulis surat pertamamu dan sapa teman baru di luar sana! | Tulis Surat Pertama |
| **No Friends** | Peni Kesepian Sendirian... 🐧 | Cari sahabat penamu sekarang dan mulai saling berkirim cerita hangat. | Cari Sahabat Pena |
| **Empty Notifications** | Belum Ada Kabar, Nih 🔔 | Peni belum bawa pemberitahuan apa pun buatmu. Begitu ada surat baru datang, Peni langsung kabari kamu! | *(tanpa CTA, atau opsional "Tulis Surat")* |

### C. Actions & Statuses
| Context | Microcopy / Text |
| :--- | :--- |
| **Send Button** | Bawa Surat Ini, Peni! 🚀 |
| **In Transit Status** | *Wusss!* Peni sedang berseluncur melintasi lautan membawa suratmu... 🌊 |
| **Sent Success** | **Title:** Surat Berhasil Diantarkan! 🎉<br>**Body:** Peni sudah menaruh suratmu dengan aman. Sekarang, mari santai sejenak sambil menunggu balasan! |

### D. Error & Edge Cases
| Context | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **Offline / No Connection** | Aduh, Peni Tergelincir! 🧊 | Sinyal internetmu terputus, nih. Peni istirahat sebentar di atas es ya. Coba periksa koneksimu lagi! | Coba Lagi |
| **Send Failed** | Angin Badai Terlalu Kencang 💨 | Suratmu gagal diantarkan. Tenang, draf suratmu aman kok. Ayo coba kirim ulang! | Kirim Ulang |
| **404 Not Found** | Peni Tersesat di Peta... 🗺️ | Halaman yang kamu cari tidak ada di sini. Yuk, balik lagi ke markas! | Pulang ke Beranda |
| **Delete Confirmation** | Yakin Mau Dibuang? 🗑️ | Peni sudah cape-cape menjaga surat draf ini, lho. Yakin mau dihapus? | Ya, Hapus Saja |
| **Logout Confirmation** | Mau Pergi Dulu? 👋 | Kamu bisa balik lagi kapan aja — suratmu bakal tetap aman menunggumu di sini. | Ya, Keluar |

#### Maintenance — pilih salah satu tone
| Opsi | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **Playful** | Peni Lagi Beres-Beres Kantor Pos 🧹 | Kami lagi upgrade sistem biar suratmu makin lancar terkirim. Balik lagi sebentar lagi, ya! | Oke, Peni! |
| **Reassuring** | Sedang Ada Perbaikan Sistem 🛠️ | Peni istirahat sejenak buat benerin beberapa hal. Suratmu aman kok, coba lagi dalam beberapa saat. | Coba Lagi |

#### Update App / Force Update — pilih salah satu tone
| Opsi | Title | Body Copy | Primary CTA |
| :--- | :--- | :--- | :--- |
| **Playful (soft update)** | Peni Punya Seragam Baru! ✨ | Ada pembaruan seru menantimu. Update dulu, yuk, biar Peni makin gesit anterin suratmu! | Update Sekarang |
| **Urgent (force update)** | Yuk, Perbarui Dulu 📲 | Versi ini sudah gak didukung lagi. Update aplikasinya supaya suratmu tetap bisa terkirim dengan aman. | Update Sekarang |

---

## 4. Push Notification Templates

* **Default / Arrival:**
    * **Title:** Surat Baru Tiba! 💌
    * **Body:** Peni baru aja mendarat membawa surat hangat dari `[Nama Pengirim]`. Buka sekarang yuk!
* **Humorous:**
    * **Title:** Nafas Peni Tersengal-sengal... 🐧💨
    * **Body:** Setelah berseluncur jauh, surat dari `[Nama Pengirim]` akhirnya sampai di tempatmu!
* **Cross-Border:**
    * **Title:** Surat Antar-Benua Tiba! 🌎✨
    * **Body:** Surat perjalanan jauh dari `[Nama Pengirim]` di `[Nama Negara]` sudah Peni antarkan dengan selamat!