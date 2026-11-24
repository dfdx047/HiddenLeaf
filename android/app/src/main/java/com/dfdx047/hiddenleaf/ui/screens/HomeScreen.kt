package com.dfdx047.hiddenleaf.ui.screens

import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.InstallState
import com.dfdx047.hiddenleaf.data.REPO_URL
import com.dfdx047.hiddenleaf.data.formatBytes
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.components.StatPill
import com.dfdx047.hiddenleaf.ui.components.TrainingGround
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import com.dfdx047.hiddenleaf.ui.theme.TitleStyle

@Composable
fun HomeScreen(vm: AppViewModel) {
    val install by vm.install.collectAsStateWithLifecycle()
    val packs by vm.texturePacks.collectAsStateWithLifecycle()
    val mods by vm.fileMods.collectAsStateWithLifecycle()
    val pickIso = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.installFromIso(uri)
    }
    var confirmRemove by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Hero()

        val installed = install is InstallState.Installed
        PlayStage(enabled = installed, onClick = vm::play)
        val active = mods.filter { it.enabled }
        if (active.isNotEmpty()) {
            Text(
                stringResource(R.string.home_mods_on, active.joinToString { it.name }),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary,
            )
        }
        // (the online service's free plan used up for today: until when, and that the local network still works)
        OnlineQuotaNotice()
        if (installed) {
            OutlinedButton(onClick = vm::createPlayShortcut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AddToHomeScreen, null, Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.shortcut_add))
            }
        }

        SectionCard(stringResource(R.string.game_data), NinjaIcons.Disc) {
            when (val s = install) {
                InstallState.NotInstalled -> {
                    Text(
                        stringResource(R.string.game_data_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = { pickIso.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.pick_iso)) }
                }
                is InstallState.Working -> {
                    Text(s.step, style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(
                        progress = { s.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(s.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = vm::cancelInstall) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.cancel))
                    }
                }
                is InstallState.Installed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Konoha.LeeGreen)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.game_data_ok, s.files, formatBytes(s.bytes)), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        stringResource(R.string.game_data_ok_text),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { confirmRemove = true }) { Text(stringResource(R.string.remove_game_data)) }
                }
                is InstallState.Failed -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { pickIso.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.pick_other_iso)) }
                        TextButton(onClick = vm::dismissInstallError) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }

        SectionCard(stringResource(R.string.engine), NinjaIcons.Engine, accent = MaterialTheme.colorScheme.secondary) {
            Text(
                stringResource(if (vm.engineAvailable) R.string.engine_included else R.string.engine_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = if (vm.engineAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Links()

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatPill(stringResource(R.string.stat_textures), "${packs.count { it.enabled }}", Konoha.SasukeBlueLight, Modifier.weight(1f))
            StatPill(stringResource(R.string.stat_mods), "${mods.count { it.enabled }}", Konoha.LeeGreen, Modifier.weight(1f))
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.remove_game_data_q)) },
            text = { Text(stringResource(R.string.remove_game_data_text)) },
            confirmButton = { TextButton(onClick = { confirmRemove = false; vm.removeGameData() }) { Text(stringResource(R.string.remove)) } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** The title: the app's name in Naruto's orange fading to amber, and a strip with the three colours under it. */
@Composable
private fun Hero() {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        val fire = Brush.verticalGradient(listOf(Konoha.ScrollCream, Konoha.ChakraAmber, Konoha.NarutoOrange))
        // one line on a phone held upright too (at 44 sp the name needs about 380 dp)
        val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 420
        Text(
            "HIDDEN LEAF", maxLines = 1, softWrap = false,
            style = TitleStyle.copy(brush = fire).let { if (narrow) it.copy(fontSize = 34.sp, lineHeight = 38.sp) else it },
        )
        // Naruto, Sasuke, Lee
        Row(Modifier.padding(top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(Konoha.NarutoOrange to 64.dp, Konoha.SasukeBlue to 40.dp, Konoha.LeeGreen to 24.dp).forEach { (c, w) ->
                Box(Modifier.width(w).height(5.dp).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The Play button on its stage: the training ground behind it (posts, shuriken, falling leaves), the button in
 * front at the bottom.
 */
@Composable
private fun PlayStage(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().height(250.dp).clip(shape)
            .border(1.5.dp, Konoha.ChakraAmber.copy(alpha = if (enabled) 0.45f else 0.15f), shape),
        contentAlignment = Alignment.BottomCenter,
    ) {
        TrainingGround(active = enabled, modifier = Modifier.fillMaxSize())
        Box(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) { PlayButton(enabled, onClick) }
    }
}

/** Orange like Naruto's jumpsuit, a green ring like Lee's around the play mark, Sasuke's indigo in the aura. */
@Composable
private fun PlayButton(enabled: Boolean, onClick: () -> Unit) {
    // Keep animation State objects (do NOT delegate with `by`) so only drawBehind invalidates, 0 recompositions/sec!
    val t = rememberInfiniteTransition(label = "play")
    val breath = t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "aura")
    val gleam = t.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "gleam")
    val shape = remember { RoundedCornerShape(22.dp) }
    val bgBrush = remember(enabled) {
        if (enabled) Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep, Konoha.NarutoOrange, Konoha.ChakraAmber))
        else Brush.horizontalGradient(listOf(Color(0xFF3A3C48), Color(0xFF2D2F3A)))
    }
    Box(
        Modifier.fillMaxWidth().height(76.dp).graphicsLayer().drawBehind {
            if (!enabled) return@drawBehind
            val b = breath.value
            for (i in 3 downTo 1) {
                val grow = (5f + 6f * i * (0.6f + 0.4f * b)).dp.toPx()
                drawRoundRect(
                    color = (if (i == 3) Konoha.SasukePurple else if (i == 2) Konoha.NarutoOrange else Konoha.ChakraAmber)
                        .copy(alpha = 0.10f + 0.06f * b / i),
                    topLeft = Offset(-grow, -grow * 0.6f),
                    size = Size(size.width + grow * 2, size.height + grow * 1.2f),
                    cornerRadius = CornerRadius(28.dp.toPx() + grow),
                )
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().clip(shape)
                .background(bgBrush)
                .border(2.dp, if (enabled) Konoha.ScrollCream.copy(alpha = 0.6f) else Color(0x33FFFFFF), shape)
                .drawBehind {
                    if (!enabled) return@drawBehind
                    val x = size.width * gleam.value
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.26f), 1f to Color.Transparent,
                            start = Offset(x - size.height, 0f), end = Offset(x + size.height, size.height),
                        ),
                    )
                }
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(if (enabled) Konoha.LeeGreenDeep else Color(0x33000000))
                        .border(2.5.dp, if (enabled) Konoha.LeeGreenLight else Color(0x22FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(32.dp), tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.play).uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = if (enabled) Konoha.NarutoBlack else Color(0xFF9A9CB0),
                )
            }
        }
    }
}

private data class LinkItem(val label: String, val url: String, val icon: ImageVector, val color: Color)

/** Small round links: the channel, the community, support, and the code. */
@Composable
private fun Links() {
    val uri = LocalUriHandler.current
    val links = listOf(
        LinkItem("YouTube", "https://www.youtube.com/@DFDX47", Icons.Rounded.SmartDisplay, Color(0xFFFF3B30)),
        LinkItem("Discord", "https://discord.gg/6XXxT547Qx", Icons.Rounded.Forum, Color(0xFF7289FF)),
        LinkItem("Ko-fi", "https://ko-fi.com/dfdx047", Icons.Rounded.LocalCafe, Color(0xFF29ABE0)),
        LinkItem("GitHub", REPO_URL, Icons.Rounded.Code, Color(0xFFE6E8F0)),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        links.forEach { l ->
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).clickable { runCatching { uri.openUri(l.url) } }.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(l.color.copy(alpha = 0.14f)).border(1.dp, l.color.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(l.icon, l.label, tint = l.color, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(4.dp))
                Text(l.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
