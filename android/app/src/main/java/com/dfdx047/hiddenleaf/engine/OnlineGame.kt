package com.dfdx047.hiddenleaf.engine

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.Online

/**
 * GameActivity's side of an online match (the app's Online screen started it with a session file; the engine's side is
 * recomp/android/host/online/hl_online.cpp): a line on screen saying how the match goes, and the room's relay, a WebSocket the
 * engine's packets go through when the two phones cannot reach each other directly.
 */
class OnlineGame private constructor(private val activity: Activity, private val peer: String, private val relayUrl: String?) {
    @Volatile private var running = true
    @Volatile private var relay: RelaySocket? = null
    private var text: TextView? = null

    /** After the engine is loaded (SDLActivity.onCreate): the line on screen and the relay's threads. */
    fun attach(layout: ViewGroup) {
        val t = TextView(activity).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(14, 6, 14, 6)
        }
        layout.addView(t, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
        text = t
        t.post(object : Runnable {
            override fun run() {
                if (!running) return
                show(t, OnlineBridge.parse(OnlineBridge.status()))
                t.postDelayed(this, 500)
            }
        })
        if (relayUrl != null) startRelay(relayUrl)
    }

    private var shownSince = 0L
    private var lastPhase = ""

    private fun show(t: TextView, s: Map<String, String>) {
        val phase = s["phase"] ?: "starting"
        if (phase != lastPhase) {
            lastPhase = phase
            shownSince = System.currentTimeMillis()
        }
        val r = activity.resources
        val line = when (phase) {
            "starting", "linking" -> r.getString(R.string.online_game_linking, peer)
            "playing" -> {
                val rtt = s["rtt"]?.toIntOrNull() ?: -1
                val side = if (s["p1"] == "1") "P1" else "P2"
                val via = if (s["relay"] == "1") r.getString(R.string.online_game_via_relay) else ""
                val desync = if ((s["desyncs"]?.toIntOrNull() ?: 0) > 0) r.getString(R.string.online_game_desync) else ""
                // (once it is going the line keeps to a corner and says little: the ping and the side)
                r.getString(R.string.online_game_playing, side, peer, if (rtt >= 0) "$rtt ms" else "?",
                    // (the delay is in vsyncs of 20 ms)
                    s["delay"]?.toIntOrNull()?.let { "${it * 20} ms" } ?: "?") + via + desync
            }
            // (the engine's reasons, match_session.cpp)
            "over" -> when (s["reason"]) {
                "the other player left" -> r.getString(R.string.online_game_peer_left, peer)
                "the player left" -> r.getString(R.string.online_game_over)
                "the other game did not start in time" -> r.getString(R.string.online_game_no_meet, peer)
                "the two games are no longer the same" -> r.getString(R.string.online_game_desync_end)
                else -> r.getString(R.string.online_game_lost, peer)
            }
            "failed" -> r.getString(R.string.online_game_failed)
            else -> ""
        }
        t.text = line
        t.visibility = if (line.isEmpty()) View.GONE else View.VISIBLE
        // the line fades when the match is going well, so it is not in the way
        t.alpha = if (phase == "playing" && System.currentTimeMillis() - shownSince > 6000 && (s["desyncs"]?.toIntOrNull() ?: 0) == 0) 0.55f else 1f
    }

    private fun startRelay(url: String) {
        Thread({
            var tries = 0
            while (running && tries < 6) {
                val s = RelaySocket.connect(url) { data -> runCatching { OnlineBridge.nativeRelayIn(data) } }
                if (s == null) {
                    tries++
                    Thread.sleep(1000L * tries)
                    continue
                }
                tries = 0
                relay = s
                Log.i("un5", "online relay: connected")
                // the engine's packets for the relay, until the match is over or the socket closes
                while (running && s.open) {
                    val out = try { OnlineBridge.nativeRelayOut(250) } catch (e: UnsatisfiedLinkError) { running = false; null } ?: continue
                    s.send(out)
                }
                s.close()
                relay = null
                val phase = OnlineBridge.parse(OnlineBridge.status())["phase"]
                if (phase == "over" || phase == "failed") break
            }
        }, "online-relay-pump").apply { isDaemon = true; start() }
    }

    /** The player quits the game (the panel's quit): the other side is told. */
    fun leave() {
        OnlineBridge.leave()
        // (the engine sends its goodbye at its next vsync; the relay closes a moment later)
        text?.postDelayed({ running = false; relay?.close() }, 300)
    }

    companion object {
        /** Before the engine starts (GameActivity.onCreate, before super): an online match when the Online screen just wrote its file. */
        fun from(activity: Activity, paths: GamePaths): OnlineGame? {
            val s = Online.readSession(paths)
            if (s.isEmpty() || s["v"]?.firstOrNull()?.toIntOrNull() != Online.NET_VERSION) return null
            val relay = s["relay_url"]?.firstOrNull()?.takeIf { s["relay"]?.firstOrNull() == "1" && it.startsWith("https://") }
            val peer = s["peer"]?.firstOrNull().orEmpty().ifEmpty { "?" }
            Log.i("un5", "online: match against $peer, relay ${if (relay != null) "on" else "off"}")
            return OnlineGame(activity, peer, relay)
        }
    }
}
