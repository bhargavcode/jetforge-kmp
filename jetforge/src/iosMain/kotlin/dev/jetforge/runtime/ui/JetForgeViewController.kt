package dev.jetforge.runtime.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import dev.jetforge.runtime.JetForge
import dev.jetforge.runtime.JetForgeConfig
import platform.UIKit.UIViewController

fun JetForgeViewController(
    endpoint: String,
    baseUrl: String = JetForge.config.baseUrl,
): UIViewController = ComposeUIViewController {
    if (baseUrl.isNotBlank()) {
        JetForge.configure(JetForgeConfig(baseUrl = baseUrl))
    }
    JetForgeScreen(endpoint = endpoint, modifier = Modifier)
}
