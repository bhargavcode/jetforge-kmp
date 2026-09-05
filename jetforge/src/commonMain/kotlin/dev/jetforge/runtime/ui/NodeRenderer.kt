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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
    val modifier = node.studioModifier()
        .then(node.surfaceModifier())
        .then(node.borderModifier())
        .then(clickModifier)
    fun nativeTap() {
        if (runTap && !hasExtra) fireAction(session, coroutine, tap, scope)
    }

    when (node.type) {
        "Scaffold" -> Box(modifier.fillMaxSize()) { StudioScaffold(node, scope) }
        "Column" -> StudioColumn(node, scope, modifier)
        "Row" -> StudioRow(node, scope, modifier)
        "Box" -> ConstraintFallbackBox(node, modifier) { child, index ->
            RenderNode(child, scope, index)
        }
        "LazyColumn" -> StudioList(node, scope, modifier)
        "Card" -> Card(
            modifier = modifier,
            shape = clipShape(node.modifiers.clip) ?: RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = node.surfaceColor() ?: MaterialTheme.colorScheme.surface,
            ),
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
        ) {
            Text(
                resolve(node, "label", scope) ?: node.props.prop("label") ?: "Action",
                color = colorToken(node.props.prop("color") ?: "primary"),
            )
        }
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
            val shape = clipShape(node.modifiers.clip) ?: RoundedCornerShape(8.dp)
            val heightMode = node.modifiers.heightMode
                ?: when {
                    node.modifiers.fillMaxHeight || node.modifiers.fillMaxSize -> "fill"
                    node.modifiers.heightDp != null -> "fixed"
                    else -> "wrap"
                }
            val preserveAspect = heightMode == "wrap" && node.modifiers.heightDp == null
            val imageModifier = if (preserveAspect) {
                // Do not force 72.dp — that crops card screenshots down to a button-sized strip.
                modifier.clip(shape)
            } else if (node.modifiers.heightDp == null && heightMode != "fill") {
                modifier.height(72.dp).clip(shape)
            } else {
                modifier.clip(shape)
            }
            RemoteImage(
                url = resolveMediaUrl(resolve(node, "url", scope).orEmpty(), JetForge.config.baseUrl),
                accent = accent,
                modifier = imageModifier,
                contentScale = contentScaleOf(node.props.prop("contentScale")),
                preserveAspectRatio = preserveAspect,
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

@Composable
private fun StudioColumn(node: UiNode, scope: BindingScope, modifier: Modifier) {
    Column(modifier, columnArrangement(node), columnAlignment(node)) {
        node.children.forEachIndexed { index, child ->
            if (
                (child.constraints?.bottomToBottomOf == "parent" || child.constraints?.vertical == "bottom") &&
                index == node.children.lastIndex
            ) {
                Spacer(Modifier.weight(1f))
            }
            Box(
                constraintFlowModifier(child.constraints)
                    .then(child.modifiers.weight?.let { Modifier.weight(it) } ?: Modifier),
            ) { RenderNode(child, scope, index) }
        }
    }
}

@Composable
private fun StudioRow(node: UiNode, scope: BindingScope, modifier: Modifier) {
    Row(modifier, rowArrangement(node), rowAlignment(node)) {
        node.children.forEachIndexed { index, child ->
            Box(
                constraintFlowModifier(child.constraints)
                    .then(child.modifiers.weight?.let { Modifier.weight(it) } ?: Modifier),
            ) { RenderNode(child, scope, index) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioScaffold(node: UiNode, scope: BindingScope) {
    val top = node.children.find { it.slot == "topBar" || it.type == "TopAppBar" }
    val bottom = node.children.find { it.slot == "bottomBar" || it.type == "NavigationBar" }
    val rail = node.children.find {
        it.slot == "rail" || it.type == "NavigationRail" || it.type == "NavigationDrawer"
    }
    val fab = node.children.find { it.slot == "fab" || it.type == "FAB" }
    val content = node.children.find { it.slot == "content" }
        ?: node.copy(children = node.children.filter { it != top && it != bottom && it != fab && it != rail })

    // Empty chrome must not eat vertical/horizontal space — content gets the full layout.
    if (top == null && bottom == null && rail == null) {
        Box(Modifier.fillMaxSize()) {
            RenderNode(content, scope, 0)
            if (fab != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    RenderNode(fab, scope, 0)
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = { if (top != null) RenderNode(top, scope, 0) },
        bottomBar = { if (bottom != null) RenderNode(bottom, scope, 0) },
        floatingActionButton = { if (fab != null) RenderNode(fab, scope, 0) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (rail != null) {
                RenderNode(rail, scope, 0)
            }
            Box(Modifier.fillMaxSize().weight(1f)) {
                RenderNode(content, scope, 0)
            }
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
        val resolved = scope.resolvePath(binding)
        // Match designer resolveProp: only use the binding when it resolves.
        if (resolved != null) return resolved.asDisplayString()
    }
    return node.props.prop(key)
}

private fun UiNode.studioModifier(): Modifier {
    var modifier: Modifier = Modifier
    if (modifiers.fillMaxSize) {
        modifier = modifier.fillMaxSize()
    } else {
        val widthMode = modifiers.widthMode
        val heightMode = modifiers.heightMode
        if (modifiers.fillMaxWidth || widthMode == "fill") modifier = modifier.fillMaxWidth()
        if (modifiers.fillMaxHeight || heightMode == "fill") modifier = modifier.fillMaxHeight()
    }
    modifiers.widthDp?.let { modifier = modifier.width(it.dp) }
    modifiers.heightDp?.let { modifier = modifier.height(it.dp) }
    modifiers.aspectRatio?.takeIf { it > 0f }?.let { modifier = modifier.aspectRatio(it) }
    modifiers.margin?.let { pad ->
        modifier = if (pad.all != null) modifier.padding(pad.all.dp)
        else modifier.padding(
            start = (pad.start ?: 0).dp,
            top = (pad.top ?: 0).dp,
            end = (pad.end ?: 0).dp,
            bottom = (pad.bottom ?: 0).dp,
        )
    }
    modifiers.padding?.let { pad ->
        modifier = if (pad.all != null) modifier.padding(pad.all.dp)
        else modifier.padding(
            start = (pad.start ?: 0).dp,
            top = (pad.top ?: 0).dp,
            end = (pad.end ?: 0).dp,
            bottom = (pad.bottom ?: 0).dp,
        )
    }
    if (modifiers.offsetXDp != null || modifiers.offsetYDp != null) {
        modifier = modifier.offset(
            x = (modifiers.offsetXDp ?: 0).dp,
            y = (modifiers.offsetYDp ?: 0).dp,
        )
    }
    clipShape(modifiers.clip)?.let { modifier = modifier.clip(it) }
    return modifier
}

@Composable
private fun UiNode.surfaceModifier(): Modifier {
    val color = surfaceColor() ?: return Modifier
    val shape = clipShape(modifiers.clip)
    return if (shape != null) Modifier.background(color, shape) else Modifier.background(color)
}

@Composable
private fun UiNode.surfaceColor(): Color? {
    modifiers.backgroundHex?.takeIf { it.isNotBlank() }?.let { return parseHex(it) }
    modifiers.backgroundToken?.takeIf { it.isNotBlank() && it != "none" }?.let { return colorToken(it) }
    return null
}

@Composable
private fun UiNode.borderModifier(): Modifier {
    val width = modifiers.borderWidthDp ?: return Modifier
    val shape = clipShape(modifiers.clip) ?: RoundedCornerShape(0.dp)
    return Modifier.border(width.dp, colorToken(modifiers.borderToken ?: "outline"), shape)
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
        "onSecondary" -> scheme.onSecondary
        "secondaryContainer" -> scheme.secondaryContainer
        "onSecondaryContainer" -> scheme.onSecondaryContainer
        "tertiary" -> scheme.tertiary
        "onTertiary" -> scheme.onTertiary
        "surface" -> scheme.surface
        "onSurface" -> scheme.onSurface
        "onSurfaceVariant" -> scheme.onSurfaceVariant
        "surfaceContainer" -> scheme.surfaceContainer
        "surfaceContainerHigh" -> scheme.surfaceContainerHigh
        "surfaceContainerLowest" -> scheme.surfaceContainerLowest
        "outline" -> scheme.outline
        "outlineVariant" -> scheme.outlineVariant
        "error" -> scheme.error
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

private fun seedScheme(seed: String, dark: Boolean) = when {
    dark && seed == "teal" -> darkColorScheme(
        primary = Color(0xFF4CDADA),
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF004F4F),
        onPrimaryContainer = Color(0xFF6FF7F6),
        secondary = Color(0xFFB0CCCC),
        surface = Color(0xFF0E1514),
        onSurface = Color(0xFFDDE4E3),
        onSurfaceVariant = Color(0xFFBEC9C8),
        surfaceContainer = Color(0xFF1A2120),
        surfaceContainerHigh = Color(0xFF242B2A),
        surfaceContainerLowest = Color(0xFF090F0F),
        outline = Color(0xFF889392),
        outlineVariant = Color(0xFF3F4948),
        error = Color(0xFFFFB4AB),
    )
    dark && seed == "blue" -> darkColorScheme(
        primary = Color(0xFFA9C7FF),
        onPrimary = Color(0xFF00315C),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E9),
        onSurfaceVariant = Color(0xFFC3C6CF),
        surfaceContainer = Color(0xFF1D2024),
        outline = Color(0xFF8D9199),
    )
    dark && seed == "orange" -> darkColorScheme(
        primary = Color(0xFFFFB870),
        onPrimary = Color(0xFF4A2800),
        surface = Color(0xFF18120D),
        onSurface = Color(0xFFEDE0D8),
        onSurfaceVariant = Color(0xFFD5C3B5),
        surfaceContainer = Color(0xFF251E19),
        outline = Color(0xFF9E8E81),
    )
    dark -> darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerLowest = Color(0xFF0F0D13),
        outline = Color(0xFF948F99),
        outlineVariant = Color(0xFF49454F),
        error = Color(0xFFFFB4AB),
    )
    seed == "teal" -> lightColorScheme(
        primary = Color(0xFF006A6A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6FF7F6),
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6363),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFF4B607C),
        surface = Color(0xFFF4FBFA),
        onSurface = Color(0xFF161D1D),
        onSurfaceVariant = Color(0xFF3F4948),
        surfaceContainer = Color(0xFFE9EFEE),
        surfaceContainerHigh = Color(0xFFE3E9E8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        outline = Color(0xFF6F7978),
        outlineVariant = Color(0xFFBEC9C8),
        error = Color(0xFFBA1A1A),
    )
    seed == "blue" -> lightColorScheme(
        primary = Color(0xFF005DB7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E3FF),
        onPrimaryContainer = Color(0xFF001B3D),
        secondary = Color(0xFF555F71),
        surface = Color(0xFFF9F9FF),
        onSurface = Color(0xFF1A1B20),
        onSurfaceVariant = Color(0xFF44474E),
        surfaceContainer = Color(0xFFEEEDF4),
        outline = Color(0xFF74777F),
    )
    seed == "orange" -> lightColorScheme(
        primary = Color(0xFF8B5000),
        onPrimary = Color(0xFFFFFFFF),
        surface = Color(0xFFFFF8F5),
        onSurface = Color(0xFF221A15),
        onSurfaceVariant = Color(0xFF51443A),
        surfaceContainer = Color(0xFFF7EDE7),
        outline = Color(0xFF837468),
    )
    else -> lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFF7D5260),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        error = Color(0xFFB3261E),
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
