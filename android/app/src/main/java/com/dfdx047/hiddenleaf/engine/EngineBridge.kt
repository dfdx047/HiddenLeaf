package com.dfdx047.hiddenleaf.engine

import android.content.Context

/**
 * The link to the game engine: Naruto Shippuden: Ultimate Ninja 5 recompiled for arm64 (PS2Recomp) as libun5.so,
 * next to SDL3 (libSDL3.so) and PCSX2's GS (libbt4gsx.so). GameActivity starts it with the app's folder
 * (GamePaths.root) as its working directory and the base ISO as an open descriptor. A build of the app without the
 * engine still works as a launcher.
 *
 * Two builds of each engine library can ship: the ARMv8.2-A one (libun5.so, libun5_<mod>.so) and an ARMv8.0-A one
 * (libun5_v80.so, libun5_<mod>_v80.so, recomp/android/build.sh with HL_V80=1). On a CPU without the ARMv8.1 atomics
 * (the Mi A3's Snapdragon 665) only the _v80 ones may run: the others stop at once on an illegal instruction.
 */
object EngineBridge {
    const val LIBRARY = "un5"
    private const val V80 = "_v80"

    /** Whether the engine library this CPU can run is packaged in this APK. Looked up without loading it. */
    fun isAvailable(context: Context): Boolean = has(context, forCpu(LIBRARY))

    /** A mod's code pack: the engine built with the mod's code (recomp/android/build.sh --mod <id>). */
    fun packOf(modId: String) = LIBRARY + "_" + modId.replace('-', '_')

    fun has(context: Context, library: String): Boolean {
        val loader = context.classLoader as? dalvik.system.BaseDexClassLoader ?: return false
        return loader.findLibrary(library) != null
    }

    /** True on ARMv8.1 and later: the kernel lists the LSE atomics and the rounding doubling multiplies (RDM). */
    val cpuIsV82: Boolean by lazy {
        val features = runCatching {
            java.io.File("/proc/cpuinfo").readLines().firstOrNull { it.startsWith("Features") }.orEmpty()
        }.getOrDefault("").split(' ', '\t', ':').toSet()
        // unreadable: take the build the test devices run
        features.isEmpty() || ("atomics" in features && "asimdrdm" in features)
    }

    /** The name of [library]'s build for this CPU. */
    fun forCpu(library: String) = if (cpuIsV82) library else library + V80

    /**
     * The library to run: the code pack of the mod switched on that has one (un5_mods.txt), else the base engine, each
     * in its build for this CPU. On an ARMv8.0 CPU a mod pack without its _v80 build is skipped: the base engine runs
     * the mod's program instead (as for a mod without a pack).
     */
    fun libraryFor(context: Context, activeMods: java.io.File): String {
        val ids = runCatching { activeMods.readLines() }.getOrDefault(emptyList())
            .map { it.trim() }.filter { it.startsWith("modlib/") }.map { it.removePrefix("modlib/").substringBefore('/') }
        return ids.map { forCpu(packOf(it)) }.firstOrNull { has(context, it) } ?: forCpu(LIBRARY)
    }
}
