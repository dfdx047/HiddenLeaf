package com.dfdx047.hiddenleaf.ui.screens

import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dfdx047.hiddenleaf.AppViewModel
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.touch.Ctl
import com.dfdx047.hiddenleaf.touch.Macro
import com.dfdx047.hiddenleaf.touch.MacroStep
import com.dfdx047.hiddenleaf.touch.PadDefaults
import com.dfdx047.hiddenleaf.touch.PadSkin
import com.dfdx047.hiddenleaf.touch.Ps2
import com.dfdx047.hiddenleaf.touch.TouchConfig
import com.dfdx047.hiddenleaf.touch.TouchMode
import com.dfdx047.hiddenleaf.touch.PadStyle
import com.dfdx047.hiddenleaf.touch.TouchLayout
import com.dfdx047.hiddenleaf.touch.TouchListener
import com.dfdx047.hiddenleaf.touch.PadScene
import androidx.compose.ui.graphics.Brush
import com.dfdx047.hiddenleaf.touch.TouchSurface
import com.dfdx047.hiddenleaf.touch.createTouchSurface
import com.dfdx047.hiddenleaf.touch.view
import com.dfdx047.hiddenleaf.ui.components.ScreenTitle
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.ui.components.SectionCard
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import kotlin.math.roundToInt

/**
 * The on-screen controller: a picture of each scene's layout, what every button does (with the buttons' own drawing),
 * when it shows and its look, "Toque Pro", the one-tap special of the engine, and the player's macros.
 */
@Composable
fun ControlsScreen(vm: AppViewModel) {
    val c by vm.touch.collectAsStateWithLifecycle()
    val engine by vm.engineSettings.collectAsStateWithLifecycle()
    ControlsContent(c, engine, onTouch = { vm.updateTouch(it) }, onSetting = { k, v -> vm.setting(k, v) })
}

/** The page itself, apart from the view model (the screenshot test draws it with a configuration of its own). */
@Composable
internal fun ControlsContent(
    c: TouchConfig,
    engine: Map<String, Int>,
    onTouch: (TouchConfig) -> Unit,
    onSetting: (String, Int) -> Unit,
    startScene: PadScene = PadScene.FIGHT,
) {
    var editing by remember { mutableStateOf(false) }
    var editMacro by remember { mutableStateOf<Macro?>(null) }
    val mobile = c.layout == TouchLayout.MOBILE

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_controls), stringResource(R.string.tc_subtitle))

        // a picture of the layout, in the chosen look, for each scene
        var scene by remember { mutableStateOf(startScene) }
        if (mobile) {
            ChipRow(
                options = listOf(
                    stringResource(R.string.pad_scene_fight) to PadScene.FIGHT.ordinal,
                    stringResource(R.string.pad_scene_campaign) to PadScene.CAMPAIGN.ordinal,
                    stringResource(R.string.pad_scene_menu) to PadScene.MENU.ordinal,
                ),
                selected = scene.ordinal,
                onSelect = { scene = PadScene.entries[it] },
            )
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.large)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2147), Color(0xFF3A2440), Color(0xFF5A2A12)))),
            contentAlignment = Alignment.Center,
        ) {
            // the controller laid out on a phone's landscape screen (at least 760 dp wide) and shrunk into the box, so the
            // buttons keep their sizes against each other as in the game instead of piling up in a narrow page
            val vw = maxOf(maxWidth, 760.dp)
            val k = maxWidth / vw
            key(c.layout) { // (the other layout is another view)
                AndroidView(
                    factory = { ctx -> createTouchSurface(ctx, c.copy(mode = TouchMode.ALWAYS)).also { it.previewScene = scene }.view },
                    update = { v -> (v as TouchSurface).also { it.config = c.copy(mode = TouchMode.ALWAYS); it.previewScene = scene } },
                    modifier = Modifier.requiredSize(vw, vw * 9f / 16f).graphicsLayer { scaleX = k; scaleY = k },
                )
            }
        }
        if (mobile) {
            Text(stringResource(R.string.tc_scenes_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.touch_edit_layout))
        }
        if (mobile) {
            Text(stringResource(R.string.tc_edit_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ButtonGuide(c, scene)
        }

        SectionCard(stringResource(R.string.touch_controls), NinjaIcons.Shuriken, accent = Konoha.ChakraBlue) {
            Text(stringResource(R.string.touch_layout), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(stringResource(R.string.touch_layout_mobile) to TouchLayout.MOBILE.ordinal, stringResource(R.string.touch_layout_classic) to TouchLayout.CLASSIC.ordinal),
                selected = c.layout.ordinal,
                onSelect = { onTouch(c.copy(layout = TouchLayout.entries[it])) },
            )
            Text(stringResource(R.string.tc_layout_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToggleRow(stringResource(R.string.touch_auto_mode), stringResource(R.string.tc_auto_mode_text), c.autoMode) { onTouch(c.copy(autoMode = it)) }
            ChipRow(
                options = listOf(stringResource(R.string.touch_off) to 0, stringResource(R.string.touch_auto) to 1, stringResource(R.string.touch_always) to 2),
                selected = c.mode.ordinal,
                onSelect = { onTouch(c.copy(mode = TouchMode.entries[it])) },
            )
            Text(stringResource(R.string.touch_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (mobile) {
                Text(stringResource(R.string.touch_style), style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf(
                        stringResource(R.string.touch_style_ink) to PadStyle.INK.ordinal,
                        stringResource(R.string.touch_style_glass) to PadStyle.GLASS.ordinal,
                    ),
                    selected = c.style.ordinal,
                    onSelect = { onTouch(c.copy(style = PadStyle.entries[it])) },
                )
            }
            Text(stringResource(R.string.touch_skin), style = MaterialTheme.typography.titleSmall)
            ChipRow(
                options = listOf(
                    stringResource(R.string.skin_playstation) to PadSkin.PLAYSTATION.ordinal,
                    stringResource(R.string.skin_xbox) to PadSkin.XBOX.ordinal,
                    stringResource(R.string.skin_nintendo) to PadSkin.NINTENDO.ordinal,
                ),
                selected = c.skin.ordinal,
                onSelect = { onTouch(c.copy(skin = PadSkin.entries[it])) },
            )
            StepSlider(stringResource(R.string.opacity), { "${it * 10}%" }, c.opacity / 10, 1..10) { onTouch(c.copy(opacity = it * 10)) }
            StepSlider(stringResource(R.string.touch_size), { "${it * 10}%" }, (c.scale * 10).roundToInt(), 5..20) { onTouch(c.copy(scale = it / 10f)) }
            if (mobile) {
                ToggleRow(stringResource(R.string.tc_menu_touch), stringResource(R.string.tc_menu_touch_text), c.menuTouch) { onTouch(c.copy(menuTouch = it)) }
                ToggleRow(stringResource(R.string.tc_show_names), stringResource(R.string.tc_show_names_text), c.showNames) { onTouch(c.copy(showNames = it)) }
            }
            ToggleRow(stringResource(R.string.touch_haptics), stringResource(R.string.touch_haptics_text), c.haptics) { onTouch(c.copy(haptics = it)) }
            ToggleRow(stringResource(R.string.touch_float), stringResource(R.string.touch_float_text), c.sticksFloat) { onTouch(c.copy(sticksFloat = it)) }
        }

        if (mobile) TouchProSection(c, onChange = onTouch, onEditLayout = { editing = true })

        SectionCard(stringResource(R.string.special_one), NinjaIcons.Kunai, accent = Konoha.ChakraAmber) {
            val specialOn = (engine[EngineSettings.SPECIAL_ONE] ?: 0) != 0
            ToggleRow(stringResource(R.string.special_one), stringResource(R.string.special_one_text), specialOn) {
                onSetting(EngineSettings.SPECIAL_ONE, if (it) 1 else 0)
            }
            if (specialOn) {
                Text(stringResource(R.string.special_button), style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf("○" to 0x20, "□" to 0x80, "△" to 0x10, "✕" to 0x40, "L1" to 0x04, "R1" to 0x08, "L2" to 0x01, "R2" to 0x02),
                    selected = engine[EngineSettings.SPECIAL_BUTTON] ?: 0x20,
                    onSelect = { onSetting(EngineSettings.SPECIAL_BUTTON, it) },
                )
                Text(stringResource(R.string.special_button_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.mash_multiplier), style = MaterialTheme.typography.titleSmall)
                ChipRow(
                    options = listOf("1x" to 100, "1,25x" to 125, "1,5x" to 150, "1,65x" to 165, "2x" to 200),
                    selected = engine[EngineSettings.MASH_PERCENT] ?: 100,
                    onSelect = { onSetting(EngineSettings.MASH_PERCENT, it) },
                )
                Text(stringResource(R.string.mash_multiplier_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        MacroSection(c, onTouch, onEdit = { editMacro = it })
    }

    if (editing) {
        LayoutEditor(c, onChange = onTouch, onDone = { editing = false })
    }
    editMacro?.let { m ->
        val index = c.macros.indexOfFirst { it.id == m.id }.let { if (it < 0) c.macros.size else it }
        MacroEditor(c, m, index, onDismiss = { editMacro = null }) { saved, place ->
            val list = if (c.macros.any { it.id == saved.id }) c.macros.map { if (it.id == saved.id) saved else it } else c.macros + saved
            onTouch(c.copy(macros = list))
            editMacro = null
            if (place) editing = true
        }
    }
}

/** The player's macros: each with its button, its steps, edit and delete. */
@Composable
private fun MacroSection(c: TouchConfig, onTouch: (TouchConfig) -> Unit, onEdit: (Macro) -> Unit) {
    val painter = rememberPadPainter(c)
    SectionCard(stringResource(R.string.macros), NinjaIcons.Scroll, accent = Konoha.ChakraAmber) {
        Text(stringResource(R.string.mc_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (c.macros.isEmpty()) {
            Text(stringResource(R.string.mc_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        c.macros.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PadIcon(painter, null, m)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        macroSummary(c.skin, m),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (m.place?.visible == false) {
                        Text(stringResource(R.string.mc_hidden), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = { onEdit(m) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.macro_edit)) }
                IconButton(onClick = { onTouch(c.copy(macros = c.macros.filter { it.id != m.id })) }) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.mc_delete))
                }
            }
        }
        FilledTonalButton(onClick = {
            onEdit(Macro(id = "m" + System.currentTimeMillis().toString(36), name = "M${c.macros.size + 1}", steps = listOf(MacroStep(Ps2.CIRCLE, 60))))
        }) {
            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.macro_add))
        }
    }
}

/** The whole screen as the game's: the controller in edit mode, with its own toolbar. */
@Composable
private fun LayoutEditor(c: TouchConfig, onChange: (TouchConfig) -> Unit, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
            // Keep the preview itself unobstructed. Help and tool labels are drawn inside NinjaPadView's
            // compact top toolbar instead of as a transparent Compose layer over the controls.
            key(c.layout) {
                AndroidView(
                    factory = { ctx ->
                        createTouchSurface(ctx, c.copy(mode = TouchMode.ALWAYS), editing = true).apply {
                            listener = object : TouchListener {
                                override fun onConfigChanged(config: TouchConfig) = onChange(config.copy(mode = c.mode))
                                override fun onEditDone() = onDone()
                            }
                        }.view
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view -> (view as TouchSurface).config = c.copy(mode = TouchMode.ALWAYS) },
                )
            }
        }
    }
}

// ---- the macro editor

/** Ready sequences a macro can start from: the game's inputs and two common ones. */
private enum class MacroTemplate(val label: Int, val steps: List<MacroStep>, val repeat: Boolean = false) {
    TOGETHER(R.string.mc_t_together, listOf(MacroStep(Ps2.TRIANGLE or Ps2.CIRCLE, 100))),
    JUTSU(R.string.mc_t_jutsu, Ctl.JUTSU.steps),
    OUGI(R.string.mc_t_ougi, Ctl.OUGI.steps),
    UP(R.string.mc_t_up, Ctl.SPECIAL_UP.steps),
    DOWN(R.string.mc_t_down, Ctl.SPECIAL_DOWN.steps),
    COMBO(R.string.mc_t_combo, List(4) { listOf(MacroStep(Ps2.CIRCLE, 60), MacroStep(0, 60)) }.flatten().dropLast(1)),
    TURBO(R.string.mc_t_turbo, listOf(MacroStep(Ps2.CIRCLE, 50), MacroStep(0, 50)), repeat = true),
}

private val STEP_TIMES = listOf(20, 50, 100, 250, 500)

/**
 * A macro, made step by step: a name, a ready start (the game's inputs), each step's buttons (several in one step are
 * pressed together) and time, its order, repeat while held, and whether and how big it is on the screen. "Save and
 * place" opens the layout editor, where it is dragged to its place.
 */
@Composable
private fun MacroEditor(c: TouchConfig, initial: Macro, index: Int, onDismiss: () -> Unit, onSave: (Macro, Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MacroEditorContent(c, initial, index, onDismiss, onSave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MacroEditorContent(c: TouchConfig, initial: Macro, index: Int, onDismiss: () -> Unit, onSave: (Macro, Boolean) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var visible by remember { mutableStateOf(initial.place?.visible ?: true) }
    var scale by remember { mutableStateOf(((initial.place?.scale ?: 1f) * 10).roundToInt()) }
    val steps = remember { mutableStateListOf<MacroStep>().apply { addAll(initial.steps) } }
    val skin = c.skin

    fun built(): Macro {
        val base = initial.place ?: PadDefaults.macroPlace(index)
        val place = if (initial.place == null && visible && scale == 10) null else base.copy(visible = visible, scale = scale / 10f)
        return initial.copy(name = name.ifBlank { initial.name }, repeat = repeat, steps = steps.toList(), place = place)
    }

    run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val wide = Modifier.widthIn(max = 720.dp).fillMaxWidth()
                Row(wide, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mc_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(12) }, label = { Text(stringResource(R.string.macro_name)) },
                    supportingText = { Text(stringResource(R.string.mc_name_text)) }, singleLine = true, modifier = wide,
                )

                Column(wide, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.mc_templates), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroTemplate.entries.forEach { t ->
                            FilterChip(selected = false, onClick = {
                                steps.clear(); steps.addAll(t.steps); repeat = t.repeat
                            }, label = { Text(stringResource(t.label)) })
                        }
                    }
                }

                Column(wide, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.mc_steps), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.mc_steps_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    steps.forEachIndexed { i, st ->
                        StepCard(
                            skin, i, st, last = i == steps.lastIndex,
                            onChange = { steps[i] = it },
                            onUp = { if (i > 0) steps.add(i - 1, steps.removeAt(i)) },
                            onDown = { if (i < steps.lastIndex) steps.add(i + 1, steps.removeAt(i)) },
                            onCopy = { steps.add(i + 1, st) },
                            onDelete = { steps.removeAt(i) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { steps += MacroStep(Ps2.CIRCLE, 60) }) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.macro_add_step))
                        }
                        OutlinedButton(onClick = { steps += MacroStep(0, 50) }) {
                            Icon(Icons.Rounded.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text(stringResource(R.string.mc_add_pause))
                        }
                    }
                    Text(
                        stringResource(R.string.mc_total, steps.sumOf { it.ms }) + "  ·  " + macroSummary(skin, built()),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(wide, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    ToggleRow(stringResource(R.string.macro_repeat), stringResource(R.string.mc_repeat_text), repeat) { repeat = it }
                    ToggleRow(stringResource(R.string.mc_on_screen), stringResource(R.string.mc_on_screen_text), visible) { visible = it }
                    StepSlider(stringResource(R.string.mc_size), { "${it * 10}%" }, scale, 6..16) { scale = it }
                }

                // (cancel is the ✕ at the top; a row that wraps, so a long label is never cut on a narrow phone)
                FlowRow(wide, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSave(built(), true) }, enabled = steps.any { it.buttons != 0 }) { Text(stringResource(R.string.mc_save_place)) }
                    Button(onClick = { onSave(built(), false) }, enabled = steps.any { it.buttons != 0 }) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}

/** One step: its buttons (together), its time, and moving, copying or deleting it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCard(
    skin: PadSkin,
    i: Int,
    st: MacroStep,
    last: Boolean,
    onChange: (MacroStep) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.macro_step_n, i + 1), style = MaterialTheme.typography.labelLarge)
                Text(
                    if (st.buttons == 0) stringResource(R.string.mc_pause) else stepKeys(skin, st.buttons),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onUp, enabled = i > 0) { Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.mc_up)) }
            IconButton(onClick = onDown, enabled = !last) { Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.mc_down)) }
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.mc_copy)) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.mc_delete)) }
        }
        FlowRow(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Ps2.MACRO_BUTTONS.forEach { b ->
                FilterChip(selected = st.buttons and b != 0, onClick = { onChange(st.copy(buttons = st.buttons xor b)) }, label = { Text(Ps2.label(skin, b)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.mc_time), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { onChange(st.copy(ms = (st.ms - 10).coerceAtLeast(20))) }) { Icon(Icons.Rounded.Remove, null) }
            Text("${st.ms} ms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { onChange(st.copy(ms = (st.ms + 10).coerceAtMost(2000))) }) { Icon(Icons.Rounded.Add, null) }
        }
        FlowRow(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            STEP_TIMES.forEach { ms ->
                FilterChip(selected = st.ms == ms, onClick = { onChange(st.copy(ms = ms)) }, label = { Text("$ms") })
            }
        }
    }
}

/** The buttons of a step, joined: "△ + ○". */
internal fun stepKeys(skin: PadSkin, buttons: Int): String =
    Ps2.MACRO_BUTTONS.filter { buttons and it != 0 }.joinToString(" + ") { Ps2.label(skin, it) }

/** A macro in one line: its presses in order, "…" for a pause, ⟳ when it repeats. */
internal fun macroSummary(skin: PadSkin, m: Macro): String =
    m.steps.joinToString("  ›  ") { if (it.buttons == 0) "…" else stepKeys(skin, it.buttons) } + if (m.repeat) "  ⟳" else ""
