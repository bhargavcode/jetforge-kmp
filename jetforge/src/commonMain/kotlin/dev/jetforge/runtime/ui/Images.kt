package dev.jetforge.runtime.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.jetforge.runtime.JetForge
import dev.jetforge.runtime.bind.resolveMediaUrl
import dev.jetforge.runtime.client.createHttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes

internal expect fun decodeImage(bytes: ByteArray): ImageBitmap?

@Composable
internal fun RemoteImage(
    url: String,
    accent: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        val absolute = resolveMediaUrl(url, JetForge.config.baseUrl).ifBlank { url }
        if (absolute.isBlank() || !(absolute.startsWith("http://") || absolute.startsWith("https://"))) {
            return@LaunchedEffect
        }
        runCatching {
            val bytes = createHttpClient().get(absolute).bodyAsBytes()
            decodeImage(bytes)
        }.onSuccess { bitmap = it }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(accent), contentAlignment = Alignment.BottomStart) {
            Box(
                Modifier
                    .fillMaxSize(0.18f)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
            )
        }
    }
}
