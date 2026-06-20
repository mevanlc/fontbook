package com.fontbook.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.fontbook.app.model.FontItem
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

/** Showcase strings used across the app. */
object Samples {
    const val PANGRAM = "The quick brown fox jumps over the lazy dog"
    const val PANGRAM_SHORT = "Sphinx of black quartz, judge my vow"

    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val NUMERALS = "0123456789"
    const val PUNCTUATION = "& . , ; : ! ? \" ' ( ) [ ] { } / \\ @ # $ % * + - = ~"

    const val PARAGRAPH =
        "Typography is the art and technique of arranging type to make written " +
            "language legible, readable, and appealing when displayed."

    /** Characters shown in the glyph-repertoire grid. */
    val GLYPHS: List<String> = buildList {
        ('A'..'Z').forEach { add(it.toString()) }
        ('a'..'z').forEach { add(it.toString()) }
        ('0'..'9').forEach { add(it.toString()) }
        listOf(
            ".", ",", ";", ":", "!", "?", "\"", "'", "(", ")", "[", "]", "{", "}",
            "/", "\\", "&", "@", "#", "$", "%", "*", "+", "-", "=", "<", ">", "~",
        ).forEach { add(it) }
    }

    /** Point sizes (sp) used by the waterfall view. */
    val WATERFALL_SIZES = listOf(10, 12, 14, 16, 18, 20, 24, 28, 36, 48, 60, 72, 96)
}

/** Remembers a Compose [FontFamily] backed by this font's Android typeface. */
@Composable
fun rememberFontFamily(item: FontItem): FontFamily =
    remember(item.file.path) { FontFamily(ComposeTypeface(item.typeface)) }
