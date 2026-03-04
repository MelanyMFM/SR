package com.example.scorereader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scorereader.ui.home.HomeScreen
import com.example.scorereader.ui.reader.ReaderScreen
import com.example.scorereader.ui.theme.ScoreReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScoreReaderTheme {
                ReaderScreen()
            }
        }
    }
}
