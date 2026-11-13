package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.dfdx047.hiddenleaf.data.ServerTest
import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha

/**
 * Online: two players, each on their phone. One creates a room and tells the other its six-digit code; the other joins with it. The
 * game then starts on both phones as one match: the room's creator is player 1, the other player 2 (data/Online.kt, the engine's
 * recomp/android/host/online).
 *
 * At the top the player picks how the two phones meet: through the rooms service on the internet, or on the same Wi-Fi (data/Lan.kt),
 * where one creates a match and the other picks it from the list of matches heard on the network. The match itself is the same.
 */
@Composable
fun OnlineScreen(vm: AppViewModel) {
    val lobby: OnlineLobby = viewModel()
    val prefs by lobby.prefs.state.collectAsStateWithLifecycle()
    val state by lobby.state.collectAsStateWithLifecycle()
    val test by lobby.test.collectAsStateWithLifecycle()
    val install by vm.install.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ready = install is InstallState.Installed && vm.engineAvailable
    val serverSet = prefs.server.startsWith("https://") && !Online.isPlaceholder(prefs.server)
    val idle = state is LobbyState.Idle || state is LobbyState.Failed
    var code by rememberSaveable { mutableStateOf("") }
    val fp = { Online.fingerprint(context, vm.paths) }

    // (a local match or search stops with the page: nobody would see the other player arrive)
    DisposableEffect(lobby) { onDispose { lobby.leaveScreen() } }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_online), stringResource(R.string.online_subtitle))

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

        if (prefs.lan) LanPlay(lobby, ready, fp) { vm.play() }
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
                is LobbyState.Working -> Busy(stringResource(s.step)) { lobby.cancel() }
                is LobbyState.Starting -> Busy(stringResource(R.string.online_starting, s.peer), null)
                else -> {
                    Button(
                        onClick = { lobby.create(fp()) { vm.play() } },
                        enabled = ready && serverSet && prefs.name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.online_create)) }
                    Text(stringResource(R.string.online_or_join), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            enabled = ready && serverSet && code.length == 6 && prefs.name.isNotBlank(),
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
private fun LanPlay(lobby: OnlineLobby, ready: Boolean, fp: () -> String, start: () -> Unit) {
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
            else -> {
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

@Composable
private fun Busy(text: String, onCancel: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (onCancel != null) TextButton(onClick = onCancel) { Text(stringResource(R.string.online_cancel)) }
    }
}
