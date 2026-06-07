package com.scoreturn.data.model

/**
 * Sealed class para representar todos los gestos que ScoreTurn puede detectar.
 * Sealed = el compilador conoce todos los subtipos posibles → when() exhaustivo.
 *
 * Diseñado para ser escalable: agregar GuestoEvent.LeftWink,
 * GestureEvent.BothEyebrowsRaised, etc. en el futuro sin romper nada.
 */
sealed class GestureEvent {
    object DoubleBlink : GestureEvent()
    object SingleBlink : GestureEvent()  // Para debug / futuro uso
    data class LeftWink(val confidence: Float) : GestureEvent()   // Fase futura
    data class RightWink(val confidence: Float) : GestureEvent()  // Fase futura
}