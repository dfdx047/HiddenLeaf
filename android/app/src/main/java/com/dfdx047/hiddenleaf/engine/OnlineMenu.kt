package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.Lan
import com.dfdx047.hiddenleaf.data.LobbyState
import com.dfdx047.hiddenleaf.data.OnlineLobby
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Naruto Online" in the game's own Mode Select (the developer, 2026-11-14): the online menu inside the game, over the game's
 * Mode Select, never out to the launcher. Quick match, a room by code (create or join) and the local Wi-Fi (create or search),
 * on wooden boards like the Mode Select's, with the game's own colours; touch and controller (the D-pad moves, ✕ chooses,
 * ○ or △ goes back). The lobby is the launcher's ([OnlineLobby]); once the other player is there, [start] restarts the game in
 * step with the other phone, and the loading cover (OnlineGame) hides the way to the character select.
 */
class OnlineMenuHost(context: Context, private val lobby: OnlineLobby, private val fp: String, private val close: () -> Unit, private val start: () -> Unit) :
    FrameLayout(context) {
    private val owner = Owner()
    private val page = mutableStateOf(OnlinePage.MAIN)
    private val compose = ComposeView(context).apply { setContent { OnlineMenu(lobby, fp, page.value, { page.value = it }, ::back, start) } }

    init {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        addView(compose, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isFocusable = true
    }

    fun installOwners(vararg roots: android.view.View) {
        for (root in roots) {
            root.setViewTreeLifecycleOwner(owner)
            root.setViewTreeSavedStateRegistryOwner(owner)
        }
    }

    fun focus() {
        if (!compose.hasFocus()) compose.requestFocus()
    }

    /** ○, △ or Back: out of what is going on (the search, the room), then up a page, then out of the menu. */
    fun back() {
        when (lobby.state.value) {
            is LobbyState.Starting -> return
            is LobbyState.Failed -> { lobby.dismiss(); return }
            LobbyState.Idle -> {}
            else -> { lobby.cancel(); return }
        }
        if (page.value != OnlinePage.MAIN) page.value = OnlinePage.MAIN else close()
    }

    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        init {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }
    }
}

enum class OnlinePage { MAIN, CODE, LAN, RANKED, LOBBY }

// The Mode Select's look: dark wood boards with a carved border, cream-gold lettering outlined in brown, the orange of the
// game's highlight, a blue-grey help bar at the bottom.
private val WOOD_TOP = Color(0xFF7A4A24)
private val WOOD_BOTTOM = Color(0xFF4A2A12)
private val WOOD_EDGE = Color(0xFF2A160A)
private val GOLD = Color(0xFFFFE6A0)
private val ORANGE = Color(0xFFFF7A1A)
private val ORANGE_DEEP = Color(0xFFE2560B)
private val CREAM = Color(0xFFEDE6DE)
private val INK = Color(0xFF2A160A)
private val LEAF = Color(0xFF3FB14A)

private val Title = FontFamily(Font(R.font.shojumaru_regular))
private val Body = FontFamily(Font(R.font.kanit_semibold))

private fun carved(size: Int) = TextStyle(
    fontFamily = Title, fontSize = size.sp, color = GOLD,
    shadow = Shadow(INK, Offset(2.5f, 2.5f), 2f),
)

private val bodyStyle = TextStyle(fontFamily = Body, fontSize = 14.sp, color = CREAM, shadow = Shadow(Color.Black, Offset(1f, 1f), 2f))

@Composable
private fun OnlineMenu(lobby: OnlineLobby, fp: String, page: OnlinePage, go: (OnlinePage) -> Unit, back: () -> Unit, start: () -> Unit) {
    val state by lobby.state.collectAsState()
    val prefs by lobby.prefs.state.collectAsState()
    // (the developer, 2026-11-14: the Mode Select goes on, Naruto at its right and its music; the menu is a panel at the left, over
    // the game's own boards)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val share = if (maxWidth / maxHeight < 1.45f) 0.64f else 0.47f
        val panel = maxWidth * share
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxHeight().width(panel)
                    .background(Brush.horizontalGradient(listOf(Color(0xF21B1009), Color(0xE6120A05), Color(0x00120A05))))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
            ) {
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(3.dp).background(Brush.verticalGradient(listOf(Color.Transparent, ORANGE, Color.Transparent))))
                Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 18.dp, top = 8.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Naruto Online", style = carved(26))
                    Text(stringResource(R.string.ingame_online_who, prefs.name), style = bodyStyle.copy(color = GOLD.copy(alpha = 0.85f), fontSize = 12.sp))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (val s = state) {
                            LobbyState.Idle -> when (page) {
                                OnlinePage.MAIN -> MainBoards(lobby, fp, go, start)
                                OnlinePage.CODE -> CodePad(onJoin = { code -> lobby.join(code, fp, start) })
                                OnlinePage.LAN -> LanBoards(lobby, fp, start)
                                OnlinePage.RANKED -> RankedBoards(lobby, fp, start)
                                OnlinePage.LOBBY -> CustomLobbyBoards(lobby, fp)
                            }
                            LobbyState.LanSearching -> LanFound(lobby, fp, start, back)
                            else -> Status(s, back)
                        }
                    }
                    HelpBar()
                }
            }
            // (the touches on the game's side stop here: they do not reach the Mode Select or the hidden on-screen controls)
            Spacer(Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}))
        }
    }
}

@Composable
private fun MainBoards(lobby: OnlineLobby, fp: String, go: (OnlinePage) -> Unit, start: () -> Unit) {
    val first = remember { FocusRequester() }
    val prefs by lobby.prefs.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val modeLabel = when (prefs.inputMode) {
        "touch" -> stringResource(R.string.online_input_touch)
        "gamepad" -> stringResource(R.string.online_input_gamepad)
        else -> stringResource(R.string.online_input_any)
    }
    Column(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Board(stringResource(R.string.online_quick), stringResource(R.string.online_quick_hint), Modifier.focusRequester(first), small = true) { lobby.quick(fp, start = start) }
        Board(stringResource(R.string.online_train_while_queue), stringResource(R.string.online_training_searching), small = true) {
            val paths = com.dfdx047.hiddenleaf.data.GamePaths(ctx)
            TrainingQueue.arm(
                paths,
                TrainingQueue.Request(
                    server = com.dfdx047.hiddenleaf.data.Online.cleanServer(prefs.server),
                    name = prefs.name,
                    fp = fp,
                    inputMode = com.dfdx047.hiddenleaf.data.Online.effectiveInputMode(prefs.inputMode),
                    delay = prefs.delay,
                    relay = prefs.relay,
                )
            )
            start()
        }
        Board(stringResource(R.string.online_ranked_board_title), "Genin → Kage · Top 100", small = true) { go(OnlinePage.RANKED) }
        Board(stringResource(R.string.online_custom_lobby_title), "2–8P · Rei da Mesa · Espectador", small = true) { go(OnlinePage.LOBBY) }
        Board(stringResource(R.string.online_create), stringResource(R.string.ingame_online_create_hint), small = true) { lobby.create(fp, start) }
        Board(stringResource(R.string.ingame_online_join), stringResource(R.string.ingame_online_join_hint), small = true) { go(OnlinePage.CODE) }
        Board(stringResource(R.string.online_mode_lan), stringResource(R.string.ingame_online_lan_hint), small = true) { go(OnlinePage.LAN) }
        Board("${stringResource(R.string.online_input_mode)}: $modeLabel", null, small = true) {
            val next = when (prefs.inputMode) { "any" -> "touch"; "touch" -> "gamepad"; else -> "any" }
            lobby.update(prefs.copy(inputMode = next))
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

@Composable
private fun RankedBoards(lobby: OnlineLobby, fp: String, start: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs by lobby.prefs.state.collectAsState()
    var profile by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.RankedProfile?>(null) }
    var entries by remember { mutableStateOf<List<com.dfdx047.hiddenleaf.data.OnlineAdvanced.LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(prefs.server) {
        loading = true
        val srv = com.dfdx047.hiddenleaf.data.Online.cleanServer(prefs.server)
        runCatching {
            profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                ctx, srv, prefs.name, com.dfdx047.hiddenleaf.data.Online.effectiveInputMode(prefs.inputMode),
            )
        }
        entries = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchLeaderboard(srv, "all", 50)
        loading = false
    }

    Column(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.online_ranked_board_title), style = carved(18))
        Board("Jogar Ranqueada", "Fila competitiva separada · Ganhe RP e Maestria", small = true) {
            lobby.quick(fp, ranked = true, start = start)
        }
        profile?.let { p ->
            com.dfdx047.hiddenleaf.ui.components.NinjaInfoCard(
                profile = p,
                onCycleChar = {
                    val list = com.dfdx047.hiddenleaf.ui.components.UN5_CHARACTERS
                    val idx = list.indexOfFirst { it.id == p.favChar }.let { if (it < 0) 0 else (it + 1) % list.size }
                    val nextId = list[idx].id
                    com.dfdx047.hiddenleaf.data.OnlineAdvanced.saveFavChar(ctx, nextId)
                    profile = p.copy(favChar = nextId)
                },
            )
        }
        if (loading) {
            Pulse()
        } else if (entries.isEmpty()) {
            Text(stringResource(R.string.online_ranked_empty), style = bodyStyle, textAlign = TextAlign.Center)
        } else {
            for (e in entries.take(12)) {
                val medal = when (e.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${e.rank}" }
                val ch = com.dfdx047.hiddenleaf.ui.components.charInfoOf(e.favChar)
                Row(
                    Modifier.fillMaxWidth()
                        .border(1.5.dp, if (e.rank <= 3) ORANGE else WOOD_EDGE, CutCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(WOOD_TOP, WOOD_BOTTOM)), CutCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(medal, style = bodyStyle.copy(color = GOLD, fontSize = 14.sp), modifier = Modifier.width(28.dp))
                    com.dfdx047.hiddenleaf.ui.components.CharacterCutout(e.favChar, e.favCharLvl, sizeDp = 40)
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = bodyStyle.copy(color = GOLD, fontSize = 14.sp))
                        Text("${e.tier} · ${ch.name} Lv.${e.favCharLvl} · ${e.wins}V/${e.losses}D", style = bodyStyle.copy(fontSize = 11.sp, color = CREAM.copy(alpha = 0.85f)))
                    }
                    Text("${e.points} RP", style = carved(15))
                }
            }
        }
    }
}

@Composable
private fun CustomLobbyBoards(lobby: OnlineLobby, fp: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs by lobby.prefs.state.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var cur by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.CustomLobby?>(null) }
    var maxP by remember { mutableStateOf(4) }
    var err by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.online_custom_lobby_title), style = carved(17))
        val active = cur
        if (active != null) {
            Plank { Text("Lobby #${active.code} (${active.members.size}/${active.maxPlayers})", style = carved(22)) }
            for (m in active.members) {
                Board("${m.name}  [${m.role.uppercase()}]", null, small = true, onClick = {})
            }
            Board(stringResource(R.string.online_custom_lobby_leave), null, small = true) {
                scope.launch {
                    com.dfdx047.hiddenleaf.data.OnlineAdvanced.leaveLobby(ctx, com.dfdx047.hiddenleaf.data.Online.cleanServer(prefs.server), active.code)
                    cur = null
                }
            }
        } else {
            Board("Máximo de jogadores: ${maxP}P", "Toque para alternar 2P / 4P / 6P / 8P", small = true) {
                maxP = when (maxP) { 2 -> 4; 4 -> 6; 6 -> 8; else -> 2 }
            }
            Board(stringResource(R.string.online_custom_lobby_create), stringResource(R.string.online_custom_lobby_hint), small = true) {
                err = null
                scope.launch {
                    runCatching {
                        cur = com.dfdx047.hiddenleaf.data.OnlineAdvanced.createLobby(
                            ctx, com.dfdx047.hiddenleaf.data.Online.cleanServer(prefs.server), prefs.name, fp, maxP, true,
                            com.dfdx047.hiddenleaf.data.Online.effectiveInputMode(prefs.inputMode),
                        )
                    }.onFailure { err = it.message }
                }
            }
            err?.let { Text(it, style = bodyStyle.copy(color = ORANGE)) }
        }
    }
}

@Composable
private fun LanBoards(lobby: OnlineLobby, fp: String, start: () -> Unit) {
    val first = remember { FocusRequester() }
    Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.online_mode_lan), style = carved(18))
        Board(stringResource(R.string.lan_create), stringResource(R.string.ingame_lan_create_hint), Modifier.focusRequester(first)) { lobby.hostLan(fp, start) }
        Board(stringResource(R.string.lan_search), stringResource(R.string.ingame_lan_search_hint)) { lobby.searchLan() }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

@Composable
private fun LanFound(lobby: OnlineLobby, fp: String, start: () -> Unit, back: () -> Unit) {
    val hosts by lobby.lanHosts.collectAsState()
    Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.lan_searching), style = carved(16), textAlign = TextAlign.Center)
        Pulse()
        if (hosts.isEmpty()) Text(stringResource(R.string.lan_none_yet), style = bodyStyle, textAlign = TextAlign.Center)
        val first = remember { FocusRequester() }
        hosts.forEachIndexed { i, h: Lan.Found ->
            val ok = h.compatible(fp)
            Board(h.beacon.name, if (ok) h.from else stringResource(R.string.lan_different), if (i == 0) Modifier.focusRequester(first) else Modifier, enabled = ok) {
                lobby.joinLan(h, fp, start)
            }
        }
        // (touch players have no ○ here: the board that stops the search)
        Board(stringResource(R.string.online_cancel), null, (if (hosts.isEmpty()) Modifier.focusRequester(first) else Modifier).width(260.dp), small = true, onClick = back)
        LaunchedEffect(hosts.size) { runCatching { first.requestFocus() } }
    }
}

/** What the lobby is doing: the room's code, the search, the other player coming, or why it stopped. */
@Composable
private fun Status(s: LobbyState, back: () -> Unit) {
    val focus = remember { FocusRequester() }
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (s) {
            is LobbyState.Working -> { Text(stringResource(s.step), style = carved(18), textAlign = TextAlign.Center); Pulse() }
            is LobbyState.Waiting -> {
                Text(stringResource(R.string.online_room_code), style = bodyStyle)
                Plank { Text(s.code.chunked(3).joinToString(" "), style = carved(44)) }
                Text(stringResource(R.string.online_waiting), style = carved(16)); Pulse()
            }
            is LobbyState.Searching -> {
                var now by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(s) { while (true) { delay(500); now = System.currentTimeMillis() } }
                val t = ((now - s.since) / 1000).coerceAtLeast(0)
                Text(stringResource(R.string.online_quick_searching, "%d:%02d".format(t / 60, t % 60)), style = carved(18), textAlign = TextAlign.Center)
                Pulse()
            }
            is LobbyState.Starting -> { Text(stringResource(R.string.online_starting, s.peer), style = carved(20), textAlign = TextAlign.Center); Pulse() }
            is LobbyState.LanHosting -> {
                Text(stringResource(R.string.lan_waiting), style = carved(18), textAlign = TextAlign.Center)
                if (s.ips.isNotEmpty()) Text(stringResource(R.string.lan_this_phone, s.ips.joinToString()), style = bodyStyle)
                Pulse()
            }
            is LobbyState.Failed -> Plank { Text(stringResource(s.message), style = bodyStyle.copy(fontSize = 16.sp), textAlign = TextAlign.Center) }
            else -> {}
        }
        if (s !is LobbyState.Starting) {
            Board(stringResource(if (s is LobbyState.Failed) R.string.online_dismiss else R.string.online_cancel), null, Modifier.focusRequester(focus).width(260.dp), small = true, onClick = back)
            LaunchedEffect(s::class) { runCatching { focus.requestFocus() } }
        }
    }
}

/** Six digits with the game's boards for keys: touch or the controller (the D-pad moves, ✕ presses). */
@Composable
private fun CodePad(onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.online_code), style = carved(14))
        Plank { Text(code.padEnd(6, '_').chunked(3).joinToString(" "), style = carved(28)) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK")).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { c, k ->
                        Key(k, if (r == 0 && c == 0) Modifier.focusRequester(first) else Modifier, enabled = k != "OK" || code.length == 6) {
                            when (k) {
                                "⌫" -> code = code.dropLast(1)
                                "OK" -> onJoin(code)
                                else -> if (code.length < 6) code += k
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

@Composable
private fun Board(title: String, hint: String?, modifier: Modifier = Modifier, small: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = CutCornerShape(10.dp)
    Row(
        modifier.fillMaxWidth()
            .border(if (focused) 3.dp else 2.dp, if (focused) ORANGE else WOOD_EDGE, shape)
            .background(Brush.verticalGradient(if (focused) listOf(Color(0xFF9A5A26), Color(0xFF5E3414)) else listOf(WOOD_TOP, WOOD_BOTTOM)), shape)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp, vertical = if (small) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // (the Mode Select's green arrows mark the board the cursor is on)
        Text(if (focused) "▶" else " ", style = bodyStyle.copy(color = LEAF, fontSize = 18.sp), modifier = Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = carved(if (small) 17 else 21))
            if (hint != null) Text(hint, style = bodyStyle.copy(fontSize = 12.sp, color = CREAM.copy(alpha = 0.9f)))
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.size(58.dp, 40.dp)
            .border(if (focused) 3.dp else 2.dp, if (focused) ORANGE else WOOD_EDGE, shape)
            .background(Brush.verticalGradient(if (focused) listOf(ORANGE, ORANGE_DEEP) else listOf(WOOD_TOP, WOOD_BOTTOM)), shape)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = carved(20)) }
}

@Composable
private fun Plank(content: @Composable () -> Unit) {
    val shape = CutCornerShape(12.dp)
    Box(
        Modifier.border(3.dp, ORANGE, shape).background(Brush.verticalGradient(listOf(WOOD_TOP, WOOD_BOTTOM)), shape).padding(horizontal = 26.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Three leaves going on and off, the game's wait. */
@Composable
private fun Pulse() {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 0 until 3) Text("◆", style = bodyStyle.copy(color = if (a.toInt() == i) ORANGE else GOLD.copy(alpha = 0.35f), fontSize = 18.sp))
    }
}

/** The bar at the bottom of the game's menus: what the buttons do here. */
@Composable
private fun HelpBar() {
    Row(
        Modifier.fillMaxWidth().background(Color(0x99223A55)).padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.ingame_online_help), style = bodyStyle.copy(fontSize = 13.sp))
    }
}
