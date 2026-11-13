package com.dfdx047.hiddenleaf

import com.dfdx047.hiddenleaf.touch.Ctl
import com.dfdx047.hiddenleaf.touch.PadScene
import com.dfdx047.hiddenleaf.touch.ProTouch
import com.dfdx047.hiddenleaf.touch.TouchConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** touch_controls.json: a file of before the specials and Toque Pro still reads, and the new fields go round. */
@RunWith(RobolectricTestRunner::class)
class TouchConfigTest {
    // (a file as the launcher of 2026-11-11 wrote it: version 3, R3 moved, a macro, no "showNames" nor "pro")
    private val old = """
        {"version":3,"mode":"AUTO","skin":"XBOX","style":"INK","opacity":60,"scale":1.1,"haptics":true,"sticksFloat":true,
         "places":{"FIGHT.AWAKEN":{"x":0.9,"y":0.4,"s":1,"v":true,"o":100}},"layout":"MOBILE","autoMode":true,
         "menuGestures":false,"classicPlaces":{},
         "macros":[{"id":"m1","name":"M1","repeat":false,"steps":[[4096,70],[0,50],[8192,90]]}]}
    """.trimIndent()

    @Test
    fun readsTheOldFile() {
        val c = TouchConfig.fromJson(JSONObject(old))
        assertEquals(60, c.opacity)
        assertFalse(c.showNames)
        assertEquals(ProTouch(), c.pro)
        assertFalse(c.pro.on)
        assertEquals(1, c.macros.size)
        // the player's own R3 stays shown; a new one starts hidden; the specials are in the fight
        assertTrue(c.placeOf(PadScene.FIGHT, Ctl.AWAKEN)!!.visible)
        assertFalse(TouchConfig().placeOf(PadScene.FIGHT, Ctl.AWAKEN)!!.visible)
        assertTrue(TouchConfig().placeOf(PadScene.FIGHT, Ctl.SPECIAL_UP)!!.visible)
        assertTrue(TouchConfig().placeOf(PadScene.FIGHT, Ctl.SPECIAL_DOWN)!!.visible)
    }

    @Test
    fun newFieldsGoRound() {
        val c = TouchConfig(showNames = true, pro = ProTouch(on = true, slideAll = false, padSize = 140, dead = 25, hit = 140))
        val back = TouchConfig.fromJson(JSONObject(c.toJson().toString()))
        assertEquals(c.showNames, back.showNames)
        assertEquals(c.pro, back.pro)
    }
}
