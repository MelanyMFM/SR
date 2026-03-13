package com.example.scorereader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scorereader.ui.reader.viewmodel.ReaderViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(filePath: String) {

    val fileName = remember(filePath) {
        File(filePath).nameWithoutExtension
    }

    var isAnnotationMode by remember { mutableStateOf(false) }

    val readerViewModel: ReaderViewModel = viewModel()
    val currentPage by readerViewModel.currentPage.collectAsState()

    var currentPath by remember { mutableStateOf(Path()) }

    val pagePaths = remember { mutableStateMapOf<Int, MutableList<Path>>() }
    val pathsForPage = pagePaths.getOrPut(currentPage) { mutableListOf() }

    var zoomTarget by remember { mutableStateOf(1f) }

    val zoom by animateFloatAsState(
        targetValue = zoomTarget,
        label = "zoomAnimation"
    )

    val minZoom = 1f
    val maxZoom = 3f

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = fileName) },
                actions = {
                    TextButton(
                        onClick = { isAnnotationMode = !isAnnotationMode }
                    ) {
                        Text(if (isAnnotationMode) "Leer" else "Anotar")
                    }
                }
            )
        }
    ) { paddingValues ->

        LaunchedEffect(filePath, currentPage) {

            bitmap = null

            val file = File(filePath)
            if (!file.exists()) return@LaunchedEffect

            val fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            val renderer = PdfRenderer(fileDescriptor)
            val totalPages = renderer.pageCount

            readerViewModel.setPageCount(totalPages)

            if (totalPages == 0) {
                renderer.close()
                fileDescriptor.close()
                return@LaunchedEffect
            }

            val safePageIndex = currentPage.coerceIn(0, totalPages - 1)

            val page = renderer.openPage(safePageIndex)

            val bmp = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

            page.render(
                bmp,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            bitmap = bmp

            page.close()
            renderer.close()
            fileDescriptor.close()
        }

        bitmap?.let {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom
                        )
                        .pointerInput(Unit) {

                            detectTransformGestures { _, _, zoomChange, _ ->

                                val newZoom = (zoomTarget * zoomChange)
                                    .coerceIn(minZoom, maxZoom)

                                zoomTarget = newZoom
                                readerViewModel.updateZoom(newZoom)
                            }
                        }
                        .pointerInput(Unit) {

                            detectTapGestures(

                                onDoubleTap = {
                                    zoomTarget = 1f
                                    readerViewModel.resetZoom()
                                },

                                onTap = { tapOffset ->

                                    val screenWidth = this.size.width

                                    if (tapOffset.x > screenWidth / 2) {
                                        readerViewModel.nextPage()
                                    } else {
                                        readerViewModel.previousPage()
                                    }
                                }
                            )
                        }
                )

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    pathsForPage.forEach { path ->

                        drawPath(
                            path = path,
                            color = Color.Red,
                            style = Stroke(width = 4f)
                        )
                    }

                    drawPath(
                        path = currentPath,
                        color = Color.Red,
                        style = Stroke(width = 4f)
                    )
                }

                if (isAnnotationMode) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {

                                detectDragGestures(

                                    onDragStart = { offset ->

                                        currentPath = Path().apply {
                                            moveTo(offset.x, offset.y)
                                        }
                                    },

                                    onDrag = { change, _ ->

                                        val newPath = Path().apply {
                                            addPath(currentPath)
                                            lineTo(
                                                change.position.x,
                                                change.position.y
                                            )
                                        }

                                        currentPath = newPath
                                    },

                                    onDragEnd = {

                                        pathsForPage.add(currentPath)
                                        currentPath = Path()
                                    }
                                )
                            }
                    )
                }
            }
        }
    }


}
