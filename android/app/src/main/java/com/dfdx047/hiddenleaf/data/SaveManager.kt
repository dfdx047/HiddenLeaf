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
    }
}
