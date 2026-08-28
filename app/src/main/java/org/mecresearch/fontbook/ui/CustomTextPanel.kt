package org.mecresearch.fontbook.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Editor for the custom-text showcase mode: a free-text field plus a font-size
 * slider. The same panel is used at the top of the browse list and inside a
 * font's detail "Custom" tab.
 */
@Composable
fun CustomTextPanel(
    custom: CustomTextState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = custom.text,
            onValueChange = { custom.text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Custom text") },
            placeholder = { Text(Samples.PANGRAM) },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${custom.size.toInt()} sp",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(56.dp),
            )
            Slider(
                value = custom.size,
                onValueChange = { custom.size = it },
                valueRange = 8f..120f,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
