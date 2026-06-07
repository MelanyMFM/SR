package com.scoreturn.data.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.scoreturn.data.detector.FaceAnalyzer
import java.util.concurrent.Executors

/**
 * Encapsula todo el setup de CameraX.
 *
 * ¿Por qué separar esto del ViewModel?
 * CameraX necesita un Context y un LifecycleOwner para bindear la cámara.
 * El ViewModel no debe tener referencias a Context (memory leak) ni a
 * LifecycleOwner directamente. CameraManager vive en la capa de datos
 * y recibe estos parámetros solo cuando los necesita.
 */
class CameraManager(private val context: Context) {

    // Executor dedicado para el análisis de imágenes
    // Usamos un single thread para procesar frames secuencialmente
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    val faceAnalyzer = FaceAnalyzer()

    private var cameraProvider: ProcessCameraProvider? = null

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                // STRATEGY_KEEP_ONLY_LATEST: si ML Kit tarda más de un frame,
                // descarta los intermedios y procesa solo el más reciente.
                // Esto evita que se acumule una cola de frames obsoletos.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor, faceAnalyzer)
                }

            // Cámara frontal — el músico mira la partitura en pantalla
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Desvincula cualquier uso previo antes de rebindear
                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                    // No añadimos Preview use case — no necesitamos mostrar
                    // la imagen de la cámara en pantalla, solo analizarla
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        faceAnalyzer.release()
        analysisExecutor.shutdown()
    }
}