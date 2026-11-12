package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha

/** Shortcuts over the game, read by the engine when it starts: skipping the opening and opening every character. */
@Composable
fun CheatsScreen(vm: AppViewModel) {
    val s by vm.engineSettings.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.ln_nav_cheats), stringResource(R.string.ln_cheats_sub))

        SectionCard(stringResource(R.string.skip_intro), Icons.Rounded.FastForward, accent = Konoha.ChakraBlue) {
            // (on by default, as the engine reads it: un5_main.cpp loadSettings)
            ToggleRow(stringResource(R.string.skip_intro), stringResource(R.string.ln_skip_intro_text), (s[EngineSettings.SKIP_INTRO] ?: 1) != 0) {
                vm.setting(EngineSettings.SKIP_INTRO, if (it) 1 else 0)
            }
        }

        SectionCard(stringResource(R.string.unlock_all), Icons.Rounded.LockOpen, accent = Konoha.ChakraAmber) {
            ToggleRow(stringResource(R.string.unlock_all), stringResource(R.string.ln_unlock_text), (s[EngineSettings.UNLOCK_ALL] ?: 0) != 0) { on ->
                if (on) confirm = true else vm.setting(EngineSettings.UNLOCK_ALL, 0)
            }
        }

        SectionCard(stringResource(R.string.how_it_works), Icons.Rounded.Info, accent = MaterialTheme.colorScheme.secondary) {
            Text(
                stringResource(R.string.ln_cheats_how),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Rounded.LockOpen, null) },
            title = { Text(stringResource(R.string.unlock_q)) },
            text = { Text(stringResource(R.string.ln_unlock_q_text)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.setting(EngineSettings.UNLOCK_ALL, 1) }) { Text(stringResource(R.string.unlock)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
