package com.scoreturn.data.detector

import android.util.Log
import com.scoreturn.data.model.EyeState
import com.scoreturn.data.model.GestureEvent
import com.scoreturn.data.model.isBlinking
import com.scoreturn.data.model.isOpen

class BlinkDetector(
    private val maxDobleBlinkWindowMs: Long = 1200L, // más tiempo entre parpadeos
    private val cooldownMs: Long = 1500L
) {

    private enum class State {
        EYES_OPEN,
        EYES_CLOSED,
        FIRST_BLINK_COMPLETE,
        SECOND_CLOSE
    }

    private var state = State.EYES_OPEN
    private var firstBlinkTimestamp = 0L
    private var lastGestureTimestamp = 0L

    fun process(eyeState: EyeState): GestureEvent? {
        val now = eyeState.timestampMs
        val left = eyeState.leftEyeOpenProb
        val right = eyeState.rightEyeOpenProb

        // Log para ver qué recibe el detector en cada frame
        Log.d("BlinkDetector", "state=$state L=${left?.let { "%.2f".format(it) }} R=${right?.let { "%.2f".format(it) }}")

        if (now - lastGestureTimestamp < cooldownMs) {
            Log.d("BlinkDetector", "En cooldown, ignorando")
            return null
        }

        when (state) {
            State.EYES_OPEN -> {
                if (eyeState.isBlinking()) {
                    Log.d("BlinkDetector", "→ EYES_CLOSED")
                    state = State.EYES_CLOSED
                }
            }
            State.EYES_CLOSED -> {
                if (eyeState.isOpen()) {
                    Log.d("BlinkDetector", "→ FIRST_BLINK_COMPLETE")
                    state = State.FIRST_BLINK_COMPLETE
                    firstBlinkTimestamp = now
                }
            }
            State.FIRST_BLINK_COMPLETE -> {
                val timeSinceFirst = now - firstBlinkTimestamp
                if (timeSinceFirst > maxDobleBlinkWindowMs) {
                    Log.d("BlinkDetector", "→ timeout, vuelve a EYES_OPEN (SingleBlink)")
                    state = State.EYES_OPEN
                    lastGestureTimestamp = now
                    return GestureEvent.SingleBlink
                }
                if (eyeState.isBlinking()) {
                    Log.d("BlinkDetector", "→ SECOND_CLOSE")
                    state = State.SECOND_CLOSE
                }
            }
            State.SECOND_CLOSE -> {
                if (eyeState.isOpen()) {
                    Log.d("BlinkDetector", "→ ¡DOUBLE BLINK!")
                    state = State.EYES_OPEN
                    lastGestureTimestamp = now
                    return GestureEvent.DoubleBlink
                }
            }
        }
        return null
    }

    fun reset() {
        state = State.EYES_OPEN
        firstBlinkTimestamp = 0L
        lastGestureTimestamp = 0L
    }
}