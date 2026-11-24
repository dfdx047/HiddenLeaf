package com.dfdx047.hiddenleaf.engine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.data.Renderer
import com.dfdx047.hiddenleaf.touch.Macro
import com.dfdx047.hiddenleaf.touch.MacroStep
import com.dfdx047.hiddenleaf.touch.MenuCombo
import com.dfdx047.hiddenleaf.touch.PadDefaults
import com.dfdx047.hiddenleaf.touch.PadMap
import com.dfdx047.hiddenleaf.touch.PadScene
import com.dfdx047.hiddenleaf.touch.PadSkin
import com.dfdx047.hiddenleaf.touch.PadStyle
import com.dfdx047.hiddenleaf.touch.ProTouch
import com.dfdx047.hiddenleaf.touch.Ps2
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchLayout
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.ui.screens.MacroEditorContent
import com.dfdx047.hiddenleaf.ui.screens.PadIcon
import com.dfdx047.hiddenleaf.ui.screens.ProGuideContent
import com.dfdx047.hiddenleaf.ui.screens.guideItems
import com.dfdx047.hiddenleaf.ui.screens.macroSummary
import com.dfdx047.hiddenleaf.ui.screens.rememberPadPainter
import com.dfdx047.hiddenleaf.ui.theme.BrushFont
import kotlinx.coroutines.delay

/*
 * The game menu: a drawer at the left of the frozen game (a sheet from the bottom when the screen is upright), in tabs:
 * Game, Graphics, Gamepad, Touch and Help. Everything the game can change while it runs, saved as it is chosen; what
 * only counts from the next start is marked so. Made for the fingers and for a controller (GameActivity turns the
 * controller's buttons into the focus moves of this page: the D-pad and the stick move, A chooses, B goes back, L1 / R1
 * change the tab).
 *
 * At the left, the start edge, where Android's navigation drawers open (and the in-game menus of the PS2 emulators on
 * Android): the rest of the frozen picture stays in view at the right. The layout editor hides the drawer altogether.
 */

/** What the menu shows, kept apart from the activity so a test can draw it. */
class GameMenuState(
    engine: Map<String, Int> = emptyMap(),
    touch: TouchConfig = TouchConfig(),
    padMap: PadMap = PadMap(),
    /** An online match runs: the game does not stop while the menu is open. */
    val online: Boolean = false,
    /** The screen's fastest refresh rate (Hz). */
    val maxRefresh: Float = 60f,
    /** The renderer this start of the game draws with ("Vulkan", "OpenGL ES"...). */
    val rendererInUse: String = "Vulkan",
) {
    var look by mutableStateOf(MenuLook())
    var open by mutableStateOf(false)
    var tab by mutableIntStateOf(0)
    var engine by mutableStateOf(engine)
    var touch by mutableStateOf(touch)
    var padMap by mutableStateOf(padMap)
    /** The PS2 button waiting for a press of the controller (the Gamepad tab's capture), or null. */
    var capture by mutableStateOf<Int?>(null)
    var overlay by mutableStateOf<MenuOverlay?>(null)
    var confirmQuit by mutableStateOf(false)
    var controllers by mutableStateOf<List<String>>(emptyList())
    var helpScene by mutableStateOf(PadScene.FIGHT)
    /** A controller is driving the menu: the focused control is ringed. */
    var padNav by mutableStateOf(false)
    /** Bumped to give the focus to the selected tab (the menu opened, L1 / R1). */
    var focusTick by mutableIntStateOf(0)
    /** Where each tab's page was left: the menu opens again where the player was. */
    val scrolls = List(TAB_COUNT) { ScrollState(0) }

    fun get(key: String, default: Int) = engine[key] ?: default

    /** B or Back: the capture, a page over the menu or the question to quit go first. False: nothing was open. */
    fun back(): Boolean {
        when {
            capture != null -> capture = null
            overlay != null -> overlay = null
            confirmQuit -> confirmQuit = false
            else -> return false
        }
        return true
    }

    fun nextTab(step: Int) {
        if (overlay != null) return
        capture = null
        confirmQuit = false
        tab = (tab + step + TAB_COUNT) % TAB_COUNT
        focusTick++
    }

    companion object {
        const val TAB_GAME = 0
        const val TAB_GRAPHICS = 1
        const val TAB_PAD = 2
        const val TAB_TOUCH = 3
        const val TAB_HELP = 4
        const val TAB_COUNT = 5
    }
}

sealed interface MenuOverlay {
    data class EditMacro(val macro: Macro) : MenuOverlay
    data object ProGuide : MenuOverlay
}

/** What the menu's choices do (GameActivity: saved, and applied at once where the engine can). */
interface GameMenuActions {
    fun setting(key: String, value: Int)
    fun touch(c: TouchConfig)
    fun padMap(m: PadMap)
    fun editLayout()
    fun openGraphicsMenu() {}
    fun resume()
    fun quit()
}

private val LocalPadNav = compositionLocalOf { false }

@Composable
fun GameMenu(state: GameMenuState, actions: GameMenuActions) {
    MenuTheme(state.look) {
        CompositionLocalProvider(LocalPadNav provides state.padNav) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val portrait = maxHeight > maxWidth * 1.05f
                AnimatedVisibility(state.open, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
                    // the game stays in view, a little darker; a touch on it goes back to the game
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f))
                            .pointerInput(Unit) { detectTapGestures { actions.resume() } },
                    )
                }
                val place = if (portrait) {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.68f)
                } else {
                    Modifier.align(Alignment.CenterStart).width(drawerWidth(maxWidth)).fillMaxHeight()
                }
                AnimatedVisibility(
                    state.open,
                    modifier = place,
                    enter = if (portrait) slideInVertically(tween(200)) { it } else slideInHorizontally(tween(200)) { -it },
                    exit = if (portrait) slideOutVertically(tween(170)) { it } else slideOutHorizontally(tween(170)) { -it },
                ) {
                    Drawer(state, actions, portrait, wide = portrait || drawerWidth(maxWidth) >= 330.dp)
                }
                state.overlay?.let { o ->
                    Box(Modifier.fillMaxSize()) {
                        when (o) {
                            is MenuOverlay.ProGuide -> ProGuideContent(state.touch, onDismiss = { state.overlay = null })
                            is MenuOverlay.EditMacro -> {
                                val c = state.touch
                                val index = c.macros.indexOfFirst { it.id == o.macro.id }.let { if (it < 0) c.macros.size else it }
                                MacroEditorContent(c, o.macro, index, onDismiss = { state.overlay = null }) { saved, place ->
                                    val list = if (c.macros.any { it.id == saved.id }) c.macros.map { if (it.id == saved.id) saved else it } else c.macros + saved
                                    actions.touch(c.copy(macros = list))
                                    state.overlay = null
                                    if (place) actions.editLayout()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** About 40% of a landscape screen: wide enough for the tabs and two chips a row, never all of a narrow one. */
internal fun drawerWidth(screen: Dp): Dp = (screen * 0.4f).coerceIn(340.dp, 460.dp).coerceAtMost(screen * 0.82f)

@Composable
private fun Drawer(state: GameMenuState, actions: GameMenuActions, portrait: Boolean, wide: Boolean) {
    val extras = LocalMenuExtras.current
    val shape = if (portrait) CutCornerShape(topStart = 18.dp, topEnd = 18.dp) else CutCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, extras.bandStart),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        val cutout = WindowInsets.displayCutout.only(if (portrait) WindowInsetsSides.Bottom else WindowInsetsSides.Start + WindowInsetsSides.Vertical)
        Column(Modifier.fillMaxSize().windowInsetsPadding(cutout)) {
            Header(state, actions)
            Tabs(state, wide)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            key(state.tab) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(state.scrolls[state.tab]).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (state.tab) {
                        GameMenuState.TAB_GAME -> GameTab(state, actions)
                        GameMenuState.TAB_GRAPHICS -> GraphicsTab(state, actions)
                        GameMenuState.TAB_PAD -> PadTab(state, actions)
                        GameMenuState.TAB_TOUCH -> TouchTab(state, actions)
                        else -> HelpTab(state)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(state: GameMenuState, actions: GameMenuActions) {
    val extras = LocalMenuExtras.current
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(Brush.horizontalGradient(listOf(extras.bandStart, extras.bandEnd)))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("HIDDEN LEAF", fontFamily = BrushFont, fontSize = 17.sp, color = extras.onBand, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(if (state.online) R.string.gm_online_running else R.string.panel_title),
            style = MaterialTheme.typography.labelMedium, color = extras.onBand.copy(alpha = 0.85f),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        val ring = CircleShape
        Box(
            Modifier.size(40.dp).clip(ring).focusRing(ring, extras.onBand).clickable(role = Role.Button) { actions.resume() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = extras.onBand)
        }
    }
}

private data class TabInfo(val label: Int, val icon: ImageVector)

private val TABS = listOf(
    TabInfo(R.string.gm_tab_game, Icons.Rounded.SportsEsports),
    TabInfo(R.string.gp_tab_graphics, Icons.Rounded.Tv),
    TabInfo(R.string.gm_tab_pad, Icons.Rounded.VideogameAsset),
    TabInfo(R.string.gm_tab_touch, Icons.Rounded.TouchApp),
    TabInfo(R.string.gm_tab_help, Icons.AutoMirrored.Rounded.HelpOutline),
)

@Composable
private fun Tabs(state: GameMenuState, wide: Boolean) {
    val requesters = remember { List(TABS.size) { FocusRequester() } }
    LaunchedEffect(state.focusTick, state.open) {
        if (state.open && state.overlay == null) {
            delay(30)
            runCatching { requesters[state.tab].requestFocus() }
        }
    }
    // (the L1 / R1 marks only where the tabs keep room for their names: not in a square screen's narrow drawer)
    val shoulders = wide && (state.padNav || state.controllers.isNotEmpty())
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shoulders) KeyHint("L1")
        TABS.forEachIndexed { i, t ->
            val on = state.tab == i
            val shape = CutCornerShape(6.dp)
            Column(
                Modifier.weight(1f).padding(horizontal = 2.dp).clip(shape)
                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .focusRing(shape).focusRequester(requesters[i])
                    .selectable(on, role = Role.Tab) {
                        state.capture = null
                        state.confirmQuit = false
                        state.tab = i
                    }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(t.icon, null, tint = color, modifier = Modifier.size(20.dp))
                FitText(stringResource(t.label), color, bold = on)
            }
        }
        if (shoulders) KeyHint("R1")
    }
}

/** One line that shrinks its letters until it fits (a tab's name in a narrow drawer, in any language). */
@Composable
private fun FitText(text: String, color: Color, bold: Boolean) {
    var size by remember(text) { mutableStateOf(11.5f) }
    Text(
        text, color = color, fontSize = size.sp, maxLines = 1, softWrap = false,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        style = MaterialTheme.typography.labelSmall,
        onTextLayout = { if (it.hasVisualOverflow && size > 8.5f) size -= 0.5f },
    )
}

@Composable
private fun KeyHint(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 2.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

// ---- Game: continue, the assists, the screen, quit

@Composable
private fun GameTab(state: GameMenuState, actions: GameMenuActions) {
    MenuButton(stringResource(R.string.panel_resume), Icons.Rounded.PlayArrow, primary = true) { actions.resume() }
    if (state.online) Hint(stringResource(R.string.gm_online_running_text))

    Section(stringResource(R.string.gp_assists))
    Label(stringResource(R.string.mash_multiplier))
    Chips(
        listOf("1x" to 100, "1,25x" to 125, "1,5x" to 150, "1,65x" to 165, "2x" to 200),
        state.get(EngineSettings.MASH_PERCENT, 100),
    ) { actions.setting(EngineSettings.MASH_PERCENT, it) }
    Hint(stringResource(R.string.mash_multiplier_text))
    if (state.online) Hint(stringResource(R.string.gm_assists_online))

    Section(stringResource(R.string.gm_screen))
    Label(stringResource(R.string.orientation))
    Chips(
        listOf(
            stringResource(R.string.orientation_landscape) to 0, stringResource(R.string.orientation_portrait) to 1,
            stringResource(R.string.orientation_sensor) to 2,
        ),
        state.get(EngineSettings.ORIENTATION, 0).coerceIn(0, 2),
    ) { actions.setting(EngineSettings.ORIENTATION, it) }

    Section(stringResource(R.string.gm_leave))
    if (!state.confirmQuit) {
        MenuButton(stringResource(R.string.panel_quit), Icons.AutoMirrored.Rounded.ExitToApp) { state.confirmQuit = true }
    } else {
        val stay = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { stay.requestFocus() } }
        Text(stringResource(R.string.gm_quit_q), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton(stringResource(R.string.cancel), null, modifier = Modifier.weight(1f).focusRequester(stay)) { state.confirmQuit = false }
            MenuButton(stringResource(R.string.gm_quit_yes), Icons.AutoMirrored.Rounded.ExitToApp, danger = true, modifier = Modifier.weight(1f)) {
                actions.quit()
            }
        }
    }
    Hint(stringResource(R.string.panel_save_hint))
}

// ---- Graphics

/** A resolution in quarters as the launcher writes it: "0.75x", "1x", "2.5x". */
internal fun scaleLabel(q: Int): String = if (q % 4 == 0) "${q / 4}x" else "${q / 4.0}x".replace(".0x", "x")

@Composable
private fun GraphicsTab(state: GameMenuState, actions: GameMenuActions) {
    MenuButton(stringResource(R.string.un5_gfx_open), Icons.Rounded.Tune, primary = true) { actions.openGraphicsMenu() }
    Section(stringResource(R.string.renderer))
    Text(
        stringResource(R.string.gm_renderer_now, state.rendererInUse),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
    )
    Label(stringResource(R.string.renderer), restart = true)
    Chips(
        listOf("Vulkan" to Renderer.VULKAN, "OpenGL ES" to Renderer.OPENGL),
        if (state.get(EngineSettings.RENDERER, Renderer.AUTO) == Renderer.OPENGL) Renderer.OPENGL else Renderer.VULKAN,
    ) { actions.setting(EngineSettings.RENDERER, it) }
    Toggle(
        stringResource(R.string.native_render), stringResource(R.string.native_render_text),
        state.get(EngineSettings.NATIVE_RENDER, 0) == 1, restart = true,
    ) { actions.setting(EngineSettings.NATIVE_RENDER, if (it) 1 else 0) }
    Toggle(
        stringResource(R.string.light_mode), stringResource(R.string.light_mode_text),
        state.get(EngineSettings.LIGHT_MODE, 0) == 1, restart = true,
    ) { actions.setting(EngineSettings.LIGHT_MODE, if (it) 1 else 0) }

    val q = state.get(EngineSettings.SCALE4, 0).takeIf { it > 0 } ?: (state.get(EngineSettings.SCALE, EngineSettings.SCALE_DEFAULT) * 4)
    Section(stringResource(R.string.resolution), restart = true)
    Chips(EngineSettings.SCALE_STEPS.map { scaleLabel(it) to it }, q) { actions.setting(EngineSettings.SCALE4, it) }
    Hint(stringResource(R.string.gm_resolution_px, "${128 * q}×${112 * q}") + " " + stringResource(R.string.gm_restart_hint))

    Section(stringResource(R.string.screen_filter))
    Chips(
        listOf(stringResource(R.string.filter_bilinear) to 0, stringResource(R.string.filter_sharp) to 1, "FXAA" to 2, "CAS" to 3),
        state.get(EngineSettings.FILTER, 0),
    ) { actions.setting(EngineSettings.FILTER, it) }
    Hint(stringResource(R.string.screen_filter_hint))

    Section(stringResource(R.string.aspect))
    Chips(
        listOf(
            "4:3" to 1333, stringResource(R.string.aspect_wide) to 1778, stringResource(R.string.aspect_ultrawide) to 2222,
            stringResource(R.string.aspect_screen) to 0,
        ),
        state.get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT),
    ) { actions.setting(EngineSettings.ASPECT, it) }
    Hint(stringResource(R.string.aspect_hint))

    Section(stringResource(R.string.panel_fps_limit))
    Chips(
        listOf("30" to 30, stringResource(R.string.panel_fps_50) to 50),
        if (state.get(EngineSettings.FPS_MODE, 50) == 30) 30 else 50,
    ) { actions.setting(EngineSettings.FPS_MODE, it) }

    Section("Frame Generation (ZeroFG · Offline)")
    Chips(
        listOf("Desligado" to 0, "2x" to 2, "3x" to 3, "4x" to 4),
        state.get(EngineSettings.ZERO_FG, 0).let { if (it in listOf(0, 2, 3, 4)) it else 0 },
    ) { actions.setting(EngineSettings.ZERO_FG, it) }
    Hint("Geração real de quadros via Vulkan 1.3 (tela até ${state.maxRefresh} Hz). Desligado automaticamente no Online.")

    Section(stringResource(R.string.gp_counter))
    Chips(
        listOf(
            stringResource(R.string.off) to 0, "FPS" to 1, stringResource(R.string.panel_counter_ms) to 2,
            stringResource(R.string.panel_counter_full) to 3,
        ),
        state.get(EngineSettings.METER, 0),
    ) { actions.setting(EngineSettings.METER, it) }
    Hint(stringResource(R.string.gm_live_hint))
}

// ---- Gamepad: the hotkey and the buttons

private fun buttonName(bit: Int): Int = when (bit) {
    Ps2.CIRCLE -> R.string.gm_b_attack
    Ps2.CROSS -> R.string.gm_b_jump
    Ps2.TRIANGLE -> R.string.gm_b_chakra
    Ps2.SQUARE -> R.string.gm_b_item
    Ps2.R2, Ps2.L2 -> R.string.gm_b_guard
    Ps2.L1 -> R.string.gm_b_prev
    Ps2.R1 -> R.string.gm_b_next
    Ps2.R3 -> R.string.gm_b_awaken
    Ps2.L3 -> R.string.gm_b_l3
    Ps2.START -> R.string.gm_b_start
    Ps2.SELECT -> R.string.gm_b_select
    Ps2.UP -> R.string.gm_b_up
    Ps2.DOWN -> R.string.gm_b_down
    Ps2.LEFT -> R.string.gm_b_left
    else -> R.string.gm_b_right
}

/** The PS2 button's colour in its coin (the PlayStation's own for the four shapes). */
private fun coinColor(bit: Int): Color = when (bit) {
    Ps2.CIRCLE -> Color(0xFFD8443A)
    Ps2.CROSS -> Color(0xFF5A82C8)
    Ps2.TRIANGLE -> Color(0xFF3FA37A)
    Ps2.SQUARE -> Color(0xFFD06AA8)
    else -> Paper.ink
}

@Composable
private fun PadTab(state: GameMenuState, actions: GameMenuActions) {
    val map = state.padMap
    Section(stringResource(R.string.gm_pads))
    Text(
        if (state.controllers.isEmpty()) stringResource(R.string.gm_pads_none) else stringResource(R.string.gm_pads_found, state.controllers.joinToString(", ")),
        style = MaterialTheme.typography.bodyMedium,
    )

    Section(stringResource(R.string.gm_hotkeys))
    Label(stringResource(R.string.gm_menu_key))
    Chips(
        listOf("SELECT + START" to MenuCombo.SELECT_START.ordinal, "L3 + R3" to MenuCombo.L3_R3.ordinal, stringResource(R.string.gm_menu_back_only) to MenuCombo.OFF.ordinal),
        map.menuCombo.ordinal,
    ) { actions.padMap(map.copy(menuCombo = MenuCombo.entries[it])) }
    Hint(stringResource(R.string.gm_menu_key_hint))

    Section(stringResource(R.string.gm_map))
    Hint(stringResource(if (map.custom) R.string.gm_map_custom else R.string.gm_map_default))
    LaunchedEffect(state.capture) {
        if (state.capture != null) {
            delay(8000)
            state.capture = null
        }
    }
    PadMap.BUTTONS.forEach { bit ->
        MapRow(bit, map.keysFor(bit), capturing = state.capture == bit) {
            state.capture = if (state.capture == bit) null else bit
        }
    }
    Hint(stringResource(R.string.gm_map_hint))
    MenuButton(stringResource(R.string.gm_map_reset), Icons.Rounded.RestartAlt, enabled = map.custom) {
        state.capture = null
        actions.padMap(PadMap(menuCombo = map.menuCombo))
    }
    Hint(stringResource(R.string.gm_sticks_hint))
}

@Composable
private fun MapRow(bit: Int, keys: List<Int>, capturing: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (capturing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .focusRing(shape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Coin(Ps2.label(PadSkin.PLAYSTATION, bit), coinColor(bit))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(buttonName(bit)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 2)
        Spacer(Modifier.width(8.dp))
        val text = when {
            capturing -> stringResource(R.string.gm_map_press)
            keys.isEmpty() -> "—"
            else -> keys.joinToString(" / ") { PadMap.keyLabel(it) }
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (capturing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 56.dp)
                .background(if (capturing) Color.Transparent else MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Coin(symbol: String, color: Color) {
    Box(
        Modifier.size(width = 46.dp, height = 26.dp).background(color, RoundedCornerShape(13.dp))
            .border(1.5.dp, Color(0xFF1A0E06), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol, color = if (color == Paper.ink) Paper.paper else Color.White,
            fontSize = if (symbol.length > 2) 9.5.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

// ---- Touch: the on-screen controls

@Composable
private fun TouchTab(state: GameMenuState, actions: GameMenuActions) {
    val c = state.touch
    val mobile = c.layout == TouchLayout.MOBILE
    fun set(n: TouchConfig) = actions.touch(n)

    MenuButton(stringResource(R.string.touch_edit_layout), Icons.Rounded.OpenWith, primary = true) { actions.editLayout() }
    Hint(stringResource(R.string.gm_edit_hint))

    Section(stringResource(R.string.touch_controls))
    Chips(
        listOf(stringResource(R.string.touch_off) to 0, stringResource(R.string.touch_auto) to 1, stringResource(R.string.touch_always) to 2),
        c.mode.ordinal,
    ) { set(c.copy(mode = TouchMode.entries[it])) }
    Hint(stringResource(R.string.touch_hint))
    Label(stringResource(R.string.touch_layout))
    Chips(
        listOf(stringResource(R.string.touch_layout_mobile) to TouchLayout.MOBILE.ordinal, stringResource(R.string.touch_layout_classic) to TouchLayout.CLASSIC.ordinal),
        c.layout.ordinal,
    ) { set(c.copy(layout = TouchLayout.entries[it])) }
    Toggle(stringResource(R.string.touch_auto_mode), stringResource(R.string.tc_auto_mode_text), c.autoMode) { set(c.copy(autoMode = it)) }
    if (mobile) {
        Label(stringResource(R.string.touch_style))
        Chips(
            listOf(stringResource(R.string.touch_style_ink) to PadStyle.INK.ordinal, stringResource(R.string.touch_style_glass) to PadStyle.GLASS.ordinal),
            c.style.ordinal,
        ) { set(c.copy(style = PadStyle.entries[it])) }
    }
    Label(stringResource(R.string.touch_skin))
    Chips(
        listOf(
            stringResource(R.string.skin_playstation) to PadSkin.PLAYSTATION.ordinal,
            stringResource(R.string.skin_xbox) to PadSkin.XBOX.ordinal,
            stringResource(R.string.skin_nintendo) to PadSkin.NINTENDO.ordinal,
        ),
        c.skin.ordinal,
    ) { set(c.copy(skin = PadSkin.entries[it])) }
    Stepper(stringResource(R.string.opacity), "${c.opacity}%", c.opacity > 10, c.opacity < 100, {
        set(c.copy(opacity = (c.opacity - 10).coerceAtLeast(10)))
    }) { set(c.copy(opacity = (c.opacity + 10).coerceAtMost(100))) }
    val size = Math.round(c.scale * 10)
    Stepper(stringResource(R.string.touch_size), "${size * 10}%", size > 5, size < 20, {
        set(c.copy(scale = (size - 1).coerceAtLeast(5) / 10f))
    }) { set(c.copy(scale = (size + 1).coerceAtMost(20) / 10f)) }
    if (mobile) Toggle(stringResource(R.string.tc_show_names), stringResource(R.string.tc_show_names_text), c.showNames) { set(c.copy(showNames = it)) }
    Toggle(stringResource(R.string.touch_haptics), stringResource(R.string.touch_haptics_text), c.haptics) { set(c.copy(haptics = it)) }
    Toggle(stringResource(R.string.touch_float), stringResource(R.string.touch_float_text), c.sticksFloat) { set(c.copy(sticksFloat = it)) }

    if (mobile) ProSection(state, c) { set(c.copy(pro = it)) }
    MacroSection(state, c, actions)
}

@Composable
private fun ProSection(state: GameMenuState, c: TouchConfig, set: (ProTouch) -> Unit) {
    val p = c.pro
    Section(stringResource(R.string.tp_title))
    Hint(stringResource(R.string.tp_intro))
    Toggle(stringResource(R.string.tp_on), stringResource(R.string.tp_on_text), p.on) { set(p.copy(on = it)) }
    if (p.on) {
        Toggle(stringResource(R.string.tp_slide), stringResource(R.string.tp_slide_text), p.slideAll) { set(p.copy(slideAll = it)) }
        // (the launcher's ranges: D-pad size 80..160 in 5s, dead zone 0..40, hit area 100..160 in 5s)
        Stepper(stringResource(R.string.tp_pad_size), "${p.padSize}%", p.padSize > 80, p.padSize < 160, {
            set(p.copy(padSize = p.padSize - 5))
        }) { set(p.copy(padSize = p.padSize + 5)) }
        Stepper(stringResource(R.string.tp_dead), "${p.dead}%", p.dead > 0, p.dead < 40, { set(p.copy(dead = p.dead - 1)) }) { set(p.copy(dead = p.dead + 1)) }
        Stepper(stringResource(R.string.tp_hit), "${p.hit}%", p.hit > 100, p.hit < 160, { set(p.copy(hit = p.hit - 5)) }) { set(p.copy(hit = p.hit + 5)) }
    }
    MenuButton(stringResource(R.string.tp_guide), Icons.AutoMirrored.Rounded.HelpOutline) { state.overlay = MenuOverlay.ProGuide }
}

@Composable
private fun MacroSection(state: GameMenuState, c: TouchConfig, actions: GameMenuActions) {
    val painter = rememberPadPainter(c)
    Section(stringResource(R.string.macros))
    Hint(stringResource(R.string.mc_intro))
    if (c.macros.isEmpty()) Hint(stringResource(R.string.mc_empty))
    c.macros.forEachIndexed { i, m ->
        val shown = m.place?.visible ?: true
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, CutCornerShape(6.dp)).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PadIcon(painter, null, m)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    macroSummary(c.skin, m), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            SmallIconButton(if (shown) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, stringResource(R.string.mc_on_screen)) {
                val base = m.place ?: PadDefaults.macroPlace(i)
                actions.touch(c.copy(macros = c.macros.map { if (it.id == m.id) it.copy(place = base.copy(visible = !shown)) else it }))
            }
            SmallIconButton(Icons.Rounded.Edit, stringResource(R.string.macro_edit)) { state.overlay = MenuOverlay.EditMacro(m) }
            SmallIconButton(Icons.Rounded.Delete, stringResource(R.string.mc_delete)) {
                actions.touch(c.copy(macros = c.macros.filter { it.id != m.id }))
            }
        }
    }
    MenuButton(stringResource(R.string.macro_add), Icons.Rounded.Add) {
        state.overlay = MenuOverlay.EditMacro(
            Macro(id = "m" + System.currentTimeMillis().toString(36), name = "M${c.macros.size + 1}", steps = listOf(MacroStep(Ps2.CIRCLE, 60))),
        )
    }
}

// ---- Help: what each button does, and the controller in this menu

@Composable
private fun HelpTab(state: GameMenuState) {
    val c = state.touch
    Chips(
        listOf(
            stringResource(R.string.pad_scene_fight) to PadScene.FIGHT.ordinal,
            stringResource(R.string.pad_scene_campaign) to PadScene.CAMPAIGN.ordinal,
            stringResource(R.string.pad_scene_menu) to PadScene.MENU.ordinal,
        ),
        state.helpScene.ordinal,
    ) { state.helpScene = PadScene.entries[it] }
    Hint(stringResource(R.string.tc_guide_intro))
    val painter = rememberPadPainter(c)
    guideItems(state.helpScene).forEach { g ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            PadIcon(painter, g.ctl, g.macro, state.helpScene)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(g.title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(g.text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Section(stringResource(R.string.gm_help_pad))
    val combo = when (state.padMap.menuCombo) {
        MenuCombo.SELECT_START -> "SELECT + START"
        MenuCombo.L3_R3 -> "L3 + R3"
        MenuCombo.OFF -> null
    }
    Text(
        if (combo != null) stringResource(R.string.gm_help_pad_menu, combo) else stringResource(R.string.gm_help_pad_menu_back),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(stringResource(R.string.gm_help_pad_nav), style = MaterialTheme.typography.bodyMedium)
}

// ---- pieces, in the game's look: paper, ink, orange; a ring around the control the controller is on

@Composable
private fun Modifier.focusRing(shape: Shape, color: Color = LocalMenuExtras.current.focus): Modifier {
    var focused by remember { mutableStateOf(false) }
    val show = focused && LocalPadNav.current
    return this.onFocusChanged { focused = it.isFocused }.then(if (show) Modifier.border(3.dp, color, shape) else Modifier)
}

@Composable
private fun Section(title: String, restart: Boolean = false) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (restart) RestartBadge()
        }
        Box(Modifier.padding(top = 2.dp).size(width = 52.dp, height = 3.dp).background(MaterialTheme.colorScheme.primaryContainer))
    }
}

@Composable
private fun Label(text: String, restart: Boolean = false) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
        if (restart) RestartBadge()
    }
}

/** "On restart": saved now, but the engine reads it when the game starts. */
@Composable
private fun RestartBadge() {
    Text(
        stringResource(R.string.gm_restart),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary, maxLines = 1,
        modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.secondary, CutCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    // the nearest option counts as chosen (a value written elsewhere may be in between), as in the launcher
    val best = options.minByOrNull { kotlin.math.abs(it.second - selected) }?.second
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (label, v) ->
            val on = v == best
            val shape = RoundedCornerShape(16.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.2.sp),
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.heightIn(min = 34.dp).clip(shape)
                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.5.dp, if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline, shape)
                    .focusRing(shape)
                    .selectable(on, role = Role.RadioButton) { onSelect(v) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun Toggle(title: String, text: String?, checked: Boolean, restart: Boolean = false, onChange: (Boolean) -> Unit) {
    val shape = CutCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).focusRing(shape).toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                if (restart) RestartBadge()
            }
            if (text != null) Hint(text)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

@Composable
private fun Stepper(label: String, value: String, canDown: Boolean, canUp: Boolean, onDown: () -> Unit, onUp: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StepButton("−", canDown, onDown)
        Text(
            value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
            modifier = Modifier.width(58.dp),
        )
        StepButton("+", canUp, onUp)
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(6.dp)
    Box(
        Modifier.size(40.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape).focusRing(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f),
        )
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        Modifier.size(40.dp).clip(shape).focusRing(shape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MenuButton(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    val bg = when {
        primary -> MaterialTheme.colorScheme.primaryContainer
        danger -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when {
        primary -> MaterialTheme.colorScheme.onPrimaryContainer
        danger -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurface
    }.copy(alpha = if (enabled) 1f else 0.4f)
    Row(
        modifier.heightIn(min = 44.dp).clip(shape).background(bg)
            .border(1.5.dp, if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline, shape)
            .focusRing(shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, textAlign = TextAlign.Center)
    }
}
