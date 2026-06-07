package com.scoreturn.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scoreturn.data.model.AppPermissions
import com.scoreturn.data.model.EyeState
import com.scoreturn.data.model.PermissionState
import com.scoreturn.data.model.isFullyGranted
import com.scoreturn.ui.theme.*

@Composable
fun MainScreen(
    statusMessage: String,
    permissions: AppPermissions,
    eyeState: EyeState?,
    lastGesture: String,
    onRequestCameraPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onStartCamera: (androidx.lifecycle.LifecycleOwner) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Iniciar cámara cuando los permisos estén listos
    LaunchedEffect(permissions) {
        if (permissions.isFullyGranted()) {
            onStartCamera(lifecycleOwner)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ScoreTurnPrimary, ScoreTurnSecondary)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎵", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ScoreTurn",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTurnOnPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            StatusCard(message = statusMessage)

            // Panel de detección — solo visible cuando la cámara está activa
            if (permissions.isFullyGranted()) {
                Spacer(modifier = Modifier.height(24.dp))
                EyeStatePanel(eyeState = eyeState, lastGesture = lastGesture)
            }

            Spacer(modifier = Modifier.height(24.dp))

            PermissionItem(
                title = "Cámara",
                description = "Para detectar tus parpadeos",
                state = permissions.camera,
                onRequest = onRequestCameraPermission
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "Dibujar sobre apps",
                description = "Para el overlay flotante sobre IMSLP",
                state = permissions.overlay,
                onRequest = onRequestOverlayPermission
            )
        }
    }
}

@Composable
private fun EyeStatePanel(eyeState: EyeState?, lastGesture: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ScoreTurnSurface.copy(alpha = 0.5f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Detección en tiempo real",
            color = ScoreTurnOnPrimary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EyeIndicator(
                label = "Ojo izq",
                probability = eyeState?.leftEyeOpenProb
            )
            EyeIndicator(
                label = "Ojo der",
                probability = eyeState?.rightEyeOpenProb
            )
        }

        if (lastGesture.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            val isDouble = lastGesture.contains("DOBLE")
            val gestureColor by animateColorAsState(
                targetValue = if (isDouble) ScoreTurnAccent else ScoreTurnOnPrimary.copy(alpha = 0.4f),
                animationSpec = tween(300),
                label = "gestureColor"
            )

            Text(
                text = lastGesture,
                color = gestureColor,
                fontSize = if (isDouble) 18.sp else 13.sp,
                fontWeight = if (isDouble) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esperando parpadeo...",
                color = ScoreTurnOnPrimary.copy(alpha = 0.3f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EyeIndicator(label: String, probability: Float?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = ScoreTurnOnPrimary.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val prob = probability ?: 0f
        val eyeColor = when {
            probability == null -> Color.Gray
            prob < 0.25f -> ScoreTurnAccent      // cerrado → rojo
            prob > 0.75f -> ScoreTurnSuccess      // abierto → verde
            else -> ScoreTurnWarning              // intermedio → naranja
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(eyeColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (probability == null) "—"
                else "${(prob * 100).toInt()}%",
                color = eyeColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    val isReady = message.startsWith("✅")
    val cardColor by animateColorAsState(
        targetValue = if (isReady) ScoreTurnSuccess.copy(alpha = 0.15f)
        else ScoreTurnWarning.copy(alpha = 0.15f),
        animationSpec = tween(500),
        label = "statusColor"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ScoreTurnOnPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    state: PermissionState,
    onRequest: () -> Unit,
) {
    val isGranted = state == PermissionState.Granted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTurnSurface.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isGranted) ScoreTurnSuccess else ScoreTurnAccent)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = ScoreTurnOnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = description, color = ScoreTurnOnPrimary.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        if (!isGranted) {
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = ScoreTurnAccent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (state == PermissionState.PermanentlyDenied) "Abrir ajustes" else "Permitir",
                    fontSize = 13.sp,
                    color = ScoreTurnOnPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "✓", color = ScoreTurnSuccess, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}