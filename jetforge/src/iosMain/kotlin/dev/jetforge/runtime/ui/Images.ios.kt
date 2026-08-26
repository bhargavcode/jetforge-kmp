package dev.jetforge.runtime.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap? {
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}
