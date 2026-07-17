package com.fontbook.app.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FontFamilyGroupTest {

    @Test
    fun groupsNormalizedFamilyNamesAndSortsVariants() {
        val fonts = listOf(
            font("Roboto-Bold.ttf", "Roboto", "Bold"),
            font("Roboto-Italic.ttf", "Roboto", "Italic"),
            font("Roboto-BoldItalic.ttf", "Roboto", "Bold Italic"),
            font("Roboto-Regular.ttf", "  roboto  ", "Regular"),
        )

        val group = fonts.groupByFamily().single()

        assertEquals("roboto", group.key)
        assertEquals("Roboto", group.displayName)
        assertEquals("Regular", group.representative.style)
        assertEquals(
            listOf("Regular", "Italic", "Bold", "Bold Italic"),
            group.variants.map { it.style },
        )
    }

    @Test
    fun sortsFamiliesAndKeepsSingleFontFamilies() {
        val fonts = listOf(
            font("Zed.ttf", "Zed", "Regular"),
            font("Alpha.ttf", "Alpha", "Regular"),
        )

        val groups = fonts.groupByFamily()

        assertEquals(listOf("Alpha", "Zed"), groups.map { it.displayName })
        assertEquals(listOf(1, 1), groups.map { it.variants.size })
    }

    @Test
    fun disambiguatesRepeatedStyleNamesWithFileNames() {
        val group = listOf(
            font("Roboto-Regular.ttf", "Roboto", "Regular"),
            font("Roboto-System-Regular.ttc", "Roboto", "Regular"),
            font("Roboto-Bold.ttf", "Roboto", "Bold"),
        ).groupByFamily().single()

        assertEquals(2, group.styleCount)
        assertEquals("3 fonts · 2 styles", group.variantSummary)
        assertEquals("Regular · Roboto-Regular.ttf", group.labelFor(group.variants[0]))
        assertEquals("Regular · Roboto-System-Regular.ttc", group.labelFor(group.variants[1]))
        assertEquals("Bold", group.labelFor(group.variants[2]))
    }

    private fun font(fileName: String, family: String, style: String) = FontItem(
        file = File("/fonts/$fileName"),
        displayName = "$family $style",
        family = family,
        style = style,
        supportedScriptTags = emptyList(),
    )
}
