package dev.jetforge.runtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.jetforge.runtime.JetForge
import dev.jetforge.runtime.JetForgeConfig
import dev.jetforge.runtime.client.JetForgeClient
import dev.jetforge.runtime.host.JetForgeLoadState
import dev.jetforge.runtime.host.JetForgeSession

/**
 * Drop-in screen that loads a published JetForge document from [endpoint]
 * and renders it with native Compose (never a WebView). A shimmer skeleton
 * is shown while the payload is in flight.
 *
 * [endpoint] may be a screen id (`us-briefing`), a path (`/api/screens/us-briefing`),
 * or an absolute URL.
 */
@Composable
fun JetForgeScreen(
    endpoint: String,
    modifier: Modifier = Modifier,
    config: JetForgeConfig = JetForge.config,
    onOpenUrl: ((String) -> Unit)? = null,
) {
    JetForgeSurface(
        endpoint = endpoint,
        modifier = modifier.fillMaxSize(),
        config = config,
        compactShimmer = false,
        onOpenUrl = onOpenUrl,
    )
}

/**
 * Embeddable published region for a traditional screen — pass the same endpoint
 * you would give [JetForgeScreen]. Size it with [modifier].
 */
@Composable
fun JetForgeComponent(
    endpoint: String,
    modifier: Modifier = Modifier,
    config: JetForgeConfig = JetForge.config,
    onOpenUrl: ((String) -> Unit)? = null,
) {
    JetForgeSurface(
        endpoint = endpoint,
        modifier = modifier,
        config = config,
        compactShimmer = true,
        onOpenUrl = onOpenUrl,
    )
}

@Composable
private fun JetForgeSurface(
    endpoint: String,
    modifier: Modifier,
    config: JetForgeConfig,
    compactShimmer: Boolean,
    onOpenUrl: ((String) -> Unit)?,
) {
    val uriHandler = LocalUriHandler.current
    val client = remember(config) { JetForgeClient(config) }
    var nonce by remember(endpoint, config) { mutableStateOf(0) }
    var state by remember(endpoint, config) { mutableStateOf<JetForgeLoadState>(JetForgeLoadState.Loading) }

    LaunchedEffect(endpoint, config, nonce) {
        state = JetForgeLoadState.Loading
        try {
            val document = client.fetchDocument(endpoint)
            val session = JetForgeSession(
                document = document,
                client = client,
                onOpenUrl = { url -> (onOpenUrl ?: uriHandler::openUri).invoke(url) },
            )
            state = JetForgeLoadState.Ready(session)
            session.loadCurrent()
        } catch (error: Exception) {
            state = JetForgeLoadState.Failed(error.message ?: "Could not load $endpoint")
        }
    }

    Box(modifier) {
        when (val current = state) {
            JetForgeLoadState.Loading -> JetForgeShimmerSkeleton(compact = compactShimmer)
            is JetForgeLoadState.Failed -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Could not load screen", style = MaterialTheme.typography.titleMedium)
                Text(
                    current.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                Button(onClick = { nonce += 1 }) { Text("Retry") }
            }
            is JetForgeLoadState.Ready -> DocumentRenderer(current.session, Modifier.fillMaxSize())
        }
    }
}
