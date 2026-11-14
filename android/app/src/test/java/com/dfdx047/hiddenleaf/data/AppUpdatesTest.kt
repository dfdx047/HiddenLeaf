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
    }

    @Test
    fun newestSection() {
        val log = "# Changelog\n\nlinks\n\n## v0.1.0-beta.2 (x)\n\n### Fixes\n- a\n\n## v0.1.0-beta (y)\n\n- b\n"
        assertEquals("### Fixes\n- a", AppUpdates.section(log))
        assertEquals("", AppUpdates.section("nothing"))
    }
}
