package com.example.scorereader.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scorereader.domain.model.ScoreFile
import com.example.scorereader.ui.library.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onScoreSelected: (ScoreFile) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    val scores by viewModel.scores.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadScores()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(scores) { score ->
            Text(
                text = score.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable {
                        onScoreSelected(score)
                    }
            )
        }
    }
}