package com.scoreturn.data.model

data class EyeState(
    val leftEyeOpenProb: Float?,
    val rightEyeOpenProb: Float?,
    val timestampMs: Long = System.currentTimeMillis()
)

const val EYE_CLOSED_THRESHOLD = 0.40f
const val EYE_OPEN_THRESHOLD = 0.65f

// Promedio de ambos ojos — robusto ante iluminación desigual
fun EyeState.averageOpenProb(): Float? {
    val left = leftEyeOpenProb ?: return null
    val right = rightEyeOpenProb ?: return null
    return (left + right) / 2f
}

fun EyeState.isBlinking(): Boolean {
    val avg = averageOpenProb() ?: return false
    return avg < EYE_CLOSED_THRESHOLD
}

fun EyeState.isOpen(): Boolean {
    val avg = averageOpenProb() ?: return false
    return avg > EYE_OPEN_THRESHOLD
}