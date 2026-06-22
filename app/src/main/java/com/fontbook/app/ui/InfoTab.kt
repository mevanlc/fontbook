package com.fontbook.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fontbook.app.data.FontBoxReader
import com.fontbook.app.model.FontItem
import com.fontbook.app.model.FontMetadata
import com.fontbook.app.model.ScriptTagInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loading / parsed / unreadable — distinct so a parse failure isn't mistaken for "still loading". */
private sealed interface MetaState {
    data object Loading : MetaState
    data class Ready(val data: FontMetadata) : MetaState
    data object Failed : MetaState
}

private data class InfoLine(val label: String, val value: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoTab(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
    modifier: Modifier = Modifier,
) {
    val state by produceState<MetaState>(initialValue = MetaState.Loading, key1 = font.file.path) {
        value = withContext(Dispatchers.IO) {
            FontBoxReader.read(font.file)?.let(MetaState::Ready) ?: MetaState.Failed
        }
    }

    when (val s = state) {
        MetaState.Loading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("Reading metadata…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        MetaState.Failed -> UnreadableInfo(font, scriptCatalog, modifier)
        is MetaState.Ready -> {
            val data = s.data
            val lines = infoLines(data, font, scriptCatalog)
            CopyableInfoColumn(lines = lines, modifier = modifier) {
                Section("Identity")
                InfoRow(lines, "Family")
                InfoRow(lines, "Style")
                InfoRow(lines, "Full name")
                InfoRow(lines, "PostScript")
                InfoRow(lines, "Version")

                SectionDivider()
                Section("Design")
                InfoRow(lines, "Foundry")
                InfoRow(lines, "Designer")
                InfoRow(lines, "Weight")
                InfoRow(lines, "Width")
                InfoRow(lines, "Classification")
                InfoRow(lines, "PANOSE")

                ScriptSection(lines)

                SectionDivider()
                Section("Technical")
                InfoRow(lines, "Format")
                InfoRow(lines, "Glyphs")
                InfoRow(lines, "Units / em")
                InfoRow(lines, "Embedding")
                InfoRow(lines, "Created")
                InfoRow(lines, "Modified")

                SectionDivider()
                Section("Legal")
                InfoRow(lines, "Copyright")
                InfoRow(lines, "Trademark")
                InfoRow(lines, "License")
                InfoRow(lines, "License URL")
                InfoRow(lines, "Vendor URL")
                InfoRow(lines, "Designer URL")

                SectionDivider()
                Section("File")
                InfoRow(lines, "Name")
                InfoRow(lines, "Path")
                InfoRow(lines, "Size")
            }
        }
    }
}

/**
 * Shown when FontBox can't parse the file (e.g. bitmap-only color emoji fonts that
 * lack a `glyf`/`loca` table). We still surface the file basics we already hold.
 */
@Composable
private fun UnreadableInfo(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
    modifier: Modifier = Modifier,
) {
    val lines = fallbackInfoLines(font, scriptCatalog)
    CopyableInfoColumn(lines = lines, modifier = modifier) {
        Section("Identity")
        InfoRow(lines, "Family")
        InfoRow(lines, "Style")

        ScriptSection(lines)

        SectionDivider()
        Section("Technical")
        Text(
            text = "Detailed metadata isn't available for this font — its tables " +
                "couldn't be read (common for bitmap color-emoji fonts).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionDivider()
        Section("File")
        InfoRow(lines, "Name")
        InfoRow(lines, "Path")
        InfoRow(lines, "Size")
    }
}

@Composable
private fun ScriptSection(lines: List<InfoLine>) {
    val scriptLines = lines.dropWhile { it.label != "Layout tags" }
        .takeWhile { it.label == "Layout tags" || it.label.length == 4 }
    if (scriptLines.isEmpty()) return

    SectionDivider()
    Section("Scripts")
    scriptLines.forEach { line ->
        InfoRow(line)
    }
}

/** Combines the manufacturer name with the 4-char OS/2 vendor code when present. */
private fun foundry(data: FontMetadata): String? {
    val name = data.manufacturer
    val id = data.vendorId
    return when {
        name != null && id != null -> "$name ($id)"
        name != null -> name
        id != null -> id
        else -> null
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}

/** Renders a label/value pair, or nothing when the value is absent. */
@Composable
private fun InfoRow(lines: List<InfoLine>, label: String) {
    val line = lines.firstOrNull { it.label == label } ?: return
    InfoRow(line)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoRow(line: InfoLine) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { clipboard.copy(line.copyText()) },
            )
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = line.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = line.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableInfoColumn(
    lines: List<InfoLine>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = { clipboard.copyAll(lines) },
            )
            .padding(16.dp),
        content = content,
    )
}

private fun infoLines(
    data: FontMetadata,
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
): List<InfoLine> = buildList {
    addOptional("Family", data.family)
    addOptional("Style", data.subfamily)
    addOptional("Full name", data.fullName)
    addOptional("PostScript", data.postScriptName)
    addOptional("Version", data.version)

    addOptional("Foundry", foundry(data))
    addOptional("Designer", data.designer)
    addOptional("Weight", data.weightClass)
    addOptional("Width", data.widthClass)
    addOptional("Classification", data.familyClass)
    addOptional("PANOSE", data.panoseFamily)

    addScriptLines(font, scriptCatalog)

    addOptional("Format", data.format)
    addOptional("Glyphs", data.numGlyphs?.toString())
    addOptional("Units / em", data.unitsPerEm?.toString())
    addOptional("Embedding", data.embedding)
    addOptional("Created", data.created)
    addOptional("Modified", data.modified)

    addOptional("Copyright", data.copyright)
    addOptional("Trademark", data.trademark)
    addOptional("License", data.license)
    addOptional("License URL", data.licenseUrl)
    addOptional("Vendor URL", data.vendorUrl)
    addOptional("Designer URL", data.designerUrl)

    add(InfoLine("Name", data.fileName))
    add(InfoLine("Path", data.filePath))
    add(InfoLine("Size", formatSize(data.fileSizeBytes)))
}

private fun fallbackInfoLines(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
): List<InfoLine> = buildList {
    addOptional("Family", font.family)
    addOptional("Style", font.style)
    addScriptLines(font, scriptCatalog)
    add(InfoLine("Name", font.file.name))
    add(InfoLine("Path", font.file.path))
    add(InfoLine("Size", formatSize(font.file.length())))
}

private fun MutableList<InfoLine>.addScriptLines(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
) {
    if (font.supportedScriptTags.isEmpty()) return
    add(InfoLine("Layout tags", font.supportedScriptTags.joinToString(", ") { it.uppercase() }))
    font.supportedScriptTags.forEach { tag ->
        val script = scriptCatalog[tag]
        add(
            InfoLine(
                label = tag.uppercase(),
                value = script?.let {
                    listOfNotNull(it.displayName, it.languageLabel?.let { label -> "($label)" })
                        .joinToString(" ")
                } ?: "Unknown script",
            ),
        )
    }
}

private fun MutableList<InfoLine>.addOptional(label: String, value: String?) {
    if (!value.isNullOrBlank()) add(InfoLine(label, value))
}

private fun androidx.compose.ui.platform.ClipboardManager.copy(line: String) {
    setText(AnnotatedString(line))
}

private fun androidx.compose.ui.platform.ClipboardManager.copyAll(lines: List<InfoLine>) {
    setText(AnnotatedString(lines.joinToString("\n") { it.copyText() }))
}

private fun InfoLine.copyText(): String = "$label: $value"

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
