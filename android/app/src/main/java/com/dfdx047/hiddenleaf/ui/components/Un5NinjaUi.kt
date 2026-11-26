package com.dfdx047.hiddenleaf.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.OnlineAdvanced
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import java.io.File

data class NinjaCharInfo(
    val id: String,
    val name: String,
    val title: String,
    val primary: Color,
    val secondary: Color,
    val hair: Color,
    val headband: Color,
    val akatsuki: Boolean = false,
)

val UN5_CHARACTERS = listOf(
    NinjaCharInfo("naruto", "Naruto", "Jinchuuriki", Color(0xFFFF7A1A), Color(0xFF1A1A22), Color(0xFFFFD54F), Color(0xFF263238)),
    NinjaCharInfo("sasuke", "Sasuke", "Uchiha", Color(0xFF3949AB), Color(0xFFECEFF1), Color(0xFF151822), Color(0xFF283593)),
    NinjaCharInfo("itachi", "Itachi", "Akatsuki", Color(0xFFD32F2F), Color(0xFF121216), Color(0xFF141418), Color(0xFF212121), akatsuki = true),
    NinjaCharInfo("minato", "Minato", "4º Hokage", Color(0xFFFFB300), Color(0xFFECEFF1), Color(0xFFFFEE58), Color(0xFF1E88E5)),
    NinjaCharInfo("kakashi", "Kakashi", "Ninja Copiador", Color(0xFF2E7D32), Color(0xFF263238), Color(0xFFCFD8DC), Color(0xFF1B5E20)),
    NinjaCharInfo("gaara", "Gaara", "Kazekage", Color(0xFF8E2430), Color(0xFF3E2723), Color(0xFFC62828), Color(0xFF4E342E)),
    NinjaCharInfo("lee", "Rock Lee", "8 Portões", Color(0xFF2E7D32), Color(0xFFFF7043), Color(0xFF111115), Color(0xFFC62828)),
    NinjaCharInfo("sakura", "Sakura", "Ninja Médica", Color(0xFFEC407A), Color(0xFF880E4F), Color(0xFFF48FB1), Color(0xFFC2185B)),
    NinjaCharInfo("neji", "Neji", "Byakugan", Color(0xFFECEFF1), Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF263238)),
    NinjaCharInfo("jiraiya", "Jiraiya", "Sannin", Color(0xFFC62828), Color(0xFF4E6B3A), Color(0xFFECEFF1), Color(0xFFB71C1C)),
    NinjaCharInfo("deidara", "Deidara", "Akatsuki", Color(0xFFD32F2F), Color(0xFF141418), Color(0xFFFFEE58), Color(0xFF212121), akatsuki = true),
    NinjaCharInfo("pain", "Pain", "Rinnegan", Color(0xFFD32F2F), Color(0xFF121216), Color(0xFFFF8A65), Color(0xFF263238), akatsuki = true),
    NinjaCharInfo("obito", "Obito / Tobi", "Akatsuki", Color(0xFFFF6D00), Color(0xFF121216), Color(0xFF151518), Color(0xFF212121), akatsuki = true),
    NinjaCharInfo("orochimaru", "Orochimaru", "Sannin", Color(0xFF7B1FA2), Color(0xFFECEFF1), Color(0xFF141418), Color(0xFF4A148C)),
    NinjaCharInfo("tsunade", "Tsunade", "5ª Hokage", Color(0xFF43A047), Color(0xFFECEFF1), Color(0xFFFFEE58), Color(0xFF1B5E20)),
    NinjaCharInfo("shikamaru", "Shikamaru", "Clã Nara", Color(0xFF2E7D32), Color(0xFF263238), Color(0xFF18181D), Color(0xFF37474F)),
    NinjaCharInfo("hinata", "Hinata", "Byakugan", Color(0xFF7E57C2), Color(0xFFECEFF1), Color(0xFF1A237E), Color(0xFF37474F)),
    NinjaCharInfo("kisame", "Kisame", "Akatsuki", Color(0xFF1565C0), Color(0xFF121216), Color(0xFF283593), Color(0xFF212121), akatsuki = true),
    NinjaCharInfo("sasori", "Sasori", "Akatsuki", Color(0xFFC62828), Color(0xFF121216), Color(0xFFD32F2F), Color(0xFF212121), akatsuki = true),
    NinjaCharInfo("guy", "Might Guy", "8 Portões", Color(0xFF2E7D32), Color(0xFFFF7043), Color(0xFF111115), Color(0xFFC62828)),
)

fun charInfoOf(id: String): NinjaCharInfo =
    UN5_CHARACTERS.firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: if (id.startsWith("char_")) {
            NinjaCharInfo(id, "Ninja #${id.removePrefix("char_")}", "Personagem", Color(0xFFFF7A1A), Color(0xFF1A1A22), Color(0xFFFFD54F), Color(0xFF263238))
        } else {
            UN5_CHARACTERS.first()
        }

private val Un5BrushFont = FontFamily(Font(R.font.shojumaru_regular))
private val Un5BodyFont = FontFamily(Font(R.font.kanit_semibold))

val UN5_WOOD_TOP = Color(0xFF7D4C25)
val UN5_WOOD_BOT = Color(0xFF472710)
val UN5_WOOD_EDGE = Color(0xFF241207)
val UN5_GOLD = Color(0xFFFFE6A0)
val UN5_SCROLL_BG = Color(0xFF1D1510)
val UN5_SCROLL_BORDER = Color(0xFFC68B3C)

/**
 * UN5-styled character portrait cutout: loads real PNG cutout from `<root>/portraits/<id>.png` if present,
 * otherwise draws a crisp stylized UN5 character silhouette + headband + chakra gear frame in pure vector.
 */
@Composable
fun CharacterCutout(
    charId: String,
    level: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 62,
    showLevel: Boolean = true,
    useCustomAvatar: Boolean = false,
    avatarVersion: Int = 0,
) {
    val info = remember(charId) { charInfoOf(charId) }
    val ctx = LocalContext.current
    var loadedBmp by remember(charId, useCustomAvatar, avatarVersion) {
        val dir = File(GamePaths(ctx).root, "portraits")
        val candidates = buildList {
            if (useCustomAvatar) add(File(dir, "custom_avatar.png"))
            add(File(dir, "bank_${info.id}.png"))
        }
        val f = candidates.firstOrNull { it.exists() && it.length() > 512L }
        mutableStateOf(if (f != null) runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull() else null)
    }
    LaunchedEffect(info.id, useCustomAvatar, avatarVersion) {
        if (loadedBmp == null) {
            val f = OnlineAdvanced.fetchCharacterArt(ctx, info.id)
            if (f != null && f.exists()) {
                loadedBmp = runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    val customBmp = loadedBmp
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    Box(
        modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(info.primary.copy(alpha = 0.85f), info.secondary, Color(0xFF120C08)),
                )
            )
            .border(2.dp, UN5_GOLD.copy(alpha = 0.85f), shape),
    ) {
        if (customBmp != null) {
            Image(
                bitmap = customBmp,
                contentDescription = info.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // UN5 gear ring behind character
                drawCircle(
                    color = UN5_GOLD.copy(alpha = 0.22f),
                    radius = w * 0.42f,
                    center = Offset(w * 0.5f, h * 0.45f),
                    style = Stroke(width = w * 0.06f),
                )
                // Cloak / shoulders cutout
                val body = Path().apply {
                    moveTo(w * 0.12f, h)
                    lineTo(w * 0.24f, h * 0.64f)
                    lineTo(w * 0.76f, h * 0.64f)
                    lineTo(w * 0.88f, h)
                    close()
                }
                drawPath(body, color = info.secondary)
                // Collar accent
                drawPath(body, color = info.primary.copy(alpha = 0.85f), style = Stroke(width = w * 0.045f))
                if (info.akatsuki) {
                    // Iconic red cloud dot on cloak
                    drawCircle(Color(0xFFE53935), radius = w * 0.08f, center = Offset(w * 0.36f, h * 0.84f))
                    drawCircle(Color(0xFFE53935), radius = w * 0.06f, center = Offset(w * 0.64f, h * 0.86f))
                }
                // Head + spiky hair silhouette
                drawCircle(Color(0xFFF3C9A8), radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.46f))
                val hair = Path().apply {
                    moveTo(w * 0.24f, h * 0.42f)
                    lineTo(w * 0.18f, h * 0.24f)
                    lineTo(w * 0.34f, h * 0.26f)
                    lineTo(w * 0.42f, h * 0.10f)
                    lineTo(w * 0.54f, h * 0.22f)
                    lineTo(w * 0.68f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.28f)
                    lineTo(w * 0.84f, h * 0.28f)
                    lineTo(w * 0.76f, h * 0.42f)
                    close()
                }
                drawPath(hair, color = info.hair)
                // Ninja headband + metal plate
                drawRect(info.headband, topLeft = Offset(w * 0.26f, h * 0.33f), size = Size(w * 0.48f, h * 0.10f))
                drawRoundRect(
                    Color(0xFFCFD8DC),
                    topLeft = Offset(w * 0.37f, h * 0.34f),
                    size = Size(w * 0.26f, h * 0.08f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        if (showLevel) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0xE61A0E06), RoundedCornerShape(topStart = 6.dp))
                    .border(1.dp, UN5_GOLD.copy(alpha = 0.7f), RoundedCornerShape(topStart = 6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text("Lv.$level", color = UN5_GOLD, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * UN5-styled Ninja Info Card showing the player's main character cutout, mastery level + XP bar,
 * rank emblem (Genin -> Kage), RP points, and W/L record.
 */
@Composable
fun NinjaInfoCard(
    profile: OnlineAdvanced.RankedProfile,
    onCycleChar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarVersion: Int = 0,
) {
    val ch = remember(profile.favChar) { charInfoOf(profile.favChar) }
    val tierColor = com.dfdx047.hiddenleaf.ui.screens.rankTierColor(profile.tier)
    val total = (profile.wins + profile.losses).coerceAtLeast(1)
    val wr = (profile.wins * 100) / total
    val xpInLevel = profile.favCharXp % 200
    val xpFrac = (xpInLevel / 200f).coerceIn(0f, 1f)
    val shape = CutCornerShape(14.dp)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2D1B0F), Color(0xFF1A120D), ch.primary.copy(alpha = 0.22f))))
            .border(2.dp, UN5_SCROLL_BORDER, shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterCutout(
                charId = profile.favChar,
                level = profile.favCharLvl,
                sizeDp = 72,
                useCustomAvatar = true,
                avatarVersion = avatarVersion,
                modifier = Modifier.clickable(onClick = onCycleChar),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        profile.name,
                        style = TextStyle(fontFamily = Un5BrushFont, fontSize = 18.sp, color = UN5_GOLD, shadow = Shadow(Color.Black, Offset(2f, 2f), 2f)),
                    )
                    Text("#${profile.rank}", style = TextStyle(fontFamily = Un5BodyFont, fontSize = 13.sp, color = tierColor))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.dfdx047.hiddenleaf.ui.screens.RankEmblem(profile.tier, size = 22)
                    Text(
                        "${profile.tier} · ${profile.points} RP · ${profile.wins}V/${profile.losses}D ($wr%)" +
                            (if (profile.streak >= 2) " 🔥${profile.streak}" else ""),
                        style = TextStyle(fontFamily = Un5BodyFont, fontSize = 12.sp, color = Konoha.ScrollCream),
                    )
                }
                // Character mastery bar
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${ch.name} (${ch.title}) · Lv.${profile.favCharLvl}",
                        style = TextStyle(fontFamily = Un5BodyFont, fontSize = 11.sp, color = ch.primary),
                    )
                    Text("· toque no retrato", fontSize = 10.sp, color = Konoha.ScrollCream.copy(alpha = 0.55f))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF3A2416)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(xpFrac.coerceAtLeast(0.06f))
                            .height(6.dp)
                            .background(Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep, UN5_GOLD))),
                    )
                }
                val topChars = profile.mastery.entries.sortedByDescending { it.value.xp }.take(4)
                if (topChars.isNotEmpty()) {
                    val summary = topChars.joinToString(" · ") { (cid, m) -> "${charInfoOf(cid).name} Lv.${m.lvl}" }
                    Text(
                        "Maestrias: $summary",
                        style = TextStyle(fontFamily = Un5BodyFont, fontSize = 10.sp, color = Konoha.ScrollCream.copy(alpha = 0.72f)),
                    )
                }
            }
        }
    }
}

/**
 * UN5 Mode-Select wooden board button for the custom Online screen.
 */
@Composable
fun Un5WoodActionBoard(
    title: String,
    subtitle: String? = null,
    accent: Color = Konoha.NarutoOrange,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CutCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(UN5_WOOD_TOP, UN5_WOOD_BOT)))
            .border(2.dp, if (enabled) accent.copy(alpha = 0.9f) else UN5_WOOD_EDGE, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▶", color = if (enabled) accent else Color.Gray, fontSize = 15.sp, modifier = Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(
                    fontFamily = Un5BrushFont, fontSize = 16.sp,
                    color = if (enabled) UN5_GOLD else UN5_GOLD.copy(alpha = 0.45f),
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 2f),
                ),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = TextStyle(fontFamily = Un5BodyFont, fontSize = 11.sp, color = Konoha.ScrollCream.copy(alpha = 0.85f)))
            }
        }
    }
}

/**
 * Animated visual guide in the launcher's style explaining every optional menu touch gesture
 * (swipe to move, tap to confirm, 2-finger tap for back, 2-finger swipe for L1/R1, hold for ○, 3-finger for START).
 */
@Composable
fun MenuGestureAnimatedGuide(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "gesture_guide")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "p",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Konoha.NightHigh)
            .border(1.dp, Konoha.NarutoOrange.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Guia animado dos gestos nos menus", style = MaterialTheme.typography.titleSmall, color = UN5_GOLD)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GestureDemoTile("Arrastar 1 dedo", "Move o cursor ↑↓←→", Modifier.weight(1f)) {
                val y = size.height * (0.25f + 0.50f * phase)
                drawLine(Konoha.NarutoOrange.copy(alpha = 0.45f), Offset(size.width * 0.5f, size.height * 0.22f), Offset(size.width * 0.5f, y), strokeWidth = 5f)
                drawCircle(Konoha.NarutoOrange, radius = 10f, center = Offset(size.width * 0.5f, y))
            }
            GestureDemoTile("Toque curto", "Confirmar (✕ / OK)", Modifier.weight(1f)) {
                val r = 8f + 16f * phase
                drawCircle(Konoha.LeeGreenLight.copy(alpha = (1f - phase) * 0.7f), radius = r, center = center, style = Stroke(3f))
                drawCircle(Konoha.LeeGreenLight, radius = 8f, center = center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GestureDemoTile("2 dedos (toque)", "Voltar (△)", Modifier.weight(1f)) {
                val r = 6f + 12f * phase
                val c1 = Offset(size.width * 0.36f, size.height * 0.5f)
                val c2 = Offset(size.width * 0.64f, size.height * 0.5f)
                drawCircle(Konoha.SasukeBlueLight.copy(alpha = (1f - phase) * 0.7f), radius = r, center = c1, style = Stroke(3f))
                drawCircle(Konoha.SasukeBlueLight.copy(alpha = (1f - phase) * 0.7f), radius = r, center = c2, style = Stroke(3f))
                drawCircle(Konoha.SasukeBlueLight, radius = 7f, center = c1)
                drawCircle(Konoha.SasukeBlueLight, radius = 7f, center = c2)
            }
            GestureDemoTile("2 dedos arrastar", "Trocar cor / aba (L1/R1)", Modifier.weight(1f)) {
                val x = size.width * (0.25f + 0.50f * phase)
                drawCircle(Konoha.ChakraAmber, radius = 7f, center = Offset(x, size.height * 0.36f))
                drawCircle(Konoha.ChakraAmber, radius = 7f, center = Offset(x, size.height * 0.64f))
            }
        }
        Text(
            "• Segurar 1 dedo = ○ (Aleatório)   ·   • Toque com 3 dedos = START",
            style = MaterialTheme.typography.bodySmall,
            color = Konoha.ScrollCream.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun GestureDemoTile(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    drawAnim: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF151218))
            .border(1.dp, Color(0xFF352C3A), RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(42.dp)) { drawAnim() }
        Text(title, style = MaterialTheme.typography.labelMedium, color = UN5_GOLD)
        Text(desc, style = MaterialTheme.typography.labelSmall, color = Konoha.ScrollCream.copy(alpha = 0.75f))
    }
}
