package com.scoreturn.data.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.scoreturn.data.detector.FaceAnalyzer
import java.util.concurrent.Executors

class CameraManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: CameraManager? = null

        fun getInstance(context: Context): CameraManager {
            return instance ?: synchronized(this) {
                instance ?: CameraManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    val faceAnalyzer = FaceAnalyzer()
    private var cameraProvider: ProcessCameraProvider? = null
    private var isRunning = false

    private fun buildImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, faceAnalyzer) }
    }

    /**
     * Inicia o transfiere la cámara al lifecycleOwner dado.
     * Si el cameraProvider ya existe (cámara ya fue iniciada antes),
     * el rebind es síncrono — no hay race condition.
     */
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val existingProvider = cameraProvider
        if (existingProvider != null) {
            // Provider ya existe — rebind inmediato y síncrono
            Log.d("CameraManager", "Rebind inmediato a: ${lifecycleOwner::class.simpleName}")
            bindCamera(existingProvider, lifecycleOwner)
            return
        }

        // Primera vez — obtenemos el provider de forma asíncrona
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            Log.d("CameraManager", "Provider obtenido — bind a: ${lifecycleOwner::class.simpleName}")
            bindCamera(provider, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(provider: ProcessCameraProvider, lifecycleOwner: LifecycleOwner) {
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                buildImageAnalysis()
            )
            isRunning = true
            Log.d("CameraManager", "✅ Cámara vinculada a: ${lifecycleOwner::class.simpleName}")
        } catch (e: Exception) {
            Log.e("CameraManager", "❌ Error vinculando cámara: ${e.message}")
            isRunning = false
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        faceAnalyzer.release()
        analysisExecutor.shutdown()
        isRunning = false
        instance = null
        Log.d("CameraManager", "Cámara detenida")
    }

    fun isRunning() = isRunning
}