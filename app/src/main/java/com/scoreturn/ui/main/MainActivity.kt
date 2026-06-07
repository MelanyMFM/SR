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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.scoreturn.service.ScoreTurnAccessibilityService
import com.scoreturn.ui.theme.ScoreTurnTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermission(granted = isGranted)
    }

    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateOverlayPermission(viewModel.checkOverlayPermission())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

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
                    isAccessibilityActive = ScoreTurnAccessibilityService.isActive(),
                    onRequestCameraPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onRequestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        overlaySettingsLauncher.launch(intent)
                    },
                    onStartCamera = { lifecycleOwner ->
                        viewModel.startCameraIfReady(lifecycleOwner)
                    },
                    onStartService = { context ->
                        viewModel.startOverlayService(context)
                    },
                    onStopService = { context ->
                        viewModel.stopOverlayService(context)
                    },
                    onRequestAccessibility = { context ->
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateOverlayPermission(viewModel.checkOverlayPermission())
    }
}