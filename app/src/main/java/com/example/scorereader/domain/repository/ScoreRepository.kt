package com.example.scorereader.domain.repository

import android.content.Context
import com.example.scorereader.domain.model.ScoreFile
import java.io.File

class ScoreRepository(private val context: Context) {

    fun getAllScores(): List<ScoreFile> {
        val filesDir = context.filesDir

        return filesDir
            .listFiles { file ->
                file.extension.lowercase() == "pdf"
            }
            ?.map { file ->
                ScoreFile(
                    name = file.nameWithoutExtension,
                    filePath = file.absolutePath
                )
            }
            ?: emptyList()
    }
}