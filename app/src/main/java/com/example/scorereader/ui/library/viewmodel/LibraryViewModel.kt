package com.example.scorereader.ui.library.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.scorereader.domain.model.ScoreFile
import com.example.scorereader.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScoreRepository(application)

    private val _scores = MutableStateFlow<List<ScoreFile>>(emptyList())
    val scores: StateFlow<List<ScoreFile>> = _scores

    fun loadScores() {
        _scores.value = repository.getAllScores()
    }
}