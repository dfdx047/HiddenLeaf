package com.dfdx047.hiddenleaf.engine

import android.util.Log

/** The on-screen controller's line to the engine (functions of the engine's host, libmain.so). */
object TouchBridge {
    /** Player 1's buttons (PS2 bits, 1 = pressed) and sticks (0..255, 128 = centre). */
    @JvmStatic external fun nativeSetPad(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int)

    /** The engine's own window (settings) is open. */
    @JvmStatic external fun nativeMenuOpen(): Boolean

    /**
     * What the game shows: 0 not known, 1 a menu, 2 a fight, 3 the campaign's map (Master mode). The engine reads it
     * from the game's state (which overlay is loaded and the game's mode), so the controls change with it.
     */
    @JvmStatic external fun nativeScene(): Int

    /** The engine's numbers of the last second (HL_PERF in hl_env.txt), for the overlay. Empty when off. */
    @JvmStatic external fun nativePerfLine(): String

    /** Vsyncs each of the game's frames takes (2: 25 pictures a second at PAL's 50 vsyncs). */
    @JvmStatic external fun nativeDivider(): Int

    /** Pictures a second drawn in between (60, 120), or 0 when each of the game's goes to the screen. */
    @JvmStatic external fun nativePresentRate(): Int

    /** The game frozen (the EE, the IOP and the sound together) or going on. */
    @JvmStatic external fun nativeSetPaused(paused: Boolean)

    /** The one-tap special (0/1), its button mask, and what a tap is worth where the game asks to press repeatedly (100..200 %), at once. */
    @JvmStatic external fun nativeSetAssist(specialOne: Int, specialButton: Int, mashPercent: Int)

    /** Resolution (quarters: 4 = 1x), screen filter, the picture's shape and the frame rate, in effect from the next frame. */
    @JvmStatic external fun nativeSetVideo(scale4: Int, filter: Int, aspectMilli: Int, fpsMode: Int)
    @JvmStatic external fun nativeSetZeroFg(multiplier: Int)

    /** "Naruto Online" in the game's own Mode Select (under Free Battle; docs/menu-online.md), on or off. */
    @JvmStatic external fun nativeSetMenuOnline(on: Boolean)

    /** How many times the player chose "Naruto Online" there; the lobby opens when it goes up (not wired yet). */
    @JvmStatic external fun nativeOnlineMenu(): Int

    /** Direct tap on menu items (normalized 0..1): 0=default CROSS, 1=UP, 2=CROSS, 3=DOWN. */
    @JvmStatic external fun nativeMenuTap(nx: Float, ny: Float): Int

    /** Player 1's active UN5 character ID during a fight (0 if not in a fight). */
    @JvmStatic external fun nativeP1CharId(): Int

    /** Live fight counter, KO winner (1=P1, 2=P2), and P1/P2 character IDs ("fight=..;win_fight=..;winner=..;p1char=..;p2char=.."). */
    @JvmStatic external fun nativeMatchResult(): String

    fun menuTap(nx: Float, ny: Float): Int = runCatching { nativeMenuTap(nx, ny) }.getOrDefault(0)

    fun p1CharId(): Int = runCatching { nativeP1CharId() }.getOrDefault(0)

    fun matchResult(): Map<String, Int> = runCatching {
        nativeMatchResult().split(';').mapNotNull {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to (it.substring(i + 1).toIntOrNull() ?: 0) else null
        }.toMap()
    }.getOrDefault(emptyMap())

    fun perfLine(): String = runCatching { nativePerfLine() }.getOrDefault("")

    fun divider(): Int = runCatching { nativeDivider() }.getOrDefault(2)

    fun presentRate(): Int = runCatching { nativePresentRate() }.getOrDefault(0)

    @JvmStatic external fun nativeSetPadHeld(held: Boolean)

    /** The online menu over the game: the game goes on, but the pads do not reach it. */
    fun setPadHeld(held: Boolean) {
        runCatching { nativeSetPadHeld(held) }
    }

    fun setPaused(paused: Boolean) {
        runCatching { nativeSetPaused(paused) }
    }

    fun setAssist(specialOne: Int, specialButton: Int, mashPercent: Int) {
        runCatching { nativeSetAssist(specialOne, specialButton, mashPercent) }
    }

    fun setVideo(scale4: Int, filter: Int, aspectMilli: Int, fpsMode: Int) {
        runCatching { nativeSetVideo(scale4, filter, aspectMilli, fpsMode) }
    }

    fun setZeroFg(multiplier: Int) {
        runCatching { nativeSetZeroFg(multiplier) }
    }

    private var nativeMissing = false
    private var logged = false

    fun scene(): Int = if (nativeMissing) 0 else try {
        nativeScene().coerceIn(0, 4)
    } catch (t: Throwable) {
        // (the launcher's own process has no engine: ask once, not three times a second with an error each time)
        if (t is UnsatisfiedLinkError) nativeMissing = true
        if (!logged) {
            logged = true
            Log.w("un5", "touch: the engine does not say the scene", t)
        }
        0
    }

    // Player 1 from two places: the on-screen controller and a physical one the app reads itself (a map of the player's,
    // touch/PadMap.kt). The buttons together; the sticks of whichever is off its centre, the touch's first.
    private val touch = intArrayOf(0, 128, 128, 128, 128)
    private val physical = intArrayOf(0, 128, 128, 128, 128)

    /** The on-screen controller's buttons and sticks. */
    fun setPad(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int) = synchronized(this) {
        touch[0] = buttons; touch[1] = lx; touch[2] = ly; touch[3] = rx; touch[4] = ry
        push()
    }

    /** The physical controller's, when the app reads it ([com.dfdx047.hiddenleaf.touch.PadTranslator]). */
    fun setPhysical(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int) = synchronized(this) {
        physical[0] = buttons; physical[1] = lx; physical[2] = ly; physical[3] = rx; physical[4] = ry
        push()
    }

    /** Everything let go (the menu opens, the app goes to the background). */
    fun releaseAll() = synchronized(this) {
        touch.fill(128); touch[0] = 0
        physical.fill(128); physical[0] = 0
        push()
    }

    private fun push() {
        val touchAtRest = touch[1] == 128 && touch[2] == 128 && touch[3] == 128 && touch[4] == 128
        val sticks = if (touchAtRest) physical else touch
        runCatching { nativeSetPad(touch[0] or physical[0], sticks[1], sticks[2], sticks[3], sticks[4]) }
    }

    fun menuOpen(): Boolean = runCatching { nativeMenuOpen() }.getOrDefault(false)
}
