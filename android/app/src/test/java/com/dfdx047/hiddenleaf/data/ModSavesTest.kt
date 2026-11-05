package com.dfdx047.hiddenleaf.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ModSavesTest {
    private fun card(): java.io.File {
        val saves = Files.createTempDirectory("saves").toFile()
        val base = java.io.File(saves, ModSaves.BASE_FOLDER).apply { mkdirs() }
        java.io.File(base, ModSaves.BASE_FOLDER).writeBytes(ByteArray(64) { 1 })
        for (n in listOf("data01", "icon.sys", "icon00.icn")) java.io.File(base, n).writeBytes(ByteArray(100) { it.toByte() })
        return saves
    }

    @Test
    fun makesTheModFolderWithTheHeaderRenamed() {
        val saves = card()
        val target = java.io.File(saves, "BASLUS-55606NARUTO6")
        assertTrue(ModSaves.makeFrom(java.io.File(saves, ModSaves.BASE_FOLDER), target))
        assertEquals(setOf("BASLUS-55606NARUTO6", "data01", "icon.sys", "icon00.icn"), target.list()!!.toSet())
        assertArrayEquals(ByteArray(64) { 1 }, java.io.File(target, "BASLUS-55606NARUTO6").readBytes())
        assertFalse(java.io.File(saves, ".BASLUS-55606NARUTO6.tmp").exists())
        assertTrue(java.io.File(saves, ModSaves.BASE_FOLDER).resolve(ModSaves.BASE_FOLDER).isFile) // the base save stays
    }

    @Test
    fun keepsAnExistingModSave() {
        val saves = card()
        val target = java.io.File(saves, "BASLUS-55606NARUTO6").apply { mkdirs() }
        java.io.File(target, "data01").writeBytes(ByteArray(5) { 9 })
        assertFalse(ModSaves.makeFrom(java.io.File(saves, ModSaves.BASE_FOLDER), target))
        assertArrayEquals(ByteArray(5) { 9 }, java.io.File(target, "data01").readBytes())
    }

    @Test
    fun nothingWithoutABaseSave() {
        val saves = Files.createTempDirectory("saves").toFile()
        val target = java.io.File(saves, "BASLUS-55606NARUTO6")
        assertFalse(ModSaves.makeFrom(java.io.File(saves, ModSaves.BASE_FOLDER), target))
        assertFalse(target.exists())
    }
}
