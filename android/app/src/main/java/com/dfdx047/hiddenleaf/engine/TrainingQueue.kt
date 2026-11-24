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
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.security.SecureRandom

/**
 * Training Mode while queuing: lets the player practice offline inside GameActivity while a background thread
 * polls /v1/quick. When an opponent is found, a Konoha-styled banner slides in at the top so the player can
 * accept and jump straight into the online match (or decline and keep practicing).
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

    fun arm(req: Request) { pending = req }
    fun clear() { pending = null }
}

class TrainingQueueOverlay(private val activity: Activity, private val paths: GamePaths, private val req: TrainingQueue.Request) {
    @Volatile private var running = true
    private var root: ViewGroup? = null
    private var badge: TextView? = null
    private var banner: View? = null
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
        startWorker()
    }

    private fun startWorker() {
        Thread({
            try {
                val s = DatagramSocket(null).apply { reuseAddress = true; bind(InetSocketAddress(0)) }
                socket = s
                val addrs = (Online.stun(s) + Online.localAddresses(s.localPort)).distinct()
                while (running) {
                    val q = try {
                        Online.quick(req.server, req.name, req.fp, addrs, Online.Opts(req.delay, req.relay), who, req.inputMode)
                    } catch (_: Exception) {
                        Thread.sleep(4000)
                        continue
                    }
                    if (q.host != null) {
                        val session = Online.Session(false, s.localPort, q.code, req.name, q.host.name, Online.candidates(q.host), q.opts.delay, q.opts.relay && req.relay, Online.relayUrl(req.server, q.code, q.key), q.token, q.inputMode)
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
                            val session = Online.Session(true, s.localPort, r.code, req.name, guest.name, Online.candidates(guest), req.delay, req.relay, Online.relayUrl(req.server, r.code, r.key), r.token, r.inputMode)
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
}
