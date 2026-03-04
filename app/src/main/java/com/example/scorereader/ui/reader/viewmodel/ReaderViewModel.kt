package com.example.scorereader.ui.reader.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReaderViewModel : ViewModel() {

    // Página actual
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    // Total de páginas
    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount

    // Zoom
    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom

    fun setPageCount(count: Int) {
        _pageCount.value = count
    }

    fun nextPage() {
        if (_currentPage.value < _pageCount.value - 1) {
            _currentPage.value++
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
        }
    }

    fun resetZoom() {
        _zoom.value = 1f
    }

    fun updateZoom(newZoom: Float) {
        _zoom.value = newZoom
    }
}