# 🏪 Plan: Peny Store — Stamps Screen → Creator Marketplace

> **Konteks**: Screen `StampsScreen.kt` yang sekarang hanya punya tab "Hadiah" dan "Energi" akan diubah menjadi sebuah **Post Office / Shop** yang menampilkan karya dari para desainer/kreator, sesuai API spec dari `peny_store_api.md` dan `peny_store_mobile_contract.md`.

---

## Visi Akhir

Screen ini bukan lagi sekadar "Stamps Screen" — melainkan **Peny Store**: sebuah Post Office digital yang berisi karya-karya ilustrator, stamp pack, paper pack, sticker pack, envelope pack, dan bundle dari berbagai kreator. Vibe-nya seperti toko pos artisan atau galeri kecil.

### Keputusan UX: STORE adalah tab pertama & default

**STORE diletakkan paling kiri** sehingga saat user pertama kali membuka screen ini, mereka langsung disambut oleh etalase kreator — bukan Hadiah atau Energi. Urutan tab:

```
[ 🏪 Toko ]  [ 🎁 Hadiah ]  [ ⚡ Energi ]
     ↑
  default
```

Enum `StampsTab` dideklarasi `{ STORE, HADIAH, ENERGI }` — urutan deklarasi menentukan urutan render tab selector. Default `selectedTab` di ViewModel berubah menjadi `StampsTab.STORE` (kecuali ada request eksplisit ke ENERGI dari `StampsEnergyTabRequestHolder`).

### Background Kustom Tab STORE

Tab STORE mendapatkan **background tekstur kayu gelap** dari `drawable/shop_bg.webp` — gambar bertekstur dengan sketch prangko & motif pos yang memperkuat nuansa post office artisan.

Implementasi: Saat `selectedTab == StampsTab.STORE`, seluruh area konten dibungkus dalam `Box` dengan background image sebagai lapisan paling bawah + scrim gelap di atasnya agar teks tetap terbaca.

```
[Peny Store Screen]
├── Header: "Peny Store" + Energi pill (tetap, solid bg)
├── Tab Bar: TOKO | HADIAH | ENERGI
└── Content area:
    ├── TOKO (default) → Background: shop_bg.webp + scrim overlay
    │   ├── Showcase Carousel
    │   ├── Filter Chips: Semua · Prangko · Kertas · Stiker · Amplop · Bundle
    │   ├── Catalog Grid (LazyVerticalGrid, 2 kolom)
    │   │   └── ProductCard: thumbnail, nama, creator, harga, ❤️ likes
    │   └── ProductDetailBottomSheet (saat item di-tap)
    │       ├── Banner/Thumbnail besar
    │       ├── Judul + Deskripsi
    │       ├── Preview aset (horizontal scroll)
    │       ├── Creator card (avatar, nama, bio, link)
    │       ├── Likes count + toggle like ❤️
    │       ├── Harga (IDR / USD)
    │       └── CTA: "Beli" / "Sudah Dimiliki ✓"
    ├── HADIAH → Background: normal (existing)
    └── ENERGI → Background: normal (existing)
```

---

## Arsitektur yang Diikuti

Mengikuti pola yang sudah ada di proyek:
- `feature/<name>/data/` → Api interface + Dto + Repository
- `ui/screens/<name>/` → screen / state / viewmodel / widgets / constants
- Hilt DI (`@HiltViewModel`, `@Inject`)
- `ApiResponse<T>` wrapper (Retrofit + Moshi)

---

## Phase 1 — Data Layer: `feature/store`

### 1.1 DTO (dari `peny_store_mobile_contract.md` Section 5)

**File baru**: `feature/store/data/StoreDto.kt`

```kotlin
@JsonClass(generateAdapter = true)
data class ShowcaseResponseDto(
    @Json(name = "items") val items: List<ShowcaseItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShowcaseItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "showcase_banner_url") val showcaseBannerUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "tier") val tier: String,
    @Json(name = "category") val category: String,
    @Json(name = "creator_name") val creatorName: String = "",
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class CatalogResponseDto(
    @Json(name = "items") val items: List<CatalogItemDto> = emptyList(),
    @Json(name = "next_cursor") val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class CatalogItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "category") val category: String,
    @Json(name = "tier") val tier: String,
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_name") val creatorName: String,
    @Json(name = "stamps_count") val stampsCount: Int = 0,
    @Json(name = "stickers_count") val stickersCount: Int = 0,
    @Json(name = "papers_count") val papersCount: Int = 0,
    @Json(name = "envelopes_count") val envelopesCount: Int = 0,
    @Json(name = "likes_count") val likesCount: Long = 0,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean = false,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PublicCreatorDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "external_link") val externalLink: String = ""
)

@JsonClass(generateAdapter = true)
data class ItemAssetDto(
    @Json(name = "asset_type") val assetType: String, // "STAMP" | "PAPER" | "STICKER" | "ENVELOPE"
    @Json(name = "asset_url") val assetUrl: String,
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class ItemDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_name") val creatorName: String,
    @Json(name = "creator") val creator: PublicCreatorDto,
    @Json(name = "category") val category: String,
    @Json(name = "tier") val tier: String,
    @Json(name = "price_idr") val priceIdr: Long = 0,
    @Json(name = "price_usd") val priceUsd: Double = 0.0,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "sub_description") val subDescription: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "showcase_banner_url") val showcaseBannerUrl: String = "",
    @Json(name = "stamps_count") val stampsCount: Int = 0,
    @Json(name = "stickers_count") val stickersCount: Int = 0,
    @Json(name = "papers_count") val papersCount: Int = 0,
    @Json(name = "envelopes_count") val envelopesCount: Int = 0,
    @Json(name = "views_count") val viewsCount: Long = 0,
    @Json(name = "likes_count") val likesCount: Long = 0,
    @Json(name = "viewer_has_liked") val viewerHasLiked: Boolean = false,
    @Json(name = "viewer_owns") val viewerOwns: Boolean = false,
    @Json(name = "assets") val assets: List<ItemAssetDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class LikeToggleDto(
    @Json(name = "has_liked") val hasLiked: Boolean,
    @Json(name = "likes_count") val likesCount: Long
)
```

### 1.2 API Interface

**File baru**: `feature/store/data/StoreApi.kt`

```kotlin
interface StoreApi {

    @GET("api/v1/shop/showcase")
    suspend fun getShowcase(): ApiResponse<ShowcaseResponseDto>

    @GET("api/v1/items")
    suspend fun getCatalog(
        @Query("category") category: String? = null,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int = 20,
    ): ApiResponse<CatalogResponseDto>

    @GET("api/v1/items/{id}")
    suspend fun getItemDetail(
        @Path("id") id: String,
    ): ApiResponse<ItemDetailDto>

    @POST("api/v1/items/{id}/like")
    suspend fun toggleLike(
        @Path("id") id: String,
    ): ApiResponse<LikeToggleDto>

    @POST("api/v1/items/{id}/view")
    suspend fun incrementView(
        @Path("id") id: String,
    ): ApiResponse<Unit>
}
```

### 1.3 Repository

**File baru**: `feature/store/data/StoreRepository.kt`

```kotlin
@Singleton
class StoreRepository @Inject constructor(
    private val api: StoreApi
) {
    suspend fun getShowcase(): Result<List<ShowcaseItemDto>>
    suspend fun getCatalog(category: String?, after: String?): Result<CatalogResponseDto>
    suspend fun getItemDetail(id: String): Result<ItemDetailDto>
    suspend fun toggleLike(id: String): Result<LikeToggleDto>
    suspend fun incrementView(id: String)  // fire-and-forget, tidak perlu await
}
```

### 1.4 Hilt Module

Tambahkan `StoreApi` ke Hilt NetworkModule (file yang sama dengan `RewardsApi`, `PenpalsApi`, dll.).

---

## Phase 2 — UI Layer: Tab Baru STORE

### 2.1 Tambah Tab STORE ke `StampsTab` enum

**File diubah**: `ui/screens/stamps/constants/StampsConstants.kt`

```kotlin
// Sebelum:
enum class StampsTab { HADIAH, ENERGI }

// Sesudah:
enum class StampsTab { HADIAH, ENERGI, STORE }
```

### 2.2 Filter Category Enum

**File baru**: `ui/screens/stamps/constants/StoreFilterCategory.kt`

```kotlin
enum class StoreFilterCategory(val queryParam: String?, val labelRes: Int) {
    ALL(null, R.string.store_filter_all),
    STAMP("stamp", R.string.store_filter_prangko),
    PAPER("paper", R.string.store_filter_kertas),
    STICKER("sticker", R.string.store_filter_stiker),
    ENVELOPE("envelope", R.string.store_filter_amplop),
    BUNDLE("bundle", R.string.store_filter_bundle),
}
```

### 2.3 UiState untuk Store

**File baru**: `ui/screens/stamps/state/StoreUiState.kt`

```kotlin
data class StoreSection(
    val isLoadingShowcase: Boolean = false,
    val isLoadingCatalog: Boolean = false,
    val showcase: List<ShowcaseItemUi> = emptyList(),
    val catalog: List<CatalogItemUi> = emptyList(),
    val nextCursor: String? = null,
    val selectedFilter: StoreFilterCategory = StoreFilterCategory.ALL,
    val isLoadingMore: Boolean = false,
    val selectedItemId: String? = null,     // trigger open bottom sheet
    val itemDetail: ItemDetailUi? = null,
    val isLoadingDetail: Boolean = false,
    val isTogglingLike: Boolean = false,
    val errorMessage: String? = null,
)

// Domain model — Showcase
data class ShowcaseItemUi(
    val id: String,
    val title: String,
    val subDescription: String,
    val bannerUrl: String,
    val thumbnailUrl: String,
    val tier: String,
    val category: String,
    val creatorName: String,
    val priceIdr: Long,
    val priceUsd: Double,
)

// Domain model — Catalog item (card di grid)
data class CatalogItemUi(
    val id: String,
    val title: String,
    val subDescription: String,
    val thumbnailUrl: String,
    val category: String,
    val tier: String,
    val priceIdr: Long,
    val priceUsd: Double,
    val creatorId: String,
    val creatorName: String,
    val stampsCount: Int,
    val stickersCount: Int,
    val papersCount: Int,
    val envelopesCount: Int,
    val likesCount: Long,
    val viewerHasLiked: Boolean,
)

// Domain model — Detail item (bottom sheet)
data class ItemDetailUi(
    val id: String,
    val title: String,
    val description: String,
    val subDescription: String,
    val thumbnailUrl: String,
    val bannerUrl: String,
    val tier: String,
    val priceIdr: Long,
    val priceUsd: Double,
    val category: String,
    val stampsCount: Int,
    val stickersCount: Int,
    val papersCount: Int,
    val envelopesCount: Int,
    val likesCount: Long,
    val viewerHasLiked: Boolean,
    val viewerOwns: Boolean,
    val assets: List<AssetUi>,
    val creator: CreatorUi,
)

data class AssetUi(
    val assetType: String,  // "STAMP" | "PAPER" | "STICKER" | "ENVELOPE"
    val assetUrl: String,
    val sortOrder: Int,
)

data class CreatorUi(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val externalLink: String,
)
```

### 2.4 Extend `StampsUiState`

**File diubah**: `ui/screens/stamps/state/StampsUiState.kt`

```kotlin
data class StampsUiState(
    // ... semua field existing tetap ...
    val store: StoreSection = StoreSection(),  // ← tambahkan satu field ini
)
```

### 2.5 StampsViewModel — tambah Store functions

**File diubah**: `ui/screens/stamps/viewmodel/StampsViewModel.kt`

Inject `StoreRepository` dan tambahkan fungsi-fungsi berikut:

```kotlin
fun loadStoreData()
// Fetch showcase + catalog (pertama kali masuk tab, atau reset setelah ganti filter)

fun onStoreFilterSelected(filter: StoreFilterCategory)
// Ganti filter, reset cursor ke null, reload catalog

fun loadMoreCatalog()
// Infinite scroll: panggil saat user hampir di ujung list dan nextCursor != null

fun onProductTapped(id: String)
// Set selectedItemId, fetch detail, buka bottom sheet

fun dismissProductDetail()
// Clear selectedItemId + itemDetail

fun toggleLike(itemId: String)
// Optimistic UI (lihat pola di bawah)

fun onViewItemDetail(itemId: String)
// Fire-and-forget, delay 1.5s sebelum increment
```

**Optimistic Like Pattern:**
```kotlin
fun toggleLike(itemId: String) {
    // 1. Langsung flip state di UI
    val wasLiked = _uiState.value.store.catalog.find { it.id == itemId }?.viewerHasLiked ?: return
    _uiState.update { s ->
        val updated = s.store.catalog.map {
            if (it.id == itemId) it.copy(
                viewerHasLiked = !wasLiked,
                likesCount = if (wasLiked) it.likesCount - 1 else it.likesCount + 1
            ) else it
        }
        s.copy(store = s.store.copy(catalog = updated))
    }
    // 2. Panggil API, konfirmasi atau rollback
    viewModelScope.launch {
        val result = storeRepository.toggleLike(itemId)
        if (result.isFailure) {
            // rollback ke state sebelumnya
            _uiState.update { s ->
                val rolled = s.store.catalog.map {
                    if (it.id == itemId) it.copy(viewerHasLiked = wasLiked,
                        likesCount = if (wasLiked) it.likesCount + 1 else it.likesCount - 1)
                    else it
                }
                s.copy(store = s.store.copy(catalog = rolled))
            }
        } else {
            // Konfirmasi dengan server count
            val data = result.getOrThrow()
            _uiState.update { s ->
                val confirmed = s.store.catalog.map {
                    if (it.id == itemId) it.copy(viewerHasLiked = data.hasLiked, likesCount = data.likesCount)
                    else it
                }
                s.copy(store = s.store.copy(catalog = confirmed))
            }
        }
    }
}
```

---

## Phase 3 — UI Composables

### 3.1 Perubahan di `StampsScreen.kt`

**Tab selector** — STORE tampil paling kiri (urutan mengikuti `StampsTab` enum):
```kotlin
// Enum order: STORE, HADIAH, ENERGI → STORE is index 0 = leftmost
listOf(
    StampsTab.STORE  to stringResource(R.string.stamps_screen_tab_store),
    StampsTab.HADIAH to stringResource(R.string.stamps_screen_tab_hadiah),
    StampsTab.ENERGI to stringResource(R.string.stamps_screen_tab_energi),
)
```

**Default tab** di ViewModel (ubah fallback dari `HADIAH` ke `STORE`):
```kotlin
// StampsViewModel.kt
private val _uiState = MutableStateFlow(
    StampsUiState(
        isLoading = true,
        // STORE adalah default; ENERGI hanya saat ada explicit request dari StampsEnergyTabRequestHolder
        selectedTab = if (stampsEnergyTabRequestHolder.consume()) StampsTab.ENERGI else StampsTab.STORE,
    )
)
```

**LaunchedEffect** — lazy load catalog saat pertama kali masuk tab STORE:
```kotlin
LaunchedEffect(uiState.selectedTab) {
    if (uiState.selectedTab == StampsTab.STORE && uiState.store.catalog.isEmpty()) {
        viewModel.loadStoreData()
    }
}
```

**when block** + custom background untuk tab STORE:
```kotlin
Box(modifier = modifier.fillMaxSize()) {

    // Custom background: hanya aktif saat tab STORE
    if (uiState.selectedTab == StampsTab.STORE) {
        Image(
            painter = painterResource(R.drawable.shop_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Scrim gelap di atas texture agar teks & card tetap terbaca
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )
    }

    // Konten utama
    LazyColumn / LazyVerticalGrid {
        // Header + Energi pill + Tab selector → tetap di sini
        ...

        when (uiState.selectedTab) {
            StampsTab.STORE  -> { PenyStoreSection(...) }
            StampsTab.HADIAH -> { /* existing HADIAH content */ }
            StampsTab.ENERGI -> { EnergyStoreSection(...) }
        }
    }
}
```

> **Catatan alpha scrim**: Nilai `0.55f` membuat tekstur kayu masih terlihat tapi tidak mengganggu keterbacaan. Jika background terlalu gelap/terang setelah dicoba di device, sesuaikan antara `0.45f`–`0.65f`.


// LaunchedEffect: lazy load STORE saat pertama masuk tab
LaunchedEffect(uiState.selectedTab) {
    if (uiState.selectedTab == StampsTab.STORE && uiState.store.catalog.isEmpty()) {
        viewModel.loadStoreData()
    }
}

// when block
when (uiState.selectedTab) {
    StampsTab.HADIAH -> { /* existing */ }
    StampsTab.ENERGI -> { /* existing */ }
    StampsTab.STORE  -> { /* PenyStoreSection(...) */ }
}
```

### 3.2 Widget: `PenyStoreSection`

**File baru**: `ui/screens/stamps/widgets/PenyStoreSection.kt`

> [!NOTE]
> Gunakan **`LazyVerticalGrid`** sebagai root composable untuk tab STORE — bukan nested di dalam LazyColumn parent. Showcase banner dan filter chips dijadikan full-span header items (`span = { GridItemSpan(maxCurrentLineSpan) }`).

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    // Header: Showcase Carousel (full span)
    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
        ShowcaseBannerCarousel(items = store.showcase)
    }

    // Header: Filter Chips (full span)
    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
        StoreFilterChips(
            selected = store.selectedFilter,
            onSelect = onFilterSelect,
        )
    }

    // Skeleton saat loading
    if (store.isLoadingCatalog && store.catalog.isEmpty()) {
        items(4, span = { GridItemSpan(1) }) { ProductCardSkeleton() }
    }

    // Catalog items
    items(store.catalog, key = { it.id }) { item ->
        ProductCard(item = item, onClick = { onProductTap(item.id) })
    }

    // Load more indicator (full span)
    if (store.isLoadingMore) {
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandGold)
            }
        }
    }
}
```

### 3.3 Widget: `ShowcaseBannerCarousel`

**File baru**: `ui/screens/stamps/widgets/ShowcaseBannerCarousel.kt`

```
┌─────────────────────────────────────────┐
│                                         │
│   [AsyncImage — ratio 16:9]             │  ← Coil, bannerUrl atau thumbnailUrl
│                                         │
│  ┌─────────────────────────────────┐    │
│  │ Autumn Botanical Collection      │    │  ← title overlay
│  │ oleh Studio Kembara · Rp 35.000 │    │  ← creator + harga
│  └─────────────────────────────────┘    │
└──────── ●  ○  ○ ─────────────────────────┘
                 ^
             dot indicator
```

- `HorizontalPager` (Foundation `pager`)
- Auto-scroll setiap 4 detik via `LaunchedEffect`
- Tap seluruh item → `onProductTap(item.id)`

### 3.4 Widget: `StoreFilterChips`

**Inline di PenyStoreSection atau file tersendiri**

```kotlin
Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
    StoreFilterCategory.entries.forEach { filter ->
        FilterChip(
            selected = selectedFilter == filter,
            onClick = { onSelect(filter) },
            label = { Text(stringResource(filter.labelRes)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandGold.copy(alpha = 0.2f),
                selectedLabelColor = BrandGold,
            ),
        )
        Spacer(Modifier.width(8.dp))
    }
}
```

### 3.5 Widget: `ProductCard`

**File baru**: `ui/screens/stamps/widgets/ProductCard.kt`

```
┌──────────────────────┐
│                      │
│   [AsyncImage]       │ ← thumbnailUrl, ratio 1:1, fillMaxWidth
│                      │
├──────────────────────┤
│ Judul Produk         │ ← ExtraBold, 13sp, max 2 baris
│ oleh Studio Kembara  │ ← Medium italic, 11sp, BrandGold.copy(0.8f)
│ Rp 35.000            │ ← Bold, 12sp, White
│ ❤️ 128               │ ← Medium, 11sp, White.copy(0.6f)
└──────────────────────┘
```

Surface: `RoundedCornerShape(16.dp)`, `color = ToolbarFrostedDark`, border `BrandGold.copy(0.12f)`.

### 3.6 Widget: `ProductDetailBottomSheet`

**File baru**: `ui/screens/stamps/widgets/ProductDetailBottomSheet.kt`

```kotlin
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = BrandInkDeep,
) {
    if (isLoadingDetail) {
        // Skeleton detail
    } else if (detail != null) {
        LazyColumn {
            // 1. Thumbnail/Banner besar (16:9)
            item { AsyncImage(detail.bannerUrl.ifEmpty { detail.thumbnailUrl }, ratio = 16/9) }

            // 2. Title + Sub-description + Description
            item { DetailHeader(detail) }

            // 3. Asset type badges (Prangko x4 · Stiker x8 dst)
            item { AssetCountBadges(detail) }

            // 4. Preview aset horizontal scroll
            item { AssetPreviewRow(detail.assets) }

            // 5. Creator card
            item { CreatorCard(detail.creator) }

            // 6. Likes row + toggle
            item { LikesRow(detail.likesCount, detail.viewerHasLiked, onToggleLike) }

            // 7. CTA Button
            item {
                when {
                    detail.viewerOwns ->
                        Button("Sudah Dimiliki ✓", enabled = false)
                    else ->
                        Button("Beli — Rp ${detail.priceIdr.toFormattedIdr()}", onClick = onBuyClick)
                }
            }
        }
    }
}
```

### 3.7 Skeleton States

- **Showcase skeleton**: satu Box shimmer ratio 16:9, `RoundedCornerShape(16.dp)`
- **Filter chips skeleton**: 5 pill shimmer
- **Product card skeleton**: 4 card shimmer 1:1 ratio
- **Detail skeleton**: banner shimmer + beberapa text line shimmer

---

## Phase 4 — IAP Integration (RevenueCat)

### Flow Pembelian

```
User tap "Beli"
    ↓
Lookup RevenueCat Product ID:
    "peny_store_{tier}_item_{itemId}"
    (contoh: "peny_store_tier_2_item_e6a1e944-...")
    ↓
Buka PaywallDialog dengan offering yang sesuai
    ↓
Setelah sukses:
    - Refetch GET /api/v1/items/:id
    - Konfirmasi viewer_owns == true
    - Tampilkan dialog "Berhasil Dibuka! 🎉"
    ↓
Setelah gagal/cancel:
    - Tutup dialog, tunjukkan LetterlyTopSnackbar error (jika bukan cancel)
```

### Fungsi helper

```kotlin
fun tierToProductId(tier: String, itemId: String): String =
    "peny_store_${tier}_item_${itemId}"
```

---

## Phase 5 — String Resources

Tambahkan ke `res/values/strings.xml`:

```xml
<!-- Tab Store -->
<string name="stamps_screen_tab_store">Toko</string>

<!-- Filter Chips -->
<string name="store_filter_all">Semua</string>
<string name="store_filter_prangko">Prangko</string>
<string name="store_filter_kertas">Kertas</string>
<string name="store_filter_stiker">Stiker</string>
<string name="store_filter_amplop">Amplop</string>
<string name="store_filter_bundle">Bundle</string>

<!-- Store Labels -->
<string name="store_section_featured">Pilihan Unggulan</string>
<string name="store_by_creator">oleh %1$s</string>
<string name="store_likes_count">%1$d suka</string>
<string name="store_price_idr">Rp %1$s</string>
<string name="store_cta_buy">Beli — Rp %1$s</string>
<string name="store_cta_owned">Sudah Dimiliki ✓</string>
<string name="store_detail_assets_preview">Isi Paket</string>
<string name="store_creator_label">Kreator</string>
<string name="store_view_external_link">Kunjungi Profil</string>
<string name="store_empty_catalog">Belum ada produk tersedia</string>
<string name="store_error_load_failed">Gagal memuat toko. Coba lagi.</string>
<string name="store_purchase_success_title">Selamat! 🎉</string>
<string name="store_purchase_success_body">Paket kini tersedia di koleksimu</string>
```

---

## Ringkasan File yang Dibuat / Diubah

### File Baru
| File | Keterangan |
|---|---|
| `feature/store/data/StoreDto.kt` | DTOs sesuai API contract |
| `feature/store/data/StoreApi.kt` | Retrofit interface |
| `feature/store/data/StoreRepository.kt` | Repository + Result wrapper |
| `ui/screens/stamps/state/StoreUiState.kt` | Domain models + StoreSection |
| `ui/screens/stamps/constants/StoreFilterCategory.kt` | Filter enum |
| `ui/screens/stamps/widgets/PenyStoreSection.kt` | Root composable tab STORE |
| `ui/screens/stamps/widgets/ShowcaseBannerCarousel.kt` | Showcase carousel |
| `ui/screens/stamps/widgets/ProductCard.kt` | Grid item card |
| `ui/screens/stamps/widgets/ProductDetailBottomSheet.kt` | Detail modal bottom sheet |
| `ui/screens/stamps/widgets/StoreSkeletonSection.kt` | Loading skeletons |

### File Diubah
| File | Perubahan |
|---|---|
| `ui/screens/stamps/constants/StampsConstants.kt` | Tambah `STORE` ke `StampsTab` |
| `ui/screens/stamps/state/StampsUiState.kt` | Tambah `store: StoreSection` |
| `ui/screens/stamps/viewmodel/StampsViewModel.kt` | Inject `StoreRepository`, tambah store functions |
| `ui/screens/stamps/screen/StampsScreen.kt` | 3-tab selector, case STORE, LaunchedEffect |
| `app/di/NetworkModule.kt` | Provide `StoreApi` |
| `res/values/strings.xml` | Tambah string resources |

---

## Urutan Pengerjaan

```
Phase 1 → Phase 2 → Phase 3a → Phase 3b → Phase 3c → Phase 4 → Phase 5

Phase 1: Data Layer (StoreDto, StoreApi, StoreRepository)
Phase 2: UiState & ViewModel (StoreSection, StoreUiState, ViewModel functions)
Phase 3a: ProductCard + StoreFilterChips (bisa lihat di UI tanpa server)
Phase 3b: ShowcaseBannerCarousel + PenyStoreSection + Skeleton
Phase 3c: ProductDetailBottomSheet + Like toggle + View increment
Phase 4: IAP Integration (RevenueCat Paywall)
Phase 5: String resources + polish animasi + price formatting
```

---

## Catatan Teknis

1. **Coil** (`io.coil-kt:coil-compose`) — untuk load thumbnail/banner dari URL CDN. Cek apakah sudah ada di `build.gradle.kts`, kalau belum tambahkan.

2. **LazyVerticalGrid** — jangan nest di dalam `LazyColumn`. Jadikan `LazyVerticalGrid` root untuk seluruh tab STORE, dengan header sebagai full-span items.

3. **Pagination** — deteksi akhir list dengan `LazyGridState`:
   ```kotlin
   val gridState = rememberLazyGridState()
   LaunchedEffect(gridState) {
       snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
           .collect { lastIndex ->
               if (lastIndex != null && lastIndex >= catalog.size - 4 && nextCursor != null) {
                   onLoadMore()
               }
           }
   }
   ```

4. **View increment** — delay 1.5s setelah bottom sheet terbuka, fire-and-forget:
   ```kotlin
   LaunchedEffect(itemId) {
       delay(1500)
       onViewItem(itemId)
   }
   ```

5. **Price formatting** — buat extension function:
   ```kotlin
   fun Long.toFormattedIdr(): String =
       NumberFormat.getInstance(Locale("id", "ID")).format(this)
   // Result: 35000 → "35.000"
   ```
