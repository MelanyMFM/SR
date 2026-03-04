package com.example.scorereader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun ReaderScreen() {
    val context = LocalContext.current

    var currentPage by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pdfFile = File(context.filesDir, "sample.pdf")

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
        pageCount = renderer.pageCount

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
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (currentPage + 1 < pageCount) {
                                currentPage++
                            }
                        }
                    )
                }
        )
    }
}