package dev.jetforge.runtime.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
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
    contentScale: ContentScale = ContentScale.Crop,
    /** When true, apply bitmap aspect ratio so wrap-height + fill-width images don't collapse. */
    preserveAspectRatio: Boolean = false,
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
    val bmp = bitmap
    if (bmp != null) {
        val sized = if (preserveAspectRatio && bmp.height > 0) {
            modifier.aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
        } else {
            modifier
        }
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            modifier = sized,
            contentScale = contentScale,
        )
    } else {
        val placeholder = if (preserveAspectRatio) {
            modifier.fillMaxWidth().height(180.dp)
        } else {
            modifier
        }
        Box(placeholder.background(accent), contentAlignment = Alignment.BottomStart) {
            Box(
                Modifier
                    .fillMaxSize(0.18f)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
            )
        }
    }
}

internal fun contentScaleOf(name: String?): ContentScale = when (name) {
    "fit" -> ContentScale.Fit
    "fillBounds" -> ContentScale.FillBounds
    "inside" -> ContentScale.Inside
    "none" -> ContentScale.None
    "fillWidth" -> ContentScale.FillWidth
    "fillHeight" -> ContentScale.FillHeight
    else -> ContentScale.Crop
}
