# Editable Image Layer — Implementation Reference

*Handoff doc for whoever (human or agent) picks up work on the Compose screen next.*

Documents the fix for a bug in the Overflow Menu's "Sisipkan Gambar" flow: it
used to drop a single `AsyncImage`, fixed and centered, with **no pointer
input at all** — so once inserted, the image couldn't be tapped, moved,
resized, or rotated. It's since grown into a small editor: move/resize/rotate,
change/duplicate/reorder/delete via a per-image menu, and a canvas that keeps
images from drifting off it entirely. Read `docs/compose-screen-spec.md` §8
for how this fits the overall screen, and
`docs/annotate-mode-and-font-picker.md` for the unrelated-but-adjacent
Annotate Mode drawing layer.

## Status

| Piece | Status |
|---|---|
| `ComposeUiState.kt` — `ImageInstance` (`cropRect`, `baseWidthPx`/`baseHeightPx` included), `images: List<ImageInstance>`, `selectedImageId`, `croppingImageId`, `canvasWidthPx`/`canvasHeightPx` | ✅ Done |
| `ComposeViewModel.kt` — insert/select/transform/replace/delete/duplicate/reorder/crop/canvas-size events (full list below) | ✅ Done |
| `strings.xml` (`values/` + `values-id/`) — all `image_edit_*`/`image_crop_*` strings | ✅ Done |
| `ui/components/AnchoredDropdownMenu.kt` — shared dark-chrome dropdown, extracted from `OverflowMenuButton.kt` so the per-image menu doesn't duplicate it | ✅ Done |
| `EditableImage.kt` (new file) — `EditableImage`, `ImageActionsMenu`, `ImageCropOverlay`/`CropFrame` (crop implemented but hidden, see below) | ✅ Done |
| `LetterCanvas.kt` — renders `uiState.images`, routes each to `EditableImage` or `ImageCropOverlay`, hosts the tap-elsewhere-to-deselect scrim and canvas-size reporting | ✅ Done |
| `ComposeToolbar.kt` — pill made `weight(1f, fill=false)` + horizontally scrollable so `SendButton` never gets clipped on narrow screens | ✅ Done |
| `ComposeScreen.kt` — two `PickVisualMedia` launchers (insert / replace), full callback wiring | ✅ Done |

## What the user can do now

- Insert an image (Overflow Menu → "Sisipkan Gambar") → it lands already
  selected, with a dashed border (edit-mode indicator) and a "···" trigger
  above it.
- **Move**: drag it anywhere on the paper — clamped so its *center* can never
  leave the canvas (it can still hang partway off an edge, but can't be
  dragged away and lost).
- **Resize**: pinch with two fingers.
- **Rotate**: twist with two fingers.
- **Tap elsewhere to confirm placement**: tapping anywhere else on the canvas
  (not the image) deselects it — the dashed border and menu trigger
  disappear, same as typing in the text field or entering Annotate Mode.
- **"···" menu** (per image): Change image, Duplicate, Send to Back, Bring to
  Front, Delete. (Crop exists in code but is hidden from this menu for now —
  see below.)
- **Insert more images freely**: each "Sisipkan Gambar" appends a new
  independent `ImageInstance`, z-ordered on top of existing ones (z-order is
  just list order — see `onImageSendToBack`/`onImageBringToFront`).

### Crop — implemented, deliberately hidden

`ImageCropOverlay`/`CropFrame` in `EditableImage.kt` are fully built (drag any
of 4 corners to live-resize a crop window, Reset/Selesai controls, applied via
a "trim" — not re-encode — of the display), but the menu entry is gated off:

```kotlin
private const val CropFeatureEnabled = false // EditableImage.kt
```

This was a deliberate call after a first pass felt rough — rather than delete
the work, it's flagged off until it gets another design pass. Flip the
constant back to `true` (and see the `if (CropFeatureEnabled) { ... }` guard
around the menu's `DropdownMenuAction`) to bring it back. Nothing else needs
to change — `croppingImageId`, `onImageCropModeToggle`, `onImageCropChange`,
and `baseWidthPx`/`baseHeightPx` tracking all still run unconditionally.

## Key files

| File | Role |
|---|---|
| `ui/screens/compose/ComposeUiState.kt` | `ImageInstance`, `FullCropRect`, and all the `ComposeUiState` fields listed above. |
| `ui/screens/compose/ComposeViewModel.kt` | `onImageInsert`, `onImageSelect`, `onImageTransform` (clamps to canvas), `onImageReplace`, `onImageDelete`, `onImageDuplicate`, `onImageSendToBack`, `onImageBringToFront`, `onImageCropModeToggle`, `onImageCropChange`, `onImageBaseSizeMeasured`, `onCanvasSizeChanged`. |
| `ui/screens/compose/EditableImage.kt` | Everything about rendering/editing one image: `EditableImage`, `ImageActionsMenu`, `ImageCropOverlay`, `CropFrame`, the dashed-border and menu-position math. |
| `ui/screens/compose/LetterCanvas.kt` | Loops `uiState.images`, wires every callback, hosts the deselect-scrim and reports canvas size. |
| `ui/screens/compose/ComposeScreen.kt` | The two `PickVisualMedia` launchers (insert / "ganti gambar" replace) and the deselect-on-text-focus wiring. |
| `ui/components/AnchoredDropdownMenu.kt` | Shared dropdown shell used by both the Overflow Menu and `ImageActionsMenu` — see `docs/component-library.md`. |

## Design decisions (don't re-litigate without reason)

- **Gesture model is pinch-to-resize + two-finger-rotate + drag-to-move via
  `detectTransformGestures`**, not a corner-handle UI — matches
  Instagram/Snapchat-style stickers and is far less code than a handle
  system.
- **Gestures work regardless of selection.** Any image can be dragged/pinched
  at any time — `isSelected` only gates the dashed border and the "···"
  trigger. A plain tap alone produces no `detectTransformGestures` delta, so
  the photo also has a `.clickable` calling `onSelect()` for that case.
- **`pointerInput` and `graphicsLayer` must stay on the same node.** In
  `EditableImage`, the photo's own Box carries both — Compose only hit-tests
  a gesture against the *visually* scaled/rotated area when the detector and
  the transform share one modifier chain. This is also *why* the "···"
  trigger is a sibling of that Box (inside an outer position-only Box), not a
  child of it: putting the trigger inside the transformed node made it
  shrink/rotate along with the photo, which is the bug this structure fixes.
- **The "···" trigger tracks the photo's real visual top edge, not a
  constant offset.** `visualTopEdgePx()` in `EditableImage.kt` computes the
  topmost point of the scaled+rotated bounding box (rotated-rectangle corner
  math) and anchors the trigger a fixed gap above *that* — a naive constant
  offset from the unscaled layout box left a huge gap when the photo was
  shrunk and could overlap it when enlarged.
- **Dragging clamps the image's center to the canvas, not its full bounds.**
  `ComposeViewModel.onImageTransform` clamps `offsetX`/`offsetY` to
  `±canvasWidthPx/2` / `±canvasHeightPx/2` (reported by `LetterCanvas`'s outer
  `Box.onSizeChanged`). The image can still hang partway off an edge — only
  its center is pinned inside — which was judged enough to fix "image
  disappears off the letter with no way to get it back" without also having
  to reason about scale/rotation in the clamp math.
- **Crop's "trim" model**: cropping doesn't re-encode the source photo. The
  display box shrinks to the cropped sub-rect's size, and the full image is
  drawn at its cached base size (`baseWidthPx`/`baseHeightPx`, captured once
  via `onSizeChanged` the first time it renders uncropped) shifted so only
  the kept region shows through a clip. Good enough for a decorative
  letter-writing feature; not pixel-perfect photo-editing.
- **Deselection** happens when the body text field gains focus, Annotate Mode
  is entered, or the user taps anywhere else on the canvas (a scrim in
  `LetterCanvas.kt`, active only while an image is selected and not
  cropping).
- **Still a separate system from stickers.** `StickerInstance` remains an
  unused stub for the not-yet-built Sticker Picker (`docs/todo.md` item #1).
  Worth unifying with `ImageInstance` if/when that gets built.
- **Toolbar pill scrolls instead of clipping Send.** `ComposeToolbar.kt`'s
  icon pill is `Modifier.weight(1f, fill = false)` +
  `Modifier.horizontalScroll(...)` instead of a fixed-intrinsic-width `Row` —
  on narrow devices (verified on a real Android 8.1/API 27 phone at 720px
  wide) six unweighted `IconButton`s plus `SendButton` overflowed the
  available width, and an unweighted `Row` doesn't shrink, it just clips
  `SendButton` down to almost nothing. Trade-off: the "···" overflow icon
  (last in the pill) can scroll out of view by default on very narrow
  screens — reachable by swiping the pill, but not obviously discoverable.
  Worth a visual affordance (fade edge, etc.) if this comes up again.

## Verification

`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`,
then `./gradlew :app:compileDebugKotlin`. This has been manually verified on
a real Android 8.1 (API 27, 720×1520) device, not just the emulator: insert an
image, confirm the dashed border + "···" trigger land right at the photo's
edge, drag it hard past the canvas edge and confirm it stops (center stays
inside), open the "···" menu and confirm it's Change image / Duplicate / Send
to Back / Bring to Front / Delete (no Crop), tap elsewhere and confirm it
deselects, and confirm the top toolbar's Send button stays a full circle
(pill scrolls instead of clipping it).
