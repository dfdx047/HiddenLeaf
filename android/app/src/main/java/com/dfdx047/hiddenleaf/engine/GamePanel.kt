package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings

/**
 * The panel over the game: the game is frozen while it is open (GameActivity). Each choice is saved in the engine's settings
 * and in effect when the game goes on.
 */
class GamePanel(
    context: Context,
    private val settings: EngineSettings,
    private val onApply: () -> Unit,
    private val onMeter: (Int) -> Unit,
    private val onControls: () -> Unit,
    private val onResume: () -> Unit,
    private val onQuit: () -> Unit,
) : FrameLayout(context) {
    private val dp = resources.displayMetrics.density
    private val accent = 0xFFFF8A1E.toInt()
    private val box = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(px(18), px(14), px(18), px(14))
    }
    private val buttons = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(px(18), px(12), px(18), px(4))
    }

    init {
        setBackgroundColor(0xB0000000.toInt())
        isClickable = true
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xF2202028.toInt())
                cornerRadius = 14 * dp
            }
        }
        card.addView(buttons)
        card.addView(
            ScrollView(context).apply { addView(box) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        val w = (resources.displayMetrics.widthPixels * 0.86f).toInt().coerceAtMost(px(620))
        val h = (resources.displayMetrics.heightPixels * 0.92f).toInt()
        addView(card, LayoutParams(w, h, Gravity.CENTER))
        title(context.getString(R.string.panel_title))

        val scale4 = settings.get(EngineSettings.SCALE4, 4)
        choices(context.getString(R.string.resolution), listOf("1x" to 4, "2x" to 8, "3x" to 12, "4x" to 16), scale4) {
            settings.set(EngineSettings.SCALE4, it)
            settings.set(EngineSettings.SCALE, it / 4)
            onApply()
        }
        hint(context.getString(R.string.panel_resolution_hint))
        choices(
            context.getString(R.string.screen_filter),
            listOf(context.getString(R.string.filter_bilinear) to 0, context.getString(R.string.filter_sharp) to 1, "FXAA" to 2, "CAS" to 3),
            settings.get(EngineSettings.FILTER, 0),
        ) {
            settings.set(EngineSettings.FILTER, it)
            onApply()
        }
        choices(
            context.getString(R.string.panel_widescreen),
            listOf("4:3" to 1333, "16:9" to 1778, context.getString(R.string.panel_full_screen) to 0),
            settings.get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT),
        ) {
            settings.set(EngineSettings.ASPECT, it)
            onApply()
        }
        choices(
            context.getString(R.string.panel_fps_limit),
            listOf(
                "30" to 30, context.getString(R.string.panel_fps_50) to 50,
                context.getString(R.string.panel_fps_60) to 60, context.getString(R.string.panel_fps_120) to 120,
            ).filter { it.second != 120 || DisplayCaps.offers120(context) },
            settings.get(EngineSettings.FPS_MODE, 50),
        ) {
            settings.set(EngineSettings.FPS_MODE, it)
            onApply()
        }
        hint(context.getString(R.string.panel_fps_hint))
        choices(
            context.getString(R.string.panel_counter),
            listOf(
                context.getString(R.string.off) to 0, "FPS" to 1, context.getString(R.string.panel_counter_ms) to 2,
                context.getString(R.string.panel_counter_full) to 3,
            ),
            settings.get(EngineSettings.METER, 0),
        ) {
            settings.set(EngineSettings.METER, it)
            onMeter(it)
        }
        hint(context.getString(R.string.panel_save_hint))
        buttons.addView(button(context.getString(R.string.panel_resume), accent, Color.BLACK) { onResume() })
        buttons.addView(button(context.getString(R.string.touch_controls), 0xFF3A3A46.toInt(), Color.WHITE) { onControls() })
        buttons.addView(button(context.getString(R.string.panel_quit), 0xFF3A3A46.toInt(), Color.WHITE) { onQuit() })
    }

    private fun px(v: Int) = (v * dp).toInt()

    private fun title(text: String) {
        box.addView(TextView(context).apply {
            this.text = text
            setTextColor(accent)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        })
    }

    private fun hint(text: String) {
        box.addView(TextView(context).apply {
            this.text = text
            setTextColor(0xFFB8B8C4.toInt())
            textSize = 11f
            setPadding(0, px(4), 0, 0)
        })
    }

    private fun button(text: String, bg: Int, fg: Int, click: () -> Unit) = TextView(context).apply {
        this.text = text
        setTextColor(fg)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(px(14), px(10), px(14), px(10))
        background = GradientDrawable().apply {
            setColor(bg)
            cornerRadius = 10 * dp
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = px(8) }
        setOnClickListener { click() }
    }

    private fun choices(label: String, options: List<Pair<String, Int>>, selected: Int, pick: (Int) -> Unit) {
        box.addView(TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, px(10), 0, px(4))
        })
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val views = ArrayList<TextView>()
        fun paint(v: Int) {
            options.forEachIndexed { i, o ->
                val on = o.second == v
                views[i].setTextColor(if (on) Color.BLACK else Color.WHITE)
                (views[i].background as GradientDrawable).setColor(if (on) accent else 0xFF3A3A46.toInt())
            }
        }
        options.forEach { (name, value) ->
            val chip = TextView(context).apply {
                text = name
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(px(12), px(8), px(12), px(8))
                background = GradientDrawable().apply { cornerRadius = 8 * dp }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = px(6) }
                setOnClickListener {
                    paint(value)
                    pick(value)
                }
                tag = name
                contentDescription = name
            }
            views += chip
            row.addView(chip)
        }
        paint(options.minByOrNull { kotlin.math.abs(it.second - selected) }?.second ?: selected)
        box.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })
    }
}
