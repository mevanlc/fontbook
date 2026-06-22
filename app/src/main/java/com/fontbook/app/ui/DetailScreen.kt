package com.fontbook.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fontbook.app.data.OpenTypeGlyphs
import com.fontbook.app.model.FontItem
import com.fontbook.app.model.ScriptTagInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TABS = listOf("Sample", "Info", "Waterfall", "Glyphs", "Metrics", "Alphabet", "Custom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
    custom: CustomTextState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val family = rememberFontFamily(font)
    var focusGlyph by rememberSaveable(font.file.path) { mutableStateOf("A") }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = font.family,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                        )
                        Text(
                            text = font.style,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
            ) {
                TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        text = { Text(title) },
                    )
                }
            }
            HorizontalDivider()

            when (selectedTab) {
                0 -> SampleTab(font, family, scriptCatalog)
                1 -> InfoTab(
                    font = font,
                    scriptCatalog = scriptCatalog,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
                2 -> WaterfallTab(family)
                3 -> GlyphsTab(
                    font = font,
                    family = family,
                    onGlyphClick = { glyph ->
                        focusGlyph = glyph
                        onSelectTab(TABS.indexOf("Metrics"))
                    },
                )
                4 -> GlyphMetricsView(
                    font = font,
                    glyph = focusGlyph,
                    onGlyphChange = { focusGlyph = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
                5 -> AlphabetTab(family)
                else -> CustomTab(family, custom)
            }
        }
    }
}

@Composable
private fun SampleTab(
    font: FontItem,
    family: FontFamily,
    scriptCatalog: Map<String, ScriptTagInfo>,
) {
    val scriptSamples = remember(font.supportedScriptTags, scriptCatalog) {
        font.supportedScriptTags
            .mapNotNull { scriptCatalog[it] }
            .flatMap { listOf(it) + it.alternates }
            .filter { !it.qbfText.isNullOrBlank() }
            .distinctBy { it.isoLang ?: it.isoBcp47 ?: it.langNameEnglish ?: it.tag }
            .take(8)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(Samples.PANGRAM, fontFamily = family, fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(Samples.PANGRAM_SHORT, fontFamily = family, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        HorizontalDivider()
        Text(Samples.PARAGRAPH, fontFamily = family, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        if (scriptSamples.isNotEmpty()) {
            HorizontalDivider()
            scriptSamples.forEach { script ->
                SpecimenBlock(
                    label = script.languageLabel ?: script.displayName,
                    text = script.qbfText.orEmpty(),
                    family = family,
                    size = 24.sp,
                )
            }
        }
    }
}

@Composable
private fun WaterfallTab(family: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Samples.WATERFALL_SIZES.forEach { size ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$size",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )
                Text(
                    text = Samples.PANGRAM_SHORT,
                    fontFamily = family,
                    fontSize = size.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun GlyphsTab(
    font: FontItem,
    family: FontFamily,
    onGlyphClick: (String) -> Unit,
) {
    val glyphs by produceState<List<OpenTypeGlyphs.Glyph>?>(initialValue = null, key1 = font.file.path) {
        value = withContext(Dispatchers.IO) { OpenTypeGlyphs.read(font.file) }
    }

    when (val currentGlyphs = glyphs) {
        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Reading glyphs...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            val displayGlyphs = currentGlyphs.ifEmpty {
                Samples.GLYPHS.map { glyph -> OpenTypeGlyphs.Glyph(glyph.codePointAt(0)) }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 60.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(displayGlyphs, key = { it.codePoint }) { glyph ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { onGlyphClick(glyph.text) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = glyph.text,
                                fontFamily = family,
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = glyph.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlphabetTab(family: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SpecimenBlock("Uppercase", Samples.UPPERCASE, family, 26.sp)
        SpecimenBlock("Lowercase", Samples.LOWERCASE, family, 26.sp)
        SpecimenBlock("Numerals", Samples.NUMERALS, family, 26.sp)
        SpecimenBlock("Punctuation", Samples.PUNCTUATION, family, 22.sp)
    }
}

@Composable
private fun SpecimenBlock(
    label: String,
    text: String,
    family: FontFamily,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            fontFamily = family,
            fontSize = size,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CustomTab(family: FontFamily, custom: CustomTextState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CustomTextPanel(custom = custom)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = custom.text.ifBlank { Samples.PANGRAM },
            fontFamily = family,
            fontSize = custom.size.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
