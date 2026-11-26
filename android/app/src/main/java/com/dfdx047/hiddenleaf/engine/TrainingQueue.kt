package com.dfdx047.hiddenleaf.engine

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.Online
import java.io.File
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.security.SecureRandom

/**
 * Training Mode while queuing: writes <root>/online/training.txt so the engine fast-forwards boot/logos/menus
 * straight to Free Battle Character Select under a smooth loading screen, while a background thread polls /v1/quick.
 * When an opponent is found, a Konoha banner slides in at the top to accept and jump straight into the online match.
 */
object TrainingQueue {
    data class Request(
        val server: String,
        val name: String,
        val fp: String,
        val inputMode: String,
        val delay: Int,
        val relay: Boolean,
    )

    @Volatile var pending: Request? = null
        private set

    fun arm(paths: GamePaths, req: Request) {
        pending = req
        runCatching {
            val dir = File(paths.root, "online").apply { mkdirs() }
            File(dir, "training.txt").writeText("1\n")
            val json = org.json.JSONObject()
                .put("server", req.server)
                .put("name", req.name)
                .put("fp", req.fp)
                .put("inputMode", req.inputMode)
                .put("delay", req.delay)
                .put("relay", req.relay)
                .put("at", System.currentTimeMillis() / 1000)
            File(dir, "training_ui.json").writeText(json.toString())
        }
    }

    /** Called in GameActivity's :game process to pick up the armed training queue request across processes. */
    fun consume(paths: GamePaths): Request? {
        pending?.let { pending = null; return it }
        val f = File(paths.root, "online/training_ui.json")
        if (!f.exists()) return null
        val text = runCatching { f.readText() }.getOrNull()
        runCatching { f.delete() }
        if (text.isNullOrBlank()) return null
        return runCatching {
            val o = org.json.JSONObject(text)
            val age = System.currentTimeMillis() / 1000 - o.optLong("at", 0L)
            if (age !in -60..180) return null
            Request(
                server = o.getString("server"),
                name = o.getString("name"),
                fp = o.getString("fp"),
                inputMode = o.optString("inputMode", "any"),
                delay = o.optInt("delay", 0),
                relay = o.optBoolean("relay", true),
            )
        }.getOrNull()
    }

    fun clear() { pending = null }
}

class TrainingQueueOverlay(private val activity: Activity, private val paths: GamePaths, private val req: TrainingQueue.Request) {
    @Volatile private var running = true
    private var root: ViewGroup? = null
    private var badge: TextView? = null
    private var banner: View? = null
    private var cover: View? = null
    private var coverBar: View? = null
    private var coverTrack: FrameLayout? = null
    private var socket: DatagramSocket? = null
    private var room: Online.Room? = null
    private val who = ByteArray(12).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, activity.resources.displayMetrics)
    private fun font(id: Int, fallback: Typeface) = runCatching { ResourcesCompat.getFont(activity, id) }.getOrNull() ?: fallback

    fun attach(layout: ViewGroup) {
        root = layout
        val b = TextView(activity).apply {
            text = activity.getString(R.string.online_training_searching)
            setTextColor(0xFFEDE6DE.toInt())
            textSize = 12f
            typeface = font(R.font.kanit_semibold, Typeface.DEFAULT_BOLD)
            background = GradientDrawable().apply {
                setColor(0xD91B1C24.toInt())
                cornerRadius = dp(10f)
                setStroke(dp(1.5f).toInt(), 0xFFFF7A1A.toInt())
            }
            setPadding(dp(10f).toInt(), dp(4f).toInt(), dp(10f).toInt(), dp(4f).toInt())
        }
        layout.addView(b, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(8f).toInt(); topMargin = dp(6f).toInt()
        })
        badge = b
        addLoadingCover(layout)
        b.post(object : Runnable {
            override fun run() {
                if (!running) return
                val st = OnlineBridge.parse(OnlineBridge.status())
                if (st["phase"] == "training") {
                    updateCover(st["field"]?.toIntOrNull() ?: 0)
                    if (cover != null) b.postDelayed(this, 200)
                } else {
                    dismissCover()
                }
            }
        })
        startWorker()
    }

    private fun addLoadingCover(layout: ViewGroup) {
        val c = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF2A160A.toInt(), 0xFF120A05.toInt(), 0xFF2A160A.toInt()))
            isClickable = true
        }
        c.addView(TextView(activity).apply {
            gravity = Gravity.CENTER
            text = "Modo Treino"
            typeface = font(R.font.shojumaru_regular, Typeface.DEFAULT_BOLD)
            textSize = 32f
            setTextColor(0xFFFFE6A0.toInt())
            setShadowLayer(4f, 3f, 3f, 0xFF000000.toInt())
        })
        c.addView(TextView(activity).apply {
            gravity = Gravity.CENTER
            text = activity.getString(R.string.online_training_searching)
            typeface = font(R.font.kanit_semibold, Typeface.DEFAULT_BOLD)
            textSize = 15f
            setTextColor(0xFFEDE6DE.toInt())
            setPadding(0, dp(4f).toInt(), 0, dp(14f).toInt())
        })
        val track = FrameLayout(activity).apply {
            background = GradientDrawable().apply { setColor(0xFF4A2A12.toInt()); cornerRadius = dp(6f); setStroke(dp(2f).toInt(), 0xFF000000.toInt()) }
        }
        val bar = View(activity).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFFE2560B.toInt(), 0xFFFF7A1A.toInt())).apply { cornerRadius = dp(6f) }
        }
        track.addView(bar, FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT))
        c.addView(track, LinearLayout.LayoutParams(dp(300f).toInt(), dp(14f).toInt()))
        layout.addView(c, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        cover = c
        coverBar = bar
        coverTrack = track
    }

    private fun updateCover(field: Int) {
        if (field >= TRAINING_END) {
            dismissCover()
            return
        }
        val w = coverTrack?.width ?: 0
        coverBar?.let { b ->
            val lp = b.layoutParams
            lp.width = (w * (field.coerceIn(0, TRAINING_END).toFloat() / TRAINING_END)).toInt()
            b.layoutParams = lp
        }
    }

    private fun dismissCover() {
        val c = cover ?: return
        cover = null
        c.animate().alpha(0f).setDuration(350).withEndAction { (c.parent as? ViewGroup)?.removeView(c) }.start()
    }

    private fun startWorker() {
        Thread({
            try {
                val s = DatagramSocket(null).apply { reuseAddress = true; bind(InetSocketAddress(0)) }
                socket = s
                val addrs = (Online.stun(s) + Online.localAddresses(s.localPort)).distinct()
                val myPid = com.dfdx047.hiddenleaf.data.OnlineAdvanced.identity(activity).first
                val isRanked = req.fp.endsWith("#ranked")
                while (running) {
                    val q = try {
                        Online.quick(req.server, req.name, req.fp, addrs, Online.Opts(req.delay, req.relay), who, req.inputMode, myPid)
                    } catch (_: Exception) {
                        Thread.sleep(4000)
                        continue
                    }
                    if (q.host != null) {
                        val session = Online.Session(false, s.localPort, q.code, req.name, q.host.name, Online.candidates(q.host), q.opts.delay, q.opts.relay && req.relay, Online.relayUrl(req.server, q.code, q.key), q.token, q.inputMode, ranked = isRanked, server = req.server, opponentId = q.host.playerId)
                        showFound(session)
                        return@Thread
                    }
                    val r = Online.Room(q.code, q.key, q.token, q.inputMode)
                    room = r
                    var lastStun = System.currentTimeMillis()
                    while (running) {
                        Thread.sleep(3000)
                        val guest = try {
                            Online.pollGuest(req.server, r)
                        } catch (e: Online.Failure) {
                            if (e.why == "no room") break
                            null
                        }
                        if (guest != null) {
                            room = null
                            val session = Online.Session(true, s.localPort, r.code, req.name, guest.name, Online.candidates(guest), req.delay, req.relay, Online.relayUrl(req.server, r.code, r.key), r.token, r.inputMode, ranked = isRanked, server = req.server, opponentId = guest.playerId)
                            showFound(session)
                            return@Thread
                        }
                        if (System.currentTimeMillis() - lastStun > 20_000) {
                            lastStun = System.currentTimeMillis()
                            Online.stun(s, 500)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("un5", "training queue stopped: ${e.message}")
            }
        }, "training-queue").apply { isDaemon = true; start() }
    }

    private fun showFound(session: Online.Session) {
        activity.runOnUiThread {
            val layout = root ?: return@runOnUiThread
            dismissCover()
            badge?.visibility = View.GONE
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xF02A1A10.toInt(), 0xF01B1C24.toInt())).apply {
                    cornerRadius = dp(14f)
                    setStroke(dp(2f).toInt(), 0xFFFF7A1A.toInt())
                }
                setPadding(dp(16f).toInt(), dp(10f).toInt(), dp(12f).toInt(), dp(10f).toInt())
                elevation = dp(10f)
            }
            card.addView(TextView(activity).apply {
                text = activity.getString(R.string.online_training_found, session.peer)
                setTextColor(0xFFFFE6A0.toInt())
                textSize = 15f
                typeface = font(R.font.kanit_bold, Typeface.DEFAULT_BOLD)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(14f).toInt() })
            card.addView(TextView(activity).apply {
                text = activity.getString(R.string.online_training_accept)
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = font(R.font.kanit_bold, Typeface.DEFAULT_BOLD)
                background = GradientDrawable().apply { setColor(0xFFFF7A1A.toInt()); cornerRadius = dp(10f) }
                setPadding(dp(14f).toInt(), dp(6f).toInt(), dp(14f).toInt(), dp(6f).toInt())
                setOnClickListener {
                    stop()
                    Online.writeSession(paths, session)
                    (activity as? GameActivity)?.restartForOnlineMatch()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8f).toInt() })
            card.addView(TextView(activity).apply {
                text = activity.getString(R.string.cancel)
                setTextColor(0xFFEDE6DE.toInt())
                textSize = 13f
                typeface = font(R.font.kanit_medium, Typeface.DEFAULT)
                setPadding(dp(10f).toInt(), dp(6f).toInt(), dp(10f).toInt(), dp(6f).toInt())
                setOnClickListener {
                    (card.parent as? ViewGroup)?.removeView(card)
                    banner = null
                    stop()
                }
            })
            layout.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(10f).toInt()
            })
            banner = card
        }
    }

    fun stop() {
        running = false
        TrainingQueue.clear()
        runCatching { socket?.close() }
        socket = null
        val r = room
        room = null
        if (r != null) Thread { Online.leave(req.server, r) }.start()
    }

    companion object {
        private const val TRAINING_END = 4660
    }
}
