package com.dfdx047.hiddenleaf.data

import android.content.Context
import android.net.wifi.WifiManager
import com.dfdx047.hiddenleaf.R
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.SecureRandom

/**
 * Local network play: two phones on the same Wi-Fi (or one on the other's hotspot) find each other without the rooms service.
 *
 * The host's game socket (the UDP port the engine opens again, as for an online room) says once a second, by broadcast to
 * [PORT], that a match is open: a [Beacon]. A guest listening on [PORT] lists the beacons; picking one sends a [Join] from its
 * own game socket to the host's game socket, and the host answers [Accept] (or [Reject]). Both then write the same session file
 * as an online match (relay off, code "LAN" + the room id, the other's LAN addresses) and start the game the same way
 * (OnlineLobby.launchMatch). Nothing of the engine changes: for it this is an online match whose addresses happen to be local.
 *
 * The messages are a few "key=value" lines after the line [MAGIC] (as the session file; no JSON, so the codec runs in a plain
 * JVM test), well under one Wi-Fi frame.
 */
object Lan {
    /** The port the guests listen on for the hosts' beacons (only discovery: the match goes on the game sockets' own ports). */
    const val PORT = 47655
    const val MAGIC = "HLLAN"
    const val BEACON_EVERY_MS = 1000L
    /** A host not heard for this long leaves the list (four beacons lost in a row: it closed, or it is out of reach). */
    const val HOST_GONE_MS = 4000L
    /** The guest's join is said again every [JOIN_EVERY_MS] until an answer or [JOIN_TIMEOUT_MS]. */
    const val JOIN_EVERY_MS = 400L
    const val JOIN_TIMEOUT_MS = 6000L
    /**
     * After its accept the host keeps saying it, on its own every [ACCEPT_EVERY_MS] and to each of the guest's repeats, this long
     * before it hands the port to the engine (2026-11-14: with 800 ms and answers only to repeats, a Honor's accept never reached an
     * RG Cube on the same Wi-Fi: the host played alone for 90 s while the guest still asked to come in).
     */
    const val ACCEPT_LINGER_MS = 2500L
    const val ACCEPT_EVERY_MS = 150L
    /** An open match or a search ends by itself after this long (nobody is left broadcasting all night). */
    const val HOST_MAX_MS = 10 * 60_000L
    const val SEARCH_MAX_MS = 5 * 60_000L
    private const val MAX_BYTES = 1400

    sealed interface Msg

    /** The host's "a match is open here": [port] is its game socket's, [addrs] its LAN addresses with that port. */
    data class Beacon(
        val v: Int, val game: String, val room: String, val name: String, val fp: String, val port: Int, val addrs: List<String>,
    ) : Msg

    /** The guest's request, sent from its game socket; [nonce] ties the answer to this request. */
    data class Join(
        val v: Int, val game: String, val room: String, val name: String, val fp: String, val port: Int, val addrs: List<String>,
        val nonce: String,
    ) : Msg

    data class Accept(val room: String, val name: String, val delay: Int, val nonce: String) : Msg

    /** [why] as the rooms service says it ("different", "full", "version"). */
    data class Reject(val room: String, val why: String, val nonce: String) : Msg

    /** A host heard on the network: its beacon, the address it came from and when (milliseconds, the guest's clock). */
    data class Found(val beacon: Beacon, val from: String, val seen: Long) {
        /** Where to reach its game socket: the address the beacon came from first (the one that surely works), then what it says. */
        fun candidates(): List<String> = LinkedHashSet<String>().apply {
            if (IP.matches(from)) add("$from:${beacon.port}")
            addAll(beacon.addrs)
        }.take(8)

        /** Same network version, same game, same app build and mods: otherwise the two games would not stay the same. */
        fun compatible(fp: String) = beacon.v == Online.NET_VERSION && beacon.game == Online.GAME && beacon.fp == fp
    }

    private val IP = Regex("""\d{1,3}(\.\d{1,3}){3}""")
    private val ADDR = Regex("""\d{1,3}(\.\d{1,3}){3}:\d{1,5}""")
    private val ROOM = Regex("[0-9A-F]{6}")
    private val NONCE = Regex("[0-9a-f]{1,16}")
    private val random = SecureRandom()

    fun newRoom(): String = "%06X".format(random.nextInt(0x1000000))
    fun newNonce(): String = "%016x".format(random.nextLong())

    /** The session's code: the engine only hashes it (both sides must write the same), the "LAN" says where it came from in the logs. */
    fun code(room: String) = "LAN$room"

    /** The session file for a LAN match: no relay (there is no room on the service), the other's local addresses. */
    fun session(host: Boolean, port: Int, room: String, me: String, peer: String, addrs: List<String>, delay: Int) =
        Online.Session(host, port, code(room), me, peer, addrs.filter { ADDR.matches(it) }.distinct().take(8), delay, false, "")

    // ---- the messages

    fun encode(m: Msg): ByteArray = buildString {
        append(MAGIC).append('\n')
        fun put(k: String, v: Any) { append(k).append('=').append(v).append('\n') }
        when (m) {
            is Beacon -> {
                put("t", "beacon"); put("v", m.v); put("game", m.game); put("room", m.room); put("name", Online.cleanName(m.name))
                put("fp", cleanFp(m.fp)); put("port", m.port)
                m.addrs.take(4).forEach { put("addr", it) }
            }
            is Join -> {
                put("t", "join"); put("v", m.v); put("game", m.game); put("room", m.room); put("name", Online.cleanName(m.name))
                put("fp", cleanFp(m.fp)); put("port", m.port); put("nonce", m.nonce)
                m.addrs.take(4).forEach { put("addr", it) }
            }
            is Accept -> {
                put("t", "accept"); put("room", m.room); put("name", Online.cleanName(m.name)); put("delay", m.delay.coerceIn(0, 4))
                put("nonce", m.nonce)
            }
            is Reject -> { put("t", "reject"); put("room", m.room); put("why", m.why.filter { it.isLetter() }.take(16)); put("nonce", m.nonce) }
        }
    }.toByteArray(Charsets.US_ASCII)

    /** A message from the network, or null for anything else (another app on the port, a broken packet, a field out of range). */
    fun decode(b: ByteArray, n: Int = b.size): Msg? {
        if (n <= MAGIC.length || n > MAX_BYTES) return null
        val lines = String(b, 0, n, Charsets.US_ASCII).split('\n')
        if (lines.first() != MAGIC) return null
        val map = lines.drop(1).mapNotNull { l -> l.indexOf('=').takeIf { it > 0 }?.let { l.substring(0, it) to l.substring(it + 1) } }
            .groupBy({ it.first }, { it.second })
        fun one(k: String) = map[k]?.firstOrNull()
        val room = one("room")?.takeIf { ROOM.matches(it) } ?: return null
        val name = Online.cleanName(one("name").orEmpty())
        val nonce = one("nonce")
        return when (one("t")) {
            "beacon", "join" -> {
                val v = one("v")?.toIntOrNull() ?: return null
                val game = one("game")?.take(16) ?: return null
                val port = one("port")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
                val addrs = map["addr"].orEmpty().filter { ADDR.matches(it) }.take(4)
                val fp = cleanFp(one("fp").orEmpty())
                if (one("t") == "beacon") Beacon(v, game, room, name, fp, port, addrs)
                else Join(v, game, room, name, fp, port, addrs, nonce?.takeIf { NONCE.matches(it) } ?: return null)
            }
            "accept" -> Accept(room, name, one("delay")?.toIntOrNull()?.coerceIn(0, 4) ?: 0, nonce?.takeIf { NONCE.matches(it) } ?: return null)
            "reject" -> Reject(room, one("why").orEmpty().filter { it.isLetter() }.take(16), nonce?.takeIf { NONCE.matches(it) } ?: return null)
            else -> null
        }
    }

    // (the fingerprint's own letters, Online.fingerprint)
    private fun cleanFp(s: String) = s.replace(Regex("[^0-9A-Za-z._:+-]"), "_").take(96)

    /** What the host said, as a text of the Online screen. */
    fun explain(why: String): Int = when (why) {
        "different", "version", "game" -> R.string.online_err_different
        "full" -> R.string.online_err_full
        else -> R.string.lan_err_refused
    }

    // ---- the network

    /** Where the beacons go: the whole-network broadcast and each network's own (some routers and hotspots pass only the latter). */
    fun broadcastTargets(): List<InetAddress> {
        val out = LinkedHashSet<InetAddress>()
        runCatching { out += InetAddress.getByName("255.255.255.255") }
        runCatching {
            for (i in NetworkInterface.getNetworkInterfaces().toList()) {
                if (!i.isUp || i.isLoopback) continue
                for (a in i.interfaceAddresses) if (a.address is Inet4Address) a.broadcast?.let { out += it }
            }
        }
        return out.toList()
    }

    /**
     * Every other address of this phone's Wi-Fi (or cable, or hotspot) network, its /24, for the beacon said to each one directly
     * (2026-11-14: a Honor's broadcast beacon stopped reaching a Mi A3 on the router's other 5 GHz network, while packets sent to
     * its address went through; a router does not always carry broadcasts from one of its networks to the other).
     */
    fun sweepTargets(): List<InetAddress> {
        val out = ArrayList<InetAddress>()
        runCatching {
            for (i in NetworkInterface.getNetworkInterfaces().toList()) {
                if (!i.isUp || i.isLoopback || !SWEEP_IFACE.containsMatchIn(i.name)) continue
                for (a in i.interfaceAddresses) {
                    val ip = a.address as? Inet4Address ?: continue
                    if (!ip.isSiteLocalAddress || a.networkPrefixLength < 16) continue
                    val b = ip.address
                    for (last in 1..254) if (last != (b[3].toInt() and 0xFF)) out += InetAddress.getByAddress(byteArrayOf(b[0], b[1], b[2], last.toByte()))
                }
            }
        }
        return out
    }
    private val SWEEP_IFACE = Regex("^(wlan|swlan|eth|ap|rndis|usb)")

    /** How often the host says its beacon to each address of its network (254 small packets a turn) while nobody has come. */
    const val SWEEP_EVERY_MS = 3000L

    /**
     * Wi-Fi drivers drop broadcast and multicast packets for apps that did not ask (to save battery): the guest holds this lock
     * while it listens for beacons. Null when there is no Wi-Fi manager (the search still works on a cable or a hotspot).
     */
    fun multicastLock(context: Context): WifiManager.MulticastLock? = runCatching {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifi?.createMulticastLock("hiddenleaf-lan")?.apply { setReferenceCounted(false); acquire() }
    }.getOrNull()
}
