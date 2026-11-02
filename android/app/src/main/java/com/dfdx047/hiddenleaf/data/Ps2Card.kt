package com.dfdx047.hiddenleaf.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar

/**
 * A PlayStation 2 memory card image (what PCSX2 keeps as a .ps2 file): read and written whole, in memory.
 * [Entry.path] is relative to the card's root with "/" between folders; a folder is an entry with data == null.
 */
object Ps2Card {
    class Entry(val path: String, val data: ByteArray?)

    private const val MAGIC = "Sony PS2 Memory Card Format "
    private const val PAGE = 512
    private const val SPARE = 16
    private const val PAGES_PER_CLUSTER = 2
    private const val PAGES_PER_BLOCK = 16
    private const val CLUSTER = PAGE * PAGES_PER_CLUSTER
    private const val CLUSTERS = 8192
    private const val IFC = 8
    private const val FAT_FIRST = 9
    private const val ALLOC_OFFSET = 41
    private const val ALLOC_END = 8135
    private const val FAT_END = -1                  // 0xFFFFFFFF: the last cluster of a chain
    private const val FAT_FREE = 0x7FFFFFFF
    private const val FAT_USED = Int.MIN_VALUE      // 0x80000000
    private const val DIR_ENTRY = 512
    private const val MODE_DIR = 0x8427
    private const val MODE_FILE = 0x8417
    private const val MODE_DOTDOT = 0xA426
    private const val MODE_EXISTS = 0x8000
    private const val MODE_IS_DIR = 0x20

    fun isCard(image: ByteArray): Boolean =
        image.size >= PAGE && String(image, 0, MAGIC.length, Charsets.ISO_8859_1) == MAGIC

    // ---- reading
    fun parse(image: ByteArray): List<Entry> {
        require(isCard(image)) { "not a memory card" }
        val b = ByteBuffer.wrap(image).order(ByteOrder.LITTLE_ENDIAN)
        val pageLen = b.getShort(40).toInt() and 0xFFFF
        val perCluster = b.getShort(42).toInt() and 0xFFFF
        val clusters = b.getInt(48)
        val alloc = b.getInt(52)
        val root = b.getInt(60)
        val clusterBytes = pageLen * perCluster
        // (with or without the spare bytes after each page)
        val raw = if (image.size.toLong() == clusters.toLong() * perCluster * pageLen) pageLen else pageLen + (pageLen / PAGE) * SPARE

        fun cluster(n: Int): ByteArray {
            val out = ByteArray(clusterBytes)
            for (p in 0 until perCluster) {
                val at = (n.toLong() * perCluster + p) * raw
                require(at + pageLen <= image.size) { "the card is cut short" }
                System.arraycopy(image, at.toInt(), out, p * pageLen, pageLen)
            }
            return out
        }
        val perFat = clusterBytes / 4
        val fatCache = HashMap<Int, ByteBuffer>()
        fun word(c: Int) = fatCache.getOrPut(c) { ByteBuffer.wrap(cluster(c)).order(ByteOrder.LITTLE_ENDIAN) }
        fun fat(n: Int): Int {
            val index = n / perFat
            val ifc = b.getInt(80 + (index / perFat) * 4)
            val fatCluster = word(ifc).getInt((index % perFat) * 4)
            return word(fatCluster).getInt((n % perFat) * 4)
        }
        fun read(first: Int, length: Int): ByteArray {
            val out = ByteArray(length)
            var c = first
            var done = 0
            var guard = clusters
            while (done < length) {
                require(guard-- > 0) { "the card's table loops" }
                val data = cluster(c + alloc)
                val n = minOf(clusterBytes, length - done)
                System.arraycopy(data, 0, out, done, n)
                done += n
                if (done >= length) break
                val next = fat(c)
                require(next != FAT_END && next and FAT_USED != 0) { "a file ends early" }
                c = next and FAT_FREE
            }
            return out
        }
        val result = ArrayList<Entry>()
        fun dir(first: Int, count: Int, prefix: String) {
            val raws = read(first, count * DIR_ENTRY)
            val d = ByteBuffer.wrap(raws).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) {
                val at = i * DIR_ENTRY
                val mode = d.getShort(at).toInt() and 0xFFFF
                if (mode and MODE_EXISTS == 0) continue
                var end = at + 64
                while (end < at + 96 && raws[end] != 0.toByte()) end++
                val name = String(raws, at + 64, end - at - 64, Charsets.ISO_8859_1)
                if (name == "." || name == ".." || name.isEmpty() || name.contains('/')) continue
                val length = d.getInt(at + 4)
                val c = d.getInt(at + 16)
                if (mode and MODE_IS_DIR != 0) {
                    result += Entry(prefix + name, null)
                    dir(c, length, "$prefix$name/")
                } else {
                    result += Entry(prefix + name, if (length > 0) read(c, length) else ByteArray(0))
                }
            }
        }
        val first = read(root, DIR_ENTRY)
        val rootCount = ByteBuffer.wrap(first).order(ByteOrder.LITTLE_ENDIAN).getInt(4)
        dir(root, rootCount, "")
        return result
    }

    // ---- writing
    private class Node(val name: String, val data: ByteArray?) {
        val children = ArrayList<Node>()
    }

    /** A freshly formatted 8 MB card holding [entries]. Throws if they do not fit. */
    fun build(entries: List<Entry>): ByteArray {
        val root = Node("", null)
        fun folder(path: List<String>): Node {
            var n = root
            for (part in path) {
                n = n.children.firstOrNull { it.name == part && it.data == null } ?: Node(part, null).also { n.children += it }
            }
            return n
        }
        for (e in entries) {
            val parts = e.path.split('/').filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue
            if (e.data == null) folder(parts) else folder(parts.dropLast(1)).children += Node(parts.last(), e.data)
        }

        val image = ByteArray(CLUSTERS * PAGES_PER_CLUSTER * (PAGE + SPARE)) { 0xFF.toByte() }
        val fat = IntArray(CLUSTERS) { if (it < ALLOC_END) FAT_FREE else FAT_END }
        var nextFree = 0

        fun writeCluster(n: Int, data: ByteArray, from: Int) {
            for (p in 0 until PAGES_PER_CLUSTER) {
                val page = ByteArray(PAGE)
                val start = from + p * PAGE
                if (start < data.size) System.arraycopy(data, start, page, 0, minOf(PAGE, data.size - start))
                writePage(image, n * PAGES_PER_CLUSTER + p, page)
            }
        }
        fun store(data: ByteArray, atLeastOne: Boolean): Int {
            val need = (data.size + CLUSTER - 1) / CLUSTER
            if (need == 0 && !atLeastOne) return FAT_FREE
            val count = maxOf(need, 1)
            require(nextFree + count <= ALLOC_END) { "the card is full" }
            val first = nextFree
            for (i in 0 until count) {
                val c = nextFree++
                writeCluster(c + ALLOC_OFFSET, data, i * CLUSTER)
                fat[c] = if (i == count - 1) FAT_END else (FAT_USED or (c + 1))
            }
            return first
        }
        val now = tod()
        fun rawEntry(out: ByteBuffer, index: Int, mode: Int, length: Int, cluster: Int, dirEntry: Int, name: String) {
            val at = index * DIR_ENTRY
            out.putShort(at, mode.toShort())
            out.putInt(at + 4, length)
            out.position(at + 8); out.put(now)
            out.putInt(at + 16, cluster)
            out.putInt(at + 20, dirEntry)
            out.position(at + 24); out.put(now)
            val n = name.toByteArray(Charsets.ISO_8859_1)
            out.position(at + 64); out.put(n, 0, minOf(n.size, 31))
        }
        // A folder's entries come after its contents' clusters are known, and its own first cluster goes into its children's
        // "." : so its clusters are taken first, then written at the end.
        fun folderOut(node: Node, parentCluster: Int, parentIndex: Int, isRoot: Boolean): Pair<Int, Int> {
            val count = node.children.size + 2
            val clusters = (count * DIR_ENTRY + CLUSTER - 1) / CLUSTER
            require(nextFree + clusters <= ALLOC_END) { "the card is full" }
            val self = nextFree
            for (i in 0 until clusters) {
                val c = nextFree++
                fat[c] = if (i == clusters - 1) FAT_END else (FAT_USED or (c + 1))
            }
            val raws = ByteArray(clusters * CLUSTER)
            val d = ByteBuffer.wrap(raws).order(ByteOrder.LITTLE_ENDIAN)
            rawEntry(d, 0, MODE_DIR, if (isRoot) count else 0, if (isRoot) 0 else parentCluster, if (isRoot) 0 else parentIndex, ".")
            rawEntry(d, 1, MODE_DOTDOT, 0, 0, 0, "..")
            node.children.forEachIndexed { i, child ->
                val index = i + 2
                if (child.data == null) {
                    val (c, n) = folderOut(child, self, index, false)
                    rawEntry(d, index, MODE_DIR, n, c, 0, child.name)
                } else {
                    val c = store(child.data, atLeastOne = false)
                    rawEntry(d, index, MODE_FILE, child.data.size, if (child.data.isEmpty()) FAT_FREE else c, 0, child.name)
                }
            }
            for (i in 0 until clusters) writeCluster(self + i + ALLOC_OFFSET, raws, i * CLUSTER)
            return self to count
        }
        folderOut(root, 0, 0, true)

        // the tables
        val perFat = CLUSTER / 4
        val fatClusters = (ALLOC_OFFSET - FAT_FIRST)
        for (i in 0 until fatClusters) {
            val page = ByteArray(CLUSTER)
            val w = ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN)
            for (e in 0 until perFat) w.putInt(e * 4, fat[i * perFat + e])
            writeCluster(FAT_FIRST + i, page, 0)
        }
        val ifc = ByteArray(CLUSTER) { 0xFF.toByte() }
        val w = ByteBuffer.wrap(ifc).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until fatClusters) w.putInt(i * 4, FAT_FIRST + i)
        writeCluster(IFC, ifc, 0)

        // the superblock
        val sb = ByteArray(PAGE) { 0xFF.toByte() }
        val s = ByteBuffer.wrap(sb).order(ByteOrder.LITTLE_ENDIAN)
        val text = (MAGIC + "1.2.0.0").toByteArray(Charsets.ISO_8859_1)
        java.util.Arrays.fill(sb, 0, 40, 0)
        System.arraycopy(text, 0, sb, 0, text.size)
        s.putShort(40, PAGE.toShort())
        s.putShort(42, PAGES_PER_CLUSTER.toShort())
        s.putShort(44, PAGES_PER_BLOCK.toShort())
        s.putShort(46, 0xFF00.toShort())
        s.putInt(48, CLUSTERS)
        s.putInt(52, ALLOC_OFFSET)
        s.putInt(56, ALLOC_END)
        s.putInt(60, 0)
        s.putInt(64, 1023)
        s.putInt(68, 1022)
        s.putInt(72, 0); s.putInt(76, 0)
        s.putInt(80, IFC)
        sb[336] = 2
        sb[337] = 0x52
        writePage(image, 0, sb)
        return image
    }

    private fun writePage(image: ByteArray, page: Int, data: ByteArray) {
        val at = page * (PAGE + SPARE)
        System.arraycopy(data, 0, image, at, PAGE)
        for (chunk in 0 until 3) {
            val ecc = ecc(data, chunk * 128)
            System.arraycopy(ecc, 0, image, at + PAGE + chunk * 3, 3)
        }
        java.util.Arrays.fill(image, at + PAGE + 9, at + PAGE + SPARE, 0)
    }

    private val PARITY = BooleanArray(256) { Integer.bitCount(it) and 1 == 1 }
    private val COLUMN = IntArray(256) { b ->
        val masks = intArrayOf(0x55, 0x33, 0x0F, 0x00, 0xAA, 0xCC, 0xF0)
        var m = 0
        for (i in masks.indices) if (PARITY[b and masks[i]]) m = m or (1 shl i)
        m
    }

    /** The Hamming code of 128 bytes, as the console's card driver computes it. */
    internal fun ecc(d: ByteArray, from: Int): ByteArray {
        var column = 0x77
        var line0 = 0x7F
        var line1 = 0x7F
        for (i in 0 until 128) {
            val b = d[from + i].toInt() and 0xFF
            column = column xor COLUMN[b]
            if (PARITY[b]) {
                line0 = line0 xor i.inv()
                line1 = line1 xor i
            }
        }
        return byteArrayOf(column.toByte(), (line0 and 0x7F).toByte(), (line1 and 0x7F).toByte())
    }

    private fun tod(): ByteArray {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        return byteArrayOf(
            0, c.get(Calendar.SECOND).toByte(), c.get(Calendar.MINUTE).toByte(), c.get(Calendar.HOUR_OF_DAY).toByte(),
            c.get(Calendar.DAY_OF_MONTH).toByte(), (c.get(Calendar.MONTH) + 1).toByte(), (year and 0xFF).toByte(), (year shr 8).toByte(),
        )
    }
}
