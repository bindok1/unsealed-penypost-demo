package com.apps.unsealed.core.model

data class PenpalLanguage(
    val code: String,
    val flag: String,
    val nativeName: String,
    val englishName: String,
) {
    val displayLabel: String
        get() = if (nativeName.equals(englishName, ignoreCase = true)) {
            "$flag  $nativeName"
        } else {
            "$flag  $nativeName ($englishName)"
        }
}

val AllPenpalLanguages = listOf(
    PenpalLanguage(code = "id", flag = "🇮🇩", nativeName = "Bahasa Indonesia", englishName = "Indonesian"),
    PenpalLanguage(code = "en", flag = "🇬🇧", nativeName = "English", englishName = "English"),
    PenpalLanguage(code = "ja", flag = "🇯🇵", nativeName = "日本語", englishName = "Japanese"),
    PenpalLanguage(code = "ko", flag = "🇰🇷", nativeName = "한국어", englishName = "Korean"),
    PenpalLanguage(code = "es", flag = "🇪🇸", nativeName = "Español", englishName = "Spanish"),
    PenpalLanguage(code = "fr", flag = "🇫🇷", nativeName = "Français", englishName = "French"),
    PenpalLanguage(code = "de", flag = "🇩🇪", nativeName = "Deutsch", englishName = "German"),
    PenpalLanguage(code = "zh", flag = "🇨🇳", nativeName = "中文", englishName = "Chinese"),
    PenpalLanguage(code = "ar", flag = "🇸🇦", nativeName = "العربية", englishName = "Arabic"),
    PenpalLanguage(code = "pt", flag = "🇧🇷", nativeName = "Português", englishName = "Portuguese"),
    PenpalLanguage(code = "ru", flag = "🇷🇺", nativeName = "Русский", englishName = "Russian"),
    PenpalLanguage(code = "it", flag = "🇮🇹", nativeName = "Italiano", englishName = "Italian"),
    PenpalLanguage(code = "tr", flag = "🇹🇷", nativeName = "Türkçe", englishName = "Turkish"),
    PenpalLanguage(code = "th", flag = "🇹🇭", nativeName = "ไทย", englishName = "Thai"),
    PenpalLanguage(code = "vi", flag = "🇻🇳", nativeName = "Tiếng Việt", englishName = "Vietnamese"),
    PenpalLanguage(code = "ms", flag = "🇲🇾", nativeName = "Bahasa Melayu", englishName = "Malay"),
    PenpalLanguage(code = "nl", flag = "🇳🇱", nativeName = "Nederlands", englishName = "Dutch"),
    PenpalLanguage(code = "hi", flag = "🇮🇳", nativeName = "हिन्दी", englishName = "Hindi"),
    PenpalLanguage(code = "tl", flag = "🇵🇭", nativeName = "Tagalog", englishName = "Filipino"),
    PenpalLanguage(code = "pl", flag = "🇵🇱", nativeName = "Polski", englishName = "Polish"),
)

fun getPenpalLanguageByCode(code: String): PenpalLanguage? {
    val cleanCode = code.trim().lowercase()
    val normalized = if (cleanCode == "in") "id" else cleanCode
    return AllPenpalLanguages.find { it.code == normalized }
}
