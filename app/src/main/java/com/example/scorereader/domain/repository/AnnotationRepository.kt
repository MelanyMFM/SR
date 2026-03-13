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

    fun loadAnnotations(pdfName: String, page: Int): List<Annotation> {
        val file = getPageFile(pdfName, page)
        if (!file.exists()) return emptyList()

        val json = file.readText()
        val type = object : TypeToken<List<Annotation>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAnnotations(pdfName: String, page: Int, annotations: List<Annotation>) {
        val file = getPageFile(pdfName, page)
        val json = gson.toJson(annotations)
        file.writeText(json)
    }

    private fun getPageFile(pdfName: String, page: Int): File {
        val dir = File(context.filesDir, "annotations/$pdfName")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return File(dir, "page_$page.json")
    }

}
