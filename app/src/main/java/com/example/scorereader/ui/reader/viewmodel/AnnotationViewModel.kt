package com.example.scorereader.ui.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.ui.graphics.Path

class AnnotationViewModel : ViewModel() {
    var currentPath: Path? = null

    fun clearTempPaths() {
        currentPath = null
    }
}
