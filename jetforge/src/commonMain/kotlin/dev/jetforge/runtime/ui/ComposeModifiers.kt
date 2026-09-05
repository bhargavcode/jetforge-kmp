package dev.jetforge.runtime.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.jetforge.runtime.model.ModifierSpec
import dev.jetforge.runtime.model.UiNode

internal fun clipShape(clip: String?) = when (clip) {
    "extraSmall" -> RoundedCornerShape(4.dp)
    "small" -> RoundedCornerShape(8.dp)
    "medium" -> RoundedCornerShape(12.dp)
    "large" -> RoundedCornerShape(16.dp)
    "full" -> CircleShape
    else -> null
}

private fun ModifierSpec.fraction(value: Float?, fallback: Float = 1f): Float {
    val raw = value ?: fallback
    return raw.coerceIn(0f, 1f)
}

internal fun UiNode.studioModifier(): Modifier {
    var modifier: Modifier = Modifier
    val m = modifiers
    val fillSize = m.fillMaxSize || (m.widthMode == "fill" && m.heightMode == "fill")
    if (fillSize) {
        modifier = modifier.fillMaxSize(m.fraction(m.fillMaxSizeFraction))
    } else {
        if (m.fillMaxWidth || m.widthMode == "fill") {
            modifier = modifier.fillMaxWidth(m.fraction(m.fillMaxWidthFraction ?: m.fillMaxSizeFraction))
        }
        if (m.fillMaxHeight || m.heightMode == "fill") {
            modifier = modifier.fillMaxHeight(m.fraction(m.fillMaxHeightFraction ?: m.fillMaxSizeFraction))
        }
    }
    m.sizeDp?.let { modifier = modifier.size(it.dp) }
    m.widthDp?.let { modifier = modifier.width(it.dp) }
    m.heightDp?.let { modifier = modifier.height(it.dp) }
    if (m.minWidthDp != null || m.maxWidthDp != null) {
        modifier = modifier.widthIn(
            min = (m.minWidthDp ?: 0).dp,
            max = m.maxWidthDp?.dp ?: Dp.Unspecified,
        )
    }
    if (m.minHeightDp != null || m.maxHeightDp != null) {
        modifier = modifier.heightIn(
            min = (m.minHeightDp ?: 0).dp,
            max = m.maxHeightDp?.dp ?: Dp.Unspecified,
        )
    }
    if (m.minWidthDp != null || m.minHeightDp != null || m.maxWidthDp != null || m.maxHeightDp != null) {
        modifier = modifier.sizeIn(
            minWidth = (m.minWidthDp ?: 0).dp,
            minHeight = (m.minHeightDp ?: 0).dp,
            maxWidth = m.maxWidthDp?.dp ?: Dp.Unspecified,
            maxHeight = m.maxHeightDp?.dp ?: Dp.Unspecified,
        )
    }
    if (m.defaultMinWidthDp != null || m.defaultMinHeightDp != null) {
        modifier = modifier.defaultMinSize(
            minWidth = (m.defaultMinWidthDp ?: 0).dp,
            minHeight = (m.defaultMinHeightDp ?: 0).dp,
        )
    }
    m.requiredSizeDp?.let { modifier = modifier.requiredSize(it.dp) }
    m.requiredWidthDp?.let { modifier = modifier.requiredWidth(it.dp) }
    m.requiredHeightDp?.let { modifier = modifier.requiredHeight(it.dp) }
    if (m.wrapContentSize) {
        modifier = modifier.wrapContentSize(
            align = twoDimensionalAlignment(m.wrapContentSizeAlign),
            unbounded = m.wrapContentSizeUnbounded,
        )
    }
    if (m.wrapContentWidth) {
        modifier = modifier.wrapContentWidth(
            align = horizontalAlignmentOf(m.wrapContentWidthAlign) ?: Alignment.CenterHorizontally,
            unbounded = m.wrapContentWidthUnbounded,
        )
    }
    if (m.wrapContentHeight) {
        modifier = modifier.wrapContentHeight(
            align = verticalAlignmentOf(m.wrapContentHeightAlign) ?: Alignment.CenterVertically,
            unbounded = m.wrapContentHeightUnbounded,
        )
    }
    m.aspectRatio?.takeIf { it > 0f }?.let {
        modifier = modifier.aspectRatio(it, m.aspectRatioMatchHeightFirst)
    }
    m.margin?.let { pad ->
        modifier = if (pad.all != null) modifier.padding(pad.all.dp)
        else modifier.padding(
            start = (pad.start ?: 0).dp,
            top = (pad.top ?: 0).dp,
            end = (pad.end ?: 0).dp,
            bottom = (pad.bottom ?: 0).dp,
        )
    }
    m.padding?.let { pad ->
        modifier = if (pad.all != null) modifier.padding(pad.all.dp)
        else modifier.padding(
            start = (pad.start ?: 0).dp,
            top = (pad.top ?: 0).dp,
            end = (pad.end ?: 0).dp,
            bottom = (pad.bottom ?: 0).dp,
        )
    }
    if (m.offsetXDp != null || m.offsetYDp != null) {
        modifier = modifier.offset(x = (m.offsetXDp ?: 0).dp, y = (m.offsetYDp ?: 0).dp)
    }
    clipShape(m.clip)?.let { modifier = modifier.clip(it) }
    if (m.clipToBounds) modifier = modifier.clipToBounds()
    val elevation = m.elevationDp ?: m.graphicsShadowElevation?.toInt()
    if (elevation != null && elevation > 0) {
        modifier = modifier.shadow(
            elevation = elevation.dp,
            shape = clipShape(m.clip) ?: RoundedCornerShape(0.dp),
            clip = m.graphicsClip || m.clipToBounds,
        )
    }
    val hasGraphics = m.alpha != null ||
        m.graphicsScaleX != null ||
        m.graphicsScaleY != null ||
        m.graphicsTranslationX != null ||
        m.graphicsTranslationY != null ||
        m.graphicsRotationX != null ||
        m.graphicsRotationY != null ||
        m.graphicsRotationZ != null ||
        m.rotationDeg != null ||
        m.graphicsShadowElevation != null ||
        m.graphicsClip
    if (hasGraphics) {
        modifier = modifier.graphicsLayer(
            alpha = m.alpha ?: 1f,
            scaleX = m.graphicsScaleX ?: 1f,
            scaleY = m.graphicsScaleY ?: 1f,
            translationX = m.graphicsTranslationX ?: 0f,
            translationY = m.graphicsTranslationY ?: 0f,
            shadowElevation = m.graphicsShadowElevation ?: 0f,
            rotationX = m.graphicsRotationX ?: 0f,
            rotationY = m.graphicsRotationY ?: 0f,
            rotationZ = m.graphicsRotationZ ?: m.rotationDeg ?: 0f,
            clip = m.graphicsClip,
        )
    }
    m.zIndex?.let { modifier = modifier.zIndex(it) }
    if (!m.semanticsLabel.isNullOrBlank() || m.semanticsMergeDescendants) {
        modifier = modifier.semantics(mergeDescendants = m.semanticsMergeDescendants) {
            m.semanticsLabel?.takeIf { it.isNotBlank() }?.let { contentDescription = it }
        }
    }
    return modifier
}

@Composable
internal fun UiNode.studioScrollModifier(): Modifier {
    val axis = when {
        modifiers.verticalScroll || modifiers.scrollAxis == "vertical" -> "vertical"
        modifiers.horizontalScroll || modifiers.scrollAxis == "horizontal" -> "horizontal"
        else -> null
    } ?: return Modifier
    val enabled = modifiers.scrollEnabled ?: true
    val reverse = modifiers.reverseScrolling
    return if (axis == "vertical") {
        Modifier.verticalScroll(rememberScrollState(), enabled = enabled, reverseScrolling = reverse)
    } else {
        Modifier.horizontalScroll(rememberScrollState(), enabled = enabled, reverseScrolling = reverse)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun UiNode.studioInteractionModifier(onClick: () -> Unit = {}): Modifier {
    val m = modifiers
    var modifier: Modifier = Modifier
    val enabled = m.interactionEnabled ?: true
    if (m.combinedClickable) {
        modifier = modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = m.onClickLabel,
            onClick = onClick,
            onLongClick = onClick,
            onDoubleClick = onClick,
        )
    }
    if (m.selectable) {
        modifier = modifier.selectable(
            selected = m.selected,
            enabled = enabled,
            onClick = onClick,
        )
    }
    if (m.toggleable) {
        modifier = modifier.toggleable(
            value = m.toggled,
            enabled = enabled,
            onValueChange = { onClick() },
        )
    }
    return modifier
}

internal fun ColumnScope.columnChildScopeModifier(child: UiNode): Modifier {
    var modifier: Modifier = constraintFlowModifier(child.constraints)
    child.modifiers.weight?.let {
        modifier = modifier.weight(it, child.modifiers.weightFill ?: true)
    }
    child.modifiers.align?.let { token ->
        horizontalAlignmentOf(token)?.let { modifier = modifier.align(it) }
    }
    return modifier
}

internal fun RowScope.rowChildScopeModifier(child: UiNode): Modifier {
    var modifier: Modifier = constraintFlowModifier(child.constraints)
    child.modifiers.weight?.let {
        modifier = modifier.weight(it, child.modifiers.weightFill ?: true)
    }
    child.modifiers.align?.let { token ->
        verticalAlignmentOf(token)?.let { modifier = modifier.align(it) }
    }
    if (child.modifiers.alignByBaseline) {
        modifier = modifier.alignByBaseline()
    }
    when (normalizeToken(child.modifiers.alignBy)) {
        "firstbaseline" -> modifier = modifier.alignBy(FirstBaseline)
        "lastbaseline" -> modifier = modifier.alignBy(LastBaseline)
        "centervertically" -> modifier = modifier.alignBy { it.measuredHeight / 2 }
        "top" -> modifier = modifier.alignBy { 0 }
        "bottom" -> modifier = modifier.alignBy { it.measuredHeight }
    }
    return modifier
}
