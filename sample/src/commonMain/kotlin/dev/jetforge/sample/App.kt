package dev.jetforge.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.jetforge.runtime.JetForge
import dev.jetforge.runtime.JetForgeComponent
import dev.jetforge.runtime.JetForgeConfig
import dev.jetforge.runtime.JetForgeScreen

private enum class AppTab { Home, Briefing, Settings }

@Composable
fun App() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    var baseUrl by remember { mutableStateOf(defaultStudioUrl()) }
    var endpoint by remember { mutableStateOf("us-briefing") }

    LaunchedEffect(baseUrl) {
        JetForge.configure(JetForgeConfig(baseUrl = baseUrl.trimEnd('/')))
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF005DB7),
            secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
        ),
    ) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            when (tab) {
                                AppTab.Home -> "Shop Home"
                                AppTab.Briefing -> "Live briefing"
                                AppTab.Settings -> "Settings"
                            },
                        )
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == AppTab.Home,
                        onClick = { tab = AppTab.Home },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = tab == AppTab.Briefing,
                        onClick = { tab = AppTab.Briefing },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Briefing") },
                    )
                    NavigationBarItem(
                        selected = tab == AppTab.Settings,
                        onClick = { tab = AppTab.Settings },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                    )
                }
            },
        ) { padding ->
            when (tab) {
                AppTab.Home -> HomeScreen(
                    padding = padding,
                    endpoint = endpoint,
                    onOpenBriefing = { tab = AppTab.Briefing },
                )
                AppTab.Briefing -> JetForgeScreen(
                    endpoint = endpoint,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
                AppTab.Settings -> SettingsScreen(
                    padding = padding,
                    baseUrl = baseUrl,
                    endpoint = endpoint,
                    onBaseUrl = { baseUrl = it },
                    onEndpoint = { endpoint = it },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    endpoint: String,
    onOpenBriefing: () -> Unit,
) {
    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Traditional Compose", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Good morning", style = MaterialTheme.typography.headlineMedium)
        Text(
            "This tab is a normal KMP screen you own: product copy, navigation, and local state. The card below is a JetForgeComponent pointed at a published endpoint — drop the same thing into any existing app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpenBriefing, modifier = Modifier.fillMaxWidth()) {
            Text("Open full JetForgeScreen")
        }
        Text("Embedded server-driven region", style = MaterialTheme.typography.titleMedium)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("JetForgeComponent(\"$endpoint\")", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                JetForgeComponent(
                    endpoint = endpoint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    baseUrl: String,
    endpoint: String,
    onBaseUrl: (String) -> Unit,
    onEndpoint: (String) -> Unit,
) {
    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Point this app at any JetForge studio. Publish a screen in the designer, then paste its id or full URL.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrl,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Studio base URL") },
            placeholder = { Text("https://studio.example.com") },
        )
        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpoint,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Screen endpoint") },
            placeholder = { Text("us-briefing") },
        )
        TextButton(onClick = {
            onBaseUrl(defaultStudioUrl())
            onEndpoint("us-briefing")
        }) {
            Text("Reset to local studio")
        }
    }
}
