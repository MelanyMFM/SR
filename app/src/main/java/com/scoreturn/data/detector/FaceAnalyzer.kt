package com.scoreturn.data.detector

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.scoreturn.data.model.EyeState
import com.scoreturn.data.model.GestureEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ImageAnalysis.Analyzer: el puente entre CameraX y ML Kit.
 *
 * CameraX llama a analyze() con cada frame. Nosotros lo pasamos a ML Kit,
 * que nos devuelve los datos faciales de forma asíncrona.
 *
 * ¿Por qué FAST_MODE en lugar de ACCURATE_MODE?
 * FAST_MODE prioriza velocidad (~30fps) sobre precisión máxima.
 * Para detectar parpadeos no necesitamos landmarks submilimétricos —
 * necesitamos probabilidad de ojo abierto/cerrado a alta frecuencia.
 * ACCURATE_MODE bajaría a ~15fps y añadiría latencia perceptible.
 */
class FaceAnalyzer : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            // Clasificaciones = probabilidades de ojo abierto/cerrado y sonrisa
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            // Solo necesitamos una cara (la del músico frente al teléfono)
            .setMinFaceSize(0.25f) // Mínimo 25% del frame — evita falsas detecciones lejanas
            .build()
    )

    private val blinkDetector = BlinkDetector()

    // StateFlow para el estado continuo de los ojos (para mostrar en UI)
    private val _eyeState = MutableStateFlow<EyeState?>(null)
    val eyeState: StateFlow<EyeState?> = _eyeState.asStateFlow()

    // SharedFlow para eventos de gestos (dispara una vez por gesto detectado)
    // replay=0: los eventos no se "guardan" — si nadie escucha en ese momento, se pierden
    private val _gestureEvent = MutableSharedFlow<GestureEvent>(
        replay = 0,
        extraBufferCapacity = 10  // buffer para no perder eventos
    )
    val gestureEvent: SharedFlow<GestureEvent> = _gestureEvent.asSharedFlow()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                handleFaces(faces)
            }
            .addOnFailureListener {
                // Frame descartado — ML Kit tuvo un error temporal
                _eyeState.value = EyeState(null, null)
            }
            .addOnCompleteListener {
                // CRÍTICO: siempre cerrar el ImageProxy
                // Si no lo hacés, CameraX deja de enviar frames nuevos
                imageProxy.close()
            }
    }

    private fun handleFaces(faces: List<Face>) {
        val face = faces.firstOrNull()

        if (face == null) {
            _eyeState.value = EyeState(null, null)
            return
        }

        val eyeState = EyeState(
            leftEyeOpenProb = face.leftEyeOpenProbability,
            rightEyeOpenProb = face.rightEyeOpenProbability
        )

        _eyeState.value = eyeState

        // Procesamos el estado en el detector de parpadeos
        val gesture = blinkDetector.process(eyeState)
        if (gesture != null) {
            // tryEmit es no-suspending — podemos llamarlo desde el listener de ML Kit
            _gestureEvent.tryEmit(gesture)
        }
    }

    fun release() {
        detector.close()
    }
}