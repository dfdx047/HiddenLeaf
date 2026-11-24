package com.dfdx047.hiddenleaf.data

import java.io.File

/**
 * The engine's settings file (port/src/plat_settings.c): `name=number` lines, names up to 23 characters, at most
 * 200 of them. The app edits the same file the engine reads at start, and keeps every line it does not know (the
 * controller bindings the engine writes, for example).
 */
class EngineSettings(private val file: File) {
    private val values = linkedMapOf<String, Int>()

    init {
        load()
    }

    @Synchronized
    fun load() {
        values.clear()
        if (!file.exists()) return
        file.forEachLine { line ->
            val eq = line.indexOf('=')
            if (eq <= 0) return@forEachLine
            val name = line.substring(0, eq).trim()
            val v = line.substring(eq + 1).trim().toIntOrNull() ?: return@forEachLine
            if (name.isNotEmpty() && name.length < 24) values[name] = v
        }
    }

    @Synchronized
    fun get(name: String, default: Int): Int = values[name] ?: default

    @Synchronized
    fun snapshot(): Map<String, Int> = LinkedHashMap(values)

    @Synchronized
    fun set(name: String, value: Int) {
        require(name.length < 24) { "setting name too long: $name" }
        if (values[name] == value) return
        values[name] = value
        write()
    }

    private fun write() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(values.entries.joinToString(separator = "\n", postfix = "\n") { "${it.key}=${it.value}" })
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    companion object Keys {
        // The engine's own (defaults as in port/src/gs/gs_draw.c)
        const val SCALE = "scale"                 // internal resolution 1..8, default 2 (whole; kept for older engines)
        const val SCALE4 = "scale4"               // the same in quarters: 2 = 0.5x, 4 = 1x, 6 = 1.5x .. 32 = 8x (wins over "scale")
        const val ASPECT = "aspect_milli"         // 1333, 1600, 1778, 2389, 3556
        const val FX_OFF = "fx_off"               // bits: 1 outline, 2 see-through tint, 4 depth tint, 8 glow, 16 blur
        const val GLOW = "glow"                   // 0..200 %, default 60
        const val MUSIC = "music"                 // 0..200 %
        const val EFFECTS = "effects"             // 0..200 %, sound effects and voices
        const val METER = "meter"                 // the counter: 0 off, 1 fps, 2 and the frame's time, 3 and EE, VU1 and GS
        const val TEXTURE_PACK = "texture_pack"   // texture packs on / off, default on
        const val FILTER = "filter"               // screen filter: 0 bilinear, 1 sharp bilinear, 2 FXAA, 3 CAS
        const val SPECIAL_ONE = "special_one"     // 1: one tap does the Jutsu or Ougi, and answers the game's prompts (hl_game.cpp)
        const val SPECIAL_BUTTON = "special_button" // its button, in the game's bits of the pad: 0x20 circle...
        const val MASH_PERCENT = "mash_percent"   // what a tap is worth where the game asks to press repeatedly: 100..200
        const val FPS_MODE = "fps_mode"           // 30 or 50 (the game's original rate)
        const val ZERO_FG = "zero_fg"             // ZeroFG 1.2 real Vulkan frame generation (offline only): 0 off, 2 = 2x, 3 = 3x, 4 = 4x
        const val SKIP_INTRO = "skip_intro"       // 1 (default): the logos and opening movies skipped, START pressed for the player
        const val LIGHT_MODE = "light_mode"       // 1: light mode, for weaker phones (Snapdragon 665 class): Nominum's VIF1 and VU1 (its recompiled
                                                  // microprograms) and, on the Snapdragon 665, the GS in two threads; faster, some effects may fail.
                                                  // 0 (default): the runtime's VU1 and the GS in one thread, as the game draws everything
        const val NATIVE_RENDER = "native_render" // 1: the fight drawn by Hidden Leaf's own renderer (experimental); 0 (default) PCSX2's GS draws all

        // the launcher's own, read by the Android glue of the engine at start
        const val UNLOCK_ALL = "dr_unlock_all"    // 1: all characters and links open (written to the card when the player saves)
        const val FPS60 = "dr_fps60"              // reserved for the 60 fps mode
        const val INTERP = "interp"               // smooth motion: 0 off, 1 60, 2 120, 3 240 pictures a second (the engine's)
        const val PRESENT = "present"             // present mode: 0 automatic, 1 vsync (FIFO), 2 mailbox, 3 immediate
        const val GPU_API = "gpu_api"             // the engine's renderer: 0 Vulkan, 1 OpenGL ES 3.2 (written from RENDERER)
        const val RENDERER = "dr_renderer"        // the player's choice: 0 automatic (OpenGL ES for Mali), 1 Vulkan, 2 OpenGL ES
        const val CLASH_SQUARE = "clash_square"   // clashes (beams, rushes): each press of Square counts as a direction; on by default
        const val LANG = "dr_lang"                // the in-game menu's language: 0 English, 1 Portuguese, 2 Spanish (written at start)
        const val ORIENTATION = "dr_orient"       // the game's screen: 0 landscape, 1 portrait, 2 as the device is held
        const val FRAG_DEPTH = "frag_depth"       // precise depth (the fragment shader writes depth): 1 on, 0 off (faster)
        const val ASTC = "astc"                   // texture packs kept as ASTC copies (textures_astc) where the GPU reads ASTC
        const val FRAME_SKIP = "frame_skip"       // skip pictures instead of slowing down
        const val GPU_TIER = "dr_gpu_tier"        // the GPU's tier the launcher's optimisations were made for (GpuProfiles)
        const val TOUCH = "dr_touch"              // on-screen controls: 0 off, 1 automatic, 2 always
        const val TOUCH_OPACITY = "dr_touch_alpha" // 10..100 %

        const val GLOW_DEFAULT = 60
        const val SCALE_DEFAULT = 1
        /**
         * The internal resolutions offered, in quarters (the engine's settings window has the same list). 0.5x and 0.75x
         * draw fewer pixels than the PS2 for weak phones (the GS's floor is 0.5x).
         */
        val SCALE_STEPS = listOf(2, 3, 4, 5, 6, 7, 8, 10, 12, 16, 20, 24, 32)

        /** The whole "scale" older engines read, from quarters (at least 1). */
        fun wholeScale(scale4: Int): Int = maxOf(1, (scale4 + 2) / 4)
        const val ASPECT_DEFAULT = 0              // the screen's own shape (the engine measures it); 1333 is the game's 4:3
    }
}
