package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle

/** One page reached from More: its name, a line on what it holds, its icon and colour. */
class MoreItem(val title: String, val text: String, val icon: ImageVector, val accent: Color, val onClick: () -> Unit)

/** The pages that do not need a place in the navigation bar, one card each. */
@Composable
fun MoreScreen(items: List<MoreItem>) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.ln_nav_more), stringResource(R.string.ln_more_sub))
        items.forEach { MoreCard(it) }
    }
}

/** A card like the launcher's sections (a coloured edge, the icon in a diamond) that opens its page. */
@Composable
private fun MoreCard(item: MoreItem) {
    val shape = MaterialTheme.shapes.large
    val accent = item.accent
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.22f), shape)
            .drawBehind { drawRect(accent, topLeft = Offset(0f, size.height * 0.15f), size = Size(4.dp.toPx(), size.height * 0.7f)) }
            .clickable(onClick = item.onClick)
            .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).graphicsLayer { rotationZ = 45f }.clip(RoundedCornerShape(6.dp))
                .background(accent.copy(alpha = 0.18f)).border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(item.icon, null, tint = accent, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = -45f }) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A page opened from More: a way back to More above it (the system's back does the same). */
@Composable
fun SubPage(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.ln_back), Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.ln_nav_more))
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
