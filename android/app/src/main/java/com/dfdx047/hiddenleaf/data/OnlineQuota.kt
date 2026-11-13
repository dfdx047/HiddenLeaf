package com.dfdx047.hiddenleaf.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The rooms service's free plan used up for the day. The Worker runs on Cloudflare's free plan: 100,000 requests a day, and D1's and the
 * Durable Objects' own daily limits. Past them Cloudflare answers 429 with its own page (error 1027), or the Worker answers 429
 * {error: "quota"} (online/worker/worker.js); all of them come back at 00:00 UTC. The app remembers when it was told (the launcher's
 * preferences, "online"), shows it on Home and on the Online page (ui/screens/OnlineScreen.kt, OnlineQuotaNotice), and forgets it at
 * that 00:00 UTC or as soon as the service answers again.
 */
object OnlineQuota {
    private const val KEY = "quota_at"
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    @Volatile private var prefs: SharedPreferences? = null
    private val _since = MutableStateFlow(0L)
    /** When the service last said its free plan was used up (ms since 1970), 0 when it did not. */
    val since: StateFlow<Long> = _since.asStateFlow()

    /** Reads what was remembered (once per process; the launcher's view models call it). */
    fun attach(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("online", Context.MODE_PRIVATE)
        prefs = p
        // (what was kept, or what a request said before this was attached, the newer)
        val at = maxOf(p.getLong(KEY, 0L), _since.value)
        set(if (active(at, System.currentTimeMillis())) at else 0L)
    }

    /** The service said it is used up. */
    fun exhausted(nowMs: Long = System.currentTimeMillis()) {
        if (active(_since.value, nowMs)) return
        set(nowMs)
    }

    /** The service answered as itself: it is working. */
    fun working() {
        if (_since.value != 0L) set(0L)
    }

    private fun set(at: Long) {
        _since.value = at
        prefs?.edit()?.putLong(KEY, at)?.apply()
    }

    /** The next 00:00 UTC after [atMs], when Cloudflare's daily free limits start again. */
    fun resetAt(atMs: Long): Long = (atMs / DAY_MS + 1) * DAY_MS

    fun active(atMs: Long, nowMs: Long): Boolean = atMs > 0L && nowMs < resetAt(atMs)

    /** Whole hours left to the reset, 1 to 24 (rounded up: "1 hour" in the last one). */
    fun hoursLeft(atMs: Long, nowMs: Long): Int = ((resetAt(atMs) - nowMs + HOUR_MS - 1) / HOUR_MS).toInt().coerceIn(1, 24)

    /**
     * Whether an answer of the service says its free plan is used up: the Worker's own {error: "quota"}, or one of Cloudflare's pages (not
     * JSON) with 429 or error 1027 ("Worker exceeded free tier daily request limit"). The Worker's own 429 "busy" (too many rooms from one
     * address) is not.
     */
    fun isQuotaAnswer(code: Int, body: String): Boolean {
        // (plain text checks: this runs in the JVM's unit tests too, where org.json is Android's empty stub)
        if (body.trimStart().startsWith("{")) return Regex(""""error"\s*:\s*"quota"""").containsMatchIn(body)
        return code == 429 || body.contains("1027")
    }
}
