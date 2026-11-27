package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.BuildConfig
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.REPO_URL
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha

/** Dedicated About page under More: version + build code, updates, changelog viewer, GitHub link and game folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(vm: AppViewModel) {
    val uri = LocalUriHandler.current

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.about), stringResource(R.string.ln_more_about))

        SectionCard(stringResource(R.string.about), Icons.Rounded.Info, accent = Konoha.NarutoOrange) {
            Text(
                stringResource(R.string.about_version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.about_author),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.about_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.checkForUpdate(manual = true) }) {
                    Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.check_updates))
                }
                OutlinedButton(onClick = { vm.openCurrentChangelog() }) {
                    Icon(Icons.Rounded.NewReleases, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.update_changelog_btn))
                }
                OutlinedButton(onClick = { runCatching { uri.openUri(REPO_URL) } }) {
                    Icon(Icons.Rounded.Code, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.github_repo))
                }
            }
        }

        SectionCard(stringResource(R.string.game_folder), Icons.Rounded.Folder, accent = Konoha.ChakraBlue) {
            Text(
                vm.paths.root.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
