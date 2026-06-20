package com.fontbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fontbook.app.ui.FontBookApp
import com.fontbook.app.ui.theme.FontbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FontbookTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FontBookApp()
                }
            }
        }
    }
}
