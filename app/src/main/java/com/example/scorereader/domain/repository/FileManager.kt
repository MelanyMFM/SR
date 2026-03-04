package com.example.scorereader.domain.repository

import android.content.Context
import java.io.File

class FileManager(private val context: Context) {

    fun scoresDir(): File {
        val dir = File(context.filesDir, "scores")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun annotationsDir(): File {
        val dir = File(context.filesDir, "annotations")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

//files/
//├── scores/
//│    ├── bach_sonata.pdf
//│    ├── mozart_concerto.pdf
//│
//├── annotations/
//│    ├── bach_sonata.json
//│    ├── mozart_concerto.json