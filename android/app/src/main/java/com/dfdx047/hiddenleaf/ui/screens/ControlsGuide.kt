package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.touch.Ctl
import com.dfdx047.hiddenleaf.touch.Macro
import com.dfdx047.hiddenleaf.touch.MacroStep
import com.dfdx047.hiddenleaf.touch.NinjaPadView
import com.dfdx047.hiddenleaf.touch.PadScene
import com.dfdx047.hiddenleaf.touch.Ps2
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import kotlin.math.hypot

/*
 * The Controls page's explanations: what each button of the scene does, drawn with the controller's own buttons
 * (NinjaPadView.paintSample), the "Toque Pro" section and its guide (cards drawn here, as Dragon Rage's TouchGuide).
 */

/** The controller's drawing of single controls, in the player's look (skin, style); not a view on screen. */
@Composable
internal fun rememberPadPainter(c: TouchConfig): NinjaPadView {
    val ctx = LocalContext.current
    val look = c.copy(mode = TouchMode.ALWAYS, showNames = false)
    return remember(look.skin, look.style) { NinjaPadView(ctx, look) }
}

/** One control as it is on the game's screen, on a dark tile (the buttons are drawn for a game behind them). */
@Composable
internal fun PadIcon(
    painter: NinjaPadView,
    ctl: Ctl?,
    macro: Macro? = null,
    scene: PadScene = PadScene.FIGHT,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(GuideBg)) {
        drawIntoCanvas {
            painter.paintSample(it.nativeCanvas, ctl, macro, size.width / 2, size.height / 2, size.minDimension * 0.86f, scene)
        }
    }
}

private val GuideBg = Color(0xFF151A2C)
private val Thumb = Color(0xFFE8C4A0)
private val Soft = Color(0xFFB8C0E0)
private val Dim = Color(0xFF3A4060)

/** One line of the guide: the button, its name and what it does. */
internal data class GuideItem(val ctl: Ctl?, val title: Int, val text: Int, val macro: Macro? = null)

internal fun guideItems(scene: PadScene): List<GuideItem> = when (scene) {
    PadScene.FIGHT -> listOf(
        GuideItem(Ctl.DPAD, R.string.tc_g_dpad_t, R.string.tc_g_dpad),
        GuideItem(Ctl.ATTACK, R.string.tc_g_attack_t, R.string.tc_g_attack),
        GuideItem(Ctl.JUMP, R.string.tc_g_jump_t, R.string.tc_g_jump),
        GuideItem(Ctl.CHAKRA, R.string.tc_g_chakra_t, R.string.tc_g_chakra),
        GuideItem(Ctl.ITEM, R.string.tc_g_item_t, R.string.tc_g_item),
        GuideItem(Ctl.ITEM_PREV, R.string.tc_g_items_t, R.string.tc_g_items),
        GuideItem(Ctl.GUARD, R.string.tc_g_guard_t, R.string.tc_g_guard),
        GuideItem(Ctl.JUTSU, R.string.tc_g_jutsu_t, R.string.tc_g_jutsu),
        GuideItem(Ctl.OUGI, R.string.tc_g_ougi_t, R.string.tc_g_ougi),
        GuideItem(Ctl.SPECIAL_UP, R.string.tc_g_sp_up_t, R.string.tc_g_sp_up),
        GuideItem(Ctl.SPECIAL_DOWN, R.string.tc_g_sp_down_t, R.string.tc_g_sp_down),
        GuideItem(Ctl.AWAKEN, R.string.tc_g_awaken_t, R.string.tc_g_awaken),
        GuideItem(Ctl.START, R.string.tc_g_start_t, R.string.tc_g_start),
        GuideItem(Ctl.MENU, R.string.tc_g_menu_t, R.string.tc_g_menu),
        GuideItem(null, R.string.tc_g_macro_t, R.string.tc_g_macro, Macro("guide", "M1", listOf(MacroStep(Ps2.CIRCLE, 60)))),
    )
    PadScene.CAMPAIGN -> listOf(
        GuideItem(Ctl.STICK, R.string.tc_g_stick_t, R.string.tc_g_stick),
        GuideItem(Ctl.ATTACK, R.string.tc_g_talk_t, R.string.tc_g_talk),
        GuideItem(Ctl.JUMP, R.string.tc_g_cjump_t, R.string.tc_g_cjump),
        GuideItem(Ctl.CHAKRA, R.string.tc_g_cother_t, R.string.tc_g_cother),
        GuideItem(Ctl.SELECT, R.string.tc_g_select_t, R.string.tc_g_select),
        GuideItem(Ctl.START, R.string.tc_g_cstart_t, R.string.tc_g_cstart),
        GuideItem(Ctl.MENU, R.string.tc_g_menu_t, R.string.tc_g_menu),
    )
    PadScene.MENU -> listOf(
        GuideItem(Ctl.DPAD, R.string.tc_g_cursor_t, R.string.tc_g_cursor),
        GuideItem(Ctl.JUMP, R.string.tc_g_ok_t, R.string.tc_g_ok),
        GuideItem(Ctl.CHAKRA, R.string.tc_g_back_t, R.string.tc_g_back),
        GuideItem(Ctl.ATTACK, R.string.tc_g_mother_t, R.string.tc_g_mother),
        GuideItem(Ctl.ITEM_PREV, R.string.tc_g_mlr_t, R.string.tc_g_mlr),
        GuideItem(Ctl.START, R.string.tc_g_mstart_t, R.string.tc_g_mstart),
        GuideItem(Ctl.MENU, R.string.tc_g_menu_t, R.string.tc_g_menu),
    )
}

/** "What each button does", for the scene picked above the preview. */
@Composable
internal fun ButtonGuide(c: TouchConfig, scene: PadScene) {
    val painter = rememberPadPainter(c)
    SectionCard(stringResource(R.string.tc_guide_title), Icons.AutoMirrored.Rounded.HelpOutline, accent = Konoha.NarutoOrange) {
        Text(stringResource(R.string.tc_guide_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        guideItems(scene).forEachIndexed { i, g ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PadIcon(painter, g.ctl, g.macro, scene)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(g.title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(stringResource(g.text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** "Toque Pro": the switch, what it changes and its tuning; the guide opens from here. */
@Composable
internal fun TouchProSection(c: TouchConfig, onChange: (TouchConfig) -> Unit, onEditLayout: () -> Unit) {
    var guide by remember { mutableStateOf(false) }
    val p = c.pro
    fun set(np: com.dfdx047.hiddenleaf.touch.ProTouch) = onChange(c.copy(pro = np))

    SectionCard(stringResource(R.string.tp_title), Icons.Rounded.TouchApp, accent = Konoha.SharinganRed) {
        Text(stringResource(R.string.tp_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { guide = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.tp_guide))
        }
        ToggleRow(stringResource(R.string.tp_on), stringResource(R.string.tp_on_text), p.on) { set(p.copy(on = it)) }
        if (p.on) {
            HorizontalDivider()
            ToggleRow(stringResource(R.string.tp_slide), stringResource(R.string.tp_slide_text), p.slideAll) { set(p.copy(slideAll = it)) }
            HorizontalDivider()
            StepSlider(stringResource(R.string.tp_pad_size), { "${it * 5}%" }, p.padSize / 5, 16..32) { set(p.copy(padSize = it * 5)) }
            Text(stringResource(R.string.tp_pad_size_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StepSlider(stringResource(R.string.tp_dead), { "$it%" }, p.dead, 0..40) { set(p.copy(dead = it)) }
            Text(stringResource(R.string.tp_dead_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            StepSlider(stringResource(R.string.tp_hit), { "${it * 5}%" }, p.hit / 5, 20..32) { set(p.copy(hit = it * 5)) }
            Text(stringResource(R.string.tp_hit_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Text(stringResource(R.string.tp_custom_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onEditLayout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.touch_edit_layout))
        }
    }
    if (guide) ProGuide(c, onDismiss = { guide = false })
}

// ---- the guide of Toque Pro: cards with a drawing each

@Composable
internal fun ProGuide(c: TouchConfig, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ProGuideContent(c, onDismiss)
    }
}

@Composable
internal fun ProGuideContent(c: TouchConfig, onDismiss: () -> Unit) {
    val painter = rememberPadPainter(c)
    run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.tpg_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(stringResource(R.string.tpg_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
                }
                GuideCard(R.string.tpg_slide, R.string.tpg_slide_text) { drawSlide(painter) }
                GuideCard(R.string.tpg_dpad, R.string.tpg_dpad_text) { drawFixedDpad(painter) }
                GuideCard(R.string.tpg_dead, R.string.tpg_dead_text) { drawDead() }
                GuideCard(R.string.tpg_hit, R.string.tpg_hit_text) { drawHit(painter) }
                GuideCard(R.string.tpg_specials, R.string.tpg_specials_text) { drawSpecials(painter) }
                GuideCard(R.string.tpg_macros, R.string.tpg_macros_text) { drawMacro(painter) }
                GuideCard(R.string.tpg_layout, R.string.tpg_layout_text) { drawLayout(painter) }
            }
        }
    }
}

@Composable
private fun GuideCard(title: Int, text: Int, draw: DrawScope.() -> Unit) {
    Card(
        Modifier.widthIn(max = 720.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(16f / 7f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(GuideBg), onDraw = draw)
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// (drawing helpers: fractions of the card's height, so the drawings scale)

private fun DrawScope.u(f: Float) = size.height * f

private fun DrawScope.ctl(painter: NinjaPadView, ctl: Ctl?, at: Offset, d: Float, macro: Macro? = null) =
    drawIntoCanvas { painter.paintSample(it.nativeCanvas, ctl, macro, at.x, at.y, d) }

/** A thumb seen from above: a rounded tip with its nail. */
private fun DrawScope.thumb(at: Offset, r: Float) {
    drawOval(Thumb.copy(alpha = 0.92f), Offset(at.x - r * 0.8f, at.y - r), Size(r * 1.6f, r * 2.6f))
    drawOval(Color.White.copy(alpha = 0.35f), Offset(at.x - r * 0.45f, at.y - r * 0.8f), Size(r * 0.9f, r * 0.9f))
}

private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, w: Float) {
    drawLine(color, from, to, w, StrokeCap.Round)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val h = w * 3.2f
    val p = Path().apply {
        moveTo(to.x + ux * h * 0.6f, to.y + uy * h * 0.6f)
        lineTo(to.x - ux * h - uy * h * 0.7f, to.y - uy * h + ux * h * 0.7f)
        lineTo(to.x - ux * h + uy * h * 0.7f, to.y - uy * h - ux * h * 0.7f)
        close()
    }
    drawPath(p, color)
}

private fun dashes(scope: DrawScope) = PathEffect.dashPathEffect(floatArrayOf(scope.u(0.03f), scope.u(0.02f)))

/** A finger's path from Attack across the Jutsu to a special, each pressed in turn. */
private fun DrawScope.drawSlide(painter: NinjaPadView) {
    val y = u(0.52f)
    val xs = listOf(0.28f, 0.5f, 0.72f).map { size.width * it }
    val ctls = listOf(Ctl.ATTACK, Ctl.JUTSU, Ctl.SPECIAL_UP)
    xs.forEachIndexed { i, x -> ctl(painter, ctls[i], Offset(x, y), u(0.42f)) }
    val p = Path().apply {
        moveTo(xs[0], y + u(0.02f))
        cubicTo(xs[0] + u(0.2f), y - u(0.32f), xs[1] - u(0.2f), y + u(0.32f), xs[1], y)
        cubicTo(xs[1] + u(0.2f), y - u(0.32f), xs[2] - u(0.2f), y + u(0.32f), xs[2], y)
    }
    drawPath(p, Color.White.copy(alpha = 0.85f), style = Stroke(u(0.014f), pathEffect = dashes(this)))
    thumb(Offset(xs[2] + u(0.05f), y + u(0.1f)), u(0.07f))
}

/** The fixed D-pad with the wide box a touch lands on it, and a thumb on a diagonal at the box's edge. */
private fun DrawScope.drawFixedDpad(painter: NinjaPadView) {
    val c = Offset(size.width * 0.5f, u(0.5f))
    val d = u(0.56f)
    val box = d / 2 * com.dfdx047.hiddenleaf.touch.ProTouch.PRO_DPAD_HIT
    drawRect(Konoha.ChakraAmber.copy(alpha = 0.10f), Offset(c.x - box, c.y - box), Size(box * 2, box * 2))
    drawRect(Konoha.ChakraAmber, Offset(c.x - box, c.y - box), Size(box * 2, box * 2), style = Stroke(u(0.012f), pathEffect = dashes(this)))
    ctl(painter, Ctl.DPAD, c, d)
    thumb(Offset(c.x + box * 0.82f, c.y - box * 0.78f), u(0.07f))
}

/** Rings: the dead zone (nothing) and the rest of the D-pad. */
private fun DrawScope.drawDead() {
    val c = Offset(size.width * 0.5f, u(0.5f))
    val r = u(0.40f)
    drawCircle(Konoha.ChakraBlue.copy(alpha = 0.10f), r, c)
    drawCircle(Konoha.ChakraBlue.copy(alpha = 0.6f), r, c, style = Stroke(u(0.012f)))
    drawCircle(Konoha.ChakraAmber.copy(alpha = 0.25f), r * 0.3f, c)
    drawCircle(Konoha.ChakraAmber, r * 0.3f, c, style = Stroke(u(0.012f), pathEffect = dashes(this)))
    for (i in 0 until 4) {
        val a = Math.PI / 2 * i
        val o = Offset(c.x + (kotlin.math.cos(a) * r * 0.68f).toFloat(), c.y + (kotlin.math.sin(a) * r * 0.68f).toFloat())
        drawCircle(Soft.copy(alpha = 0.8f), u(0.035f), o)
    }
    thumb(Offset(c.x + r * 0.12f, c.y + r * 0.1f), u(0.06f))
}

/** A button with the larger ring a touch counts on. */
private fun DrawScope.drawHit(painter: NinjaPadView) {
    val c = Offset(size.width * 0.5f, u(0.5f))
    val r = u(0.24f)
    drawCircle(Konoha.ChakraAmber.copy(alpha = 0.12f), r * 1.35f, c)
    drawCircle(Konoha.ChakraAmber, r * 1.35f, c, style = Stroke(u(0.012f), pathEffect = dashes(this)))
    ctl(painter, Ctl.ATTACK, c, r * 2f)
    thumb(Offset(c.x + r * 1.15f, c.y + r * 0.75f), u(0.07f))
}

/** Specials: one touch, the input it plays. */
private fun DrawScope.drawSpecials(painter: NinjaPadView) {
    val y = u(0.5f)
    ctl(painter, Ctl.SPECIAL_UP, Offset(size.width * 0.12f, y), u(0.5f))
    sequence(listOf("↑", "↑", "○"), size.width * 0.25f, y)
    ctl(painter, Ctl.SPECIAL_DOWN, Offset(size.width * 0.60f, y), u(0.5f))
    sequence(listOf("↓", "↓", "○"), size.width * 0.73f, y)
}

/** Boxes for the presses in order, with a gap (a let-go) between them. */
private fun DrawScope.sequence(keys: List<String>, x0: Float, y: Float) {
    val w = u(0.13f)
    drawIntoCanvas { cv ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = w * 0.7f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        keys.forEachIndexed { i, k ->
            val x = x0 + i * w * 1.35f
            drawRoundRect(Color(0xFF262C45), Offset(x, y - w / 2), Size(w, w), androidx.compose.ui.geometry.CornerRadius(w * 0.2f))
            val fm = paint.fontMetrics
            cv.nativeCanvas.drawText(k, x + w / 2, y - (fm.ascent + fm.descent) / 2, paint)
        }
    }
}

/** A macro button and its steps on a time line: held, a pause, held together. */
private fun DrawScope.drawMacro(painter: NinjaPadView) {
    val y = u(0.5f)
    ctl(painter, null, Offset(size.width * 0.14f, y), u(0.5f), Macro("g", "M1", listOf(MacroStep(Ps2.CIRCLE, 60))))
    val x0 = size.width * 0.3f
    val x1 = size.width * 0.94f
    drawLine(Soft.copy(alpha = 0.6f), Offset(x0, y + u(0.2f)), Offset(x1, y + u(0.2f)), u(0.008f))
    val blocks = listOf(Triple(0.00f, 0.22f, Konoha.ChakraBlue), Triple(0.30f, 0.38f, Dim), Triple(0.46f, 0.80f, Konoha.NarutoOrange))
    blocks.forEach { (a, b, col) ->
        drawRoundRect(col.copy(alpha = 0.85f), Offset(x0 + (x1 - x0) * a, y - u(0.12f)), Size((x1 - x0) * (b - a), u(0.24f)), androidx.compose.ui.geometry.CornerRadius(u(0.04f)))
    }
    drawIntoCanvas { cv ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(20, 18, 16)
            textSize = u(0.13f)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val fm = paint.fontMetrics
        cv.nativeCanvas.drawText("△", x0 + (x1 - x0) * 0.11f, y - (fm.ascent + fm.descent) / 2, paint)
        cv.nativeCanvas.drawText("△ + ○", x0 + (x1 - x0) * 0.63f, y - (fm.ascent + fm.descent) / 2, paint)
    }
    thumb(Offset(size.width * 0.14f + u(0.12f), y + u(0.14f)), u(0.06f))
}

/** A screen with a button being dragged to a new place and grown. */
private fun DrawScope.drawLayout(painter: NinjaPadView) {
    val l = size.width * 0.12f
    val t = u(0.1f)
    val w = size.width * 0.76f
    val h = u(0.8f)
    drawRoundRect(Color(0xFF1C2238), Offset(l, t), Size(w, h), androidx.compose.ui.geometry.CornerRadius(u(0.04f)))
    drawRoundRect(Soft.copy(alpha = 0.5f), Offset(l, t), Size(w, h), androidx.compose.ui.geometry.CornerRadius(u(0.04f)), style = Stroke(u(0.008f)))
    ctl(painter, Ctl.DPAD, Offset(l + w * 0.14f, t + h * 0.62f), u(0.34f))
    val from = Offset(l + w * 0.62f, t + h * 0.66f)
    val to = Offset(l + w * 0.84f, t + h * 0.42f)
    drawCircle(Dim, u(0.1f), from, style = Stroke(u(0.01f), pathEffect = dashes(this)))
    arrow(from, Offset(to.x - u(0.12f), to.y + u(0.08f)), Konoha.ChakraAmber, u(0.012f))
    ctl(painter, Ctl.JUTSU, to, u(0.3f))
    drawCircle(Konoha.NarutoOrange, u(0.19f), to, style = Stroke(u(0.01f), pathEffect = dashes(this)))
}
