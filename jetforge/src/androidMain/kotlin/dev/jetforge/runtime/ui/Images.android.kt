package dev.jetforge.runtime.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bitmap.asImageBitmap()
}
