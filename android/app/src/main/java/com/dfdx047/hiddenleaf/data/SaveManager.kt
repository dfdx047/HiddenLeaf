package com.dfdx047.hiddenleaf.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * The memory card of the game: <root>/saves, one folder per save, what the runtime sees as mc0. Before anything is imported
 * a zip of it goes to <root>/saves_backups (the last [KEEP]).
 */
class SaveManager(private val context: Context, private val paths: GamePaths) {
    private val backups = File(paths.root, "saves_backups")

    /** The save folders on the card, with their size in KB. */
    fun saves(): List<Pair<String, Long>> =
        paths.saves.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.map { d ->
            d.name to d.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1024
        }.orEmpty()

    fun backupList(): List<File> =
        backups.listFiles()?.filter { it.name.endsWith(".zip") }?.sortedByDescending { it.name }.orEmpty()

    /** A zip of the card as it is now. Null when there is nothing on it. */
    fun backup(): File? {
        val files = paths.saves.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return null
        backups.mkdirs()
        val name = "saves-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".zip"
        val out = File(backups, name)
        ZipOutputStream(out.outputStream().buffered()).use { z ->
            for (f in files) {
                z.putNextEntry(ZipEntry(f.relativeTo(paths.saves).path))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        backupList().drop(KEEP).forEach { it.delete() }
        return out
    }

    /**
     * Installs a clean 100% unlocked European save (BESLES-55605NARUTO5) onto mc0, backing up the current card first.
     * Unlocks all characters, support links, Collection tables and max progress with a valid UN5 checksum, and
     * propagates to mods (e.g. UN6 A35) via [ModSaves.prepare].
     */
    fun install100PercentSave(): Int {
        backup()
        val folder = File(paths.saves, BASE_SAVE_FOLDER).apply { mkdirs() }
        val data1File = File(folder, "data01")
        val data = if (data1File.exists() && data1File.length() == SLOT_SIZE.toLong()) {
            val existing = data1File.readBytes()
            if (existing.any { it != 0xFF.toByte() }) existing else makeCleanSlot()
        } else {
            makeCleanSlot()
        }
        // Apply 100% unlocks matching hl_game.cpp unlockEverything()
        for (i in 0x38 until 0x38 + 0x8D0) data[i] = 0xFF.toByte()
        for (i in 0x909 until 0x971) data[i] = 3
        for (i in 0x990 until 0xB6D) if ((data[i].toInt() and 0xFF) < 3) data[i] = 3
        // Progress counter (0x6E = 110)
        data[0xE68] = 0x6E; data[0xE69] = 0; data[0xE6A] = 0; data[0xE6B] = 0
        // Recompute UN5 16-bit checksum: sum of bytes [0..1] + [4..9215], stored little-endian at [2..3]
        var sum = (data[0].toInt() and 0xFF) + (data[1].toInt() and 0xFF)
        for (i in 4 until SLOT_SIZE) sum += (data[i].toInt() and 0xFF)
        val cks = sum and 0xFFFF
        data[2] = (cks and 0xFF).toByte()
        data[3] = ((cks ushr 8) and 0xFF).toByte()

        data1File.writeBytes(data)
        File(folder, "data04").writeBytes(data) // autosave mirror
        val empty = ByteArray(SLOT_SIZE) { 0xFF.toByte() }
        for (slot in listOf("data02", "data03")) {
            val f = File(folder, slot)
            if (!f.exists()) f.writeBytes(empty)
        }
        // 64-byte header (4 x 16-byte slot descriptors: slot 1 and slot 4 active with matching checksum)
        val hdr = ByteArray(64)
        fun writeSlotDesc(off: Int, active: Boolean) {
            if (!active) {
                for (i in 9 until 16) hdr[off + i] = 0xFF.toByte()
                return
            }
            hdr[off] = 1
            hdr[off + 1] = 0
            hdr[off + 2] = data[2]
            hdr[off + 3] = data[3]
            hdr[off + 4] = data[4]
            hdr[off + 5] = data[5]
            hdr[off + 6] = data[6]
            hdr[off + 7] = data[7]
            // Timestamp (2026-11-24 00:00:00)
            hdr[off + 8] = 0; hdr[off + 9] = 0; hdr[off + 10] = 0; hdr[off + 11] = 0
            hdr[off + 12] = 24; hdr[off + 13] = 11; hdr[off + 14] = (2026 and 0xFF).toByte(); hdr[off + 15] = (2026 ushr 8).toByte()
        }
        writeSlotDesc(0, true)
        writeSlotDesc(16, false)
        writeSlotDesc(32, false)
        writeSlotDesc(48, true)
        File(folder, BASE_SAVE_FOLDER).writeBytes(hdr)
        runCatching { ModSaves.prepare(paths) }
        return 1
    }

    private fun makeCleanSlot(): ByteArray {
        val b = ByteArray(SLOT_SIZE)
        b[0] = 0x03; b[1] = 0x00
        b[4] = 0xA8.toByte(); b[5] = 0x04
        b[13] = 0x01; b[14] = 0x01
        b[16] = 0x03; b[17] = 0xFF.toByte(); b[18] = 0x10
        b[20] = 0x20; b[22] = 0x40
        b[24] = 0x80.toByte(); b[26] = 0x04
        b[28] = 0x08; b[30] = 0x01
        return b
    }

    /** A .ps2 card image or a zip. The saves it has replace those of the same name; the others stay. How many came in. */
    fun importFile(uri: Uri): Int {
        val temp = File(paths.temp, "import.card").apply { parentFile?.mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)!!.use { i -> temp.outputStream().use { i.copyTo(it) } }
            val head = temp.inputStream().use { s -> ByteArray(512).also { s.read(it) } }
            if (Ps2Card.isCard(head)) return install(Ps2Card.parse(temp.readBytes()))
            require(head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) { "neither a .ps2 card nor a zip" }
            val entries = ArrayList<Ps2Card.Entry>()
            ZipFile(temp).use { z ->
                for (e in z.entries()) {
                    val path = cleanPath(e.name) ?: continue
                    entries += Ps2Card.Entry(path, if (e.isDirectory) null else z.getInputStream(e).use { it.readBytes() })
                }
            }
            return install(entries)
        } finally {
            temp.delete()
        }
    }

    /** A folder picked in the system's file manager: mc0 itself (saves in it) or one save. */
    fun importFolder(tree: Uri): Int {
        val entries = ArrayList<Ps2Card.Entry>()
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val rootName = rootId.substringAfterLast('/').substringAfterLast(':')
        fun walk(id: String, prefix: String) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
            context.contentResolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val childId = c.getString(0)
                    val name = c.getString(1)
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        entries += Ps2Card.Entry(prefix + name, null)
                        walk(childId, "$prefix$name/")
                    } else if (c.getLong(3) <= MAX_SAVE_FILE) { // (an ISO left in the folder would not fit in memory)
                        val u = DocumentsContract.buildDocumentUriUsingTree(tree, childId)
                        entries += Ps2Card.Entry(prefix + name, context.contentResolver.openInputStream(u)!!.use { it.readBytes() })
                    }
                }
            }
        }
        walk(rootId, "")
        // (files directly in the folder: it is a save itself)
        val isOneSave = entries.any { it.data != null && !it.path.contains('/') }
        return install(if (isOneSave) entries.map { Ps2Card.Entry("$rootName/${it.path}", it.data) } else entries)
    }

    /** The card as a .ps2 file PCSX2 and the like can open. */
    fun exportCard(uri: Uri) {
        val image = Ps2Card.build(currentEntries())
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(image) }
    }

    /** The card's folders, copied into a folder picked in the system's file manager. */
    fun exportFolder(tree: Uri): Int {
        val r = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val rootDoc = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val made = HashMap<String, Uri>()
        fun dir(path: String): Uri {
            if (path.isEmpty()) return rootDoc
            return made.getOrPut(path) {
                val parent = dir(path.substringBeforeLast('/', ""))
                DocumentsContract.createDocument(r, parent, DocumentsContract.Document.MIME_TYPE_DIR, path.substringAfterLast('/'))!!
            }
        }
        var files = 0
        for (e in currentEntries()) {
            if (e.data == null) {
                dir(e.path)
                continue
            }
            val parent = dir(e.path.substringBeforeLast('/', ""))
            val doc = DocumentsContract.createDocument(r, parent, "application/octet-stream", e.path.substringAfterLast('/'))!!
            r.openOutputStream(doc, "wt")!!.use { it.write(e.data) }
            files++
        }
        return files
    }

    private fun currentEntries(): List<Ps2Card.Entry> =
        paths.saves.walkTopDown().filter { it != paths.saves }.map { f ->
            Ps2Card.Entry(f.relativeTo(paths.saves).path, if (f.isDirectory) null else f.readBytes())
        }.toList()

    private fun cleanPath(raw: String): String? {
        val parts = raw.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
        return if (parts.isEmpty() || parts.any { it == ".." }) null else parts.joinToString("/")
    }

    private fun install(entries: List<Ps2Card.Entry>): Int {
        val clean = entries.mapNotNull { e -> cleanPath(e.path)?.let { Ps2Card.Entry(it, e.data) } }
        val tops = clean.map { it.path.substringBefore('/') }.toSet()
        require(tops.isNotEmpty()) { "there are no saves in it" }
        backup()
        for (t in tops) File(paths.saves, t).deleteRecursively()
        for (e in clean) {
            val f = File(paths.saves, e.path)
            if (e.data == null) {
                f.mkdirs()
            } else {
                f.parentFile?.mkdirs()
                f.writeBytes(e.data)
            }
        }
        return tops.size
    }

    companion object {
        private const val KEEP = 10
        private const val MAX_SAVE_FILE = 8L * 1024 * 1024 // the size of a whole PS2 card
        private const val BASE_SAVE_FOLDER = "BESLES-55605NARUTO5"
        private const val SLOT_SIZE = 9216
    }
}
