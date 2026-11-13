package com.dfdx047.hiddenleaf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.dfdx047.hiddenleaf.data.Online
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.OnlineLobby
import com.dfdx047.hiddenleaf.engine.OnlineBridge
import com.dfdx047.hiddenleaf.ui.screens.OnlineScreen
import com.dfdx047.hiddenleaf.ui.theme.HiddenLeafTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Online screen, drawn (build/shots/online_*.png), and the session file the engine reads. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnlineScreenshotTest {
    private val out = File(System.getProperty("hl.shots") ?: "build/shots").also { it.mkdirs() }

    private fun shoot(name: String, lan: Boolean = false) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        // (the page's choice through its own model: Robolectric keeps the preferences of an earlier test in a cache)
        val lobby = ViewModelProvider(activity)[OnlineLobby::class.java]
        lobby.update(lobby.prefs.state.value.copy(lan = lan))
        activity.setContent { HiddenLeafTheme { OnlineScreen(AppViewModel(activity.application)) } }
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50)) }
        val v = activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "pt-w400dp-h880dp-port-xhdpi")
    fun onlinePortrait() = shoot("online_portrait")

    @Test
    @Config(qualifiers = "en-w880dp-h400dp-land-xhdpi")
    fun onlineLandscape() = shoot("online_landscape")

    @Test
    @Config(qualifiers = "pt-w400dp-h880dp-port-xhdpi")
    fun lanPortrait() = shoot("online_lan_portrait", lan = true)

    @Test
    fun sessionFile() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val paths = GamePaths(app)
        Online.writeSession(
            paths,
            Online.Session(true, 40123, "123456", "Naruto", "Sasuke\"", listOf("1.2.3.4:40123", "10.0.0.2:40123"), 2, true, "https://x.workers.dev/v1/ws?code=123456&key=k"),
        )
        val s = Online.readSession(paths)
        assertEquals("1", s["v"]?.single())
        assertEquals("host", s["role"]?.single())
        assertEquals("40123", s["port"]?.single())
        assertEquals("Sasuke", s["peer"]?.single())
        assertEquals(listOf("1.2.3.4:40123", "10.0.0.2:40123"), s["addr"])
        assertEquals("1", s["relay"]?.single())
        assertTrue(s["relay_url"]!!.single().startsWith("https://"))
        assertEquals(
            listOf("1.2.3.4:5000", "5.6.7.8:5000"),
            Online.candidates(Online.Peer("p", listOf("1.2.3.4:5000"), "5.6.7.8")),
        )
        assertEquals("playing", OnlineBridge.parse("phase=playing;rtt=40;reason=")["phase"])
    }
}
