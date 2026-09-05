package dev.jetforge.runtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.jetforge.runtime.bind.int
import dev.jetforge.runtime.bind.prop
import dev.jetforge.runtime.model.ConstraintSpec
import dev.jetforge.runtime.model.PaddingSpec
import dev.jetforge.runtime.model.UiNode

private const val PARENT = "parent"

internal fun hasPeerAnchors(constraints: ConstraintSpec?): Boolean {
    if (constraints == null) return false
    return constraints.startToStartOf != null ||
        constraints.startToEndOf != null ||
        constraints.endToStartOf != null ||
        constraints.endToEndOf != null ||
        constraints.topToTopOf != null ||
        constraints.topToBottomOf != null ||
        constraints.bottomToTopOf != null ||
        constraints.bottomToBottomOf != null ||
        constraints.horizontalCenterOf != null ||
        constraints.verticalCenterOf != null
}

internal fun normalizeToken(value: String?): String =
    value.orEmpty().substringAfterLast('.').replace(" ", "").lowercase()

internal fun columnArrangement(node: UiNode): Arrangement.Vertical {
    val gap = node.props.int("spacedBy", 8).dp
    val key = normalizeToken(
        node.props.prop("verticalArrangement") ?: node.props.prop("arrangement") ?: "Top",
    )
    val spacedAlign = verticalAlignmentOf(node.props.prop("spacedByAlignment"))
    return when (key) {
        "center" -> Arrangement.spacedBy(gap, Alignment.CenterVertically)
        "bottom", "end" -> Arrangement.spacedBy(gap, Alignment.Bottom)
        "spacebetween" -> Arrangement.SpaceBetween
        "spaceevenly" -> Arrangement.SpaceEvenly
        "spacearound" -> Arrangement.SpaceAround
        else -> if (spacedAlign != null) Arrangement.spacedBy(gap, spacedAlign) else Arrangement.spacedBy(gap)
    }
}

internal fun rowArrangement(node: UiNode): Arrangement.Horizontal {
    val gap = node.props.int("spacedBy", 8).dp
    val key = normalizeToken(
        node.props.prop("horizontalArrangement") ?: node.props.prop("arrangement") ?: "Start",
    )
    val spacedAlign = horizontalAlignmentOf(node.props.prop("spacedByAlignment"))
    return when (key) {
        "center" -> Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
        "end", "bottom" -> Arrangement.spacedBy(gap, Alignment.End)
        "spacebetween" -> Arrangement.SpaceBetween
        "spaceevenly" -> Arrangement.SpaceEvenly
        "spacearound" -> Arrangement.SpaceAround
        else -> if (spacedAlign != null) Arrangement.spacedBy(gap, spacedAlign) else Arrangement.spacedBy(gap)
    }
}

internal fun columnAlignment(node: UiNode): Alignment.Horizontal =
    horizontalAlignmentOf(node.props.prop("horizontalAlignment") ?: node.props.prop("alignment"))
        ?: Alignment.Start

internal fun rowAlignment(node: UiNode): Alignment.Vertical =
    verticalAlignmentOf(node.props.prop("verticalAlignment") ?: node.props.prop("alignment"))
        ?: Alignment.Top

internal fun boxAlignment(node: UiNode): Alignment =
    twoDimensionalAlignment(node.props.prop("contentAlignment") ?: node.props.prop("alignment"))

internal fun horizontalAlignmentOf(value: String?): Alignment.Horizontal? = when (normalizeToken(value)) {
    "end", "topend", "centerend", "bottomend" -> Alignment.End
    "center", "centerhorizontally", "topcenter", "bottomcenter" -> Alignment.CenterHorizontally
    "start", "top", "topstart", "centerstart", "bottomstart" -> Alignment.Start
    else -> null
}

internal fun verticalAlignmentOf(value: String?): Alignment.Vertical? = when (normalizeToken(value)) {
    "bottom", "end", "bottomstart", "bottomcenter", "bottomend" -> Alignment.Bottom
    "center", "centervertically", "centerstart", "centerend" -> Alignment.CenterVertically
    "top", "start", "topstart", "topcenter", "topend" -> Alignment.Top
    else -> null
}

internal fun twoDimensionalAlignment(value: String?): Alignment = when (normalizeToken(value)) {
    "topcenter" -> Alignment.TopCenter
    "topend" -> Alignment.TopEnd
    "centerstart" -> Alignment.CenterStart
    "center", "centerhorizontally", "centervertically" -> Alignment.Center
    "centerend" -> Alignment.CenterEnd
    "bottomstart" -> Alignment.BottomStart
    "bottomcenter" -> Alignment.BottomCenter
    "bottomend" -> Alignment.BottomEnd
    "end" -> Alignment.TopEnd
    "bottom" -> Alignment.BottomStart
    else -> Alignment.TopStart
}

/**
 * Parent alignment / stretch for flow Column children when ConstraintLayout
 * is unavailable. Peer anchors are handled by [PeerConstraintLayout].
 */
internal fun ColumnScope.constraintFlowModifier(constraints: ConstraintSpec?): Modifier {
    if (constraints == null) return Modifier
    var result = marginModifier(constraints.margin)
    result = when {
        constraints.startToStartOf == PARENT && constraints.endToEndOf == PARENT ||
            constraints.horizontal == "stretch" -> result.fillMaxWidth()
        constraints.endToEndOf == PARENT || constraints.horizontal == "end" -> result.align(Alignment.End)
        constraints.horizontal == "center" || constraints.horizontalCenterOf != null ->
            result.align(Alignment.CenterHorizontally)
        else -> result
    }
    return result
}

internal fun RowScope.constraintFlowModifier(constraints: ConstraintSpec?): Modifier {
    if (constraints == null) return Modifier
    var result = marginModifier(constraints.margin)
    result = when {
        constraints.bottomToBottomOf == PARENT || constraints.vertical == "bottom" ->
            result.align(Alignment.Bottom)
        constraints.vertical == "center" || constraints.verticalCenterOf != null ->
            result.align(Alignment.CenterVertically)
        else -> result
    }
    return result
}

internal fun marginModifier(padding: PaddingSpec?): Modifier {
    if (padding == null) return Modifier
    return if (padding.all != null) Modifier.padding(padding.all.dp) else Modifier.padding(
        start = (padding.start ?: 0).dp,
        top = (padding.top ?: 0).dp,
        end = (padding.end ?: 0).dp,
        bottom = (padding.bottom ?: 0).dp,
    )
}

@Composable
internal fun ConstraintFallbackBox(
    node: UiNode,
    modifier: Modifier,
    render: @Composable (UiNode, Int) -> Unit,
) {
    if (node.children.any { hasPeerAnchors(it.constraints) }) {
        PeerConstraintLayout(node, modifier, render)
        return
    }
    Box(modifier, contentAlignment = boxAlignment(node)) {
        node.children.forEachIndexed { index, child ->
            Box(
                Modifier
                    .then(marginModifier(child.constraints?.margin))
                    .align(
                        child.modifiers.align?.let { twoDimensionalAlignment(it) }
                            ?: boxChildAlignment(child.constraints),
                    ),
            ) { render(child, index) }
        }
    }
}

/**
 * Lightweight ConstraintLayout stand-in for CMP targets that lack
 * constraintlayout-compose.
 *
 * Compose layout contract:
 * - [measure] each child at most once
 * - never query intrinsics (Lazy lists / SubcomposeLayout crash on them)
 */
@Composable
internal fun PeerConstraintLayout(
    node: UiNode,
    modifier: Modifier,
    render: @Composable (UiNode, Int) -> Unit,
) {
    Layout(
        content = {
            node.children.forEachIndexed { index, child -> render(child, index) }
        },
        modifier = modifier.fillMaxSize(),
    ) { measurables, incoming ->
        val width = incoming.maxWidth.coerceAtLeast(0)
        val height = if (incoming.hasBoundedHeight) {
            incoming.maxHeight.coerceAtLeast(0)
        } else {
            incoming.minHeight.coerceAtLeast(1)
        }
        val density = this
        fun marginPx(value: Int?) = with(density) { (value ?: 0).dp.roundToPx() }

        data class Frame(var x: Int = 0, var y: Int = 0, var w: Int = 0, var h: Int = 0)

        val frames = node.children.associate { it.id to Frame() }.toMutableMap()
        frames[PARENT] = Frame(0, 0, width, height)
        fun peer(id: String?): Frame = frames[id ?: PARENT] ?: frames.getValue(PARENT)

        fun isStretchH(c: ConstraintSpec?): Boolean {
            if (c == null) return false
            return c.horizontal == "stretch" || (c.startToStartOf != null && c.endToEndOf != null)
        }

        fun isStretchV(c: ConstraintSpec?): Boolean = c?.vertical == "stretch"

        fun siblingIds(c: ConstraintSpec?): List<String> {
            if (c == null) return emptyList()
            return listOfNotNull(
                c.startToStartOf, c.startToEndOf, c.endToStartOf, c.endToEndOf,
                c.topToTopOf, c.topToBottomOf, c.bottomToTopOf, c.bottomToBottomOf,
                c.horizontalCenterOf, c.verticalCenterOf,
            ).filter { it != PARENT }
        }

        fun parentStretchSize(child: UiNode): Pair<Int?, Int?> {
            val c = child.constraints ?: return null to null
            val margin = c.margin
            val mStart = marginPx(margin?.start ?: margin?.all)
            val mTop = marginPx(margin?.top ?: margin?.all)
            val mEnd = marginPx(margin?.end ?: margin?.all)
            val mBottom = marginPx(margin?.bottom ?: margin?.all)
            val w = if (c.horizontal == "stretch" || (c.startToStartOf == PARENT && c.endToEndOf == PARENT)) {
                (width - mStart - mEnd).coerceAtLeast(0)
            } else {
                null
            }
            val h = if (c.vertical == "stretch") {
                (height - mTop - mBottom).coerceAtLeast(0)
            } else {
                null
            }
            return w to h
        }

        val measured = BooleanArray(measurables.size)
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val idToIndex = node.children.mapIndexed { index, child -> child.id to index }.toMap()

        fun measureChild(index: Int, child: UiNode, fixedW: Int?, fixedH: Int?) {
            if (measured[index]) return
            val maxW = (fixedW ?: width).coerceAtLeast(0)
            val maxH = (fixedH ?: height).coerceAtLeast(0)
            val placeable = measurables[index].measure(
                Constraints(
                    minWidth = if (fixedW != null) maxW else 0,
                    maxWidth = maxW,
                    minHeight = if (fixedH != null) maxH else 0,
                    maxHeight = maxH,
                ),
            )
            placeables[index] = placeable
            measured[index] = true
            frames.getValue(child.id).apply {
                w = placeable.width
                h = placeable.height
            }
        }

        fun resolvePosition(child: UiNode): Pair<Int?, Int?> {
            val c = child.constraints
            val frame = frames.getValue(child.id)
            val margin = c?.margin
            val mStart = marginPx(margin?.start ?: margin?.all)
            val mTop = marginPx(margin?.top ?: margin?.all)
            val mEnd = marginPx(margin?.end ?: margin?.all)
            val mBottom = marginPx(margin?.bottom ?: margin?.all)

            if (c == null) return mStart to mTop

            var left: Int? = null
            var right: Int? = null
            var top: Int? = null
            var bottom: Int? = null

            when {
                c.startToStartOf != null -> left = peer(c.startToStartOf).x + mStart
                c.startToEndOf != null -> left = peer(c.startToEndOf).x + peer(c.startToEndOf).w + mStart
            }
            when {
                c.endToEndOf != null -> right = peer(c.endToEndOf).x + peer(c.endToEndOf).w - mEnd
                c.endToStartOf != null -> right = peer(c.endToStartOf).x - mEnd
            }
            when {
                c.topToTopOf != null -> top = peer(c.topToTopOf).y + mTop
                c.topToBottomOf != null -> top = peer(c.topToBottomOf).y + peer(c.topToBottomOf).h + mTop
            }
            when {
                c.bottomToBottomOf != null -> bottom = peer(c.bottomToBottomOf).y + peer(c.bottomToBottomOf).h - mBottom
                c.bottomToTopOf != null -> bottom = peer(c.bottomToTopOf).y - mBottom
            }

            if (c.horizontalCenterOf != null) {
                val p = peer(c.horizontalCenterOf)
                left = p.x + (p.w - frame.w) / 2
            } else if (c.horizontal == "center" && left == null && right == null) {
                left = (width - frame.w) / 2
            } else if (c.horizontal == "end" && right == null && left == null) {
                right = width - mEnd
            } else if (c.horizontal == "stretch" || (c.startToStartOf == PARENT && c.endToEndOf == PARENT)) {
                left = mStart
                right = width - mEnd
            }

            if (c.verticalCenterOf != null) {
                val p = peer(c.verticalCenterOf)
                top = p.y + (p.h - frame.h) / 2
            } else if (c.vertical == "center" && top == null && bottom == null) {
                top = (height - frame.h) / 2
            } else if (c.vertical == "bottom" && bottom == null && top == null) {
                bottom = height - mBottom
            } else if (c.vertical == "stretch") {
                top = mTop
                bottom = height - mBottom
            }

            val x = when {
                left != null -> left
                right != null -> right - frame.w
                else -> mStart
            }
            val y = when {
                top != null -> top
                bottom != null -> bottom - frame.h
                else -> mTop
            }
            return x to y
        }

        fun stretchSpan(child: UiNode): Pair<Int?, Int?> {
            val c = child.constraints ?: return null to null
            val margin = c.margin
            val mStart = marginPx(margin?.start ?: margin?.all)
            val mTop = marginPx(margin?.top ?: margin?.all)
            val mEnd = marginPx(margin?.end ?: margin?.all)
            val mBottom = marginPx(margin?.bottom ?: margin?.all)

            var left: Int? = null
            var right: Int? = null
            var top: Int? = null
            var bottom: Int? = null

            when {
                c.startToStartOf != null -> left = peer(c.startToStartOf).x + mStart
                c.startToEndOf != null -> left = peer(c.startToEndOf).x + peer(c.startToEndOf).w + mStart
            }
            when {
                c.endToEndOf != null -> right = peer(c.endToEndOf).x + peer(c.endToEndOf).w - mEnd
                c.endToStartOf != null -> right = peer(c.endToStartOf).x - mEnd
            }
            when {
                c.topToTopOf != null -> top = peer(c.topToTopOf).y + mTop
                c.topToBottomOf != null -> top = peer(c.topToBottomOf).y + peer(c.topToBottomOf).h + mTop
            }
            when {
                c.bottomToBottomOf != null -> bottom = peer(c.bottomToBottomOf).y + peer(c.bottomToBottomOf).h - mBottom
                c.bottomToTopOf != null -> bottom = peer(c.bottomToTopOf).y - mBottom
            }

            if (c.horizontal == "stretch" || (c.startToStartOf == PARENT && c.endToEndOf == PARENT)) {
                left = mStart
                right = width - mEnd
            }
            if (c.vertical == "stretch") {
                top = mTop
                bottom = height - mBottom
            }

            val w = if (isStretchH(c) && left != null && right != null) (right - left).coerceAtLeast(0) else null
            val h = if (isStretchV(c) && top != null && bottom != null) (bottom - top).coerceAtLeast(0) else null
            return w to h
        }

        fun applyPosition(child: UiNode) {
            val frame = frames.getValue(child.id)
            val (x, y) = resolvePosition(child)
            frame.x = x ?: 0
            frame.y = y ?: 0
        }

        // Pass 1: measure children that don't need sibling sizes.
        node.children.forEachIndexed { index, child ->
            val c = child.constraints
            val needsSibling = siblingIds(c).isNotEmpty()
            val (parentW, parentH) = parentStretchSize(child)
            when {
                parentW != null || parentH != null -> {
                    measureChild(index, child, parentW, parentH)
                    applyPosition(child)
                }
                !needsSibling -> {
                    measureChild(index, child, null, null)
                    applyPosition(child)
                }
            }
        }

        // Pass 2+: measure remaining once sibling deps are measured.
        var guard = node.children.size + 2
        while (guard-- > 0 && measured.any { !it }) {
            var progressed = false
            node.children.forEachIndexed { index, child ->
                if (measured[index]) return@forEachIndexed
                val c = child.constraints
                val depsReady = siblingIds(c).all { depId ->
                    idToIndex[depId]?.let { measured[it] } ?: true
                }
                if (!depsReady) return@forEachIndexed

                val (sw, sh) = stretchSpan(child)
                measureChild(
                    index,
                    child,
                    if (isStretchH(c)) sw else null,
                    if (isStretchV(c)) sh else null,
                )
                applyPosition(child)
                progressed = true
            }
            if (!progressed) {
                node.children.forEachIndexed { index, child ->
                    if (!measured[index]) {
                        measureChild(index, child, null, null)
                        applyPosition(child)
                    }
                }
                break
            }
        }

        repeat(2) {
            node.children.forEach { child -> applyPosition(child) }
        }

        val layoutHeight = height.coerceAtLeast(
            frames.filterKeys { it != PARENT }.values.maxOfOrNull { it.y + it.h } ?: height,
        )
        layout(width, layoutHeight) {
            node.children.forEachIndexed { index, child ->
                val frame = frames.getValue(child.id)
                placeables[index]?.placeRelative(frame.x, frame.y)
            }
        }
    }
}

private fun boxChildAlignment(constraints: ConstraintSpec?): Alignment = when {
    constraints?.horizontal == "center" && constraints.vertical == "center" -> Alignment.Center
    constraints?.horizontal == "end" && constraints.vertical == "bottom" -> Alignment.BottomEnd
    constraints?.horizontal == "end" -> Alignment.TopEnd
    constraints?.vertical == "bottom" -> Alignment.BottomStart
    constraints?.vertical == "center" -> Alignment.CenterStart
    else -> Alignment.TopStart
}
