package com.example.scorereader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.scorereader.ui.reader.viewmodel.ReaderViewModel
import java.io.File

@Composable
fun ReaderScreen() {
    val context = LocalContext.current

    val readerViewModel: ReaderViewModel = viewModel()
    val currentPage by readerViewModel.currentPage.collectAsState()
    val pageCount by readerViewModel.pageCount.collectAsState()

    var zoomTarget by remember { mutableStateOf(1f) }
    val zoom by animateFloatAsState(
        targetValue = zoomTarget,
        label = "zoomAnimation"
    )
    val minZoom = 1f
    val maxZoom = 3f


    var bitmap by remember { mutableStateOf<Bitmap?>(null) }


    var twoPagesMode by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage) {
        val assetManager = context.assets
        val inputStream = assetManager.open("sample.pdf")

        val tempFile = File(context.cacheDir, "temp.pdf")
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val fileDescriptor = ParcelFileDescriptor.open(
            tempFile,
            ParcelFileDescriptor.MODE_READ_ONLY
        )

        val renderer = PdfRenderer(fileDescriptor)
        readerViewModel.setPageCount(renderer.pageCount)

        val page = renderer.openPage(currentPage.coerceIn(0, pageCount - 1))

        val bmp = Bitmap.createBitmap(
            page.width,
            page.height,
            Bitmap.Config.ARGB_8888
        )

        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap = bmp

        page.close()
        renderer.close()
        fileDescriptor.close()
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Página PDF",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        zoomTarget = (zoomTarget * zoomChange).coerceIn(minZoom, maxZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoomTarget = 1f  // reset
                        },
                        onTap = { tapOffset ->
                            val screenWidth = size.width
                            if (tapOffset.x > screenWidth / 2) {
                                readerViewModel.nextPage()
                            } else {
                                readerViewModel.previousPage()
                            }
                        }
                    )
                }
        )
    }
}