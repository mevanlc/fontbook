package org.mecresearch.fontbook.model

import android.graphics.Typeface
import java.io.File

/**
 * A single font discovered on the device, ready to be showcased.
 *
 * [displayName] is the human-readable full name pulled from the font's OpenType
 * `name` table when available (e.g. "Roboto Condensed Bold"), falling back to a
 * prettified file name. The Android [typeface] is created lazily so scanning the
 * whole font directory stays cheap.
 */
data class FontItem(
    val file: File,
    val displayName: String,
    val family: String,
    val style: String,
    val supportedScriptTags: List<String>,
) {
    val typeface: Typeface by lazy {
        runCatching { Typeface.createFromFile(file) }.getOrDefault(Typeface.DEFAULT)
    }

    /** Lowercase haystack used for search matching. */
    val searchKey: String = "$displayName $family $style ${file.name} ${supportedScriptTags.joinToString(" ")}"
        .lowercase()
}
