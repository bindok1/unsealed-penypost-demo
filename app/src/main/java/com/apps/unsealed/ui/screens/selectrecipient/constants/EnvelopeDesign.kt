package com.apps.unsealed.ui.screens.selectrecipient.constants

import androidx.annotation.DrawableRes
import com.apps.unsealed.R

/** The 5 envelope backgrounds in `res/drawable-widecg/`, 1720x900px landscape. */
enum class EnvelopeDesign(@DrawableRes val drawableRes: Int) {
    ENVELOPE_1(R.drawable.envelope_1),
    ENVELOPE_2(R.drawable.envelope_2),
    ENVELOPE_3(R.drawable.envelope_3),
    ENVELOPE_4(R.drawable.envelope_4),
    ENVELOPE_5(R.drawable.envelope_5);

    companion object {
        /** Maps the backend's `envelope` string (e.g. `"ENVELOPE_2"`) to the enum.
         *  Falls back to [ENVELOPE_1] for unknown values. */
        fun fromApiString(value: String): EnvelopeDesign =
            entries.firstOrNull { it.name == value } ?: ENVELOPE_1
    }
}

/** Real asset ratio (1720x900px) — used to size envelope thumbnails/cards so
 * they never look stretched or letterboxed. */
val EnvelopeAspectRatio = 1720f / 900f
