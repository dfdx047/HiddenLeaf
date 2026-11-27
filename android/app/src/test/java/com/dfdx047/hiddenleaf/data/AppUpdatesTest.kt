package com.dfdx047.hiddenleaf.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatesTest {
    @Test
    fun tags() {
        assertEquals("0.1.0-beta", AppUpdates.versionOf("v0.1.0-beta"))
        assertEquals("0.2.0", AppUpdates.versionOf("android-v0.2.0"))
        assertEquals("0.2.3", AppUpdates.versionOf("v0.2.3"))
        assertNull(AppUpdates.versionOf("android-nightly"))
        assertNull(AppUpdates.versionOf("good-20261105-1"))
    }

    @Test
    fun order() {
        val c = AppUpdates::compareVersions
        assertTrue(c("0.1.0-beta.2", "0.1.0-beta") > 0)
        assertTrue(c("0.1.0", "0.1.0-beta.9") > 0)
        assertTrue(c("0.1.1-beta", "0.1.0") > 0)
        assertTrue(c("0.1.0-beta.10", "0.1.0-beta.9") > 0)
        assertEquals(0, c("0.1.0-beta", "0.1.0-beta"))
        assertTrue(c("0.1.0-beta", "0.2.0") < 0)
        assertTrue(c("0.2.0-beta", "0.2.1") < 0)
        assertTrue(c("0.2.1", "0.2.2") < 0)
        assertTrue(c("0.2.2", "0.2.3") < 0)
    }

    @Test
    fun versionCodesAlwaysIncreaseForInPlaceUpgrades() {
        val codes = listOf("0.1.0-beta", "0.2.0-beta", "0.2.0-beta.2", "0.2.0", "0.2.1", "0.2.2", "0.2.3", "0.2.4", "0.3.0-beta", "0.3.0", "1.0.0")
            .map { AppUpdates.expectedVersionCode(it) }
        for (i in 1 until codes.size) {
            assertTrue("expected ${codes[i - 1]} < ${codes[i]}", codes[i - 1] < codes[i])
        }
        assertEquals(10001, AppUpdates.expectedVersionCode("0.1.0-beta"))
        assertEquals(20001, AppUpdates.expectedVersionCode("0.2.0-beta"))
        assertEquals(20199, AppUpdates.expectedVersionCode("0.2.1"))
        assertEquals(20299, AppUpdates.expectedVersionCode("0.2.2"))
        assertEquals(20399, AppUpdates.expectedVersionCode("0.2.3"))
    }

    @Test
    fun newestSection() {
        val log = "# Changelog\n\nlinks\n\n## v0.2.3 (27/11/2026)\n\n### Fixes\n- a\n\n## v0.2.2 (26/11/2026)\n\n- b\n"
        assertEquals("### Fixes\n- a", AppUpdates.section(log))
        assertEquals("### Fixes\n- a", AppUpdates.section(log, "0.2.3"))
        assertEquals("- b", AppUpdates.section(log, "0.2.2"))
        assertEquals("", AppUpdates.section("nothing"))
    }

    @Test
    fun trilingualBodyExtraction() {
        val body = """
            ## 🇧🇷 Português — Hidden Leaf v0.2.3

            Novidade PT

            ## 🇺🇸 English — Hidden Leaf v0.2.3

            English news

            ## 🇪🇸 Español — Hidden Leaf v0.2.3

            Novedades ES
        """.trimIndent()
        assertEquals("Novidade PT", AppUpdates.extractBodyForLanguage(body, "pt"))
        assertEquals("English news", AppUpdates.extractBodyForLanguage(body, "en"))
        assertEquals("Novedades ES", AppUpdates.extractBodyForLanguage(body, "es"))
    }

    @Test
    fun atomTagExtraction() {
        val xml = """<feed><entry><link href="https://github.com/dfdx047/HiddenLeaf/releases/tag/v0.2.3"/></entry><entry><link href="https://github.com/dfdx047/HiddenLeaf/releases/tag/v0.2.2"/></entry></feed>"""
        assertEquals(listOf("v0.2.3", "v0.2.2"), AppUpdates.parseAtomTags(xml))
    }
}
