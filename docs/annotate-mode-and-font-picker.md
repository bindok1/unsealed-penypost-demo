
# Font Picker & Annotate Mode — Implementation Reference

*Handoff doc for whoever (human or agent) picks up work on the Compose screen next.*

This documents two features built on top of `docs/compose-screen-spec.md`'s P0 scaffold: the **Font Picker bottom sheet** (spec §3) and the **Annotate Mode drawing layer** (spec §5), including a custom ink color picker that goes beyond what the original spec described. Read `docs/compose-screen-spec.md` first for the overall screen layout and `docs/motion-rules.md` for the animation vocabulary (`LetterlySpring`, `reducedMotionSpring`) referenced throughout.

---

## Status

| Piece | Status | Notes |
|---|---|---|
| Font Picker sheet | ✅ Done | Real picker UI. As of `docs/font-completion-bold-italic-google-fonts.md`, all 6 `LetterFont` entries have real font assets (the "Known limitations" note below is superseded — kept for history). |
| Annotate Mode canvas | ✅ Done | Pen/Fine/Wave/Deco/Eraser all work, strokes persist after leaving the mode. |
| Annotate ink color picker | ✅ Done | 3 modes (Spektrum/Grid/Penggeser) + opacity + hex copy — goes beyond the original spec's simple preset-swatch idea. |
| Undo/Redo (top toolbar) | 🔜 Not done | No cross-mode history stack exists. Buttons stay disabled. |
| Draft auto-save / Room | 🔜 Not done | Declined for this pass — no persistence layer exists anywhere in the project yet. |
| Sticker Picker, Paper/Template Picker, real Overflow Menu | 🔜 Not done | Untouched by this work. |

---

## Files

| File | Role |
|---|---|
| `ui/screens/compose/FontPickerSheet.kt` | Font Picker bottom sheet: 6-card grid + size slider. |
| `ui/screens/compose/AnnotateCanvas.kt` | The drawing layer itself — renders strokes, handles pointer input per tool. |
| `ui/screens/compose/AnnotateToolbar.kt` | Bottom toolbar shown in Annotate Mode: 5 tool buttons + color trigger. |
| `ui/screens/compose/AnnotateColorPicker.kt` | The ink color picker popup (trigger button, Spektrum/Grid/Penggeser modes, opacity, hex copy). |
| `ui/screens/compose/ComposeUiState.kt` | `LetterFont` enum, `ComposeFontSizeMin/Max`, `AnnotateTool`/`AnnotatePath`/`AnnotateState` shapes. |
| `ui/screens/compose/ComposeViewModel.kt` | Event methods for both features (see below). |
| `ui/screens/compose/LetterCanvas.kt` | Wires `selectedFont`/`fontSize` into the actual `BasicTextField`, hosts `AnnotateCanvas` as the top layer. |
| `ui/screens/compose/ComposeScreen.kt` | Wires `FontPickerSheet`/`AnnotateToolbar` into the screen; toggles between `FormattingBar` and `AnnotateToolbar` based on `isAnnotateMode`. |
| `ui/screens/compose/FormattingBar.kt` | Its `Aa` icon opens the same Font Picker sheet as the top toolbar's `Aa`. |
| `ui/theme/Color.kt` | No annotate-ink enum anymore — colors are freeform via the picker, not presets (see history: an earlier `AnnotateInk` enum was added then removed once the picker went freeform). |
| `core/util/LetterlySpring.kt`, `core/util/MotionUtils.kt` | Reused as-is; see [Motion compliance](#motion-compliance). |

---

## Font Picker

**Entry points:** the `Aa` icon in the top `ComposeToolbar` and the `Aa` icon in `FormattingBar` (visible when the keyboard is up) both call `viewModel.onFontPickerOpenChange(true)`.

**UI (`FontPickerSheet.kt`):**
```kotlin
@Composable
fun FontPickerSheet(
    selectedFont: LetterFont,
    fontSize: Float,
    onFontSelect: (LetterFont) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- Stock `ModalBottomSheet` (default drag-to-dismiss), title "Pilih Gaya Tulisan".
- 6 `LetterFont` entries in a 2×3 grid, each card previewing "Halo," in that font. Stagger-in on open (40ms/card, motion-rules §3.7), tap-to-select with a scale pulse + animated border.
- A `Slider` below the grid (range `ComposeFontSizeMin..ComposeFontSizeMax`, i.e. 12–24sp) live-resizes the letter body text.

**State:** `ComposeUiState.selectedFont: LetterFont` (default `CAVEAT`), `ComposeUiState.fontSize: Float` (default `15f`), `ComposeUiState.isFontPickerOpen: Boolean`.

**ViewModel:** `onFontSelect(font: LetterFont)`, `onFontSizeChange(size: Float)` (coerces into `ComposeFontSizeMin..ComposeFontSizeMax`), `onFontPickerOpenChange(open: Boolean)`.

**Wired into the canvas:** `LetterCanvas.kt`'s `BasicTextField` uses `LetterBodyStyle.copy(fontFamily = uiState.selectedFont.fontFamily, fontSize = uiState.fontSize.sp, ...)` — previously this was hardcoded to the static `LetterBodyStyle` defaults.

### Known limitations

- ~~Only `LetterFont.CAVEAT` has a real font file...~~ **Superseded** — all 6 entries now have real font assets, plus Bold/Italic toggles and a downloadable Google Fonts extension. See `docs/font-completion-bold-italic-google-fonts.md`.

---

## Annotate Mode

**Entry point:** the pencil icon (`✏`) in `ComposeToolbar` calls `viewModel.onToggleAnnotateMode()`. When active, it shows a `LemonYellow` pill (already existed before this work) and swaps `FormattingBar` for `AnnotateToolbar` at the bottom of `ComposeScreen.kt`.

**Layer stack (`LetterCanvas.kt`):** `AnnotateCanvas` is the last child of the paper `Box`, rendered above the `BasicTextField` — matching spec §2's Layer 4. It **always renders existing strokes** regardless of mode (so annotations stay visible after leaving Annotate Mode); only the pointer-input gesture detector is gated by `isAnnotateMode`. When off, the `Canvas` has no pointer modifier attached, so touches fall through to the text field beneath it.

### Tools (`AnnotateCanvas.kt`)

| Tool | Behavior | Rendering |
|---|---|---|
| `PEN` | Freehand drag, smooth stroke | Quadratic-through-midpoints path, `Stroke(width=6f, cap/join=Round)` |
| `FINE` | Same as Pen, thinner | Same rendering, `strokeWidth=3f` |
| `WAVE` | Freehand drag, procedurally turned into a sine wave | Raw points resampled at fixed arc-length steps, perturbed perpendicular to the local tangent by `amplitude·sin(2π·distance/wavelength)` — a stand-in for the spec's "wave preset," since no such asset/infra exists |
| `DECO` | Tap-to-stamp | Procedural filled heart `Path` (two `cubicTo` lobes) — substitutes the spec's asset-based stamp grid, since no sticker/stamp PNGs exist in the project |
| `ERASER` | Drag-to-erase, real-time | Distance-to-segment hit-test against every path, radius = `eraserRadiusPx + path.strokeWidth/2`; never produces a committed path |

Per-tool stroke width is a preset set by `ComposeViewModel.presetStrokeWidth(tool)` (6f/3f/5f/40f/6f for Pen/Fine/Wave/Deco/Eraser). **Non-obvious:** for `DECO`, `AnnotatePath.strokeWidth` is repurposed as the stamp's render size, not an actual stroke width — see the comment at the point of use in `AnnotateCanvas.kt`.

### Ink color picker (`AnnotateColorPicker.kt`)

This is the one place that went beyond compose-screen-spec.md §5's original description (which only called for 5 fixed preset swatches + a color wheel). Instead:

- **Trigger:** a single circular button in `AnnotateToolbar` showing the current `annotateState.inkColor`. Tapping it opens a `Popup` anchored above the button (via `Alignment.BottomCenter` + a negative Y offset).
- **3 modes**, switchable via a segmented toggle at the top of the popup:
  - **Spektrum** — a hue (x) × saturation/value (y) gradient box. Drag or tap anywhere to pick any color. Implemented via 3 stacked `Brush` gradients (a 12-stop hue sweep, then a white-fade overlay on the top half, then a black-fade overlay on the bottom half) rather than a per-pixel bitmap — this is mathematically exact for HSV since `HSV(h,s,1) == lerp(white, HSV(h,1,1), s)` and `HSV(h,1,v) == lerp(black, HSV(h,1,1), v)`.
  - **Grid** — a grayscale ramp row + 8 rows × 12 hue columns of discrete swatches, sampling the *same* hue/lightness formula as Spektrum (`colorForHueAndLightness(hue, yFraction)`), just quantized into tappable chips.
  - **Penggeser** — R/G/B sliders (0–255 each) with a **typable numeric field** next to each (tap it, clear it, type an exact value — it live-updates the slider/color as you type), plus an `#RRGGBB` hex readout with a tap-to-copy button (copies via `LocalClipboardManager`, flashes a checkmark for 1.2s).
- **Opacity slider** — shared across all 3 modes, adjusts alpha independently of hue/lightness (picking a new color keeps the current opacity; adjusting opacity keeps the current hue).
- All of the above call back through a single `onColorSelect: (Color) -> Unit` (live, on every drag/tap/keystroke — not just on dismiss).

**State:** `AnnotateState` (in `ComposeUiState.kt`) — `selectedTool: AnnotateTool`, `inkColor: Color`, `strokeWidth: Float`, `paths: List<AnnotatePath>`.

**ViewModel:** `onAnnotateToolSelect(tool)`, `onAnnotateColorSelect(color)`, `onAnnotatePathCommit(path)` (appends to `paths`), `onAnnotateErase(point, radiusPx)` (filters `paths`).

### Known limitations

- **No undo/redo for strokes.** The top toolbar's Undo/Redo buttons stay disabled — there's no history stack (per-mode or shared) anywhere in `ComposeViewModel`. Erasing is the only way to remove a stroke.
- **Deco is a single procedural heart**, not the spec's asset-based stamp variety (heart/star/flower) — no sticker/stamp PNGs exist in the project. (Unrelated: user-inserted photos via the overflow menu's "Sisipkan Gambar" are a separate, now fully editable layer — drag/pinch-resize/rotate/replace/delete, see `docs/image-edit-mode-handoff.md` — not part of Annotate Mode.)
- **Wave is a procedural sine perturbation** of the user's own drag, not a designed "wave preset" — there's no such asset/infra to draw on.

---

## Motion compliance

Everything in this feature set follows `docs/motion-rules.md`:
- Every tappable element (font cards, annotate tool buttons, the color trigger, mode-toggle tabs, grid swatches, the hex-copy row) uses scale-only press feedback (Snappy down / Bouncy back via a shared `rememberPressScale()` helper in `AnnotateColorPicker.kt`) — **no ripple** anywhere (§3.1/§4).
- The R/G/B and Opacity `Slider`s use a custom `thumb` (via Compose Material3's `@ExperimentalMaterial3Api` `thumb =`/`interactionSource =` overload) instead of Material3's default thumb, specifically to strip out its built-in ripple/state-layer.
- The color-picker popup animates in (`Gentle`, ~380ms) and out (`Stiff`, ~250ms — faster than enter), scaling/fading from the trigger button, instead of snapping instantly (§3.4).
- All of the above branch through `reducedMotionSpring`/`rememberIsReducedMotion()` (§5).
- **Deliberately not retrofitted:** the pre-existing `ComposeToolbar`/`FormattingBar` buttons still use default `IconButton` ripple — that's a pre-existing P0 deviation from spec, out of scope for this pass (only *new* surface area was held to the motion-rules bar).

---

## Verification

- `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (no standalone JDK on this machine), then `./gradlew :app:compileDebugKotlin` / `:app:assembleDebug`.
- Manual, on the `Pixel_9a` AVD: navigate to the Write tab → tap `Aa` for the Font Picker, tap `✏` for Annotate Mode → try each tool, tap the color circle to open the picker, switch between Spektrum/Grid/Penggeser, adjust opacity, type an exact RGB value, copy the hex.
