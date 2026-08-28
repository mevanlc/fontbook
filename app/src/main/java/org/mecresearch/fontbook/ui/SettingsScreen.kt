package org.mecresearch.fontbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mecresearch.fontbook.model.FontItem
import org.mecresearch.fontbook.model.ScriptTagInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hiddenScriptCount: Int,
    onBack: () -> Unit,
    onScriptFilterClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SettingSwitchRow(
                title = "Group font families",
                subtitle = "Collapse related styles into expandable families",
                checked = settings.groupFontFamilies,
                onCheckedChange = { settings.groupFontFamilies = it },
            )
            HorizontalDivider()
            SettingSwitchRow(
                title = "Prefer localized sample",
                subtitle = "Use localized pangrams in the font list when available",
                checked = settings.preferLocalizedSample,
                onCheckedChange = { settings.preferLocalizedSample = it },
            )
            HorizontalDivider()
            SettingNavRow(
                title = "Script Filter",
                subtitle = if (hiddenScriptCount == 0) {
                    "All scripts selected"
                } else {
                    "$hiddenScriptCount hidden"
                },
                onClick = onScriptFilterClick,
            )
            HorizontalDivider()
            SettingNavRow(
                title = "Privacy policy",
                subtitle = "How FontBook handles data",
                onClick = onPrivacyPolicyClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptFilterScreen(
    fonts: List<FontItem>,
    scriptCatalog: Map<String, ScriptTagInfo>,
    hiddenScriptTags: Set<String>,
    onHiddenScriptTagsChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val scriptRows = fonts
        .flatMap { it.supportedScriptTags }
        .distinct()
        .sortedWith(compareBy({ scriptCatalog[it]?.displayName ?: it.uppercase() }, { it }))
    val allScriptTags = scriptRows.toSet()
    val allSelected = hiddenScriptTags.none { it in allScriptTags }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Script Filter") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                SelectAllRow(
                    checked = allSelected,
                    label = if (allSelected) "Select none" else "Select all",
                    onClick = {
                        onHiddenScriptTagsChange(
                            if (allSelected) hiddenScriptTags + allScriptTags else hiddenScriptTags - allScriptTags,
                        )
                    },
                )
                HorizontalDivider()
            }
            items(scriptRows, key = { it }) { tag ->
                val script = scriptCatalog[tag]
                ScriptFilterRow(
                    tag = tag,
                    script = script,
                    checked = tag !in hiddenScriptTags,
                    onCheckedChange = { checked ->
                        onHiddenScriptTagsChange(
                            if (checked) hiddenScriptTags - tag else hiddenScriptTags + tag,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun SelectAllRow(
    checked: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ScriptFilterRow(
    tag: String,
    script: ScriptTagInfo?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script?.displayName ?: tag.uppercase(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    tag.uppercase(),
                    script?.languageSummary(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier)
    }
}

private fun ScriptTagInfo.languageSummary(): String? {
    val names = (listOfNotNull(langNameEnglish, langName) + alternates.flatMap {
        listOfNotNull(it.langNameEnglish, it.langName)
    })
        .distinct()
    return names.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
