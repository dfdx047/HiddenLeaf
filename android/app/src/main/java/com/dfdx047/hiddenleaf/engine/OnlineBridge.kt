package com.dfdx047.hiddenleaf.engine

/** The online match's line to the engine (recomp/android/host/online/hl_online.cpp). */
object OnlineBridge {
    /**
     * Where the match is, as "key=value;..." (phase starting|linking|playing|over|failed, me, peer, host, p1, rtt, delay, field,
     * waits, desyncs, relay, linked, reason). Empty when this start is not an online match.
     */
    @JvmStatic external fun nativeStatus(): String

    /** The player leaves: the other side is told at the next vsync. */
    @JvmStatic external fun nativeLeave()

    /** A packet that came through the room's relay. */
    @JvmStatic external fun nativeRelayIn(data: ByteArray)

    /** The next packet for the room's relay, waiting up to [timeoutMs]; null when none came. */
    @JvmStatic external fun nativeRelayOut(timeoutMs: Int): ByteArray?

    /** (An engine without the online match, an old one: nothing, rather than a crash.) */
    fun status(): String = try { nativeStatus() } catch (e: UnsatisfiedLinkError) { "" }

    fun leave() {
        try { nativeLeave() } catch (e: UnsatisfiedLinkError) { }
    }

    /** The status line's fields. */
    fun parse(status: String): Map<String, String> =
        status.split(';').mapNotNull { f -> f.indexOf('=').takeIf { it > 0 }?.let { f.substring(0, it) to f.substring(it + 1) } }.toMap()
}
