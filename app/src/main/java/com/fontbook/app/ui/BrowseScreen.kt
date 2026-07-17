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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fontbook.app.model.FontFamilyGroup
import com.fontbook.app.model.FontItem
import com.fontbook.app.model.ScriptTagInfo
import com.fontbook.app.model.groupByFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    fonts: List<FontItem>?,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    listState: LazyListState,
    expandedFamilyKeys: Set<String>,
    onFamilyExpandedChange: (String, Boolean) -> Unit,
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

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            listState.scrollToItem(0)
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
    val familyGroups = remember(filtered) { filtered?.groupByFamily() }

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
                                text = fontCountSummary(
                                    totalFontCount = fonts.size,
                                    shownFontCount = filtered?.size ?: fonts.size,
                                    familyCount = familyGroups?.size,
                                    showFamilyCount = settings.groupFontFamilies,
                                ),
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
                settings.groupFontFamilies && query.isBlank() -> GroupedFontList(
                    groups = familyGroups.orEmpty(),
                    scriptCatalog = scriptCatalog,
                    settings = settings,
                    custom = custom,
                    listState = listState,
                    expandedFamilyKeys = expandedFamilyKeys,
                    onFamilyExpandedChange = onFamilyExpandedChange,
                    onFontClick = onFontClick,
                )
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

private fun fontCountSummary(
    totalFontCount: Int,
    shownFontCount: Int,
    familyCount: Int?,
    showFamilyCount: Boolean,
): String {
    val fontLabel = if (shownFontCount == totalFontCount) {
        "$totalFontCount ${if (totalFontCount == 1) "font" else "fonts"}"
    } else {
        "$shownFontCount of $totalFontCount fonts"
    }
    if (!showFamilyCount || familyCount == null) {
        return if (shownFontCount == totalFontCount) "$fontLabel on this device" else fontLabel
    }
    return "$fontLabel · $familyCount ${if (familyCount == 1) "family" else "families"}"
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
private fun GroupedFontList(
    groups: List<FontFamilyGroup>,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    listState: LazyListState,
    expandedFamilyKeys: Set<String>,
    onFamilyExpandedChange: (String, Boolean) -> Unit,
    onFontClick: (FontItem) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        groups.forEach { group ->
            if (group.variants.size == 1) {
                val font = group.variants.single()
                item(key = "font:${font.file.path}") {
                    FontRow(
                        font = font,
                        scriptCatalog = scriptCatalog,
                        settings = settings,
                        custom = custom,
                        onClick = { onFontClick(font) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            } else {
                val expanded = group.key in expandedFamilyKeys
                item(key = "family:${group.key}") {
                    FontFamilyRow(
                        group = group,
                        scriptCatalog = scriptCatalog,
                        settings = settings,
                        custom = custom,
                        expanded = expanded,
                        onClick = { onFamilyExpandedChange(group.key, !expanded) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
                if (expanded) {
                    items(group.variants, key = { "variant:${it.file.path}" }) { font ->
                        FontRow(
                            font = font,
                            title = group.labelFor(font),
                            horizontalStartPadding = 32.dp,
                            scriptCatalog = scriptCatalog,
                            settings = settings,
                            custom = custom,
                            onClick = { onFontClick(font) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FontFamilyRow(
    group: FontFamilyGroup,
    scriptCatalog: Map<String, ScriptTagInfo>,
    settings: AppSettings,
    custom: CustomTextState,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val font = group.representative
    val family: FontFamily = rememberFontFamily(font)
    val scripts = remember(font.supportedScriptTags, scriptCatalog) {
        font.scriptSummary(scriptCatalog)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(group.variantSummary)
                    scripts?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = if (custom.enabled) sampleText else defaultSample,
                fontFamily = family,
                fontSize = if (custom.enabled) custom.size.sp else 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (custom.enabled) 1 else 3,
                softWrap = !custom.enabled,
                overflow = if (custom.enabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = if (expanded) {
                "Collapse ${group.displayName}"
            } else {
                "Expand ${group.displayName}"
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(8.dp)
                .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
        )
    }
}

@Composable
private fun FontRow(
    font: FontItem,
    title: String = font.displayName,
    horizontalStartPadding: Dp = 16.dp,
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
            .padding(start = horizontalStartPadding, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = title,
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
