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
    fun isAvailable(context: Context): Boolean {
        val loader = context.classLoader as? dalvik.system.BaseDexClassLoader ?: return false
        return loader.findLibrary(LIBRARY) != null
    }
}
