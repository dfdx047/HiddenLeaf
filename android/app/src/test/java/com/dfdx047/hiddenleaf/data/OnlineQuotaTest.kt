package com.dfdx047.hiddenleaf.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The free plan's notice (data/OnlineQuota.kt): when it ends, the hours shown, which answers count. Plain JVM, no Android. */
class OnlineQuotaTest {
    private val h = 3_600_000L
    private val day = 24 * h
    private val midnight = 20_405 * day // 2025-11-13 00:00 UTC (any whole day)

    @Test
    fun resetsAtTheNextMidnightUtc() {
        assertEquals(midnight + day, OnlineQuota.resetAt(midnight))
        assertEquals(midnight + day, OnlineQuota.resetAt(midnight + 23 * h + 59 * 60_000))
        val at = midnight + 21 * h + 30 * 60_000 // 21:30 UTC (18:30 in Brasília)
        assertTrue(OnlineQuota.active(at, at))
        assertTrue(OnlineQuota.active(at, midnight + day - 1))
        assertFalse(OnlineQuota.active(at, midnight + day))
        assertFalse(OnlineQuota.active(0L, at))
    }

    @Test
    fun hoursLeftRoundUp() {
        val at = midnight + 21 * h + 30 * 60_000
        assertEquals(3, OnlineQuota.hoursLeft(at, at)) // 2 h 30 min left
        assertEquals(1, OnlineQuota.hoursLeft(at, midnight + day - 60_000))
        assertEquals(24, OnlineQuota.hoursLeft(midnight, midnight))
        assertEquals(22, OnlineQuota.hoursLeft(midnight + 2 * h, midnight + 2 * h))
    }

    @Test
    fun whichAnswersMeanUsedUp() {
        // Cloudflare's own page for a Worker over its daily requests (HTML, 429, error 1027)
        assertTrue(OnlineQuota.isQuotaAnswer(429, "<!DOCTYPE html><title>Worker exceeded free tier daily request limit | Cloudflare</title> Error 1027"))
        assertTrue(OnlineQuota.isQuotaAnswer(503, "error code: 1027"))
        // the Worker's own answer when D1 or the Durable Objects are used up
        assertTrue(OnlineQuota.isQuotaAnswer(429, """{"error":"quota"}"""))
        // the Worker's own 429 for too many rooms from one address, and its other errors: not the free plan
        assertFalse(OnlineQuota.isQuotaAnswer(429, """{"error":"busy"}"""))
        assertFalse(OnlineQuota.isQuotaAnswer(503, """{"error":"unavailable"}"""))
        assertFalse(OnlineQuota.isQuotaAnswer(502, "<html>Bad gateway</html>"))
    }
}
