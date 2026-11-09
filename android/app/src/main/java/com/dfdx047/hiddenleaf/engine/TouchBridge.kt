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

    /** The one-tap special (0/1) and what a tap is worth where the game asks to press repeatedly (100..200 %), at once. */
    @JvmStatic external fun nativeSetAssist(specialOne: Int, mashPercent: Int)

    /** Resolution (quarters: 4 = 1x), screen filter, the picture's shape and the frame rate, in effect from the next frame. */
    @JvmStatic external fun nativeSetVideo(scale4: Int, filter: Int, aspectMilli: Int, fpsMode: Int)

    fun perfLine(): String = runCatching { nativePerfLine() }.getOrDefault("")

    fun divider(): Int = runCatching { nativeDivider() }.getOrDefault(2)

    fun presentRate(): Int = runCatching { nativePresentRate() }.getOrDefault(0)

    fun setPaused(paused: Boolean) {
        runCatching { nativeSetPaused(paused) }
    }

    fun setAssist(specialOne: Int, mashPercent: Int) {
        runCatching { nativeSetAssist(specialOne, mashPercent) }
    }

    fun setVideo(scale4: Int, filter: Int, aspectMilli: Int, fpsMode: Int) {
        runCatching { nativeSetVideo(scale4, filter, aspectMilli, fpsMode) }
    }

    private var nativeMissing = false
    private var logged = false

    fun scene(): Int = if (nativeMissing) 0 else try {
        nativeScene().coerceIn(0, 3)
    } catch (t: Throwable) {
        // (the launcher's own process has no engine: ask once, not three times a second with an error each time)
        if (t is UnsatisfiedLinkError) nativeMissing = true
        if (!logged) {
            logged = true
            Log.w("un5", "touch: the engine does not say the scene", t)
        }
        0
    }

    fun setPad(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int) {
        runCatching { nativeSetPad(buttons, lx, ly, rx, ry) }
    }

    fun menuOpen(): Boolean = runCatching { nativeMenuOpen() }.getOrDefault(false)
}
