package com.dfdx047.hiddenleaf.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Ps2CardTest {
    private fun sample() = listOf(
        Ps2Card.Entry("BESLES-55605", null),
        Ps2Card.Entry("BESLES-55605/icon.sys", ByteArray(964) { (it % 251).toByte() }),
        Ps2Card.Entry("BESLES-55605/BESLES-55605", ByteArray(70000) { (it * 7 % 253).toByte() }),
        Ps2Card.Entry("BESLES-55605/empty", ByteArray(0)),
        Ps2Card.Entry("OTHER", null),
        Ps2Card.Entry("OTHER/nested", null),
        Ps2Card.Entry("OTHER/nested/a.bin", ByteArray(1024) { 1 }),
    ) + (0 until 30).map { Ps2Card.Entry("OTHER/f$it", ByteArray(it * 100 + 1) { 2 }) }

    @Test
    fun roundTrip() {
        val image = Ps2Card.build(sample())
        assertEquals(8650752, image.size)
        assertTrue(Ps2Card.isCard(image))
        val back = Ps2Card.parse(image).associateBy { it.path }
        for (e in sample()) {
            val b = back[e.path] ?: error("missing ${e.path}")
            if (e.data == null) assertEquals(null, b.data) else assertArrayEquals(e.path, e.data, b.data)
        }
        assertEquals(sample().size, back.size)
        // for tools/check of the format from outside
        System.getProperty("java.io.tmpdir")?.let { File(it, "hl_card_test.ps2").writeBytes(image) }
    }

    @Test
    fun eccOfZeros() {
        // (a page of zeros is 0x77 0x7F 0x7F on a real card)
        val e = Ps2Card.ecc(ByteArray(128), 0)
        assertArrayEquals(byteArrayOf(0x77, 0x7F, 0x7F), e)
    }
}
