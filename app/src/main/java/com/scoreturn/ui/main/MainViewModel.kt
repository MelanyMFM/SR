package com.scoreturn.ui.main

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.scoreturn.data.camera.CameraManager
import com.scoreturn.data.model.AppPermissions
import com.scoreturn.data.model.EyeState
import com.scoreturn.data.model.GestureEvent
import com.scoreturn.data.model.PermissionState
import com.scoreturn.data.model.isFullyGranted
import com.scoreturn.service.OverlayService
import com.scoreturn.service.ScoreTurnAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Usamos el Singleton — misma instancia que el Service
    private val cameraManager = CameraManager.getInstance(application)

    private val _permissions = MutableStateFlow(AppPermissions())
    val permissions: StateFlow<AppPermissions> = _permissions.asStateFlow()

    private val _statusMessage = MutableStateFlow("ScoreTurn listo")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val eyeState: StateFlow<EyeState?> = cameraManager.faceAnalyzer.eyeState

    private val _lastGesture = MutableStateFlow("")
    val lastGesture: StateFlow<String> = _lastGesture.asStateFlow()

    init {
        viewModelScope.launch {
            cameraManager.faceAnalyzer.gestureEvent.collect { event ->
                when (event) {
                    is GestureEvent.DoubleBlink -> _lastGesture.value = "👁️👁️ DOBLE PARPADEO DETECTADO"
                    is GestureEvent.SingleBlink -> _lastGesture.value = "· parpadeo simple"
                    else -> {}
                }
            }
        }
    }

    fun startCameraIfReady(lifecycleOwner: LifecycleOwner) {
        if (_permissions.value.isFullyGranted() && !cameraManager.isRunning()) {
            cameraManager.startCamera(lifecycleOwner)
        }
    }

    fun checkOverlayPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(getApplication())
        else true

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
            _permissions.update { it.copy(overlay = if (granted) PermissionState.Granted else PermissionState.Denied) }
            updateStatusMessage()
        }
    }

    fun startOverlayService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(OverlayService.startIntent(context))
        } else {
            context.startService(OverlayService.startIntent(context))
        }
    }

    fun stopOverlayService(context: Context) {
        context.startService(OverlayService.stopIntent(context))
    }

    fun isAccessibilityServiceActive(): Boolean = ScoreTurnAccessibilityService.isActive()

    private fun updateStatusMessage() {
        val perms = _permissions.value
        _statusMessage.value = when {
            perms.isFullyGranted() -> "✅ Cámara activa — parpadeá dos veces"
            perms.camera != PermissionState.Granted && perms.overlay != PermissionState.Granted ->
                "⚠️ Se necesitan permisos de cámara y overlay"
            perms.camera != PermissionState.Granted -> "⚠️ Se necesita permiso de cámara"
            else -> "⚠️ Se necesita permiso de overlay"
        }
    }

    override fun onCleared() {
        super.onCleared()
        // NO detenemos la cámara aquí — el Service puede seguir usándola
        // La cámara se detiene solo cuando el Service llama stopCamera()
    }
}