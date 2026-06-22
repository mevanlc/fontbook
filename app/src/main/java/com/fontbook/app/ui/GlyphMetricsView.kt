package com.fontbook.app.ui

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fontbook.app.data.FontHeadReader
import com.fontbook.app.model.FontItem
import kotlin.math.min
import kotlin.math.roundToInt

// Fixed annotation colors (readable on both light and dark backgrounds).
private val AscentColor = Color(0xFF4C8DBA)
private val CapColor = Color(0xFFC98A3C)
private val XHeightColor = Color(0xFFD9B25A)
private val DescentColor = Color(0xFF4C8DBA)
private val LineGapColor = Color(0xFF6F9A5F)
private val AdvanceColor = Color(0xFF9B6BB0)
private val BearingColor = Color(0xFF3F9E9E)

private const val REF = 1000f // reference text size used to derive em-relative metrics

/** Glyph metrics expressed relative to the em square (baseline at 0, up is positive). */
private data class GlyphMetrics(
    val glyph: String,
    val unitsPerEm: Int,
    val ascentEm: Float,
    val descentEm: Float,
    val lineGapEm: Float,
    val capHeightEm: Float,
    val xHeightEm: Float,
    val advanceEm: Float,
    val lsbEm: Float,
    val inkRightEm: Float,
) {
    val rsbEm: Float get() = advanceEm - inkRightEm

    /** Converts an em-relative value to the font's design units. */
    fun units(em: Float): Int = (em * unitsPerEm).roundToInt()
}

private fun computeGlyphMetrics(font: FontItem, glyph: String): GlyphMetrics {
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = font.typeface
        textSize = REF
    }
    val fm = paint.fontMetrics
    val advance = paint.measureText(glyph)

    val inkBounds = Rect().also { paint.getTextBounds(glyph, 0, glyph.length, it) }
    val capBounds = Rect().also { paint.getTextBounds("H", 0, 1, it) }
    val xBounds = Rect().also { paint.getTextBounds("x", 0, 1, it) }

    val upm = FontHeadReader.unitsPerEm(font.file) ?: 1000

    return GlyphMetrics(
        glyph = glyph,
        unitsPerEm = upm,
        ascentEm = -fm.ascent / REF,
        descentEm = fm.descent / REF,
        lineGapEm = fm.leading / REF,
        capHeightEm = -capBounds.top / REF,
        xHeightEm = -xBounds.top / REF,
        advanceEm = advance / REF,
        lsbEm = inkBounds.left / REF,
        inkRightEm = inkBounds.right.toFloat() / REF,
    )
}

@Composable
fun GlyphMetricsView(
    font: FontItem,
    glyph: String,
    onGlyphChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = remember(font.file.path, glyph) { computeGlyphMetrics(font, glyph) }
    val glyphName = remember(glyph) { glyph.unicodeNameLabel() }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlyphSelector(glyph = glyph, onGlyphChange = onGlyphChange)
        if (glyphName != null) {
            Text(
                text = glyphName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MetricsCanvas(metrics = metrics, font = font)
        Text(
            text = "Units per em: ${metrics.unitsPerEm}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetricsLegend(metrics = metrics)
    }
}

@Composable
private fun GlyphSelector(glyph: String, onGlyphChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = glyph,
            onValueChange = { value ->
                onGlyphChange(value.lastCodePointText())
            },
            label = { Text("Glyph") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.width(120.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("A", "a", "g", "x", "R", "Q", "e", "8", "@", "&").forEach { ch ->
                SuggestionChip(
                    onClick = { onGlyphChange(ch) },
                    label = { Text(ch) },
                )
            }
        }
    }
}

private fun String.lastCodePointText(): String {
    if (isEmpty()) return ""
    val offset = offsetByCodePoints(length, -1)
    return substring(offset)
}

private fun String.unicodeNameLabel(): String? {
    if (isEmpty()) return null

    val labels = ArrayList<String>()
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        val code = "U+${codePoint.toString(16).uppercase().padStart(4, '0')}"
        val name = runCatching { Character.getName(codePoint) }.getOrNull()
        labels += if (name.isNullOrBlank()) code else "$code $name"
        index += Character.charCount(codePoint)
    }
    return labels.joinToString(" + ")
}

@Composable
private fun MetricsCanvas(metrics: GlyphMetrics, font: FontItem) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val glyphPaint = remember(font.file.path) {
        Paint().apply { isAntiAlias = true; typeface = font.typeface }
    }
    val tagPaint = remember { Paint().apply { isAntiAlias = true; textAlign = Paint.Align.RIGHT } }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant.copy(alpha = 0.35f)),
    ) {
        val w = size.width
        val h = size.height

        val contentTopEm = metrics.ascentEm
        val contentBottomEm = metrics.descentEm + metrics.lineGapEm
        val totalVerticalEm = (contentTopEm + contentBottomEm).coerceAtLeast(0.01f)

        // Fit within both height and width, preserving the em aspect ratio.
        val vScale = (h * 0.80f) / totalVerticalEm
        val hScale = if (metrics.advanceEm > 0.01f) (w * 0.65f) / metrics.advanceEm else vScale
        val scale = min(vScale, hScale)

        val baselineY = (h - totalVerticalEm * scale) / 2f + contentTopEm * scale
        val originX = (w - metrics.advanceEm * scale) / 2f

        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
        val tagPx = 11.sp.toPx()

        // The glyph, drawn first so the guides overlay it.
        glyphPaint.textSize = scale
        glyphPaint.color = onSurface.toArgb()
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(metrics.glyph, originX, baselineY, glyphPaint)
        }

        // Horizontal guides. yEm is the signed distance above the baseline.
        fun hGuide(yEm: Float, color: Color, label: String, solid: Boolean = false) {
            val y = baselineY - yEm * scale
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (solid) 2.5f else 1.5f,
                pathEffect = if (solid) null else dash,
            )
            drawIntoCanvas { canvas ->
                tagPaint.textSize = tagPx
                tagPaint.color = labelColor.toArgb()
                canvas.nativeCanvas.drawText(label, w - 8f, y - 6f, tagPaint)
            }
        }

        hGuide(metrics.ascentEm, AscentColor, "ascent")
        if (metrics.capHeightEm > 0.01f) hGuide(metrics.capHeightEm, CapColor, "cap")
        if (metrics.xHeightEm > 0.01f) hGuide(metrics.xHeightEm, XHeightColor, "x-height")
        hGuide(0f, onSurface, "baseline", solid = true)
        hGuide(-metrics.descentEm, DescentColor, "descent")
        if (metrics.lineGapEm > 0.001f) {
            hGuide(-(metrics.descentEm + metrics.lineGapEm), LineGapColor, "line gap")
        }

        // Vertical guides span the inked region.
        val yTop = baselineY - metrics.ascentEm * scale
        val yBottom = baselineY + contentBottomEm * scale
        fun vGuide(xEm: Float, color: Color) {
            val x = originX + xEm * scale
            drawLine(
                color = color,
                start = Offset(x, yTop),
                end = Offset(x, yBottom),
                strokeWidth = 1.5f,
                pathEffect = dash,
            )
        }

        if (metrics.advanceEm > 0.01f) {
            vGuide(0f, AdvanceColor) // origin / pen start
            vGuide(metrics.advanceEm, AdvanceColor) // advance / next origin
        }
        vGuide(metrics.lsbEm, BearingColor) // left edge of ink (LSB boundary)
        vGuide(metrics.inkRightEm, BearingColor) // right edge of ink (RSB boundary)
    }
}

@Composable
private fun MetricsLegend(metrics: GlyphMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LegendRow(AscentColor, "Ascender", metrics.units(metrics.ascentEm))
        LegendRow(CapColor, "Cap height", metrics.units(metrics.capHeightEm))
        LegendRow(XHeightColor, "x-height", metrics.units(metrics.xHeightEm))
        LegendRow(DescentColor, "Descender", metrics.units(metrics.descentEm))
        LegendRow(LineGapColor, "Line gap", metrics.units(metrics.lineGapEm))
        LegendRow(AdvanceColor, "Advance width", metrics.units(metrics.advanceEm))
        LegendRow(BearingColor, "Left side bearing", metrics.units(metrics.lsbEm))
        LegendRow(BearingColor, "Right side bearing", metrics.units(metrics.rsbEm))
    }
}

@Composable
private fun LegendRow(color: Color, name: String, units: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$units upm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
