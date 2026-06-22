package com.fontbook.app.model

data class ScriptTagInfo(
    val tag: String,
    val langNameEnglish: String?,
    val langName: String?,
    val scriptName: String?,
    val scriptNameEnglish: String?,
    val qbfText: String?,
    val isoLang: String?,
    val isoBcp47: String?,
    val alternates: List<ScriptTagInfo> = emptyList(),
) {
    val searchKey: String = buildString {
        append(tag)
        append(' ')
        append(langNameEnglish.orEmpty())
        append(' ')
        append(langName.orEmpty())
        append(' ')
        append(scriptName.orEmpty())
        append(' ')
        append(scriptNameEnglish.orEmpty())
        append(' ')
        append(isoLang.orEmpty())
        append(' ')
        append(isoBcp47.orEmpty())
        alternates.forEach {
            append(' ')
            append(it.langNameEnglish.orEmpty())
            append(' ')
            append(it.langName.orEmpty())
            append(' ')
            append(it.isoLang.orEmpty())
            append(' ')
            append(it.isoBcp47.orEmpty())
        }
    }.lowercase()

    val displayName: String
        get() = scriptNameEnglish ?: langNameEnglish ?: tag.uppercase()

    val languageLabel: String?
        get() = listOfNotNull(langNameEnglish, langName?.takeIf { it != langNameEnglish })
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")
}
