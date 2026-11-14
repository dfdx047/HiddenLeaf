package com.dfdx047.hiddenleaf.engine

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
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
    private var root: ViewGroup? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, activity.resources.displayMetrics)
    private fun font(id: Int, fallback: Typeface) = runCatching { ResourcesCompat.getFont(activity, id) }.getOrNull() ?: fallback

    /**
     * The end of the match, in the launcher's colours (the Konoha palette: Naruto's orange on the night's black, the brush title
     * font): the developer found Android's plain white dialog out of place over the game. A dark veil holds the game behind it
     * (the engine already holds both pads still); one button goes back to the menu.
     */
    private fun showEnd(message: String) {
        val layout = root ?: return
        val r = activity.resources
        val veil = FrameLayout(activity).apply {
            setBackgroundColor(0xB3000000.toInt())
            isClickable = true // (the touch pad behind does not get the taps)
        }
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF2A2420.toInt(), 0xFF1B1C24.toInt())).apply {
                cornerRadius = dp(18f)
                setStroke(dp(3f).toInt(), ORANGE)
            }
            setPadding(dp(28f).toInt(), dp(18f).toInt(), dp(28f).toInt(), dp(20f).toInt())
            elevation = dp(12f)
        }
        // the orange band with the title, as the launcher's headings
        card.addView(TextView(activity).apply {
            text = r.getString(R.string.online_game_end_title)
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = font(R.font.shojumaru_regular, Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ORANGE_DEEP, ORANGE, ORANGE_DEEP)).apply { cornerRadius = dp(10f) }
            setPadding(dp(22f).toInt(), dp(6f).toInt(), dp(22f).toInt(), dp(8f).toInt())
            setShadowLayer(dp(2f), 0f, dp(1.5f), 0xAA000000.toInt())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        card.addView(TextView(activity).apply {
            text = message
            setTextColor(CREAM)
            textSize = 16f
            typeface = font(R.font.kanit_medium, Typeface.DEFAULT)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f).toInt() })
        card.addView(TextView(activity).apply {
            text = r.getString(R.string.online_game_back)
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = font(R.font.kanit_bold, Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(ORANGE, ORANGE_DEEP)).apply {
                cornerRadius = dp(26f)
                setStroke(dp(2f).toInt(), 0xFFFFC08A.toInt())
            }
            setPadding(dp(30f).toInt(), dp(10f).toInt(), dp(30f).toInt(), dp(12f).toInt())
            isFocusable = true
            setOnClickListener { isEnabled = false; (activity as? GameActivity)?.quitToApp() }
            post { requestFocus() } // (a controller's ✕/A presses it too)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(20f).toInt() })
        veil.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        layout.addView(veil, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        text?.visibility = View.GONE
    }

    /** After the engine is loaded (SDLActivity.onCreate): the line on screen and the relay's threads. */
    fun attach(layout: ViewGroup) {
        // The Wi-Fi held awake for the match (2026-11-14): a Honor host heard nothing from the other phone for 2 s at a time, every
        // few seconds, while its own packets kind of went out; its Wi-Fi saves power on what comes in (a ping to it idle: 746 ms).
        // The low-latency lock turns that saving off while the game is in front.
        runCatching {
            val wm = activity.applicationContext.getSystemService(WifiManager::class.java)
            // (where the chip has no low-latency mode Android holds it at high performance instead)
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "hiddenleaf:online").apply { setReferenceCounted(false); acquire() }
            Log.i("un5", "online: Wi-Fi held awake")
        }.onFailure { Log.w("un5", "online: no Wi-Fi lock: ${it.message}") }
        val t = TextView(activity).apply {
            setTextColor(CREAM)
            background = GradientDrawable().apply {
                setColor(0xD91B1C24.toInt())
                cornerRadius = dp(10f)
                setStroke(dp(1.5f).toInt(), ORANGE)
            }
            textSize = 12f
            typeface = font(R.font.kanit_semibold, Typeface.DEFAULT_BOLD)
            setPadding(dp(10f).toInt(), dp(3f).toInt(), dp(10f).toInt(), dp(3f).toInt())
        }
        layout.addView(t, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(6f).toInt(); topMargin = dp(4f).toInt()
        })
        root = layout
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
    private var endShown = false

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
        t.visibility = if (line.isEmpty() || endShown) View.GONE else View.VISIBLE
        // (the developer, 2026-11-14: after a drop each phone went on alone. The engine now holds the game still once the match is
        // over; here the player is told why and goes back to the menu)
        if (phase == "over" || phase == "failed") runCatching { wifiLock?.release() }
        if ((phase == "over" || phase == "failed") && !endShown && s["reason"] != "the player left") {
            endShown = true
            showEnd(line)
        }
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
        // (ui/theme/Color.kt's Konoha.NarutoOrange, NarutoOrangeDeep and the launcher's text colour)
        private const val ORANGE = 0xFFFF7A1A.toInt()
        private const val ORANGE_DEEP = 0xFFE2560B.toInt()
        private const val CREAM = 0xFFEDE6DE.toInt()

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
