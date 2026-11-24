package com.dfdx047.hiddenleaf.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.SaveManager
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The memory card: what is on it, in and out as a .ps2 card or a folder, with a backup before anything comes in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SavesCard(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { SaveManager(context.applicationContext, vm.paths) }
    var changes by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var confirm100 by remember { mutableStateOf(false) }
    var saves by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var backups by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    LaunchedEffect(changes) {
        val (s, b) = withContext(Dispatchers.IO) {
            runCatching { manager.saves() }.getOrDefault(emptyList()) to
                runCatching { manager.backupList() }.getOrDefault(emptyList())
        }
        saves = s
        backups = b
    }
    val done = stringResource(R.string.saves_done)
    val done100 = stringResource(R.string.saves_100_done)
    val failed = stringResource(R.string.saves_failed)

    fun run(work: () -> Int) {
        scope.launch {
            status = runCatching { withContext(Dispatchers.IO) { work() } }
                .fold({ done.format(it) }, { failed.format(it.message ?: it.javaClass.simpleName) })
            changes++
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run { manager.importFile(uri) }
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) run { manager.importFolder(uri) }
    }
    val saveCard = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) run { manager.exportCard(uri); saves.size }
    }
    val saveFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) run { manager.exportFolder(uri) }
    }

    SectionCard(stringResource(R.string.saves_title), Icons.Rounded.Save) {
        if (saves.isEmpty()) {
            Text(stringResource(R.string.saves_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            saves.forEach { (name, kb) -> Text("$name · $kb KB", style = MaterialTheme.typography.bodySmall) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { confirm100 = true }) { Text(stringResource(R.string.saves_install_100)) }
            OutlinedButton(onClick = { pickFile.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.saves_import_file)) }
            OutlinedButton(onClick = { pickFolder.launch(null) }) { Text(stringResource(R.string.saves_import_folder)) }
            OutlinedButton(onClick = { saveCard.launch("HiddenLeaf-Mcd001.ps2") }, enabled = saves.isNotEmpty()) { Text(stringResource(R.string.saves_export_file)) }
            OutlinedButton(onClick = { saveFolder.launch(null) }, enabled = saves.isNotEmpty()) { Text(stringResource(R.string.saves_export_folder)) }
        }
        Text(
            stringResource(R.string.saves_backup_hint, backups.size),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (confirm100) {
        AlertDialog(
            onDismissRequest = { confirm100 = false },
            title = { Text(stringResource(R.string.saves_install_100)) },
            text = { Text(stringResource(R.string.saves_100_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm100 = false
                    scope.launch {
                        status = runCatching { withContext(Dispatchers.IO) { manager.install100PercentSave() } }
                            .fold({ done100 }, { failed.format(it.message ?: it.javaClass.simpleName) })
                        changes++
                    }
                }) { Text(stringResource(R.string.saves_install_100)) }
            },
            dismissButton = { TextButton(onClick = { confirm100 = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
