package org.mecresearch.fontbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Privacy policy") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Last updated August 28, 2026", style = MaterialTheme.typography.labelLarge)
            Text(
                "FontBook does not collect, transmit, share, or sell personal or sensitive " +
                    "user data. It has no ads, analytics, accounts, or network access.",
            )
            Text(
                "FontBook reads font files supplied by Android from system font directories " +
                    "solely to display type samples, glyphs, metrics, and font metadata on your " +
                    "device. Font files and their metadata are not transmitted.",
            )
            Text(
                "Text entered for custom previews and app settings remain on the device and are " +
                    "not transmitted. Uninstalling FontBook removes its app data.",
            )
            Text(
                "This policy may be updated if FontBook's data practices change. Material changes " +
                    "will be reflected here and in the policy linked from the Google Play listing.",
            )
            Text("Developer: Mike Clark (MEC Research)")
            Text(
                text = "Privacy inquiries: mevanlc@mecresearch.org",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    uriHandler.openUri("mailto:mevanlc@mecresearch.org?subject=FontBook%20privacy")
                },
            )
        }
    }
}
