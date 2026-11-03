package com.dfdx047.hiddenleaf.engine

import android.content.Context

/**
 * The link to the game engine: Naruto Shippuden: Ultimate Ninja 5 recompiled for arm64 (PS2Recomp) as libun5.so,
 * next to SDL3 (libSDL3.so) and PCSX2's GS (libbt4gsx.so). GameActivity starts it with the app's folder
 * (GamePaths.root) as its working directory and the base ISO as an open descriptor. A build of the app without the
 * engine still works as a launcher.
 */
object EngineBridge {
    const val LIBRARY = "un5"

    /** Whether the engine library is packaged in this APK. Looked up without loading it. */
    fun isAvailable(context: Context): Boolean = has(context, LIBRARY)

    /** A mod's code pack: the engine built with the mod's code (recomp/android/build.sh --mod <id>). */
    fun packOf(modId: String) = LIBRARY + "_" + modId.replace('-', '_')

    fun has(context: Context, library: String): Boolean {
        val loader = context.classLoader as? dalvik.system.BaseDexClassLoader ?: return false
        return loader.findLibrary(library) != null
    }

    /** The library to run: the code pack of the mod switched on that has one (un5_mods.txt), else the base engine. */
    fun libraryFor(context: Context, activeMods: java.io.File): String {
        val ids = runCatching { activeMods.readLines() }.getOrDefault(emptyList())
            .map { it.trim() }.filter { it.startsWith("modlib/") }.map { it.removePrefix("modlib/").substringBefore('/') }
        return ids.map { packOf(it) }.firstOrNull { has(context, it) } ?: LIBRARY
    }
}
