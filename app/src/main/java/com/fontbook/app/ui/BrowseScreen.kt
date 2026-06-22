package com.fontbook.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fontbook.app.model.FontItem
import com.fontbook.app.model.ScriptTagInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    fonts: List<FontItem>?,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    listState: LazyListState,
    onFontClick: (FontItem) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val filtered = remember(fonts, query, scriptCatalog, settings.hiddenScriptTags) {
        val q = query.trim().lowercase()
        val visibleFonts = fonts?.filterNot { font ->
            font.supportedScriptTags.any { it in settings.hiddenScriptTags }
        }
        when {
            fonts == null -> null
            q.isEmpty() -> visibleFonts
            else -> visibleFonts?.filter { it.matches(q, scriptCatalog) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = buildAnnotatedString {
                                append("Font")
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append("Book")
                                }
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (fonts != null) {
                            Text(
                                text = if (filtered != null && filtered.size != fonts.size) {
                                    "${filtered.size} of ${fonts.size} fonts"
                                } else {
                                    "${fonts.size} fonts on this device"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { custom.enabled = !custom.enabled }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Custom text",
                            tint = if (custom.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = {
                        if (searchOpen) {
                            searchOpen = false
                            query = ""
                        } else {
                            searchOpen = true
                        }
                    }) {
                        Icon(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search",
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (searchOpen) {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        focusRequester = searchFocusRequester,
                    )
                }
            if (custom.enabled) {
                CustomTextPanel(custom = custom)
            }
            HorizontalDivider()

            when {
                filtered == null -> LoadingState()
                filtered.isEmpty() -> EmptyState(query = query)
                else -> FontList(
                    fonts = filtered,
                    scriptCatalog = scriptCatalog,
                    settings = settings,
                    custom = custom,
                    listState = listState,
                    onFontClick = onFontClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("Search fonts") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun FontList(
    fonts: List<FontItem>,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    listState: LazyListState,
    onFontClick: (FontItem) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(fonts, key = { it.file.path }) { font ->
            FontRow(
                font = font,
                scriptCatalog = scriptCatalog,
                settings = settings,
                custom = custom,
                onClick = { onFontClick(font) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun FontRow(
    font: FontItem,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    onClick: () -> Unit,
) {
    val family: FontFamily = rememberFontFamily(font)
    val scripts = remember(font.supportedScriptTags, scriptCatalog) {
        font.scriptSummary(scriptCatalog)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = font.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (scripts != null) {
            Text(
                text = scripts,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val localizedSample = remember(font.supportedScriptTags, scriptCatalog) {
            font.localizedSample(scriptCatalog)
        }
        val defaultSample = if (settings.preferLocalizedSample) {
            localizedSample ?: Samples.PANGRAM
        } else {
            Samples.PANGRAM
        }
        val sampleText = custom.text.ifBlank { defaultSample }
        Text(
            text = if (custom.enabled) sampleText else defaultSample,
            fontFamily = family,
            fontSize = if (custom.enabled) custom.size.sp else 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            // Custom text can be set very large; keep it to one clipped line so tall
            // display faces can't wrap and overlap the next line. The default pangram
            // stays at a comfortable 22sp where wrapping to a few lines looks fine.
            maxLines = if (custom.enabled) 1 else 3,
            softWrap = !custom.enabled,
            overflow = if (custom.enabled) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun FontItem.localizedSample(scriptCatalog: Map<String, ScriptTagInfo>): String? =
    supportedScriptTags
        .asSequence()
        .mapNotNull { scriptCatalog[it] }
        .flatMap { sequenceOf(it) + it.alternates.asSequence() }
        .mapNotNull { it.qbfText?.takeIf(String::isNotBlank) }
        .firstOrNull()

private fun FontItem.matches(query: String, scriptCatalog: Map<String, ScriptTagInfo>): Boolean =
    searchKey.contains(query) || supportedScriptTags.any { tag ->
        scriptCatalog[tag]?.searchKey?.contains(query) == true
    }

private fun FontItem.scriptSummary(scriptCatalog: Map<String, ScriptTagInfo>): String? {
    if (supportedScriptTags.isEmpty()) return null
    val names = supportedScriptTags
        .map { tag -> scriptCatalog[tag]?.displayName ?: tag.uppercase() }
        .distinct()
    if (names.isEmpty()) return null
    return if (names.size <= 4) {
        names.joinToString(", ")
    } else {
        names.take(4).joinToString(", ") + " +${names.size - 4}"
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Scanning fonts…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (query.isBlank()) "No fonts found" else "No fonts match \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
