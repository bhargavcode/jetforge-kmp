package dev.jetforge.runtime.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Native Compose pointer handling for published interactions.
 * Never a WebView — tap, double-tap, long-press, and swipes stay on the Compose tree.
 */
internal fun Modifier.studioGestures(enabled: Boolean, onEvent: (String) -> Unit): Modifier {
    if (!enabled) return this
    return pointerInput(onEvent) {
        detectTapGestures(
            onDoubleTap = { onEvent("doubleTap") },
            onLongPress = { onEvent("longPress") },
            onTap = { onEvent("tap") },
        )
    }.pointerInput(onEvent) {
        var total = Offset.Zero
        detectDragGestures(
            onDragStart = { total = Offset.Zero },
            onDrag = { change, dragAmount ->
                total += dragAmount
                change.consume()
            },
            onDragEnd = {
                val absX = abs(total.x)
                val absY = abs(total.y)
                if (absX < 48f && absY < 48f) return@detectDragGestures
                when {
                    absX >= absY && total.x < 0 -> onEvent("swipeLeft")
                    absX >= absY -> onEvent("swipeRight")
                    total.y < 0 -> onEvent("swipeUp")
                    else -> onEvent("swipeDown")
                }
            },
        )
    }
}
