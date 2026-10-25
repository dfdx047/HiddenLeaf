package com.dfdx047.hiddenleaf.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.Busy

/** The background of every screen: night over the village, Naruto's orange glow from the top, Sasuke's indigo from
 *  the lower left, a touch of Lee's green, and a few still leaves. */
fun Modifier.leafBackground(primary: Color, secondary: Color): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            0f to primary.copy(alpha = 0.20f),
            1f to Color.Transparent,
            center = Offset(size.width * 0.85f, -size.height * 0.05f),
            radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            0f to secondary.copy(alpha = 0.16f),
            1f to Color.Transparent,
            center = Offset(0f, size.height * 0.9f),
            radius = size.maxDimension * 0.7f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            0f to com.dfdx047.hiddenleaf.ui.theme.Konoha.LeeGreen.copy(alpha = 0.10f),
            1f to Color.Transparent,
            center = Offset(size.width, size.height),
            radius = size.maxDimension * 0.5f,
        ),
    )
    // still leaves, very faint: where they fall in the Play animation, at rest
    val u = size.minDimension / 14f
    for ((fx, fy, rot, k) in listOf(
        Quad(0.08f, 0.18f, -30f, 1.0f), Quad(0.92f, 0.42f, 40f, 0.8f), Quad(0.18f, 0.72f, 120f, 0.9f),
        Quad(0.78f, 0.86f, -70f, 1.1f), Quad(0.55f, 0.08f, 15f, 0.7f),
    )) {
        drawLeaf(Offset(size.width * fx, size.height * fy), u * k, rot, primary.copy(alpha = 0.06f))
    }
}

private data class Quad(val x: Float, val y: Float, val rot: Float, val k: Float)

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Box(
            Modifier.padding(top = 2.dp, bottom = 4.dp).size(width = 44.dp, height = 3.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BusyBar(busy: Busy?) {
    if (busy == null) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(busy.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            if (busy.detail.isNotEmpty()) {
                Text(
                    busy.detail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.size(6.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/** A small "power level" pill: label and value. */
@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
