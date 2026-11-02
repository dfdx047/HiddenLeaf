package com.dfdx047.hiddenleaf.engine

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import com.dfdx047.hiddenleaf.MainActivity
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.Renderer
import com.dfdx047.hiddenleaf.data.GpuDrivers
import com.dfdx047.hiddenleaf.touch.PadSkin
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.touch.TouchListener
import com.dfdx047.hiddenleaf.touch.TouchSurface
import com.dfdx047.hiddenleaf.touch.createTouchSurface
import com.dfdx047.hiddenleaf.touch.view
import com.dfdx047.hiddenleaf.touch.TouchStore
import java.io.File
import org.libsdl.app.SDLActivity

/**
 * The game itself: SDL's activity, which loads libSDL3.so and libun5.so (the recompiled engine) and calls
 * SDL_main with the arguments below. It runs in a process of its own (":game" in the manifest): when the game ends
 * its process goes with it and the launcher stays as it was.
 *
 * Over the game's surface: the on-screen controller ([TouchSurface]: the game-specific layout or the classic one), which feeds player 1's pad through
 * [TouchBridge] and steps aside while the engine's own window (settings, online) is open.
 */
class GameActivity : SDLActivity() {
    private lateinit var paths: GamePaths
    private lateinit var store: TouchStore
    private var pad: TouchSurface? = null
    private val handler = Handler(Looper.getMainLooper())

    private val watchMenu = object : Runnable {
        override fun run() {
            pad?.suppressed = panel != null
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        paths = GamePaths(this).also { it.ensure() }
        // the renderer for this start (Vulkan, or OpenGL ES for Mali when automatic): written before the engine reads it
        runCatching {
            val es = EngineSettings(paths.settings)
            Renderer.apply(es, paths)
            // the in-game menu's language: the launcher's (0 English, 1 Portuguese, 2 Spanish)
            val lang = when (resources.configuration.locales[0].language) { "pt" -> 1; "es" -> 2; else -> 0 }
            if (es.get(EngineSettings.LANG, -1) != lang) es.set(EngineSettings.LANG, lang)
            orientation = es.get(EngineSettings.ORIENTATION, 0)
        }
        requestedOrientation = orientationFlag()
        store = TouchStore(paths.root)
        installDataList()
        runCatching { copyGsxResources() }
        super.onCreate(savedInstanceState)
        addTouchPad()
    }

    /** The player's choice (launcher, Settings): 0 landscape, 1 portrait, 2 as the device is held. */
    private var orientation = 0

    private fun orientationFlag(): Int = when (orientation) {
        1 -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        2 -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    /** SDL picks an orientation from the window's size when it is made; the player's choice wins over it. */
    override fun setOrientationBis(w: Int, h: Int, resizable: Boolean, hint: String?) {
        requestedOrientation = orientationFlag()
    }

    /** The counter (the panel's choice, settings' "meter"): nothing on screen when off. */
    private var meterLevel = 0

    private fun addPerfOverlay(layout: ViewGroup) {
        val text = TextView(this).apply {
            setTextColor(android.graphics.Color.YELLOW)
            setBackgroundColor(0x80000000.toInt())
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            visibility = android.view.View.GONE
            setPadding(8, 2, 8, 2)
        }
        layout.addView(text, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.TOP or android.view.Gravity.END))
        text.post(object : Runnable {
            override fun run() {
                val line = meterText(TouchBridge.perfLine(), meterLevel)
                text.text = line
                text.visibility = if (line.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                text.postDelayed(this, 500)
            }
        })
    }

    /** The engine's line ("50.0 fps | EE .. | worst 21 ms") as much of it as the level shows. */
    private fun meterText(raw: String, level: Int): String {
        if (level == 0 || raw.isEmpty()) return ""
        val vsyncs = raw.substringBefore(' ').toFloatOrNull() ?: return ""
        val picture = vsyncs / TouchBridge.divider().coerceAtLeast(1)
        val fps = "%.0f fps (%.0f Hz)".format(picture, vsyncs)
        if (level == 1) return fps
        val worst = Regex("worst ([0-9.]+)").find(raw)?.groupValues?.get(1) ?: "?"
        val frame = "$fps  %.1f ms  max %s".format(1000f / vsyncs.coerceAtLeast(1f), worst)
        if (level == 2) return frame
        fun cat(n: String) = Regex("\\b$n ([0-9.]+)").find(raw)?.groupValues?.get(1) ?: "?"
        return "$frame\nEE ${cat("EE")}  VU1 ${cat("VU1")}  GS ${cat("GS")}  IOP ${cat("IOP")}  idle ${cat("idle")}"
    }

    // ---- the panel over the game: it freezes the game while open
    private var panel: GamePanel? = null

    private fun openPanel() {
        if (panel != null) return
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        TouchBridge.setPad(0, 128, 128, 128, 128)
        TouchBridge.setPaused(true)
        val es = EngineSettings(paths.settings)
        val p = GamePanel(
            this, es,
            onApply = { applyVideo(es) },
            onMeter = { meterLevel = it },
            onControls = { showOptions() },
            onResume = { closePanel() },
            onQuit = { quitToApp() },
        )
        layout.addView(p, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        panel = p
    }

    private fun closePanel() {
        val p = panel ?: return
        (p.parent as? ViewGroup)?.removeView(p)
        panel = null
        TouchBridge.setPaused(false)
    }

    private fun applyVideo(es: EngineSettings) {
        TouchBridge.setVideo(
            es.get(EngineSettings.SCALE4, 4), es.get(EngineSettings.FILTER, 0),
            es.get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT), es.get(EngineSettings.FPS_MODE, 50),
        )
    }

    private fun addTouchPad() {
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        val view = createTouchSurface(this, store.load())
        view.listener = object : TouchListener {
            override fun onPad(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int) = TouchBridge.setPad(buttons, lx, ly, rx, ry)

            override fun onMenu() = openPanel()

            override fun onOptions() = showOptions()

            override fun onConfigChanged(config: TouchConfig) = store.save(config)

            override fun onEditDone() {
                view.editing = false
            }
        }
        layout.addView(view.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        pad = view
        meterLevel = runCatching { EngineSettings(paths.settings).get(EngineSettings.METER, 0) }.getOrDefault(0)
        addPerfOverlay(layout)
    }

    override fun onResume() {
        super.onResume()
        pad?.let { it.config = store.load() }
        handler.post(watchMenu)
    }

    override fun onPause() {
        handler.removeCallbacks(watchMenu)
        TouchBridge.setPad(0, 128, 128, 128, 128)
        super.onPause()
    }

    /** The controller's own quick menu: look, opacity, layout editing, shown or not. */
    private fun showOptions() {
        val view = pad ?: return
        var c = view.config
        val dp = resources.displayMetrics.density
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), 0)
        }
        box.addView(TextView(this).apply { setText(R.string.touch_skin) })
        val skins = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        PadSkin.entries.forEachIndexed { i, s ->
            skins.addView(RadioButton(this).apply {
                id = 1000 + i
                setText(skinName(s))
                isChecked = c.skin == s
            })
        }
        skins.setOnCheckedChangeListener { _, id ->
            c = c.copy(skin = PadSkin.entries[id - 1000])
            view.config = c
            store.save(c)
        }
        box.addView(skins)
        val opacityLabel = TextView(this).apply { text = getString(R.string.touch_opacity_n, c.opacity) }
        box.addView(opacityLabel)
        box.addView(SeekBar(this).apply {
            max = 90
            progress = c.opacity - 10
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    c = c.copy(opacity = p + 10)
                    opacityLabel.text = getString(R.string.touch_opacity_n, c.opacity)
                    view.config = c
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) = store.save(c)
            })
        })
        AlertDialog.Builder(this)
            .setTitle(R.string.touch_controls)
            .setView(box)
            .setPositiveButton(R.string.touch_edit_layout) { _, _ -> view.editing = true }
            .setNeutralButton(if (c.mode == TouchMode.OFF) R.string.touch_show_controls else R.string.touch_hide_controls) { _, _ ->
                c = c.copy(mode = if (c.mode == TouchMode.OFF) TouchMode.AUTO else TouchMode.OFF)
                view.config = c
                store.save(c)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun skinName(s: PadSkin): Int = when (s) {
        PadSkin.PLAYSTATION -> R.string.skin_playstation
        PadSkin.XBOX -> R.string.skin_xbox
        PadSkin.NINTENDO -> R.string.skin_nintendo
    }

    /** A physical controller in use: the on-screen one hides (automatic mode) until the screen is touched again. */
    private fun padUsed() {
        val v = pad ?: return
        if (v.config.mode == TouchMode.AUTO && !v.editing) v.hiddenForPad = true
    }

    private fun fromController(source: Int): Boolean =
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (fromController(ev.source)) padUsed()
        return super.dispatchGenericMotionEvent(ev)
    }

    /**
     * HTTPS for the engine's online play (port/src/gs/net_match.c, through JNI on the engine's own thread): a POST of
     * [body] (JSON) to [url], the answer's text, or "" when there was none. Only the matchmaking service is asked this
     * way (a room code and the addresses the two games can reach each other at; no game data).
     */
    @Suppress("unused")
    fun netPost(url: String, body: String): String = netRequest(url, body, "")

    /**
     * The engine's requests to the matchmaking service (port/src/gs/net_match.c, through JNI on the engine's thread):
     * a POST of [body], or a GET when it is null; [headers] are extra "Name: value" lines (an official build signs
     * its requests with them). The answer's text, "" when there is none.
     */
    @Suppress("unused")
    fun netRequest(url: String, body: String?, headers: String?): String = runCatching {
        val c = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = if (body != null) "POST" else "GET"
            doOutput = body != null
            connectTimeout = 8_000
            readTimeout = 12_000
            if (body != null) setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "HiddenLeaf-Android")
            headers.orEmpty().lineSequence().forEach { line ->
                val k = line.substringBefore(':', "").trim()
                if (k.isNotEmpty()) setRequestProperty(k, line.substringAfter(':').trim())
            }
        }
        try {
            if (body != null) c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            // (in the log for a report about online play: the matchmaking's refusals, limits and outages)
            if (code !in 200..299) android.util.Log.w("bt3", "net: HTTP $code from ${url.substringAfterLast('/')}: ${text.take(200)}")
            text
        } finally {
            c.disconnect()
        }
    }.getOrElse {
        android.util.Log.w("bt3", "net: ${url.substringAfterLast('/')} failed: $it")
        ""
    }

    /**
     * "Quit to launcher" of the in-game window (port/src/gs/ui.cpp, through JNI on the engine's thread): the launcher
     * comes forward and the game's own process ends. A game cannot be started again in the same process (the engine
     * needs the fixed addresses only a fresh one has), so the process goes, as it does when the game is closed.
     */
    @Suppress("unused")
    fun quitToApp() {
        runOnUiThread {
            TouchBridge.setPad(0, 128, 128, 128, 128)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        // a moment for the launcher to be started, then this process goes (the engine's saves are closed as they are written)
        Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 450)
    }

    /** The list of where the game's data tables come from (made with the engine, android.py) next to the game's files. */
    private fun installDataList() {
        // the list of the game's data tables (libbt3.so.dat) and of the engine's own addresses (libbt3.so.rel, for
        // loading it elsewhere than its usual place), both made with the engine (android.py), next to the game's files
        // and of the game's own variables (libbt3.so.mem: what a saved state holds, for online play)
        for (name in listOf("libbt3.so.dat", "libbt3.so.rel", "libbt3.so.mem")) {
            val out = File(paths.root, name)
            runCatching {
                assets.open(name).use { input ->
                    val bytes = input.readBytes()
                    if (!out.exists() || out.length() != bytes.size.toLong() || !out.readBytes().contentEquals(bytes)) {
                        out.writeBytes(bytes)
                    }
                }
            }
        }
        // the files the engine reads beside itself (android.py: assets/engine): fonts/ and tbo/ui/, copied when the
        // app's version changed (or they are missing)
        val stamp = File(paths.root, ".engine-files")
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).lastUpdateTime.toString() }.getOrDefault("0")
        if (!stamp.exists() || stamp.readText() != version) {
            runCatching {
                copyAssetTree("engine", paths.root)
                stamp.writeText(version)
            }.onFailure { android.util.Log.w("bt3", "engine files: $it") }
        }
    }

    private fun copyAssetTree(path: String, into: File) {
        val names = assets.list(path).orEmpty()
        if (names.isEmpty()) { // a file
            val rel = path.substringAfter('/', "")
            if (rel.isEmpty()) return
            val out = File(into, rel)
            out.parentFile?.mkdirs()
            assets.open(path).use { input -> out.outputStream().use { input.copyTo(it) } }
            return
        }
        names.forEach { copyAssetTree("$path/$it", into) }
    }

    /**
     * The back key opens and closes the panel over the game, wherever it comes from. On handhelds such as the AYN Odin the
     * back button belongs to the built-in controller, and SDL would hand it to the game as that controller's
     * Back/Select button.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (pad?.editing == true) {
                if (event.action == KeyEvent.ACTION_UP) pad?.editing = false
                return true
            }
            if (event.action == KeyEvent.ACTION_UP) {
                if (panel != null) closePanel() else openPanel()
            }
            return true
        }
        if (KeyEvent.isGamepadButton(event.keyCode) || fromController(event.source)) padUsed()
        return super.dispatchKeyEvent(event)
    }

    /** PCSX2's GS (libbt4gsx.so) reads its shaders and fonts from files: the APK's assets/gsx/resources go to gsx/resources,
     *  again whenever the APK's copy is another version. Its Vulkan shaders compiled in advance go where it keeps its cache,
     *  when there is none yet: without them the first start spends some 25 s compiling them. */
    private fun copyGsxResources() {
        val names = assets.list("gsx") ?: return
        if ("version.txt" !in names) return
        val want = assets.open("gsx/version.txt").use { it.readBytes().decodeToString().trim() }
        val dst = File(paths.root, "gsx")
        val cache = File(dst, "ARMSX2/cache")
        val files = assets.list("gsx/cache").orEmpty()
        files.filter { it.endsWith(".bin") && it.removeSuffix(".bin") + ".idx" in files }.forEach { bin ->
            val idx = bin.removeSuffix(".bin") + ".idx"
            if (File(cache, bin).exists() || File(cache, idx).exists()) return@forEach
            cache.mkdirs()
            for (name in listOf(bin, idx)) {
                val tmp = File(cache, "$name.part")
                assets.open("gsx/cache/$name").use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                tmp.renameTo(File(cache, name))
            }
        }
        val mark = File(dst, "version.txt")
        if (mark.exists() && mark.readText().trim() == want) return
        File(dst, "resources").deleteRecursively()
        fun copy(from: String, to: File) {
            val kids = assets.list(from).orEmpty()
            if (kids.isEmpty()) {
                to.parentFile?.mkdirs()
                assets.open(from).use { i -> to.outputStream().use { o -> i.copyTo(o) } }
            } else {
                to.mkdirs()
                kids.forEach { copy("$from/$it", File(to, it)) }
            }
        }
        copy("gsx/resources", File(dst, "resources"))
        mark.writeText(want + "\n")
    }

    override fun getLibraries(): Array<String> = arrayOf("SDL3", EngineBridge.LIBRARY)

    /** The base ISO, opened here (the engine reads it through /proc/self/fd: a content:// address cannot be reopened
     *  by path from native code). Kept open for the life of the game's process. */
    private var iso: android.os.ParcelFileDescriptor? = null

    /**
     * The game folder, the app's native libraries (the engine, adrenotools' hooks), the chosen GPU driver (folder and
     * file, "" for the system's) and the base ISO as /proc/self/fd/N ("" if it cannot be opened).
     */
    override fun getArguments(): Array<String> {
        val driver = runCatching { GpuDrivers(this, contentResolver).selected() }.getOrNull()
        val isoArg = runCatching {
            val uri = android.net.Uri.parse(paths.isoUri.readText().trim())
            iso = contentResolver.openFileDescriptor(uri, "r")
            "/proc/self/fd/${iso!!.fd}"
        }.getOrDefault("")
        return arrayOf(paths.root.path, applicationInfo.nativeLibraryDir, driver?.dir?.path ?: "", driver?.library ?: "", isoArg)
    }
}
