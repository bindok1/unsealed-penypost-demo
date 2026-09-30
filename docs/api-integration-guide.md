# How to Integrate a New API Endpoint

A step-by-step checklist for adding a new backend endpoint to Unsealed, keeping the architecture consistent with existing features like `GET /api/v1/penpals/feed` and `GET /api/v1/users/:id`.

---

## Architecture Overview

```
API Interface  →  Repository  →  ViewModel  →  Composable (Screen / BottomSheet)
   (Retrofit)      (suspend)     (StateFlow)     (collectAsState)
```

All network responses are wrapped in `ApiResponse<T>` (standard success envelope).  
All repository results are wrapped in `AuthResult<T>` (typed Success / Error sealed class).

---

## Step-by-Step Checklist

### 1. Add the DTO(s) in the feature's `data/` package

**File:** `feature/<module>/data/<Module>Dto.kt`

- Use `@JsonClass(generateAdapter = true)` (Moshi) for new DTOs **not** already using `@Serializable`.
- Use `@Json(name = "snake_case_field")` for every field whose Kotlin name differs from the JSON key.
- Keep DTOs flat; don't reuse `UserDto` for endpoints that intentionally expose fewer fields.

```kotlin
// Example: PublicProfileDto — only public fields, no private/internal ones
@JsonClass(generateAdapter = true)
data class PublicProfileDto(
    val id: String,
    val nickname: String,
    @Json(name = "is_online") val isOnline: Boolean,
    @Json(name = "last_active_at") val lastActiveAt: String?,
    // ... only what the endpoint documents as public
)
```

> **Rule:** One DTO per endpoint contract. If an endpoint returns fewer fields than another,
> create a separate DTO — don't make fields on a shared DTO nullable to paper over the difference.

---

### 2. Declare the endpoint in the feature's `Api` interface

**File:** `feature/<module>/data/<Module>Api.kt`

- Use `@GET`, `@POST`, `@PATCH`, `@PUT`, `@DELETE` from Retrofit.
- Use `Response<ApiResponse<T>>` when you need to inspect the HTTP status code (e.g., 404).
- Use `ApiResponse<T>` directly when you only care about the parsed body.
- `@Path`, `@Query`, `@Body` arguments follow standard Retrofit conventions.

```kotlin
// Use Response<> wrapper when HTTP status matters (404, 409, etc.)
@GET("api/v1/users/{id}")
suspend fun getPublicProfile(
    @Path("id") userId: String,
): Response<ApiResponse<PublicProfileDto>>

// Use ApiResponse directly when any non-2xx is an error
@GET("api/v1/penpals/feed")
suspend fun getFeed(
    @Query("cursor") cursor: String? = null,
    @Query("limit")  limit: Int? = null,
): ApiResponse<FeedResponse>
```

---

### 3. Add a method to the Repository

**File:** `feature/<module>/data/<Module>Repository.kt`

- Wrap the call in `runCatching { ... }.fold(onSuccess = ..., onFailure = ...)`.
- Return `AuthResult.Success(data)` / `AuthResult.Error(message)`.
- Map known HTTP codes (e.g., 404) to `null` inside `AuthResult.Success` — callers decide how to display it.
- Always record exceptions to Crashlytics before returning `AuthResult.Error`.

```kotlin
suspend fun fetchPublicProfile(userId: String): AuthResult<PublicProfileDto?> =
    runCatching {
        val response = api.getPublicProfile(userId)
        when {
            response.isSuccessful -> response.body()?.data
            response.code() == 404 -> null          // "not found" or "blocked" — same UX
            else -> error("GET /users/$userId failed: HTTP ${response.code()}")
        }
    }.fold(
        onSuccess = { AuthResult.Success(it) },
        onFailure = {
            crashlytics.recordException(it)
            AuthResult.Error(it.toUserFacingMessage(context, R.string.error_generic), it)
        },
    )
```

---

### 4. Create (or extend) the ViewModel

**Choose based on scope:**

| Scenario | Approach |
|---|---|
| Shared global state (e.g., auth user) | Extend existing ViewModel (`AuthViewModel`) |
| Screen-scoped state | New `@HiltViewModel` in `feature/<module>/` or `ui/screens/<screen>/viewmodel/` |
| Ephemeral sheet state (reloaded per open) | New dedicated `@HiltViewModel` for the sheet |

**Pattern for a dedicated ViewModel:**

```kotlin
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        _uiState.value = PublicProfileUiState.Loading   // always reset first
        viewModelScope.launch {
            _uiState.value = when (val result = repository.fetchPublicProfile(userId)) {
                is AuthResult.Success -> if (result.data == null) PublicProfileUiState.NotFound
                                        else PublicProfileUiState.Success(result.data)
                is AuthResult.Error  -> PublicProfileUiState.Error(result.message)
            }
        }
    }
}
```

**UI State sealed class convention:**

```kotlin
sealed class PublicProfileUiState {
    object Loading : PublicProfileUiState()
    data class Success(val profile: PublicProfileDto) : PublicProfileUiState()
    object NotFound : PublicProfileUiState()        // 404 — distinct from Error
    data class Error(val message: String) : PublicProfileUiState()
}
```

---

### 5. Build the Composable (Screen or BottomSheet)

**File:** `ui/screens/<feature>/<FeatureScreen>.kt`

- Collect state with `collectAsState()` or `collectAsStateWithLifecycle()`.
- Use `LaunchedEffect(key)` to trigger loads that depend on dynamic inputs.
- Handle all sealed class branches — don't forget `Loading`, `Error`, and domain-specific states like `NotFound`.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileBottomSheet(
    userId: String,
    onDismiss: () -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when (val state = uiState) {
            is PublicProfileUiState.Loading  -> LoadingContent()
            is PublicProfileUiState.NotFound -> NotFoundContent()
            is PublicProfileUiState.Error    -> ErrorContent(state.message)
            is PublicProfileUiState.Success  -> ProfileContent(state.profile)
        }
    }
}
```

---

### 6. Wire up in the parent Screen / NavHost

**Bottom sheet pattern (state held in parent):**

```kotlin
// In the parent screen composable:
var profileUserId by remember { mutableStateOf<String?>(null) }

// Trigger:
onViewProfileClick = { senderId -> profileUserId = senderId }

// Show sheet:
profileUserId?.let { userId ->
    PublicProfileBottomSheet(
        userId = userId,
        onDismiss = { profileUserId = null },
    )
}
```

**Full-screen navigation pattern (add route to NavHost):**

```kotlin
// 1. Declare route constant in Destinations.kt or UnsealedNavHost.kt
internal const val PublicProfileRoute = "public_profile"
internal const val PublicProfileUserIdArg = "userId"
private const val PublicProfileRoutePattern = "$PublicProfileRoute/{$PublicProfileUserIdArg}"

// 2. Add composable in UnsealedNavHost
composable(
    route = PublicProfileRoutePattern,
    arguments = listOf(navArgument(PublicProfileUserIdArg) { type = NavType.StringType }),
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString(PublicProfileUserIdArg) ?: return@composable
    PublicProfileScreen(userId = userId, onBackClick = { navController.popBackStack() })
}

// 3. Navigate from caller
navController.navigate("$PublicProfileRoute/${Uri.encode(userId)}")
```

---

### 7. Add string resources

**Always add entries to both locale files:**

| File | Purpose |
|---|---|
| `res/values/strings.xml` | English (default) |
| `res/values-id/strings.xml` | Indonesian |

Naming convention: `<feature>_<context>_<label>` — e.g., `public_profile_online_now`, `penpals_premium_sheet_title`.

---

## Key Conventions Cheatsheet

| Concern | Convention |
|---|---|
| JSON ↔ Kotlin field mapping | `@Json(name = "snake_case")` (Moshi) |
| HTTP 404 = "not found or blocked" | Return `null` inside `AuthResult.Success` |
| Network errors | Always `crashlytics.recordException(it)` before returning `AuthResult.Error` |
| Loading state reset | `_uiState.value = Loading` at top of `load()` — prevents flashing stale data |
| Stale data across sheet re-opens | Dedicated ViewModel + `LaunchedEffect(userId)` trigger |
| Relative time rendering | Client-side from raw RFC 3339 (server sends raw timestamp, client formats) |
| `is_online` threshold | Server-computed (5 min), never client-side — use the value from the DTO directly |
| Private/internal field exclusion | Create a purpose-built DTO; never make fields nullable to hide them |
