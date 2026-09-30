# Envelope Catalog API

Backend baru untuk `EnvelopePickerSheet.kt` — mirroring `GET /stamps` (`docs/be/stamps_api.md`'s sibling, sudah live) persis, biar admin bisa nambah desain amplop baru dari CMS tanpa release app baru. Sebelum endpoint ini ada, client render 5 desain bundled (`ENVELOPE_1`..`ENVELOPE_5`, aset lokal `res/drawable-widecg/envelope_*.webp`) sebagai fallback — lihat `app/src/main/java/com/apps/unsealed/ui/screens/selectrecipient/CatalogFallback.kt`.

Semua endpoint `/api/v1`, butuh Bearer token.

## `GET /api/v1/envelopes`

Response `200`: array `EnvelopeResponse` — **shape sama persis** dengan `StampResponse` (`docs/be/api_contract.md`'s `GET /stamps`):
```json
[{ "id": "ENVELOPE_2", "name": "Envelope 2", "image_url": "https://...", "is_premium": false }]
```

| Field | Type | Catatan |
|---|---|---|
| `id` | string | Ini yang dikirim balik sebagai `envelope` field di `POST /letters` — **wajib** seed 5 baris dengan id `ENVELOPE_1`..`ENVELOPE_5` supaya surat lama/yang dikirim sebelum catalog online tetap valid dan konsisten sama enum `EnvelopeDesign` yang sudah ada di client. Id baru boleh string apa aja (mis. `env_procreate_floral`) buat desain yang ditambah lewat CMS nanti. |
| `name` | string | Label buat admin/tampilan, bebas. |
| `image_url` | string | Aset landscape 1720×900px (rasio `EnvelopeAspectRatio` yang sudah dipakai client) — di-serve dari R2/CDN yang sama dengan `composite_image_url`. |
| `is_premium` | boolean | Belum ada gating di client sekarang (selalu treated bisa dipilih) — field-nya disiapkan buat nanti nyambung ke Stamps & Store (`docs/be/stamps_api.md`, M6), bukan dipakai aktif hari ini. |

Client-side: `feature/catalog/data/{CatalogApi,CatalogDto,CatalogRepository}.kt` — `CatalogItemDto` dipakai identik buat envelope & stamp, di-cache in-memory per sesi. Kalau endpoint ini belum live/gagal, `SelectRecipientViewModel` diam-diam fallback ke 5 desain bundled di atas — jangan expect FE lapor error ke user buat kegagalan endpoint ini, itu memang sengaja (lihat `SelectRecipientViewModel.loadCatalogs()`).

**Jangan disamakan dengan** `GET /api/v1/stamps/catalog` di `docs/be/stamps_api.md` — itu toko-beli-stamp premium (harga, cara dapat), konsep berbeda dari catalog referensi sederhana ini.

## Status: sudah live

`GET /api/v1/envelopes` **sudah diimplementasi penuh** (`app/module/envelope/` — handler/service/repository), bukan lagi roadmap. Shape response di atas cocok dengan `dto.EnvelopeResponse` (`app/module/envelope/dto/envelope_dto.go`).

- Admin CRUD buat nambah/edit desain (CMS side) — di luar scope dokumen ini.
