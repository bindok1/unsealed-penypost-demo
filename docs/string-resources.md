# String Resources Rule

*Convention doc — read this before writing any user-facing text in this project.*

---

## The rule

**Never hardcode a user-facing string literal in Kotlin.** Every piece of text a user can see or an accessibility service can read — labels, titles, button text, dialog messages, placeholder text, toast/snackbar copy, `contentDescription`s — goes in `strings.xml`, referenced from code via `stringResource(R.string.xxx)` (Composable) or `context.getString(R.string.xxx)` (non-Composable).

```kotlin
// ❌ Don't
Text("Pilih Gaya Tulisan")
Icon(imageVector = Icons.Filled.Brush, contentDescription = "Pen")

// ✅ Do
Text(stringResource(R.string.font_picker_title))
Icon(imageVector = Icons.Filled.Brush, contentDescription = stringResource(R.string.annotate_tool_pen))
```

**Why:** centralizes copy for editing/review without touching Kotlin, and — the main driver — lets the app ship in more than one language without hunting down literals scattered across composables. Content descriptions count too: they're read aloud by TalkBack, so they're just as "user-facing" as visible text.

**Exception:** proper nouns that don't get translated — font family names (`LetterFont.label`, e.g. "Caveat", "Kalam"), brand names — don't need a resource entry just for translation's sake, though pulling them into resources anyway is fine if they're reused in multiple places.

---

## Locale setup

This project ships two locales:

| File | Locale | Role |
|---|---|---|
| `res/values/strings.xml` | English | **Default** — used as the fallback for any locale without its own override, and the source of truth for English-speaking devices. |
| `res/values-id/strings.xml` | Indonesian | Override — Android automatically picks this set on devices set to Indonesian, falling back to `values/strings.xml` for any key not present here. |

Both files must define the **same set of keys** — every string added to `values/strings.xml` needs a matching entry in `values-id/strings.xml` (and vice versa). A key present in only one file still compiles (Android silently falls back to the default), but it means that locale silently shows the wrong language for that string — treat a missing translation as a bug, not a shortcut.

This mirrors the existing pattern in `navigation/Destinations.kt`, which already does this correctly and predates this doc:
```kotlin
data object Inbox : Destinations("inbox", R.string.nav_inbox, Icons.Filled.Home)
```
consumed via `stringResource(destination.labelRes)` in `BottomNavBar.kt`. Follow that shape for new destinations/screens.

---

## Naming convention

`snake_case`, prefixed by the feature/screen area so keys stay greppable and don't collide:

```
font_picker_title
font_picker_size_label
annotate_tool_pen
annotate_color_mode_spectrum
```

Not `title1`, `label`, `text` — those collide across features and give a translator zero context.

---

## Adding a new string — checklist

1. Add the key + English text to `res/values/strings.xml`.
2. Add the same key + Indonesian text to `res/values-id/strings.xml`.
3. Reference it via `stringResource(R.string.key)` — never inline the literal, not even "temporarily."
4. If the string takes a placeholder (a name, a count), use `%1$s`/`%1$d` format args (`stringResource(R.string.key, value)`) rather than manual string concatenation — this keeps word order translatable (Indonesian and English don't always order clauses the same way).

---

## Where this has been applied

`FontPickerSheet.kt` and the Annotate Mode files (`AnnotateToolbar.kt`, `AnnotateColorPicker.kt`) were retrofitted to this rule as the first pass — see `docs/annotate-mode-and-font-picker.md` for what those features do. Anything touched after this doc was written is expected to follow the rule from the start, not get retrofitted later.
