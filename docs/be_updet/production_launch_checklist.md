# Production launch checklist (BE)

Checklist buat tim BE sebelum "beneran" ngebuka Unsealed API ke production traffic (real users, bukan dev/testing). Bukan status report — ini daftar yang perlu di-cek/dieksekusi, dengan catatan apa yang udah diverifikasi read-only per 2026-08-18.

**Snapshot DB saat ini** (dicek read-only lewat `DATABASE_URL` di `.env`, connect ke Railway/Postgres):

| Tabel | Rows | Catatan |
|---|---|---|
| `users` | 1 | 1 akun nyata (`hani`, `is_seed=false`), dibuat 2026-07-28 — bukan seed persona |
| `letters` | 0 | belum ada surat sama sekali |
| `stamps` / `stamp_catalog` | 16 / 7 | udah keisi lewat migration seed |
| `stickers` | 21 | udah keisi lewat migration seed |
| `paper_catalog` | 3 | udah keisi lewat migration seed |
| `envelopes` | 5 | udah keisi lewat migration seed |
| `admin_audit_log` | 0 | belum ada aksi admin/moderation sama sekali |

Kesimpulan: schema & katalog item (stamps/stickers/papers/envelopes) udah siap, tapi **belum ada konten/user** — DB ini efektif masih kosong buat kebutuhan "kerasa hidup" pas user pertama masuk (lihat §4 seed accounts).

---

## 1. Migrations (`db/migrations/001` s/d `027`)

- Total 27 file migration, semua `IF NOT EXISTS`/idempotent, di-apply otomatis tiap app start lewat `RunMigrations()` (`app/bootstrap/app.go`) — **gak ada tracking table**, jadi semua file di-`exec` ulang tiap boot (aman karena idempotent, tapi berarti startup time nambah dikit tiap deploy sebanding jumlah file, akan terus nambah ke depannya).
- Migration terbaru (`027_letters_recipient_nickname.sql`) nambah kolom `recipient_nickname` + backfill dari `users` — udah dicek row count `letters` = 0, jadi backfill-nya instant, gak ada risiko lock/long transaction. Detail perubahan ini ada di `docs/be/letters_api.md` §"Gap (2026-08-18)".
- **Action item:** kalau launch ditunda & `letters` udah keisi banyak row beneran sebelum deploy jalan, cek ulang row count (`SELECT count(*) FROM letters`) — migration `ADD COLUMN` sendiri tetap aman (metadata-only di Postgres 11+), tapi backfill UPDATE-nya sebanding jumlah row.
- **Action item:** belum ada migration tracking table (`schema_migrations` dsb.) — kalau tim mulai lebih dari satu orang ngerjain migration bersamaan, worth dipertimbangkan supaya gak ada 2 migration beda nomor collision atau re-run yang gak diniatkan. Bukan blocker buat launch pertama, tapi worth dicatat sebagai tech debt.

## 2. Environment / secrets (Railway)

Semua env var ini **wajib** (`config.Load()` pakai `mustEnv`, app `log.Fatal` kalau kosong):

- [ ] `DATABASE_URL` — udah kekonfirmasi jalan (dipakai buat semua read-only check di doc ini).
- [ ] `FIREBASE_CREDENTIALS` — service account JSON buat Auth + FCM push. **Verify**: project Firebase yang dipakai udah yang production (bukan dev/staging Firebase project), APNs/FCM key masih valid & belum expired.
- [ ] `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` / `R2_ENDPOINT` / `R2_BUCKET` / `R2_PUBLIC_URL` — bucket Cloudflare R2 buat composite image surat & foto profil. **Verify**: bucket production terpisah dari bucket dev (biar test upload gak numpuk sama data real), dan `R2_PUBLIC_URL` udah nunjuk domain yang benar (bukan default R2 domain kalau ada custom domain).
- [ ] `GEMINI_API_KEY` — dipakai `suggest` module. **Verify**: quota/billing cukup buat traffic production, bukan key trial gratis yang gampang kena rate-limit.
- [ ] `ADMIN_API_KEY` — gate semua `/api/v1/admin/**` (`mw.AdminKey`, dicek di `app/router/router.go:65`). **Verify**: value-nya udah rotated dari default/dev value, dan cuma dipegang tim ops (lihat catatan di `docs/be/admin_dashboard_web.md:12`: "jangan pernah di-hardcode/commit ke repo dashboard").
- [ ] `PORT` — optional (`getEnv` fallback `8080`), biasanya di-inject otomatis sama Railway, gak perlu action.

**Action item:** gak ada cara buat saya verify nilai env var ini di sisi Railway dari sini (cuma bisa liat `.env` lokal) — perlu dicek manual di Railway dashboard sebelum go-live, terutama Firebase & R2 project mana yang kepasang (dev vs prod).

## 3. Server hardening (gap yang ditemukan, worth didiskusiin sebelum production traffic beneran ramai)

Dari `app/bootstrap/fiber.go`:

- **CORS**: `cors.New()` dipanggil tanpa config eksplisit → default gofiber, `Allow-Origin: *` buat semua origin. Buat API yang consumer utamanya mobile app, ini biasanya gak masalah (mobile client gak kena CORS), tapi kalau nanti ada admin dashboard web di domain lain (`docs/be/admin_dashboard_web.md`), worth di-restrict ke domain dashboard spesifik + karena semua endpoint sensitif tetap butuh Bearer token/Admin key, risk-nya rendah — tapi ini keputusan eksplisit yang worth didokumentasiin, bukan default yang kebetulan.
- **Rate limiting**: gak ada middleware rate-limit sama sekali di `app/bootstrap/fiber.go` — semua endpoint (termasuk `POST /letters`, `POST /auth/*`) gak dibatasi. Worth dipertimbangkan minimal buat endpoint yang murah di-abuse (send letter spam, auth brute-force) sebelum traffic publik masuk.
- **Graceful shutdown**: `app.Listen(":" + env.Port)` di `app/bootstrap/app.go` gak dibungkus signal handling (`signal.Notify` + `app.ShutdownWithContext`) — pas Railway restart/redeploy, request yang lagi in-flight bisa keputus mendadak alih-alih di-drain dulu. Worth ditambahin kalau deploy frequency bakal tinggi pasca-launch (tiap deploy = potential dropped request).
- Ketiga ini **bukan launch-blocker teknis** (app tetap jalan & aman secara auth), tapi worth keputusan sadar dari tim sebelum traffic production ramai, bukan kebetulan default framework.

## 4. Data & content readiness

- **Seed accounts (chicken-egg problem)** — `docs/be/seed_accounts_api.md` udah nyebut eksplisit: app pen-pal butuh kepadatan konten/user lain dari awal biar gak berasa kosong. Endpoint admin buat bikin & operate seed persona (`is_seed` column ada sejak migration `003`) udah live, tapi **saat ini 0 seed persona ada di DB** (`users` cuma 1 row, bukan seed) dan **0 surat**. Ini **action item paling konkret buat konten/ops sebelum buka user asli**: generate seed personas + kirim beberapa surat awal (public/showcase) via endpoint admin, supaya PenPals feed & showcase gak kosong pas user pertama masuk.
- Katalog (stamps/stickers/papers/envelopes) udah keisi lengkap via migration seed — gak ada action item di sisi ini.
- `admin_audit_log` kosong — bukan masalah, itu emang bakal keisi natural begitu ada aksi moderasi.

## 5. Background jobs

- `delivery` module (`app/module/delivery/service`) — worker in-process (bukan Railway cron terpisah, dikonfirmasi di `docs/be/notifications_api.md`) yang jalanin delivery + push notification. Start dipanggil sebelum `app.Listen` (`app/bootstrap/app.go`), jadi otomatis aktif tiap instance nyala.
- `dailystack` module — generator stack harian penpal, sama pola (`Start(ctx)` in-process).
- **Action item:** kedua job ini jalan **per-instance** (goroutine in-process, bukan job terpisah/leader-election). Kalau nanti Railway di-scale ke >1 instance, perlu dipastikan job-nya gak dobel-jalan (mis. delivery yang sama diproses 2x, atau daily-stack generation race). Saat ini masih 1 instance jadi aman, tapi ini keputusan yang perlu direvisit **sebelum** horizontal scaling, bukan sesuatu yang otomatis aman.

## 6. Known feature gaps (bukan launch-blocker BE, tapi biar semua orang aware)

Dikutip dari status yang udah ada di `docs/be/*.md`, murni informational — semua ini **sudah** ada keputusan produk buat ditunda, bukan hal yang perlu buru-buru dikerjain:

- **Report/Block bukan gap** — koreksi dari draft sebelumnya. `trust` module (`POST /reports`, `POST/DELETE/GET /blocks`) & `admin` module (suspend/ban/moderation-action/report management) **udah full live** (`829cbf7`, `78c7c3c`), block juga udah otomatis nyaring feed. `docs/be/moderation_api.md` sempat nulis status "belum diimplementasi" tapi itu doc lama yang gak di-update pasca-implementasi — udah dikoreksi (2026-08-18). Gak ada action item BE di sini.
- `GET /supporter/tiers` sengaja `is_active: false`, RevenueCat belum dikonfigurasi — didesain post-launch (`docs/be/stamps_api.md:225`). Ini yang beneran gap/ditunda.
- Beberapa nilai "harga" (`UnlockEnergyCost`, `RefreshFeedEnergyCost`) masih placeholder, belum keputusan produk final — gak nge-block launch, tapi jangan kaget kalau nanti sering berubah.
- Sticker picker UI masih stub di client (`docs/be/stickers_api.md`) — BE-nya udah siap duluan.

## 7. Perubahan terbaru yang relevan (`recipient_nickname`)

Detail lengkap ada di `docs/be/letters_api.md` § `GET /api/v1/letters/{id}` "Gap (2026-08-18)" dan Roadmap. Ringkas:

- Migration `027_letters_recipient_nickname.sql` + kode di `letter_service.go`/`letter_repository.go` udah nambahin `recipient_id`/`recipient_name` ke response `GET /letters/inbox`, `/letters/sent`, `GET /letters/{id}`. Additive, gak breaking.
- `go build`/`go vet`/`go test ./app/module/letter/...` semua pass.
- ~~Caveat: surat `visibility=public` (`recipient_id` = pseudo-id `"penpal:<region>"`) berpotensi `recipient_name` kosong/gagal~~ — **diklarifikasi & sebagian dibenerin (2026-08-19)**. Ternyata dua hal beda:
  - `recipient_name` kosong buat surat public itu **expected by design**, bukan bug — pseudo-recipient `"penpal:<region>"` memang bukan user beneran, gak punya nama buat didenormalize. Gak perlu di-fix.
  - Yang **beneran bug** (baru ketemu & udah difix): user asli (bukan admin/seed) yang coba `POST /letters` dengan `visibility: "public"` bakal **gagal total** (`400 user not found`), karena `sendInternal` (`letter_service.go`) nyoba lookup pseudo-id itu ke tabel `users` kayak recipient biasa. Sebelumnya cuma jalur admin (`PostToFeedAsSeed`, khusus seed persona) yang bisa post ke feed publik — user asli sama sekali gak bisa. Sekarang `sendInternal` skip lookup itu kalau `visibility=="public"` dan validasi format `"penpal:<region>"`-nya langsung, jadi user asli sekarang bisa post ke feed publik juga.
- **Action item pasca-deploy:** smoke test (a) `POST /letters` (private) → `GET /letters/{id}` → pastikan `recipient_name` keisi, dan (b) `POST /letters` (`visibility: "public"`, sebagai user asli) → pastikan berhasil (bukan `400`) dan langsung nongol di `GET /penpals/feed`.

---

## Ringkasan action items (urutan prioritas)

1. **Konten**: generate seed personas + surat awal (§4) — biar app gak berasa kosong pas user pertama masuk. Ini yang paling nentuin "kerasa production-ready" atau nggak dari sisi pengalaman user.
2. **Env var verification** di Railway dashboard (§2) — terutama Firebase project & R2 bucket yang kepasang bener-bener yang production.
3. **Keputusan sadar** soal CORS/rate-limit/graceful shutdown (§3) — gak wajib sebelum launch pertama kalau traffic awal kecil & terkontrol, tapi jangan didiemin selamanya.
4. **Revisit background job scaling** (§5) sebelum nambah instance Railway.
5. Smoke test `recipient_nickname` pasca-deploy (§7) & update status doc.
