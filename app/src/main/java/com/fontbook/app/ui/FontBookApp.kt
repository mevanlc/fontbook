package com.fontbook.app.ui

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
import com.fontbook.app.data.SystemFontScanner
import com.fontbook.app.model.FontItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FontBookApp() {
    // null => still scanning.
    val fonts by produceState<List<FontItem>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { SystemFontScanner.scan() }
    }

    var selectedPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Hoisted custom-text state, shared between Browse and Detail.
    val custom = rememberCustomTextState()

    // Plain `remember` (not saveable): survives navigation between the list and a
    // font's detail, but resets on app restart, per the requested behavior.
    val listState = rememberLazyListState()
    var detailTab by remember { mutableIntStateOf(0) }

    val selected = remember(fonts, selectedPath) {
        val path = selectedPath
        fonts?.firstOrNull { it.file.path == path }
    }

    BackHandler(enabled = selected != null) { selectedPath = null }

    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false))
        },
        label = "screen",
    ) { current ->
        if (current == null) {
            BrowseScreen(
                fonts = fonts,
                custom = custom,
                listState = listState,
                onFontClick = { selectedPath = it.file.path },
            )
        } else {
            DetailScreen(
                font = current,
                custom = custom,
                selectedTab = detailTab,
                onSelectTab = { detailTab = it },
                onBack = { selectedPath = null },
            )
        }
    }
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
