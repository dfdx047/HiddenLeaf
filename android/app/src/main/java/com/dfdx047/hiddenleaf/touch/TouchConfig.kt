package com.dfdx047.hiddenleaf.touch

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** The look of the on-screen controller's button letters: which console's. The PS2 buttons behind them are the same. */
enum class PadSkin { PLAYSTATION, XBOX, NINTENDO }

/** How the controls are drawn: INK dark translucent buttons with a coloured rim; GLASS lighter, see-through. */
enum class PadStyle { INK, GLASS }

/** Which layout of the on-screen controller: the game's own (by scene) or the classic generic pad. */
enum class TouchLayout { MOBILE, CLASSIC }

/** When the on-screen controller shows: never, until a physical controller is used (and again on a touch), always. */
enum class TouchMode { OFF, AUTO, ALWAYS }

/**
 * What the game is showing, and so which controls are on the screen:
 *  - FIGHT: a 2D fight. A D-pad (the fights are on a plane), the fighting buttons and one-touch jutsu;
 *  - CAMPAIGN: the Master mode's map, walked with the stick;
 *  - MENU: menus, with the D-pad and the PS2 diamond.
 * The engine says which one it is (TouchBridge.scene); the SCENE button changes it by hand.
 */
enum class PadScene { FIGHT, CAMPAIGN, MENU }

/** The PS2 pad's buttons, as the engine's pad buffer has them. */
object Ps2 {
    const val SELECT = 0x0001
    const val L3 = 0x0002
    const val R3 = 0x0004
    const val START = 0x0008
    const val UP = 0x0010
    const val RIGHT = 0x0020
    const val DOWN = 0x0040
    const val LEFT = 0x0080
    const val L2 = 0x0100
    const val R2 = 0x0200
    const val L1 = 0x0400
    const val R1 = 0x0800
    const val TRIANGLE = 0x1000
    const val CIRCLE = 0x2000
    const val CROSS = 0x4000
    const val SQUARE = 0x8000

    /** Buttons a macro step can press, in the order the editor lists them. */
    val MACRO_BUTTONS = listOf(CROSS, CIRCLE, SQUARE, TRIANGLE, L1, R1, L2, R2, UP, DOWN, LEFT, RIGHT, L3, R3, START, SELECT)

    /** The four directions: a sequence that presses one of them owns the D-pad while it plays. */
    const val DIRECTIONS = UP or RIGHT or DOWN or LEFT

    /** A button's name in a console's look (positions stay the PS2's: south is cross). */
    fun label(skin: PadSkin, bit: Int): String = when (bit) {
        UP -> "↑"; DOWN -> "↓"; LEFT -> "←"; RIGHT -> "→"
        else -> when (skin) {
            PadSkin.PLAYSTATION -> when (bit) {
                CROSS -> "✕"; CIRCLE -> "○"; SQUARE -> "□"; TRIANGLE -> "△"
                L1 -> "L1"; L2 -> "L2"; R1 -> "R1"; R2 -> "R2"; L3 -> "L3"; R3 -> "R3"
                START -> "START"; SELECT -> "SELECT"; else -> "?"
            }
            PadSkin.XBOX -> when (bit) {
                CROSS -> "A"; CIRCLE -> "B"; SQUARE -> "X"; TRIANGLE -> "Y"
                L1 -> "LB"; L2 -> "LT"; R1 -> "RB"; R2 -> "RT"; L3 -> "LS"; R3 -> "RS"
                START -> "MENU"; SELECT -> "VIEW"; else -> "?"
            }
            PadSkin.NINTENDO -> when (bit) {
                CROSS -> "B"; CIRCLE -> "A"; SQUARE -> "Y"; TRIANGLE -> "X"
                L1 -> "L"; L2 -> "ZL"; R1 -> "R"; R2 -> "ZR"; L3 -> "LS"; R3 -> "RS"
                START -> "+"; SELECT -> "−"; else -> "?"
            }
        }
    }
}

/** What a control on the screen is. */
enum class CtlKind { DPAD, STICK, BUTTON, SHOULDER, PILL, SYSTEM, MACRO }

/** One step of a macro: these buttons held for [ms] milliseconds (0 buttons: a pause). */
data class MacroStep(val buttons: Int, val ms: Int)

/**
 * The controls of Ultimate Ninja 5. The functions are the series' on the PS2 (Ultimate Ninja 2's manual: ○ attack,
 * ✕ jump, △ chakra for a secret technique, □ item, L1/R1 change item, L2/R2 guard); the one-touch jutsu play the
 * series' inputs: △ then ○ for a jutsu, △ △ then ○ for the ultimate one; the two specials play the game's ↑ ↑ ○ and
 * ↓ ↓ ○ (the direction let go between the two, so the game sees two presses). Touched on a device (docs/registro.md,
 * 2026-11-09): the D-pad, attack, jump, chakra, item, guard, jutsu, Ougi and START. R3 (AWAKEN) was never seen doing
 * anything in UN5, so it starts hidden ([PadDefaults.FIGHT]); the layout editor still shows it.
 */
enum class Ctl(val kind: CtlKind, val bit: Int, val size: Float, val steps: List<MacroStep> = emptyList()) {
    DPAD(CtlKind.DPAD, 0, 140f),
    STICK(CtlKind.STICK, 0, 150f),
    ATTACK(CtlKind.BUTTON, Ps2.CIRCLE, 88f),
    JUMP(CtlKind.BUTTON, Ps2.CROSS, 66f),
    CHAKRA(CtlKind.BUTTON, Ps2.TRIANGLE, 64f),
    ITEM(CtlKind.BUTTON, Ps2.SQUARE, 62f),
    JUTSU(CtlKind.MACRO, 0, 60f, listOf(MacroStep(Ps2.TRIANGLE, 70), MacroStep(0, 50), MacroStep(Ps2.CIRCLE, 90))),
    OUGI(CtlKind.MACRO, 0, 60f, listOf(
        MacroStep(Ps2.TRIANGLE, 70), MacroStep(0, 50), MacroStep(Ps2.TRIANGLE, 70), MacroStep(0, 50), MacroStep(Ps2.CIRCLE, 90),
    )),
    SPECIAL_UP(CtlKind.MACRO, 0, 56f, listOf(
        MacroStep(Ps2.UP, 70), MacroStep(0, 50), MacroStep(Ps2.UP, 70), MacroStep(0, 50), MacroStep(Ps2.CIRCLE, 90),
    )),
    SPECIAL_DOWN(CtlKind.MACRO, 0, 56f, listOf(
        MacroStep(Ps2.DOWN, 70), MacroStep(0, 50), MacroStep(Ps2.DOWN, 70), MacroStep(0, 50), MacroStep(Ps2.CIRCLE, 90),
    )),
    GUARD(CtlKind.BUTTON, Ps2.R2, 60f),
    AWAKEN(CtlKind.BUTTON, Ps2.R3, 52f),
    ITEM_PREV(CtlKind.SHOULDER, Ps2.L1, 72f),
    ITEM_NEXT(CtlKind.SHOULDER, Ps2.R1, 72f),
    START(CtlKind.PILL, Ps2.START, 56f),
    SELECT(CtlKind.PILL, Ps2.SELECT, 56f),
    MENU(CtlKind.SYSTEM, 0, 40f),     // the engine's own menu (settings)
    OPTIONS(CtlKind.SYSTEM, 0, 40f),  // the controller's quick menu
    SCENE(CtlKind.SYSTEM, 0, 44f),    // fight / campaign / menu by hand
}

/** Where a control is: centre as a fraction of the screen's safe area, its scale, shown or not, its opacity. */
data class Placement(val x: Float, val y: Float, val scale: Float = 1f, val visible: Boolean = true, val opacity: Int = 100)

/** Which controls each scene has, and where they start. A control missing from a scene is not on screen in it. */
object PadDefaults {
    // (the controller's options are in the game menu's Layout tab; the scene's button is back up here (2026-11-13): in the
    // Versão Brasileira's campaign the controls stayed the conversation's after it ended, with no way to change them)
    private val top = listOf(
        Ctl.MENU to Placement(0.44f, 0.075f),
        Ctl.START to Placement(0.56f, 0.075f),
        Ctl.SCENE to Placement(0.68f, 0.075f),
    )

    val FIGHT: Map<Ctl, Placement> = linkedMapOf(
        Ctl.DPAD to Placement(0.13f, 0.68f),
        Ctl.ATTACK to Placement(0.84f, 0.74f),
        Ctl.JUMP to Placement(0.955f, 0.60f),
        Ctl.CHAKRA to Placement(0.86f, 0.49f),
        Ctl.ITEM to Placement(0.725f, 0.82f),   // (0.82 / 0.83: above the game's item bar, L1/R1 x∞, at the bottom)
        Ctl.JUTSU to Placement(0.73f, 0.58f),
        Ctl.OUGI to Placement(0.80f, 0.29f),
        Ctl.SPECIAL_UP to Placement(0.68f, 0.30f),
        Ctl.SPECIAL_DOWN to Placement(0.63f, 0.47f),
        Ctl.GUARD to Placement(0.955f, 0.83f),
        // (R3 was never seen doing anything in a fight of UN5: hidden until it is; the layout editor shows it)
        Ctl.AWAKEN to Placement(0.955f, 0.38f, visible = false),
        Ctl.ITEM_PREV to Placement(0.07f, 0.20f),
        Ctl.ITEM_NEXT to Placement(0.93f, 0.17f),
    ) + top

    val CAMPAIGN: Map<Ctl, Placement> = linkedMapOf(
        Ctl.STICK to Placement(0.15f, 0.70f),
        Ctl.ATTACK to Placement(0.84f, 0.74f),
        Ctl.JUMP to Placement(0.955f, 0.60f),
        Ctl.CHAKRA to Placement(0.86f, 0.49f),
        Ctl.ITEM to Placement(0.725f, 0.82f),
        Ctl.ITEM_PREV to Placement(0.07f, 0.20f),
        Ctl.ITEM_NEXT to Placement(0.93f, 0.17f),
        Ctl.SELECT to Placement(0.27f, 0.075f),
    ) + top

    val MENU: Map<Ctl, Placement> = linkedMapOf(
        Ctl.DPAD to Placement(0.14f, 0.62f),
        Ctl.CHAKRA to Placement(0.855f, 0.43f, 0.95f),  // △ north
        Ctl.ATTACK to Placement(0.935f, 0.62f, 0.72f),  // ○ east
        Ctl.JUMP to Placement(0.855f, 0.81f, 0.95f),    // ✕ south
        Ctl.ITEM to Placement(0.775f, 0.62f, 1f),       // □ west
        Ctl.ITEM_PREV to Placement(0.07f, 0.20f),
        Ctl.ITEM_NEXT to Placement(0.93f, 0.20f),
        Ctl.SELECT to Placement(0.27f, 0.075f),
    ) + top

    fun of(scene: PadScene): Map<Ctl, Placement> = when (scene) {
        PadScene.FIGHT -> FIGHT
        PadScene.CAMPAIGN -> CAMPAIGN
        PadScene.MENU -> MENU
    }

    /** Where the user's own macros start (by their place in the list): left of the specials, in rows of three. */
    fun macroPlace(i: Int) = Placement(0.54f - 0.075f * (i % 3), 0.40f - 0.14f * (i / 3))
}

/**
 * A button of the user's that plays a sequence of presses: a combo, or several buttons at once. [repeat]: it plays
 * again for as long as the button is held. Shown in fights and in the campaign.
 */
data class Macro(
    val id: String,
    val name: String,
    val steps: List<MacroStep>,
    val repeat: Boolean = false,
    val place: Placement? = null,
)

data class TouchConfig(
    val mode: TouchMode = TouchMode.AUTO,
    val skin: PadSkin = PadSkin.PLAYSTATION,
    val style: PadStyle = PadStyle.INK,
    val opacity: Int = 75,          // %
    val scale: Float = 1f,          // all controls
    val haptics: Boolean = true,
    val sticksFloat: Boolean = true, // the stick starts where the thumb lands (left part of the screen)
    val places: Map<String, Placement> = emptyMap(), // "FIGHT.ATTACK" → moved; the rest at PadDefaults
    val layout: TouchLayout = TouchLayout.MOBILE,
    val autoMode: Boolean = true,   // the scene follows the game (the SCENE button still overrides)
    val classicPlaces: Map<String, Placement> = emptyMap(), // the classic layout's own moved controls (by ClassicCtl name)
    val macros: List<Macro> = emptyList(),
    // the game's menus by gestures, no buttons on screen (NinjaPadView.menuGesture). Off: the virtual pad in the menus too
    // (2026-11-10, for the launch; tapping a menu item directly comes later). Stored as "menuGestures": the "menuTouch"
    // that earlier versions saved as true is not read, so everyone gets the pad.
    val menuTouch: Boolean = false,
    val showNames: Boolean = false, // the action's name under each fight button (off: the icon and the console letter)
    val pro: ProTouch = ProTouch(),
) {
    fun placeOf(scene: PadScene, c: Ctl): Placement? = places["${scene.name}.${c.name}"] ?: PadDefaults.of(scene)[c]

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 3)
        put("mode", mode.name)
        put("skin", skin.name)
        put("style", style.name)
        put("opacity", opacity)
        put("scale", scale.toDouble())
        put("haptics", haptics)
        put("sticksFloat", sticksFloat)
        put("places", JSONObject().apply { places.forEach { (k, p) -> put(k, p.json()) } })
        put("layout", layout.name)
        put("autoMode", autoMode)
        put("menuGestures", menuTouch)
        put("showNames", showNames)
        put("pro", pro.json())
        put("classicPlaces", JSONObject().apply { classicPlaces.forEach { (n, p) -> put(n, p.json()) } })
        put("macros", JSONArray().apply {
            macros.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("repeat", m.repeat)
                    m.place?.let { put("place", it.json()) }
                    put("steps", JSONArray().apply { m.steps.forEach { put(JSONArray().put(it.buttons).put(it.ms)) } })
                })
            }
        })
    }

    companion object {
        fun fromJson(o: JSONObject): TouchConfig {
            val places = mutableMapOf<String, Placement>()
            if (o.optInt("version", 1) >= 3) {
                o.optJSONObject("places")?.let { p -> p.keys().forEach { k -> p.optJSONObject(k)?.let { places[k] = placement(it) } } }
            }
            val classic = mutableMapOf<String, Placement>()
            o.optJSONObject("classicPlaces")?.let { p ->
                p.keys().forEach { k -> p.optJSONObject(k)?.let { classic[k] = placement(it) } }
            }
            val macros = mutableListOf<Macro>()
            o.optJSONArray("macros")?.let { a ->
                for (i in 0 until a.length()) {
                    val m = a.optJSONObject(i) ?: continue
                    val steps = mutableListOf<MacroStep>()
                    m.optJSONArray("steps")?.let { s ->
                        for (j in 0 until s.length()) {
                            val st = s.optJSONArray(j) ?: continue
                            steps += MacroStep(st.optInt(0), st.optInt(1, 100).coerceIn(16, 5000))
                        }
                    }
                    macros += Macro(
                        id = m.optString("id").ifEmpty { "m$i" },
                        name = m.optString("name").ifEmpty { "M${i + 1}" },
                        steps = steps,
                        repeat = m.optBoolean("repeat"),
                        place = m.optJSONObject("place")?.let { placement(it) },
                    )
                }
            }
            return TouchConfig(
                mode = runCatching { TouchMode.valueOf(o.optString("mode")) }.getOrDefault(TouchMode.AUTO),
                skin = runCatching { PadSkin.valueOf(o.optString("skin")) }.getOrDefault(PadSkin.PLAYSTATION),
                style = runCatching { PadStyle.valueOf(o.optString("style")) }.getOrDefault(PadStyle.INK),
                opacity = o.optInt("opacity", 75).coerceIn(10, 100),
                scale = o.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 2f),
                haptics = o.optBoolean("haptics", true),
                sticksFloat = o.optBoolean("sticksFloat", true),
                places = places,
                layout = runCatching { TouchLayout.valueOf(o.optString("layout")) }.getOrDefault(TouchLayout.MOBILE),
                autoMode = o.optBoolean("autoMode", true),
                classicPlaces = classic,
                macros = macros,
                menuTouch = o.optBoolean("menuGestures", false),
                showNames = o.optBoolean("showNames", false),
                pro = o.optJSONObject("pro")?.let { ProTouch.fromJson(it) } ?: ProTouch(),
            )
        }

        private fun Placement.json() = JSONObject().put("x", x.toDouble()).put("y", y.toDouble()).put("s", scale.toDouble()).put("v", visible).put("o", opacity)

        private fun placement(o: JSONObject) = Placement(
            o.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
            o.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
            o.optDouble("s", 1.0).toFloat().coerceIn(0.4f, 2.5f),
            o.optBoolean("v", true),
            o.optInt("o", 100).coerceIn(10, 100),
        )
    }
}

/**
 * "Toque Pro": the controller tuned for fast play (online, competitive). Off, the controller is the plain one. On:
 *  - [slideAll]: a finger sliding off a control onto another presses the new one, the jutsu, the specials, the macros
 *    and L1/R1 included (the plain controller slides between the action buttons only);
 *  - the fight's D-pad is fixed where the layout editor put it, [padSize] % of its plain size (larger by default), and
 *    a touch well around it still lands on it ([PRO_DPAD_HIT]); a thumb that went down on it steers it from anywhere;
 *  - [dead] % of the D-pad's (and the stick's) radius presses nothing, [hit] % of a button's size a touch counts on.
 * Kept in touch_controls.json as "pro" (absent before 2026-11-12: the defaults, off). Until 2026-11-13 the D-pad and
 * the stick could float under the thumb ("move", "zone": no longer read) and "padSize" was 85% by default: the size
 * is stored as "dpadSize" now, so the larger fixed D-pad is everyone's default.
 */
data class ProTouch(
    val on: Boolean = false,
    val slideAll: Boolean = true,
    val padSize: Int = 125,
    val dead: Int = 18,
    val hit: Int = 120,
) {
    fun json(): JSONObject = JSONObject()
        .put("on", on).put("slideAll", slideAll)
        .put("dpadSize", padSize).put("dead", dead).put("hit", hit)

    companion object {
        /** How far around the Toque Pro D-pad a touch still lands on it: of its half-size, each way. */
        const val PRO_DPAD_HIT = 1.5f

        fun fromJson(o: JSONObject) = ProTouch(
            on = o.optBoolean("on", false),
            slideAll = o.optBoolean("slideAll", true),
            padSize = o.optInt("dpadSize", 125).coerceIn(80, 160),
            dead = o.optInt("dead", 18).coerceIn(0, 50),
            hit = o.optInt("hit", 120).coerceIn(100, 160),
        )
    }
}

/**
 * touch_controls.json in the game's folder: read by the launcher (to edit it) and by the game's process (to show
 * the controller), which run in different processes, so it is always read from the file.
 */
class TouchStore(root: File) {
    val file = File(root, "touch_controls.json")

    fun load(): TouchConfig = runCatching {
        if (file.exists()) TouchConfig.fromJson(JSONObject(file.readText())) else TouchConfig()
    }.getOrDefault(TouchConfig())

    fun save(c: TouchConfig) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(c.toJson().toString(2))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }
}
