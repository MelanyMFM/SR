package com.scoreturn.data.model

// Modelamos el estado de permisos como un sealed class
// para que la UI pueda reaccionar de forma exhaustiva
sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object NotRequested : PermissionState()
}

data class AppPermissions(
    val camera: PermissionState = PermissionState.NotRequested,
    val overlay: PermissionState = PermissionState.NotRequested,
    val notification: PermissionState = PermissionState.NotRequested,
)

// ¿Están todos los permisos necesarios para operar?
fun AppPermissions.isFullyGranted(): Boolean =
    camera == PermissionState.Granted &&
            overlay == PermissionState.Granted