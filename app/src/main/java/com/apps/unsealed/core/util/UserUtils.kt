package com.apps.unsealed.core.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * Calculates user's age from birthday string (expected format "YYYY-MM-DD").
 * Returns fallback if date cannot be parsed.
 */
fun calculateAge(birthday: String?, fallbackAge: Int = 24): Int {
    if (birthday.isNull_or_empty()) return fallbackAge
    return runCatching {
        val birthDate = LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE)
        val currentDate = LocalDate.now()
        Period.between(birthDate, currentDate).years
    }.getOrDefault(fallbackAge)
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

/**
 * Calculates astrological zodiac symbol and name from birthday string ("YYYY-MM-DD").
 */
fun calculateZodiac(birthday: String?): String {
    if (birthday.isNull_or_empty()) return "♋"
    val date = runCatching {
        LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull() ?: return "♋"

    val month = date.monthValue
    val day = date.dayOfMonth

    return when (month) {
        1 -> if (day <= 19) "♑" else "♒"
        2 -> if (day <= 18) "♒" else "♓"
        3 -> if (day <= 20) "♓" else "♈"
        4 -> if (day <= 19) "♈" else "♉"
        5 -> if (day <= 20) "♉" else "♊"
        6 -> if (day <= 20) "♊" else "♋"
        7 -> if (day <= 22) "♋" else "♌"
        8 -> if (day <= 22) "♌" else "♍"
        9 -> if (day <= 22) "♍" else "♎"
        10 -> if (day <= 22) "♎" else "♏"
        11 -> if (day <= 21) "♏" else "♐"
        12 -> if (day <= 21) "♐" else "♑"
        else -> "♋"
    }
}

/**
 * Formats raw gender string ("m"/"f"/"male"/"female") to display label.
 */
fun formatGender(gender: String?): String {
    return when (gender?.lowercase()?.trim()) {
        "m", "male" -> "Male"
        "f", "female" -> "Female"
        else -> "Other"
    }
}
