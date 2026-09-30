
# Font Completion, Bold/Italic, & Downloadable Google Fonts — Implementation Reference

*Handoff doc for whoever (human or agent) picks up work on the Font Picker next.*

This documents three additions built on top of `docs/annotate-mode-and-font-picker.md`'s Font Picker sheet: **real font assets for all 6 bundled `LetterFont` entries** (previously all 5 non-Caveat entries silently rendered as Caveat), **Bold/Italic toggles** in `FormattingBar`, and a **downloadable Google Fonts** extension ("More Fonts" section) using Android's official Downloadable Fonts mechanism. Read `docs/annotate-mode-and-font-picker.md` first for the Font Picker's original shape, and `docs/motion-rules.md` for the animation vocabulary referenced throughout.

---

## Status

| Piece | Status | Notes |
|---|---|---|
| 6 bundled fonts | ✅ Done | All real `.ttf` assets now, no more silent Caveat fallback. |
| Bold / Italic toggles | ✅ Done | In `FormattingBar`, applies to the whole letter body regardless of which font is selected. |
| Downloadable Google Fonts | ✅ Done | Starter catalog of 4 fonts, tap-to-download via Android's official Downloadable Fonts API. |
| Persisted "downloaded" state | 🔜 Not done | In-memory only for this pass — see [Known limitations](#known-limitations). |

---

## Files

| File | Role |
|---|---|
| `res/font/*.ttf` | 6 real font files: `caveat_regular`, `kalam_regular`/`kalam_bold`, `architects_daughter_regular`, `indie_flower_regular`, `nothing_you_could_do_regular`, `shadows_into_light_regular`. |
| `docs/font-licenses/*.txt` | OFL license text for each bundled font family — OFL requires the license travel with a redistributed font. |
| `ui/theme/Type.kt` | One `FontFamily` val per bundled font. |
| `ui/screens/compose/ComposeUiState.kt` | `LetterFont` enum now points at real families; added `isBold`/`isItalic`, `selectedGoogleFontName`, `googleFontStatuses`. |
| `ui/screens/compose/ComposeViewModel.kt` | `onBoldToggle`/`onItalicToggle`; `onGoogleFontDownloadStart`/`onGoogleFontDownloaded`/`onGoogleFontDownloadFailed`/`onGoogleFontSelect`; `onFontSelect` now also clears `selectedGoogleFontName`. |
| `ui/screens/compose/ToolbarButtons.kt` | New — `PillIconToggleButton`, extracted from `AnnotateToolbar.kt`'s old private `AnnotateToolButton` so `FormattingBar`'s Bold/Italic buttons can reuse the same no-ripple scale+pill feedback. |
| `ui/screens/compose/GoogleFontCatalog.kt` | New — the downloadable-fonts catalog, `GoogleFontProvider`, `FontDownloadStatus`, `downloadGoogleFont()`, `ComposeUiState.activeFontFamily()`. |
| `ui/screens/compose/FontPickerSheet.kt` | Added the "More Fonts" section (`GoogleFontCard`) below the existing 6-card grid. |
| `ui/screens/compose/LetterCanvas.kt` | Uses `uiState.activeFontFamily()` (bundled or downloaded Google font) instead of `uiState.selectedFont.fontFamily` directly; textStyle now also carries `fontWeight`/`fontStyle` from `isBold`/`isItalic`. |
| `ui/screens/compose/FormattingBar.kt` | Two new `PillIconToggleButton`s (Bold/Italic); its 4 pre-existing hardcoded `contentDescription`s were also retrofitted to string resources while the file was open. |
| `res/values/font_certs.xml` | Standard `com_google_android_gms_fonts_certs` array (dev + prod cert hashes) required by the Downloadable Fonts provider — fetched verbatim from Android's official sample, not hand-typed. |
| `AndroidManifest.xml` | Added `INTERNET` permission (required for Play Services to fetch a font). |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Added `androidx.compose.ui:ui-text-google-fonts`. |

---

## The 6 bundled fonts

All sourced from the canonical `google/fonts` GitHub repo (OFL-licensed, standard practice to bundle):

| `LetterFont` entry | Asset(s) | Real Bold face? |
|---|---|---|
| `CAVEAT` | `caveat_regular.ttf` | No (synthesized) |
| `KALAM` | `kalam_regular.ttf` + `kalam_bold.ttf` | **Yes** |
| `ARCHITECTS_DAUGHTER` | `architects_daughter_regular.ttf` | No (synthesized) |
| `INDIE_FLOWER` | `indie_flower_regular.ttf` | No (synthesized) |
| `NOTHING_YOU_COULD_DO` | `nothing_you_could_do_regular.ttf` | No (synthesized) |
| `SHADOWS_INTO_LIGHT` | `shadows_into_light_regular.ttf` | No (synthesized) |

Kalam is the only one with a real Bold face registered in `Type.kt` (`KalamFontFamily`); the rest rely on Compose/Skia's synthetic bold when `FontWeight.Bold` is requested — this is expected platform behavior, not a bug.

---

## Bold / Italic

**State:** `ComposeUiState.isBold: Boolean`, `ComposeUiState.isItalic: Boolean` (both default `false`).

**ViewModel:** `onBoldToggle()` / `onItalicToggle()` — simple flips, same pattern as `onToggleAnnotateMode()`.

**UI:** two `PillIconToggleButton`s in `FormattingBar` (`Icons.Filled.FormatBold` / `FormatItalic`), next to the alignment button. Selected state uses the same `LemonYellow` pill as `AnnotateToolbar`'s tool buttons, via the newly-shared `PillIconToggleButton` (see [ToolbarButtons.kt](#files)).

**Wired into the canvas:** `LetterCanvas.kt`'s `BasicTextField` textStyle (and the "Dear ___," placeholder) now also set `fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal` and `fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal` — independent of which font is selected, so Bold/Italic persist across font switches.

---

## Downloadable Google Fonts

Compose-screen-spec.md's Font Picker only described the 6 bundled fonts; this extends it with a "More Fonts" section for pulling in additional handwriting-style fonts on demand.

**Chosen approach: Android's official Downloadable Fonts API**, not a hand-rolled HTTP/file-cache stack — it's the Google-blessed mechanism for exactly this, needs no new HTTP client dependency or manual storage/eviction, and downloads are cached system-wide by Google Play Services. The trade-off is a Play Services dependency on-device (confirmed present on the `Pixel_9a` dev AVD, so it's actually testable there).

**Catalog (`GoogleFontCatalog.kt`):** `DownloadableFontEntry(googleFontName, label)`, currently 4 entries — Patrick Hand, Dancing Script, Reenie Beanie, Permanent Marker.

**Status enum:** `FontDownloadStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, FAILED }`, tracked per font name in `ComposeUiState.googleFontStatuses: Map<String, FontDownloadStatus>`.

**Download trigger (`downloadGoogleFont(context, fontName): Typeface`):** wraps AndroidX Core's lower-level `FontsContractCompat.requestFont(...)` in `suspendCancellableCoroutine`, resuming on `onTypefaceRetrieved`/`onTypefaceRequestFailed`. This is deliberately *not* the simpler "just drop `Font(GoogleFont(name), provider)` into a `Text` and let it swap in later" path — that path exists too and is what actually renders the font once downloaded (see `googleFontFamily()`), but gives no explicit completion signal, and the UI needs one to drive the spinner → checkmark transition.

**UI (`FontPickerSheet.kt`'s `GoogleFontCard`):**
- `NOT_DOWNLOADED` — dimmed preview + small cloud-download icon, tap starts the download.
- `DOWNLOADING` — small `CircularProgressIndicator` overlay (indeterminate spinners are motion-rules §3.10's one sanctioned continuous-loop exception).
- `DOWNLOADED` — full-opacity preview rendered in the real font (via `googleFontFamily(name)`), same `Icons.Filled.Check` badge as the bundled cards; tap selects it.
- `FAILED` — error-tinted icon + an entrance shake (§3.9 keyframes, using `FastOutSlowInEasing` — `keyframes { using(...) }` takes an `Easing`, not a spring spec, so `LetterlySpring.Stiff` couldn't be used literally here despite the doc's illustrative snippet), tap retries.

**Selection model:** `ComposeUiState.selectedGoogleFontName: String?` — when non-null, it overrides the bundled `selectedFont` for rendering purposes via `ComposeUiState.activeFontFamily()`:
```kotlin
fun ComposeUiState.activeFontFamily(): FontFamily =
    selectedGoogleFontName?.let { googleFontFamily(it) } ?: selectedFont.fontFamily
```
Selecting a bundled `LetterFont` (`onFontSelect`) clears `selectedGoogleFontName`; downloading (`onGoogleFontDownloaded`) or re-selecting (`onGoogleFontSelect`) an already-downloaded Google Font sets it. Once downloaded, GMS resolves the font near-instantly from its on-device cache, so reconstructing the `FontFamily` on demand rather than storing one in state is cheap.

### Known limitations

- **`googleFontStatuses` / `selectedGoogleFontName` are in-memory ViewModel state only** — they reset on process death. GMS's own on-device font cache persists regardless, so a re-tap after restart resolves near-instantly rather than re-downloading over the network; only the picker's "already downloaded" checkmark badge resets to locked. Adding real persistence (e.g. a one-line `SharedPreferences` string set) is a small, easy follow-up if this proves annoying in practice — deliberately deferred here to avoid introducing a new persistence layer beyond what was asked for (this project has no persistence layer anywhere yet).
- **Requires Google Play Services on-device.** No fallback path exists for GMS-less devices/emulators — the "More Fonts" cards would just fail to download there.
- **Small starter catalog (4 entries).** Expanding it is just adding more `DownloadableFontEntry` values; no code changes needed.

---

## Motion compliance

- Bold/Italic buttons and Google Font cards all follow the same scale/press-feedback and no-ripple conventions established in `docs/annotate-mode-and-font-picker.md` — no new deviations introduced.
- The Google Font download-failure shake is the first real use of motion-rules §3.9's keyframe shake outside its own doc snippet; discovered in the process that `keyframes { ... using(spring) }` doesn't compile (`using` takes an `Easing`, not an `AnimationSpec`) — used `FastOutSlowInEasing` instead as the closest sharp/non-bouncy stand-in. Worth fixing the doc's illustrative snippet at some point so it doesn't mislead the next implementer.

---

## Verification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, then `./gradlew :app:compileDebugKotlin` / `:app:assembleDebug`.
- Manual, on the `Pixel_9a` AVD: open the Font Picker — all 6 bundled cards render visually distinct handwriting styles; tap Bold/Italic in `FormattingBar` and confirm the letter body's weight/slant changes and survives switching fonts; in "More Fonts", tap a locked card (requires the AVD's network) and confirm it downloads and becomes selectable, with the letter canvas switching to the real downloaded font.
