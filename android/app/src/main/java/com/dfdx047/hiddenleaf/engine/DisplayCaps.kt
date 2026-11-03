package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/** What the device's screen can show. */
object DisplayCaps {
    /** The highest refresh rate the built-in screen offers, in Hz. */
    fun maxRefresh(context: Context): Float {
        val dm = context.getSystemService(DisplayManager::class.java) ?: return 60f
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return 60f
        return display.supportedModes.maxOfOrNull { it.refreshRate } ?: display.refreshRate
    }

    /** 120 interpolated pictures a second only where the screen shows them: on a 60 Hz panel half of them are never
     *  seen, and the RG Cube lost steadiness with it (5.4% of vsync pairs over 44 ms, against 0.7% at 60). */
    fun offers120(context: Context): Boolean = maxRefresh(context) >= 100f
}
