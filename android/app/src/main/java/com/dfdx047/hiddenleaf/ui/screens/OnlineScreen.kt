package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.InstallState
import com.dfdx047.hiddenleaf.data.Lan
import com.dfdx047.hiddenleaf.data.LobbyState
import com.dfdx047.hiddenleaf.data.Online
import com.dfdx047.hiddenleaf.data.OnlineLobby
import com.dfdx047.hiddenleaf.data.OnlineQuota
import com.dfdx047.hiddenleaf.data.ServerTest
import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Online: two players, each on their phone. One creates a room and tells the other its six-digit code; the other joins with it. The
 * game then starts on both phones as one match: the room's creator is player 1, the other player 2 (data/Online.kt, the engine's
 * recomp/android/host/online).
 *
 * At the top the player picks how the two phones meet: through the rooms service on the internet, or on the same Wi-Fi (data/Lan.kt),
 * where one creates a match and the other picks it from the list of matches heard on the network. The match itself is the same.
 * On the internet there is also the quick match: a random opponent with the same app and mods, no code (the service pairs the two).
 *
 * For now online and local matches are base game only: with a mod switched on, the page says so and leads to the Mods page
 * ([onOpenMods]) instead of its buttons. When the service's free plan is used up for the day, a notice says until when.
 */
@Composable
fun OnlineScreen(vm: AppViewModel, onOpenMods: () -> Unit = {}) {
    val lobby: OnlineLobby = viewModel()
    val prefs by lobby.prefs.state.collectAsStateWithLifecycle()
    val state by lobby.state.collectAsStateWithLifecycle()
    val test by lobby.test.collectAsStateWithLifecycle()
    val waiting by lobby.waiting.collectAsStateWithLifecycle()
    val install by vm.install.collectAsStateWithLifecycle()
    val fileMods by vm.fileMods.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ready = install is InstallState.Installed && vm.engineAvailable
    val serverSet = Online.isAllowedServer(prefs.server)
    val idle = state is LobbyState.Idle || state is LobbyState.Failed
    // (online and local matches are base game only for now: the launcher's list, and the engine's own as a check)
    val modsOn = remember(fileMods) { fileMods.any { it.enabled } || Online.modsOn(vm.paths) }
    var code by rememberSaveable { mutableStateOf("") }
    val fp = { Online.fingerprint(context, vm.paths) }

    // (a local match or search stops with the page: nobody would see the other player arrive)
    DisposableEffect(lobby) { onDispose { lobby.leaveScreen() } }
    // (one cheap request as the page opens: the free plan's notice and the number searching a quick match are current)
    LaunchedEffect(prefs.server) { if (serverSet) lobby.checkService(fp()) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_online), stringResource(R.string.online_subtitle))

        OnlineQuotaNotice(onLan = if (prefs.lan || !idle) null else ({ lobby.update(prefs.copy(lan = true)) }))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false to R.string.online_mode_internet, true to R.string.online_mode_lan).forEachIndexed { i, (lan, label) ->
                SegmentedButton(
                    selected = prefs.lan == lan,
                    onClick = { lobby.update(prefs.copy(lan = lan)) },
                    enabled = idle,
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(stringResource(label)) }
            }
        }

        if (prefs.lan) LanPlay(lobby, ready, modsOn, onOpenMods, fp) { vm.play() }
        else SectionCard(stringResource(R.string.online_play_title), NinjaIcons.Shuriken, accent = Konoha.NarutoOrange) {
            when {
                !ready -> Text(stringResource(R.string.online_need_game), style = MaterialTheme.typography.bodyMedium)
                !serverSet -> Text(stringResource(R.string.online_need_server), style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = prefs.name,
                onValueChange = { lobby.update(prefs.copy(name = Online.cleanName(it))) },
                label = { Text(stringResource(R.string.online_name)) },
                singleLine = true,
                enabled = idle,
                modifier = Modifier.fillMaxWidth(),
            )
            when (val s = state) {
                is LobbyState.Waiting -> RoomCode(s.code) { lobby.cancel() }
                is LobbyState.Searching -> QuickSearching(s.since) { lobby.cancel() }
                is LobbyState.Working -> Busy(stringResource(s.step)) { lobby.cancel() }
                is LobbyState.Starting -> Busy(stringResource(R.string.online_starting, s.peer), null)
                else -> if (modsOn) ModsOnly(onOpenMods) else {
                    val can = ready && serverSet && prefs.name.isNotBlank()
                    Button(onClick = { lobby.quick(fp()) { vm.play() } }, enabled = can, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.online_quick))
                    }
                    OutlinedButton(
                        onClick = {
                            com.dfdx047.hiddenleaf.engine.TrainingQueue.arm(
                                vm.paths,
                                com.dfdx047.hiddenleaf.engine.TrainingQueue.Request(
                                    server = Online.cleanServer(prefs.server),
                                    name = prefs.name,
                                    fp = fp(),
                                    inputMode = Online.effectiveInputMode(prefs.inputMode),
                                    delay = prefs.delay,
                                    relay = prefs.relay,
                                )
                            )
                            vm.play()
                        },
                        enabled = can,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.online_train_while_queue))
                    }
                    Text(
                        stringResource(R.string.online_quick_hint) +
                            (if (waiting > 0) " " + pluralStringResource(R.plurals.online_quick_others, waiting, waiting) + "." else ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.online_or_friend), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { lobby.create(fp()) { vm.play() } }, enabled = can, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.online_create))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text(stringResource(R.string.online_code)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { lobby.join(code, fp()) { vm.play() } },
                            enabled = can && code.length == 6,
                        ) { Text(stringResource(R.string.online_join)) }
                    }
                    if (s is LobbyState.Failed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(s.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { lobby.dismiss() }) { Text(stringResource(R.string.online_dismiss)) }
                        }
                    }
                }
            }
        }

        if (!prefs.lan) RankedAndLobbySection(prefs, ready, serverSet, fp)

        SectionCard(stringResource(R.string.online_guide_title), NinjaIcons.Scroll, accent = Konoha.LeeGreen) {
            Text(stringResource(if (prefs.lan) R.string.lan_guide else R.string.online_guide), style = MaterialTheme.typography.bodyMedium)
        }

        SectionCard(stringResource(R.string.online_options_title), NinjaIcons.Tools, accent = Konoha.SasukeBlueLight) {
            Text(stringResource(R.string.online_delay), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (d in 0..4) {
                    FilterChip(
                        selected = prefs.delay == d,
                        onClick = { lobby.update(prefs.copy(delay = d)) },
                        enabled = idle,
                        label = { Text(if (d == 0) stringResource(R.string.online_delay_auto) else "$d") },
                    )
                }
            }
            Text(stringResource(R.string.online_delay_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // (no relay on the local network: there is no room on the service to go through)
            if (!prefs.lan) {
                Text(stringResource(R.string.online_input_mode), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((key, labelId) in listOf("any" to R.string.online_input_any, "touch" to R.string.online_input_touch, "gamepad" to R.string.online_input_gamepad)) {
                        FilterChip(
                            selected = prefs.inputMode == key,
                            onClick = { lobby.update(prefs.copy(inputMode = key)) },
                            enabled = idle,
                            label = { Text(stringResource(labelId)) },
                        )
                    }
                }
                Text(stringResource(R.string.online_input_mode_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!prefs.lan) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.online_relay), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.online_relay_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = prefs.relay, onCheckedChange = { lobby.update(prefs.copy(relay = it)) }, enabled = idle)
            }
        }

        if (!prefs.lan) SectionCard(stringResource(R.string.online_server_card), NinjaIcons.Gate, accent = Konoha.ChakraAmber) {
            OutlinedTextField(
                value = prefs.server,
                onValueChange = { lobby.update(prefs.copy(server = it.trim())) },
                label = { Text(stringResource(R.string.online_server)) },
                singleLine = true,
                enabled = idle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.online_server_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { lobby.testServer() }, enabled = serverSet && test !is ServerTest.Running) {
                    Text(stringResource(R.string.online_test))
                }
                when (val t = test) {
                    is ServerTest.Running -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    is ServerTest.Done -> Text(
                        stringResource(R.string.online_test_ok, t.hello.ms, listOf(t.hello.colo, t.hello.country).filter { it.isNotEmpty() }.joinToString(", ")),
                        style = MaterialTheme.typography.bodyMedium, color = Konoha.LeeGreen,
                    )
                    is ServerTest.Failed -> Text(stringResource(t.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    ServerTest.Idle -> {}
                }
            }
        }
    }
}

/** The local network's card: the name, then creating a match or searching for one (the list of matches heard, tap to join). */
@Composable
private fun LanPlay(lobby: OnlineLobby, ready: Boolean, modsOn: Boolean, onOpenMods: () -> Unit, fp: () -> String, start: () -> Unit) {
    val prefs by lobby.prefs.state.collectAsStateWithLifecycle()
    val state by lobby.state.collectAsStateWithLifecycle()
    val hosts by lobby.lanHosts.collectAsStateWithLifecycle()
    val idle = state is LobbyState.Idle || state is LobbyState.Failed
    // (this phone's fingerprint, taken when the search starts: the list says which matches it can join)
    var myFp by rememberSaveable { mutableStateOf("") }

    SectionCard(stringResource(R.string.lan_play_title), NinjaIcons.Shuriken, accent = Konoha.NarutoOrange) {
        if (!ready) Text(stringResource(R.string.online_need_game), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = prefs.name,
            onValueChange = { lobby.update(prefs.copy(name = Online.cleanName(it))) },
            label = { Text(stringResource(R.string.online_name)) },
            singleLine = true,
            enabled = idle,
            modifier = Modifier.fillMaxWidth(),
        )
        when (val s = state) {
            is LobbyState.LanHosting -> LanWaiting(s.ips) { lobby.cancel() }
            is LobbyState.LanSearching -> {
                Busy(stringResource(R.string.lan_searching)) { lobby.cancel() }
                if (hosts.isEmpty()) Text(stringResource(R.string.lan_none_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (h in hosts) LanHostRow(h, h.compatible(myFp), ready && prefs.name.isNotBlank()) { lobby.joinLan(h, myFp, start) }
            }
            is LobbyState.Working -> Busy(stringResource(s.step)) { lobby.cancel() }
            is LobbyState.Starting -> Busy(stringResource(R.string.online_starting, s.peer), null)
            else -> if (modsOn) ModsOnly(onOpenMods) else {
                val can = ready && prefs.name.isNotBlank()
                Button(onClick = { lobby.hostLan(fp(), start) }, enabled = can, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lan_create))
                }
                OutlinedButton(onClick = { myFp = fp(); lobby.searchLan() }, enabled = can, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lan_search))
                }
                if (s is LobbyState.Failed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(s.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { lobby.dismiss() }) { Text(stringResource(R.string.online_dismiss)) }
                    }
                }
            }
        }
        Text(stringResource(R.string.lan_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The open local match: waiting, with this phone's address (the other phone's should start the same way: same network), and a way out. */
@Composable
private fun LanWaiting(ips: List<String>, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.lan_waiting), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            stringResource(R.string.lan_this_phone, ips.joinToString("  ·  ")),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onCancel) { Text(stringResource(R.string.online_cancel)) }
    }
}

/** A match heard on the network: who, where, and whether this phone can play it (same app build and mods). */
@Composable
private fun LanHostRow(h: Lan.Found, compatible: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(h.beacon.name.ifEmpty { "?" }, style = MaterialTheme.typography.titleSmall)
            Text(h.from, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!compatible) Text(stringResource(R.string.lan_different), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onJoin, enabled = enabled && compatible) { Text(stringResource(R.string.online_join)) }
    }
}

/** The open room: its code large, for the other player, and a way out. */
@Composable
private fun RoomCode(code: String, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.online_room_code), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            code.chunked(3).joinToString(" "),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 6.sp),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.online_waiting), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.online_cancel)) }
    }
}

/** Quick match: searching, how long so far, and a way out. */
@Composable
private fun QuickSearching(since: Long, onCancel: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val s = ((now - since) / 1000).coerceAtLeast(0)
    Busy(stringResource(R.string.online_quick_searching, "%d:%02d".format(s / 60, s % 60)), onCancel)
}

/** A mod is switched on: online and local matches are base game only for now. Instead of the buttons: why, and the way to the Mods page. */
@Composable
private fun ModsOnly(onOpenMods: () -> Unit) {
    Text(stringResource(R.string.online_mods_only), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Konoha.NarutoOrangeDeep)
    FilledTonalButton(onClick = onOpenMods) { Text(stringResource(R.string.online_mods_open)) }
}

/**
 * The rooms service's free plan used up for the day (data/OnlineQuota.kt): what happened, when it comes back (00:00 UTC, the hours to
 * it) and that the local network still works; [onLan] switches the Online page to it. Nothing while the service works. Shown on Home
 * and on the Online page, in the look of Dragon Rage's online banner.
 */
@Composable
fun OnlineQuotaNotice(onLan: (() -> Unit)? = null) {
    val since by OnlineQuota.since.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000)
        }
    }
    if (!OnlineQuota.active(since, now)) return
    val color = Konoha.NarutoOrange
    val hours = OnlineQuota.hoursLeft(since, now)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        // (a dark card of its own, readable on the page's night and on the cream cards alike)
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.13f).compositeOver(Konoha.NightHigh), contentColor = Konoha.ScrollCream),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.online_quota_label).uppercase(), style = MaterialTheme.typography.labelLarge, color = color)
            }
            Text(stringResource(R.string.online_quota_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.online_quota_text, pluralStringResource(R.plurals.online_quota_hours, hours, hours)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.online_quota_lan), style = MaterialTheme.typography.bodyMedium, color = Konoha.LeeGreenLight)
            if (onLan != null) FilledTonalButton(onClick = onLan) { Text(stringResource(R.string.online_quota_use_lan)) }
        }
    }
}

@Composable
private fun Busy(text: String, onCancel: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (onCancel != null) TextButton(onClick = onCancel) { Text(stringResource(R.string.online_cancel)) }
    }
}

@Composable
private fun RankedAndLobbySection(
    prefs: com.dfdx047.hiddenleaf.data.OnlinePrefs.State,
    ready: Boolean,
    serverSet: Boolean,
    fp: () -> String,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var profile by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.RankedProfile?>(null) }
    var leaderboard by remember { mutableStateOf<List<com.dfdx047.hiddenleaf.data.OnlineAdvanced.LeaderboardEntry>>(emptyList()) }
    var loadingRank by remember { mutableStateOf(false) }
    var customLobby by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.CustomLobby?>(null) }
    var lobbyCode by remember { mutableStateOf("") }
    var lobbyMax by remember { mutableStateOf(4) }
    var lobbyErr by remember { mutableStateOf<String?>(null) }

    // Auto-load ranked profile & Top 100 leaderboard when server is ready
    LaunchedEffect(prefs.server, serverSet) {
        if (!serverSet) return@LaunchedEffect
        loadingRank = true
        val srv = Online.cleanServer(prefs.server)
        runCatching {
            profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                context, srv, prefs.name, Online.effectiveInputMode(prefs.inputMode),
            )
        }
        leaderboard = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchLeaderboard(srv, "all", 50)
        loadingRank = false
    }

    // Ranked & Top 100 Global + Custom Lobby (2-8) in official UN5 wood/scroll look, responsive Landscape (2-col) / Portrait (1-col)
    val rankedContent: @Composable () -> Unit = {
        SectionCard(stringResource(R.string.online_ranked_board_title), NinjaIcons.Scroll, accent = Konoha.ChakraAmber) {
            profile?.let { p ->
                com.dfdx047.hiddenleaf.ui.components.NinjaInfoCard(
                    profile = p,
                    onCycleChar = {
                        val list = com.dfdx047.hiddenleaf.ui.components.UN5_CHARACTERS
                        val idx = list.indexOfFirst { it.id == p.favChar }.let { if (it < 0) 0 else (it + 1) % list.size }
                        val nextId = list[idx].id
                        com.dfdx047.hiddenleaf.data.OnlineAdvanced.saveFavChar(context, nextId)
                        profile = p.copy(favChar = nextId)
                        if (serverSet) {
                            scope.launch {
                                runCatching {
                                    profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                        context, Online.cleanServer(prefs.server), prefs.name, Online.effectiveInputMode(prefs.inputMode), nextId,
                                    )
                                }
                            }
                        }
                    },
                )
            }
            com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                title = stringResource(R.string.online_ranked_refresh),
                subtitle = "Top 100 Global · Nível de Maestria por Personagem",
                accent = Konoha.ChakraAmber,
                enabled = serverSet && !loadingRank,
                onClick = {
                    if (!serverSet) return@Un5WoodActionBoard
                    loadingRank = true
                    scope.launch {
                        val srv = Online.cleanServer(prefs.server)
                        runCatching {
                            profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                context, srv, prefs.name, Online.effectiveInputMode(prefs.inputMode),
                            )
                        }
                        leaderboard = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchLeaderboard(srv, "all", 50)
                        loadingRank = false
                    }
                },
            )
            if (leaderboard.isEmpty()) {
                Text(stringResource(R.string.online_ranked_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (e in leaderboard.take(15)) {
                        val tc = rankTierColor(e.tier)
                        val ch = com.dfdx047.hiddenleaf.ui.components.charInfoOf(e.favChar)
                        val medal = when (e.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${e.rank}" }
                        val wr = if (e.wins + e.losses > 0) (e.wins * 100) / (e.wins + e.losses) else 0
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(if (e.rank <= 3) tc.copy(alpha = 0.16f) else com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                                .border(1.dp, if (e.rank <= 3) tc.copy(alpha = 0.6f) else com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BORDER.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(medal, style = MaterialTheme.typography.labelLarge, color = tc, modifier = Modifier.width(30.dp))
                            com.dfdx047.hiddenleaf.ui.components.CharacterCutout(e.favChar, e.favCharLvl, sizeDp = 44)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyMedium, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                                    RankEmblem(e.tier, size = 18)
                                }
                                Text(
                                    "${ch.name} Lv.${e.favCharLvl} · ${e.tier} · ${e.wins}V/${e.losses}D ($wr%)" + (if (e.streak >= 2) " 🔥${e.streak}" else ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Konoha.ScrollCream.copy(alpha = 0.78f),
                                )
                            }
                            Text("${e.points} RP", style = MaterialTheme.typography.titleSmall, color = tc)
                        }
                    }
                }
            }
        }
    }

    val lobbyContent: @Composable () -> Unit = {
        SectionCard(stringResource(R.string.online_custom_lobby_title), NinjaIcons.Shuriken, accent = Konoha.SasukeBlueLight) {
            Text(stringResource(R.string.online_custom_lobby_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val cur = customLobby
            if (cur != null) {
                Text("Lobby #${cur.code} (${cur.members.size}/${cur.maxPlayers})", style = MaterialTheme.typography.titleMedium, color = Konoha.NarutoOrange)
                for (m in cur.members) {
                    Text("• ${m.name} [${m.role.uppercase()}]", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        com.dfdx047.hiddenleaf.data.OnlineAdvanced.leaveLobby(context, Online.cleanServer(prefs.server), cur.code)
                        customLobby = null
                    }
                }) { Text(stringResource(R.string.online_custom_lobby_leave)) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in listOf(2, 4, 6, 8)) {
                        FilterChip(selected = lobbyMax == n, onClick = { lobbyMax = n }, label = { Text("${n}P") })
                    }
                }
                com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                    title = stringResource(R.string.online_custom_lobby_create),
                    subtitle = "Até ${lobbyMax} jogadores · Rei da Mesa · Espectador",
                    accent = Konoha.SasukeBlueLight,
                    enabled = ready && serverSet && prefs.name.isNotBlank(),
                    onClick = {
                        lobbyErr = null
                        scope.launch {
                            runCatching {
                                customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.createLobby(
                                    context, Online.cleanServer(prefs.server), prefs.name, fp(), lobbyMax, true,
                                    Online.effectiveInputMode(prefs.inputMode),
                                )
                            }.onFailure { lobbyErr = it.message }
                        }
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lobbyCode,
                        onValueChange = { lobbyCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.online_code)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            lobbyErr = null
                            scope.launch {
                                runCatching {
                                    customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.joinLobby(
                                        context, Online.cleanServer(prefs.server), lobbyCode, prefs.name, fp(),
                                        Online.effectiveInputMode(prefs.inputMode), spectator = false,
                                    )
                                }.onFailure { lobbyErr = it.message }
                            }
                        },
                        enabled = ready && serverSet && lobbyCode.length == 6,
                    ) { Text(stringResource(R.string.online_custom_lobby_join)) }
                    OutlinedButton(
                        onClick = {
                            lobbyErr = null
                            scope.launch {
                                runCatching {
                                    customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.joinLobby(
                                        context, Online.cleanServer(prefs.server), lobbyCode, prefs.name, fp(),
                                        Online.effectiveInputMode(prefs.inputMode), spectator = true,
                                    )
                                }.onFailure { lobbyErr = it.message }
                            }
                        },
                        enabled = ready && serverSet && lobbyCode.length == 6,
                    ) { Text(stringResource(R.string.online_custom_lobby_spectate)) }
                }
                lobbyErr?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    if (cfg.screenWidthDp >= 700) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1.15f)) { rankedContent() }
            Column(Modifier.weight(0.85f)) { lobbyContent() }
        }
    } else {
        rankedContent()
        lobbyContent()
    }
}

internal fun rankTierColor(tier: String): Color = when (tier) {
    "Kage" -> Color(0xFFFF6D00)
    "Sannin" -> Color(0xFFFFD54F)
    "ANBU" -> Color(0xFFEF5350)
    "Jonin" -> Color(0xFFAB47BC)
    "Chunin" -> Color(0xFF42A5F5)
    else -> Color(0xFF66BB6A)
}

@Composable
internal fun RankEmblem(tier: String, size: Int = 34) {
    val c = rankTierColor(tier)
    val sym = when (tier) {
        "Kage" -> "♛"
        "Sannin" -> "★"
        "ANBU" -> "◈"
        "Jonin" -> "✦"
        "Chunin" -> "⚔"
        else -> "🍃"
    }
    Box(
        Modifier.size(size.dp)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.22f))
            .border(1.5.dp, c, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(sym, color = c, fontSize = (size * 0.46f).sp)
    }
}
