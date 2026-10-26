package com.dfdx047.hiddenleaf.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.StatFs
import com.dfdx047.hiddenleaf.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface InstallState {
    data object NotInstalled : InstallState
    data class Working(val step: String, val progress: Float, val detail: String) : InstallState
    data class Installed(val files: Int, val bytes: Long, val modified: Boolean = false) : InstallState
    data class Failed(val message: String) : InstallState
}

/**
 * Imports the base game for the recompiled engine: the European ISO of Naruto Shippuden: Ultimate Ninja 5
 * (SLES-55605), unmodified.
 *
 *  1. the ISO stays where the player keeps it: the app takes a persistent permission to its document and remembers
 *     the address (iso_uri.txt); the engine reads the big archives (DATA/DATA.CVM, the AFS files) from it;
 *  2. the boot program and the overlays are checked by SHA-256 against the release the engine was recompiled from:
 *     the engine's code is that program, so another one cannot run. An image of a mod (Ultimate Ninja 6, the
 *     Brazilian version) is recognised and the player is told to import it on the Mods page instead. Then the
 *     whole image is checked by SHA-1 against Redump's dump, so a damaged or patched file elsewhere is caught too;
 *  3. only the small files are copied (SYSTEM.CNF, the ELF, PRG/, MODULES/, the small DATA/ .BIN files) to runtime/data/.
 */
class GameDataInstaller(
    private val context: Context,
    private val paths: GamePaths,
    private val resolver: ContentResolver,
) {

    class Stop(message: String) : Exception(message)

    fun currentState(): InstallState {
        if (!paths.installedMarker.exists()) return InstallState.NotInstalled
        val lines = paths.installedMarker.readText().lines()
        val files = lines.firstOrNull { it.startsWith("files=") }?.substringAfter('=')?.toIntOrNull() ?: 0
        val bytes = lines.firstOrNull { it.startsWith("bytes=") }?.substringAfter('=')?.toLongOrNull() ?: 0L
        return InstallState.Installed(files, bytes, false)
    }

    /** The base ISO's address, if one was imported. */
    fun baseUri(): Uri? = runCatching { Uri.parse(paths.isoUri.readText().trim()) }.getOrNull()

    /** Opens the base ISO again (for the engine, and to compare a mod's files with it). */
    fun openBase(): ParcelFileDescriptor? = baseUri()?.let { runCatching { resolver.openFileDescriptor(it, "r") }.getOrNull() }

    suspend fun install(uri: Uri, report: (InstallState.Working) -> Unit): InstallState.Installed = withContext(Dispatchers.IO) {
        // the permission first: without it the engine could not open the ISO at the next start
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            throw Stop(context.getString(R.string.err_permission))
        }
        val pfd = resolver.openFileDescriptor(uri, "r") ?: throw Stop(context.getString(R.string.err_open_file))
        pfd.use {
            val iso = try {
                IsoImage(FileInputStream(pfd.fileDescriptor).channel)
            } catch (e: IOException) {
                throw Stop(context.getString(R.string.err_not_iso))
            }
            iso.use {
                report(InstallState.Working(context.getString(R.string.step_checking), 0f, context.getString(R.string.detail_finding)))
                verify(iso, report)
                copyBoot(iso, report).also {
                    // the old base's permission is let go when another ISO replaces it
                    val old = baseUri()
                    if (old != null && old != uri) runCatching { resolver.releasePersistableUriPermission(old, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    paths.isoUri.writeText(uri.toString())
                }
            }
        }
    }

    /** The program SYSTEM.CNF boots ("cdrom0:\SLES_556.05;1" → "SLES_556.05"). */
    private fun bootName(iso: IsoImage): String? {
        val cnf = iso.find("SYSTEM.CNF") ?: return null
        val text = String(iso.read(cnf.offset, cnf.size.toInt().coerceAtMost(4096)), Charsets.ISO_8859_1)
        val line = text.lines().firstOrNull { it.trim().startsWith("BOOT2", ignoreCase = true) } ?: return null
        return line.substringAfter('=').trim().substringAfterLast('\\').substringBefore(';').trim().ifEmpty { null }
    }

    private suspend fun verify(iso: IsoImage, report: (InstallState.Working) -> Unit) {
        val boot = bootName(iso) ?: throw Stop(context.getString(R.string.err_not_ps2))
        val elf = iso.find(boot) ?: throw Stop(context.getString(R.string.err_not_ps2))
        report(InstallState.Working(context.getString(R.string.step_checking), 0.05f, context.getString(R.string.detail_checksums)))
        coroutineContext.ensureActive()
        val elfSha = sha256(iso, elf)
        KnownVersions.modByElf(elfSha)?.let { throw Stop(context.getString(R.string.err_is_mod, it.name)) }
        if (!boot.equals(KnownVersions.BASE_ELF, ignoreCase = true)) {
            throw Stop(context.getString(R.string.err_not_un5, boot.replace('_', '-').replace(".", "")))
        }
        if (elfSha != KnownVersions.BASE_ELF_SHA256) throw Stop(context.getString(R.string.err_base_modified))
        // the overlays are code too: a changed one means a mod (or a damaged image)
        var i = 0
        for ((path, want) in KnownVersions.BASE_OVERLAYS) {
            coroutineContext.ensureActive()
            val e = iso.find(path) ?: throw Stop(context.getString(R.string.err_missing_file, path))
            if (sha256(iso, e) != want) throw Stop(context.getString(R.string.err_base_modified))
            i++
            report(InstallState.Working(context.getString(R.string.step_checking), 0.05f + 0.1f * i / KnownVersions.BASE_OVERLAYS.size, path))
        }
        // the program is the right one; the rest of the disc (the data, the IOP modules) has to be Redump's dump
        val sha1 = imageSha1(iso) { percent ->
            report(InstallState.Working(context.getString(R.string.step_checking), 0.15f + 0.85f * percent / 100, context.getString(R.string.detail_image_checksum, percent)))
        }
        if (sha1 != KnownVersions.BASE_ISO_SHA1) throw Stop(context.getString(R.string.err_base_not_redump, sha1, KnownVersions.BASE_ISO_SHA1))
    }

    private suspend fun copyBoot(iso: IsoImage, report: (InstallState.Working) -> Unit): InstallState.Installed {
        val take = iso.files.filter { isBootFile(it) }
        val total = take.sumOf { it.size }
        val free = StatFs(paths.root.path).availableBytes
        if (free < total + 64L * 1024 * 1024) throw Stop(context.getString(R.string.err_space, formatBytes(total), formatBytes(free)))
        paths.installedMarker.delete()
        paths.boot.deleteRecursively()
        paths.boot.mkdirs()
        val buf = ByteBuffer.allocateDirect(1 shl 20)
        var done = 0L
        var files = 0
        for (f in take) {
            coroutineContext.ensureActive()
            val out = File(paths.boot, f.path.uppercase())
            out.parentFile?.mkdirs()
            copyOut(iso, f.offset, f.size, out, buf)
            if (out.length() != f.size) throw Stop(context.getString(R.string.err_copy_short, f.path))
            done += f.size
            files++
            val percent = if (total > 0) (done * 100 / total).toInt() else 100
            report(InstallState.Working(context.getString(R.string.step_extracting), done.toFloat() / total.coerceAtLeast(1), context.getString(R.string.detail_extract, percent, files)))
        }
        paths.installedMarker.writeText("version=${KnownVersions.BASE_SERIAL}\nfiles=$files\nbytes=$total\nlayout=recomp\n")
        return InstallState.Installed(files, total)
    }

    fun uninstall() {
        baseUri()?.let { runCatching { resolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        paths.installedMarker.delete()
        paths.isoUri.delete()
        paths.boot.deleteRecursively()
    }

    companion object {
        /** The files copied out of the disc; everything else is read from the ISO itself. */
        fun isBootFile(e: IsoImage.Entry): Boolean {
            val u = e.path.uppercase()
            return u == "SYSTEM.CNF" || Regex("^S[LC][A-Z]{2}_\\d{3}\\.\\d{2}$").matches(u) ||
                u.startsWith("PRG/") || u.startsWith("MODULES/") ||
                (u.startsWith("DATA/") && u.endsWith(".BIN") && e.size <= 2L * 1024 * 1024)
        }

        suspend fun sha256(iso: IsoImage, e: IsoImage.Entry): String {
            val md = MessageDigest.getInstance("SHA-256")
            val buf = ByteBuffer.allocate(1 shl 20)
            var pos = e.offset
            var left = e.size
            while (left > 0) {
                coroutineContext.ensureActive()
                buf.clear()
                buf.limit(minOf(left, buf.capacity().toLong()).toInt())
                iso.readInto(pos, buf)
                md.update(buf.array(), 0, buf.limit())
                pos += buf.limit()
                left -= buf.limit()
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        /** SHA-1 of the whole image file, with [progress] in percent. */
        suspend fun imageSha1(iso: IsoImage, progress: (Int) -> Unit): String {
            val md = MessageDigest.getInstance("SHA-1")
            val buf = ByteBuffer.allocate(4 shl 20)
            val total = iso.length
            var pos = 0L
            var shown = -1
            while (pos < total) {
                coroutineContext.ensureActive()
                buf.clear()
                buf.limit(minOf(total - pos, buf.capacity().toLong()).toInt())
                iso.readInto(pos, buf)
                md.update(buf.array(), 0, buf.limit())
                pos += buf.limit()
                val percent = (pos * 100 / total).toInt()
                if (percent != shown) {
                    shown = percent
                    progress(percent)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        suspend fun copyOut(iso: IsoImage, offset: Long, size: Long, out: File, buf: ByteBuffer, onBytes: (Long) -> Unit = {}) {
            out.outputStream().channel.use { oc ->
                var pos = offset
                var left = size
                while (left > 0) {
                    coroutineContext.ensureActive()
                    buf.clear()
                    buf.limit(minOf(left, buf.capacity().toLong()).toInt())
                    iso.readInto(pos, buf)
                    buf.flip()
                    val n = buf.limit().toLong()
                    while (buf.hasRemaining()) oc.write(buf)
                    pos += n
                    left -= n
                    onBytes(n)
                }
            }
        }
    }
}
