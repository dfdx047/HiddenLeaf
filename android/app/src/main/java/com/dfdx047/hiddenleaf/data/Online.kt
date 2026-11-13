package com.dfdx047.hiddenleaf.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dfdx047.hiddenleaf.BuildConfig
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.engine.EngineBridge
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Online play: the rooms service (online/worker, a Cloudflare Worker) and what the engine needs to start a match.
 *
 * The service only puts two players in touch: a room with a six-digit code, each one's addresses, and the room's relay for when the two
 * phones cannot reach each other directly. The match itself goes between the two engines (recomp/android/host/online), started afresh
 * with the session file this writes (<root>/online/session.txt): who created the room, the UDP port the lobby used (the other side's
 * router knows that one), the other's addresses, the input delay and whether the relay may be used. GameActivity (OnlineGame) reads
 * the same file for the relay's address.
 *
 * The same network version is in the engine (HL_NET_VERSION, net_version.h) and in the service (ALLOWED_VERSIONS).
 */
object Online {
    const val NET_VERSION = 1
    const val GAME = "un5"
    /** The developer's Worker (online/worker, published 2026-11-13); the Online page can still point at another one. */
    const val DEFAULT_SERVER = "https://hiddenleaf-online.hiddenleaf.workers.dev"
    const val MAX_NAME = 16
    /** The engine takes a session file this many seconds old at most (hl_online.cpp, kSessionMaxAge). */
    const val SESSION_MAX_AGE_S = 180L

    class Failure(val why: String) : Exception(why)
    data class Peer(val name: String, val addrs: List<String>, val seen: String)
    data class Opts(val delay: Int, val relay: Boolean)
    data class Room(val code: String, val key: String)
    data class Joined(val key: String, val host: Peer, val opts: Opts)
    data class Hello(val ok: Boolean, val ms: Long, val colo: String, val country: String)

    fun isPlaceholder(server: String) = server.contains("SEU-SUBDOMINIO")

    /** The address as typed: spaces and the last slash off. */
    fun cleanServer(s: String): String = s.trim().trimEnd('/')

    /** The name as the service takes it: printable ASCII without quotes, 16 letters at most. */
    fun cleanName(s: String): String = s.filter { it.code in 0x20..0x7e && it != '"' && it != '\\' }.trim().take(MAX_NAME)

    /** What must be the same on both phones for the two games to stay the same: the app's build, the engine and the mods switched on. */
    fun fingerprint(context: Context, paths: GamePaths): String {
        val lib = runCatching { EngineBridge.libraryFor(context, paths.activeMods) }.getOrDefault("un5")
        // (the engine's list of mods switched on: "modlib/<id>/disc" a line)
        val enabled = runCatching { paths.activeMods.readLines() }.getOrDefault(emptyList())
            .map { it.trim().removePrefix("modlib/").substringBefore('/') }.filter { it.isNotEmpty() }
        val mods = enabled.sorted().joinToString("+").ifEmpty { "none" }
        return "a${BuildConfig.VERSION_CODE}.$lib.$mods".replace(Regex("[^0-9A-Za-z._:+-]"), "_").take(96)
    }

    // ---- the service

    suspend fun hello(server: String): Hello = withContext(Dispatchers.IO) {
        val body = JSONObject().put("v", NET_VERSION).put("game", GAME)
        post(server, "/v1/hello", body) // (the first one pays for the name lookup and the TLS handshake: the second is the round trip)
        val t0 = System.nanoTime()
        val r = post(server, "/v1/hello", body)
        val ms = (System.nanoTime() - t0) / 1_000_000
        Hello(r.optBoolean("ok"), ms, r.optString("colo"), r.optString("country"))
    }

    fun host(server: String, name: String, fp: String, addrs: List<String>, opts: Opts): Room {
        val r = post(server, "/v1/host", base(name, fp, addrs).put("opts", JSONObject().put("delay", opts.delay).put("relay", opts.relay)))
        return Room(r.getString("code"), r.getString("key"))
    }

    fun join(server: String, code: String, name: String, fp: String, addrs: List<String>): Joined {
        val r = post(server, "/v1/join", base(name, fp, addrs).put("code", code))
        return Joined(r.getString("key"), peer(r.getJSONObject("host")), opts(r.optJSONObject("opts")))
    }

    /** The host's poll: the one who joined, once there. */
    fun pollGuest(server: String, room: Room): Peer? =
        post(server, "/v1/poll", JSONObject().put("code", room.code).put("key", room.key)).optJSONObject("guest")?.let(::peer)

    fun leave(server: String, room: Room) {
        runCatching { post(server, "/v1/leave", JSONObject().put("code", room.code).put("key", room.key)) }
    }

    fun relayUrl(server: String, code: String, key: String): String =
        "$server/v1/ws?code=${URLEncoder.encode(code, "UTF-8")}&key=${URLEncoder.encode(key, "UTF-8")}"

    private fun base(name: String, fp: String, addrs: List<String>) =
        JSONObject().put("v", NET_VERSION).put("game", GAME).put("fp", fp).put("name", cleanName(name)).put("addrs", JSONArray(addrs.take(8)))

    private fun peer(o: JSONObject): Peer {
        val a = o.optJSONArray("addrs") ?: JSONArray()
        return Peer(o.optString("name"), (0 until a.length()).map { a.getString(it) }, o.optString("seen"))
    }

    private fun opts(o: JSONObject?) = Opts(o?.optInt("delay", 0)?.coerceIn(0, 4) ?: 0, o?.optBoolean("relay", true) ?: true)

    private fun post(server: String, path: String, body: JSONObject): JSONObject {
        if (!server.startsWith("https://") || isPlaceholder(server)) throw Failure("server")
        val c = URL(server + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 6000
            c.readTimeout = 10000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("User-Agent", "HiddenLeaf-Android")
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            if (c.responseCode == 429) throw Failure("limit") // (the free plan's limit; Cloudflare's own page is no JSON)
            val stream = if (c.responseCode < 400) c.inputStream else (c.errorStream ?: throw Failure("network"))
            val json = JSONObject(stream.bufferedReader().use { it.readText() })
            json.optString("error").takeIf { it.isNotEmpty() }?.let { throw Failure(it) }
            return json
        } catch (e: Failure) {
            throw e
        } catch (e: Exception) {
            throw Failure("network")
        } finally {
            c.disconnect()
        }
    }

    /** What the service said, as a text of this screen. */
    fun explain(why: String): Int = when (why) {
        "version" -> R.string.online_err_version
        "game" -> R.string.online_err_version
        "different" -> R.string.online_err_different
        "no room" -> R.string.online_err_no_room
        "full" -> R.string.online_err_full
        "busy", "limit" -> R.string.online_err_busy
        "server" -> R.string.online_err_server
        "network" -> R.string.online_err_network
        else -> R.string.online_err_other
    }

    // ---- this phone's addresses

    /**
     * The address the router shows to the internet for [socket] (a STUN Binding request to two public servers, no account), as "ip:port".
     * The other player sends there; the router lets it in once this socket has sent something out (the engine's hellos do that).
     */
    fun stun(socket: DatagramSocket, timeoutMs: Long = 1500): List<String> {
        val servers = listOf("stun.l.google.com" to 19302, "stun.cloudflare.com" to 3478).mapNotNull { (host, port) ->
            runCatching { InetSocketAddress(InetAddress.getAllByName(host).first { it is Inet4Address }, port) }.getOrNull()
        }
        if (servers.isEmpty()) return emptyList()
        val random = SecureRandom()
        val asked = mutableListOf<ByteArray>()
        val found = LinkedHashSet<String>()
        val old = socket.soTimeout
        val end = System.currentTimeMillis() + timeoutMs
        var lastAsk = 0L
        var rounds = 0
        try {
            while (System.currentTimeMillis() < end && found.size < servers.size) {
                if (rounds < 3 && System.currentTimeMillis() - lastAsk >= 400) {
                    lastAsk = System.currentTimeMillis()
                    rounds++
                    for (s in servers) {
                        val m = ByteArray(20)
                        m[1] = 1 // Binding request, no attributes
                        COOKIE.copyInto(m, 4)
                        val tx = ByteArray(12).also { random.nextBytes(it) }
                        tx.copyInto(m, 8)
                        asked += tx
                        runCatching { socket.send(DatagramPacket(m, m.size, s)) }
                    }
                }
                socket.soTimeout = 150
                val buf = ByteArray(512)
                val p = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(p)
                } catch (e: SocketTimeoutException) {
                    continue
                }
                parseStun(buf, p.length, asked)?.let { found += it }
            }
        } catch (e: Exception) {
            // (no network: the local addresses are all there is)
        } finally {
            runCatching { socket.soTimeout = old }
        }
        return found.toList()
    }

    private val COOKIE = byteArrayOf(0x21, 0x12, 0xA4.toByte(), 0x42)

    private fun parseStun(p: ByteArray, n: Int, asked: List<ByteArray>): String? {
        if (n < 20 || p[0].toInt() != 1 || p[1].toInt() != 1) return null // Binding success
        if ((0 until 4).any { p[4 + it] != COOKIE[it] }) return null
        if (asked.none { tx -> (0 until 12).all { p[8 + it] == tx[it] } }) return null
        var at = 20
        while (at + 4 <= n) {
            val type = (p[at].toInt() and 0xFF) shl 8 or (p[at + 1].toInt() and 0xFF)
            val len = (p[at + 2].toInt() and 0xFF) shl 8 or (p[at + 3].toInt() and 0xFF)
            if (at + 4 + len > n) break
            if ((type == 0x0020 || type == 0x0001) && len >= 8 && p[at + 5].toInt() == 1) {
                val x = type == 0x0020
                val v = at + 4
                val port = ((p[v + 2].toInt() and 0xFF) shl 8 or (p[v + 3].toInt() and 0xFF)) xor (if (x) 0x2112 else 0)
                val ip = (0 until 4).map { (p[v + 4 + it].toInt() xor (if (x) COOKIE[it].toInt() else 0)) and 0xFF }
                return ip.joinToString(".") + ":" + port
            }
            at += 4 + ((len + 3) and 3.inv())
        }
        return null
    }

    /** This phone's own addresses with [port] (Wi-Fi and cable first), for a player on the same network. */
    fun localAddresses(port: Int): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            .sortedBy { i -> if (listOf("wlan", "swlan", "eth", "ap").any { i.name.startsWith(it) }) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
            .filter { !it.startsWith("169.254.") && !it.startsWith("192.0.0.") && !it.startsWith("0.") }
            .map { "$it:$port" }.take(3)
    }.getOrDefault(emptyList())

    /** The other player's addresses for the engine: what they reported, then the address the service saw them under with each of their ports. */
    fun candidates(peer: Peer): List<String> {
        val out = LinkedHashSet(peer.addrs.filter { Regex("""[0-9.]{7,15}:\d{1,5}""").matches(it) })
        if (Regex("""[0-9.]{7,15}""").matches(peer.seen)) {
            for (a in peer.addrs) out += peer.seen + ":" + a.substringAfterLast(':')
        }
        return out.take(8)
    }

    // ---- the session file (read by the engine, hl_online.cpp, and by GameActivity, OnlineGame)

    fun sessionFile(paths: GamePaths) = File(paths.root, "online/session.txt")

    data class Session(
        val host: Boolean, val port: Int, val code: String, val me: String, val peer: String,
        val addrs: List<String>, val delay: Int, val relay: Boolean, val relayUrl: String,
    )

    fun writeSession(paths: GamePaths, s: Session) {
        val f = sessionFile(paths)
        f.parentFile?.mkdirs()
        val text = buildString {
            append("v=$NET_VERSION\n")
            append("role=${if (s.host) "host" else "guest"}\n")
            append("at=${System.currentTimeMillis() / 1000}\n")
            append("port=${s.port}\n")
            append("code=${s.code}\n")
            append("me=${cleanName(s.me)}\n")
            append("peer=${cleanName(s.peer)}\n")
            append("delay=${s.delay.coerceIn(0, 4)}\n")
            append("relay=${if (s.relay) 1 else 0}\n")
            if (s.relay) append("relay_url=${s.relayUrl}\n")
            s.addrs.forEach { append("addr=$it\n") }
        }
        // (written aside and renamed: the engine must never read half of it)
        val aside = File(f.parentFile, "session.tmp")
        aside.writeText(text)
        if (!aside.renameTo(f)) {
            f.delete()
            aside.renameTo(f)
        }
    }

    /** The session file's lines as key -> values; empty when it is missing or older than the engine takes. */
    fun readSession(paths: GamePaths): Map<String, List<String>> {
        val f = sessionFile(paths)
        val lines = runCatching { f.readLines() }.getOrDefault(emptyList())
        val map = lines.mapNotNull { l -> l.indexOf('=').takeIf { it > 0 }?.let { l.substring(0, it) to l.substring(it + 1) } }
            .groupBy({ it.first }, { it.second })
        val at = map["at"]?.firstOrNull()?.toLongOrNull() ?: return emptyMap()
        val age = System.currentTimeMillis() / 1000 - at
        return if (age in -60..SESSION_MAX_AGE_S) map else emptyMap()
    }
}

/** The Online screen's choices, kept in the launcher's preferences. */
class OnlinePrefs(context: Context) {
    private val p = context.getSharedPreferences("online", Context.MODE_PRIVATE)

    /** [lan]: the page's choice between the internet (rooms service) and the local network (data/Lan.kt). */
    data class State(val server: String, val name: String, val delay: Int, val relay: Boolean, val lan: Boolean = false)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<State> = _state.asStateFlow()

    private fun read() = State(
        p.getString("server", null) ?: Online.DEFAULT_SERVER,
        p.getString("name", null) ?: "Ninja${(10..99).random()}",
        p.getInt("delay", 0).coerceIn(0, 4),
        p.getBoolean("relay", true),
        p.getBoolean("lan", false),
    )

    fun set(s: State) {
        _state.value = s
        p.edit().putString("server", s.server).putString("name", s.name).putInt("delay", s.delay).putBoolean("relay", s.relay).putBoolean("lan", s.lan).apply()
    }
}

/** Where the lobby is. Texts are this screen's string resources. */
sealed interface LobbyState {
    data object Idle : LobbyState
    data class Working(val step: Int) : LobbyState
    /** The room is open: its code, while nobody has joined. */
    data class Waiting(val code: String) : LobbyState
    /** The other player is there: the game is starting. */
    data class Starting(val peer: String, val code: String) : LobbyState
    data class Failed(val message: Int) : LobbyState
    /** A local match is open (data/Lan.kt): this phone's LAN addresses, for the other player to check they are on the same network. */
    data class LanHosting(val ips: List<String>) : LobbyState
    /** Listening for local matches: what was found is in OnlineLobby.lanHosts. */
    data object LanSearching : LobbyState
}

sealed interface ServerTest {
    data object Idle : ServerTest
    data object Running : ServerTest
    data class Done(val hello: Online.Hello) : ServerTest
    data class Failed(val message: Int) : ServerTest
}

/**
 * The lobby: creating or joining a room, and starting the match. A room's UDP socket is opened first, so the addresses given to the
 * service are that socket's (its port goes to the engine, which opens the same port again); it stays open while the room waits, and a
 * STUN request every 20 s keeps the router's mapping alive. A local network match (data/Lan.kt) opens the same kind of socket and
 * meets on it without the service; both end in launchMatch.
 */
class OnlineLobby(app: Application) : AndroidViewModel(app) {
    val prefs = OnlinePrefs(app)
    private val paths = GamePaths(app)

    private val _state = MutableStateFlow<LobbyState>(LobbyState.Idle)
    val state: StateFlow<LobbyState> = _state.asStateFlow()
    private val _test = MutableStateFlow<ServerTest>(ServerTest.Idle)
    val test: StateFlow<ServerTest> = _test.asStateFlow()

    private var job: Job? = null
    private var socket: DatagramSocket? = null
    private var room: Online.Room? = null
    private var roomServer = ""

    fun update(s: OnlinePrefs.State) {
        prefs.set(s)
        if (_test.value !is ServerTest.Running) _test.value = ServerTest.Idle
    }

    fun testServer() {
        val server = Online.cleanServer(prefs.state.value.server)
        _test.value = ServerTest.Running
        viewModelScope.launch {
            _test.value = try {
                val h = Online.hello(server)
                if (h.ok) ServerTest.Done(h) else ServerTest.Failed(R.string.online_err_version)
            } catch (e: Online.Failure) {
                ServerTest.Failed(Online.explain(e.why))
            }
        }
    }

    /** Opens the room and waits for someone; [start] runs on the main thread once the session file is written. */
    fun create(fp: String, start: () -> Unit) {
        if (job?.isActive == true) return
        val p = prefs.state.value
        val server = Online.cleanServer(p.server)
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = LobbyState.Working(R.string.online_step_address)
                val s = openSocket()
                val addrs = (Online.stun(s) + Online.localAddresses(s.localPort)).distinct()
                ActivityLog.info("online", "room: own addresses ${addrs.joinToString()}")
                _state.value = LobbyState.Working(R.string.online_step_room)
                val r = Online.host(server, p.name, fp, addrs, Online.Opts(p.delay, p.relay))
                room = r
                roomServer = server
                _state.value = LobbyState.Waiting(r.code)
                ActivityLog.info("online", "room ${r.code} open")
                var every = 2000L
                var lastStun = System.currentTimeMillis()
                while (isActive) {
                    delay(every)
                    val guest = try {
                        Online.pollGuest(server, r).also { every = 2000L }
                    } catch (e: Online.Failure) {
                        if (e.why == "limit" || e.why == "network") { every = (every * 2).coerceAtMost(30_000L); null } else throw e
                    }
                    if (guest != null) {
                        ActivityLog.info("online", "room ${r.code}: ${guest.name} joined from ${guest.seen}, addresses ${guest.addrs.joinToString()}")
                        launchMatch(Online.Session(true, s.localPort, r.code, p.name, guest.name, Online.candidates(guest), p.delay, p.relay, Online.relayUrl(server, r.code, r.key)), start)
                        room = null // (the room stays on the service for the relay's connection; it goes by itself)
                        return@launch
                    }
                    if (System.currentTimeMillis() - lastStun > 20_000) {
                        lastStun = System.currentTimeMillis()
                        Online.stun(s, 600) // (keeps the router's mapping of this port alive while the room waits)
                    }
                }
            } catch (e: Online.Failure) {
                ActivityLog.info("online", "room failed: ${e.why}")
                _state.value = LobbyState.Failed(Online.explain(e.why))
                closeRoom()
            } catch (e: java.io.IOException) {
                _state.value = LobbyState.Failed(R.string.online_err_network)
                closeRoom()
            }
        }
    }

    /** Joins the room [code]; [start] runs on the main thread once the session file is written. */
    fun join(code: String, fp: String, start: () -> Unit) {
        if (job?.isActive == true) return
        val p = prefs.state.value
        val server = Online.cleanServer(p.server)
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) {
            _state.value = LobbyState.Failed(R.string.online_err_code)
            return
        }
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = LobbyState.Working(R.string.online_step_address)
                val s = openSocket()
                val addrs = (Online.stun(s) + Online.localAddresses(s.localPort)).distinct()
                _state.value = LobbyState.Working(R.string.online_step_join)
                val j = Online.join(server, digits, p.name, fp, addrs)
                ActivityLog.info("online", "joined room $digits of ${j.host.name} (${j.host.seen}), addresses ${j.host.addrs.joinToString()}")
                launchMatch(Online.Session(false, s.localPort, digits, p.name, j.host.name, Online.candidates(j.host), j.opts.delay, j.opts.relay && p.relay, Online.relayUrl(server, digits, j.key)), start)
            } catch (e: Online.Failure) {
                ActivityLog.info("online", "join failed: ${e.why}")
                _state.value = LobbyState.Failed(Online.explain(e.why))
                closeSocket()
            } catch (e: java.io.IOException) {
                _state.value = LobbyState.Failed(R.string.online_err_network)
                closeSocket()
            }
        }
    }

    // ---- the local network (data/Lan.kt): no service, the two phones find each other by broadcast

    private val _lanHosts = MutableStateFlow<List<Lan.Found>>(emptyList())
    /** The local matches heard while searching, newest name order; empty when not searching. */
    val lanHosts: StateFlow<List<Lan.Found>> = _lanHosts.asStateFlow()
    @Volatile private var scanSocket: DatagramSocket? = null

    /**
     * Opens a local match: the game socket says so by broadcast every second and takes the first guest with the same fingerprint;
     * [start] runs on the main thread once the session file is written.
     */
    fun hostLan(fp: String, start: () -> Unit) {
        if (job?.isActive == true) return
        val p = prefs.state.value
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = openSocket().apply { broadcast = true; soTimeout = 200 }
                val addrs = Online.localAddresses(s.localPort)
                if (addrs.isEmpty()) {
                    _state.value = LobbyState.Failed(R.string.lan_err_no_network)
                    closeSocket()
                    return@launch
                }
                val room = Lan.newRoom()
                val beacon = Lan.encode(Lan.Beacon(Online.NET_VERSION, Online.GAME, room, p.name, fp, s.localPort, addrs))
                _state.value = LobbyState.LanHosting(addrs.map { it.substringBeforeLast(':') })
                ActivityLog.info("online", "local match $room open on port ${s.localPort}, addresses ${addrs.joinToString()}")
                val began = System.currentTimeMillis()
                var lastBeacon = 0L
                var targets = Lan.broadcastTargets()
                var guest: Lan.Join? = null
                var guestFrom = ""
                var acceptedAt = 0L
                val buf = ByteArray(1500)
                while (isActive) {
                    val now = System.currentTimeMillis()
                    if (guest != null && now - acceptedAt >= Lan.ACCEPT_LINGER_MS) {
                        ActivityLog.info("online", "local match $room: ${guest.name} joined from $guestFrom, addresses ${guest.addrs.joinToString()}")
                        val peerAddrs = listOf(guestFrom) + guest.addrs
                        launchMatch(Lan.session(true, s.localPort, room, p.name, guest.name, peerAddrs, p.delay), start)
                        return@launch
                    }
                    if (now - began > Lan.HOST_MAX_MS) {
                        _state.value = LobbyState.Failed(R.string.lan_err_host_timeout)
                        closeSocket()
                        return@launch
                    }
                    if (guest == null && now - lastBeacon >= Lan.BEACON_EVERY_MS) {
                        if (lastBeacon != 0L && (now / 1000) % 10 == 0L) targets = Lan.broadcastTargets() // (the Wi-Fi may have changed)
                        lastBeacon = now
                        for (t in targets) runCatching { s.send(DatagramPacket(beacon, beacon.size, t, Lan.PORT)) }
                    }
                    val packet = DatagramPacket(buf, buf.size)
                    try {
                        s.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val join = Lan.decode(buf, packet.length) as? Lan.Join ?: continue
                    if (join.room != room) continue
                    val answer: Lan.Msg = when {
                        join.v != Online.NET_VERSION || join.game != Online.GAME || join.fp != fp -> Lan.Reject(room, "different", join.nonce)
                        guest != null && join.nonce != guest.nonce -> Lan.Reject(room, "full", join.nonce)
                        else -> {
                            if (guest == null) {
                                guest = join
                                guestFrom = "${packet.address.hostAddress}:${packet.port}"
                                acceptedAt = System.currentTimeMillis()
                                _state.value = LobbyState.Starting(join.name, Lan.code(room))
                            }
                            Lan.Accept(room, p.name, p.delay, join.nonce)
                        }
                    }
                    if (answer is Lan.Reject) ActivityLog.info("online", "local match $room: refused ${join.name} (${answer.why}, their fp ${join.fp})")
                    val bytes = Lan.encode(answer)
                    runCatching { s.send(DatagramPacket(bytes, bytes.size, packet.socketAddress)) }
                }
            } catch (e: java.io.IOException) {
                if (!isActive) return@launch // (cancelled: the socket was closed under the receive)
                ActivityLog.info("online", "local match failed: ${e.message}")
                _state.value = LobbyState.Failed(R.string.lan_err_network)
                closeSocket()
            }
        }
    }

    /** Listens for local matches ([lanHosts]) until one is picked, the screen is left or [Lan.SEARCH_MAX_MS] goes by. */
    fun searchLan() {
        if (job?.isActive == true) return
        _lanHosts.value = emptyList()
        job = viewModelScope.launch(Dispatchers.IO) {
            val lock = Lan.multicastLock(getApplication<Application>())
            var listen: DatagramSocket? = null
            try {
                if (Online.localAddresses(0).isEmpty()) {
                    _state.value = LobbyState.Failed(R.string.lan_err_no_network)
                    return@launch
                }
                _state.value = LobbyState.LanSearching
                listen = DatagramSocket(null).apply {
                    reuseAddress = true // (a second phone app, or this one a moment ago, may still hold it)
                    broadcast = true
                    soTimeout = 300
                    bind(InetSocketAddress(Lan.PORT))
                }
                scanSocket = listen
                val found = LinkedHashMap<String, Lan.Found>()
                val began = System.currentTimeMillis()
                val buf = ByteArray(1500)
                while (isActive) {
                    val now = System.currentTimeMillis()
                    if (now - began > Lan.SEARCH_MAX_MS) {
                        _state.value = LobbyState.Failed(R.string.lan_err_search_timeout)
                        return@launch
                    }
                    val packet = DatagramPacket(buf, buf.size)
                    val beacon = try {
                        listen.receive(packet)
                        Lan.decode(buf, packet.length) as? Lan.Beacon
                    } catch (e: SocketTimeoutException) {
                        null
                    }
                    if (beacon != null) {
                        val first = beacon.room !in found
                        found[beacon.room] = Lan.Found(beacon, packet.address.hostAddress ?: "", now)
                        if (first) ActivityLog.info("online", "local match ${beacon.room} of ${beacon.name} at ${packet.address.hostAddress}, fp ${beacon.fp}")
                    }
                    found.values.removeAll { now - it.seen > Lan.HOST_GONE_MS }
                    val list = found.values.sortedBy { it.beacon.name.lowercase() }
                    if (list != _lanHosts.value) _lanHosts.value = list
                }
            } catch (e: java.io.IOException) {
                if (!isActive) return@launch
                ActivityLog.info("online", "local search failed: ${e.message}")
                _state.value = LobbyState.Failed(R.string.lan_err_listen)
            } finally {
                runCatching { listen?.close() }
                scanSocket = null
                runCatching { lock?.release() }
                _lanHosts.value = emptyList()
            }
        }
    }

    /** Joins the local match [host] (from [lanHosts]); [start] runs on the main thread once the session file is written. */
    fun joinLan(host: Lan.Found, fp: String, start: () -> Unit) {
        val p = prefs.state.value
        val before = job
        before?.cancel()
        closeScan()
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                before?.join() // (the search's own cleanup first: its socket and its lock)
                _state.value = LobbyState.Working(R.string.lan_step_join)
                val s = openSocket().apply { soTimeout = 200 }
                val nonce = Lan.newNonce()
                val request = Lan.encode(Lan.Join(Online.NET_VERSION, Online.GAME, host.beacon.room, p.name, fp, s.localPort, Online.localAddresses(s.localPort), nonce))
                val targets = host.candidates().mapNotNull { a ->
                    runCatching { InetSocketAddress(InetAddress.getByName(a.substringBeforeLast(':')), a.substringAfterLast(':').toInt()) }.getOrNull()
                }
                val began = System.currentTimeMillis()
                var lastAsk = 0L
                val buf = ByteArray(1500)
                while (isActive) {
                    val now = System.currentTimeMillis()
                    if (now - began > Lan.JOIN_TIMEOUT_MS) {
                        ActivityLog.info("online", "local match ${host.beacon.room}: no answer from ${targets.joinToString()}")
                        _state.value = LobbyState.Failed(R.string.lan_err_no_answer)
                        closeSocket()
                        return@launch
                    }
                    if (now - lastAsk >= Lan.JOIN_EVERY_MS) {
                        lastAsk = now
                        for (t in targets) runCatching { s.send(DatagramPacket(request, request.size, t)) }
                    }
                    val packet = DatagramPacket(buf, buf.size)
                    try {
                        s.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    when (val m = Lan.decode(buf, packet.length)) {
                        is Lan.Accept -> if (m.room == host.beacon.room && m.nonce == nonce) {
                            val from = "${packet.address.hostAddress}:${packet.port}"
                            ActivityLog.info("online", "joined local match ${m.room} of ${m.name} at $from")
                            launchMatch(Lan.session(false, s.localPort, m.room, p.name, m.name.ifEmpty { host.beacon.name }, listOf(from) + host.candidates(), m.delay), start)
                            return@launch
                        }
                        is Lan.Reject -> if (m.room == host.beacon.room && m.nonce == nonce) {
                            ActivityLog.info("online", "local match ${m.room} refused: ${m.why}")
                            _state.value = LobbyState.Failed(Lan.explain(m.why))
                            closeSocket()
                            return@launch
                        }
                        else -> {}
                    }
                }
            } catch (e: java.io.IOException) {
                if (!isActive) return@launch
                ActivityLog.info("online", "local join failed: ${e.message}")
                _state.value = LobbyState.Failed(R.string.lan_err_network)
                closeSocket()
            }
        }
    }

    /** The Online page was left: a local match or search stops (nobody would see a guest arrive), a match already starting goes on. */
    fun leaveScreen() {
        val s = _state.value
        if (s is LobbyState.LanHosting || s is LobbyState.LanSearching || (s is LobbyState.Working && s.step == R.string.lan_step_join)) cancel()
    }

    private fun closeScan() {
        runCatching { scanSocket?.close() }
        scanSocket = null
    }

    /** The match is agreed (a room on the service, or a local one): the session file for the engine, then the game, as [start] does. */
    private suspend fun launchMatch(session: Online.Session, start: () -> Unit) {
        closeSocket() // (the engine opens this port again, session.port: the other side's router knows it)
        Online.writeSession(paths, session)
        _state.value = LobbyState.Starting(session.peer, session.code)
        withContext(Dispatchers.Main) { start() }
        delay(4000)
        _state.value = LobbyState.Idle
    }

    fun cancel() {
        job?.cancel()
        job = null
        closeScan()
        closeRoom()
        _state.value = LobbyState.Idle
    }

    fun dismiss() {
        if (_state.value is LobbyState.Failed) _state.value = LobbyState.Idle
    }

    private fun openSocket(): DatagramSocket {
        closeSocket()
        return DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(0))
        }.also { socket = it }
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun closeRoom() {
        closeSocket()
        val r = room ?: return
        room = null
        val server = roomServer
        viewModelScope.launch(Dispatchers.IO + NonCancellable) { Online.leave(server, r) }
    }

    override fun onCleared() {
        job?.cancel()
        closeSocket()
        closeScan()
        room?.let { r -> Thread { Online.leave(roomServer, r) }.start() }
    }
}
