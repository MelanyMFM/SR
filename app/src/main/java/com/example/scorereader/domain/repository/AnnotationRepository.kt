package com.example.scorereader.domain.repository

import android.content.Context
import com.example.scorereader.domain.model.Annotation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class AnnotationRepository(
    private val context: Context
) {
    private val gson = Gson()

    fun loadAnnotations(documentId: String): List<Annotation> {
        val file = getAnnotationFile(documentId)
        if (!file.exists()) return emptyList()

        val json = file.readText()
        val type = object : TypeToken<List<Annotation>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAnnotations(documentId: String, annotations: List<Annotation>) {
        val file = getAnnotationFile(documentId)
        val json = gson.toJson(annotations)
        file.writeText(json)
    }

    private fun getAnnotationFile(documentId: String): File {
        val dir = File(context.filesDir, "annotations")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$documentId.json")
    }
}