package com.dfdx047.hiddenleaf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.engine.GameMenu
import com.dfdx047.hiddenleaf.engine.GameMenuActions
import com.dfdx047.hiddenleaf.engine.GameMenuState
import com.dfdx047.hiddenleaf.engine.MenuLook
import com.dfdx047.hiddenleaf.touch.Macro
import com.dfdx047.hiddenleaf.touch.MacroStep
import com.dfdx047.hiddenleaf.touch.PadMap
import com.dfdx047.hiddenleaf.touch.ProTouch
import com.dfdx047.hiddenleaf.touch.Ps2
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The game menu (the drawer over the frozen game), drawn for each tab in the launcher's light and dark themes, on a
 * 20:9 phone (1560x720) held both ways, a 16:9 handheld (1920x1080) and a square one (RG Cube, 720x720):
 * build/shots/menu_*.png. The "_full" pictures are a tab's whole page, its screens one under the other.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameMenuScreenshotTest {
    private val out = File(System.getProperty("hl.shots") ?: "build/shots").also { it.mkdirs() }

    private val names = listOf("game", "graphics", "pad", "touch", "help")

    private fun state(theme: ThemeMode, padNav: Boolean = false) = GameMenuState(
        engine = mapOf(EngineSettings.SCALE4 to 8, EngineSettings.ASPECT to 0, EngineSettings.FPS_MODE to 60, EngineSettings.METER to 1),
        touch = TouchConfig(
            pro = ProTouch(on = true),
            macros = listOf(Macro("m1", "Combo", listOf(MacroStep(Ps2.CIRCLE, 60), MacroStep(0, 60), MacroStep(Ps2.CIRCLE, 60)))),
        ),
        padMap = PadMap().bind(Ps2.TRIANGLE, android.view.KeyEvent.KEYCODE_BUTTON_X),
        maxRefresh = 120f,
        rendererInUse = "Vulkan",
    ).apply {
        look = MenuLook(theme)
        open = true
        controllers = listOf("Retroid Pocket Controller")
        this.padNav = padNav
    }

    private val actions = object : GameMenuActions {
        override fun setting(key: String, value: Int) {}
        override fun touch(c: TouchConfig) {}
        override fun padMap(m: PadMap) {}
        override fun editLayout() {}
        override fun resume() {}
        override fun quit() {}
    }

    private fun idle() = repeat(12) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50)) }

    private fun grab(activity: ComponentActivity): Bitmap {
        val v = activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        return bmp
    }

    private fun save(bmp: Bitmap, name: String) =
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

    /** Every tab in both themes; [full]: also each tab's whole page (the drawer's part of the screen, page under page). */
    /**
     * One picture: a new activity each time (Robolectric draws the first composition; later changes of the state are
     * not recomposed here). [scroll]: the page scrolled down that far first.
     */
    private fun picture(theme: ThemeMode, tab: Int, scroll: Int = 0): Pair<Bitmap, GameMenuState> {
        val st = state(theme, padNav = true)
        st.tab = tab
        if (scroll > 0) runBlocking { st.scrolls[tab].scrollTo(scroll) }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            Box(Modifier.fillMaxSize()) {
                FrozenGame()
                GameMenu(st, actions)
            }
        }
        idle()
        return grab(activity) to st
    }

    private fun shoot(prefix: String, full: Boolean = false, tabs: List<Int> = names.indices.toList()) {
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            val themeName = theme.name.lowercase()
            for (t in tabs) {
                val (first, st) = picture(theme, t)
                save(first, "menu_${prefix}_${names[t]}_$themeName")
                if (full) {
                    val scroll = st.scrolls[t]
                    val pages = arrayListOf(first)
                    val step = (scroll.viewportSize * 0.85f).toInt().coerceAtLeast(100)
                    var y = step
                    while (y < scroll.maxValue + step && pages.size < 14) {
                        pages += picture(theme, t, y.coerceAtMost(scroll.maxValue)).first
                        y += step
                    }
                    val w = minOf(pages[0].width, 720) // (the drawer's part)
                    val h = pages[0].height
                    val sheet = Bitmap.createBitmap(w, h * pages.size, Bitmap.Config.ARGB_8888)
                    val c = Canvas(sheet)
                    pages.forEachIndexed { i, p -> c.drawBitmap(p, 0f, (i * h).toFloat(), null) }
                    save(sheet, "menu_${prefix}_${names[t]}_${themeName}_full")
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "pt-w780dp-h360dp-land-xhdpi")
    fun phoneLandscapePt() = shoot("phone_land_pt", full = true)

    @Test
    @Config(qualifiers = "pt-w360dp-h780dp-port-xhdpi")
    fun phonePortraitPt() = shoot("phone_port_pt")

    @Test
    @Config(qualifiers = "en-w780dp-h360dp-land-xhdpi")
    fun phoneLandscapeEn() = shoot("phone_land_en")

    @Test
    @Config(qualifiers = "es-w780dp-h360dp-land-xhdpi")
    fun phoneLandscapeEs() = shoot("phone_land_es")

    @Test
    @Config(qualifiers = "pt-w640dp-h360dp-land-xxhdpi")
    fun handheld1080() = shoot("1080p_pt", tabs = listOf(0, 2))

    @Test
    @Config(qualifiers = "es-w360dp-h360dp-land-xhdpi")
    fun squareCube() = shoot("square_es", tabs = listOf(0, 2, 3))
}

/** Something like a fight behind the menu: sky, ground, two fighters and the health bars. */
@androidx.compose.runtime.Composable
private fun FrozenGame() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF5B8FD8), Color(0xFFF2C38B), Color(0xFF6E8B3D))))) {
        Box(Modifier.align(Alignment.TopStart).offset(24.dp, 16.dp).size(220.dp, 14.dp).background(Color(0xFFE8D24A), RoundedCornerShape(4.dp)))
        Box(Modifier.align(Alignment.TopEnd).offset((-24).dp, 16.dp).size(220.dp, 14.dp).background(Color(0xFFE8D24A), RoundedCornerShape(4.dp)))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.22f).background(Color(0xFF4A3A22)))
        Box(Modifier.align(Alignment.Center).offset((-90).dp, 40.dp).size(46.dp, 110.dp).background(Color(0xFFF07A1A), RoundedCornerShape(18.dp)))
        Box(Modifier.align(Alignment.Center).offset(90.dp, 40.dp).size(46.dp, 110.dp).background(Color(0xFF2D3A8C), RoundedCornerShape(18.dp)))
    }
}
