package com.dfdx047.hiddenleaf.data

import java.io.File

/**
 * The save a mod needs and does not make. The A35 only loads a save in its own card folder (BASLUS-55606NARUTO6); with
 * none, New game and Load game both stop at "no data of Ultimate Ninja 5 in slot 1". The folder is the base game's save
 * (BESLES-55605NARUTO5) under the mod's name: the same files, with the header file (named as its folder) renamed. That
 * is what was done by hand on 2026-11-03, and the A35 loads it and fights.
 *
 * Runs before the engine starts. Does nothing when the folder is there (the mod's own progress is kept) or when there is
 * no base save yet.
 */
object ModSaves {
    const val BASE_FOLDER = "BESLES-55605NARUTO5"

    fun prepare(paths: GamePaths) {
        val ids = runCatching { paths.activeMods.readLines() }.getOrDefault(emptyList())
            .map { it.trim() }.filter { it.startsWith("modlib/") }.map { it.removePrefix("modlib/").substringBefore('/') }
        for (id in ids) {
            val folder = KnownVersions.MODS.firstOrNull { it.id == id }?.cardFolder.orEmpty()
            if (folder.isNotEmpty()) makeFrom(File(paths.saves, BASE_FOLDER), File(paths.saves, folder))
        }
    }

    /** [base] copied to [target], the header file renamed to [target]'s name; through a temporary folder, so a stop
     *  halfway leaves no half save. */
    fun makeFrom(base: File, target: File): Boolean {
        if (target.exists() || !File(base, base.name).isFile) return false
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        tmp.deleteRecursively()
        if (!tmp.mkdirs()) return false
        base.listFiles { f -> f.isFile }.orEmpty().forEach { f ->
            f.copyTo(File(tmp, if (f.name == base.name) target.name else f.name), overwrite = true)
        }
        return tmp.renameTo(target)
    }
}
