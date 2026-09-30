# Monetization Brainstorm — Digital Art / Stamps / Supporter Club

Status: brainstorm, belum ada keputusan final. Ditulis berdasarkan struktur kode yang sudah ada di `stamps/` feature module per 2026-08-24.

## Kondisi kode saat ini

- Soft-currency **Energi Peni** ⚡ — didapat gratis dari daily check-in streak (`DailyStreakRewardCard`) dan daily quests (`DailyQuestsSection`). Loop engagement ini sudah live.
- `StampPackEntry` (6 desain stamp) dan `PaperPackEntry` (8 template kertas) — koleksi kosmetik yang dulunya bisa dibeli pakai ⚡, tapi section beli-nya **di-comment out** di `StampsScreen.kt` dan diganti `ComingSoonBottomSheet`.
- `SubscriptionPlanEntry` untuk Supporter Club (Rp 25.000/bulan) juga masih dummy, nunggu integrasi RevenueCat.
- Setiap tap ke fitur yang di-comment (`onBuyStampPackClick`, `onBuyPaperPackClick`, `onJoinSupporterClubClick`) sekarang cuma men-set `comingSoonFeature` state di `StampsViewModel` — belum ada tracking/analytics di titik ini.

## 1. Tunda monetisasi vs bangun sekarang?

Bukan pilihan biner — jalan paralel:

- **Alat validasi demand yang sudah ada dan gratis**: instrument analytics event di `comingSoonFeature` (fitur apa yang di-tap, berapa kali, oleh siapa). Ini kasih sinyal purchase-intent nyata sebelum invest bangun CMS + payment sama sekali.
- **Sekarang**: biarkan loop gratis (streak + quest) jadi mesin retensi & growth. Fokus marketing ke situ.
- **Trigger buat mulai bangun payment**: bukan tanggal, tapi metrik — begitu retensi D7/D30 sehat DAN tap-rate ke "coming soon" store cukup tinggi, itu tanda user sudah kecanduan loop-nya dan siap ditawari upgrade.
- Jangan bangun CMS besar + marketplace kreator dulu — investasi paling mahal (moderasi, payout, IP). Taruh paling akhir, setelah demand tervalidasi.

## 2. Ide business model — art & digital product

Urutan dari paling murah dibangun ke paling mahal:

1. **Cosmetic IAP langsung pakai infra yang sudah ada** — `StampPackEntry` / `PaperPackEntry` tinggal dikasih harga Rupiah selain harga energi. Tambahan ide: wax seal, washi tape overlay, warna tinta, font tulisan tangan, bundle "stationery set" musiman/kolaborasi artis.
2. **Season pass / battle pass di quest system yang sudah ada** — `DailyQuestsSection` + `DailyStreakRewardCard` adalah infra yang pas buat premium track (reward gratis vs reward premium di quest yang sama). Effort implementasi kecil karena data model quest sudah ada; battle pass terbukti salah satu model revenue tertinggi di kategori gratis-plus-mikrotransaksi.
3. **Supporter Club subscription** — lihat poin 3.
4. **Creator marketplace** (paling relevan sama "jualan art") — buka slot buat ilustrator luar submit desain stamp/paper, kurasi internal, revenue share (mirip stiker LINE / preset VSCO, misal split 70/30). Ini butuh CMS beneran (upload, review, approve, payout) — taruh di fase paling akhir setelah demand poin 1 tervalidasi.

## 3. Ide benefit Supporter Club

App ini inherently sosial — satu surat dibaca orang lain (penerima). Value terbaik bukan cuma buat pembeli, tapi buat **apa yang dilihat penerima surat**:

- Wax seal/border eksklusif pada surat yang keliatan sama penerima — status yang terlihat orang lain, bukan cuma di profil sendiri.
- Drop stamp/paper eksklusif bulanan khusus supporter — alasan buat tetap subscribe tiap bulan (ini juga argumen kenapa CMS ringan perlu dibangun bareng fitur ini, karena butuh jadwal rilis konten baru).
- Multiplier energi dari streak/quest — percepat koleksi tanpa merusak ekonomi gratis (bukan pay-to-win karena app non-kompetitif).
- Early access pack baru (misal 48 jam sebelum user gratis).

## 4. Keputusan: positioning Supporter Club — jujur, bukan transaksional

Supporter Club dijual sebagai dukungan tulus ke PenyPost (donation-style), bukan "bayar dapat fitur X/Y/Z". Skema creator marketplace (poin 2.4) resmi **ditunda ke next phase**, baru dikerjakan kalau Supporter Club terbukti ada yang mau bayar.

Teknik copywriting yang dipakai (semua legit, bukan dark pattern):

- **Founding member framing** — cohort awal beneran terbatas karena waktu (bukan fake scarcity), dapat pengakuan permanen di profil.
- **Transparency copy** — bilang terus terang uangnya buat apa (server + development), transparency itu sendiri jadi trust signal buat app indie.
- **Belonging, bukan FOMO** — framing positif, hindari framing rugi/mengancam.
- **Social proof jujur** — angka supporter aktif beneran, bukan dikarang.
- **Reciprocity kecil** — 1 item gratis pas subscribe pertama kali.

Guardrail: hindari dark pattern klasik (fake countdown, sembunyiin info auto-renew, confirmshaming) — kontradiksi sama value "jujur" yang mau dibangun, dan kalau ketauan malah ngerusak trust yang jadi modal utama app kecil.

### Draft copywriting (siap dipindah ke `strings.xml` saat implementasi)

**Hero / paywall headline:** "Jadi Bagian dari Perjalanan Peny"

**Body copy:** "PenyPost dibuat dan dirawat sepenuh hati, tanpa investor besar di belakangnya. Kalau kamu suka nulis surat di sini, dukungan kamu langsung menghidupi biaya server dan pengembangan fitur baru — bukan buat beli fitur, tapi biar Peny bisa terus terbang."

**3 tier:**

| Tier | Harga | Copy |
|---|---|---|
| Teman Peny | Rp 15.000/bln | "Dukungan kecil, tapi berarti besar buat kami." |
| Sahabat Peny (badge "Paling Disukai") | Rp 29.000/bln | "Pilihan favorit para pendukung. Terima kasih sudah jadi bagian dari perjalanan ini." |
| Penjaga Peny | Rp 59.000/bln | "Buat kamu yang pengen PenyPost berkembang lebih jauh, lebih cepat." |

**Reciprocity line (bawah CTA):** "Sebagai ucapan terima kasih pertama, kamu langsung dapat 1 stamp eksklusif begitu gabung."

**Transparency footer:** "Dukunganmu 100% dipakai buat biaya server & pengembangan — bukan buat beli fitur, cuma buat bilang makasih."

**CTA button:** "Dukung PenyPost"

**Badge permanen di profil:** "Supporter sejak {bulan} {tahun}"

## 5. RevenueCat & iOS timing

- Setup RevenueCat duluan di Android dengan skema IAP: produk consumable/non-consumable (stamp/paper pack) + subscription (Supporter Club, 3 tier di atas).
- Pakai fitur **RevenueCat Paywalls + Experiments** buat A/B test harga tanpa perlu update app — harga di poin 4 masih hipotesis, biar data yang mutusin titik harga final.
- **iOS ditunda** ke next phase, sama seperti creator marketplace. Apple Developer Program $99/tahun kecil secara nominal, tapi biaya sebenarnya ada di effort maintain dua codebase + dua App Store review process sebelum model monetisasi Android-nya terbukti kepakai. Trigger buat mulai iOS: begitu eksperimen IAP di Android sudah kasih sinyal ada yang beneran bayar.

## Next steps yang mungkin

- Instrument event analytics di `ComingSoonBottomSheet` / `comingSoonFeature` untuk mulai kumpulkan data demand.
- Setup RevenueCat di Android: produk IAP (stamp/paper pack) + subscription (3 tier Supporter Club).
- Pasang draft copywriting poin 4 ke `strings.xml`, lokalisasi ke Inggris kalau perlu (strings lain di app saat ini berbahasa Inggris).
- Jalankan RevenueCat Experiment buat validasi titik harga sebelum dikunci permanen.
- Scope CMS webadmin minimal viable: CRUD sederhana untuk entity yang sudah ada (`StampPackEntry`, `PaperPackEntry`, `SubscriptionPlanEntry`) sebelum mikir moderasi/submission pihak ketiga.
- Creator marketplace & iOS: revisit setelah ada sinyal Supporter Club/IAP kepakai.
