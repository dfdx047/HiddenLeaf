package com.dfdx047.hiddenleaf.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.FileMod
import com.dfdx047.hiddenleaf.data.InstallState
import com.dfdx047.hiddenleaf.data.formatBytes
import com.dfdx047.hiddenleaf.ui.components.EmptyState
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha

/**
 * The mods over the base game. A mod is added from the ISO of the game with the mod applied; the app keeps only the
 * files that differ from the base. Data mods (translations, voices, models) stack; a mod that changes the code (such
 * as Ultimate Ninja 6) is one at a time and needs its code pack in the engine.
 */
@Composable
fun ModsScreen(vm: AppViewModel) {
    val mods by vm.fileMods.collectAsStateWithLifecycle()
    val install by vm.install.collectAsStateWithLifecycle()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importModIso(uri)
    }
    var deleting by remember { mutableStateOf<FileMod?>(null) }
    val base = install is InstallState.Installed

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_mods), stringResource(R.string.mods_subtitle))

        SectionCard(stringResource(R.string.mods_how_title), Icons.Rounded.Extension, accent = Konoha.LeeGreen) {
            Text(stringResource(R.string.mods_how), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { pick.launch(arrayOf("*/*")) }, enabled = base) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.mods_add_iso))
            }
            if (!base) {
                Text(stringResource(R.string.mods_need_base), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        if (mods.isEmpty()) {
            EmptyState(Icons.Rounded.Extension, stringResource(R.string.mods_empty_title), stringResource(R.string.mods_empty))
        } else {
            mods.forEachIndexed { i, m ->
                ModCard(
                    m, first = i == 0, last = i == mods.lastIndex,
                    onToggle = { vm.setModEnabled(m, it) },
                    onUp = { vm.moveMod(m, -1) }, onDown = { vm.moveMod(m, 1) },
                    onDelete = { deleting = m },
                )
            }
            Text(stringResource(R.string.mods_order), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    deleting?.let { m ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.mod_delete_q, m.name)) },
            text = { Text(stringResource(R.string.mod_delete_text)) },
            confirmButton = { TextButton(onClick = { deleting = null; vm.deleteMod(m) }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ModCard(
    m: FileMod, first: Boolean, last: Boolean,
    onToggle: (Boolean) -> Unit, onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (m.enabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.mod_info, m.files, formatBytes(m.bytes)) + if (m.boot.isNotEmpty()) " · ${m.boot}" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column {
                    IconButton(onClick = onUp, enabled = !first) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
                    IconButton(onClick = onDown, enabled = !last) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
                }
                Switch(checked = m.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (m.changesCode) {
                    AssistChip(
                        onClick = {}, label = { Text(stringResource(R.string.mod_kind_code)) },
                        leadingIcon = { Icon(Icons.Rounded.Code, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Konoha.SasukeBlueLight, leadingIconContentColor = Konoha.SasukeBlueLight),
                    )
                } else {
                    AssistChip(
                        onClick = {}, label = { Text(stringResource(R.string.mod_kind_data)) },
                        leadingIcon = { Icon(Icons.Rounded.Translate, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Konoha.LeeGreenLight, leadingIconContentColor = Konoha.LeeGreenLight),
                    )
                }
            }
            if (m.changesCode && !m.pack) {
                Text(
                    stringResource(R.string.mod_no_pack),
                    style = MaterialTheme.typography.bodySmall, color = Konoha.ChakraAmber,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}
