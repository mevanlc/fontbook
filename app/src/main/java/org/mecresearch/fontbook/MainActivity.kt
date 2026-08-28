package org.mecresearch.fontbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.mecresearch.fontbook.ui.FontBookApp
import org.mecresearch.fontbook.ui.theme.FontBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FontBookTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FontBookApp()
                }
            }
        }
    }
}
