package com.example.scorereader.domain.model

data class ScoreDocument(
    val id: String,              // UUID o hash
    val name: String,            // Nombre visible
    val pdfPath: String,         // Ruta al PDF
    val annotationsPath: String? // Ruta futura a anotaciones
)