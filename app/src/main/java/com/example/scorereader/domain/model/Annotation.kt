package com.example.scorereader.domain.model

data class Annotation(
    val id: String,
    val page: Int,
    val type: AnnotationType,
    val points: List<Point>,
    val color: String,
    val strokeWidth: Float
)