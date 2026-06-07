package com.scoreturn.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ScoreTurnAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: ScoreTurnAccessibilityService? = null

        fun isActive(): Boolean = instance != null

        fun performNextPage() {
            val service = instance
            if (service == null) {
                Log.w("A11yService", "Servicio no activo")
                return
            }

            // Delay de 200ms para asegurar que IMSLP tiene el foco
            // antes de enviar el gesto
            Handler(Looper.getMainLooper()).postDelayed({
                service.doNextPage()
            }, 200)
        }

        fun performPreviousPage() {
            val service = instance ?: return
            Handler(Looper.getMainLooper()).postDelayed({
                service.doPreviousPage()
            }, 200)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("A11yService", "✅ Servicio de accesibilidad conectado")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("A11yService", "Servicio de accesibilidad destruido")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun doNextPage() {
        Log.d("A11yService", "Ejecutando siguiente página")

        // Estrategia 1: swipe izquierda (más universal en lectores de partituras)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val swiped = performSwipeLeft()
            Log.d("A11yService", "Swipe izquierda: $swiped")
            if (swiped) return
        }

        // Estrategia 2: tecla flecha derecha
        val keyed = performGlobalAction(GLOBAL_ACTION_KEYCODE_DPAD_RIGHT)
        Log.d("A11yService", "DPAD_RIGHT: $keyed")
    }

    private fun doPreviousPage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            performSwipeRight()
        } else {
            performGlobalAction(GLOBAL_ACTION_KEYCODE_DPAD_LEFT)
        }
    }

    private fun performSwipeLeft(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        // Swipe de derecha a izquierda en el centro vertical de la pantalla
        val path = Path().apply {
            moveTo(w * 0.80f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.50f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d("A11yService", "Swipe completado")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w("A11yService", "Swipe cancelado")
            }
        }, null)
    }

    private fun performSwipeRight(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()

        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.80f, h * 0.50f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    // Constantes correctas para DPAD
    private val GLOBAL_ACTION_KEYCODE_DPAD_RIGHT = 21
    private val GLOBAL_ACTION_KEYCODE_DPAD_LEFT = 20
}