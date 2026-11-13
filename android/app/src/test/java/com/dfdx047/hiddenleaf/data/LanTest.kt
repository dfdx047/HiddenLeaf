package com.dfdx047.hiddenleaf.data

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The local network's messages (data/Lan.kt): plain JVM, no Android. */
class LanCodecTest {
    private val beacon = Lan.Beacon(Online.NET_VERSION, Online.GAME, "A1B2C3", "Naruto", "a12.un5.none", 40123, listOf("192.168.1.5:40123", "10.0.0.7:40123"))

    @Test
    fun beaconRoundTrip() {
        val bytes = Lan.encode(beacon)
        assertTrue(String(bytes, Charsets.US_ASCII).startsWith("HLLAN\n"))
        assertTrue(bytes.size < 512)
        assertEquals(beacon, Lan.decode(bytes))
        // (a packet in a larger buffer, as DatagramSocket.receive leaves it)
        val buf = ByteArray(1500).also { bytes.copyInto(it) }
        assertEquals(beacon, Lan.decode(buf, bytes.size))
    }

    @Test
    fun joinAcceptRejectRoundTrip() {
        val join = Lan.Join(1, "un5", "00FF10", "Sasuke", "a12.un5.un6-a35", 5000, listOf("192.168.1.9:5000"), "0123456789abcdef")
        assertEquals(join, Lan.decode(Lan.encode(join)))
        val accept = Lan.Accept("00FF10", "Naruto", 3, "0123456789abcdef")
        assertEquals(accept, Lan.decode(Lan.encode(accept)))
        val reject = Lan.Reject("00FF10", "full", "0123456789abcdef")
        assertEquals(reject, Lan.decode(Lan.encode(reject)))
    }

    @Test
    fun cleansWhatGoesOut() {
        val b = Lan.decode(Lan.encode(beacon.copy(name = "Na\nru\"to-with-a-very-long-name", fp = "a12 un5/x")))!! as Lan.Beacon
        assertEquals("Naruto-with-a-ve", b.name) // (no line breaks or quotes, 16 letters, as the rooms service takes it)
        assertEquals("a12_un5_x", b.fp)
        assertEquals(4, Lan.decode(Lan.encode(beacon.copy(addrs = List(6) { "10.0.0.$it:1" })))!!.let { (it as Lan.Beacon).addrs.size })
    }

    @Test
    fun refusesForeignPackets() {
        assertNull(Lan.decode(ByteArray(0)))
        assertNull(Lan.decode("hello".toByteArray()))
        assertNull(Lan.decode("HLLAN\nt=beacon\nroom=A1B2C3\n".toByteArray())) // (no port)
        assertNull(Lan.decode(String(Lan.encode(beacon)).replace("room=A1B2C3", "room=zz").toByteArray()))
        assertNull(Lan.decode(String(Lan.encode(beacon)).replace("port=40123", "port=70000").toByteArray()))
        assertNull(Lan.decode("HLLAN\nt=accept\nroom=A1B2C3\n".toByteArray())) // (no nonce)
        assertNull(Lan.decode("HLLAN\nt=other\nroom=A1B2C3\nnonce=1\n".toByteArray()))
        assertNull(Lan.decode(ByteArray(2000) { 'H'.code.toByte() }))
        // a bad address is dropped, the rest stays
        val b = Lan.decode(String(Lan.encode(beacon)).replace("addr=10.0.0.7:40123", "addr=evil").toByteArray()) as Lan.Beacon
        assertEquals(listOf("192.168.1.5:40123"), b.addrs)
    }

    @Test
    fun foundCandidatesAndCompatibility() {
        val f = Lan.Found(beacon, "192.168.1.5", 0)
        assertEquals(listOf("192.168.1.5:40123", "10.0.0.7:40123"), f.candidates())
        assertEquals("172.16.0.2:40123", Lan.Found(beacon, "172.16.0.2", 0).candidates().first())
        assertTrue(f.compatible("a12.un5.none"))
        assertFalse(f.compatible("a12.un5.un6-a35"))
        assertFalse(Lan.Found(beacon.copy(v = Online.NET_VERSION + 1), "192.168.1.5", 0).compatible("a12.un5.none"))
    }

    @Test
    fun roomsAndNonces() {
        repeat(50) {
            val r = Lan.newRoom()
            assertTrue(r, Regex("[0-9A-F]{6}").matches(r))
            val n = Lan.newNonce()
            assertTrue(n, Regex("[0-9a-f]{16}").matches(n))
        }
        assertEquals("LANA1B2C3", Lan.code("A1B2C3"))
    }

    /** A join and its accept through two real sockets on the loopback, as the two phones' game sockets would. */
    @Test
    fun joinOverLoopback() {
        DatagramSocket(InetSocketAddress(InetAddress.getLoopbackAddress(), 0)).use { host ->
            DatagramSocket(InetSocketAddress(InetAddress.getLoopbackAddress(), 0)).use { guest ->
                host.soTimeout = 2000
                guest.soTimeout = 2000
                val join = Lan.encode(Lan.Join(1, "un5", "A1B2C3", "Sasuke", "fp", guest.localPort, emptyList(), "abc"))
                guest.send(DatagramPacket(join, join.size, host.localSocketAddress))
                val buf = ByteArray(1500)
                val p = DatagramPacket(buf, buf.size)
                host.receive(p)
                val got = Lan.decode(buf, p.length) as Lan.Join
                assertEquals(guest.localPort, p.port)
                val accept = Lan.encode(Lan.Accept(got.room, "Naruto", 2, got.nonce))
                host.send(DatagramPacket(accept, accept.size, p.socketAddress))
                val q = DatagramPacket(buf, buf.size)
                guest.receive(q)
                assertEquals(Lan.Accept("A1B2C3", "Naruto", 2, "abc"), Lan.decode(buf, q.length))
            }
        }
    }
}

/** The session file a local match writes for the engine (Robolectric: GamePaths needs a context). */
@RunWith(RobolectricTestRunner::class)
class LanSessionTest {
    @Test
    fun sessionForLan() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val paths = GamePaths(app)
        val s = Lan.session(false, 40500, "A1B2C3", "Sasuke", "Naruto", listOf("192.168.1.5:40123", "bad", "192.168.1.5:40123", "10.0.0.7:40123"), 2)
        assertFalse(s.relay)
        assertEquals("LANA1B2C3", s.code)
        Online.writeSession(paths, s)
        val m = Online.readSession(paths)
        assertEquals(Online.NET_VERSION.toString(), m["v"]?.single())
        assertEquals("guest", m["role"]?.single())
        assertEquals("40500", m["port"]?.single())
        assertEquals("LANA1B2C3", m["code"]?.single())
        assertEquals("Naruto", m["peer"]?.single())
        assertEquals("2", m["delay"]?.single())
        assertEquals("0", m["relay"]?.single())
        assertNull(m["relay_url"])
        assertEquals(listOf("192.168.1.5:40123", "10.0.0.7:40123"), m["addr"])
        // (the host writes the same code, so the engine's session hash is the same on both phones)
        assertEquals(s.code, Lan.session(true, 40123, "A1B2C3", "Naruto", "Sasuke", listOf("192.168.1.9:40500"), 2).code)
    }
}
