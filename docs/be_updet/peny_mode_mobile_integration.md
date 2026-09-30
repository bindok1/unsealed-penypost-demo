# Mode Peny — Mobile Integration Guide

Panduan cara pakai API Mode Peny dari sisi client Android. Beda dari dua dokumen lain yang sudah ada:
- `docs/be/peny_mode_api.md` — rationale desain backend (kenapa begini), buat yang kerja di sisi Go.
- `docs/api_contract.md` §`peny` — kontrak DTO mentah (field, tipe, error code), sumber kebenaran buat shape request/response.
- **Dokumen ini** — alur pemakaian & state management dari sisi client: kapan manggil apa, apa yang disimpan di mana, gimana nanganin tiap error case secara UX. Kalau ada beda antara dokumen ini dan `api_contract.md` soal shape data, `api_contract.md` yang benar (dokumen ini fokus ke alur, bukan re-dokumentasi field).

Base URL & auth sama seperti endpoint lain: `Authorization: Bearer <firebase_id_token>`, base `https://unsealed-be-development.up.railway.app` (dev) — lihat `docs/api_contract.md` §Auth.

---

## 1. Pembagian tanggung jawab: API vs murni client

Draft produk aslinya (animasi tinta-merembes, efek glow, threshold 3-lapis kapan harus manggil AI, daftar respons lokal buat sapaan) **hampir semuanya murni kerjaan client** — cuma satu titik yang benar-benar nyentuh network:

| Lapis (dari draft produk) | Butuh API? | Catatan |
|---|---|---|
| Animasi serap kertas + glow | ❌ Tidak | Visual murni, jalan dari karakter pertama yang diketik. |
| Respons lokal (sapaan, kata ambigu 1-3 kata) | ❌ Tidak | Daftar hardcode di client, dipilih random. Reveal animasinya sama persis kayak respons AI, cuma sumber teksnya beda — user gak bisa bedain. |
| Input bermakna (≥4-5 kata + jeda ketik ~1.5-2 detik) | ✅ Ya | **Ini satu-satunya titik yang manggil `POST /peny/reply`.** |

Kalau lagi bangun trigger/threshold logic-nya, jangan panggil API di luar kasus ketiga — dua kasus pertama sengaja didesain zero-network supaya kertas selalu "hidup" tanpa buang energy/biaya API tiap kali user ngetik "hai".

---

## 2. Alur pemakaian yang disarankan

**Saat compose screen dibuka:**
1. `GET /peny/status` sekali → simpan `is_enabled` di state screen. Kalau `false`, jangan render trigger sihir sama sekali (bukan render-lalu-disable) — fitur bisa dimatikan kapan saja dari webadmin tanpa app perlu tahu lebih detail dari boolean ini.
2. Kalau user bukan premium, sama juga — jangan render trigger (cek `is_premium` dari `GET /auth/me` yang sudase ada di state app, gak perlu request baru). `is_enabled` dan premium adalah dua gate independen di backend (lihat `api_contract.md`), tapi dari sisi UI keduanya sama-sama alasan buat **tidak menampilkan** trigger — user gak perlu tau bedanya.

**Saat mode sihir diaktifkan (trigger di-tap):**
3. Draft surat asli disimpan di memory (state existing, tidak berubah). TextField beralih ke mode percakapan. Buat array kosong di memory: `penyHistory: List<Turn>` — ini **cuma hidup di memory**, tidak ditulis ke Room/DataStore/disk apa pun (draft produk eksplisit: "rahasia yang lenyap, bukan yang dikoleksi").

**Tiap giliran user ngetik & jeda (lolos threshold Lapis 3, lihat §1):**
4. Panggil `POST /peny/reply` dengan body:
   ```json
   { "history": penyHistory, "message": "<teks yang baru diketik>" }
   ```
   `penyHistory` di titik ini **belum** termasuk giliran yang baru ini — cuma giliran-giliran sebelumnya.
5. Response sukses → append **dua** entri ke `penyHistory`: `{role: "user", text: message}` lalu `{role: "peny", text: response.reply}`. Render `response.reply` lewat animasi reveal (fade-in, bukan typewriter — lihat draft produk soal detail animasi, di luar scope dokumen ini).
6. Update tampilan saldo Energy langsung dari `response.current_energy_balance` — **jangan** refetch `GET /auth/me` cuma buat ini, response-nya sudah kasih angka terbaru (sama pola kayak `POST /stamps/{id}/unlock`).

**Saat mode sihir dimatikan (atau user keluar dari compose screen):**
7. `penyHistory` di-drop total dari memory. Kertas kembali nampilin draft surat asli (state yang dari langkah 3 tadi, tidak pernah tersentuh). **Tidak ada** API call buat "mengakhiri sesi" — statelessness-nya murni di sisi client, server memang tidak pernah tahu sesi ini "mulai" atau "selesai".

---

## 3. Nanganin tiap error case

| Response | Kapan terjadi | Yang harus ditampilkan ke user |
|---|---|---|
| `400 message is required` / `400 message is too long` | Seharusnya sudah dicegah validasi client (Lapis 3 threshold + batas panjang) — kalau tetap muncul, itu bug di client, bukan skenario normal user. | Jangan tampilkan raw error; treat sebagai bug, log buat debug. |
| `400 INSUFFICIENT_ENERGY` | Skenario nyata & terduga — user kehabisan Energy di tengah ngobrol. | **Bukan error generik** — tampilkan pesan hangat sesuai karakter Peny (bukan dialog error sistem), dan idealnya arahkan ke cara dapat Energy lagi (daily check-in/quest, `docs/be/rewards_api.md`). Giliran user yang barusan diketik **tidak** ke-append ke `penyHistory` karena gagal — biarkan tetap di TextField/hilang sesuai desain "diserap" biar user bisa retry setelah dapat Energy. |
| `403 premium only` | Seharusnya gak kejadian kalau trigger memang di-hide buat non-premium (§2 langkah 2). Kalau tetap muncul (mis. status premium berubah di tengah sesi), fallback ke paywall/upgrade prompt. | |
| `403 peny mode is currently unavailable` | Seharusnya gak kejadian kalau `GET /peny/status` dicek di awal (§2 langkah 1). Fallback: sembunyikan trigger, kasih pesan singkat "lagi nggak bisa dipakai sementara". | |
| `500 ...` | AI provider gagal/timeout — energy **tidak** terpotong, aman untuk auto-retry atau minta user coba lagi. | Animasi "gagal" yang halus (mis. tinta yang balik memudar), bukan dialog error teknis — biar gak merusak ilusi kertas hidup. |

---

## 4. Ringkasan: apa yang disimpan di mana

| Data | Lokasi | Catatan |
|---|---|---|
| Draft surat asli | Client, state compose screen yang sudah ada | Tidak pernah disentuh selama mode sihir aktif. |
| `penyHistory` (percakapan sesi ini) | Client, **in-memory saja** | Drop total saat mode dimatikan/screen ditutup — jangan taruh di Room/DataStore. |
| Persona, model AI, harga Energy, keyword krisis | Server (`peny_config`, diatur webadmin) | Client tidak pernah lihat/kirim ini — cukup panggil `GET /peny/status` buat tau on/off. |
| Saldo Energy | Server, sumber kebenaran — client cuma cache dari response terakhir (`current_energy_balance`, atau `GET /auth/me`) | Jangan hitung sendiri di client (mis. `saldo - 5` optimistic sebelum response datang) — tunggu response biar konsisten kalau ada race dengan aksi belanja Energy lain. |

---

## 5. Quick reference kontrak

Detail lengkap & final selalu di `docs/api_contract.md` §`peny` — ringkasan di sini cuma buat orientasi cepat, bisa basi kalau ada perubahan field:

```
GET  /api/v1/peny/status
→ { "is_enabled": bool }

POST /api/v1/peny/reply
→ { "history": [{role, text}], "message": string }
← { "reply": string, "source": "ai"|"crisis_fallback", "energy_spent": int, "current_energy_balance": int }
```
