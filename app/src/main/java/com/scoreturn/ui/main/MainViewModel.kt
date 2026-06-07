package com.scoreturn.ui.main

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scoreturn.data.model.AppPermissions
import com.scoreturn.data.model.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * AndroidViewModel en lugar de ViewModel puro porque necesitamos el Application context
 * para verificar el permiso de overlay (que no pasa por el sistema de permisos estándar).
 * En general preferimos ViewModel puro + Hilt para inyectar contexto, pero para
 * esta fase inicial AndroidViewModel es suficiente y evita dependencias extras.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _permissions = MutableStateFlow(AppPermissions())
    val permissions: StateFlow<AppPermissions> = _permissions.asStateFlow()

    private val _statusMessage = MutableStateFlow("ScoreTurn listo")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Verificar overlay requiere leer Settings del sistema, no el sistema de permisos de Android
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(getApplication())
        } else {
            true // Antes de Android 6, el permiso se otorga en instalación
        }
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
            perms.camera == PermissionState.Granted &&
                    perms.overlay == PermissionState.Granted ->
                "✅ ScoreTurn listo para usar"
            perms.camera != PermissionState.Granted &&
                    perms.overlay != PermissionState.Granted ->
                "⚠️ Se necesitan permisos de cámara y overlay"
            perms.camera != PermissionState.Granted ->
                "⚠️ Se necesita permiso de cámara"
            else ->
                "⚠️ Se necesita permiso de overlay (dibujar sobre apps)"
        }
    }
}