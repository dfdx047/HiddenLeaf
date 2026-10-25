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
