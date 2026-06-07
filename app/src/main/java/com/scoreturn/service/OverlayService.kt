package com.scoreturn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.scoreturn.R
import com.scoreturn.data.camera.CameraManager
import com.scoreturn.data.model.GestureEvent
import com.scoreturn.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.scoreturn.OVERLAY_START"
        const val ACTION_STOP = "com.scoreturn.OVERLAY_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "scoreturn_overlay"

        fun startIntent(context: Context) =
            Intent(context, OverlayService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, OverlayService::class.java).apply { action = ACTION_STOP }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var isOverlayAdded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                addOverlayBubble()
                transferCameraToProcess()
                startGestureCollector()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    /**
     * ProcessLifecycleOwner = lifecycle del PROCESO completo.
     * Solo pasa a STOPPED cuando toda la app muere — nunca cuando
     * el usuario cambia de Activity o va a background.
     * Es exactamente lo que necesitamos para mantener la cámara viva.
     */
    private fun transferCameraToProcess() {
        Log.d("OverlayService", "Vinculando cámara a ProcessLifecycleOwner")
        CameraManager.getInstance(this)
            .startCamera(ProcessLifecycleOwner.get())
    }

    private fun startGestureCollector() {
        serviceScope.launch(Dispatchers.IO) {
            Log.d("OverlayService", "Collector iniciado")
            CameraManager.getInstance(this@OverlayService)
                .faceAnalyzer.gestureEvent.collect { event ->
                    Log.d("OverlayService", "Gesto: $event")
                    when (event) {
                        is GestureEvent.DoubleBlink -> {
                            Log.d("OverlayService", "¡DOBLE PARPADEO!")
                            launch(Dispatchers.Main) { onDoubleBlink() }
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun onDoubleBlink() {
        ScoreTurnAccessibilityService.performNextPage()
        overlayView?.animate()?.alpha(1f)?.setDuration(80)
            ?.withEndAction {
                overlayView?.animate()?.alpha(0.8f)?.setDuration(150)
            }
    }

    private fun addOverlayBubble() {
        if (isOverlayAdded) return
        val size = (48 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16; y = 100
        }
        overlayView = FrameLayout(this).apply {
            background = androidx.core.content.ContextCompat.getDrawable(
                this@OverlayService, android.R.drawable.presence_online
            )
            alpha = 0.8f
        }
        try {
            windowManager.addView(overlayView, params)
            isOverlayAdded = true
            Log.d("OverlayService", "Bubble agregada")
        } catch (e: Exception) {
            Log.e("OverlayService", "Error bubble: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        CameraManager.getInstance(this).stopCamera()
        if (isOverlayAdded) {
            try { windowManager.removeView(overlayView) }
            catch (e: Exception) { Log.e("OverlayService", "Error: ${e.message}") }
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, stopIntent(this), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener", stopIntent
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}