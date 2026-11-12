package com.dfdx047.hiddenleaf.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle

/** A placeholder for the Online page, so the navigation compiles; the online work replaces this file. */
@Composable
fun OnlineScreen(vm: AppViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        ScreenTitle(stringResource(R.string.ln_nav_online))
        Text(
            stringResource(R.string.ln_online_soon),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
