package org.mecresearch.fontbook.model

import java.util.Locale

/** A set of discovered font faces that share the same OpenType family name. */
data class FontFamilyGroup(
    val key: String,
    val displayName: String,
    val variants: List<FontItem>,
) {
    val representative: FontItem = variants.firstOrNull { it.style.isRegularStyle() }
        ?: variants.first()

    val styleCount: Int = variants
        .map { it.style.normalizedStyle() }
        .distinct()
        .size

    val variantSummary: String = if (styleCount == variants.size) {
        "${variants.size} ${if (variants.size == 1) "style" else "styles"}"
    } else {
        "${variants.size} fonts · $styleCount ${if (styleCount == 1) "style" else "styles"}"
    }

    fun labelFor(font: FontItem): String {
        val duplicateStyleCount = variants.count {
            it.style.normalizedStyle() == font.style.normalizedStyle()
        }
        return if (duplicateStyleCount > 1) {
            "${font.style} · ${font.file.name}"
        } else {
            font.style
        }
    }
}

/** Groups fonts by a whitespace- and case-normalized family name. */
fun List<FontItem>.groupByFamily(): List<FontFamilyGroup> =
    groupBy { it.family.familyKey() }
        .map { (key, variants) ->
            FontFamilyGroup(
                key = key,
                displayName = variants.first().family.normalizedWhitespace(),
                variants = variants.sortedWith(
                    compareBy(
                        { it.style.styleWeight() },
                        { it.style.isItalicStyle() },
                        { it.style.lowercase(Locale.ROOT) },
                        { it.file.name.lowercase(Locale.ROOT) },
                    ),
                ),
            )
        }
        .sortedWith(compareBy({ it.key }, { it.displayName }))

private val whitespace = Regex("\\s+")
private val regularStyleNames = setOf("regular", "normal", "roman", "book", "plain")

private fun String.normalizedWhitespace(): String = trim().replace(whitespace, " ")

private fun String.familyKey(): String = normalizedWhitespace().lowercase(Locale.ROOT)

private fun String.normalizedStyle(): String =
    normalizedWhitespace()
        .lowercase(Locale.ROOT)
        .replace("-", " ")
        .replace(whitespace, " ")

private fun String.isRegularStyle(): Boolean =
    normalizedStyle() in regularStyleNames

private fun String.isItalicStyle(): Boolean {
    val style = normalizedStyle()
    return "italic" in style || "oblique" in style || "slanted" in style
}

private fun String.styleWeight(): Int {
    val compact = normalizedStyle().replace(" ", "")
    return when {
        "thin" in compact || "hairline" in compact -> 100
        "extralight" in compact || "ultralight" in compact -> 200
        "semilight" in compact || "demilight" in compact -> 350
        "light" in compact -> 300
        isRegularStyle() ||
            compact == "italic" ||
            compact == "oblique" ||
            regularStyleNames.any { compact.startsWith(it) } -> 400
        "medium" in compact -> 500
        "semibold" in compact || "demibold" in compact -> 600
        "extrabold" in compact || "ultrabold" in compact -> 800
        "bold" in compact -> 700
        "black" in compact || "heavy" in compact -> 900
        else -> 1_000
    }
}
