package com.dfdx047.hiddenleaf.ui.screens

import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.dfdx047.hiddenleaf.data.REPO_URL
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dfdx047.hiddenleaf.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.BuildConfig
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.data.Renderer
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import com.dfdx047.hiddenleaf.ui.theme.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by vm.engineSettings.collectAsStateWithLifecycle()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun get(k: String, d: Int) = s[k] ?: d

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.settings_title), stringResource(R.string.settings_sub))

        SectionCard(stringResource(R.string.video), Icons.Rounded.Tv) {
            val steps = EngineSettings.SCALE_STEPS
            val q = get(EngineSettings.SCALE4, 0).takeIf { it > 0 } ?: (get(EngineSettings.SCALE, EngineSettings.SCALE_DEFAULT) * 4)
            val at = steps.indices.minByOrNull { kotlin.math.abs(steps[it] - q) } ?: 4
            StepSlider(
                label = stringResource(R.string.resolution),
                format = { i -> steps[i].let { "${if (it % 4 == 0) (it / 4).toString() else (it / 4.0).toString()}x · ${128 * it}×${112 * it}" } },
                value = at, range = 0..steps.lastIndex,
                onChange = { i ->
                    vm.setting(EngineSettings.SCALE4, steps[i])
                    vm.setting(EngineSettings.SCALE, EngineSettings.wholeScale(steps[i]))
                },
            )
            Text(
                stringResource(R.string.resolution_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.screen_filter), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(
                    stringResource(R.string.filter_bilinear) to 0, stringResource(R.string.filter_sharp) to 1, "FXAA" to 2,
                    "CAS" to 3,
                ),
                selected = get(EngineSettings.FILTER, 0),
                onSelect = { vm.setting(EngineSettings.FILTER, it) },
            )
            Text(
                stringResource(R.string.screen_filter_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.aspect), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(
                    "4:3" to 1333, stringResource(R.string.aspect_wide) to 1778, stringResource(R.string.aspect_ultrawide) to 2222,
                    stringResource(R.string.aspect_screen) to 0,
                ),
                selected = get(EngineSettings.ASPECT, EngineSettings.ASPECT_DEFAULT),
                onSelect = { vm.setting(EngineSettings.ASPECT, it) },
            )
            Text(
                stringResource(R.string.aspect_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(stringResource(R.string.ln_screen), Icons.Rounded.ScreenRotation, accent = Konoha.ChakraBlue) {
            // GameActivity.orientationFlag: 0 landscape, 1 portrait, 2 as the device is held
            Text(stringResource(R.string.orientation), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(
                    stringResource(R.string.orientation_landscape) to 0, stringResource(R.string.orientation_portrait) to 1,
                    stringResource(R.string.orientation_sensor) to 2,
                ),
                selected = get(EngineSettings.ORIENTATION, 0).coerceIn(0, 2),
                onSelect = { vm.setting(EngineSettings.ORIENTATION, it) },
            )
            Text(
                stringResource(R.string.ln_orientation_text),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(stringResource(R.string.performance), Icons.Rounded.Speed, accent = MaterialTheme.colorScheme.secondary) {
            Text(stringResource(R.string.panel_fps_limit), style = MaterialTheme.typography.titleSmall)
            val fpsVal = if (get(EngineSettings.FPS_MODE, 50) == 30) 30 else 50
            ChipRow(
                options = listOf("30" to 30, stringResource(R.string.panel_fps_50) to 50),
                selected = fpsVal,
                onSelect = { vm.setting(EngineSettings.FPS_MODE, it) },
            )
            Text(
                "Velocidade nativa do jogo (50 Hz original ou 30 Hz para economia de bateria).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Frame Generation (ZeroFG · Somente Offline)", style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf("Desligado" to 0, "2x" to 2, "3x" to 3, "4x" to 4),
                selected = get(EngineSettings.ZERO_FG, 0).let { if (it in listOf(0, 2, 3, 4)) it else 0 },
                onSelect = { vm.setting(EngineSettings.ZERO_FG, it) },
            )
            Text(
                "Geração real de quadros via Vulkan 1.3 (ZeroFG 1.2, sem DLL externa). Multiplica a fluidez na tela sem alterar a física do jogo. Desativado automaticamente no modo Online.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // the engine reads "gpu_api", which Renderer.apply writes from this choice (automatic is Vulkan)
            Text(stringResource(R.string.renderer), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf("Vulkan" to Renderer.VULKAN, "OpenGL ES" to Renderer.OPENGL),
                selected = if (get(EngineSettings.RENDERER, Renderer.AUTO) == Renderer.OPENGL) Renderer.OPENGL else Renderer.VULKAN,
                onSelect = { vm.setRenderer(it) },
            )
            Text(
                stringResource(R.string.ln_renderer_text),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ToggleRow(stringResource(R.string.light_mode), stringResource(R.string.light_mode_text), get(EngineSettings.LIGHT_MODE, 0) == 1) {
                vm.setting(EngineSettings.LIGHT_MODE, if (it) 1 else 0)
            }
        }


        SectionCard(stringResource(R.string.appearance), Icons.Rounded.Palette, accent = MaterialTheme.colorScheme.tertiary) {
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(stringResource(R.string.theme_dark) to ThemeMode.DARK.ordinal, stringResource(R.string.theme_light) to ThemeMode.LIGHT.ordinal, stringResource(R.string.theme_system) to ThemeMode.SYSTEM.ordinal),
                selected = ui.theme.ordinal,
                onSelect = { vm.setTheme(ThemeMode.entries[it]) },
            )
            if (vm.canPickLanguage) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                var lang by remember { mutableStateOf(vm.language()) }
                val langs = listOf("", "en", "pt", "es")
                ChipRow(
                    options = listOf(stringResource(R.string.lang_system) to 0, "English" to 1, "Português" to 2, "Español" to 3),
                    selected = langs.indexOfFirst { it.isNotEmpty() && lang.startsWith(it) }.coerceAtLeast(0),
                    onSelect = { lang = langs[it]; vm.setLanguage(langs[it]) },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleRow(stringResource(R.string.material_you), stringResource(R.string.material_you_text), ui.dynamicColor) {
                    vm.setDynamicColor(it)
                }
            }
        }

        SavesCard(vm)

        SectionCard(stringResource(R.string.about), Icons.Rounded.Info, accent = MaterialTheme.colorScheme.outline) {
            Text("Hidden Leaf ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.about_author), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.about_text),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val uri = LocalUriHandler.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { runCatching { uri.openUri(REPO_URL) } }) {
                    Icon(Icons.Rounded.Code, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.github_repo))
                }
                OutlinedButton(onClick = { vm.checkForUpdate(manual = true) }) {
                    Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.check_updates))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.game_folder), style = MaterialTheme.typography.titleSmall)
            Text(vm.paths.root.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** A slider over whole steps that writes only when the finger lifts. */
@Composable
internal fun StepSlider(label: String, format: (Int) -> String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var drag by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                format(drag.roundToInt()),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = drag,
            onValueChange = { drag = it },
            onValueChangeFinished = { onChange(drag.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipRow(options: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    // the nearest option counts as selected (an aspect written by the engine may be in between)
    val best = options.minByOrNull { kotlin.math.abs(it.second - selected) }?.second
    // (wrapped, not scrolled sideways: on a phone held upright a scrolled row hid the last options under the card's edge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (label, v) ->
            FilterChip(selected = v == best, onClick = { onSelect(v) }, label = { Text(label) })
        }
    }
}
