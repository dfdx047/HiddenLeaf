package com.dfdx047.hiddenleaf.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

/**
 * The scene behind the Play button: a training ground at dusk. Wooden posts stand in a row, shuriken are thrown at
 * them from the sides and stick (spinning in flight, still once they hit, with a few splinters), and leaves fall over
 * all of it, in the corners and across the middle. Everything is drawn here (no image of the series is used), and
 * the scene is deterministic: it depends only on the time, so it costs no allocation per frame beyond the paths.
 *
 * [active] false (no game data yet): the scene is dimmer and slower, and no shuriken are thrown.
 */
@Composable
fun TrainingGround(active: Boolean, modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
            }
        }
    }
    Box(
        modifier.graphicsLayer().drawWithCache {
            val dim = if (active) 1f else 0.55f
            val w = size.width
            val h = size.height
            val skyBrush = Brush.verticalGradient(
                0f to Konoha.SasukeBlue.copy(alpha = 0.55f * dim),
                0.55f to Konoha.SasukePurple.copy(alpha = 0.28f * dim),
                1f to Konoha.NarutoOrangeDeep.copy(alpha = 0.42f * dim),
            )
            val sunCenter = Offset(w * 0.5f, h * 0.86f)
            val sunRadius = (w * 0.45f).coerceAtLeast(1f)
            val sunBrush = Brush.radialGradient(
                0f to Konoha.ChakraAmber.copy(alpha = 0.55f * dim),
                1f to Color.Transparent,
                center = sunCenter,
                radius = sunRadius,
            )
            val groundY = h * 0.80f
            val groundBrush = Brush.verticalGradient(
                0f to Konoha.LeeGreenDeep.copy(alpha = 0.85f * dim),
                1f to Konoha.Night.copy(alpha = 0.9f),
                startY = groundY, endY = h,
            )
            val pw = (w * 0.075f).coerceIn(h * 0.14f, h * 0.26f)
            val posts = listOf(
                Post(w * 0.12f, h * 0.18f, groundY, pw),
                Post(w * 0.37f, h * 0.06f, groundY, pw * 1.08f),
                Post(w * 0.63f, h * 0.12f, groundY, pw),
                Post(w * 0.88f, h * 0.24f, groundY, pw * 0.94f),
            )
            val postBrushes = posts.map { p ->
                val left = p.x - p.w / 2
                Brush.horizontalGradient(
                    0f to Konoha.WoodDark, 0.35f to Konoha.WoodLight, 0.7f to Konoha.Wood, 1f to Konoha.WoodDark,
                    startX = left, endX = left + p.w,
                )
            }
            val grainPaths = posts.map { p ->
                val left = p.x - p.w / 2
                val ph = p.bottom - p.top
                val capH = p.w * 0.32f
                Path().apply {
                    for (k in 1..3) {
                        val gx = left + p.w * (0.2f + 0.25f * k)
                        moveTo(gx, p.top + capH)
                        cubicTo(gx - p.w * 0.06f, p.top + ph * 0.35f, gx + p.w * 0.06f, p.top + ph * 0.65f, gx - p.w * 0.02f, p.bottom)
                    }
                }
            }
            val grassDark = Path().apply {
                val n = 26
                for (i in 0..n) {
                    val x = w * i / n
                    val tall = h * (0.025f + 0.02f * ((i * 7) % 5) / 4f)
                    moveTo(x, groundY + 1f); lineTo(x + tall * 0.3f, groundY - tall)
                }
            }
            val grassLight = Path().apply {
                val n = 26
                for (i in 0..n) {
                    val x = w * i / n
                    val tall = h * (0.025f + 0.02f * ((i * 7) % 5) / 4f)
                    moveTo(x + 4f * density, groundY + 1f); lineTo(x + 2f * density - tall * 0.25f, groundY - tall * 0.8f)
                }
            }
            val cornerRadius = CornerRadius(26f * density)
            val grainColor = Konoha.WoodDark.copy(alpha = 0.45f * dim)
            val grainStroke = Stroke(width = 1.2f * density)
            val grassDarkStroke = Stroke(width = 2f * density, cap = StrokeCap.Round)
            val grassLightStroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round)
            val scratchPath = Path()

            onDrawBehind {
                val t = time.floatValue * if (active) 1f else 0.55f
                drawRoundRect(skyBrush, cornerRadius = cornerRadius)
                drawCircle(sunBrush, radius = sunRadius, center = sunCenter)
                posts.forEachIndexed { idx, p -> drawPostCached(p, postBrushes[idx], grainPaths[idx], grainColor, grainStroke, dim) }
                drawRect(groundBrush, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
                drawPath(grassDark, Konoha.LeeGreen.copy(alpha = 0.7f * dim), style = grassDarkStroke)
                drawPath(grassLight, Konoha.LeeGreenLight.copy(alpha = 0.45f * dim), style = grassLightStroke)
                if (active) posts.indices.forEach { lane -> shurikenLane(t, lane, posts) }
                leaves(t, dim, scratchPath)
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------------------------
// layout

private class Post(val x: Float, val top: Float, val bottom: Float, val w: Float)

/** Four posts of different heights; the second and third stand behind the button, the others at the sides. */
private fun DrawScope.posts(): List<Post> {
    val w = size.width
    val h = size.height
    val pw = (w * 0.075f).coerceIn(h * 0.14f, h * 0.26f)
    val base = h * 0.80f
    return listOf(
        Post(w * 0.12f, h * 0.18f, base, pw),
        Post(w * 0.37f, h * 0.06f, base, pw * 1.08f),
        Post(w * 0.63f, h * 0.12f, base, pw),
        Post(w * 0.88f, h * 0.24f, base, pw * 0.94f),
    )
}

// ---------------------------------------------------------------------------------------------------------------
// background

private fun DrawScope.sky(dim: Float) {
    // dusk: Naruto's orange low on the horizon, Sasuke's indigo up high
    drawRoundRect(
        Brush.verticalGradient(
            0f to Konoha.SasukeBlue.copy(alpha = 0.55f * dim),
            0.55f to Konoha.SasukePurple.copy(alpha = 0.28f * dim),
            1f to Konoha.NarutoOrangeDeep.copy(alpha = 0.42f * dim),
        ),
        cornerRadius = CornerRadius(26f * density),
    )
    drawCircle(
        Brush.radialGradient(
            0f to Konoha.ChakraAmber.copy(alpha = 0.55f * dim),
            1f to Color.Transparent,
            center = Offset(size.width * 0.5f, size.height * 0.86f),
            radius = size.width * 0.45f,
        ),
        radius = size.width * 0.45f,
        center = Offset(size.width * 0.5f, size.height * 0.86f),
    )
}

private fun DrawScope.ground(dim: Float) {
    val y = size.height * 0.80f
    drawRect(
        Brush.verticalGradient(
            0f to Konoha.LeeGreenDeep.copy(alpha = 0.85f * dim),
            1f to Konoha.Night.copy(alpha = 0.9f),
            startY = y, endY = size.height,
        ),
        topLeft = Offset(0f, y),
        size = Size(size.width, size.height - y),
    )
    // grass tufts along the edge
    val n = 26
    for (i in 0..n) {
        val x = size.width * i / n
        val tall = size.height * (0.025f + 0.02f * ((i * 7) % 5) / 4f)
        drawLine(Konoha.LeeGreen.copy(alpha = 0.7f * dim), Offset(x, y + 1f), Offset(x + tall * 0.3f, y - tall), strokeWidth = 2f * density, cap = StrokeCap.Round)
        drawLine(Konoha.LeeGreenLight.copy(alpha = 0.45f * dim), Offset(x + 4f * density, y + 1f), Offset(x + 2f * density - tall * 0.25f, y - tall * 0.8f), strokeWidth = 1.5f * density, cap = StrokeCap.Round)
    }
}

/** A cut log standing upright: pre-cached bark gradient & grain path, two rope bands, and the cut end on top. */
private fun DrawScope.drawPostCached(p: Post, bark: Brush, grainPath: Path, grainColor: Color, grainStroke: Stroke, dim: Float) {
    val left = p.x - p.w / 2
    val height = p.bottom - p.top
    val capH = p.w * 0.32f
    drawOval(Color.Black.copy(alpha = 0.28f * dim), topLeft = Offset(left - p.w * 0.25f, p.bottom - capH * 0.4f), size = Size(p.w * 1.5f, capH * 0.8f))
    drawRect(bark, topLeft = Offset(left, p.top + capH / 2), size = Size(p.w, height - capH / 2), alpha = dim)
    drawPath(grainPath, grainColor, style = grainStroke)
    for (fy in listOf(0.34f, 0.46f)) {
        val by = p.top + height * fy
        drawRect(Konoha.ScrollCream.copy(alpha = 0.75f * dim), topLeft = Offset(left - 1f, by), size = Size(p.w + 2f, p.w * 0.11f))
        drawRect(Konoha.WoodDark.copy(alpha = 0.35f * dim), topLeft = Offset(left - 1f, by + p.w * 0.08f), size = Size(p.w + 2f, p.w * 0.03f))
    }
    val capTop = Offset(left, p.top)
    drawOval(Konoha.WoodLight.copy(alpha = dim), topLeft = capTop, size = Size(p.w, capH))
    drawOval(Konoha.ChakraAmber.copy(alpha = 0.35f * dim), topLeft = capTop + Offset(p.w * 0.18f, capH * 0.18f), size = Size(p.w * 0.64f, capH * 0.64f), style = grainStroke)
    drawOval(Konoha.Wood.copy(alpha = 0.8f * dim), topLeft = capTop + Offset(p.w * 0.36f, capH * 0.36f), size = Size(p.w * 0.28f, capH * 0.28f), style = grainStroke)
}

// ---------------------------------------------------------------------------------------------------------------
// shuriken

/** A deterministic "random" number in 0..1 for an integer key (a hash, so every throw has its own values). */
private fun rnd(a: Int, b: Int): Float {
    var x = a * 374761393 + b * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFFFF) / 16777215f
}

/** The part of the scene above the Play button (HomeScreen's PlayStage: 250 dp, the button's top at ~156 dp). */
private const val VISIBLE_BOTTOM = 0.6f

private const val THROW_PERIOD = 3.6f
private const val FLIGHT = 0.42f // seconds in the air

/**
 * One lane per post: a shuriken thrown at it every [THROW_PERIOD] seconds (lanes offset from each other), from the
 * side the post is nearer to or from above. It spins in flight, stops when it hits, quivers, stays, and fades just
 * before the next one comes. Two throws of a lane are on screen at once: the one stuck and the one coming.
 */
private fun DrawScope.shurikenLane(t: Float, lane: Int, posts: List<Post>) {
    val offset = lane * THROW_PERIOD / posts.size + 0.3f
    val local = t - offset
    if (local < 0f) return
    val n = floor(local / THROW_PERIOD).toInt()
    for (k in maxOf(0, n - 1)..n) {
        val age = local - k * THROW_PERIOD
        if (age < 0f || age > THROW_PERIOD * 1.9f) continue
        throwOne(age, lane, k, posts[lane])
    }
}

private fun DrawScope.throwOne(age: Float, lane: Int, k: Int, post: Post) {
    val r = (post.w * 0.36f).coerceAtLeast(9f * density)
    // where it lands: on the post's face, a little to either side, between the cut end and the line where the Play
    // button starts (VISIBLE_BOTTOM of the scene), so every hit is seen
    val from = post.top + post.w * 0.45f
    val to = maxOf(from + 1f, size.height * VISIBLE_BOTTOM - r)
    val hit = Offset(
        post.x + (rnd(lane, k * 3) - 0.5f) * post.w * 0.5f,
        from + (to - from) * rnd(lane, k * 3 + 1),
    )
    // where it comes from: off the near side, or from above for the inner posts
    val fromLeft = post.x < size.width / 2
    val start = when ((k + lane) % 3) {
        0 -> Offset(if (fromLeft) -r * 3 else size.width + r * 3, hit.y - size.height * 0.25f)
        1 -> Offset(if (fromLeft) -r * 3 else size.width + r * 3, hit.y + size.height * 0.05f)
        else -> Offset(hit.x + (if (fromLeft) -1 else 1) * size.width * 0.18f, -r * 3)
    }
    val impactAngle = 360f * rnd(lane, k * 3 + 2)
    val fadeStart = THROW_PERIOD * 1.55f
    val alpha = if (age < fadeStart) 1f else (1f - (age - fadeStart) / (THROW_PERIOD * 0.35f)).coerceIn(0f, 1f)
    if (alpha <= 0f) return

    if (age < FLIGHT) {
        // in flight: along a shallow arc, spinning, with a faint trail
        val f = age / FLIGHT
        val ease = f * (2f - f)
        val arc = sin(PI.toFloat() * f) * size.height * 0.06f
        val pos = Offset(start.x + (hit.x - start.x) * ease, start.y + (hit.y - start.y) * ease - arc)
        val spin = impactAngle - (1f - f) * 1440f
        for (g in 1..3) {
            val gf = (ease - g * 0.06f).coerceAtLeast(0f)
            val gp = Offset(start.x + (hit.x - start.x) * gf, start.y + (hit.y - start.y) * gf - sin(PI.toFloat() * (gf)) * size.height * 0.06f)
            drawCircle(Konoha.HeadbandSteel.copy(alpha = 0.12f / g), radius = r * (1f - 0.15f * g), center = gp)
        }
        drawShuriken(pos, r, spin, 1f)
    } else {
        // stuck: a damped quiver, splinters for a moment, then still
        val s = age - FLIGHT
        val quiver = 9f * exp(-s * 14f) * sin(s * 70f)
        if (s < 0.22f) splinters(hit, r, s / 0.22f, lane, k)
        // the blade sunk into the wood: drawn a bit smaller and tilted toward where it came from
        val tilt = if (start.x < hit.x) -12f else 12f
        drawShuriken(hit, r * 0.92f, impactAngle + quiver + tilt, alpha)
    }
}

private fun DrawScope.splinters(at: Offset, r: Float, f: Float, lane: Int, k: Int) {
    val a = 1f - f
    for (i in 0 until 6) {
        val ang = 2f * PI.toFloat() * (i / 6f + rnd(lane * 31 + i, k))
        val d0 = r * (0.6f + 1.2f * f)
        val d1 = d0 + r * 0.5f
        drawLine(
            Konoha.WoodLight.copy(alpha = 0.9f * a),
            Offset(at.x + cos(ang) * d0, at.y + sin(ang) * d0),
            Offset(at.x + cos(ang) * d1, at.y + sin(ang) * d1),
            strokeWidth = 2f * density, cap = StrokeCap.Round,
        )
    }
}

/** A four-bladed throwing star with a ring in the middle. */
private val shurikenBladePath = Path()
private val shurikenSteel = Color(0xFFC5CCD8)
private val shurikenRim = Color(0xFF2B3040)

fun DrawScope.drawShuriken(center: Offset, radius: Float, degrees: Float, alpha: Float = 1f) {
    rotate(degrees, center) {
        val blades = shurikenBladePath
        blades.reset()
        for (i in 0 until 4) {
            val a = i * PI.toFloat() / 2f
            val tipX = center.x + cos(a) * radius
            val tipY = center.y + sin(a) * radius
            val l = a + PI.toFloat() / 4f
            val rr = radius * 0.30f
            val back = a - PI.toFloat() / 4f
            blades.moveTo(center.x + cos(back) * rr, center.y + sin(back) * rr)
            blades.quadraticTo(center.x + cos(a - 0.35f) * radius * 0.75f, center.y + sin(a - 0.35f) * radius * 0.75f, tipX, tipY)
            blades.lineTo(center.x + cos(l) * rr, center.y + sin(l) * rr)
            blades.lineTo(center.x, center.y)
            blades.close()
        }
        drawPath(blades, shurikenSteel.copy(alpha = alpha))
        drawPath(blades, shurikenRim.copy(alpha = 0.8f * alpha), style = Stroke(width = (radius * 0.06f).coerceAtLeast(1f)))
        drawCircle(shurikenRim.copy(alpha = alpha), radius = radius * 0.2f, center = center)
        drawCircle(Konoha.HeadbandSteel.copy(alpha = alpha), radius = radius * 0.2f, center = center, style = Stroke(width = radius * 0.07f))
    }
}

// ---------------------------------------------------------------------------------------------------------------
// leaves

/** An almond-shaped leaf with its vein, pointing along [degrees]. */
fun DrawScope.drawLeaf(center: Offset, length: Float, degrees: Float, color: Color, reuse: Path? = null) {
    rotate(degrees, center) {
        val half = length / 2
        val w = length * 0.42f
        val p = (reuse ?: Path()).apply {
            reset()
            moveTo(center.x - half, center.y)
            quadraticTo(center.x - half * 0.1f, center.y - w, center.x + half, center.y)
            quadraticTo(center.x - half * 0.1f, center.y + w * 0.9f, center.x - half, center.y)
            close()
        }
        drawPath(p, color)
        drawLine(
            lerp(color, Color.Black, 0.35f).copy(alpha = color.alpha),
            Offset(center.x - half * 1.15f, center.y + length * 0.02f), Offset(center.x + half * 0.8f, center.y),
            strokeWidth = (length * 0.05f).coerceAtLeast(1f),
        )
    }
}

private const val LEAVES = 18

/**
 * Leaves falling all the time: a third in each corner band, the rest over the middle, each with its own speed, sway
 * and turn. A few are the orange of autumn (and Naruto), most are green (and Lee).
 */
private fun DrawScope.leaves(t: Float, dim: Float, reuse: Path) {
    val len = (size.minDimension * 0.085f).coerceAtLeast(9f * density)
    for (i in 0 until LEAVES) {
        val band = i % 3
        val x0 = when (band) {
            0 -> 0.02f + 0.16f * rnd(i, 1)
            1 -> 0.82f + 0.16f * rnd(i, 1)
            else -> 0.22f + 0.56f * rnd(i, 1)
        }
        val speed = 0.11f + 0.09f * rnd(i, 2) // screen heights per second
        val phase = rnd(i, 3)
        val y = ((t * speed + phase) % 1.25f) - 0.12f
        val sway = sin(t * (0.9f + rnd(i, 4)) + i) * size.width * (0.025f + 0.03f * rnd(i, 5))
        val turn = sin(t * (1.3f + rnd(i, 6)) + i * 0.7f) * 70f + 180f * rnd(i, 7)
        val k = 0.7f + 0.6f * rnd(i, 8)
        val c = when {
            i % 6 == 0 -> Konoha.NarutoOrange
            i % 6 == 3 -> Konoha.ChakraAmber
            i % 2 == 0 -> Konoha.LeafLight
            else -> Konoha.Leaf
        }
        // fade in at the top edge and out near the bottom
        val edge = (minOf(y + 0.12f, 1.13f - y) / 0.12f).coerceIn(0f, 1f)
        // flutter: the leaf narrows as it turns edge-on
        val flutter = 0.55f + 0.45f * abs(cos(t * (2.2f + rnd(i, 9)) + i))
        val center = Offset(size.width * x0 + sway, size.height * y)
        drawLeaf(center, len * k * flutter.coerceAtLeast(0.6f), turn, c.copy(alpha = 0.85f * dim * edge), reuse)
    }
}
