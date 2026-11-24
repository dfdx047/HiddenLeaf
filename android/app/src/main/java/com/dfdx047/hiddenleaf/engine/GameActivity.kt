package com.dfdx047.hiddenleaf.engine

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import com.dfdx047.hiddenleaf.MainActivity
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.data.GamePaths
import com.dfdx047.hiddenleaf.data.Renderer
import com.dfdx047.hiddenleaf.data.GpuDrivers
import com.dfdx047.hiddenleaf.touch.PadMap
import com.dfdx047.hiddenleaf.touch.PadMapStore
import com.dfdx047.hiddenleaf.touch.PadScene
import com.dfdx047.hiddenleaf.touch.PadTranslator
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.touch.TouchListener
import com.dfdx047.hiddenleaf.touch.TouchSurface
import com.dfdx047.hiddenleaf.touch.createTouchSurface
import com.dfdx047.hiddenleaf.touch.view
import com.dfdx047.hiddenleaf.touch.TouchStore
import java.io.File
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLControllerManager
import org.libsdl.app.SDLSurface

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
    private lateinit var es: EngineSettings
    private lateinit var padStore: PadMapStore
    private var pad: TouchSurface? = null
    /** The physical controller's buttons (controller.json); a map of the player's is translated here ([translator]). */
    private var padMap = PadMap()
    private val translator = PadTranslator(PadMap())
    /** What this start of the game draws with, for the menu ("Vulkan", "OpenGL ES"). */
    private var rendererInUse = "Vulkan"
    /** An online match (the launcher's Online screen started this one): its line on screen and the room's relay. */
    private var online: OnlineGame? = null
    private val handler = Handler(Looper.getMainLooper())

    /** How many times "Naruto Online" was chosen in the game's Mode Select when this start began (hl_game.cpp's counter). */
    private var onlineChosen = -1
    private var lastDetectedCharId = 0

    private val watchMenu = object : Runnable {
        override fun run() {
            pad?.suppressed = menu?.isOpen == true || onlineMenu != null || graphicsMenu != null
            // "Naruto Online" in the game's own Mode Select (docs/menu-online.md; the developer, 2026-11-14: a menu of its own
            // inside the game, never out to the launcher): the online menu over the game (OnlineMenu.kt)
            if (online == null) {
                val n = runCatching { TouchBridge.nativeOnlineMenu() }.getOrDefault(-1)
                if (onlineChosen < 0) onlineChosen = n
                else if (n > onlineChosen) {
                    onlineChosen = n
                    openOnlineMenu()
                }
            }
            // Auto-detect Player 1's chosen character in battle, credit per-character mastery XP, and export real HUD portrait
            val cid = TouchBridge.p1CharId()
            if (cid > 0 && cid != lastDetectedCharId && TouchBridge.scene() == 2) {
                lastDetectedCharId = cid
                val charKey = com.dfdx047.hiddenleaf.data.OnlineAdvanced.charKeyForGameId(cid)
                com.dfdx047.hiddenleaf.data.OnlineAdvanced.recordLocalCharFight(this@GameActivity, charKey, xpGain = 60)
                scheduleAutoPortraitExport(charKey)
            }
            handler.postDelayed(this, 50)
        }
    }

    /** Captures Player 1's real in-game character portrait directly from the battle HUD into <root>/portraits/<charKey>.png. */
    private fun scheduleAutoPortraitExport(charKey: String) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val outFile = File(File(paths.root, "portraits"), "$charKey.png")
        if (outFile.exists() && outFile.length() > 1024L) return
        handler.postDelayed({
            runCatching {
                val layout = SDLActivity.getContentView() as? ViewGroup ?: return@runCatching
                val surf = (0 until layout.childCount).map { layout.getChildAt(it) }.firstOrNull { it is SDLSurface } as? SDLSurface ?: return@runCatching
                if (surf.width <= 0 || surf.height <= 0 || !surf.holder.surface.isValid) return@runCatching
                val full = android.graphics.Bitmap.createBitmap(surf.width, surf.height, android.graphics.Bitmap.Config.ARGB_8888)
                android.view.PixelCopy.request(surf, full, { res ->
                    if (res == android.view.PixelCopy.SUCCESS) {
                        runCatching {
                            // Top-left P1 HUD portrait box in UN5 battle screen (works in both 16:9 and 4:3/1:1)
                            val x = (full.width * 0.025f).toInt().coerceIn(0, full.width - 16)
                            val y = (full.height * 0.045f).toInt().coerceIn(0, full.height - 16)
                            val side = (minOf(full.width, full.height) * 0.145f).toInt().coerceIn(32, minOf(full.width - x, full.height - y))
                            val crop = android.graphics.Bitmap.createBitmap(full, x, y, side, side)
                            val scaled = android.graphics.Bitmap.createScaledBitmap(crop, 128, 128, true)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 92, it) }
                            scaled.recycle()
                            crop.recycle()
                            android.util.Log.i("un5", "portrait: auto-exported $charKey -> ${outFile.name}")
                        }
                    }
                    full.recycle()
                }, handler)
            }
        }, 1800L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        paths = GamePaths(this).also { it.ensure() }
        es = EngineSettings(paths.settings)
        padStore = PadMapStore(paths.root)
        padMap = padStore.load()
        translator.map = padMap
        // the renderer for this start (Vulkan, or OpenGL ES for Mali when automatic): written before the engine reads it
        runCatching {
            Renderer.apply(es, paths)
            rendererInUse = (if (es.get(EngineSettings.GPU_API, 0) == 1) "OpenGL ES" else "Vulkan") +
                (if (es.get(EngineSettings.NATIVE_RENDER, 0) == 1) " · " + getString(R.string.gm_renderer_native) else "") +
                if (es.get(EngineSettings.LIGHT_MODE, 1) == 1) " · " + getString(R.string.light_mode_short) else ""
            // the in-game menu's language: the launcher's (0 English, 1 Portuguese, 2 Spanish)
            val lang = when (resources.configuration.locales[0].language) { "pt" -> 1; "es" -> 2; else -> 0 }
            if (es.get(EngineSettings.LANG, -1) != lang) es.set(EngineSettings.LANG, lang)
            orientation = es.get(EngineSettings.ORIENTATION, 0)
            // (90 and 120 are offered on every screen now: above the panel's rate the extra pictures are not seen, but the
            // player can measure how far the device goes; GamePanel's hint says so)
        }
        requestedOrientation = orientationFlag()
        store = TouchStore(paths.root)
        installDataList()
        runCatching { copyGsxResources() }
        runCatching { com.dfdx047.hiddenleaf.data.ModSaves.prepare(paths) } // (a mod's card folder, before the engine reads the card)
        online = runCatching { OnlineGame.from(this, paths) }.getOrNull() // (before the engine reads and puts away the session file)
        super.onCreate(savedInstanceState)
        // the "Naruto Online" entry of the game's Mode Select: only outside a match and with the base game (online is base only)
        if (online == null && !com.dfdx047.hiddenleaf.data.Online.modsOn(paths)) runCatching { TouchBridge.nativeSetMenuOnline(true) }
        preferRefresh()
        addTouchPad()
        (SDLActivity.getContentView() as? ViewGroup)?.let { layout ->
            online?.attach(layout)
            if (online == null) {
                TrainingQueue.consume(paths)?.let { req ->
                    trainingQueue = TrainingQueueOverlay(this, paths, req).also { it.attach(layout) }
                }
            }
        }
    }

    /** 90 or 120 pictures a second: the screen's fastest mode at its current size (a 120 Hz panel left at 60 shows half). */
    private fun preferRefresh() {
        runCatching {
            val fps = EngineSettings(paths.settings).get(EngineSettings.FPS_MODE, 50)
            val display = windowManager.defaultDisplay
            val now = display.mode
            val best = display.supportedModes
                .filter { it.physicalWidth == now.physicalWidth && it.physicalHeight == now.physicalHeight }
                .maxByOrNull { if (fps > 60) it.refreshRate else -kotlin.math.abs(it.refreshRate - 60f) } ?: return
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
            android.util.Log.i("un5", "display: fps mode $fps, mode ${best.modeId} at ${best.refreshRate} Hz")
        }
    }

    /** The player's choice (launcher, Settings): 0 landscape, 1 portrait, 2 as the device is held. */
    private var orientation = 0

    // The activity handles orientation and size changes itself (configChanges in the manifest): a turn is not a restart.
    // SDL reports the new surface size, and the engine redoes the GS's surface and the picture's shape for it
    // (un5_main.cpp, SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED). Some handhelds' ROMs (the Retroid's) keep the screen in
    // landscape whatever is asked; the launcher's hint says so.
    private fun orientationFlag(): Int = when (orientation) {
        1 -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        2 -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER // (respects the rotation lock)
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
        val rate = TouchBridge.presentRate()
        val fps = if (rate > 0) getString(R.string.meter_interpolated, rate, picture.toInt()) else "%.0f fps (%.0f Hz)".format(picture, vsyncs)
        if (level == 1) return fps
        val worst = Regex("worst ([0-9.]+)").find(raw)?.groupValues?.get(1) ?: "?"
        val frame = "$fps  %.1f ms  max %s".format(1000f / vsyncs.coerceAtLeast(1f), worst)
        if (level == 2) return frame
        fun cat(n: String) = Regex("\\b$n ([0-9.]+)").find(raw)?.groupValues?.get(1) ?: "?"
        return "$frame\nEE ${cat("EE")}  VU1 ${cat("VU1")}  GS ${cat("GS")}  IOP ${cat("IOP")}  idle ${cat("idle")}"
    }

    // ---- the online menu inside the game (OnlineMenu.kt): over the game's Mode Select, the game frozen behind it
    private var onlineMenu: OnlineMenuHost? = null
    private var onlineLobby: com.dfdx047.hiddenleaf.data.OnlineLobby? = null

    private fun openOnlineMenu() {
        if (onlineMenu != null) return
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        releaseInputs()
        // (the developer, 2026-11-14: the Mode Select goes on behind the menu, Naruto and its music; only the buttons are held)
        TouchBridge.setPadHeld(true)
        val lobby = onlineLobby ?: com.dfdx047.hiddenleaf.data.OnlineLobby(application).also { onlineLobby = it }
        val fp = com.dfdx047.hiddenleaf.data.Online.fingerprint(this, paths)
        val host = OnlineMenuHost(this, lobby, fp, close = ::closeOnlineMenu, start = ::restartIntoMatch)
        host.installOwners(window.decorView, layout)
        layout.addView(host, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        onlineMenu = host
        pad?.suppressed = true
        host.post { host.focus() }
    }

    private fun closeOnlineMenu() {
        val host = onlineMenu ?: return
        onlineLobby?.cancel()
        (host.parent as? ViewGroup)?.removeView(host)
        onlineMenu = null
        TouchBridge.setPadHeld(false)
        focusGame()
    }

    /**
     * The other player is there and the session file is written: the game starts again in step with the other phone. This
     * process goes; OnlineRestartActivity (the launcher's process, nothing on screen) waits for it to be gone and opens the
     * game, whose loading cover (OnlineGame) hides the way to the character select.
     */
    private fun restartIntoMatch() {
        runOnUiThread {
            TouchBridge.releaseAll()
            startActivity(Intent(this, com.dfdx047.hiddenleaf.OnlineRestartActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 200)
        }
    }

    /** The controller in the online menu: the D-pad moves between the boards, ✕ (A) chooses, ○ / △ (B, Y) or Back goes back. */
    private fun onlineMenuKey(host: OnlineMenuHost, event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                return super.dispatchKeyEvent(KeyEvent(event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_DPAD_CENTER, event.repeatCount))
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (!down) host.back()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                host.focus()
                return super.dispatchKeyEvent(event)
            }
        }
        return true
    }

    private var onlineHat = 0

    /** The hat and the left stick move like the D-pad in the online menu. */
    private fun onlineMenuMotion(ev: MotionEvent): Boolean {
        val x = ev.getAxisValue(MotionEvent.AXIS_HAT_X).let { if (kotlin.math.abs(it) > 0.5f) it else ev.getAxisValue(MotionEvent.AXIS_X) }
        val y = ev.getAxisValue(MotionEvent.AXIS_HAT_Y).let { if (kotlin.math.abs(it) > 0.5f) it else ev.getAxisValue(MotionEvent.AXIS_Y) }
        val dir = when {
            y < -0.5f -> KeyEvent.KEYCODE_DPAD_UP
            y > 0.5f -> KeyEvent.KEYCODE_DPAD_DOWN
            x < -0.5f -> KeyEvent.KEYCODE_DPAD_LEFT
            x > 0.5f -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> 0
        }
        if (dir != onlineHat) {
            onlineHat = dir
            if (dir != 0) {
                onlineMenu?.focus()
                super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, dir))
                super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, dir))
            }
        }
        return true
    }

    // ---- the native-styled UN5 Graphics menu (GraphicsMenu.kt): live game behind it, wooden boards
    private var graphicsMenu: GraphicsMenuHost? = null

    private var trainingQueue: TrainingQueueOverlay? = null

    fun restartForOnlineMatch() {
        runCatching {
            startActivity(Intent(this, com.dfdx047.hiddenleaf.OnlineRestartActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun openGraphicsMenu() {
        closeMenu()
        if (graphicsMenu != null) return
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        releaseInputs()
        TouchBridge.setPadHeld(true)
        val host = GraphicsMenuHost(
            this, es.snapshot(), rendererInUse,
            onSetting = { k, v -> applySetting(k, v) },
            close = ::closeGraphicsMenu,
        )
        host.installOwners(window.decorView, layout)
        layout.addView(host, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        graphicsMenu = host
        pad?.suppressed = true
        host.post { host.focus() }
    }

    private fun closeGraphicsMenu() {
        val host = graphicsMenu ?: return
        (host.parent as? ViewGroup)?.removeView(host)
        graphicsMenu = null
        TouchBridge.setPadHeld(false)
        focusGame()
    }

    // ---- the game menu (GameMenu, a drawer at the left): it freezes the game while open (not an online match)
    private var menu: GameMenuHost? = null

    private val menuActions = object : GameMenuActions {
        override fun setting(key: String, value: Int) = applySetting(key, value)
        override fun touch(c: TouchConfig) = setTouch(c)
        override fun padMap(m: PadMap) = setPadMap(m)
        override fun editLayout() = startLayoutEdit()
        override fun openGraphicsMenu() = this@GameActivity.openGraphicsMenu()
        override fun resume() = closeMenu()
        override fun quit() = quitToApp()
    }

    private fun menuHost(): GameMenuHost? {
        menu?.let { return it }
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return null
        val state = GameMenuState(
            es.snapshot(), pad?.config ?: store.load(), padMap, online = online != null,
            maxRefresh = DisplayCaps.maxRefresh(this), rendererInUse = rendererInUse,
        )
        return GameMenuHost(this, state, menuActions).also {
            it.installOwners(window.decorView, layout)
            layout.addView(it, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            menu = it
        }
    }

    private fun openMenu(byPad: Boolean) {
        val m = menuHost() ?: return
        if (m.isOpen) return
        releaseInputs()
        // (an online match does not stop: the other phone's game would wait for this one; the menu is over a game going on)
        if (online == null) TouchBridge.setPaused(true)
        m.state.engine = es.snapshot()
        m.state.touch = pad?.config ?: store.load()
        m.state.padMap = padMap
        m.state.controllers = connectedControllers()
        m.state.helpScene = when (TouchBridge.scene()) {
            1 -> PadScene.MENU
            3 -> PadScene.CAMPAIGN
            else -> PadScene.FIGHT
        }
        m.state.look = MenuLook.of(this)
        menuDowns.clear()
        captureSwallow.clear()
        m.show(byPad)
    }

    private fun closeMenu() {
        val m = menu ?: return
        if (!m.isOpen) return
        if (pad?.editing == true) pad?.editing = false
        stopNav()
        m.hide()
        if (online == null) TouchBridge.setPaused(false)
        focusGame()
    }

    /** The keys go to SDL's surface again (the menu had them). */
    private fun focusGame() {
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        (0 until layout.childCount).map { layout.getChildAt(it) }.firstOrNull { it is SDLSurface }?.requestFocus()
    }

    /** The layout editor over the frozen game: the drawer steps aside until Done (or Back). */
    private fun startLayoutEdit() {
        val p = pad ?: return
        menu?.stepAside()
        p.editing = true
    }

    private fun endLayoutEdit() {
        val p = pad ?: return
        p.editing = false
        menu?.let {
            it.state.touch = p.config
            if (it.isOpen) it.comeBack()
        }
    }

    /** A choice of the menu: saved, and applied at once where the engine or this activity can. */
    private fun applySetting(key: String, value: Int) {
        es.set(key, value)
        when (key) {
            EngineSettings.SCALE4 -> {
                // (the GS takes a new resolution at the next start: hl_gsx.cpp applyVideoIfAsked)
                es.set(EngineSettings.SCALE, EngineSettings.wholeScale(value))
                applyVideo()
            }
            EngineSettings.FILTER, EngineSettings.ASPECT -> applyVideo()
            EngineSettings.FPS_MODE -> {
                applyVideo()
                preferRefresh()
            }
            EngineSettings.METER -> meterLevel = value
            EngineSettings.ORIENTATION -> {
                orientation = value.coerceIn(0, 2)
                requestedOrientation = orientationFlag()
            }
            EngineSettings.SPECIAL_ONE, EngineSettings.SPECIAL_BUTTON, EngineSettings.MASH_PERCENT ->
                TouchBridge.setAssist(
                    0,
                    es.get(EngineSettings.SPECIAL_BUTTON, 0x20),
                    es.get(EngineSettings.MASH_PERCENT, 100),
                )
            EngineSettings.RENDERER -> runCatching { Renderer.apply(es, paths) }
        }
        menu?.state?.engine = es.snapshot()
    }

    private fun applyVideo() {
        TouchBridge.setVideo(
            es.get(EngineSettings.SCALE4, 4), es.get(EngineSettings.FILTER, 0),
            es.get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT), es.get(EngineSettings.FPS_MODE, 50),
        )
    }

    /** The on-screen controls' settings; another layout (the game's or the classic) is another view, made here. */
    private fun setTouch(c: TouchConfig) {
        val old = pad
        if (old != null && old.config.layout != c.layout) {
            val parent = old.view.parent as? ViewGroup
            if (parent != null) {
                val at = parent.indexOfChild(old.view)
                parent.removeView(old.view)
                val v = createTouchSurface(this, c)
                v.listener = padListener
                v.suppressed = old.suppressed
                parent.addView(v.view, at, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                pad = v
            }
        } else {
            old?.config = c
        }
        store.save(c)
        menu?.state?.touch = c
    }

    private fun setPadMap(m: PadMap) {
        padMap = m
        padStore.save(m)
        translator.reset()
        translator.map = m
        TouchBridge.setPhysical(0, 128, 128, 128, 128)
        menu?.state?.padMap = m
    }

    private fun connectedControllers(): List<String> = runCatching {
        InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }
            .filter { !it.isVirtual && fromController(it.sources) }.map { it.name }.distinct()
    }.getOrDefault(emptyList())

    private val padListener = object : TouchListener {
        override fun onPad(buttons: Int, lx: Int, ly: Int, rx: Int, ry: Int) = TouchBridge.setPad(buttons, lx, ly, rx, ry)

        override fun onMenu() = openMenu(byPad = false)

        override fun onOptions() = openMenu(byPad = false)

        override fun onConfigChanged(config: TouchConfig) {
            store.save(config)
            menu?.state?.touch = config
        }

        override fun onEditDone() = endLayoutEdit()
    }

    private fun addTouchPad() {
        val layout = SDLActivity.getContentView() as? ViewGroup ?: return
        val view = createTouchSurface(this, store.load())
        view.listener = padListener
        layout.addView(view.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        pad = view
        meterLevel = es.get(EngineSettings.METER, 0)
        addPerfOverlay(layout)
    }

    override fun onResume() {
        super.onResume()
        pad?.let { it.config = store.load() }
        handler.post(watchMenu)
        runCatching { getSystemService(InputManager::class.java)?.registerInputDeviceListener(deviceListener, handler) }
        controllerGone() // (one may have been taken away while the game was in the background)
    }

    override fun onPause() {
        runCatching { getSystemService(InputManager::class.java)?.unregisterInputDeviceListener(deviceListener) }
        handler.removeCallbacks(watchMenu)
        stopNav()
        releaseInputs()
        super.onPause()
    }

    // ---- the physical controller

    /** Controller keys down now, for the menu's pair of buttons. */
    private val held = HashSet<Int>()
    /** The second button of the menu's pair: its release does not go to the game either. */
    private val comboSwallow = HashSet<Int>()
    /** Keys SDL was given pressed (key code to device), let go when the menu opens. */
    private val sdlDown = HashMap<Int, Int>()
    /** Keys pressed while the drawer was on screen: only a whole press there closes or goes back. */
    private val menuDowns = HashSet<Int>()
    /** A key just mapped in the Gamepad tab: its release is not a choice. */
    private val captureSwallow = HashSet<Int>()
    /** Devices whose D-pad sends keys: their hat (if they also report one) does not move the menu twice. */
    private val dpadKeyDevices = HashSet<Int>()

    /** Nothing held any more: the touch controls, the translated controller and the keys SDL was given. */
    private fun releaseInputs() {
        TouchBridge.releaseAll()
        translator.reset()
        for ((code, device) in sdlDown) runCatching { SDLControllerManager.onNativePadUp(device, code, 0) }
        sdlDown.clear()
    }

    private fun pushPhysical() = TouchBridge.setPhysical(translator.buttons(), translator.lx, translator.ly, translator.rx, translator.ry)

    /**
     * The developer (2026-11-14): a controller that goes away in the middle of a game (out of battery, Bluetooth gone, the
     * cable pulled) must bring the on-screen controls back at once, the player has nothing else to play with; online too,
     * where the other phone does not wait. Its buttons are let go (a button held when it went would stay held).
     */
    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = Unit

        override fun onInputDeviceRemoved(deviceId: Int) {
            dpadKeyDevices.remove(deviceId)
            val gone = sdlDown.filterValues { it == deviceId }.keys
            for (code in gone) runCatching { SDLControllerManager.onNativePadUp(deviceId, code, 0) }
            sdlDown.keys.removeAll(gone)
            controllerGone()
        }

        override fun onInputDeviceChanged(deviceId: Int) = controllerGone()
    }

    /** No controller left: what it held is let go and the on-screen controls come back. */
    private fun controllerGone() {
        if (connectedControllers().isNotEmpty()) return
        translator.reset()
        pushPhysical()
        held.clear()
        val v = pad ?: return
        if (v.hiddenForPad) {
            v.hiddenForPad = false
            v.view.invalidate()
        }
    }

    /** A physical controller in use: the on-screen one hides (automatic mode) until the screen is touched again. */
    private fun padUsed() {
        val v = pad ?: return
        if (v.config.mode == TouchMode.AUTO && !v.editing) v.hiddenForPad = true
    }

    private fun fromController(source: Int): Boolean =
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    private fun isPadKey(ev: KeyEvent): Boolean =
        KeyEvent.isGamepadButton(ev.keyCode) || fromController(ev.source) ||
            ev.keyCode in DPAD_KEYS && ev.device?.let { fromController(it.sources) } == true

    /** A real push, not the noise of a resting stick, which would hide the on-screen controller by itself on handhelds. */
    private fun axisPushed(ev: MotionEvent): Boolean = PAD_AXES.any { kotlin.math.abs(ev.getAxisValue(it)) > 0.5f }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (fromController(ev.source) && ev.actionMasked == MotionEvent.ACTION_MOVE) {
            if (onlineMenu != null) return onlineMenuMotion(ev)
            val m = menu
            if (m != null && m.isShowing) return menuMotion(m, ev)
            if (pad?.editing == true) return true
            if (axisPushed(ev)) padUsed()
            if (padMap.custom) {
                val twoSticks = ev.device?.getMotionRange(MotionEvent.AXIS_Z, ev.source) != null
                translator.axes(
                    ev.getAxisValue(MotionEvent.AXIS_HAT_X), ev.getAxisValue(MotionEvent.AXIS_HAT_Y),
                    maxOf(ev.getAxisValue(MotionEvent.AXIS_LTRIGGER), ev.getAxisValue(MotionEvent.AXIS_BRAKE)),
                    maxOf(ev.getAxisValue(MotionEvent.AXIS_RTRIGGER), ev.getAxisValue(MotionEvent.AXIS_GAS)),
                    ev.getAxisValue(MotionEvent.AXIS_X), ev.getAxisValue(MotionEvent.AXIS_Y),
                    ev.getAxisValue(if (twoSticks) MotionEvent.AXIS_Z else MotionEvent.AXIS_RX),
                    ev.getAxisValue(if (twoSticks) MotionEvent.AXIS_RZ else MotionEvent.AXIS_RY),
                )
                pushPhysical()
                return true
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    // ---- the controller in the menu: the stick and the hat move the focus as the D-pad does, with a repeat when held

    private var navDir = 0
    private val navRepeat = object : Runnable {
        override fun run() {
            if (navDir == 0) return
            navStep(navDir)
            handler.postDelayed(this, 110)
        }
    }

    private fun stopNav() {
        navDir = 0
        handler.removeCallbacks(navRepeat)
    }

    private fun navStep(code: Int) {
        menu?.ensureFocus()
        val t = SystemClock.uptimeMillis()
        deliver(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        deliver(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }

    private fun menuMotion(m: GameMenuHost, ev: MotionEvent): Boolean {
        val st = m.state
        st.padNav = true
        val useHat = ev.deviceId !in dpadKeyDevices
        val hx = if (useHat) ev.getAxisValue(MotionEvent.AXIS_HAT_X) else 0f
        val hy = if (useHat) ev.getAxisValue(MotionEvent.AXIS_HAT_Y) else 0f
        val bit = st.capture
        if (bit != null) {
            // a trigger that reports only as an axis, or a hat, can be mapped too
            val code = when {
                maxOf(ev.getAxisValue(MotionEvent.AXIS_LTRIGGER), ev.getAxisValue(MotionEvent.AXIS_BRAKE)) > 0.6f -> KeyEvent.KEYCODE_BUTTON_L2
                maxOf(ev.getAxisValue(MotionEvent.AXIS_RTRIGGER), ev.getAxisValue(MotionEvent.AXIS_GAS)) > 0.6f -> KeyEvent.KEYCODE_BUTTON_R2
                hy < -0.5f -> KeyEvent.KEYCODE_DPAD_UP
                hy > 0.5f -> KeyEvent.KEYCODE_DPAD_DOWN
                hx < -0.5f -> KeyEvent.KEYCODE_DPAD_LEFT
                hx > 0.5f -> KeyEvent.KEYCODE_DPAD_RIGHT
                else -> 0
            }
            if (code != 0) {
                setPadMap(st.padMap.bind(bit, code))
                st.capture = null
            }
            return true
        }
        val x = ev.getAxisValue(MotionEvent.AXIS_X)
        val y = ev.getAxisValue(MotionEvent.AXIS_Y)
        val dir = when {
            hy < -0.5f || y < -0.6f -> KeyEvent.KEYCODE_DPAD_UP
            hy > 0.5f || y > 0.6f -> KeyEvent.KEYCODE_DPAD_DOWN
            hx < -0.5f || x < -0.6f -> KeyEvent.KEYCODE_DPAD_LEFT
            hx > 0.5f || x > 0.6f -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> 0
        }
        if (dir != navDir) {
            stopNav()
            navDir = dir
            if (dir != 0) {
                navStep(dir)
                handler.postDelayed(navRepeat, 380)
            }
        }
        return true
    }

    /** A key to the view that has the focus (the menu's page), past this activity's own handling. */
    private fun deliver(ev: KeyEvent): Boolean = super.dispatchKeyEvent(ev)

    /**
     * The controller's buttons in the menu: the D-pad moves the focus, A chooses (as the D-pad's centre), B and Back go
     * back (the capture, a page over the menu, then the menu), L1 / R1 change the tab, START goes back to the game. A
     * button waited for by the Gamepad tab's capture is mapped instead.
     */
    private fun menuKey(m: GameMenuHost, ev: KeyEvent, fromPad: Boolean): Boolean {
        val st = m.state
        val code = ev.keyCode
        val down = ev.action == KeyEvent.ACTION_DOWN
        if (fromPad || code in DPAD_KEYS) st.padNav = true
        val bit = st.capture
        if (bit != null) {
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
                if (!down) st.capture = null
                return true
            }
            if (down && ev.repeatCount == 0 && PadMap.bindable(code)) {
                setPadMap(st.padMap.bind(bit, code))
                st.capture = null
                captureSwallow += code
            }
            return true
        }
        if (code in captureSwallow) {
            if (!down) captureSwallow -= code
            return true
        }
        if (down && ev.repeatCount == 0) menuDowns += code
        val pressed = !down && menuDowns.remove(code)
        when (code) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_ESCAPE -> {
                if (pressed && !st.back()) closeMenu()
                return true
            }
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_MODE -> {
                if (pressed && st.overlay == null) closeMenu()
                return true
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (down && ev.repeatCount == 0) st.nextTab(-1)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (down && ev.repeatCount == 0) st.nextTab(1)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_A -> {
                m.ensureFocus()
                deliver(
                    KeyEvent(
                        ev.downTime, ev.eventTime, ev.action, KeyEvent.KEYCODE_DPAD_CENTER, ev.repeatCount, ev.metaState,
                        ev.deviceId, ev.scanCode, ev.flags, ev.source,
                    ),
                )
                return true
            }
        }
        if (KeyEvent.isGamepadButton(code)) return true // (the controller's other buttons do nothing here)
        if (code in DPAD_KEYS) {
            // (always taken: a move past the page's last control must not carry the focus out to the game's surface)
            m.ensureFocus()
            deliver(ev)
            return true
        }
        return deliver(ev)
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
    fun quitToApp(page: String? = null) {
        trainingQueue?.stop()
        online?.leave() // (the other phone is told before this process goes)
        runOnUiThread {
            TouchBridge.releaseAll()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .apply { if (page != null) putExtra(MainActivity.EXTRA_PAGE, page) },
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
     * The controller and the keys. Back (wherever it comes from: on handhelds such as the AYN Odin it belongs to the
     * built-in controller, and SDL would hand it to the game as the Back/Select button), the Guide key and the menu's
     * pair (SELECT + START by default) open the menu; in the menu the keys are its own ([menuKey]). Otherwise the
     * controller goes to SDL as it is, or through [translator] when the player mapped its buttons.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val down = event.action == KeyEvent.ACTION_DOWN
        val fromPad = isPadKey(event)
        if (fromPad) {
            if (down) held += code else held -= code
            if (code in DPAD_KEYS) dpadKeyDevices += event.deviceId
        }
        // (the second button of the menu's pair: neither its press nor its release reaches the game)
        if (!down && comboSwallow.remove(code)) return true
        if (pad?.editing == true) {
            // the layout editor over the frozen game: Back or B is its Done
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_ESCAPE) {
                if (!down) endLayoutEdit()
                return true
            }
            return if (fromPad) true else super.dispatchKeyEvent(event)
        }
        onlineMenu?.let { return onlineMenuKey(it, event) }
        graphicsMenu?.let { host ->
            val down = event.action == KeyEvent.ACTION_DOWN
            when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                    return super.dispatchKeyEvent(KeyEvent(event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_DPAD_CENTER, event.repeatCount))
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (!down) host.back()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    host.focus()
                    return super.dispatchKeyEvent(event)
                }
            }
            return true
        }
        val m = menu
        if (m != null && m.isShowing) return menuKey(m, event, fromPad)
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (!down) openMenu(byPad = fromPad || code == KeyEvent.KEYCODE_BUTTON_MODE)
            return true
        }
        if (fromPad) {
            // Strict Touch-Only online fairness: ignore external controller input during a touch-only match.
            if (online?.touchOnly == true) return true
            padUsed()
            val combo = padMap.menuCombo.keys
            if (down && event.repeatCount == 0 && code in combo && held.containsAll(combo)) {
                comboSwallow += code
                openMenu(byPad = true)
                return true
            }
            if (padMap.custom && PadMap.bindable(code)) {
                translator.key(code, down)
                pushPhysical()
                return true
            }
            if (down) sdlDown[code] = event.deviceId else sdlDown.remove(code)
        }
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

    override fun getLibraries(): Array<String> =
        arrayOf("SDL3", EngineBridge.libraryFor(this, com.dfdx047.hiddenleaf.data.GamePaths(this).activeMods))

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

private val DPAD_KEYS = setOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)

private val PAD_AXES = intArrayOf(
    MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
    MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER,
)
