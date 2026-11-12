package com.dfdx047.hiddenleaf

import android.view.KeyEvent
import com.dfdx047.hiddenleaf.touch.MenuCombo
import com.dfdx047.hiddenleaf.touch.PadMap
import com.dfdx047.hiddenleaf.touch.PadMapStore
import com.dfdx047.hiddenleaf.touch.PadTranslator
import com.dfdx047.hiddenleaf.touch.Ps2
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The physical controller's map (touch/PadMap.kt): binding, the file, and the translation into PS2 buttons. */
@RunWith(RobolectricTestRunner::class)
class PadMapTest {
    @Test
    fun defaultIsSdlsMap() {
        val m = PadMap()
        assertFalse(m.custom)
        assertEquals(Ps2.CROSS, m.bitOf(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(Ps2.CIRCLE, m.bitOf(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(listOf(KeyEvent.KEYCODE_BUTTON_START), m.keysFor(Ps2.START))
    }

    @Test
    fun bindMovesTheKey() {
        // X (□ by default) becomes △: □ is left without a key, Y (△ before) too
        val m = PadMap().bind(Ps2.TRIANGLE, KeyEvent.KEYCODE_BUTTON_X)
        assertTrue(m.custom)
        assertEquals(Ps2.TRIANGLE, m.bitOf(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(emptyList<Int>(), m.keysFor(Ps2.SQUARE))
        assertEquals(0, m.bitOf(KeyEvent.KEYCODE_BUTTON_Y))
        // and back: the default again
        val back = m.bind(Ps2.SQUARE, KeyEvent.KEYCODE_BUTTON_X).bind(Ps2.TRIANGLE, KeyEvent.KEYCODE_BUTTON_Y)
        assertFalse(back.custom)
    }

    @Test
    fun fileRoundTrip() {
        val dir = Files.createTempDirectory("padmap").toFile()
        val store = PadMapStore(dir)
        assertEquals(PadMap(), store.load())
        val m = PadMap(menuCombo = MenuCombo.L3_R3).bind(Ps2.R2, KeyEvent.KEYCODE_BUTTON_R1).bind(Ps2.R1, KeyEvent.KEYCODE_BUTTON_R2)
        store.save(m)
        assertEquals(m, store.load())
        dir.deleteRecursively()
    }

    @Test
    fun translator() {
        val t = PadTranslator(PadMap().bind(Ps2.R2, KeyEvent.KEYCODE_BUTTON_R1))
        assertTrue(t.key(KeyEvent.KEYCODE_BUTTON_R1, true))
        assertEquals(Ps2.R2, t.buttons())
        // the trigger as an axis is the R2 key: now without a button of its own
        t.axes(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f)
        assertEquals(Ps2.R2, t.buttons())
        // the hat presses the D-pad's keys
        t.axes(-1f, 1f, 0f, 0f, 0f, -1f, 0f, 0f)
        assertEquals(Ps2.R2 or Ps2.LEFT or Ps2.DOWN, t.buttons())
        assertEquals(0, t.ly)
        assertEquals(128, t.lx)
        t.key(KeyEvent.KEYCODE_BUTTON_R1, false)
        assertFalse(t.key(KeyEvent.KEYCODE_VOLUME_UP, true))
        t.reset()
        assertEquals(0, t.buttons())
    }
}
