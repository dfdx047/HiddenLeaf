package com.dfdx047.hiddenleaf.touch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import androidx.core.content.res.ResourcesCompat
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.engine.TouchBridge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The on-screen controller for Ultimate Ninja 5. Three scenes ([PadScene]): a fight (D-pad, the fighting buttons and
 * one-touch jutsu), the campaign's map (a stick) and menus (D-pad and the PS2 diamond); the scene follows the game
 * ([TouchBridge.scene]) or the SCENE button. Every button shows what it does (a kunai to attack, a spiral for chakra, a
 * shuriken for items…) with the console's letter in a corner.
 *
 * Touches become the PS2 pad's buttons and stick ([TouchListener.onPad]); the jutsu buttons, the two specials (↑ ↑ ○,
 * ↓ ↓ ○) and the user's macros play their sequence of presses. The buttons show their icon only; the names come with
 * [TouchConfig.showNames]. "Toque Pro" ([TouchConfig.pro]) slides across every control and gives the fight a larger
 * fixed D-pad with a wide touch area. In edit mode the controls of the scene shown are moved, resized and hidden, each scene
 * with its own layout, and the changed layout goes to [TouchListener.onConfigChanged]. The same view is the in-game
 * overlay and the launcher's editor and preview.
 */
@SuppressLint("ViewConstructor")
class NinjaPadView(context: Context, initial: TouchConfig, editing: Boolean = false) : View(context), TouchSurface {

    override var listener: TouchListener? = null

    override var config: TouchConfig = initial
        set(v) {
            field = v
            rebuild()
        }

    override var editing: Boolean = editing
        set(v) {
            if (field == v) return
            field = v
            selected = null
            releaseAll()
            rebuild()
        }

    override var hiddenForPad = false
        set(v) {
            if (field != v) {
                field = v
                if (v) releaseAll()
                invalidate()
            }
        }

    override var suppressed = false
        set(v) {
            if (field != v) {
                field = v
                if (v) releaseAll()
                invalidate()
            }
        }

    override var previewScene: PadScene? = null
        set(v) {
            field = v
            if (v != null) scene = v
        }

    /** The scene on screen now. */
    private var scene: PadScene = PadScene.FIGHT
        set(v) {
            if (field != v) {
                field = v
                selected = null
                releaseAll()
                rebuild()
            }
        }

    // ---- layout

    private class El(
        val key: String,
        val ctl: Ctl?,
        val macro: Macro?,
        val kind: CtlKind,
        var cx: Float,
        var cy: Float,
        var w: Float,
        var h: Float,
        var scale: Float,
        var visible: Boolean,
        var opacity: Int,
        var cacheK: Float = -1f,
        var cacheDown: Boolean = false,
        var cacheGlass: Boolean = false,
        var cacheCol: Int = 0,
        var bodyShader: Shader? = null,
        var rimShader: Shader? = null,
        var dpadPath: Path? = null,
        var dpadCx: Float = Float.NaN,
        var dpadCy: Float = Float.NaN,
        var dpadW: Float = Float.NaN,
    ) {
        val round: Boolean get() = kind != CtlKind.DPAD && kind != CtlKind.PILL && kind != CtlKind.SHOULDER
        fun hit(x: Float, y: Float, slack: Float): Boolean =
            if (round) hypot(x - cx, y - cy) <= w / 2 * slack
            else abs(x - cx) <= w / 2 * slack && abs(y - cy) <= h / 2 * slack
    }

    private val dp = resources.displayMetrics.density
    private val els = mutableListOf<El>()
    private var insetL = 0f
    private var insetT = 0f
    private var insetR = 0f
    private var insetB = 0f
    private val safeW get() = (width - insetL - insetR).coerceAtLeast(1f)
    private val safeH get() = (height - insetT - insetB).coerceAtLeast(1f)

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cut = insets.displayCutout
        insetL = (cut?.safeInsetLeft ?: 0).toFloat()
        insetT = (cut?.safeInsetTop ?: 0).toFloat()
        insetR = (cut?.safeInsetRight ?: 0).toFloat()
        insetB = (cut?.safeInsetBottom ?: 0).toFloat()
        rebuild()
        return super.onApplyWindowInsets(insets)
    }

    private fun baseSize(kind: CtlKind, size: Float): Pair<Float, Float> = when (kind) {
        CtlKind.SHOULDER -> size to size * 0.5f
        CtlKind.PILL -> size to size * 0.46f
        else -> size to size
    }

    private fun rebuild() {
        val selectedKey = selected?.key
        els.clear()
        if (width <= 0 || height <= 0) return
        val all = config.scale
        fun place(key: String, ctl: Ctl?, macro: Macro?, kind: CtlKind, size: Float, p: Placement) {
            val (bw, bh) = baseSize(kind, size)
            val w = bw * dp * all * p.scale
            val h = bh * dp * all * p.scale
            val mx = (w / 2 + 8 * dp).coerceAtMost(safeW / 2)
            val my = (h / 2 + 6 * dp).coerceAtMost(safeH / 2)
            val cx = (insetL + p.x * safeW).coerceIn(insetL + mx, width - insetR - mx)
            val cy = (insetT + p.y * safeH).coerceIn(insetT + my, height - insetB - my)
            els += El(key, ctl, macro, kind, cx, cy, w, h, p.scale, p.visible, p.opacity)
        }
        val gestures = scene == PadScene.MENU && config.menuTouch
        // (Toque Pro: the fight's D-pad at its own size, larger by default; the menus' D-pad and the stick as they are)
        val padK = if (config.pro.on && scene == PadScene.FIGHT) config.pro.padSize / 100f else 1f
        for (c in Ctl.entries) {
            if (c == Ctl.OPTIONS) continue
            if (gestures && c != Ctl.MENU && c != Ctl.START && c != Ctl.SCENE) continue // (START: the title screen and the 2P join)
            val p = config.placeOf(scene, c) ?: continue
            val size = if (c.kind == CtlKind.DPAD) c.size * padK else c.size
            place("${scene.name}.${c.name}", c, null, c.kind, size, p)
        }
        if (scene != PadScene.MENU) {
            config.macros.forEachIndexed { i, m ->
                place("M:" + m.id, null, m, CtlKind.MACRO, 58f, m.place ?: PadDefaults.macroPlace(i))
            }
        }
        selected = selectedKey?.let { k -> els.firstOrNull { it.key == k } }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    // ---- state

    // (originX/Y: the centre the D-pad's and the stick's directions are read from; the element's own, or where the thumb
    // landed when it floats)
    private class Track(var el: El?, val downX: Float, val downY: Float, var originX: Float, var originY: Float, var moved: Boolean = false)

    private val tracks = HashMap<Int, Track>()
    private var dpadBits = 0
    private var lx = 128
    private var ly = 128
    private var lastSent = -1L
    private val pressed = HashSet<El>()
    private val pressedAt = HashMap<El, Long>() // when each button went down (for MIN_HOLD_MS)
    private val players = HashMap<String, Player>()
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Plays a sequence of presses: a jutsu button, a special or one of the user's macros. A sequence with a direction in
     * it ([ownsDirs]: the specials' ↑ ↑ ○) owns the D-pad while it plays, so a thumb resting on the D-pad does not turn
     * its ↑ ↑ into a diagonal or fill the gap between the two presses.
     */
    private inner class Player(val steps: List<MacroStep>, val repeat: Boolean, val holdLast: Boolean = false) {
        var held = false
        var mask = 0
        private var holdingLast = false
        private val hasDirs = steps.any { it.buttons and Ps2.DIRECTIONS != 0 }
        val ownsDirs get() = hasDirs && !holdingLast
        private var index = 0
        var running = false
            private set
        private val step = object : Runnable {
            override fun run() {
                if (steps.isEmpty()) return finish()
                if (index >= steps.size) {
                    if (holdLast && held) {
                        holdingLast = true
                        mask = steps.last().buttons
                        recompute()
                        return
                    }
                    if (repeat && held) index = 0 else return finish()
                }
                val s = steps[index++]
                mask = s.buttons
                recompute()
                handler.postDelayed(this, s.ms.toLong().coerceAtLeast(16))
            }
        }

        fun start() {
            held = true
            if (running) return
            running = true
            holdingLast = false
            index = 0
            handler.post(step)
        }

        fun release() {
            held = false
            if (holdingLast) finish()
        }

        fun cancel() {
            handler.removeCallbacks(step)
            finish()
        }

        private fun finish() {
            running = false
            holdingLast = false
            mask = 0
            recompute()
        }
    }

    // The game's scene, asked ten times a second. A new scene counts after it has held for half a second, so a loading
    // screen's flicker or a moment of the renderer changing hands does not swap the controls. A change of the game's scene
    // wins over a choice by hand.
    private var lastEngineScene = 0
    private var candidateScene = 0
    private var candidateSince = 0L
    // When a conversation of the campaign ends (4 → 3), the menu's ✕ the player was tapping to move the text on sits where
    // the campaign's Attack/○ (Talk) is: the next tap opened the same conversation again (docs/registro.md 2026-11-11).
    // For a moment after that change, a new touch on a button does nothing; the stick and the d-pad work at once.
    private var talkEndedAt = 0L
    private val scenePoll = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            if (!editing && previewScene == null && config.autoMode) {
                val s = TouchBridge.scene()
                val now = android.os.SystemClock.uptimeMillis()
                if (s != candidateScene) {
                    candidateScene = s
                    candidateSince = now
                }
                // (an area's loading screen is a moment of "menu": leaving play takes longer than coming back to it)
                val hold = if (s == 1 && lastEngineScene > 1) SCENE_LEAVE_MS else SCENE_HOLD_MS
                if (s != 0 && s != lastEngineScene && now - candidateSince >= hold) {
                    if (lastEngineScene == 4 && s == 3) talkEndedAt = now
                    lastEngineScene = s
                    android.util.Log.i("un5", "touch: scene $s")
                    releaseAll()
                    scene = when (s) {
                        1, 4 -> PadScene.MENU // (4: a conversation of the campaign, the player held)
                        3 -> PadScene.CAMPAIGN
                        else -> PadScene.FIGHT
                    }
                }
            }
            handler.postDelayed(this, 100L)
        }
    }

    private fun releaseAll() {
        handler.removeCallbacks(gRepeat); handler.removeCallbacks(gHold); gActive = false; gDir = 0; gPulse = 0
        tracks.clear()
        pressed.clear()
        dpadBits = 0
        lx = 128; ly = 128
        players.values.forEach { it.cancel() }
        players.clear()
        recompute()
        invalidate()
    }

    private fun recompute() {
        val macroOwnsDirs = players.values.any { it.running && it.ownsDirs }
        var b = if (macroOwnsDirs) 0 else dpadBits
        val sx = if (macroOwnsDirs) 128 else lx
        val sy = if (macroOwnsDirs) 128 else ly
        for (e in pressed) if (e.ctl != null && e.kind != CtlKind.MACRO && e.kind != CtlKind.SYSTEM) b = b or e.ctl.bit
        for (p in players.values) b = b or p.mask
        val packed = (b.toLong() shl 16) or (sx.toLong() shl 8) or sy.toLong()
        if (packed != lastSent) {
            lastSent = packed
            listener?.onPad(b, sx, sy, 128, 128)
        }
        invalidate()
    }

    private fun buzz() {
        if (config.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    // ---- touches

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (suppressed && !editing) return false
        if (config.mode == TouchMode.OFF && !editing) return false
        if (hiddenForPad && !editing) {
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) hiddenForPad = false
            return true
        }
        if (editing) return editTouch(ev)
        if (scene == PadScene.MENU && config.menuTouch && menuGesture(ev)) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = ev.actionIndex
                down(ev.getPointerId(i), ev.getX(i), ev.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until ev.pointerCount) move(ev.getPointerId(i), ev.getX(i), ev.getY(i))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = ev.actionIndex
                up(ev.getPointerId(i), ev.getX(i), ev.getY(i))
            }
            MotionEvent.ACTION_CANCEL -> releaseAll()
        }
        return true
    }

    private fun shown(e: El) = editing || e.visible

    // ---- the game's menus by touch (no buttons on screen; the Controls tab of the game menu lists these)
    //  tap ✕ (OK) · slide: the direction, held: it repeats · hold still: ○ · two fingers: △ (back) · two fingers
    //  sliding left/right: L1/R1 · two fingers held: □ · three fingers: START
    private var gDownX = 0f
    private var gDownY = 0f
    private var gFingers = 0
    private var gDir = 0
    private var gDone = false   // this gesture already pressed something (a slide, a hold)
    private var gActive = false
    private var gPulse = 0
    private val gRelease = Runnable { gPulse = 0; recomputeMenu() }
    private val gRepeat = object : Runnable {
        override fun run() {
            if (gDir == 0) return
            pulse(gDir)
            handler.postDelayed(this, MENU_REPEAT_MS)
        }
    }
    private val gHold = Runnable {
        if (gDone || gDir != 0) return@Runnable
        gDone = true
        pulse(if (gFingers >= 2) Ps2.SQUARE else Ps2.CIRCLE)
    }

    private fun recomputeMenu() {
        val packed = (gPulse.toLong() shl 16) or (128L shl 8) or 128L
        if (packed != lastSent) {
            lastSent = packed
            listener?.onPad(gPulse, 128, 128, 128, 128)
        }
    }

    /** One press of [bits] long enough for the game's menus to see (they read the pad once a frame, 25 or 50 a second). */
    private fun pulse(bits: Int) {
        handler.removeCallbacks(gRelease)
        gPulse = bits
        buzz()
        recomputeMenu()
        handler.postDelayed(gRelease, MENU_PRESS_MS)
    }

    private fun menuGesture(ev: MotionEvent): Boolean {
        val slide = 18 * dp
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // the menu button and START stay buttons
                if (find(ev.x, ev.y, 1.2f, setOf(CtlKind.SYSTEM, CtlKind.PILL)) != null) return false
                gActive = true; gDownX = ev.x; gDownY = ev.y; gFingers = 1; gDir = 0; gDone = false
                handler.postDelayed(gHold, MENU_HOLD_MS)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!gActive) return false
                gFingers = maxOf(gFingers, ev.pointerCount)
                gDownX = (0 until ev.pointerCount).map { ev.getX(it) }.average().toFloat()
                gDownY = (0 until ev.pointerCount).map { ev.getY(it) }.average().toFloat()
                handler.removeCallbacks(gHold)
                handler.postDelayed(gHold, MENU_HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gActive) return false
                val x = (0 until ev.pointerCount).map { ev.getX(it) }.average().toFloat()
                val y = (0 until ev.pointerCount).map { ev.getY(it) }.average().toFloat()
                val dx = x - gDownX
                val dy = y - gDownY
                if (gFingers >= 2) {
                    if (!gDone && abs(dx) > slide * 1.5f && abs(dx) > abs(dy)) {
                        gDone = true
                        handler.removeCallbacks(gHold)
                        pulse(if (dx < 0) Ps2.L1 else Ps2.R1)
                    }
                } else if (hypot(dx, dy) > slide) {
                    val dir = if (abs(dx) > abs(dy)) (if (dx < 0) Ps2.LEFT else Ps2.RIGHT) else (if (dy < 0) Ps2.UP else Ps2.DOWN)
                    handler.removeCallbacks(gHold)
                    gDone = true
                    // Native smooth drag-scrolling: every slide step immediately moves one menu item and resets the drag anchor
                    gDownX = x; gDownY = y
                    pulse(dir)
                    if (dir != gDir) {
                        gDir = dir
                        handler.removeCallbacks(gRepeat)
                        handler.postDelayed(gRepeat, MENU_REPEAT_FIRST_MS)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {}
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!gActive) return false
                gActive = false
                handler.removeCallbacks(gHold)
                handler.removeCallbacks(gRepeat)
                gDir = 0
                if (!gDone && ev.actionMasked == MotionEvent.ACTION_UP) {
                    pulse(when {
                        gFingers >= 3 -> Ps2.START
                        gFingers == 2 -> Ps2.TRIANGLE
                        else -> when (TouchBridge.menuTap(ev.x / width.coerceAtLeast(1), ev.y / height.coerceAtLeast(1))) {
                            1 -> Ps2.UP
                            3 -> Ps2.DOWN
                            else -> Ps2.CROSS
                        }
                    })
                }
            }
        }
        return true
    }

    private fun find(x: Float, y: Float, slack: Float = touchSlack(), kinds: Set<CtlKind>? = null): El? =
        els.filter { shown(it) && (kinds == null || it.kind in kinds) && it.hit(x, y, if (it.kind == CtlKind.DPAD) dpadSlack(slack) else slack) }
            .minByOrNull { hypot(x - it.cx, y - it.cy) }

    // ---- Toque Pro (TouchConfig.pro): null when it is off

    private val pro: ProTouch? get() = config.pro.takeIf { it.on }

    /** How far around a button a touch still counts (of its size). */
    private fun touchSlack(): Float = pro?.let { it.hit / 100f } ?: 1.12f

    /** What a sliding finger presses: the action buttons; in Toque Pro the jutsu, specials, macros and L1/R1 too. */
    private fun slideKinds(): Set<CtlKind> = if (pro?.slideAll == true) SLIDE_ALL else SLIDE_FACE

    /**
     * How far around the D-pad a touch lands on it: in a Toque Pro fight a wide box ([ProTouch.PRO_DPAD_HIT] of its
     * half-size each way), so a thumb that lands a little off the fixed D-pad still steers it.
     */
    private fun dpadSlack(slack: Float): Float =
        if (pro != null && scene == PadScene.FIGHT) max(slack, ProTouch.PRO_DPAD_HIT) else slack

    private fun down(id: Int, x: Float, y: Float) {
        var e = find(x, y)
        val t = Track(e, x, y, e?.cx ?: x, e?.cy ?: y)
        val stick = els.firstOrNull { it.kind == CtlKind.STICK && it.visible }
        // (the campaign's stick starts where the thumb lands, when it floats; the D-pad is always where it was placed)
        if (e == null && stick != null && config.sticksFloat && x < width * 0.45f && y > height * 0.3f) {
            e = stick; t.el = stick; t.originX = x; t.originY = y
        }
        if (e != null && e.kind == CtlKind.BUTTON && scene == PadScene.CAMPAIGN &&
            SystemClock.uptimeMillis() - talkEndedAt < TALK_END_GUARD_MS
        ) {
            t.el = null; e = null // (the finger stays tracked: lifted, it presses nothing)
        }
        tracks[id] = t
        if (e == null) return
        when (e.kind) {
            CtlKind.DPAD -> {
                dpadBits = dpadDir(e, t, x, y); buzz(); recompute()
            }
            CtlKind.STICK -> stick(e, t, x, y)
            else -> press(e)
        }
    }

    /** A button, plate, jutsu or macro goes down (by a touch, or by a finger sliding onto it). */
    private fun press(e: El) {
        if (e.kind == CtlKind.MACRO) {
            val steps = e.macro?.steps ?: e.ctl?.steps ?: return
            val holdLast = e.ctl?.holdLast ?: false
            // Cancel any other active macro so ↑↑○/↓↓○ can never overlap with Jutsu (△+○) or Ougi (△△○)
            players.forEach { (k, p) -> if (k != e.key && p.running) p.cancel() }
            players.getOrPut(e.key) { Player(steps, e.macro?.repeat ?: false, holdLast) }.start()
        } else {
            pressedAt[e] = SystemClock.uptimeMillis()
        }
        pressed += e
        buzz()
        recompute()
    }

    /** A button, plate, jutsu or macro that the finger of [t] leaves (lifted, or slid off it). */
    private fun letGo(t: Track, e: El) {
        when (e.kind) {
            CtlKind.MACRO -> {
                // (the sequence plays to its end; a repeating one stops at the end of the round)
                players[e.key]?.release()
                if (tracks.values.none { it !== t && it.el === e }) pressed -= e
                invalidate()
            }
            else -> {
                // a quick tap stays down MIN_HOLD_MS: the campaign reads the pad every other vsync, and a tap shorter than
                // that was lost (talking to Jiraiya, docs/registro.md 2026-11-09)
                val left = MIN_HOLD_MS - (SystemClock.uptimeMillis() - (pressedAt[e] ?: 0L))
                if (left > 0L) {
                    handler.postDelayed({
                        if (tracks.values.none { it.el === e } && pressed.remove(e)) recompute()
                    }, left)
                } else if (tracks.values.none { it !== t && it.el === e }) {
                    pressed -= e
                }
                recompute()
            }
        }
    }

    private fun move(id: Int, x: Float, y: Float) {
        val t = tracks[id] ?: return
        val e = t.el
        val kinds = slideKinds()
        if (e == null) {
            // a finger that landed between buttons and slides onto one presses it (never auto-slide onto GUARD/R2 or JUTSU/OUGI)
            find(x, y, 1.0f, kinds)?.takeIf { it.ctl != Ctl.GUARD && it.ctl != Ctl.JUTSU && it.ctl != Ctl.OUGI }?.let { n ->
                t.el = n; press(n)
            }
            return
        }
        when (e.kind) {
            CtlKind.DPAD -> {
                val d = dpadDir(e, t, x, y)
                if (d != dpadBits) {
                    dpadBits = d; recompute()
                }
            }
            CtlKind.STICK -> stick(e, t, x, y)
            in kinds -> if (!e.hit(x, y, max(1.25f, touchSlack() + 0.13f))) {
                // sliding across the buttons: the one under the finger now (never auto-slide onto GUARD/R2 or JUTSU/OUGI to prevent accidental Kawarimi or △+○)
                letGo(t, e)
                val n = find(x, y, 1.0f, kinds)?.takeIf { it.ctl != Ctl.GUARD && it.ctl != Ctl.JUTSU && it.ctl != Ctl.OUGI }
                t.el = n
                if (n != null) press(n)
            }
            else -> {}
        }
    }

    private fun up(id: Int, x: Float, y: Float) {
        val t = tracks.remove(id) ?: return
        val e = t.el ?: return
        when (e.kind) {
            CtlKind.DPAD -> {
                dpadBits = 0; recompute()
            }
            CtlKind.STICK -> {
                lx = 128; ly = 128; recompute()
            }
            CtlKind.SYSTEM -> {
                pressed -= e; invalidate()
                if (e.hit(x, y, 1.3f)) when (e.ctl) {
                    Ctl.MENU -> listener?.onMenu()
                    Ctl.SCENE -> scene = PadScene.entries[(scene.ordinal + 1) % PadScene.entries.size]
                    else -> listener?.onOptions()
                }
            }
            else -> letGo(t, e)
        }
    }

    /**
     * The D-pad's direction for the thumb at ([x], [y]), read from its centre: eight ways, from anywhere on the screen
     * once the thumb went down on it. In Toque Pro the dead zone is the player's.
     */
    private fun dpadDir(e: El, t: Track, x: Float, y: Float): Int {
        val p = pro
        val r = e.w / 2
        val dx = x - t.originX
        val dy = y - t.originY
        val dead = if (p != null) r * p.dead / 100f else e.w * 0.12f
        if (hypot(dx, dy) < dead) return 0
        val a = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble())).let { if (it < 0) it + 360 else it }
        // a fighting game reads the four directions more than the diagonals: 60° for each straight one, 30° diagonals
        return when {
            a < 30 || a >= 330 -> Ps2.RIGHT
            a < 60 -> Ps2.RIGHT or Ps2.UP
            a < 120 -> Ps2.UP
            a < 150 -> Ps2.UP or Ps2.LEFT
            a < 210 -> Ps2.LEFT
            a < 240 -> Ps2.LEFT or Ps2.DOWN
            a < 300 -> Ps2.DOWN
            else -> Ps2.DOWN or Ps2.RIGHT
        }
    }

    private fun stick(e: El, t: Track, x: Float, y: Float) {
        val r = e.w * 0.40f
        val p = pro
        var dx = (x - t.originX) / r
        var dy = (y - t.originY) / r
        val m = hypot(dx, dy)
        if (m > 1f) {
            dx /= m; dy /= m
        }
        if (p != null) {
            // the dead zone presses nothing; from its edge the tilt grows from 0, so walking stays possible
            val dead = p.dead / 100f
            val mm = min(m, 1f)
            val k = if (mm <= dead || mm <= 0f) 0f else (mm - dead) / (1f - dead) / mm
            dx *= k; dy *= k
        }
        lx = (128 + dx * 127).roundToInt().coerceIn(0, 255)
        ly = (128 + dy * 127).roundToInt().coerceIn(0, 255)
        recompute()
    }

    // ---- edit mode

    private var selected: El? = null
    private var dragging: Track? = null
    private val toolRects = Array(8) { RectF() } // size −/+, opacity −/+, show/hide, reset, scene, done

    private fun editTouch(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val ti = toolRects.indexOfFirst { it.contains(x, y) }
                if (ti >= 0) {
                    tool(ti); return true
                }
                val e = els.filter { it.hit(x, y, 1.1f) }.minByOrNull { hypot(x - it.cx, y - it.cy) }
                selected = e
                dragging = e?.let { Track(it, x, y, it.cx, it.cy) }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> dragging?.let { t ->
                val e = t.el ?: return true
                val mx = e.w / 2 + 8 * dp
                val my = e.h / 2 + 6 * dp
                e.cx = (t.originX + x - t.downX).coerceIn(insetL + mx, max(insetL + mx, width - insetR - mx))
                e.cy = (t.originY + y - t.downY).coerceIn(insetT + my, max(insetT + my, height - insetB - my))
                t.moved = true
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val t = dragging
                dragging = null
                if (t?.moved == true) t.el?.let { store(it) }
            }
        }
        return true
    }

    private fun tool(i: Int) {
        val e = selected
        when (i) {
            0, 1 -> {
                val f = if (i == 0) 1f / 1.1f else 1.1f
                if (e == null) {
                    config = config.copy(scale = (config.scale * f).coerceIn(0.5f, 2f))
                    listener?.onConfigChanged(config)
                } else {
                    e.scale = (e.scale * f).coerceIn(0.4f, 2.5f)
                    store(e)
                }
            }
            2, 3 -> {
                val delta = if (i == 2) -10 else 10
                if (e == null) {
                    config = config.copy(opacity = (config.opacity + delta).coerceIn(10, 100))
                    listener?.onConfigChanged(config)
                } else {
                    e.opacity = (e.opacity + delta).coerceIn(10, 100)
                    store(e)
                }
            }
            4 -> if (e != null) {
                e.visible = !e.visible
                store(e)
            }
            5 -> {
                val prefix = "${scene.name}."
                config = when {
                    e == null -> config.copy(places = config.places.filterKeys { !it.startsWith(prefix) }, scale = 1f, opacity = 75,
                        macros = if (scene == PadScene.MENU) config.macros else config.macros.map { it.copy(place = null) })
                    e.ctl != null -> config.copy(places = config.places - e.key)
                    else -> config.copy(macros = config.macros.map { if ("M:" + it.id == e.key) it.copy(place = null) else it })
                }
                selected = null
                listener?.onConfigChanged(config)
            }
            6 -> scene = PadScene.entries[(scene.ordinal + 1) % PadScene.entries.size]
            7 -> {
                selected = null
                listener?.onEditDone()
            }
        }
        invalidate()
    }

    /** The element's position, scale, visibility and opacity into the configuration (for the scene shown). */
    private fun store(e: El) {
        val p = Placement(((e.cx - insetL) / safeW).coerceIn(0f, 1f), ((e.cy - insetT) / safeH).coerceIn(0f, 1f), e.scale, e.visible, e.opacity)
        val key = e.key
        config = if (e.ctl != null) config.copy(places = config.places + (key to p))
        else config.copy(macros = config.macros.map { if ("M:" + it.id == key) it.copy(place = p) else it })
        selected = els.firstOrNull { it.key == key }
        listener?.onConfigChanged(config)
    }

    // ---- drawing

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val kanit: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.kanit_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = kanit }
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ORANGE
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }
    private val path = Path()
    private val rect = RectF()

    private fun a(c: Int, k: Float): Int = Color.argb((Color.alpha(c) * k).roundToInt().coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))

    override fun onDraw(canvas: Canvas) {
        if (!editing && (suppressed || hiddenForPad || config.mode == TouchMode.OFF)) return
        val alpha = config.opacity / 100f
        for (e in els) {
            if (!shown(e)) continue
            val k = if (!e.visible) 0.3f else alpha * (e.opacity / 100f)
            val down = e in pressed || players[e.key]?.held == true
            when (e.kind) {
                CtlKind.DPAD -> drawDpad(canvas, e, k)
                CtlKind.STICK -> drawStick(canvas, e, k)
                CtlKind.SHOULDER, CtlKind.PILL -> drawPlate(canvas, e, k, down)
                else -> drawButton(canvas, e, k, down)
            }
            if (editing && e === selected) {
                dash.strokeWidth = 2.5f * dp
                if (e.round) canvas.drawCircle(e.cx, e.cy, e.w / 2 + 6 * dp, dash)
                else canvas.drawRect(e.cx - e.w / 2 - 6 * dp, e.cy - e.h / 2 - 6 * dp, e.cx + e.w / 2 + 6 * dp, e.cy + e.h / 2 + 6 * dp, dash)
            }
        }
        if (editing) drawToolbar(canvas)
    }

    /** The colour of a control's rim and its icon. */
    private fun colorOf(e: El): Int = when (e.ctl) {
        Ctl.ATTACK -> ORANGE
        Ctl.JUMP -> SASUKE_LIGHT
        Ctl.CHAKRA -> CHAKRA
        Ctl.ITEM -> LEE
        Ctl.JUTSU -> AMBER
        Ctl.OUGI -> RED
        Ctl.GUARD -> WOOD
        Ctl.AWAKEN -> PURPLE
        Ctl.SPECIAL_UP -> TEAL
        Ctl.SPECIAL_DOWN -> PINK
        null -> AMBER // the user's macros
        else -> STEEL
    }

    /**
     * A round button: dark ink (or glass) with a rim of its colour, the icon and the console letter (or the input it
     * plays) in a corner; its name under the icon only with [TouchConfig.showNames].
     */
    private fun drawButton(c: Canvas, e: El, k: Float, down: Boolean) {
        val col = colorOf(e)
        val r = e.w / 2 * (if (down) 0.94f else 1f)
        val glass = config.style == PadStyle.GLASS
        // Cached shaders per El (eliminates ~22 Shader allocations per touch frame on weak GPUs)
        if (editing || e.bodyShader == null || e.cacheK != k || e.cacheDown != down || e.cacheGlass != glass || e.cacheCol != col) {
            e.cacheK = k
            e.cacheDown = down
            e.cacheGlass = glass
            e.cacheCol = col
            e.bodyShader = RadialGradient(
                e.cx - r * 0.3f, e.cy - r * 0.35f, r * 1.3f,
                if (down) a(col, 0.75f * k) else if (glass) a(Color.argb(70, 255, 255, 255), k) else a(Color.argb(200, 30, 33, 44), k),
                if (down) a(col, 0.45f * k) else if (glass) a(Color.argb(25, 255, 255, 255), k) else a(Color.argb(170, 10, 12, 18), k),
                Shader.TileMode.CLAMP,
            )
            e.rimShader = LinearGradient(e.cx, e.cy - r, e.cx, e.cy + r, a(lighten(col), k), a(col, 0.55f * k), Shader.TileMode.CLAMP)
        }
        // body
        fill.shader = e.bodyShader
        c.drawCircle(e.cx, e.cy, r, fill)
        fill.shader = null
        // rim: the colour, brighter at the top
        stroke.shader = e.rimShader
        stroke.strokeWidth = max(2f * dp, r * 0.07f)
        c.drawCircle(e.cx, e.cy, r - stroke.strokeWidth / 2, stroke)
        stroke.shader = null
        val ink = if (down) Color.rgb(20, 18, 16) else col
        val menuLook = sceneNow == PadScene.MENU && e.ctl != null && e.ctl.bit != 0 && e.kind == CtlKind.BUTTON
        val name = nameOf(e)
        val showName = config.showNames && r >= 26 * dp && !menuLook && e.kind != CtlKind.SYSTEM && e.macro == null
        val iconY = if (showName) e.cy - r * 0.12f else e.cy
        val iconR = r * (if (showName) 0.42f else 0.5f)
        when {
            menuLook -> psSymbol(c, e.ctl!!.bit, e.cx, e.cy, r * 0.42f, a(if (down) ink else Color.WHITE, k))
            e.kind == CtlKind.SYSTEM -> {
                systemIcon(c, e.ctl, e.cx, e.cy, r * 0.5f, a(if (down) ink else Color.WHITE, k))
                if (e.ctl == Ctl.SCENE) {
                    text.textSize = r * 0.42f
                    text.color = a(Color.WHITE, k)
                    c.drawText(sceneName(scene), e.cx, e.cy + r + text.textSize * 1.05f, text)
                }
            }
            else -> icon(c, e, e.cx, iconY, iconR, a(if (down) ink else Color.WHITE, k), a(ink, k))
        }
        if (showName) {
            text.color = a(if (down) Color.rgb(20, 18, 16) else Color.rgb(236, 230, 222), k)
            text.textSize = min(r * 0.25f, r * 1.9f / max(5, name.length))
            c.drawText(name, e.cx, e.cy + r * 0.62f, text)
        }
        // the console's letter (or the jutsu's input) in the lower right
        val badge = when {
            e.kind == CtlKind.MACRO && e.ctl != null -> e.ctl.steps.filter { it.buttons != 0 }.joinToString("") { Ps2.label(config.skin, it.buttons) }
            e.kind == CtlKind.BUTTON && e.ctl != null && !menuLook -> Ps2.label(config.skin, e.ctl.bit)
            else -> null
        }
        if (badge != null) {
            val br = r * 0.27f
            val bx = e.cx + r * 0.70f
            val by = e.cy + r * 0.70f
            text.textSize = br * (if (badge.length > 2) 0.85f else 1.15f)
            val bw = max(br * 2, text.measureText(badge) + br * 0.8f)
            rect.set(bx - bw / 2, by - br, bx + bw / 2, by + br)
            fill.color = a(Color.argb(235, 12, 13, 18), k)
            c.drawRoundRect(rect, br, br, fill)
            stroke.color = a(col, 0.8f * k)
            stroke.strokeWidth = 1.2f * dp
            c.drawRoundRect(rect, br, br, stroke)
            text.color = a(Color.WHITE, k)
            val fm = text.fontMetrics
            c.drawText(badge, bx, by - (fm.ascent + fm.descent) / 2, text)
        }
    }

    /** L1 / R1, Start, Select: a plate with cut corners. */
    private fun drawPlate(c: Canvas, e: El, k: Float, down: Boolean) {
        val col = if ((e.ctl == Ctl.ITEM_PREV || e.ctl == Ctl.ITEM_NEXT) && sceneNow != PadScene.MENU) LEE else STEEL
        val l = e.cx - e.w / 2
        val t = e.cy - e.h / 2
        val rgt = e.cx + e.w / 2
        val b = e.cy + e.h / 2
        val cut = e.h * 0.35f
        path.reset()
        path.moveTo(l + cut, t); path.lineTo(rgt, t); path.lineTo(rgt, b - cut); path.lineTo(rgt - cut, b); path.lineTo(l, b); path.lineTo(l, t + cut); path.close()
        fill.color = if (down) a(col, 0.8f * k) else a(Color.argb(190, 18, 20, 28), k)
        c.drawPath(path, fill)
        stroke.color = a(col, 0.9f * k)
        stroke.strokeWidth = 1.6f * dp
        c.drawPath(path, stroke)
        val label = when (e.ctl) {
            Ctl.ITEM_PREV -> "◀ " + Ps2.label(config.skin, Ps2.L1)
            Ctl.ITEM_NEXT -> Ps2.label(config.skin, Ps2.R1) + " ▶"
            else -> Ps2.label(config.skin, e.ctl?.bit ?: 0)
        }
        text.color = a(if (down) Color.rgb(20, 18, 16) else Color.WHITE, k)
        text.textSize = e.h * 0.42f
        val fm = text.fontMetrics
        // ("Item" is what L1/R1 do in a fight, with the names shown; in the menus they are plain L1/R1)
        val sub = if ((e.ctl == Ctl.ITEM_PREV || e.ctl == Ctl.ITEM_NEXT) && sceneNow != PadScene.MENU && config.showNames) {
            context.getString(R.string.pad_item)
        } else {
            null
        }
        if (sub != null) {
            c.drawText(label, e.cx, e.cy - e.h * 0.06f - (fm.ascent + fm.descent) / 2 - e.h * 0.12f, text)
            text.textSize = e.h * 0.26f
            text.color = a(if (down) Color.rgb(20, 18, 16) else LEE, k)
            c.drawText(sub, e.cx, e.cy + e.h * 0.34f, text)
        } else {
            c.drawText(label, e.cx, e.cy - (fm.ascent + fm.descent) / 2, text)
        }
    }

    /** The D-pad: a cross of dark glass with its four arrows, the pressed ones in Naruto's orange, a leaf in the middle. */
    private fun drawDpad(c: Canvas, e: El, k: Float) {
        val ox = e.cx
        val oy = e.cy
        val s = e.w / 2
        val arm = s * 0.36f
        fill.color = a(Color.argb(60, 0, 0, 0), k)
        c.drawCircle(ox, oy, s * 1.02f, fill)
        var v = e.dpadPath
        if (v == null || e.dpadCx != ox || e.dpadCy != oy || e.dpadW != e.w) {
            val vert = Path().apply { addRoundRect(ox - arm, oy - s, ox + arm, oy + s, arm * 0.45f, arm * 0.45f, Path.Direction.CW) }
            val hz = Path().apply { addRoundRect(ox - s, oy - arm, ox + s, oy + arm, arm * 0.45f, arm * 0.45f, Path.Direction.CW) }
            vert.op(hz, Path.Op.UNION)
            e.dpadPath = vert
            e.dpadCx = ox; e.dpadCy = oy; e.dpadW = e.w
            v = vert
        }
        fill.color = a(if (config.style == PadStyle.GLASS) Color.argb(60, 255, 255, 255) else Color.argb(190, 18, 20, 28), k)
        c.drawPath(v, fill)
        stroke.color = a(STEEL, 0.55f * k)
        stroke.strokeWidth = 1.4f * dp
        c.drawPath(v, stroke)
        fun arrow(bit: Int, dx: Float, dy: Float) {
            val on = dpadBits and bit != 0
            val cx = ox + dx * s * 0.68f
            val cy = oy + dy * s * 0.68f
            if (on) {
                fill.color = a(ORANGE, 0.85f * k)
                c.drawCircle(cx, cy, arm * 0.95f, fill)
            }
            val t = arm * 0.5f
            path.reset()
            path.moveTo(cx + dx * t, cy + dy * t)
            path.lineTo(cx - dx * t * 0.6f + dy * t, cy - dy * t * 0.6f + dx * t)
            path.lineTo(cx - dx * t * 0.6f - dy * t, cy - dy * t * 0.6f - dx * t)
            path.close()
            fill.color = a(if (on) Color.rgb(20, 18, 16) else Color.WHITE, k)
            c.drawPath(path, fill)
        }
        arrow(Ps2.UP, 0f, -1f); arrow(Ps2.DOWN, 0f, 1f); arrow(Ps2.LEFT, -1f, 0f); arrow(Ps2.RIGHT, 1f, 0f)
        leaf(c, ox, oy, arm * 1.1f, -40f, a(LEE, 0.8f * k))
    }

    /** The stick: a ring with eight marks and a knob with a leaf, where the thumb pushes it. */
    private fun drawStick(c: Canvas, e: El, k: Float) {
        val t = tracks.values.firstOrNull { it.el === e }
        val ox = t?.originX ?: e.cx
        val oy = t?.originY ?: e.cy
        val r = e.w / 2
        fill.color = a(if (config.style == PadStyle.GLASS) Color.argb(45, 255, 255, 255) else Color.argb(140, 14, 16, 24), k)
        c.drawCircle(ox, oy, r, fill)
        stroke.color = a(SASUKE_LIGHT, 0.55f * k)
        stroke.strokeWidth = 1.8f * dp
        c.drawCircle(ox, oy, r - stroke.strokeWidth, stroke)
        for (i in 0 until 8) {
            val ang = i * PI / 4
            val x1 = ox + (cos(ang) * r * 0.80f).toFloat()
            val y1 = oy + (sin(ang) * r * 0.80f).toFloat()
            val x2 = ox + (cos(ang) * r * 0.90f).toFloat()
            val y2 = oy + (sin(ang) * r * 0.90f).toFloat()
            c.drawLine(x1, y1, x2, y2, stroke)
        }
        val kx = ox + (lx - 128) / 127f * r * 0.40f
        val ky = oy + (ly - 128) / 127f * r * 0.40f
        fill.shader = RadialGradient(kx - r * 0.12f, ky - r * 0.14f, r * 0.5f, a(Color.rgb(70, 80, 120), k), a(Color.rgb(28, 32, 52), k), Shader.TileMode.CLAMP)
        c.drawCircle(kx, ky, r * 0.42f, fill)
        fill.shader = null
        stroke.color = a(SASUKE_LIGHT, 0.9f * k)
        c.drawCircle(kx, ky, r * 0.42f, stroke)
        leaf(c, kx, ky, r * 0.42f, -40f, a(LEE, 0.9f * k))
    }

    // ---- icons (drawn: no artwork of the series)

    private fun icon(c: Canvas, e: El, x: Float, y: Float, r: Float, color: Int, accent: Int) {
        stroke.color = color
        stroke.strokeWidth = max(1.6f * dp, r * 0.14f)
        fill.color = color
        when (e.ctl) {
            Ctl.ATTACK -> kunai(c, x, y, r, color)
            Ctl.JUMP -> {
                for (i in 0..1) {
                    val oy = y + r * (0.25f - 0.55f * i)
                    path.reset()
                    path.moveTo(x - r * 0.7f, oy + r * 0.35f); path.lineTo(x, oy - r * 0.3f); path.lineTo(x + r * 0.7f, oy + r * 0.35f)
                    c.drawPath(path, stroke)
                }
            }
            Ctl.CHAKRA -> {
                path.reset()
                var first = true
                var ang = 0.0
                while (ang < 4.2 * PI) {
                    val rr = r * (0.08f + 0.9f * (ang / (4.2 * PI)).toFloat())
                    val px = x + (cos(ang) * rr).toFloat()
                    val py = y + (sin(ang) * rr).toFloat()
                    if (first) path.moveTo(px, py) else path.lineTo(px, py)
                    first = false
                    ang += 0.18
                }
                c.drawPath(path, stroke)
            }
            Ctl.ITEM -> shuriken(c, x, y, r, color)
            Ctl.JUTSU -> {
                // an orb of chakra, with its swirl
                c.drawCircle(x, y, r * 0.85f, stroke)
                rect.set(x - r * 0.55f, y - r * 0.55f, x + r * 0.55f, y + r * 0.55f)
                for (i in 0 until 3) {
                    c.drawArc(rect, i * 120f, 80f, false, stroke)
                }
                c.drawCircle(x, y, r * 0.14f, fill)
            }
            Ctl.OUGI -> {
                // a burst: the ultimate technique
                path.reset()
                for (i in 0 until 16) {
                    val ang = i * PI / 8 - PI / 2
                    val rr = if (i % 2 == 0) r else r * 0.45f
                    val px = x + (cos(ang) * rr).toFloat()
                    val py = y + (sin(ang) * rr).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                c.drawPath(path, fill)
            }
            Ctl.GUARD -> {
                // the substitution log
                val w = r * 1.5f
                val h = r * 0.8f
                rect.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
                c.drawRoundRect(rect, h / 2, h / 2, stroke)
                rect.set(x + w / 2 - h * 0.55f, y - h / 2, x + w / 2 + h * 0.05f, y + h / 2)
                c.drawOval(rect, stroke)
                c.drawLine(x - w * 0.3f, y - h * 0.1f, x + w * 0.05f, y - h * 0.1f, stroke)
            }
            Ctl.SPECIAL_UP, Ctl.SPECIAL_DOWN -> {
                // the input itself: two filled arrowheads (the two presses) inside the ring of the ○ that ends it
                val dir = if (e.ctl == Ctl.SPECIAL_UP) -1f else 1f
                c.drawCircle(x, y, r * 0.95f, stroke)
                for (i in 0..1) {
                    val tip = y + dir * (r * 0.46f - r * 0.5f * i)
                    val base = tip - dir * r * 0.42f
                    path.reset()
                    path.moveTo(x, tip)
                    path.lineTo(x + r * 0.44f, base)
                    path.lineTo(x - r * 0.44f, base)
                    path.close()
                    c.drawPath(path, fill)
                }
            }
            Ctl.AWAKEN -> {
                // a flame
                path.reset()
                path.moveTo(x, y - r)
                path.cubicTo(x + r * 0.9f, y - r * 0.2f, x + r * 0.8f, y + r, x, y + r)
                path.cubicTo(x - r * 0.8f, y + r, x - r * 0.9f, y + r * 0.1f, x - r * 0.25f, y - r * 0.35f)
                path.cubicTo(x - r * 0.1f, y + r * 0.05f, x + r * 0.15f, y - r * 0.4f, x, y - r)
                path.close()
                c.drawPath(path, fill)
            }
            else -> if (e.macro != null) {
                text.color = color
                text.textSize = r * 0.75f
                val fm = text.fontMetrics
                c.drawText(e.macro.name.take(3), x, y - (fm.ascent + fm.descent) / 2, text)
            }
        }
    }

    private fun systemIcon(c: Canvas, ctl: Ctl?, x: Float, y: Float, r: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = max(1.5f * dp, r * 0.16f)
        fill.color = color
        when (ctl) {
            Ctl.MENU -> for (i in -1..1) c.drawLine(x - r * 0.6f, y + i * r * 0.45f, x + r * 0.6f, y + i * r * 0.45f, stroke)
            Ctl.OPTIONS -> {
                // two sliders
                c.drawLine(x - r * 0.7f, y - r * 0.35f, x + r * 0.7f, y - r * 0.35f, stroke)
                c.drawLine(x - r * 0.7f, y + r * 0.35f, x + r * 0.7f, y + r * 0.35f, stroke)
                c.drawCircle(x + r * 0.25f, y - r * 0.35f, r * 0.2f, fill)
                c.drawCircle(x - r * 0.3f, y + r * 0.35f, r * 0.2f, fill)
            }
            Ctl.SCENE -> when (scene) {
                PadScene.FIGHT -> {
                    // two kunai crossed
                    kunai(c, x, y, r * 0.95f, color)
                    c.save(); c.rotate(-90f, x, y); kunai(c, x, y, r * 0.95f, color); c.restore()
                }
                PadScene.CAMPAIGN -> {
                    // the map: a gate
                    c.drawLine(x - r, y - r * 0.6f, x + r, y - r * 0.6f, stroke)
                    c.drawLine(x - r * 0.75f, y - r * 0.2f, x + r * 0.75f, y - r * 0.2f, stroke)
                    c.drawLine(x - r * 0.5f, y - r * 0.6f, x - r * 0.5f, y + r * 0.8f, stroke)
                    c.drawLine(x + r * 0.5f, y - r * 0.6f, x + r * 0.5f, y + r * 0.8f, stroke)
                }
                PadScene.MENU -> {
                    // a scroll's lines
                    rect.set(x - r * 0.75f, y - r * 0.8f, x + r * 0.75f, y + r * 0.8f)
                    c.drawRect(rect, stroke)
                    c.drawLine(x - r * 0.4f, y - r * 0.25f, x + r * 0.4f, y - r * 0.25f, stroke)
                    c.drawLine(x - r * 0.4f, y + r * 0.2f, x + r * 0.2f, y + r * 0.2f, stroke)
                }
            }
            else -> {}
        }
    }

    private fun kunai(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        c.save()
        c.rotate(45f, x, y)
        fill.color = color
        stroke.color = color
        path.reset()
        path.moveTo(x, y - r * 1.05f)
        path.lineTo(x + r * 0.32f, y - r * 0.1f)
        path.lineTo(x, y + r * 0.05f)
        path.lineTo(x - r * 0.32f, y - r * 0.1f)
        path.close()
        c.drawPath(path, fill)
        val sw = stroke.strokeWidth
        stroke.strokeWidth = r * 0.16f
        c.drawLine(x, y + r * 0.05f, x, y + r * 0.62f, stroke)
        stroke.strokeWidth = r * 0.11f
        c.drawCircle(x, y + r * 0.82f, r * 0.2f, stroke)
        stroke.strokeWidth = sw
        c.restore()
    }

    private fun shuriken(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        path.reset()
        for (i in 0 until 4) {
            val ang = i * PI / 2 - PI / 2
            val tip = (x + cos(ang) * r).toFloat() to (y + sin(ang) * r).toFloat()
            val l = ang + PI / 4
            val rr = r * 0.3
            val side = (x + cos(l) * rr).toFloat() to (y + sin(l) * rr).toFloat()
            val b = ang - PI / 4
            val side2 = (x + cos(b) * rr).toFloat() to (y + sin(b) * rr).toFloat()
            val q = (x + cos(ang - 0.35) * r * 0.75).toFloat() to (y + sin(ang - 0.35) * r * 0.75).toFloat()
            path.moveTo(side2.first, side2.second)
            path.quadTo(q.first, q.second, tip.first, tip.second)
            path.lineTo(side.first, side.second)
            path.lineTo(x, y)
            path.close()
        }
        fill.color = color
        c.drawPath(path, fill)
        fill.color = Color.argb(Color.alpha(color), 20, 22, 30)
        c.drawCircle(x, y, r * 0.16f, fill)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, len: Float, deg: Float, color: Int) {
        c.save()
        c.rotate(deg, x, y)
        val h = len / 2
        val w = len * 0.42f
        path.reset()
        path.moveTo(x - h, y)
        path.quadTo(x - h * 0.1f, y - w, x + h, y)
        path.quadTo(x - h * 0.1f, y + w * 0.9f, x - h, y)
        path.close()
        fill.color = color
        c.drawPath(path, fill)
        c.restore()
    }

    private fun psSymbol(c: Canvas, bit: Int, x: Float, y: Float, r: Float, color: Int) {
        if (config.skin != PadSkin.PLAYSTATION) {
            text.color = color
            text.textSize = r * 1.7f
            val fm = text.fontMetrics
            c.drawText(Ps2.label(config.skin, bit), x, y - (fm.ascent + fm.descent) / 2, text)
            return
        }
        stroke.color = color
        stroke.strokeWidth = max(2.5f * dp, r * 0.22f)
        when (bit) {
            Ps2.CROSS -> {
                c.drawLine(x - r, y - r, x + r, y + r, stroke)
                c.drawLine(x - r, y + r, x + r, y - r, stroke)
            }
            Ps2.CIRCLE -> c.drawCircle(x, y, r, stroke)
            Ps2.SQUARE -> c.drawRect(x - r * 0.9f, y - r * 0.9f, x + r * 0.9f, y + r * 0.9f, stroke)
            else -> {
                path.reset()
                path.moveTo(x, y - r * 1.05f)
                path.lineTo(x + r * 1.05f, y + r * 0.75f)
                path.lineTo(x - r * 1.05f, y + r * 0.75f)
                path.close()
                c.drawPath(path, stroke)
            }
        }
    }

    private fun lighten(col: Int): Int = Color.argb(Color.alpha(col), min(255, Color.red(col) + 60), min(255, Color.green(col) + 60), min(255, Color.blue(col) + 60))

    private fun nameOf(e: El): String = e.macro?.name ?: when (e.ctl) {
        Ctl.ATTACK -> context.getString(R.string.pad_attack)
        Ctl.JUMP -> context.getString(R.string.pad_jump)
        Ctl.CHAKRA -> context.getString(R.string.pad_chakra)
        Ctl.ITEM -> context.getString(R.string.pad_item)
        Ctl.JUTSU -> context.getString(R.string.pad_jutsu)
        Ctl.OUGI -> context.getString(R.string.pad_ougi)
        Ctl.GUARD -> context.getString(R.string.pad_guard)
        Ctl.AWAKEN -> context.getString(R.string.pad_awaken)
        Ctl.SPECIAL_UP -> context.getString(R.string.tc_special_up)
        Ctl.SPECIAL_DOWN -> context.getString(R.string.tc_special_down)
        Ctl.ITEM_PREV, Ctl.ITEM_NEXT -> context.getString(R.string.pad_item)
        Ctl.DPAD -> "D-pad"
        Ctl.STICK -> context.getString(R.string.pad_stick)
        Ctl.MENU -> context.getString(R.string.touch_ctl_menu)
        Ctl.OPTIONS -> context.getString(R.string.touch_ctl_options)
        Ctl.SCENE -> sceneName(scene)
        Ctl.START -> "Start"
        Ctl.SELECT -> "Select"
        null -> ""
    }

    private fun sceneName(s: PadScene) = context.getString(when (s) {
        PadScene.FIGHT -> R.string.pad_scene_fight
        PadScene.CAMPAIGN -> R.string.pad_scene_campaign
        PadScene.MENU -> R.string.pad_scene_menu
    })

    private fun drawToolbar(c: Canvas) {
        val labels = listOf(
            "−", "+", "α−", "α+",
            context.getString(if (selected?.visible == false) R.string.touch_show else R.string.touch_hide),
            context.getString(R.string.touch_reset),
            context.getString(R.string.touch_scene, sceneName(scene)),
            context.getString(R.string.touch_done),
        )
        val baseH = 24f * dp
        val gap = 3f * dp
        text.textSize = 11f * dp
        val widths0 = labels.mapIndexed { i, l -> if (i < 4) baseH else text.measureText(l) + 14f * dp }
        val left = insetL + 8f * dp
        val avail = (width - insetR - 8f * dp - left).coerceAtLeast(1f)
        val fit = min(1f, avail / (widths0.sum() + gap * (labels.size - 1)))
        val h = baseH * fit
        val widths = widths0.map { it * fit }
        val y = insetT + 2f * dp
        rect.set(left - 4f * dp, y - 2f * dp, left + widths.sum() + gap * fit * (labels.size - 1) + 4f * dp, y + h + 16f * dp)
        fill.color = Color.argb(236, 14, 16, 22)
        c.drawRect(rect, fill)
        var x = left
        labels.forEachIndexed { i, label ->
            toolRects[i].set(x, y, x + widths[i], y + h)
            fill.color = when {
                i == 4 && selected != null -> ORANGE
                i == 6 -> Color.rgb(52, 68, 158)
                i == 7 -> Color.rgb(47, 162, 74)
                else -> Color.argb(240, 32, 35, 46)
            }
            c.drawRoundRect(toolRects[i], 4f * dp, 4f * dp, fill)
            text.color = if (i == 4 && selected != null) Color.rgb(20, 18, 16) else Color.WHITE
            text.textSize = (if (i < 4) 14f else 11f) * dp * fit
            val fm = text.fontMetrics
            c.drawText(label, toolRects[i].centerX(), toolRects[i].centerY() - (fm.ascent + fm.descent) / 2, text)
            x += widths[i] + gap * fit
        }
        val sel = selected
        val status = if (sel == null) context.getString(R.string.touch_edit_all, config.opacity)
        else context.getString(R.string.touch_edit_one, nameOf(sel).take(18), sel.opacity)
        text.textAlign = Paint.Align.LEFT
        text.color = Color.rgb(224, 226, 236)
        text.textSize = 9f * dp * fit
        c.drawText(status, left, y + h + 11.5f * dp, text)
        text.textAlign = Paint.Align.CENTER
    }

    // ---- one control on its own (the launcher's guide draws the real buttons with it)

    private var paintScene: PadScene? = null
    private val sceneNow: PadScene get() = paintScene ?: scene

    /**
     * Draws [ctl] (or the user's [macro]) alone, centred at ([cx], [cy]) and [size] px across, as it looks in [inScene]:
     * the same drawing as on the game's screen, for the Controls page's guide. The view need not be laid out.
     */
    fun paintSample(c: Canvas, ctl: Ctl?, macro: Macro?, cx: Float, cy: Float, size: Float, inScene: PadScene = PadScene.FIGHT) {
        val kind = ctl?.kind ?: CtlKind.MACRO
        val (bw, bh) = baseSize(kind, ctl?.size ?: 58f)
        val k = size / max(bw, bh)
        val e = El("sample", ctl, macro, kind, cx, cy, bw * k, bh * k, 1f, true, 100)
        paintScene = inScene
        try {
            when (kind) {
                CtlKind.DPAD -> drawDpad(c, e, 1f)
                CtlKind.STICK -> drawStick(c, e, 1f)
                CtlKind.SHOULDER, CtlKind.PILL -> drawPlate(c, e, 1f, false)
                else -> drawButton(c, e, 1f, false)
            }
        } finally {
            paintScene = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rebuild()
        handler.removeCallbacks(scenePoll)
        handler.post(scenePoll)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        players.values.forEach { it.cancel() }
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val SCENE_HOLD_MS = 500L
        private const val SCENE_LEAVE_MS = 2500L
        private const val TALK_END_GUARD_MS = 700L
        private const val MIN_HOLD_MS = 80L           // (a button down at least this long: 2 vsyncs and a margin)
        private const val MENU_PRESS_MS = 90L          // (2 to 4 of the game's frames)
        private const val MENU_HOLD_MS = 550L
        private const val MENU_REPEAT_FIRST_MS = 380L
        private const val MENU_REPEAT_MS = 150L
        private val ORANGE = Color.rgb(255, 122, 26)
        private val AMBER = Color.rgb(255, 177, 59)
        private val SASUKE_LIGHT = Color.rgb(169, 182, 255)
        private val PURPLE = Color.rgb(124, 92, 196)
        private val CHAKRA = Color.rgb(111, 200, 255)
        private val LEE = Color.rgb(47, 162, 74)
        private val RED = Color.rgb(215, 38, 61)
        private val WOOD = Color.rgb(185, 131, 82)
        private val STEEL = Color.rgb(185, 194, 208)
        private val TEAL = Color.rgb(64, 204, 170)
        private val PINK = Color.rgb(232, 112, 186)

        /** What a sliding finger presses: the action buttons; in Toque Pro with its sliding on, the rest too. */
        private val SLIDE_FACE = setOf(CtlKind.BUTTON)
        private val SLIDE_ALL = setOf(CtlKind.BUTTON, CtlKind.MACRO, CtlKind.SHOULDER)
    }
}
