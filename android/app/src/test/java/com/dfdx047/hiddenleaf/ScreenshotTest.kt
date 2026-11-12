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
