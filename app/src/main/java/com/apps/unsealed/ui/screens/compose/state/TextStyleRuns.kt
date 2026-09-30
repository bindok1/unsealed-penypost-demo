package com.apps.unsealed.ui.screens.compose.state

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Indent width added per level — roughly one Tab's worth of space. */
private const val IndentStepSp = 24f

/** Indent cycles back to 0 after this many presses instead of growing forever
 * — there's only an "increase" button in [FormattingBar], no separate outdent. */
const val MaxIndentLevel = 4

/**
 * One contiguous slice of [ComposeUiState.bodyText] carrying its own
 * bold/italic/size/indent overrides. A [ComposeUiState.bodyStyleRuns] list
 * always fully partitions `[0, bodyText.length)` — no gaps, no overlaps (see
 * [merged]/[applyOverride]) — so every character has exactly one owning run.
 * That's what lets FormattingBar's Bold/Italic/size/indent controls restyle
 * only the exact range the user selected instead of the whole letter, which
 * was the original bug report (changing size while a selection was active
 * used to change every other character too).
 */
@JsonClass(generateAdapter = true)
data class StyleRun(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    @field:Json(name = "font_size") val fontSize: Float,
    @field:Json(name = "indent_level") val indentLevel: Int = 0,
)

/** A single default-styled run spanning the whole text — the starting point
 * the very first character typed into an empty letter gets. */
fun defaultStyleRuns(textLength: Int, fontSize: Float): List<StyleRun> =
    if (textLength <= 0) emptyList() else listOf(StyleRun(0, textLength, fontSize = fontSize))

/** The run that owns [index], or `null` if [index] falls outside every run
 * (e.g. an empty letter). */
fun List<StyleRun>.runAt(index: Int): StyleRun? = firstOrNull { index >= it.start && index < it.end }

/** Merges neighboring runs left with identical styling after an
 * [applyOverride]/[afterTextChange] call, so the list doesn't grow without
 * bound over a long editing session. */
private fun List<StyleRun>.merged(): List<StyleRun> {
    if (isEmpty()) return this
    val out = mutableListOf(this[0])
    for (i in 1 until size) {
        val prev = out.last()
        val cur = this[i]
        if (prev.end == cur.start &&
            prev.bold == cur.bold &&
            prev.italic == cur.italic &&
            prev.fontSize == cur.fontSize &&
            prev.indentLevel == cur.indentLevel
        ) {
            out[out.lastIndex] = prev.copy(end = cur.end)
        } else {
            out += cur
        }
    }
    return out
}

/**
 * Splits every run overlapping `[start, end)` and applies [transform] to just
 * the overlapping slice, leaving everything outside the range untouched. This
 * is the one primitive FormattingBar's Bold/Italic/size/indent buttons all go
 * through — restyling a range never touches runs outside it.
 */
fun List<StyleRun>.applyOverride(start: Int, end: Int, transform: (StyleRun) -> StyleRun): List<StyleRun> {
    if (start >= end) return this
    val out = mutableListOf<StyleRun>()
    for (run in this) {
        val overlapStart = maxOf(run.start, start)
        val overlapEnd = minOf(run.end, end)
        if (overlapStart >= overlapEnd) {
            out += run
            continue
        }
        if (run.start < overlapStart) out += run.copy(end = overlapStart)
        out += transform(run.copy(start = overlapStart, end = overlapEnd))
        if (overlapEnd < run.end) out += run.copy(start = overlapEnd)
    }
    return out.merged()
}

/**
 * Keeps [StyleRun] offsets in sync after [oldText] becomes [newText] — trims
 * deleted ranges, shifts everything past an insertion/deletion point, and
 * gives freshly typed characters whatever [insertStyle] returns (the
 * "pending" bold/italic/size the user toggled with the cursor collapsed, or
 * the style already sitting at that position by default). Diffs via a
 * common-prefix/suffix comparison since BasicTextField only reports the net
 * result of an edit (typing, paste, backspace, autocomplete, selection
 * replace...), never the individual keystrokes.
 */
fun List<StyleRun>.afterTextChange(
    oldText: String,
    newText: String,
    insertStyle: (previousRun: StyleRun?) -> StyleRun,
): List<StyleRun> {
    if (oldText == newText) return this

    var prefix = 0
    val maxPrefix = minOf(oldText.length, newText.length)
    while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
    var suffix = 0
    val maxSuffix = maxPrefix - prefix
    while (suffix < maxSuffix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++

    val deletedStart = prefix
    val deletedEnd = oldText.length - suffix
    val insertedStart = prefix
    val insertedEnd = newText.length - suffix
    val insertedLength = insertedEnd - insertedStart
    val deltaAfterDeletion = insertedLength - (deletedEnd - deletedStart)

    if (isEmpty()) {
        return if (newText.isEmpty()) {
            emptyList()
        } else {
            listOf(insertStyle(null).copy(start = 0, end = newText.length))
        }
    }

    val beforeInsertion = mutableListOf<StyleRun>()
    var styleBeforeCursor: StyleRun? = null
    for (run in this) {
        when {
            run.end <= deletedStart -> {
                beforeInsertion += run
                styleBeforeCursor = run
            }
            run.start >= deletedEnd -> {
                beforeInsertion += run.copy(start = run.start + deltaAfterDeletion, end = run.end + deltaAfterDeletion)
            }
            else -> {
                // Run partially or fully covers the deleted range — keep only
                // the slice(s) outside [deletedStart, deletedEnd), shifting
                // whatever remains after the edit point.
                if (run.start < deletedStart) {
                    beforeInsertion += run.copy(end = deletedStart)
                    styleBeforeCursor = run
                }
                if (run.end > deletedEnd) {
                    beforeInsertion += run.copy(
                        start = deletedEnd + deltaAfterDeletion,
                        end = run.end + deltaAfterDeletion,
                    )
                }
            }
        }
    }

    if (insertedLength <= 0) return beforeInsertion.merged()

    val insertedRun = insertStyle(styleBeforeCursor).copy(start = insertedStart, end = insertedStart + insertedLength)
    val result = mutableListOf<StyleRun>()
    var inserted = false
    for (run in beforeInsertion) {
        if (!inserted && run.start >= insertedStart) {
            result += insertedRun
            inserted = true
        }
        result += run
    }
    if (!inserted) result += insertedRun
    return result.merged()
}

/**
 * Renders [text] + [runs] into the [AnnotatedString] BasicTextField actually
 * displays/edits — the only place [StyleRun]s turn into real Compose
 * [SpanStyle]/[ParagraphStyle]s. Indent is emitted once per whole paragraph
 * (never per-run) since a [ParagraphStyle] range that only partly covers a
 * visual line renders as two separate lines instead of an indented one — see
 * ui-text's `AnnotatedString.Builder.addStyle(ParagraphStyle, ...)` docs on
 * nested/split paragraphs.
 */
fun buildStyledBody(text: String, runs: List<StyleRun>): AnnotatedString {
    return AnnotatedString.Builder(text).apply {
        for (run in runs) {
            if (run.start >= run.end) continue
            addStyle(
                SpanStyle(
                    fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                    fontSize = run.fontSize.sp,
                ),
                run.start,
                run.end,
            )
        }
        var paragraphStart = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                addParagraphIndent(paragraphStart, i + 1, runs)
                paragraphStart = i + 1
            }
        }
        addParagraphIndent(paragraphStart, text.length, runs)
    }.toAnnotatedString()
}

private fun AnnotatedString.Builder.addParagraphIndent(start: Int, end: Int, runs: List<StyleRun>) {
    if (start > end) return
    val level = runs.runAt(start)?.indentLevel ?: runs.runAt((start - 1).coerceAtLeast(0))?.indentLevel ?: 0
    if (level <= 0) return
    val indentSp = (IndentStepSp * level).sp
    addStyle(ParagraphStyle(textIndent = TextIndent(firstLine = indentSp, restLine = indentSp)), start, end)
}

/** Start (inclusive) / end (exclusive, includes the trailing `\n` if any — see
 * [buildStyledBody]'s matching walk) of the paragraph containing [index]. */
fun paragraphRangeContaining(text: String, index: Int): TextRange {
    val safeIndex = index.coerceIn(0, text.length)
    var start = 0
    for (i in text.indices) {
        if (text[i] == '\n') {
            val end = i + 1
            if (safeIndex < end) return TextRange(start, end)
            start = end
        }
    }
    return TextRange(start, text.length)
}

/** Every paragraph [selection] touches — the indent button always acts on
 * whole paragraphs so it never fragments a line (see [buildStyledBody]). */
fun paragraphRangesTouching(text: String, selection: TextRange): List<TextRange> {
    val lo = selection.min
    val hi = maxOf(selection.max, lo)
    val result = mutableListOf<TextRange>()
    var cursor = lo
    while (true) {
        val paragraph = paragraphRangeContaining(text, cursor)
        result += paragraph
        if (paragraph.end >= hi || paragraph.end >= text.length) break
        cursor = paragraph.end
    }
    return result
}
