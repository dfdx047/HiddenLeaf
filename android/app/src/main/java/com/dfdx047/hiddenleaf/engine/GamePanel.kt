package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.touch.PadSkin
import com.dfdx047.hiddenleaf.touch.PadStyle
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode

/**
 * The game's menu, over the frozen game (GameActivity): three tabs, Graphics, Controls (with the tips: what each button
 * does, the menus by touch) and Layout (the on-screen controls). Warm paper and ink, the colours of the game's own menus,
 * drawn here (nothing of the game's art). Each choice is saved at once: the engine's settings (un5_settings.txt) and
 * touch_controls.json.
 */
class GamePanel(
    context: Context,
    private val settings: EngineSettings,
    private val touch: () -> TouchConfig,
    private val setTouch: (TouchConfig) -> Unit,
    private val onApply: () -> Unit,
    private val onMeter: (Int) -> Unit,
    private val onEditLayout: () -> Unit,
    private val onResume: () -> Unit,
    private val onQuit: () -> Unit,
) : FrameLayout(context) {
    private val dp = resources.displayMetrics.density
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(px(20), px(6), px(20), px(18))
    }
    private val scroll = ScrollView(context).apply { addView(content) }
    private val heading = TextView(context).apply {
        setTextColor(PAPER)
        textSize = 19f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        setPadding(px(20), px(10), px(20), px(10))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ORANGE, RED))
    }
    private val tabViews = ArrayList<TextView>()
    private var tab = 0

    init {
        setBackgroundColor(0xC0100804.toInt())
        isClickable = true
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(PAPER)
                cornerRadius = 16 * dp
                setStroke(px(3), INK)
            }
            clipToOutline = true
        }
        // the rail: the tabs, then continue and quit at the bottom
        val rail = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(INK)
            setPadding(px(10), px(14), px(10), px(12))
        }
        rail.addView(TextView(context).apply {
            text = context.getString(R.string.panel_title)
            setTextColor(ORANGE)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            setPadding(px(6), 0, px(6), px(12))
        })
        listOf(R.string.gp_tab_graphics, R.string.gp_tab_controls, R.string.gp_tab_layout).forEachIndexed { i, r ->
            val t = tabButton(context.getString(r)) { show(i) }
            tabViews += t
            rail.addView(t)
        }
        rail.addView(View(context), LinearLayout.LayoutParams(1, 0, 1f))
        rail.addView(railButton(context.getString(R.string.panel_resume), ORANGE, INK) { onResume() })
        rail.addView(railButton(context.getString(R.string.panel_quit), 0xFF5A3A24.toInt(), PAPER) { onQuit() })
        card.addView(rail, LinearLayout.LayoutParams(px(170), ViewGroup.LayoutParams.MATCH_PARENT))
        val right = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        right.addView(heading, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        right.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        val w = (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(px(900))
        val h = (resources.displayMetrics.heightPixels * 0.92f).toInt()
        addView(card, LayoutParams(w, h, Gravity.CENTER))
        show(0)
    }

    private fun show(i: Int) {
        tab = i
        tabViews.forEachIndexed { k, t ->
            val on = k == i
            t.setTextColor(if (on) INK else PAPER)
            (t.background as GradientDrawable).setColor(if (on) ORANGE else Color.TRANSPARENT)
        }
        content.removeAllViews()
        when (i) {
            0 -> graphics()
            1 -> controls()
            else -> layout()
        }
        scroll.scrollTo(0, 0)
    }

    // ---- Graphics

    private fun graphics() {
        heading.text = context.getString(R.string.gp_tab_graphics)
        val scale4 = settings.get(EngineSettings.SCALE4, 4)
        section(context.getString(R.string.resolution))
        choices(
            listOf("1x · 448p" to 4, "2x · 896p" to 8, "3x · 1344p" to 12, "4x · 1792p" to 16, "6x · 2688p" to 24, "8x · 4K" to 32),
            scale4,
        ) {
            settings.set(EngineSettings.SCALE4, it)
            settings.set(EngineSettings.SCALE, it / 4)
            onApply()
        }
        hint(context.getString(R.string.gp_resolution_hint))
        section(context.getString(R.string.screen_filter))
        choices(
            listOf(context.getString(R.string.filter_bilinear) to 0, context.getString(R.string.filter_sharp) to 1, "FXAA" to 2, "CAS" to 3),
            settings.get(EngineSettings.FILTER, 0),
        ) {
            settings.set(EngineSettings.FILTER, it)
            onApply()
        }
        hint(context.getString(R.string.gp_filter_hint))
        section(context.getString(R.string.gp_shape))
        choices(
            listOf("4:3" to 1333, "16:9" to 1778, context.getString(R.string.panel_full_screen) to 0),
            settings.get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT),
        ) {
            settings.set(EngineSettings.ASPECT, it)
            onApply()
        }
        section(context.getString(R.string.gp_fps))
        choices(
            listOf(
                "30" to 30, context.getString(R.string.panel_fps_50) to 50, "60" to 60, "90" to 90, "120" to 120,
            ),
            settings.get(EngineSettings.FPS_MODE, 50),
        ) {
            settings.set(EngineSettings.FPS_MODE, it)
            onApply()
        }
        hint(context.getString(R.string.gp_fps_hint, DisplayCaps.maxRefresh(context)))
        section(context.getString(R.string.gp_counter))
        choices(
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
    }

    // ---- Controls: the buttons and the tips

    private fun controls() {
        heading.text = context.getString(R.string.gp_tab_controls)
        section(context.getString(R.string.gp_fight))
        tip("○", PS_RED, R.string.gp_tip_attack)
        tip("✕", PS_BLUE, R.string.gp_tip_jump)
        tip("△", PS_GREEN, R.string.gp_tip_chakra)
        tip("□", PS_PINK, R.string.gp_tip_item)
        tip("R2", INK, R.string.gp_tip_guard)
        tip("L1 R1", INK, R.string.gp_tip_items)
        tip("R3", INK, R.string.gp_tip_awaken)
        tip("△○", ORANGE, R.string.gp_tip_jutsu)
        tip("△△○", RED, R.string.gp_tip_ougi)
        section(context.getString(R.string.gp_assists))
        choices(
            listOf(context.getString(R.string.off) to 0, context.getString(R.string.on) to 1),
            settings.get(EngineSettings.SPECIAL_ONE, 1),
        ) {
            settings.set(EngineSettings.SPECIAL_ONE, it)
            TouchBridge.setAssist(it, settings.get(EngineSettings.MASH_PERCENT, 100))
        }
        hint(context.getString(R.string.gp_special_one_hint))
        label(context.getString(R.string.gp_mash))
        choices(
            listOf("100%" to 100, "150%" to 150, "200%" to 200),
            settings.get(EngineSettings.MASH_PERCENT, 100),
        ) {
            settings.set(EngineSettings.MASH_PERCENT, it)
            TouchBridge.setAssist(settings.get(EngineSettings.SPECIAL_ONE, 1), it)
        }
        hint(context.getString(R.string.gp_mash_hint))
        section(context.getString(R.string.gp_campaign))
        tip("◎", INK, R.string.gp_tip_stick)
        tip("SELECT", INK, R.string.gp_tip_select)
        section(context.getString(R.string.gp_menus))
        tip("☝", INK, R.string.gp_tip_tap)
        tip("⇆", INK, R.string.gp_tip_slide)
        tip("☝…", INK, R.string.gp_tip_hold)
        tip("✌", INK, R.string.gp_tip_two)
        tip("✌⇆", INK, R.string.gp_tip_two_slide)
        tip("✌…", INK, R.string.gp_tip_two_hold)
        tip("3☝", INK, R.string.gp_tip_three)
        hint(context.getString(R.string.gp_controls_hint))
    }

    // ---- Layout: the on-screen controls

    private fun layout() {
        heading.text = context.getString(R.string.gp_tab_layout)
        val c = touch()
        section(context.getString(R.string.gp_touch_mode))
        choices(
            listOf(
                context.getString(R.string.gp_touch_auto) to TouchMode.AUTO.ordinal,
                context.getString(R.string.gp_touch_always) to TouchMode.ALWAYS.ordinal,
                context.getString(R.string.off) to TouchMode.OFF.ordinal,
            ),
            c.mode.ordinal,
        ) { setTouch(touch().copy(mode = TouchMode.entries[it])) }
        hint(context.getString(R.string.gp_touch_mode_hint))
        section(context.getString(R.string.touch_skin))
        choices(
            listOf(
                context.getString(R.string.skin_playstation) to PadSkin.PLAYSTATION.ordinal,
                context.getString(R.string.skin_xbox) to PadSkin.XBOX.ordinal,
                context.getString(R.string.skin_nintendo) to PadSkin.NINTENDO.ordinal,
            ),
            c.skin.ordinal,
        ) { setTouch(touch().copy(skin = PadSkin.entries[it])) }
        label(context.getString(R.string.gp_style))
        choices(
            listOf(context.getString(R.string.gp_style_ink) to PadStyle.INK.ordinal, context.getString(R.string.gp_style_glass) to PadStyle.GLASS.ordinal),
            c.style.ordinal,
        ) { setTouch(touch().copy(style = PadStyle.entries[it])) }
        label(context.getString(R.string.gp_opacity))
        choices(listOf("25%" to 25, "50%" to 50, "75%" to 75, "100%" to 100), c.opacity) { setTouch(touch().copy(opacity = it)) }
        label(context.getString(R.string.gp_size))
        choices(listOf("80%" to 80, "100%" to 100, "120%" to 120, "140%" to 140), (c.scale * 100).toInt()) {
            setTouch(touch().copy(scale = it / 100f))
        }
        section(context.getString(R.string.gp_touch_more))
        label(context.getString(R.string.gp_menu_touch))
        onOff(c.menuTouch) { setTouch(touch().copy(menuTouch = it)) }
        hint(context.getString(R.string.gp_menu_touch_hint))
        label(context.getString(R.string.gp_float_stick))
        onOff(c.sticksFloat) { setTouch(touch().copy(sticksFloat = it)) }
        label(context.getString(R.string.gp_haptics))
        onOff(c.haptics) { setTouch(touch().copy(haptics = it)) }
        content.addView(railButton(context.getString(R.string.gp_edit_layout), ORANGE, INK) { onEditLayout() }.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = px(16)
        })
        hint(context.getString(R.string.touch_edit_hint))
    }

    // ---- pieces

    private fun px(v: Int) = (v * dp).toInt()

    private fun tabButton(text: String, click: () -> Unit) = TextView(context).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(px(12), px(11), px(12), px(11))
        background = GradientDrawable().apply { cornerRadii = floatArrayOf(0f, 0f, 14 * dp, 14 * dp, 14 * dp, 14 * dp, 0f, 0f) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(6) }
        setOnClickListener { click() }
        contentDescription = text
    }

    private fun railButton(text: String, bg: Int, fg: Int, click: () -> Unit) = TextView(context).apply {
        this.text = text
        setTextColor(fg)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(px(10), px(11), px(10), px(11))
        background = GradientDrawable().apply {
            setColor(bg)
            cornerRadius = 22 * dp
            setStroke(px(2), 0xFF1A0E06.toInt())
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(8) }
        setOnClickListener { click() }
        contentDescription = text
    }

    private fun section(text: String) {
        content.addView(TextView(context).apply {
            this.text = text
            setTextColor(INK)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            setPadding(0, px(14), 0, px(2))
        })
        content.addView(View(context).apply { setBackgroundColor(ORANGE) }, LinearLayout.LayoutParams(px(56), px(3)).apply { bottomMargin = px(6) })
    }

    private fun label(text: String) {
        content.addView(TextView(context).apply {
            this.text = text
            setTextColor(INK)
            textSize = 13f
            setPadding(0, px(8), 0, px(4))
        })
    }

    private fun hint(text: String) {
        content.addView(TextView(context).apply {
            this.text = text
            setTextColor(0xFF7A5A3C.toInt())
            textSize = 11.5f
            setPadding(0, px(4), 0, 0)
        })
    }

    /** A button of the game's (its symbol in a coin) and what it does. */
    private fun tip(symbol: String, color: Int, text: Int) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, px(4), 0, px(4))
        }
        row.addView(TextView(context).apply {
            this.text = symbol
            setTextColor(if (color == INK) PAPER else Color.WHITE)
            textSize = if (symbol.length > 2) 11f else 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 15 * dp
                setStroke(px(2), 0xFF1A0E06.toInt())
            }
        }, LinearLayout.LayoutParams(px(58), px(30)))
        row.addView(TextView(context).apply {
            this.text = context.getString(text)
            setTextColor(INK)
            textSize = 13f
            setPadding(px(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(row)
    }

    private fun onOff(on: Boolean, pick: (Boolean) -> Unit) =
        choices(listOf(context.getString(R.string.off) to 0, context.getString(R.string.on) to 1), if (on) 1 else 0) { pick(it == 1) }

    private fun choices(options: List<Pair<String, Int>>, selected: Int, pick: (Int) -> Unit) {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val views = ArrayList<TextView>()
        fun paint(v: Int) {
            options.forEachIndexed { i, o ->
                val on = o.second == v
                views[i].setTextColor(if (on) INK else 0xFF5A3A24.toInt())
                views[i].typeface = if (on) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                (views[i].background as GradientDrawable).apply {
                    setColor(if (on) ORANGE else PAPER_DARK)
                    setStroke(px(2), if (on) INK else 0xFFB89868.toInt())
                }
            }
        }
        options.forEach { (name, value) ->
            val chip = TextView(context).apply {
                text = name
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(px(14), px(9), px(14), px(9))
                background = GradientDrawable().apply { cornerRadius = 18 * dp }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = px(8) }
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
        content.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })
    }

    private companion object {
        val PAPER = 0xFFF5E6C4.toInt()
        val PAPER_DARK = 0xFFEAD3A0.toInt()
        val INK = 0xFF2E1C10.toInt()
        val ORANGE = 0xFFF28C1E.toInt()
        val RED = 0xFFC9361F.toInt()
        val PS_RED = 0xFFD8443A.toInt()
        val PS_BLUE = 0xFF5A82C8.toInt()
        val PS_GREEN = 0xFF3FA37A.toInt()
        val PS_PINK = 0xFFD06AA8.toInt()
    }
}
