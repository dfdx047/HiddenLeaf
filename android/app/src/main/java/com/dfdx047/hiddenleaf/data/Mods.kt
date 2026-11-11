package com.dfdx047.hiddenleaf.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dfdx047.hiddenleaf.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Properties
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * A mod over the base game: the files of its disc that differ from the European release (or that it adds), kept in
 * modlib/<id>/disc/ with the disc's paths.
 *
 * [changesCode]: its boot program or an overlay (PRG/) differs, so the engine needs the mod's code pack to run it
 * ([pack]); a mod that only changes data (text, voices, models, textures) runs over the base as it is.
 */
data class FileMod(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val files: Int,
    val bytes: Long,
    val dir: File,
    val changesCode: Boolean = false,
    val known: Boolean = false,
    val pack: Boolean = true,
    val boot: String = "",
    val baseCode: Boolean = false,
    // the mod only loads a game saved in its own card folder, and neither that folder nor the base save has one
    val needsBaseSave: Boolean = false,
)

/**
 * The mods' library. A mod is imported from its own ISO (an image with the mod applied): each of its files is
 * compared with the base ISO's, and only the ones that differ are kept. The base stays as it is, and several data mods
 * can be on at once (a later one wins for the same file); only one mod that changes the code can be on.
 *
 * For the engine: un5_mods.txt lists the mods switched on, in order (one folder per line).
 */
class Mods(private val context: Context, private val paths: GamePaths, private val resolver: ContentResolver) {

    class Stop(message: String) : Exception(message)

    private val stateFile get() = File(paths.modLibrary, "mods.txt") // order and switches: "1|folder" per line

    fun listMods(): List<FileMod> {
        val dirs = paths.modLibrary.listFiles { f -> f.isDirectory }.orEmpty().associateBy { it.name }
        val state = readState()
        val ordered = state.map { it.first }.filter { it in dirs } + dirs.keys.filter { k -> state.none { it.first == k } }.sorted()
        return ordered.map { id ->
            val d = dirs.getValue(id)
            val p = Properties().apply { runCatching { File(d, "mod.properties").inputStream().use { load(it) } } }
            FileMod(
                id = id,
                name = p.getProperty("name", id),
                enabled = state.firstOrNull { it.first == id }?.second ?: false,
                files = p.getProperty("files")?.toIntOrNull() ?: File(d, "disc").countFiles(),
                bytes = p.getProperty("bytes")?.toLongOrNull() ?: File(d, "disc").sizeDeep(),
                dir = d,
                changesCode = p.getProperty("code") == "1",
                known = p.getProperty("known") == "1",
                pack = com.dfdx047.hiddenleaf.engine.EngineBridge.run { has(context, forCpu(packOf(id))) } ||
                    (KnownVersions.MODS.firstOrNull { it.id == id }?.pack ?: (p.getProperty("code") != "1")),
                boot = p.getProperty("boot", ""),
                baseCode = KnownVersions.MODS.firstOrNull { it.id == id }?.baseCode ?: false,
                needsBaseSave = KnownVersions.MODS.firstOrNull { it.id == id }?.cardFolder.orEmpty().let { folder ->
                    folder.isNotEmpty() && !ModSaves.hasGame(File(paths.saves, folder)) &&
                        !ModSaves.hasGame(File(paths.saves, ModSaves.BASE_FOLDER))
                },
            )
        }
    }

    private fun readState(): List<Pair<String, Boolean>> =
        if (!stateFile.exists()) emptyList()
        else stateFile.readLines().mapNotNull { l ->
            val bar = l.indexOf('|')
            if (bar < 1) null else l.substring(bar + 1) to (l.substring(0, bar) == "1")
        }

    private fun writeState(mods: List<FileMod>) {
        stateFile.writeText(mods.joinToString("\n", postfix = "\n") { "${if (it.enabled) 1 else 0}|${it.id}" })
        apply(mods)
    }

    /** The list the engine reads: the mods switched on, in order. */
    private fun apply(mods: List<FileMod>) {
        paths.activeMods.writeText(mods.filter { it.enabled }.joinToString("\n", postfix = "\n") { "modlib/${it.id}/disc" })
    }

    /** Switches a mod. Switching on a mod that changes the code switches off the other one that does. */
    suspend fun setEnabled(mod: FileMod, enabled: Boolean) = withContext(Dispatchers.IO) {
        writeState(listMods().map {
            when {
                it.id == mod.id -> it.copy(enabled = enabled)
                enabled && mod.changesCode && it.changesCode -> it.copy(enabled = false)
                else -> it
            }
        })
    }

    suspend fun move(mod: FileMod, delta: Int) = withContext(Dispatchers.IO) {
        val list = listMods().toMutableList()
        val i = list.indexOfFirst { it.id == mod.id }
        val j = (i + delta).coerceIn(0, list.lastIndex)
        if (i < 0 || i == j) return@withContext
        list.add(j, list.removeAt(i))
        writeState(list)
    }

    suspend fun delete(mod: FileMod) = withContext(Dispatchers.IO) {
        mod.dir.deleteRecursively()
        writeState(listMods())
    }

    /**
     * Imports a mod from its ISO, comparing it file by file with the base ([base]: the base ISO, open). The files
     * that are the same are skipped; the others are copied. Takes a while on a whole disc: every file of the same
     * size is read in both images.
     */
    suspend fun importIso(uri: Uri, base: IsoImage, onProgress: (String) -> Unit): FileMod = withContext(Dispatchers.IO) {
        val pfd = resolver.openFileDescriptor(uri, "r") ?: throw Stop(context.getString(R.string.err_open_file))
        pfd.use {
            val iso = try {
                IsoImage(FileInputStream(pfd.fileDescriptor).channel)
            } catch (e: IOException) {
                throw Stop(context.getString(R.string.err_not_iso))
            }
            iso.use { importFrom(iso, base, onProgress) }
        }
    }

    private suspend fun importFrom(iso: IsoImage, base: IsoImage, onProgress: (String) -> Unit): FileMod {
        val boot = bootName(iso) ?: throw Stop(context.getString(R.string.err_not_ps2))
        val elf = iso.find(boot) ?: throw Stop(context.getString(R.string.err_not_ps2))
        onProgress(context.getString(R.string.detail_checksums))
        val elfSha = GameDataInstaller.sha256(iso, elf)
        val known = KnownVersions.modByElf(elfSha)
        // a known mod only from an image of it the launcher knows (the whole image, by SHA-256): each player brings
        // their own, and a damaged or different build would run code the engine was not made for
        val isoSha = GameDataInstaller.imageSha256(iso) { percent ->
            onProgress(context.getString(R.string.detail_image_checksum, percent))
        }
        if (known != null && known.isoSha256.isNotEmpty() && isoSha !in known.isoSha256) {
            throw Stop(context.getString(R.string.err_mod_image, known.name, isoSha, known.isoSha256.first()))
        }
        val id = known?.id ?: "mod-${elfSha.take(8)}"
        val dir = File(paths.modLibrary, id)
        val disc = File(dir, "disc")
        dir.deleteRecursively()
        disc.mkdirs()

        val total = iso.files.sumOf { it.size }.coerceAtLeast(1)
        var seen = 0L
        var files = 0
        var bytes = 0L
        var code = elfSha != KnownVersions.BASE_ELF_SHA256 || !boot.equals(KnownVersions.BASE_ELF, ignoreCase = true)
        val buf = ByteBuffer.allocateDirect(1 shl 20)
        try {
            for (f in iso.files) {
                coroutineContext.ensureActive()
                val b = base.find(f.path)
                val same = b != null && b.size == f.size && sameBytes(iso, f, base, b) { n ->
                    seen += n
                    onProgress(context.getString(R.string.detail_compare, (seen * 100 / total).toInt(), f.path))
                }
                if (b == null || b.size != f.size) seen += f.size
                if (same) continue
                val out = File(disc, f.path.uppercase())
                out.parentFile?.mkdirs()
                GameDataInstaller.copyOut(iso, f.offset, f.size, out, buf)
                if (out.length() != f.size) throw Stop(context.getString(R.string.err_copy_short, f.path))
                files++
                bytes += f.size
                if (f.path.uppercase().startsWith("PRG/")) code = true
                onProgress(context.getString(R.string.detail_compare, (seen * 100 / total).toInt(), f.path))
            }
        } catch (e: Exception) {
            dir.deleteRecursively()
            throw e
        }
        if (files == 0) {
            dir.deleteRecursively()
            throw Stop(context.getString(R.string.err_mod_is_base))
        }
        val p = Properties()
        p["name"] = known?.name ?: context.getString(R.string.mod_unknown_name, boot)
        p["boot"] = boot
        p["elf_sha256"] = elfSha
        p["iso_sha256"] = isoSha
        p["files"] = files.toString()
        p["bytes"] = bytes.toString()
        p["code"] = if (code) "1" else "0"
        p["known"] = if (known != null) "1" else "0"
        File(dir, "mod.properties").outputStream().use { p.store(it, "Hidden Leaf mod") }
        // a new mod comes switched on (and a code mod switches off the other one)
        val mod = listMods().first { it.id == id }
        setEnabled(mod, true)
        return listMods().first { it.id == id }
    }

    private suspend fun sameBytes(a: IsoImage, fa: IsoImage.Entry, b: IsoImage, fb: IsoImage.Entry, onBytes: (Long) -> Unit): Boolean {
        if (fa.size != fb.size) return false
        if (fa.offset == fb.offset && a === b) return true
        val ba = ByteBuffer.allocate(1 shl 20)
        val bb = ByteBuffer.allocate(1 shl 20)
        var pos = 0L
        while (pos < fa.size) {
            coroutineContext.ensureActive()
            val n = minOf(fa.size - pos, ba.capacity().toLong()).toInt()
            ba.clear(); ba.limit(n); a.readInto(fa.offset + pos, ba)
            bb.clear(); bb.limit(n); b.readInto(fb.offset + pos, bb)
            ba.flip(); bb.flip()
            if (ba != bb) {
                onBytes(fa.size - pos)
                return false
            }
            pos += n
            onBytes(n.toLong())
        }
        return true
    }

    private fun bootName(iso: IsoImage): String? {
        val cnf = iso.find("SYSTEM.CNF") ?: return null
        val text = String(iso.read(cnf.offset, cnf.size.toInt().coerceAtMost(4096)), Charsets.ISO_8859_1)
        val line = text.lines().firstOrNull { it.trim().startsWith("BOOT2", ignoreCase = true) } ?: return null
        return line.substringAfter('=').trim().substringAfterLast('\\').substringBefore(';').trim().ifEmpty { null }
    }

    companion object {
        fun sha256(f: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { s ->
                val b = ByteArray(1 shl 16)
                while (true) {
                    val n = s.read(b)
                    if (n < 0) break
                    md.update(b, 0, n)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
