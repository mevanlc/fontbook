package org.mecresearch.fontbook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.mecresearch.fontbook.data.ScriptTagCatalog
import org.mecresearch.fontbook.data.SystemFontScanner
import org.mecresearch.fontbook.model.FontItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppScreen {
    Browse,
    Settings,
    ScriptFilter,
    PrivacyPolicy,
}

private sealed interface AppRoute {
    data object Browse : AppRoute
    data object Settings : AppRoute
    data object ScriptFilter : AppRoute
    data object PrivacyPolicy : AppRoute
    data class Detail(val font: FontItem) : AppRoute
}

@Composable
fun FontBookApp() {
    val context = LocalContext.current
    val scriptCatalog = remember(context) { ScriptTagCatalog.load(context) }

    // null => still scanning.
    val fonts by produceState<List<FontItem>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { SystemFontScanner.scan() }
    }

    var selectedPath by rememberSaveable { mutableStateOf<String?>(null) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.Browse) }

    // Hoisted custom-text state, shared between Browse and Detail.
    val custom = rememberCustomTextState()
    val settings = rememberAppSettings()

    // Plain `remember` (not saveable): survives navigation between the list and a
    // font's detail, but resets on app restart, per the requested behavior.
    val listState = rememberLazyListState()
    var detailTab by remember { mutableIntStateOf(0) }
    var expandedFamilyKeys by rememberSaveable(
        stateSaver = Saver<Set<String>, ArrayList<String>>(
            save = { ArrayList(it) },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(emptySet()) }

    val selected = remember(fonts, selectedPath) {
        val path = selectedPath
        fonts?.firstOrNull { it.file.path == path }
    }

    BackHandler(enabled = selected != null) { selectedPath = null }
    BackHandler(enabled = selected == null && screen == AppScreen.ScriptFilter) {
        screen = AppScreen.Settings
    }
    BackHandler(enabled = selected == null && screen == AppScreen.PrivacyPolicy) {
        screen = AppScreen.Settings
    }
    BackHandler(enabled = selected == null && screen == AppScreen.Settings) {
        screen = AppScreen.Browse
    }

    val route = selected?.let(AppRoute::Detail) ?: when (screen) {
        AppScreen.Browse -> AppRoute.Browse
        AppScreen.Settings -> AppRoute.Settings
        AppScreen.ScriptFilter -> AppRoute.ScriptFilter
        AppScreen.PrivacyPolicy -> AppRoute.PrivacyPolicy
    }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false))
        },
        label = "screen",
    ) { currentRoute ->
        when (currentRoute) {
            AppRoute.Browse -> BrowseScreen(
                fonts = fonts,
                scriptCatalog = scriptCatalog,
                settings = settings,
                custom = custom,
                listState = listState,
                expandedFamilyKeys = expandedFamilyKeys,
                onFamilyExpandedChange = { key, expanded ->
                    expandedFamilyKeys = if (expanded) {
                        expandedFamilyKeys + key
                    } else {
                        expandedFamilyKeys - key
                    }
                },
                onFontClick = { selectedPath = it.file.path },
                onSettingsClick = { screen = AppScreen.Settings },
            )
            AppRoute.Settings -> SettingsScreen(
                settings = settings,
                hiddenScriptCount = settings.hiddenScriptTags.size,
                onBack = { screen = AppScreen.Browse },
                onScriptFilterClick = { screen = AppScreen.ScriptFilter },
                onPrivacyPolicyClick = { screen = AppScreen.PrivacyPolicy },
            )
            AppRoute.ScriptFilter -> ScriptFilterScreen(
                fonts = fonts.orEmpty(),
                scriptCatalog = scriptCatalog,
                hiddenScriptTags = settings.hiddenScriptTags,
                onHiddenScriptTagsChange = { settings.hiddenScriptTags = it },
                onBack = { screen = AppScreen.Settings },
            )
            AppRoute.PrivacyPolicy -> PrivacyPolicyScreen(
                onBack = { screen = AppScreen.Settings },
            )
            is AppRoute.Detail -> DetailScreen(
                font = currentRoute.font,
                scriptCatalog = scriptCatalog,
                custom = custom,
                selectedTab = detailTab,
                onSelectTab = { detailTab = it },
                onBack = { selectedPath = null },
            )
        }
    }
}

class AppSettings(
    preferLocalizedSample: Boolean,
    hiddenScriptTags: Set<String>,
    groupFontFamilies: Boolean,
) {
    var preferLocalizedSample by mutableStateOf(preferLocalizedSample)
    var hiddenScriptTags by mutableStateOf(hiddenScriptTags)
    var groupFontFamilies by mutableStateOf(groupFontFamilies)

    companion object {
        val Saver: Saver<AppSettings, List<Any>> = Saver(
            save = { listOf(it.preferLocalizedSample, it.hiddenScriptTags.toList(), it.groupFontFamilies) },
            restore = {
                @Suppress("UNCHECKED_CAST")
                AppSettings(
                    preferLocalizedSample = it[0] as Boolean,
                    hiddenScriptTags = (it[1] as List<String>).toSet(),
                    groupFontFamilies = it.getOrNull(2) as? Boolean ?: true,
                )
            },
        )
    }
}

@Composable
fun rememberAppSettings(): AppSettings =
    rememberSaveable(saver = AppSettings.Saver) {
        AppSettings(
            preferLocalizedSample = true,
            hiddenScriptTags = emptySet(),
            groupFontFamilies = true,
        )
    }

/** Mutable holder for the global custom-text showcase mode. */
class CustomTextState(
    text: String,
    size: Float,
    enabled: Boolean,
) {
    var text by mutableStateOf(text)
    var size by mutableStateOf(size)
    var enabled by mutableStateOf(enabled)

    companion object {
        val Saver: Saver<CustomTextState, List<Any>> = Saver(
            save = { listOf(it.text, it.size, it.enabled) },
            restore = { CustomTextState(it[0] as String, it[1] as Float, it[2] as Boolean) },
        )
    }
}

@Composable
fun rememberCustomTextState(): CustomTextState =
    rememberSaveable(saver = CustomTextState.Saver) {
        CustomTextState(text = "Handgloves 1234", size = 28f, enabled = false)
    }
