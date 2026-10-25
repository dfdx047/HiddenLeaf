package com.dfdx047.hiddenleaf.data

import android.content.Context
import java.io.File

/**
 * Where everything lives. The recompiled engine runs with [root] as its working directory:
 *
 *   <root>/runtime/data/        the boot files copied from the player's ISO (SYSTEM.CNF, the ELF, PRG/, MODULES/,
 *                               the small DATA/ .BIN files); the big archives are read from the ISO where it is
 *   <root>/iso_uri.txt          the content:// address of the base ISO (persistent permission)
 *   <root>/.installed           what was imported (version, files, bytes)
 *   <root>/modlib/<mod>/        the mods: disc/<PATH> holds the files that differ from the base, mod.properties
 *                               says what it is; mods.txt the order and switches
 *   <root>/un5_mods.txt         the mods switched on, in order, for the engine
 *   <root>/textures/            texture packs in PCSX2 naming; textures_off/ the ones switched off
 *   <root>/saves/               memory cards
 *   <root>/un5_settings.txt     the engine's settings (name=number)
 *
 * The root is the app's folder on shared storage (Android/data/<package>/files), so a file manager or adb can
 * reach it too.
 */
class GamePaths(context: Context) {
    val root: File = (context.getExternalFilesDir(null) ?: context.filesDir).also { it.mkdirs() }
    val runtime = File(root, "runtime")
    val boot = File(runtime, "data")
    val isoUri = File(root, "iso_uri.txt")
    val installedMarker = File(root, ".installed")
    val textures = File(root, "textures")
    val texturesOff = File(root, "textures_off")
    val saves = File(root, "saves")
    val settings = File(root, "un5_settings.txt")
    val modLibrary = File(root, "modlib")
    val activeMods = File(root, "un5_mods.txt")
    val temp = File(context.cacheDir, "import")

    fun ensure() {
        listOf(boot, textures, texturesOff, saves, modLibrary, temp).forEach { it.mkdirs() }
    }
}
