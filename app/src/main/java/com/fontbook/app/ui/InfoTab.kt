package com.fontbook.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fontbook.app.data.FontBoxReader
import com.fontbook.app.model.FontItem
import com.fontbook.app.model.FontMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loading / parsed / unreadable — distinct so a parse failure isn't mistaken for "still loading". */
private sealed interface MetaState {
    data object Loading : MetaState
    data class Ready(val data: FontMetadata) : MetaState
    data object Failed : MetaState
}

@Composable
fun InfoTab(font: FontItem, modifier: Modifier = Modifier) {
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
        MetaState.Failed -> UnreadableInfo(font, modifier)
        is MetaState.Ready -> {
            val data = s.data
            Column(modifier = modifier.padding(16.dp)) {
            Section("Identity")
            InfoRow("Family", data.family)
            InfoRow("Style", data.subfamily)
            InfoRow("Full name", data.fullName)
            InfoRow("PostScript", data.postScriptName)
            InfoRow("Version", data.version)

            SectionDivider()
            Section("Design")
            InfoRow("Foundry", foundry(data))
            InfoRow("Designer", data.designer)
            InfoRow("Weight", data.weightClass)
            InfoRow("Width", data.widthClass)
            InfoRow("Classification", data.familyClass)
            InfoRow("PANOSE", data.panoseFamily)

            SectionDivider()
            Section("Technical")
            InfoRow("Format", data.format)
            InfoRow("Glyphs", data.numGlyphs?.toString())
            InfoRow("Units / em", data.unitsPerEm?.toString())
            InfoRow("Embedding", data.embedding)
            InfoRow("Created", data.created)
            InfoRow("Modified", data.modified)

            SectionDivider()
            Section("Legal")
            InfoRow("Copyright", data.copyright)
            InfoRow("Trademark", data.trademark)
            InfoRow("License", data.license)
            InfoRow("License URL", data.licenseUrl)
            InfoRow("Vendor URL", data.vendorUrl)
            InfoRow("Designer URL", data.designerUrl)

            SectionDivider()
            Section("File")
            InfoRow("Name", data.fileName)
            InfoRow("Path", data.filePath)
            InfoRow("Size", formatSize(data.fileSizeBytes))
            }
        }
    }
}

/**
 * Shown when FontBox can't parse the file (e.g. bitmap-only color emoji fonts that
 * lack a `glyf`/`loca` table). We still surface the file basics we already hold.
 */
@Composable
private fun UnreadableInfo(font: FontItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Section("Identity")
        InfoRow("Family", font.family)
        InfoRow("Style", font.style)

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
        InfoRow("Name", font.file.name)
        InfoRow("Path", font.file.path)
        InfoRow("Size", formatSize(font.file.length()))
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
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
