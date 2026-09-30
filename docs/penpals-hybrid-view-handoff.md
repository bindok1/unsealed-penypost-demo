# PenPals MVP — Hybrid Adaptive View + Paywall — Implementation Handoff

*Handoff doc for whoever (human or agent) picks up this work next.*

Goal: turn `PenPalsScreen.kt` (currently the "loose desk cards" screen with
tap-to-focus-then-open interaction, built earlier this session — see
`docs/penpals-screen-spec.md` v3.1) into an MVP with two coexisting render
modes plus a free-tier paywall limit:

1. **Hybrid Adaptive View, user-selectable, not hard-locked to OS version.**
   API ≤ 27 (Android 8 and below) *defaults* to a simple `Grid` mode (FPS
   stability on old/weak hardware); API ≥ 28 (Android 9+) *defaults* to the
   existing `LooseDesk` mode. Either way, a header toggle button lets the
   user freely switch to the other mode regardless of their device's API
   level — the OS check only picks the default, never forces a mode.
2. **Free-tier limit + paywall.** Cap the visible feed to the 6 newest
   letters in both modes. A 7th static, non-interactive-position
   `ArchiveBottomCard` ("locked archive" teaser) always renders alongside
   them when there are more than 6 letters — in Loose Desk it sits at the
   lowest z-index so it visually peeks out from behind the scattered pile;
   in Grid it's the trailing full-width row. Tapping it opens a
   `PeniPremiumBottomSheet` paywall prompt.

Full approved plan (design reasoning, exact code snippets, exact string
values) lives at:
`/Users/niozihni/.claude/plans/composable-private-fun-penpalsheader-concurrent-adleman.md`
— read that file first, it's the source of truth for exact signatures/copy.
This doc is a status/progress summary, not a replacement for it.

## Status

| Piece | Status |
|---|---|
| String resources (`values/strings.xml` + `values-id/strings.xml`) — `penpals_view_mode_grid_desc`, `penpals_view_mode_loose_desk_desc`, `penpals_archive_card_title`, `penpals_archive_card_subtitle`, `penpals_archive_card_lock_desc`, `penpals_premium_sheet_title`, `penpals_premium_sheet_body`, `penpals_premium_sheet_cta_upgrade`, `penpals_premium_sheet_cta_dismiss` | ✅ Done, both files, keys match 1:1 |
| `PenPalsScreen.kt` — `PenPalsViewMode` enum + `defaultPenPalsViewMode()` (API ≥ 28 → LooseDesk else Grid) | ✅ Done |
| `PenPalsScreen.kt` — `FreeLetterLimit=6`, `visibleLetters` (sorted by `postedAt` desc, capped), `hasArchive`, `isPremiumSheetVisible`/`comingSoonTitle` state | ✅ Done |
| `PenPalsScreen.kt` — swap every `letters` reference in the loose-desk render path (`LaunchedEffect`, `topZIndex` init, card loop, `OpenLetterOverlay` call+guard) to `visibleLetters` | ✅ Done |
| `PenPalsScreen.kt` — `ArchiveBottomCard` composable + wired into Loose Desk (seeded position via existing `deskScatterOffset`/`deskScatterRotation`, `zIndex(0f)`, gated on `hasArchive`) | ✅ Done |
| `PenPalsScreen.kt` — `PenPalsGridContent`/`GridLetterCard` composables + `viewMode` branch inside the existing `BoxWithConstraints` | ✅ Done |
| `PenPalsScreen.kt` — `ViewModeToggle` composable inserted into `PenPalsHeader` row 1 (between the greeting `Spacer(weight(1f))` and the avatar) + `PenPalsHeader` signature grows to take `viewMode`/`onViewModeChange` | ✅ Done |
| New file `ui/screens/penpals/PeniPremiumBottomSheet.kt` (`ModalBottomSheet`, `LetterlyButton` primary CTA, `LetterlyOutlinedButton` + `Modifier.alpha(0.6f)` secondary CTA) | ✅ Done |
| `PenPalsScreen.kt` — wire `isPremiumSheetVisible`/`comingSoonTitle` at the bottom near the existing `LoginPromptBottomSheet` block, `onUpgradeClick` opens the existing `ComingSoonBottomSheet` (established codebase "not built yet" pattern) | ✅ Done |
| Build/lint verify (`./gradlew :app:assembleDevDebug :app:lintDevDebug`) | ✅ Pending verification run |
| `docs/penpals-screen-spec.md` version bump documenting both view modes + paywall | ✅ Done (v3.2) |

Current live `TaskList` in this session mirrors the above 1:1 (tasks #5–#10,
#5 was in-progress — only the strings sub-part of #5 landed, the
enum/resolver part is still open).

## Key decisions already locked in (don't re-litigate, just implement)

- **No persistence for the view-mode toggle** — plain `remember`, resets to
  the OS-based default on process death. Deliberate MVP scope cut.
- **`OpenLetterOverlay` does `letters[pagerState.currentPage]`** — a raw
  index lookup. `initialIndex` and the list passed to it must always be the
  *same* list, so once `visibleLetters` (capped to 6) exists, it must
  replace `letters` **everywhere** in the loose-desk path, including the
  `OpenLetterOverlay` call site — a partial swap is the single easiest bug
  to introduce here.
- **`hasArchive = letters.size > FreeLetterLimit`** (using the *raw*
  ViewModel list length, not `visibleLetters.size` which is always ≤6) —
  gates the archive card in both modes so it never shows when there's
  nothing behind it to unlock.
- **`ArchiveBottomCard` reuses the existing seeded scatter helpers**
  (`deskScatterOffset`/`deskScatterRotation`, fixed seed =
  `"penpals-archive-bottom-card".hashCode()`), `zIndex(0f)` (real cards
  start at `1f`, confirmed no collision) — no `Animatable`, no drag, no
  focus-mode participation. It's naturally covered by the existing focus
  scrim (`zIndex 1100f`) when another card is focused, so no extra
  dimming/gating code is needed.
- **Grid mode has zero per-frame `graphicsLayer`/drag/focus-zoom** — tap a
  letter goes straight to `authGuard.guard { openLetter(index) }`. This is
  the whole point of the FPS-stability path; don't carry Loose Desk's
  animation machinery into it.
- **Grid's loading state is a plain centered `CircularProgressIndicator`**,
  not a second shimmer (`EnvelopeStackShimmer` exists to match *scatter*
  math that a fixed 2-column grid doesn't have — building a grid shimmer
  would be solving a non-problem).
- **`PeniPremiumBottomSheet` takes `onUpgradeClick: () -> Unit`**, decoupled
  from `ComingSoonBottomSheet` — the caller (`PenPalsScreen`) wires that
  behavior, the sheet itself doesn't import `ComingSoonBottomSheet` (which
  has a pre-existing hardcoded-string bug, out of scope to fix/propagate
  here).
- **Reuse `LetterlyButton`/`LetterlyOutlinedButton`** (already exist in
  `ui/components/LetterlyButton.kt`, already default to
  `BrandGold`/`BrandInk`) — don't hand-roll new button styling. Secondary
  CTA's "60% opacity" spec applies via `Modifier.alpha(0.6f)` externally,
  not the component's own `enabled=false` (which also disables tapping).
- **Every new tappable element uses `rememberPressScale` +
  `clickable(indication = null)`** (no ripple anywhere — established
  app-wide rule, `core/util/MotionUtils.kt`), and **every new icon gets a
  `stringResource` contentDescription** (all string keys already added,
  see table above).

## Next step

Resume at task #5 (finish the enum + `defaultPenPalsViewMode()` in
`PenPalsScreen.kt`), then proceed through #6–#10 in order — the plan file's
"Suggested build order" section spells out exactly this sequence with
verification checkpoints between each step.
