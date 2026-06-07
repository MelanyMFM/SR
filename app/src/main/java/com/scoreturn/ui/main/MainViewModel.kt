package com.scoreturn.ui.main

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scoreturn.data.camera.CameraManager
import com.scoreturn.data.model.AppPermissions
import com.scoreturn.data.model.EyeState
import com.scoreturn.data.model.GestureEvent
import com.scoreturn.data.model.PermissionState
import com.scoreturn.data.model.isFullyGranted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraManager = CameraManager(application)

    private val _permissions = MutableStateFlow(AppPermissions())
    val permissions: StateFlow<AppPermissions> = _permissions.asStateFlow()

    private val _statusMessage = MutableStateFlow("ScoreTurn listo")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Estado en tiempo real de los ojos (para mostrar probabilidades en UI)
    val eyeState: StateFlow<EyeState?> = cameraManager.faceAnalyzer.eyeState

    // Último gesto detectado (para mostrar en UI)
    private val _lastGesture = MutableStateFlow<String>("")
    val lastGesture: StateFlow<String> = _lastGesture.asStateFlow()

    private var cameraStarted = false

    init {
        // Escuchamos los eventos de gestos del FaceAnalyzer
        viewModelScope.launch {
            cameraManager.faceAnalyzer.gestureEvent.collect { event ->
                when (event) {
                    is GestureEvent.DoubleBlink -> {
                        _lastGesture.value = "👁️👁️ DOBLE PARPADEO DETECTADO"
                        // En Fase 3 aquí irá: simular tecla siguiente página
                    }
                    is GestureEvent.SingleBlink -> {
                        _lastGesture.value = "· parpadeo simple"
                    }
                    else -> {}
                }
            }
        }
    }

    fun startCameraIfReady(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        if (_permissions.value.isFullyGranted() && !cameraStarted) {
            cameraStarted = true
            cameraManager.startCamera(lifecycleOwner)
        }
    }

    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(getApplication())
        } else true
    }

    fun updateCameraPermission(granted: Boolean, permanentlyDenied: Boolean = false) {
        viewModelScope.launch {
            _permissions.update { current ->
                current.copy(
                    camera = when {
                        granted -> PermissionState.Granted
                        permanentlyDenied -> PermissionState.PermanentlyDenied
                        else -> PermissionState.Denied
                    }
                )
            }
            updateStatusMessage()
        }
    }

    fun updateOverlayPermission(granted: Boolean) {
        viewModelScope.launch {
            _permissions.update { current ->
                current.copy(
                    overlay = if (granted) PermissionState.Granted else PermissionState.Denied
                )
            }
            updateStatusMessage()
        }
    }

    private fun updateStatusMessage() {
        val perms = _permissions.value
        _statusMessage.value = when {
            perms.isFullyGranted() -> "✅ Cámara activa — parpadeá dos veces"
            perms.camera != PermissionState.Granted &&
                    perms.overlay != PermissionState.Granted -> "⚠️ Se necesitan permisos de cámara y overlay"
            perms.camera != PermissionState.Granted -> "⚠️ Se necesita permiso de cámara"
            else -> "⚠️ Se necesita permiso de overlay"
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopCamera()
    }
}