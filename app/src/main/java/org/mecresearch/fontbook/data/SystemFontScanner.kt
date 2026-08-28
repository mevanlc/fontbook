package org.mecresearch.fontbook.data

import org.mecresearch.fontbook.model.FontItem
import java.io.File
import java.util.Locale

/**
 * Discovers fonts that ship with the OS by enumerating the standard font
 * directories. No permissions are required: these locations are world-readable.
 */
object SystemFontScanner {

    private val FONT_DIRS = listOf(
        "/system/fonts",
        "/product/fonts",
        "/system/font",
        "/system_ext/fonts",
    )

    private val EXTENSIONS = setOf("ttf", "otf", "ttc", "otc")

    fun scan(): List<FontItem> {
        val items = ArrayList<FontItem>()
        val seenNames = HashSet<String>()

        for (path in FONT_DIRS) {
            val dir = File(path)
            if (!dir.isDirectory) continue
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (!file.isFile) continue
                val ext = file.extension.lowercase(Locale.ROOT)
                if (ext !in EXTENSIONS) continue
                if (!seenNames.add(file.name)) continue // de-dupe across mirror dirs

                val names = OpenTypeNames.read(file)
                val family = names?.family?.takeIf { it.isNotBlank() }
                    ?: prettify(file.nameWithoutExtension)
                val style = names?.subfamily?.takeIf { it.isNotBlank() } ?: "Regular"
                val display = names?.full?.takeIf { it.isNotBlank() }
                    ?: buildString {
                        append(family)
                        if (!style.equals("Regular", ignoreCase = true)) {
                            append(' ')
                            append(style)
                        }
                    }

                items += FontItem(
                    file = file,
                    displayName = display,
                    family = family,
                    style = style,
                    supportedScriptTags = OpenTypeScriptTags.read(file),
                )
            }
        }

        // Alphabetical by the name shown in the list, case-insensitive.
        return items.sortedWith(
            compareBy(
                { it.displayName.lowercase(Locale.ROOT) },
                { it.file.name.lowercase(Locale.ROOT) },
            ),
        )
    }

    /** Turns a file stem like "NotoSans-BoldItalic" into "Noto Sans Bold Italic". */
    private fun prettify(stem: String): String {
        val withSpaces = stem
            .replace('_', ' ')
            .replace('-', ' ')
            // insert a space between a lowercase/digit and an uppercase letter
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        return withSpaces.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}
