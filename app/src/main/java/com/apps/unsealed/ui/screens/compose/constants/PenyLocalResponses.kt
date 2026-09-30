package com.apps.unsealed.ui.screens.compose.constants

import android.content.Context
import com.apps.unsealed.R

/**
 * Lapis 2 (zero-network) responses for Mode Peny — greetings and short,
 * ambiguous invites get an instant reply from this static pool instead of
 * ever reaching `POST /peny/reply` (peny_mode_mobile_integration.md §1). The
 * reveal animation is identical to an AI reply (`PenyReplyOverlay`), so the
 * user can't tell the source apart.
 *
 * Copy lives in strings.xml (`compose_peny_local_greetings`/
 * `compose_peny_local_invites` arrays), not literal Kotlin strings, so
 * translators/copywriters can edit it without touching code
 * (peny-mode-spec.md §9).
 */
object PenyLocalResponses {

    private val GreetingPattern = Regex("^(hai|halo|hello|hi|woy|yo)[\\s!.,]*$", RegexOption.IGNORE_CASE)

    /** Null when [input] matches neither pattern — caller falls through to Lapis 3.
     *
     * [isFirstTurn] gates the single-bare-word "ambiguous invite" branch below —
     * see its doc comment for why. */
    fun matchLocal(context: Context, input: String, isFirstTurn: Boolean): String? {
        val trimmed = input.trim()
        return when {
            GreetingPattern.matches(trimmed) -> greetings(context).random()
            // Single bare word only (e.g. "capek", "sedih", "libur") — the
            // original 1-3 word range also caught genuine short questions
            // like "kamu siapa" or "hallo aku siapa" and gave them a canned
            // "tell me more" reply instead of routing to the AI, which read
            // as broken. 2+ words is treated as meaningful enough on its
            // own now, regardless of punctuation.
            //
            // isFirstTurn also gates this: it only makes sense as an
            // opener-disambiguator. Mid-conversation, a bare one-word message
            // ("iya", "boleh", "nggak") is overwhelmingly a direct answer to
            // whatever Peny just asked, not a vague conversation starter —
            // matching it here fired an unrelated "cerita lebih dong" canned
            // reply instead of letting the AI actually respond to the answer,
            // which read as the app randomly ignoring what the user just said.
            isFirstTurn && trimmed.split(Regex("\\s+")).size == 1 && !looksLikeSentence(trimmed) -> invites(context).random()
            else -> null
        }
    }

    private fun greetings(context: Context): Array<String> =
        context.resources.getStringArray(R.array.compose_peny_local_greetings)

    private fun invites(context: Context): Array<String> =
        context.resources.getStringArray(R.array.compose_peny_local_invites)

    /** Trailing punctuation is treated as a "meaningful enough" signal (e.g.
     * "capek.") even under the word-count threshold — see peny-mode-spec.md
     * §4.2. When true, the caller should fall through to Lapis 3 instead of
     * matching [AmbiguousInvite]-style copy. */
    private fun looksLikeSentence(s: String) = s.endsWith(".") || s.endsWith("?") || s.endsWith("!")
}
