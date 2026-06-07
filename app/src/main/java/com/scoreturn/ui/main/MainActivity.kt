package com.scoreturn.ui.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.scoreturn.ui.theme.ScoreTurnTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /*
     * ActivityResultLauncher para el permiso de cámara.
     * Esta es la forma moderna (recomendada por Google) de pedir permisos en runtime.
     * Reemplaza al deprecated onRequestPermissionsResult.
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermission(granted = isGranted)
    }

    /*
     * Para el overlay NO usamos RequestPermission — ese sistema no funciona para
     * SYSTEM_ALERT_WINDOW. En su lugar abrimos la pantalla de Settings de Android
     * y cuando el usuario regresa a nuestra app, verificamos en onResume.
     */
    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // El resultado no nos dice si se otorgó — hay que verificar manualmente
        viewModel.updateOverlayPermission(viewModel.checkOverlayPermission())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScoreTurnTheme {
                val permissions by viewModel.permissions.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()
                val eyeState by viewModel.eyeState.collectAsState()
                val lastGesture by viewModel.lastGesture.collectAsState()

                MainScreen(
                    statusMessage = statusMessage,
                    permissions = permissions,
                    eyeState = eyeState,
                    lastGesture = lastGesture,
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onRequestOverlayPermission = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        )
                        overlaySettingsLauncher.launch(intent)
                    },
                    onStartCamera = { lifecycleOwner ->
                        viewModel.startCameraIfReady(lifecycleOwner)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificamos overlay cada vez que el usuario regresa a la app
        // (puede haber cambiado en Settings sin que lo detectemos de otra forma)
        viewModel.updateOverlayPermission(viewModel.checkOverlayPermission())
    }
}