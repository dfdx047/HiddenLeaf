package com.dfdx047.hiddenleaf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.dfdx047.hiddenleaf.touch.NinjaPadView
import com.dfdx047.hiddenleaf.touch.PadScene
import com.dfdx047.hiddenleaf.touch.PadStyle
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.touch.Macro
import com.dfdx047.hiddenleaf.touch.MacroStep
import com.dfdx047.hiddenleaf.touch.ProTouch
import com.dfdx047.hiddenleaf.touch.Ps2
import com.dfdx047.hiddenleaf.ui.screens.ControlsContent
import com.dfdx047.hiddenleaf.ui.screens.MacroEditorContent
import com.dfdx047.hiddenleaf.ui.screens.ProGuideContent
import com.dfdx047.hiddenleaf.ui.theme.HiddenLeafTheme
import com.dfdx047.hiddenleaf.ui.theme.ThemeMode
import androidx.activity.compose.setContent
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper

/**
 * Pictures of the launcher and of the on-screen controller, drawn by Robolectric's native graphics, for looking at
 * the design without a device: build/shots/ (gradle testDebugUnitTest --tests '*ScreenshotTest*').
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    private val out = File(System.getProperty("hl.shots") ?: "build/shots").also { it.mkdirs() }

    private fun save(v: View, name: String, background: ((Canvas) -> Unit)? = null) {
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        background?.invoke(c)
        v.draw(c)
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "pt-w800dp-h400dp-land-xhdpi")
    fun pad() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (style in PadStyle.entries) for (scene in PadScene.entries) {
            val v = NinjaPadView(ctx, TouchConfig(mode = TouchMode.ALWAYS, style = style))
            v.previewScene = scene
            val w = (800 * 2)
            val h = (400 * 2)
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            save(v, "pad_${style.name.lowercase()}_${scene.name.lowercase()}") { c ->
                // a stand-in for the game: a dusk sky over a ground line
                val p = Paint()
                p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(60, 80, 140), Color.rgb(200, 140, 90), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
                p.shader = null
                p.color = Color.rgb(70, 110, 60)
                c.drawRect(0f, h * 0.78f, w.toFloat(), h.toFloat(), p)
            }
        }
    }

    /** The fight's controller at [w] x [h] px: the defaults, with the names, and Toque Pro with the thumb on the D-pad. */
    private fun fightPads(w: Int, h: Int, tag: String) {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cases = listOf(
            "default" to TouchConfig(mode = TouchMode.ALWAYS),
            "names" to TouchConfig(mode = TouchMode.ALWAYS, showNames = true),
            "pro" to TouchConfig(mode = TouchMode.ALWAYS, pro = ProTouch(on = true)),
        )
        for ((name, cfg) in cases) {
            val v = NinjaPadView(ctx, cfg)
            v.previewScene = PadScene.FIGHT
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            if (cfg.pro.on) {
                // a thumb landing just off the fixed D-pad (inside its wide touch area) and pushing up-right: a diagonal
                val t = android.os.SystemClock.uptimeMillis()
                val x = w * 0.13f + h * 0.05f
                val y = h * 0.68f
                v.onTouchEvent(android.view.MotionEvent.obtain(t, t, android.view.MotionEvent.ACTION_DOWN, x, y, 0))
                v.onTouchEvent(android.view.MotionEvent.obtain(t, t + 30, android.view.MotionEvent.ACTION_MOVE, x + h * 0.10f, y - h * 0.10f, 0))
            }
            save(v, "pad_${tag}_fight_$name") { c ->
                val p = Paint()
                p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(60, 80, 140), Color.rgb(200, 140, 90), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
                p.shader = null
                p.color = Color.rgb(70, 110, 60)
                c.drawRect(0f, h * 0.78f, w.toFloat(), h.toFloat(), p)
            }
        }
    }

    // (20:9 and 16:9 phones in landscape, 720 px high at 2x: 780 x 360 dp and 640 x 360 dp, the tightest)
    @Test
    @Config(qualifiers = "pt-w780dp-h360dp-land-xhdpi")
    fun pad209() = fightPads(1560, 720, "209")

    @Test
    @Config(qualifiers = "pt-w640dp-h360dp-land-xhdpi")
    fun pad169() = fightPads(1280, 720, "169")

    /** A composable on its own in an activity as tall as the page, so the whole page is in one picture. */
    private fun shootCompose(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        val controller = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup()
        controller.get().setContent {
            HiddenLeafTheme(mode = ThemeMode.DARK) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) { content() }
            }
        }
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50)) }
        save(controller.get().window.decorView, name)
    }

    private val sample = TouchConfig(
        mode = TouchMode.ALWAYS,
        macros = listOf(
            Macro("a", "Kage", listOf(MacroStep(Ps2.TRIANGLE, 70), MacroStep(0, 50), MacroStep(Ps2.CIRCLE or Ps2.TRIANGLE, 100))),
            Macro("b", "Turbo", listOf(MacroStep(Ps2.CIRCLE, 50), MacroStep(0, 50)), repeat = true),
        ),
    )

    @Test
    @Config(qualifiers = "pt-w400dp-h5200dp-port-xhdpi")
    fun controlsPage() = shootCompose("controls_pt") {
        ControlsContent(sample.copy(pro = ProTouch(on = true)), emptyMap(), onTouch = {}, onSetting = { _, _ -> })
    }

    @Test
    @Config(qualifiers = "pt-w400dp-h2200dp-port-xhdpi")
    fun controlsCampaignGuide() = shootCompose("controls_pt_campaign") {
        ControlsContent(sample, emptyMap(), onTouch = {}, onSetting = { _, _ -> }, startScene = PadScene.CAMPAIGN)
    }

    @Test
    @Config(qualifiers = "pt-w400dp-h3000dp-port-xhdpi")
    fun proGuide() = shootCompose("controls_pt_proguide") { ProGuideContent(sample) {} }

    @Test
    @Config(qualifiers = "pt-w400dp-h2400dp-port-xhdpi")
    fun macroEditor() = shootCompose("controls_pt_macro") { MacroEditorContent(sample, sample.macros[0], 0, {}) { _, _ -> } }

    @Test
    @Config(qualifiers = "pt-w780dp-h1600dp-land-xhdpi")
    fun macroEditorWide() = shootCompose("controls_pt_macro_wide") { MacroEditorContent(sample, sample.macros[0], 0, {}) { _, _ -> } }

    private fun shootActivity(name: String) {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50)) }
        val root = controller.get().window.decorView
        save(root, name)
    }

    /** The page [page] (a route of MainActivity's Dest) as the launcher shows it at start. */
    private fun shootPage(page: String, name: String) {
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_PAGE, page)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50)) }
        save(controller.get().window.decorView, name)
        controller.pause().stop().destroy()
    }

    /**
     * Every page in Portuguese and Spanish, on a 1560x720 phone (xhdpi: 780x360 dp) in landscape and in portrait, and
     * on tall windows of the same widths that show each page whole (for text cut at the bottom of long pages).
     */
    @Test
    fun pages() {
        val pages = listOf("home", "online", "controls", "settings", "more", "textures", "mods", "drivers", "cheats", "debug")
        val shapes = listOf(
            "land" to "w780dp-h360dp-land", "port" to "w360dp-h780dp-port",
            "landfull" to "w780dp-h2000dp", "portfull" to "w360dp-h2600dp-port",
        )
        for (lang in listOf("pt", "es")) for ((shape, q) in shapes) {
            org.robolectric.RuntimeEnvironment.setQualifiers("$lang-$q-xhdpi")
            for (p in pages) shootPage(p, "page_${p}_${lang}_$shape")
        }
    }

    @Test
    @Config(qualifiers = "pt-w400dp-h880dp-port-xhdpi")
    fun homePortrait() = shootActivity("home_portrait")

    @Test
    @Config(qualifiers = "pt-w880dp-h400dp-land-xhdpi")
    fun homeLandscape() = shootActivity("home_landscape")
}
