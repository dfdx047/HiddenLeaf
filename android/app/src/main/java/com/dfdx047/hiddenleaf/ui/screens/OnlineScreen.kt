package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
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
    var showCasualScreen by rememberSaveable { mutableStateOf(false) }
    var showRankedScreen by rememberSaveable { mutableStateOf(false) }
    val fp = { Online.fingerprint(context, vm.paths) }

    if (showCasualScreen && !prefs.lan) {
        CasualDedicatedScreen(
            vm = vm,
            lobby = lobby,
            prefs = prefs,
            state = state,
            waiting = waiting,
            ready = ready,
            serverSet = serverSet,
            modsOn = modsOn,
            onOpenMods = onOpenMods,
            fp = fp,
            onBack = { showCasualScreen = false },
        )
        return
    }

    if (showRankedScreen && !prefs.lan) {
        RankedDedicatedScreen(
            vm = vm,
            lobby = lobby,
            prefs = prefs,
            state = state,
            ready = ready,
            serverSet = serverSet,
            modsOn = modsOn,
            fp = fp,
            onBack = { showRankedScreen = false },
        )
        return
    }

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
        else {
            SectionCard(stringResource(R.string.online_play_title), NinjaIcons.Shuriken, accent = Konoha.NarutoOrange) {
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
                if (modsOn) {
                    ModsOnly(onOpenMods)
                } else {
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Modo Casual",
                        subtitle = "Partida Rápida · Treinar na Fila · Sala com Amigo · Sala Personalizada & Espectador" +
                            (if (waiting > 0) " · $waiting na fila" else ""),
                        accent = Konoha.NarutoOrange,
                        enabled = serverSet,
                        onClick = { showCasualScreen = true },
                    )
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Modo Ranqueado",
                        subtitle = "Jogar Ranqueada · Top 100 Global · Genin até Kage · Maestria de Personagem",
                        accent = Konoha.ChakraAmber,
                        enabled = serverSet,
                        onClick = { showRankedScreen = true },
                    )
                }
            }
        }

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

/** Quick match: searching, how long so far, optional 15s fallback to accept any controller, and a way out. */
@Composable
private fun QuickSearching(
    since: Long,
    inputMode: String = "any",
    onAcceptAny: (() -> Unit)? = null,
    onCancel: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val s = ((now - since) / 1000).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Busy(stringResource(R.string.online_quick_searching, "%d:%02d".format(s / 60, s % 60)), onCancel)
        if (inputMode != "any" && s >= 15 && onAcceptAny != null) {
            FilledTonalButton(onClick = onAcceptAny, modifier = Modifier.fillMaxWidth()) {
                Text("Demorando? Aceitar qualquer controle")
            }
        }
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CasualDedicatedScreen(
    vm: AppViewModel,
    lobby: OnlineLobby,
    prefs: com.dfdx047.hiddenleaf.data.OnlinePrefs.State,
    state: LobbyState,
    waiting: Int,
    ready: Boolean,
    serverSet: Boolean,
    modsOn: Boolean,
    onOpenMods: () -> Unit,
    fp: () -> String,
    onBack: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val idle = state is LobbyState.Idle || state is LobbyState.Failed
    val can = ready && serverSet && !modsOn && prefs.name.isNotBlank()
    val effectiveMode = Online.effectiveInputMode(prefs.inputMode)

    val leftCol: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("Filtro de Controle (Casual)", NinjaIcons.Tools, accent = Konoha.SasukeBlueLight) {
                Text(
                    "Escolha se prefere enfrentar apenas quem joga na Tela, no Controle ou Todos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((key, label) in listOf("touch" to "Só Tela", "gamepad" to "Só Controle", "any" to "Todos")) {
                        FilterChip(
                            selected = prefs.inputMode == key || (prefs.inputMode == "auto" && effectiveMode == key),
                            onClick = { lobby.update(prefs.copy(inputMode = key)) },
                            enabled = idle,
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }

            SectionCard("Partida Rápida & Amigos (1v1)", NinjaIcons.Shuriken, accent = Konoha.NarutoOrange) {
                when (val s = state) {
                    is LobbyState.Waiting -> RoomCode(s.code) { lobby.cancel() }
                    is LobbyState.Searching -> QuickSearching(
                        since = s.since,
                        inputMode = effectiveMode,
                        onAcceptAny = {
                            lobby.cancel()
                            lobby.update(prefs.copy(inputMode = "any"))
                            lobby.quick(fp(), ranked = false, inputModeOverride = "any") { vm.play() }
                        },
                        onCancel = { lobby.cancel() },
                    )
                    is LobbyState.Working -> Busy(stringResource(s.step)) { lobby.cancel() }
                    is LobbyState.Starting -> Busy(stringResource(R.string.online_starting, s.peer), null)
                    else -> if (modsOn) ModsOnly(onOpenMods) else {
                        com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                            title = stringResource(R.string.online_quick),
                            subtitle = "Fila casual · Controle: ${when (effectiveMode) { "touch" -> "Só Tela"; "gamepad" -> "Só Controle"; else -> "Todos" }}" +
                                (if (waiting > 0) " · $waiting na fila" else ""),
                            accent = Konoha.NarutoOrange,
                            enabled = can,
                            onClick = { lobby.quick(fp(), ranked = false) { vm.play() } },
                        )
                        OutlinedButton(
                            onClick = {
                                com.dfdx047.hiddenleaf.engine.TrainingQueue.arm(
                                    vm.paths,
                                    com.dfdx047.hiddenleaf.engine.TrainingQueue.Request(
                                        server = Online.cleanServer(prefs.server),
                                        name = prefs.name,
                                        fp = fp(),
                                        inputMode = effectiveMode,
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
                        Text(stringResource(R.string.online_or_friend), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { lobby.create(fp()) { vm.play() } }, enabled = can, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.online_create))
                        }
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text(stringResource(R.string.online_code)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { lobby.join(code, fp()) { vm.play() } },
                            enabled = can && code.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.online_join)) }
                        if (s is LobbyState.Failed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(s.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { lobby.dismiss() }) { Text(stringResource(R.string.online_dismiss)) }
                            }
                        }
                    }
                }
            }
        }
    }

    val rightCol: @Composable () -> Unit = {
        CustomLobbyCard(vm.paths, lobby, prefs, ready, serverSet, fp) { vm.play() }
    }

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Modo Casual", style = MaterialTheme.typography.headlineSmall, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                Text("Partida Rápida · Amigos 1v1 · Sala Personalizada & Espectador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (cfg.screenWidthDp >= 680) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(0.95f)) { leftCol() }
                Column(Modifier.weight(1.05f)) { rightCol() }
            }
        } else {
            leftCol()
            rightCol()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankedDedicatedScreen(
    vm: AppViewModel,
    lobby: OnlineLobby,
    prefs: com.dfdx047.hiddenleaf.data.OnlinePrefs.State,
    state: LobbyState,
    ready: Boolean,
    serverSet: Boolean,
    modsOn: Boolean,
    fp: () -> String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.RankedProfile?>(null) }
    var leaderboard by remember { mutableStateOf<List<com.dfdx047.hiddenleaf.data.OnlineAdvanced.LeaderboardEntry>>(emptyList()) }
    var lbMode by remember { mutableStateOf("all") }
    var loadingRank by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var avatarVersion by remember { mutableStateOf(0) }
    var inspectedPlayerId by remember { mutableStateOf<String?>(null) }
    var inspectedProfile by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.PublicPlayerProfile?>(null) }
    var loadingInspected by remember { mutableStateOf(false) }
    var profilePublic by remember { mutableStateOf(com.dfdx047.hiddenleaf.data.OnlineAdvanced.savedProfilePublic(context)) }
    val pickAvatar = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            if (com.dfdx047.hiddenleaf.data.OnlineAdvanced.saveCustomAvatarFromUri(context, uri)) {
                avatarVersion++
                showAvatarDialog = false
                if (serverSet) {
                    scope.launch {
                        runCatching {
                            profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                context, Online.cleanServer(prefs.server), prefs.name, Online.effectiveInputMode(prefs.inputMode),
                                profilePublic = profilePublic,
                            )
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(inspectedPlayerId) {
        val pid = inspectedPlayerId ?: return@LaunchedEffect
        loadingInspected = true
        inspectedProfile = null
        val srv = Online.cleanServer(prefs.server)
        inspectedProfile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchPublicProfile(srv, pid)
        loadingInspected = false
    }
    val canPlay = ready && serverSet && !modsOn && prefs.name.isNotBlank()
    val effectiveMode = Online.effectiveInputMode(prefs.inputMode)

    LaunchedEffect(prefs.server, serverSet, lbMode) {
        if (!serverSet) return@LaunchedEffect
        loadingRank = true
        val srv = Online.cleanServer(prefs.server)
        runCatching {
            profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                context, srv, prefs.name, Online.effectiveInputMode(prefs.inputMode),
            ).also { profilePublic = it.profilePublic }
        }
        leaderboard = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchLeaderboard(srv, lbMode, 50)
        loadingRank = false
    }

    val leftCol: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            profile?.let { p ->
                com.dfdx047.hiddenleaf.ui.components.NinjaInfoCard(
                    profile = p,
                    avatarVersion = avatarVersion,
                    onCycleChar = { showAvatarDialog = true },
                )
                Row(
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                        .border(1.dp, com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BORDER.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (profilePublic) "Perfil público" else "Perfil privado",
                            style = MaterialTheme.typography.labelLarge,
                            color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD,
                        )
                        Text(
                            if (profilePublic) "Outros jogadores podem ver sua foto, vitórias e maestrias"
                            else "Seu histórico detalhado e foto ficam ocultos no ranking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = profilePublic,
                        onCheckedChange = { pub ->
                            profilePublic = pub
                            com.dfdx047.hiddenleaf.data.OnlineAdvanced.saveProfilePublic(context, pub)
                            if (serverSet) {
                                scope.launch {
                                    runCatching {
                                        profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                            context, Online.cleanServer(prefs.server), prefs.name,
                                            Online.effectiveInputMode(prefs.inputMode), p.favChar, profilePublic = pub,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
            if (showAvatarDialog) {
                val p = profile
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showAvatarDialog = false },
                    title = { Text("Avatar & Personagem Principal") },
                    text = {
                        Column(
                            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Escolha uma imagem da sua galeria ou um personagem do banco de artes:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { pickAvatar.launch(arrayOf("image/*")) }) {
                                    Text("Imagem da Galeria", maxLines = 1)
                                }
                                if (com.dfdx047.hiddenleaf.data.OnlineAdvanced.hasCustomAvatar(context)) {
                                    OutlinedButton(onClick = {
                                        com.dfdx047.hiddenleaf.data.OnlineAdvanced.clearCustomAvatar(context)
                                        avatarVersion++
                                        if (serverSet) {
                                            scope.launch {
                                                runCatching {
                                                    profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                                        context, Online.cleanServer(prefs.server), prefs.name,
                                                        Online.effectiveInputMode(prefs.inputMode), profilePublic = profilePublic,
                                                    )
                                                }
                                            }
                                        }
                                    }) {
                                        Text("Usar Arte do Ninja", maxLines = 1)
                                    }
                                }
                            }
                            com.dfdx047.hiddenleaf.ui.components.UN5_CHARACTERS.chunked(4).forEach { rowChars ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (ch in rowChars) {
                                        val m = p?.mastery?.get(ch.id)
                                        val sel = p?.favChar == ch.id
                                        Column(
                                            Modifier.weight(1f)
                                                .clip(MaterialTheme.shapes.small)
                                                .background(if (sel) ch.primary.copy(alpha = 0.25f) else Color.Transparent)
                                                .border(
                                                    width = if (sel) 2.dp else 1.dp,
                                                    color = if (sel) com.dfdx047.hiddenleaf.ui.components.UN5_GOLD else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = MaterialTheme.shapes.small,
                                                )
                                                .clickable {
                                                    com.dfdx047.hiddenleaf.data.OnlineAdvanced.saveFavChar(context, ch.id)
                                                    if (p != null) {
                                                        profile = p.copy(favChar = ch.id, favCharLvl = m?.lvl ?: 1, favCharXp = m?.xp ?: 0)
                                                    }
                                                    showAvatarDialog = false
                                                    if (serverSet) {
                                                        scope.launch {
                                                            runCatching {
                                                                profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                                                    context, Online.cleanServer(prefs.server), prefs.name, Online.effectiveInputMode(prefs.inputMode), ch.id,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            com.dfdx047.hiddenleaf.ui.components.CharacterCutout(ch.id, m?.lvl ?: 1, sizeDp = 50)
                                            Text(ch.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAvatarDialog = false }) { Text("Fechar") }
                    },
                )
            }
            // Strict Touch vs Gamepad separation directly inside Ranked
            SectionCard("Filtro de Controle (Ranqueada)", NinjaIcons.Tools, accent = Konoha.SasukeBlueLight) {
                Text(
                    "Jogue apenas contra quem usa o mesmo controle. Se demorar 15s, você pode liberar para qualquer um.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((key, label) in listOf("touch" to "Só Tela", "gamepad" to "Só Controle", "any" to "Todos")) {
                        FilterChip(
                            selected = prefs.inputMode == key || (prefs.inputMode == "auto" && effectiveMode == key),
                            onClick = { lobby.update(prefs.copy(inputMode = key)) },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }
            when (val s = state) {
                is LobbyState.Searching -> QuickSearching(
                    since = s.since,
                    inputMode = effectiveMode,
                    onAcceptAny = {
                        lobby.cancel()
                        lobby.update(prefs.copy(inputMode = "any"))
                        lobby.quick(fp(), ranked = true, inputModeOverride = "any") { vm.play() }
                    },
                    onCancel = { lobby.cancel() },
                )
                is LobbyState.Working -> Busy(stringResource(s.step)) { lobby.cancel() }
                is LobbyState.Starting -> Busy(stringResource(R.string.online_starting, s.peer), null)
                else -> {
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Jogar Ranqueada",
                        subtitle = "Fila competitiva separada · Modo: ${when (effectiveMode) { "touch" -> "Só Tela"; "gamepad" -> "Só Controle"; else -> "Qualquer um" }}",
                        accent = Konoha.NarutoOrange,
                        enabled = canPlay,
                        onClick = { lobby.quick(fp(), ranked = true) { vm.play() } },
                    )
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Treinar na Fila Ranqueada",
                        subtitle = "Pula direto para Seleção de Personagem enquanto busca",
                        accent = Konoha.ChakraAmber,
                        enabled = canPlay,
                        onClick = {
                            com.dfdx047.hiddenleaf.engine.TrainingQueue.arm(
                                vm.paths,
                                com.dfdx047.hiddenleaf.engine.TrainingQueue.Request(
                                    server = Online.cleanServer(prefs.server),
                                    name = prefs.name,
                                    fp = "${fp()}#ranked",
                                    inputMode = effectiveMode,
                                    delay = prefs.delay,
                                    relay = prefs.relay,
                                )
                            )
                            vm.play()
                        },
                    )
                    if (s is LobbyState.Failed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(s.message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { lobby.dismiss() }) { Text(stringResource(R.string.online_dismiss)) }
                        }
                    }
                }
            }
        }
    }

    val rightCol: @Composable () -> Unit = {
        SectionCard("Top 100 Global · Maestria", NinjaIcons.Scroll, accent = Konoha.ChakraAmber) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for ((m, lbl) in listOf("all" to "Todos", "touch" to "Tela", "gamepad" to "Controle")) {
                        FilterChip(
                            selected = lbMode == m,
                            onClick = { lbMode = m },
                            label = { Text(lbl, maxLines = 1) },
                        )
                    }
                }
                OutlinedButton(
                    enabled = serverSet && !loadingRank,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        loadingRank = true
                        scope.launch {
                            val srv = Online.cleanServer(prefs.server)
                            runCatching {
                                profile = com.dfdx047.hiddenleaf.data.OnlineAdvanced.syncProfile(
                                    context, srv, prefs.name, Online.effectiveInputMode(prefs.inputMode),
                                )
                            }
                            leaderboard = com.dfdx047.hiddenleaf.data.OnlineAdvanced.fetchLeaderboard(srv, lbMode, 50)
                            loadingRank = false
                        }
                    },
                ) {
                    Text(
                        if (loadingRank) "Atualizando..." else stringResource(R.string.online_ranked_refresh),
                        maxLines = 1,
                    )
                }
            }
            if (leaderboard.isEmpty()) {
                Text(stringResource(R.string.online_ranked_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (e in leaderboard.take(20)) {
                        val tc = rankTierColor(e.tier)
                        val ch = com.dfdx047.hiddenleaf.ui.components.charInfoOf(e.favChar)
                        val medal = when (e.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#${e.rank}" }
                        val wr = if (e.wins + e.losses > 0) (e.wins * 100) / (e.wins + e.losses) else 0
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(if (e.rank <= 3) tc.copy(alpha = 0.16f) else com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                                .border(1.dp, if (e.rank <= 3) tc.copy(alpha = 0.65f) else com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BORDER.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                                .clickable { inspectedPlayerId = e.playerId }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(medal, style = MaterialTheme.typography.labelLarge, color = tc, modifier = Modifier.width(30.dp))
                            com.dfdx047.hiddenleaf.ui.components.CharacterCutout(
                                e.favChar, e.favCharLvl, sizeDp = 46,
                                avatarB64 = if (e.profilePublic) e.avatarB64 else null,
                            )
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyMedium, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                                    RankEmblem(e.tier, size = 18)
                                    if (!e.profilePublic) Text("🔒", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    if (e.profilePublic) {
                                        "${ch.name} Lv.${e.favCharLvl} · ${e.tier} · ${e.wins}V/${e.losses}D ($wr%)" + (if (e.streak >= 2) " 🔥${e.streak}" else "")
                                    } else {
                                        "${e.tier} · Perfil privado"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Konoha.ScrollCream.copy(alpha = 0.8f),
                                )
                            }
                            Text("${e.points} RP", style = MaterialTheme.typography.titleSmall, color = tc)
                        }
                    }
                }
            }
        }
    }

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Modo Ranqueado", style = MaterialTheme.typography.headlineSmall, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                Text("Genin → Kage · Maestria de Personagem · Top 100 Global", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (cfg.screenWidthDp >= 680) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(0.95f)) { leftCol() }
                Column(Modifier.weight(1.05f)) { rightCol() }
            }
        } else {
            leftCol()
            rightCol()
        }
    }

    if (inspectedPlayerId != null) {
        val ip = inspectedProfile
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { inspectedPlayerId = null },
            title = {
                Text(ip?.name ?: "Perfil Ninja", color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
            },
            text = {
                if (loadingInspected) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Carregando perfil...")
                    }
                } else if (ip == null) {
                    Text("Não foi possível carregar o perfil deste ninja.")
                } else if (!ip.profilePublic) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RankEmblem(ip.tier, size = 28)
                            Text("#${ip.rank} · ${ip.tier} · ${ip.points} RP", style = MaterialTheme.typography.titleSmall, color = rankTierColor(ip.tier))
                        }
                        Text(
                            "🔒 Este jogador optou por manter o perfil privado. Estatísticas detalhadas, foto personalizada e maestrias estão ocultas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val tc = rankTierColor(ip.tier)
                    val wr = if (ip.matches > 0) (ip.wins * 100) / ip.matches else 0
                    val modeLabel = when (ip.inputMode) { "touch" -> "Tela"; "gamepad" -> "Controle"; else -> "Todos" }
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.dfdx047.hiddenleaf.ui.components.CharacterCutout(
                                ip.favChar, ip.favCharLvl, sizeDp = 78,
                                avatarB64 = ip.avatarB64,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RankEmblem(ip.tier, size = 22)
                                    Text("#${ip.rank} · ${ip.tier}", style = MaterialTheme.typography.titleSmall, color = tc)
                                }
                                Text("${ip.points} RP", style = MaterialTheme.typography.titleMedium, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                                Text("Controle: $modeLabel" + if (ip.streak >= 2) " · 🔥 ${ip.streak} seguidas" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Detailed match stats
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((lbl, valStr, col) in listOf(
                                Triple("Partidas", "${ip.matches}", com.dfdx047.hiddenleaf.ui.components.UN5_GOLD),
                                Triple("Vitórias", "${ip.wins}", Konoha.LeeGreenLight),
                                Triple("Derrotas", "${ip.losses}", MaterialTheme.colorScheme.error),
                                Triple("Winrate", "$wr%", Konoha.SasukeBlueLight),
                            )) {
                                Column(
                                    Modifier.weight(1f)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                                        .border(1.dp, com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BORDER.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(valStr, style = MaterialTheme.typography.titleSmall, color = col, maxLines = 1)
                                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        // Characters played & per-character mastery breakdown
                        Text("Personagens e Maestria", style = MaterialTheme.typography.titleSmall, color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD)
                        val sortedMastery = ip.mastery.entries.sortedByDescending { it.value.xp }
                        if (sortedMastery.isEmpty()) {
                            Text("Nenhuma maestria registrada ainda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for ((cid, m) in sortedMastery) {
                                    val ch = com.dfdx047.hiddenleaf.ui.components.charInfoOf(cid)
                                    val xpInLvl = m.xp % 200
                                    val frac = (xpInLvl / 200f).coerceIn(0.05f, 1f)
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .background(com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                                            .border(1.dp, ch.primary.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        com.dfdx047.hiddenleaf.ui.components.CharacterCutout(cid, m.lvl, sizeDp = 42)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("${ch.name} · Lv.${m.lvl}", style = MaterialTheme.typography.labelLarge, color = ch.primary)
                                                Text("${m.matches} lutas · ${m.xp} XP", style = MaterialTheme.typography.labelSmall, color = Konoha.ScrollCream.copy(alpha = 0.78f))
                                            }
                                            Box(
                                                Modifier.fillMaxWidth().height(5.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF3A2416)),
                                            ) {
                                                Box(
                                                    Modifier.fillMaxWidth(frac).height(5.dp)
                                                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep, com.dfdx047.hiddenleaf.ui.components.UN5_GOLD))),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedPlayerId = null }) { Text("Fechar") }
            },
        )
    }
}

@Composable
private fun CustomLobbyCard(
    paths: com.dfdx047.hiddenleaf.data.GamePaths,
    lobby: OnlineLobby,
    prefs: com.dfdx047.hiddenleaf.data.OnlinePrefs.State,
    ready: Boolean,
    serverSet: Boolean,
    fp: () -> String,
    start: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customLobby by remember { mutableStateOf<com.dfdx047.hiddenleaf.data.OnlineAdvanced.CustomLobby?>(null) }
    var lobbyCode by remember { mutableStateOf("") }
    var lobbyMax by remember { mutableStateOf(4) }
    var lobbyErr by remember { mutableStateOf<String?>(null) }
    var spectatingCode by remember { mutableStateOf<String?>(null) }

    // Live polling while inside a Casual Custom Lobby so all friends see joins, role swaps & match start in real time
    LaunchedEffect(customLobby?.code, prefs.server) {
        val code = customLobby?.code ?: return@LaunchedEffect
        val srv = Online.cleanServer(prefs.server)
        while (true) {
            delay(1500)
            runCatching {
                customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.pollLobby(context, srv, code)
            }.onFailure {
                if (it.message == "no room") customLobby = null
            }
        }
    }

    SectionCard("Sala Personalizada & Espectador (Casual)", NinjaIcons.Shuriken, accent = Konoha.SasukeBlueLight) {
        Text(
            "Exclusivo para Partida Casual: entre com amigos na mesma sala, escolha quem luta (P1/P2) e quem assiste ao vivo (Espectador), e troque de papel quando quiser.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val cur = customLobby
        if (cur != null) {
            val isHost = cur.playerId == cur.hostId
            val myMember = cur.members.firstOrNull { it.id == cur.playerId }
            val p1 = cur.members.firstOrNull { it.role == "p1" }
            val p2 = cur.members.firstOrNull { it.role == "p2" }
            val srv = Online.cleanServer(prefs.server)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sala #${cur.code.chunked(3).joinToString(" ")}", style = MaterialTheme.typography.titleLarge, color = Konoha.NarutoOrange)
                    Text("${cur.members.size}/${cur.maxPlayers} participantes · Casual com Espectador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        com.dfdx047.hiddenleaf.data.OnlineAdvanced.leaveLobby(context, srv, cur.code)
                        customLobby = null
                        spectatingCode = null
                    }
                }) { Text(stringResource(R.string.online_custom_lobby_leave)) }
            }

            // Interactive role switcher for every member in the room
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (m in cur.members) {
                    val roleLabel = when (m.role) {
                        "p1" -> "JOGADOR 1"
                        "p2" -> "JOGADOR 2"
                        "spectator" -> "ESPECTADOR"
                        else -> "NA FILA"
                    }
                    val roleColor = when (m.role) {
                        "p1" -> Konoha.NarutoOrange
                        "p2" -> Konoha.SasukeBlueLight
                        "spectator" -> Konoha.ChakraAmber
                        else -> Konoha.LeeGreen
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(com.dfdx047.hiddenleaf.ui.components.UN5_SCROLL_BG)
                            .border(1.dp, roleColor.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.name + (if (m.id == cur.hostId) " ★" else "") + (if (m.id == cur.playerId) " (Você)" else ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.dfdx047.hiddenleaf.ui.components.UN5_GOLD,
                            )
                            Text(roleLabel, style = MaterialTheme.typography.labelSmall, color = roleColor)
                        }
                        if (isHost) {
                            if (m.role != "p1") {
                                FilterChip(selected = false, onClick = {
                                    scope.launch { runCatching { customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.setLobbyRole(context, srv, cur.code, m.id, "p1") } }
                                }, label = { Text("P1") })
                            }
                            if (m.role != "p2") {
                                FilterChip(selected = false, onClick = {
                                    scope.launch { runCatching { customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.setLobbyRole(context, srv, cur.code, m.id, "p2") } }
                                }, label = { Text("P2") })
                            }
                            if (m.role != "spectator") {
                                FilterChip(selected = false, onClick = {
                                    scope.launch { runCatching { customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.setLobbyRole(context, srv, cur.code, m.id, "spectator") } }
                                }, label = { Text("Assistir") })
                            }
                        } else if (m.id == cur.playerId) {
                            val wantRole = if (m.role == "spectator") "play" else "spectator"
                            FilterChip(
                                selected = false,
                                onClick = {
                                    scope.launch { runCatching { customLobby = com.dfdx047.hiddenleaf.data.OnlineAdvanced.setLobbyRole(context, srv, cur.code, m.id, wantRole) } }
                                },
                                label = { Text(if (m.role == "spectator") "Quero Jogar" else "Virar Espectador") },
                            )
                        }
                    }
                }
            }

            // Linked Casual Match launch & live Spectator join
            if (isHost && p1 != null && p2 != null) {
                com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                    title = "Iniciar Luta (${p1.name} vs ${p2.name})",
                    subtitle = "Abre a partida casual interligada para P1, P2 e Espectadores",
                    accent = Konoha.NarutoOrange,
                    enabled = ready && serverSet,
                    onClick = {
                        lobby.createForLobby(
                            fp = fp(),
                            onCode = { roomCode ->
                                scope.launch {
                                    runCatching { com.dfdx047.hiddenleaf.data.OnlineAdvanced.pollLobby(context, srv, cur.code, activeRoom = roomCode) }
                                }
                            },
                            start = start,
                        )
                    },
                )
            } else if (cur.activeRoom != null) {
                val active = cur.activeRoom
                if (myMember?.role == "p2") {
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Entrar na Luta Agora (P2)",
                        subtitle = "Sala de luta #$active pronta · Toque para lutar",
                        accent = Konoha.LeeGreen,
                        enabled = ready,
                        onClick = { lobby.join(active, fp(), start) },
                    )
                } else {
                    val matchTitle = listOfNotNull(p1?.name, p2?.name).joinToString(" vs ").ifEmpty { "Sala #$active" }
                    com.dfdx047.hiddenleaf.ui.components.Un5WoodActionBoard(
                        title = "Assistir Luta Ao Vivo (Sala #$active)",
                        subtitle = "Abre o jogo em tempo real como Espectador (somente leitura)",
                        accent = Konoha.ChakraAmber,
                        enabled = ready,
                        onClick = {
                            spectatingCode = active
                            Online.writeSpectatorSession(
                                paths,
                                code = active,
                                me = prefs.name.ifBlank { "Espectador" },
                                matchLabel = matchTitle,
                                relayUrl = com.dfdx047.hiddenleaf.data.OnlineAdvanced.spectatorRelayUrl(srv, active),
                            )
                            start()
                        },
                    )
                }
            } else {
                Text(
                    if (p1 == null || p2 == null) "Aguardando definir Jogador 1 e Jogador 2 na sala..."
                    else "Aguardando o anfitrião iniciar a luta (${p1.name} vs ${p2.name})...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            spectatingCode?.let { sc ->
                Text(
                    "Conectado como Espectador na sala #$sc (${com.dfdx047.hiddenleaf.data.OnlineAdvanced.spectatorRelayUrl(srv, sc)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Konoha.ChakraAmber,
                )
            }
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
            // Full-width room code field + balanced Entrar / Assistir row below so it NEVER clips on any DPI or screen width
            OutlinedTextField(
                value = lobbyCode,
                onValueChange = { lobbyCode = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text(stringResource(R.string.online_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.online_custom_lobby_spectate)) }
            }
            lobbyErr?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
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
