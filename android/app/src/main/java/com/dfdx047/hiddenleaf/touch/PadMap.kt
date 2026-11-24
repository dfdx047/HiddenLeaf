package com.dfdx047.hiddenleaf.touch

import android.view.KeyEvent
import java.io.File
import org.json.JSONObject

/** The button pair of a physical controller that opens the game menu (the Back key and the Guide key always do). */
enum class MenuCombo(val keys: Set<Int>) {
    SELECT_START(setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START)),
    L3_R3(setOf(KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR)),
    OFF(emptySet()),
}

/**
 * The physical controller's buttons: which Android key presses which PS2 button, and the menu's hotkey. With the
 * default map the engine reads the controller itself (SDL, un5_main.cpp updatePad, the same map); with one of the
 * player's, GameActivity translates the controller here ([PadTranslator]) and SDL does not see it. Kept in
 * controller.json in the game's folder.
 */
data class PadMap(
    val keys: Map<Int, Int> = DEFAULT_KEYS,
    val menuCombo: MenuCombo = MenuCombo.SELECT_START,
) {
    /** Another map than SDL's: the app translates the controller. */
    val custom: Boolean get() = keys != DEFAULT_KEYS

    fun bitOf(keyCode: Int): Int = keys[keyCode] ?: 0

    /** The keys that press [bit] (none when it was left without one). */
    fun keysFor(bit: Int): List<Int> = keys.entries.filter { it.value == bit }.map { it.key }.sorted()

    /** [keyCode] presses [bit], and only it: the key's old button and the button's old keys are let go. */
    fun bind(bit: Int, keyCode: Int): PadMap = copy(keys = keys.filter { it.value != bit && it.key != keyCode } + (keyCode to bit))

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("menuCombo", menuCombo.name)
        put("keys", JSONObject().apply { keys.forEach { (k, v) -> put(k.toString(), v) } })
    }

    companion object {
        /** SDL's own map of an Android controller (un5_main.cpp updatePad): south ✕, east ○, west □, north △. */
        val DEFAULT_KEYS: Map<Int, Int> = linkedMapOf(
            KeyEvent.KEYCODE_BUTTON_A to Ps2.CROSS,
            KeyEvent.KEYCODE_BUTTON_B to Ps2.CIRCLE,
            KeyEvent.KEYCODE_BUTTON_X to Ps2.SQUARE,
            KeyEvent.KEYCODE_BUTTON_Y to Ps2.TRIANGLE,
            KeyEvent.KEYCODE_BUTTON_L1 to Ps2.L1,
            KeyEvent.KEYCODE_BUTTON_R1 to Ps2.R1,
            KeyEvent.KEYCODE_BUTTON_L2 to Ps2.L2,
            KeyEvent.KEYCODE_BUTTON_R2 to Ps2.R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL to Ps2.L3,
            KeyEvent.KEYCODE_BUTTON_THUMBR to Ps2.R3,
            KeyEvent.KEYCODE_BUTTON_START to Ps2.START,
            KeyEvent.KEYCODE_BUTTON_SELECT to Ps2.SELECT,
            KeyEvent.KEYCODE_DPAD_UP to Ps2.UP,
            KeyEvent.KEYCODE_DPAD_DOWN to Ps2.DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT to Ps2.LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to Ps2.RIGHT,
        )

        /** The PS2 buttons the menu lists, in its order. */
        val BUTTONS = listOf(
            Ps2.CIRCLE, Ps2.CROSS, Ps2.TRIANGLE, Ps2.SQUARE, Ps2.R2, Ps2.L2, Ps2.L1, Ps2.R1, Ps2.R3, Ps2.L3,
            Ps2.START, Ps2.SELECT, Ps2.UP, Ps2.DOWN, Ps2.LEFT, Ps2.RIGHT,
        )

        /** The keys a map may use: the controller's buttons and its D-pad (not Back, Home or the volume). */
        fun bindable(keyCode: Int): Boolean =
            KeyEvent.isGamepadButton(keyCode) && keyCode != KeyEvent.KEYCODE_BUTTON_MODE ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT

        /** A key's name as the controller prints it. */
        fun keyLabel(keyCode: Int): String = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "A"
            KeyEvent.KEYCODE_BUTTON_B -> "B"
            KeyEvent.KEYCODE_BUTTON_X -> "X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y"
            KeyEvent.KEYCODE_BUTTON_C -> "C"
            KeyEvent.KEYCODE_BUTTON_Z -> "Z"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
            KeyEvent.KEYCODE_BUTTON_START -> "START"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
            KeyEvent.KEYCODE_DPAD_UP -> "↑"
            KeyEvent.KEYCODE_DPAD_DOWN -> "↓"
            KeyEvent.KEYCODE_DPAD_LEFT -> "←"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "→"
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").removePrefix("BUTTON_")
        }

        fun fromJson(o: JSONObject): PadMap {
            val keys = linkedMapOf<Int, Int>()
            o.optJSONObject("keys")?.let { k ->
                k.keys().forEach { name ->
                    val code = name.toIntOrNull() ?: return@forEach
                    val bit = k.optInt(name)
                    if (bindable(code) && bit != 0 && bit and (bit - 1) == 0 && bit <= 0x8000) keys[code] = bit
                }
            }
            return PadMap(
                keys = if (o.has("keys")) keys else DEFAULT_KEYS,
                menuCombo = runCatching { MenuCombo.valueOf(o.optString("menuCombo")) }.getOrDefault(MenuCombo.SELECT_START),
            )
        }
    }
}

/** controller.json in the game's folder (read by the game's process when it starts and when the menu changes it). */
class PadMapStore(root: File) {
    val file = File(root, "controller.json")

    fun load(): PadMap = runCatching {
        if (file.exists()) PadMap.fromJson(JSONObject(file.readText())) else PadMap()
    }.getOrDefault(PadMap())

    fun save(m: PadMap) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(m.toJson().toString(2))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }
}

/**
 * A physical controller read by the app (a map of the player's): the keys held, what the D-pad's hat and the triggers
 * press, and the sticks, into player 1's PS2 buttons and sticks (0..255, 128 the centre).
 */
class PadTranslator(var map: PadMap) {
    private val held = HashSet<Int>()
    private val fromAxes = HashSet<Int>()
    var lx = 128; private set
    var ly = 128; private set
    var rx = 128; private set
    var ry = 128; private set

    /** A key went down or up; false when the map does not know it (the caller lets it go on its way). */
    fun key(keyCode: Int, down: Boolean): Boolean {
        if (!PadMap.bindable(keyCode)) return false
        if (down) held += keyCode else held -= keyCode
        return true
    }

    /**
     * The controller's axes: the hat (-1..1), the triggers (0..1) and the sticks (-1..1). The hat presses the D-pad's
     * keys and the triggers L2 and R2, as their own keys would, so a map of them applies to both.
     */
    fun axes(hatX: Float, hatY: Float, lTrigger: Float, rTrigger: Float, x: Float, y: Float, z: Float, rz: Float) {
        fromAxes.clear()
        if (hatX < -0.5f) fromAxes += KeyEvent.KEYCODE_DPAD_LEFT
        if (hatX > 0.5f) fromAxes += KeyEvent.KEYCODE_DPAD_RIGHT
        if (hatY < -0.5f) fromAxes += KeyEvent.KEYCODE_DPAD_UP
        if (hatY > 0.5f) fromAxes += KeyEvent.KEYCODE_DPAD_DOWN
        if (lTrigger > 0.50f) fromAxes += KeyEvent.KEYCODE_BUTTON_L2
        if (rTrigger > 0.50f) fromAxes += KeyEvent.KEYCODE_BUTTON_R2
        lx = stick(x); ly = stick(y); rx = stick(z); ry = stick(rz)
    }

    /** The PS2 buttons pressed now. */
    fun buttons(): Int {
        var b = 0
        for (k in held) b = b or map.bitOf(k)
        for (k in fromAxes) b = b or map.bitOf(k)
        return b
    }

    fun reset() {
        held.clear()
        fromAxes.clear()
        lx = 128; ly = 128; rx = 128; ry = 128
    }

    private fun stick(v: Float): Int = if (kotlin.math.abs(v) < 0.12f) 128 else ((v.coerceIn(-1f, 1f) + 1f) * 127.5f).toInt().coerceIn(0, 255)
}
