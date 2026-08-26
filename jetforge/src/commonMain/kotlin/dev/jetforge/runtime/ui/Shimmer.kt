package dev.jetforge.runtime.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.jetForgeShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "jetforge-shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE8E4EE),
            Color(0xFFF6F3FA),
            Color(0xFFE8E4EE),
        ),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 200f),
    )
    return this.background(brush)
}

@Composable
fun JetForgeShimmerSkeleton(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF))
            .padding(if (compact) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!compact) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .jetForgeShimmer(),
            )
        }
        Box(
            Modifier
                .padding(horizontal = if (compact) 0.dp else 16.dp)
                .fillMaxWidth(0.4f)
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .jetForgeShimmer(),
        )
        Box(
            Modifier
                .padding(horizontal = if (compact) 0.dp else 16.dp)
                .fillMaxWidth(0.85f)
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .jetForgeShimmer(),
        )
        Spacer(Modifier.height(4.dp))
        repeat(if (compact) 2 else 4) {
            Row(
                Modifier.padding(horizontal = if (compact) 0.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .jetForgeShimmer(),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .jetForgeShimmer(),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .jetForgeShimmer(),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .jetForgeShimmer(),
                    )
                }
            }
        }
    }
}
