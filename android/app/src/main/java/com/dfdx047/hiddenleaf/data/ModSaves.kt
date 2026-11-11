package com.dfdx047.hiddenleaf.data

import java.io.File

/**
 * The save a mod needs and does not make. The A35 only loads a save in its own card folder (BASLUS-55606NARUTO6); with
 * none, New game and Load game both stop at "no data of Ultimate Ninja 5 in slot 1". The folder is the base game's save
 * (BESLES-55605NARUTO5) under the mod's name: the same files, with the header file (named as its folder) renamed. That
 * is what was done by hand on 2026-11-03, and the A35 loads it and fights.
 *
 * Runs before the engine starts. Does nothing when the folder holds a saved game (the mod's own progress is kept) or when
 * there is no base save yet. A folder made from a base save with no game in it (the base's New Game writes the save
 * before any slot is used: data01..04 all 0xFF, the A35 then shows its slots "Empty") is made again once the base save
 * has a game (docs/registro.md 2026-11-11).
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

    /** A game saved in a slot: a data file of the save with anything but the erased 0xFF (one with a game in slot 1 had
     *  data01 and its copy data04 written, 2026-11-02; an unused save has all four erased). */
    fun hasGame(folder: File): Boolean =
        (1..4).any { n ->
            val f = File(folder, "data0$n")
            f.isFile && runCatching { f.readBytes().any { it != 0xFF.toByte() } }.getOrDefault(false)
        }

    /** [base] copied to [target], the header file renamed to [target]'s name; through a temporary folder, so a stop
     *  halfway leaves no half save. A [target] with no game is replaced when [base] has one. */
    fun makeFrom(base: File, target: File): Boolean {
        if (!File(base, base.name).isFile) return false
        if (target.exists()) {
            if (hasGame(target) || !hasGame(base)) return false
        }
        val tmp = File(target.parentFile, ".${target.name}.tmp")
        tmp.deleteRecursively()
        if (!tmp.mkdirs()) return false
        base.listFiles { f -> f.isFile }.orEmpty().forEach { f ->
            f.copyTo(File(tmp, if (f.name == base.name) target.name else f.name), overwrite = true)
        }
        if (target.exists()) {
            val old = File(target.parentFile, ".${target.name}.empty")
            old.deleteRecursively()
            if (!target.renameTo(old)) return false
            if (!tmp.renameTo(target)) { old.renameTo(target); return false }
            old.deleteRecursively()
            return true
        }
        return tmp.renameTo(target)
    }
}
