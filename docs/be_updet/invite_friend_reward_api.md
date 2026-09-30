# Invite Friend — Energy Reward (Referral Bonus)

**Status: 🔜 Request baru, belum dibangun.** Dokumen ini nambahin *reward* ke fitur invite yang endpoint-nya udah BE-implemented (`docs/be_updet/invite_friend_api.md` §2) — nge-resolve item roadmap yang sempat digantung di situ (§7: *"Reward/insentif buat invite sukses ... belum ada keputusan produk"*). Keputusan produknya udah diambil, ditulis di §1 di bawah. Ini bukan endpoint baru yang berdiri sendiri — numpang penuh di dua sistem yang udah ada: `invites` table (`invite_friend_api.md`) dan quest/energy economy (`rewards_api.md`).

**Baca dulu sebelum ngerjain:** `invite_friend_api.md` (khususnya §2 & §4) dan `rewards_api.md` (khususnya §3/§4, format `DailyQuestDto`/claim). Dokumen ini asumsiin kamu udah familiar sama dua itu, gak diulang detailnya di sini.

---

## 1. Keputusan Produk

| Pertanyaan | Keputusan |
|---|---|
| Siapa yang dapat energi? | **Kedua belah pihak** — inviter (yang bikin kode) dapat *referral bonus*, invitee (user baru) dapat *welcome bonus*. |
| Kapan reward cair? | **Bukan** langsung pas redeem/signup — baru cair begitu invitee **berhasil kirim surat pertama mereka** (`POST /letters` sukses, hitungan pertama seumur akun). Sengaja ditunda dari titik redeem supaya lebih defensif terhadap akun kosong/abal-abal yang cuma numpang farm energi (redeem doang gratis & instan; kirim surat asli butuh nulis ≥20 karakter beneran, jauh lebih mahal buat di-script massal). |
| Berapa energinya? | Default yang diajukan di dokumen ini — **+30 Energi buat inviter**, **+15 Energi buat invitee**. Angka final terserah tim produk/BE, gampang diubah (cuma konstanta, gak ngubah struktur). Skala ini sengaja ditaruh di atas quest harian biasa (`quest_send_letter` = 15, lihat `rewards_api.md` §3) karena referral sukses lebih jarang & lebih bernilai buat growth daripada aktivitas harian rutin. |

---

## 2. Kenapa lewat sistem Quest yang udah ada, bukan bikin mekanisme baru

Pendekatan yang diajukan: reward ini muncul sebagai **item baru di `GET /api/v1/rewards/quests`** (satu buat inviter, satu buat invitee — masing-masing di quest list mereka sendiri), diklaim lewat **endpoint claim yang udah ada**, `POST /api/v1/rewards/quests/{quest_id}/claim`.

Kenapa ini pilihan yang lebih baik daripada auto-grant energi langsung (mis. nempelin field baru ke response `POST /letters`):

- **Nol perubahan di Android client.** `DailyQuestsSection`/`StampsViewModel` di client udah generic — render `title`/`description`/`energy_reward` apa adanya dari response, gak ada logic yang hardcode per-`quest.id` (`StampsViewModel.kt`, `DailyQuestsSection.kt`). Item quest baru otomatis muncul & bisa diklaim tanpa app perlu update.
- **UX yang udah teruji dipakai lagi** — celebration dialog + confetti pas klaim quest (`ClaimRewardCelebrationDialog`, `StampsScreen.kt`) otomatis jalan buat reward ini juga, gak perlu desain flow baru.
- **Konsisten:** semua "dapat energi" di app selalu lewat aksi eksplisit klaim quest/daily-reward, bukan ada satu jalur diam-diam auto-credit di tengah endpoint yang gak berhubungan (`POST /letters`). Lebih gampang di-audit & di-debug kalau user komplain saldo energi ganjil.

Trade-off yang disadari: quest yang ada sekarang semuanya *daily* (reset tiap hari). Reward ini **bukan** daily — sekali muncul, harus tetap ada di list sampai diklaim (gak boleh ke-reset/hilang keesokan harinya sebelum sempat diklaim), karena user bisa aja gak buka app selama beberapa hari setelah reward-nya unlock. Detail teknis di §5.

---

## 3. Precondition fix yang WAJIB dulu sebelum reward ini aman dibangun

**Gap di `invites` table sekarang:** gak ada constraint yang mencegah **satu user redeem lebih dari satu kode invite**. `chk_invite_not_self` cuma cegah self-invite; gak ada yang cegah User X redeem kode dari Inviter A, terus besoknya redeem kode lain dari Inviter B juga.

Tanpa reward, ini harmless (paling cuma dua-duanya jadi address-book contact X). **Dengan** reward, ini jadi celah abuse: satu invitee (atau grup akun yang berkoordinasi) bisa redeem banyak kode dari banyak "inviter" (termasuk akun sendiri) dan berpotensi ke-multiple-count kalau logic-nya gak hati-hati. Fix-nya di §5 di bawah (kolom + partial unique index) — **kerjain ini duluan**, sebelum nyalain reward.

---

## 4. Perubahan Database

Migrasi baru, lanjutin nomor dari migrasi terakhir (invites ada di `030_invites.sql` per status `invite_friend_api.md` — cek nomor aktual terbaru sebelum nentuin nomor final):

```sql
-- 1. Cegah satu user redeem lebih dari satu invite seumur hidup (§3).
--    Partial unique index (bukan UNIQUE constraint biasa) karena kolom ini
--    nullable buat baris yang belum di-redeem sama sekali.
CREATE UNIQUE INDEX IF NOT EXISTS uq_invites_redeemed_by_user_id
    ON invites(redeemed_by_user_id)
    WHERE redeemed_by_user_id IS NOT NULL;

-- 2. Idempotency guard — nandain invite ini reward-nya udah pernah dicairkan,
--    supaya panggilan POST /letters berikutnya (atau retry/concurrent call)
--    gak generate quest dobel.
ALTER TABLE invites ADD COLUMN IF NOT EXISTS reward_granted_at TIMESTAMPTZ;

-- 3. Cuma perlu kalau kolom belum ada di `users` — dipakai buat deteksi
--    "ini beneran surat pertama sender seumur akun", biar gak query
--    COUNT(*) FROM letters tiap kali user kirim surat (lihat §5 langkah 1).
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_letter_sent_at TIMESTAMPTZ;

-- 4. Tempat nyimpen dua "quest instance" (inviter + invitee) yang muncul
--    sekali per invite sukses. Terpisah dari apapun mekanisme penyimpanan
--    quest harian yang sekarang (rewards_api.md §6 gak nyebut tabel quest
--    eksplisit — kemungkinan besar daily quest dihitung on-the-fly dari
--    tabel lain; referral quest BEDA karena harus dia sendiri yang
--    persisted, karena "kejadian"-nya (invite sukses) gak bisa dihitung
--    ulang dari data lain kapan aja seperti quest harian bisa).
CREATE TABLE IF NOT EXISTS referral_reward_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_id UUID NOT NULL REFERENCES invites(id) ON DELETE CASCADE,
    beneficiary_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('inviter', 'invitee')),
    energy_reward INT NOT NULL,
    is_claimed BOOLEAN NOT NULL DEFAULT false,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Satu invite cuma pernah generate maksimal 1 quest row per role —
    -- pasangan sama constraint reward_granted_at di atas sebagai
    -- idempotency guard kedua (belt-and-suspenders).
    CONSTRAINT uq_referral_reward_quests_invite_role UNIQUE (invite_id, role)
);
CREATE INDEX IF NOT EXISTS idx_referral_reward_quests_beneficiary
    ON referral_reward_quests(beneficiary_user_id) WHERE is_claimed = false;
```

---

## 5. Logic Hook — di dalam `POST /api/v1/letters`

Tambahan step **setelah** letter berhasil di-insert (transaksi yang sama atau transaksi susulan, yang penting atomik terhadap langkah 1-4 di bawah):

1. Cek `users.first_letter_sent_at` milik sender. Kalau **masih NULL** → ini surat pertama sender seumur akun. Set jadi `now()`. Kalau **sudah ada isinya** → bukan surat pertama, **stop di sini**, gak ada reward apapun buat surat ini (sender bukan lagi kandidat, gak peduli invite status-nya apa).
2. (Lanjut cuma kalau langkah 1 baru pertama kali keisi.) Cari row `invites` dengan `redeemed_by_user_id = sender.id`. Kalau gak ketemu → sender gak pernah masuk lewat invite manapun, **stop**, gak ada reward.
3. Kalau ketemu tapi `reward_granted_at` **sudah** keisi → sudah pernah diproses sebelumnya (harusnya gak mungkin kejadian bareng langkah 1 karena `first_letter_sent_at` udah jadi guard duluan, tapi tetap dicek sebagai defense-in-depth) → **stop**.
4. Kalau ketemu dan `reward_granted_at` masih NULL:
   - Insert 2 baris ke `referral_reward_quests`:
     - `role='inviter'`, `beneficiary_user_id = invites.inviter_user_id`, `energy_reward = 30` (§1).
     - `role='invitee'`, `beneficiary_user_id = sender.id`, `energy_reward = 15` (§1).
   - Set `invites.reward_granted_at = now()`.
   - Kirim push notification ke inviter — event baru, lihat §7.
   - Invitee gak perlu push (dia lagi aktif di app saat ini juga) — quest barunya otomatis kelihatan lain kali dia buka `GET /rewards/quests` (mis. pas balik ke tab Stamps).

Race condition yang perlu diantisipasi: dua request `POST /letters` yang somehow nyampe hampir bersamaan dari sender yang sama (harusnya gak mungkin karena user cuma bisa submit satu compose screen sekaligus, tapi retry/double-tap tetap mungkin) — `UNIQUE(invite_id, role)` di §4 + row-level lock pas baca `invites.reward_granted_at` (mis. `SELECT ... FOR UPDATE`) mencegah dobel insert.

---

## 6. Perubahan di `GET /rewards/quests` & `POST /rewards/quests/{quest_id}/claim`

**`GET /rewards/quests`:** response `items` di-union dengan baris `referral_reward_quests WHERE beneficiary_user_id = viewer AND is_claimed = false`, di-map ke shape `DailyQuestDto` yang udah ada (tidak ada field baru yang wajib — semuanya muat ke shape lama):

```json
{
  "id": "referral_inviter_3f9a2b71",
  "title": "Teman Baru Bergabung! 🎉",
  "description": "Kirana baru saja mengirim surat pertamanya lewat undanganmu",
  "target_count": 1,
  "current_count": 1,
  "energy_reward": 30,
  "is_completed": true,
  "is_claimed": false
}
```
```json
{
  "id": "referral_invitee_3f9a2b71",
  "title": "Bonus Selamat Datang! 🐧",
  "description": "Terima kasih sudah bergabung lewat undangan teman",
  "target_count": 1,
  "current_count": 1,
  "energy_reward": 15,
  "is_completed": true,
  "is_claimed": false
}
```
`id` disarankan pakai prefix `referral_inviter_`/`referral_invitee_` + suffix pendek dari `referral_reward_quests.id` — biar handler `claim` gampang bedain "ini quest harian biasa vs quest referral" cukup dari prefix-nya, tanpa harus query semua sumber tiap kali.

**Tetap ada selamanya sampai diklaim** — beda dari quest harian yang boleh reset tiap hari, baris `referral_reward_quests` yang `is_claimed = false` harus terus muncul di `GET /rewards/quests` sampai user benar-benar klaim, gak peduli udah berapa hari.

**`POST /rewards/quests/{quest_id}/claim`:** kalau `{quest_id}` match prefix `referral_`, jangan lewat logic quest harian yang sekarang ada — ambil `energy_reward` dari row `referral_reward_quests` yang sesuai, tambahin ke saldo energi user (`current_energy_balance`, konsisten sama response shape claim yang sudah ada di `rewards_api.md` §4), set `is_claimed = true` + `claimed_at = now()`. Response shape-nya sama persis kayak claim quest biasa:
```json
{
  "success": true,
  "data": {
    "quest_id": "referral_inviter_3f9a2b71",
    "energy_granted": 30,
    "current_energy_balance": 185,
    "claimed_at": "2026-09-10T09:36:00Z"
  }
}
```
**Error case tambahan:** `404`/`400` kalau `quest_id` berprefix `referral_` tapi row-nya gak ketemu atau bukan milik viewer (`beneficiary_user_id != viewer.id`) — jangan biarkan user klaim quest referral milik user lain cuma dengan nebak/enumerate id.

---

## 7. Push Notification Baru

Event baru, pola persis sama kayak yang udah didokumentasiin di `notifications_api.md` §4 (contoh `address_book_added`) — tambahin baris baru ke matrix §2 dokumen itu:

| Event | Kapan | Data payload |
|---|---|---|
| `referral_reward_unlocked` | Invitee kirim surat pertama mereka, reward buat inviter baru di-generate (§5 langkah 4) | `{"event": "referral_reward_unlocked", "invitee_name": "<nama>"}` |

```go
Notification: &messaging.Notification{
    Title: "Bonus Referral Menunggu! ⚡",
    Body:  fmt.Sprintf("%s baru aja kirim surat pertamanya — ada +30 Energi Peni buat kamu di tab Prangko!", inviteeNickname),
},
Data: map[string]string{
    "event":        "referral_reward_unlocked",
    "invitee_name": inviteeNickname,
},
```
Ini terpisah dari push `invite_redeemed` yang sudah dispesifikasikan di `invite_friend_api.md` §2 (fire lebih dulu, pas redeem/signup) — dua event beda waktu buat dua kejadian beda (redeem vs reward unlock), bukan pengganti satu sama lain. Cross-check ke tim BE: pastikan `invite_redeemed` di dokumen itu emang udah kepasang di §2 `notifications_api.md`'s event matrix — dari yang saya lihat sekarang belum ada barisnya di sana, kemungkinan kelewat waktu invite endpoint dibangun; tambahin bareng kalau memang belum ada.

---

## 8. Anti-abuse

Bar utama udah cukup tinggi dari desain §1 (reward butuh surat asli ≥20 karakter terkirim, bukan cuma signup kosong) + fix wajib di §3 (satu user satu redeem seumur hidup). Belum ada cap tambahan yang di-mandatory-in di MVP ini, tapi kalau nanti kejadian abuse beneran (banyak akun beruntun invite-redeem-kirim-surat-template dari inviter yang sama), gampang ditambahin belakangan tanpa ubah skema: tinggal cek count di `referral_reward_quests WHERE beneficiary_user_id = inviter_id AND role = 'inviter' AND created_at > now() - interval '30 days'` sebelum insert row baru di §5 langkah 4, skip reward (tapi tetap set `invites.reward_granted_at` biar gak infinite-retry) kalau udah lewat cap-nya.

---

## 9. Yang TIDAK berubah (scope guard)

- **Endpoint invite yang udah ada** (`POST /invites`, `POST /invites/{code}/redeem`) — kontrak request/response-nya sama persis, gak ada field baru.
- **Endpoint `POST /letters`'s response shape** — tetap `{id, status, estimated_arrival_at}`, gak nambah field.
- **Android client** — nol perubahan kode diperlukan (§2). `GET /rewards/quests`/`POST /rewards/quests/{quest_id}/claim` yang udah dipanggil `StampsViewModel` otomatis nangkep quest baru ini.
- **Sistem quest harian yang sekarang** — logicnya gak disentuh, referral quest cuma numpang response shape yang sama.

---

## 10. Checklist Implementasi (urutan disarankan)

| Urutan | Task |
|---|---|
| 1 | Migrasi §4 (fix constraint dulu — §3 — baru kolom/tabel baru) |
| 2 | Hook di `POST /letters` (§5) — test manual dulu lewat DB langsung (cek row `referral_reward_quests` muncul), belum perlu UI |
| 3 | `GET /rewards/quests` union query (§6) |
| 4 | `POST /rewards/quests/{quest_id}/claim` branch buat prefix `referral_` (§6) |
| 5 | Push notification `referral_reward_unlocked` (§7) + cross-check `invite_redeemed` beneran udah ada |
| 6 | End-to-end test: akun A generate invite → akun B redeem → B kirim surat pertama → cek A dapat push + quest baru muncul di A & B, dua-duanya bisa diklaim, saldo energi nambah bener |
| 7 (opsional) | Cap anti-abuse §8, kalau/pas dibutuhkan |

---

## 11. Pertanyaan Terbuka Buat Tim BE

- Angka `+30`/`+15` di §1 masih tentatif — sesuaikan ke game balance yang dirasa pas.
- Format persis `id` quest referral (§6, `referral_inviter_<...>`) — silakan sesuaikan ke konvensi internal, yang penting client gak butuh tau formatnya (opaque string).
- Kalau ternyata daily quest list disimpan di tabel yang gak kesebut di `rewards_api.md` §6 (bukan dihitung on-the-fly), pertimbangkan reuse tabel itu alih-alih `referral_reward_quests` baru — dokumen ini asumsiin belum ada tabel yang cocok berdasarkan yang terdokumentasi.
