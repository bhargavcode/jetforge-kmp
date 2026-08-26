package dev.jetforge.runtime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.jetforge.runtime.JetForge
import dev.jetforge.runtime.bind.BindingScope
import dev.jetforge.runtime.bind.asDisplayString
import dev.jetforge.runtime.bind.bool
import dev.jetforge.runtime.bind.int
import dev.jetforge.runtime.bind.isNodeVisible
import dev.jetforge.runtime.bind.prop
import dev.jetforge.runtime.bind.resolveList
import dev.jetforge.runtime.bind.resolveMediaUrl
import dev.jetforge.runtime.bind.resolvePath
import dev.jetforge.runtime.host.JetForgeSession
import dev.jetforge.runtime.model.ClickAction
import dev.jetforge.runtime.model.EnterAnimation
import dev.jetforge.runtime.model.UiNode
import dev.jetforge.runtime.model.actionFor
import dev.jetforge.runtime.model.hasExtraGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

internal val LocalJetForgeSession = compositionLocalOf<JetForgeSession?> { null }
internal val LocalBindingScope = compositionLocalOf<BindingScope> { mutableMapOf() }

@Composable
internal fun DocumentRenderer(session: JetForgeSession, modifier: Modifier = Modifier) {
    val dark = session.document.theme.mode == "dark"
    MaterialTheme(colorScheme = seedScheme(session.document.theme.seed, dark)) {
        CompositionLocalProvider(
            LocalJetForgeSession provides session,
            LocalBindingScope provides session.scope,
        ) {
            Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                RenderNode(session.root, session.scope, 0)
            }
        }
    }
}

@Composable
private fun RenderNode(node: UiNode, scope: BindingScope, itemIndex: Int) {
    val session = LocalJetForgeSession.current
    if (session != null && !isNodeVisible(node.visibleWhen, session.uiState, session.hasFormError)) {
        return
    }
    AnimatedVisibility(
        visible = true,
        enter = enterTransition(node.animation, itemIndex),
    ) {
        NodeBody(node, scope, itemIndex)
    }
}

private fun fireAction(
    session: JetForgeSession?,
    coroutine: CoroutineScope,
    action: ClickAction?,
    scope: BindingScope,
) {
    if (session == null || action == null || action.type == "none") return
    coroutine.launch { session.dispatch(action, scope) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeBody(node: UiNode, scope: BindingScope, itemIndex: Int) {
    val session = LocalJetForgeSession.current
    val coroutine = rememberCoroutineScope()
    val tap = node.actionFor("tap")
    val runTap = session != null && tap != null && tap.type != "none"
    val hasExtra = node.hasExtraGestures()
    val clickModifier = when {
        hasExtra -> Modifier.studioGestures(true) { event ->
            fireAction(session, coroutine, node.actionFor(event), scope)
        }
        runTap -> Modifier.clickable { fireAction(session, coroutine, tap, scope) }
        else -> Modifier
    }
    val modifier = node.studioModifier().then(clickModifier)
    fun nativeTap() {
        if (runTap && !hasExtra) fireAction(session, coroutine, tap, scope)
    }

    when (node.type) {
        "Scaffold" -> StudioScaffold(node, scope)
        "Column" -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(node.props.int("spacedBy", 8).dp),
        ) { node.children.forEach { RenderNode(it, scope, itemIndex) } }
        "Row" -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(node.props.int("spacedBy", 8).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { node.children.forEach { RenderNode(it, scope, itemIndex) } }
        "Box" -> Box(modifier) { node.children.forEach { RenderNode(it, scope, itemIndex) } }
        "LazyColumn" -> StudioList(node, scope, modifier)
        "Card" -> Card(
            modifier = modifier,
            shape = clipShape(node.modifiers.clip) ?: RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (node.props.prop("variant") == "outlined") 0.dp else 1.dp,
            ),
            border = if (node.props.prop("variant") == "outlined") {
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            } else null,
        ) {
            Column { node.children.forEach { RenderNode(it, scope, itemIndex) } }
        }
        "TopAppBar" -> TopAppBar(
            title = { Text(resolve(node, "title", scope) ?: "Title") },
            navigationIcon = {
                IconButton(onClick = { nativeTap() }) {
                    Icon(iconOf(node.props.prop("navigationIcon") ?: "menu"), contentDescription = "nav")
                }
            },
            actions = { Icon(Icons.Filled.Notifications, contentDescription = null) },
        )
        "NavigationBar" -> NavigationBar {
            node.children.forEach { child ->
                val selected = (
                    session != null &&
                        child.actionFor("tap")?.screenId != null &&
                        session.screenId == child.actionFor("tap")?.screenId
                    ) || child.props.bool("selected")
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        fireAction(session, coroutine, child.actionFor("tap"), scope)
                    },
                    icon = { Icon(iconOf(child.props.prop("icon") ?: "home"), contentDescription = null) },
                    label = { Text(child.props.prop("label").orEmpty()) },
                )
            }
        }
        "FAB" -> FloatingActionButton(onClick = { nativeTap() }) {
            Icon(iconOf(node.props.prop("icon") ?: "add"), contentDescription = null)
        }
        "FilledButton" -> Button(
            onClick = { nativeTap() },
            modifier = modifier,
        ) { Text(resolve(node, "label", scope) ?: "Action") }
        "OutlinedButton" -> OutlinedButton(
            onClick = { nativeTap() },
            modifier = modifier,
        ) { Text(resolve(node, "label", scope) ?: "Action") }
        "TextButton" -> TextButton(
            onClick = { nativeTap() },
            modifier = modifier,
        ) { Text(resolve(node, "label", scope) ?: "Action") }
        "Chip" -> AssistChip(
            onClick = { nativeTap() },
            label = { Text(resolve(node, "label", scope) ?: "Chip") },
        )
        "TextField" -> {
            val formId = node.formField?.formId
            val name = node.formField?.name
            val formValue = if (formId != null && name != null) session?.formValues[formId]?.get(name) else null
            val value = formValue ?: resolve(node, "value", scope).orEmpty()
            val invalid = formId != null && name != null && session?.formErrors[formId]?.containsKey(name) == true
            OutlinedTextField(
                value = value,
                onValueChange = { next ->
                    if (formId != null && name != null) session?.setFormValue(formId, name, next)
                },
                modifier = modifier.fillMaxWidth(),
                label = { Text(resolve(node, "label", scope).orEmpty()) },
                placeholder = { Text(node.props.prop("placeholder").orEmpty()) },
                isError = invalid,
                enabled = session != null,
            )
        }
        "Switch" -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(resolve(node, "label", scope).orEmpty(), modifier = Modifier.weight(1f))
            Switch(checked = node.props.bool("checked"), onCheckedChange = null)
        }
        "Checkbox" -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(resolve(node, "label", scope).orEmpty(), modifier = Modifier.weight(1f))
            Checkbox(checked = node.props.bool("checked"), onCheckedChange = null)
        }
        "Text" -> Text(
            text = resolve(node, "text", scope).orEmpty(),
            style = typeStyle(node.props.prop("style")),
            color = colorToken(node.props.prop("color")),
            modifier = modifier,
        )
        "Image" -> {
            val accent = parseHex(resolve(node, "accent", scope) ?: node.props.prop("accent") ?: "#6750A4")
            RemoteImage(
                url = resolveMediaUrl(resolve(node, "url", scope).orEmpty(), JetForge.config.baseUrl),
                accent = accent,
                modifier = modifier.height((node.modifiers.heightDp ?: 72).dp).clip(clipShape(node.modifiers.clip) ?: RoundedCornerShape(8.dp)),
                contentDescription = node.props.prop("alt"),
            )
        }
        "Icon" -> {
            val custom = resolveMediaUrl(resolve(node, "url", scope).orEmpty(), JetForge.config.baseUrl)
            if (custom.isNotBlank()) {
                RemoteImage(
                    url = custom,
                    accent = Color.Transparent,
                    modifier = Modifier.size(node.props.int("size", 24).dp),
                    contentDescription = null,
                )
            } else {
                Icon(
                    iconOf(node.props.prop("name") ?: "star"),
                    contentDescription = null,
                    tint = colorToken(node.props.prop("color") ?: "primary"),
                    modifier = Modifier.size(node.props.int("size", 24).dp),
                )
            }
        }
        "ListItem" -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(iconOf(node.props.prop("leadingIcon") ?: "star"), contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(resolve(node, "headline", scope).orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(resolve(node, "supporting", scope).orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        "Divider" -> HorizontalDivider(modifier)
        "Spacer" -> Spacer(Modifier.height(node.props.int("height", 16).dp))
        "CircularProgress" -> CircularProgressIndicator(Modifier.size(node.props.int("size", 40).dp))
        else -> Text("Unknown ${node.type}", color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioScaffold(node: UiNode, scope: BindingScope) {
    val top = node.children.find { it.slot == "topBar" || it.type == "TopAppBar" }
    val bottom = node.children.find { it.slot == "bottomBar" || it.type == "NavigationBar" }
    val fab = node.children.find { it.slot == "fab" || it.type == "FAB" }
    val content = node.children.find { it.slot == "content" }
        ?: node.copy(children = node.children.filter { it != top && it != bottom && it != fab })
    Scaffold(
        topBar = { if (top != null) RenderNode(top, scope, 0) },
        bottomBar = { if (bottom != null) RenderNode(bottom, scope, 0) },
        floatingActionButton = { if (fab != null) RenderNode(fab, scope, 0) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            RenderNode(content, scope, 0)
        }
    }
}

@Composable
private fun StudioList(node: UiNode, scope: BindingScope, modifier: Modifier) {
    val path = node.itemBinding.orEmpty()
    if (path.isBlank()) {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(node.props.int("spacedBy", 12).dp)) {
            items(node.children.size) { index -> RenderNode(node.children[index], scope, index) }
        }
        return
    }
    val items = resolveList(scope, path)
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(node.props.int("spacedBy", 12).dp)) {
        itemsIndexed(items) { index, item ->
            val childScope = scope.toMutableMap().apply { put("item", item) }
            Column {
                node.children.forEach { RenderNode(it, childScope, index) }
            }
        }
    }
}

private fun resolve(node: UiNode, key: String, scope: BindingScope): String? {
    val binding = node.bindings[key]
    if (!binding.isNullOrBlank()) {
        return scope.resolvePath(binding)?.asDisplayString()
    }
    return node.props.prop(key)
}

private fun UiNode.studioModifier(): Modifier {
    var modifier: Modifier = Modifier
    if (modifiers.fillMaxWidth) modifier = modifier.fillMaxWidth()
    if (modifiers.fillMaxHeight) modifier = modifier.fillMaxHeight()
    modifiers.widthDp?.let { modifier = modifier.width(it.dp) }
    modifiers.heightDp?.let { modifier = modifier.height(it.dp) }
    modifiers.padding?.let { pad ->
        modifier = if (pad.all != null) modifier.padding(pad.all.dp)
        else modifier.padding(
            start = (pad.start ?: 0).dp,
            top = (pad.top ?: 0).dp,
            end = (pad.end ?: 0).dp,
            bottom = (pad.bottom ?: 0).dp,
        )
    }
    clipShape(modifiers.clip)?.let { modifier = modifier.clip(it) }
    return modifier
}

private fun clipShape(clip: String?) = when (clip) {
    "extraSmall" -> RoundedCornerShape(4.dp)
    "small" -> RoundedCornerShape(8.dp)
    "medium" -> RoundedCornerShape(12.dp)
    "large" -> RoundedCornerShape(16.dp)
    "full" -> CircleShape
    else -> null
}

private fun enterTransition(animation: EnterAnimation?, itemIndex: Int) = run {
    val delay = (animation?.delayMs ?: 0) + itemIndex * (animation?.staggerMs ?: 0)
    val spec = tween<Float>(durationMillis = animation?.durationMs ?: 280, delayMillis = delay)
    when (animation?.type) {
        "slideUp" -> fadeIn(spec) + slideInVertically(animationSpec = tween(animation.durationMs, delay)) { it / 5 }
        "slideLeft" -> fadeIn(spec) + slideInHorizontally(animationSpec = tween(animation.durationMs, delay)) { it / 4 }
        "scale" -> fadeIn(spec) + scaleIn(initialScale = 0.92f, animationSpec = tween(animation.durationMs, delay))
        "none", null -> fadeIn(tween(0))
        else -> fadeIn(spec)
    }
}

@Composable
private fun typeStyle(name: String?): TextStyle = when (name) {
    "displayLarge" -> MaterialTheme.typography.displayLarge
    "headlineMedium" -> MaterialTheme.typography.headlineMedium
    "titleLarge" -> MaterialTheme.typography.titleLarge
    "titleMedium" -> MaterialTheme.typography.titleMedium
    "bodyMedium" -> MaterialTheme.typography.bodyMedium
    "labelLarge" -> MaterialTheme.typography.labelLarge
    "labelMedium" -> MaterialTheme.typography.labelMedium
    else -> MaterialTheme.typography.bodyLarge
}

@Composable
private fun colorToken(name: String?): Color {
    val scheme = MaterialTheme.colorScheme
    return when (name) {
        "primary" -> scheme.primary
        "onPrimary" -> scheme.onPrimary
        "primaryContainer" -> scheme.primaryContainer
        "onPrimaryContainer" -> scheme.onPrimaryContainer
        "secondary" -> scheme.secondary
        "onSurfaceVariant" -> scheme.onSurfaceVariant
        "error" -> scheme.error
        "tertiary" -> scheme.tertiary
        else -> scheme.onSurface
    }
}

private fun iconOf(name: String): ImageVector = when (name) {
    "home" -> Icons.Filled.Home
    "search" -> Icons.Filled.Search
    "cart" -> Icons.Filled.ShoppingCart
    "person" -> Icons.Filled.Person
    "add" -> Icons.Filled.Add
    "favorite" -> Icons.Filled.Favorite
    "settings" -> Icons.Filled.Settings
    "back" -> Icons.AutoMirrored.Filled.ArrowBack
    "menu" -> Icons.Filled.Menu
    "notifications" -> Icons.Filled.Notifications
    "tune" -> Icons.Filled.Tune
    else -> Icons.Filled.Star
}

private fun seedScheme(seed: String, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(if (seed == "teal") 0xFF4CDADA else if (seed == "blue") 0xFFA9C7FF else if (seed == "orange") 0xFFFFB870 else 0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
    )
} else {
    lightColorScheme(
        primary = Color(if (seed == "teal") 0xFF006A6A else if (seed == "blue") 0xFF005DB7 else if (seed == "orange") 0xFF8B5000 else 0xFF6750A4),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
    )
}

internal fun parseHex(raw: String): Color {
    val hex = raw.removePrefix("#")
    val value = hex.toLongOrNull(16) ?: return Color(0xFF6750A4)
    return when (hex.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> Color(0xFF6750A4)
    }
}
