package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.BuildConfig
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.UpdateState
import com.dfdx047.hiddenleaf.data.formatBytes
import com.dfdx047.hiddenleaf.ui.components.MarkdownNotes
import com.dfdx047.hiddenleaf.ui.theme.Konoha

@Composable
private fun ChangelogBox(version: String, notes: String, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, Konoha.ChakraAmber.copy(alpha = 0.28f), shape)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showTitle) {
            Text(
                stringResource(R.string.update_whats_new, version.removePrefix("v")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Konoha.ChakraAmber,
            )
        }
        if (notes.isNotBlank()) {
            MarkdownNotes(notes)
        } else {
            Text(
                stringResource(R.string.update_changelog_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The pop-up for a new version: offer with full changelog, download with progress + changelog, and install permission step. */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val st by vm.update.collectAsStateWithLifecycle()
    when (val s = st) {
        is UpdateState.Available -> Dialog(
            onDismissRequest = vm::dismissUpdate,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val maxH = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 640.dp).heightIn(max = maxH),
            ) {
                Column {
                    // header: what is out, and from which version
                    Row(
                        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep.copy(alpha = 0.55f), Konoha.SasukeBlue.copy(alpha = 0.45f))))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(34.dp), tint = Konoha.ChakraAmber)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.update_available), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.update_versions, s.info.version, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (s.info.size > 0) {
                            Text(formatBytes(s.info.size), style = MaterialTheme.typography.labelLarge, color = Konoha.ChakraAmber)
                        }
                    }
                    // framed scrollable changelog so every player sees what changed before installing
                    Column(
                        Modifier.weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (s.info.nightly) {
                            Text(stringResource(R.string.update_nightly), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ChangelogBox(s.info.version, s.info.notes, Modifier.weight(1f, fill = false))
                        Text(stringResource(R.string.update_keeps), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = vm::dismissUpdate) { Text(stringResource(R.string.update_later)) }
                        Spacer(Modifier.size(8.dp))
                        Button(onClick = vm::startUpdate) { Text(stringResource(R.string.update_now)) }
                    }
                }
            }
        }

        is UpdateState.Downloading -> Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val maxH = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 640.dp).heightIn(max = maxH),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep.copy(alpha = 0.55f), Konoha.SasukeBlue.copy(alpha = 0.45f))))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(32.dp), tint = Konoha.ChakraAmber)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${s.info.version} · " + formatBytes(s.done) + if (s.total > 0) " / ${formatBytes(s.total)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (s.total > 0) LinearProgressIndicator(progress = { (s.done.toFloat() / s.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    ChangelogBox(
                        s.info.version,
                        s.info.notes,
                        Modifier.weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = vm::cancelUpdate) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
        }

        is UpdateState.NeedPermission -> AlertDialog(
            onDismissRequest = vm::dismissUpdate,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_text)) },
            confirmButton = { Button(onClick = vm::openInstallPermission) { Text(stringResource(R.string.update_permission_open)) } },
            dismissButton = { TextButton(onClick = vm::dismissUpdate) { Text(stringResource(R.string.cancel)) } },
        )

        else -> {}
    }
}

/** Shown once after upgrading to a new version (or on demand from More -> About -> View changelog). */
@Composable
fun WhatsNewDialog(vm: AppViewModel) {
    val updateSt by vm.update.collectAsStateWithLifecycle()
    val info by vm.whatsNew.collectAsStateWithLifecycle()
    val (version, notes) = info ?: return
    if (updateSt !is UpdateState.Idle) return
    Dialog(
        onDismissRequest = vm::dismissWhatsNew,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val maxH = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 640.dp).heightIn(max = maxH),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep.copy(alpha = 0.55f), Konoha.SasukeBlue.copy(alpha = 0.45f))))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.NewReleases, null, Modifier.size(34.dp), tint = Konoha.ChakraAmber)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.update_whats_new, version.removePrefix("v")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.about_version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                ChangelogBox(
                    version,
                    notes,
                    Modifier.weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 12.dp),
                    showTitle = false,
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = vm::dismissWhatsNew) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}
