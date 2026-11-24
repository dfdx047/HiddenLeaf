package com.dfdx047.hiddenleaf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.Busy

/** The background of every screen: night over the village, Naruto's orange glow from the top, Sasuke's indigo from
 *  the lower left, a touch of Lee's green, and a few still leaves. */
fun Modifier.leafBackground(primary: Color, secondary: Color): Modifier = drawWithCache {
    val g1 = Brush.radialGradient(
        0f to primary.copy(alpha = 0.20f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.85f, -size.height * 0.05f),
        radius = (size.maxDimension * 0.75f).coerceAtLeast(1f),
    )
    val g2 = Brush.radialGradient(
        0f to secondary.copy(alpha = 0.16f),
        1f to Color.Transparent,
        center = Offset(0f, size.height * 0.9f),
        radius = (size.maxDimension * 0.7f).coerceAtLeast(1f),
    )
    val g3 = Brush.radialGradient(
        0f to com.dfdx047.hiddenleaf.ui.theme.Konoha.LeeGreen.copy(alpha = 0.10f),
        1f to Color.Transparent,
        center = Offset(size.width, size.height),
        radius = (size.maxDimension * 0.5f).coerceAtLeast(1f),
    )
    val u = size.minDimension / 14f
    val leafColor = primary.copy(alpha = 0.06f)
    val leaves = listOf(
        Quad(0.08f, 0.18f, -30f, 1.0f), Quad(0.92f, 0.42f, 40f, 0.8f), Quad(0.18f, 0.72f, 120f, 0.9f),
        Quad(0.78f, 0.86f, -70f, 1.1f), Quad(0.55f, 0.08f, 15f, 0.7f),
    )
    val scratch = Path()
    onDrawBehind {
        drawRect(g1)
        drawRect(g2)
        drawRect(g3)
        for ((fx, fy, rot, k) in leaves) {
            drawLeaf(Offset(size.width * fx, size.height * fy), u * k, rot, leafColor, scratch)
        }
    }
}

private data class Quad(val x: Float, val y: Float, val rot: Float, val k: Float)

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val second = MaterialTheme.colorScheme.tertiary
    Column(modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        // a brush stroke under the title: thick at the start, thinning out
        Canvas(Modifier.padding(top = 2.dp, bottom = 6.dp).size(width = 120.dp, height = 7.dp)) {
            val p = Path().apply {
                moveTo(0f, size.height * 0.35f)
                quadraticTo(size.width * 0.5f, 0f, size.width, size.height * 0.55f)
                quadraticTo(size.width * 0.45f, size.height * 0.75f, 0f, size.height)
                close()
            }
            drawPath(p, Brush.horizontalGradient(listOf(accent, second.copy(alpha = 0.6f))))
        }
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A card of the launcher: cut corners like a forehead protector's plate, a coloured edge on the left, the icon in a
 * diamond and a rope-like line under the title.
 */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val rope = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.22f), shape)
            .drawBehind {
                // the plate's edge
                drawRect(accent, topLeft = Offset(0f, size.height * 0.12f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height * 0.76f))
            },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier.size(34.dp).graphicsLayer { rotationZ = 45f }.clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.18f)).border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = -45f }) }
                    Spacer(Modifier.width(14.dp))
                }
                Text(title.uppercase(), style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.2.sp), color = accent)
            }
            Spacer(
                Modifier.fillMaxWidth().height(3.dp).drawWithCache {
                    val step = 7.dp.toPx()
                    val ropePath = Path().apply {
                        var x = 0f
                        while (x < size.width) {
                            moveTo(x, size.height)
                            lineTo(x + step * 0.6f, 0f)
                            x += step
                        }
                    }
                    val ropeStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    onDrawBehind { drawPath(ropePath, rope, style = ropeStroke) }
                },
            )
            content()
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(84.dp).drawBehind {
                drawCircle(accent.copy(alpha = 0.10f))
                drawCircle(accent.copy(alpha = 0.45f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(40.dp), tint = accent.copy(alpha = 0.85f)) }
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BusyBar(busy: Busy?) {
    if (busy == null) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NinjaIcons.Shuriken, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(busy.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (busy.detail.isNotEmpty()) {
                Text(
                    busy.detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(6.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
        }
    }
}

/** A tag like the one hanging from a scroll: the number large in the brush type, the label under it. */
@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier.clip(shape).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.35f), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(52.dp).background(color))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
