package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dfdx047.hiddenleaf.R
import com.dfdx047.hiddenleaf.data.EngineSettings
import com.dfdx047.hiddenleaf.data.Renderer

/**
 * In-game Graphics menu styled like Ultimate Ninja 5's own Mode Select / Options wooden boards:
 * carved dark wood planks, golden Shojumaru titles, pulsing orange focus border, smooth slide-in
 * animation, and the classic blue-grey bottom help bar. The live game keeps animating behind it
 * (with TouchBridge.setPadHeld(true)); navigable by both direct touch and gamepad.
 */
class GraphicsMenuHost(
    context: Context,
    initial: Map<String, Int>,
    private val rendererInUse: String,
    private val onSetting: (String, Int) -> Unit,
    private val close: () -> Unit,
) : FrameLayout(context) {
    private val owner = Owner()
    private val settingsState = mutableStateOf(initial)
    private val page = mutableIntStateOf(0) // 0 = Main categories, 1 = Video, 2 = Filters & FPS, 3 = Engine
    private val compose = ComposeView(context).apply {
        setContent {
            GraphicsMenuContent(
                settings = settingsState.value,
                rendererInUse = rendererInUse,
                page = page.intValue,
                onPage = { page.intValue = it },
                onSet = { k, v ->
                    onSetting(k, v)
                    settingsState.value = settingsState.value + (k to v)
                },
                onBack = ::back,
            )
        }
    }

    init {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        addView(compose, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isFocusable = true
    }

    fun installOwners(vararg roots: android.view.View) {
        for (root in roots) {
            root.setViewTreeLifecycleOwner(owner)
            root.setViewTreeSavedStateRegistryOwner(owner)
        }
    }

    fun focus() {
        if (!compose.hasFocus()) compose.requestFocus()
    }

    fun back() {
        if (page.intValue != 0) page.intValue = 0 else close()
    }

    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        init {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }
    }
}

private val G_WOOD_TOP = Color(0xFF7D4B24)
private val G_WOOD_MID = Color(0xFF5B3418)
private val G_WOOD_BOT = Color(0xFF3E220E)
private val G_WOOD_EDGE = Color(0xFF231107)
private val G_GOLD = Color(0xFFFFE7A3)
private val G_ORANGE = Color(0xFFFF7A1A)
private val G_ORANGE_DEEP = Color(0xFFD94D08)
private val G_CREAM = Color(0xFFF3ECE2)
private val G_INK = Color(0xFF241208)
private val G_HELP_BG = Color(0xD8254252)

@Composable
private fun GraphicsMenuContent(
    settings: Map<String, Int>,
    rendererInUse: String,
    page: Int,
    onPage: (Int) -> Unit,
    onSet: (String, Int) -> Unit,
    onBack: () -> Unit,
) {
    val shojumaru = remember { FontFamily(Font(R.font.shojumaru_regular)) }
    val kanit = remember { FontFamily(Font(R.font.kanit_semibold)) }
    val firstFocus = remember { FocusRequester() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(page) { runCatching { firstFocus.requestFocus() } }

    val pulse = rememberInfiniteTransition(label = "un5_gfx_pulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )

    fun get(k: String, d: Int) = settings[k] ?: d

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelFraction = if (maxWidth > maxHeight) 0.52f else 0.92f
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(220)) { -it / 2 } + fadeIn(tween(180)),
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(panelFraction).fillMaxHeight(),
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color(0xD6120B06), Color(0xA6120B06), Color.Transparent)))
                    .padding(start = 18.dp, end = 24.dp, top = 16.dp, bottom = 46.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header banner matching UN5's wooden title plaque
                val title = when (page) {
                    1 -> stringResource(R.string.un5_gfx_video)
                    2 -> stringResource(R.string.un5_gfx_filters)
                    3 -> stringResource(R.string.un5_gfx_engine)
                    else -> stringResource(R.string.un5_gfx_title)
                }
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = shojumaru,
                        fontSize = 22.sp,
                        color = G_GOLD,
                        shadow = Shadow(G_INK, Offset(2f, 3f), 3f),
                    ),
                )
                Text(
                    stringResource(R.string.gm_renderer_now, rendererInUse),
                    style = TextStyle(fontFamily = kanit, fontSize = 12.sp, color = G_CREAM.copy(alpha = 0.85f)),
                )

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (page) {
                        0 -> {
                            val q = get(EngineSettings.SCALE4, 4)
                            val aspect = get(EngineSettings.ASPECT, 0)
                            val filter = get(EngineSettings.FILTER, 0)
                            val fps = get(EngineSettings.FPS_MODE, 50)
                            val light = get(EngineSettings.LIGHT_MODE, 0) == 1
                            WoodBoard(
                                title = stringResource(R.string.un5_gfx_video),
                                subtitle = "${scaleLabel(q)} · ${aspectName(aspect)}",
                                shojumaru = shojumaru, kanit = kanit, pulse = pulse.value,
                                modifier = Modifier.focusRequester(firstFocus),
                                onClick = { onPage(1) },
                            )
                            WoodBoard(
                                title = stringResource(R.string.un5_gfx_filters),
                                subtitle = "${filterName(filter)} · $fps FPS",
                                shojumaru = shojumaru, kanit = kanit, pulse = pulse.value,
                                onClick = { onPage(2) },
                            )
                            WoodBoard(
                                title = stringResource(R.string.un5_gfx_engine),
                                subtitle = if (light) stringResource(R.string.light_mode) else "Vulkan / GS",
                                shojumaru = shojumaru, kanit = kanit, pulse = pulse.value,
                                onClick = { onPage(3) },
                            )
                            WoodBoard(
                                title = stringResource(R.string.cancel),
                                subtitle = null,
                                shojumaru = shojumaru, kanit = kanit, pulse = pulse.value,
                                onClick = onBack,
                            )
                        }
                        1 -> {
                            val q = get(EngineSettings.SCALE4, 4)
                            val aspect = get(EngineSettings.ASPECT, 0)
                            WoodOptionRow(
                                label = stringResource(R.string.resolution),
                                value = "${scaleLabel(q)} (${128 * q}×${112 * q}) · reinicie",
                                kanit = kanit, pulse = pulse.value,
                                modifier = Modifier.focusRequester(firstFocus),
                                onPrev = {
                                    val steps = EngineSettings.SCALE_STEPS
                                    val idx = steps.indexOf(q).let { if (it <= 0) steps.lastIndex else it - 1 }
                                    onSet(EngineSettings.SCALE4, steps[idx])
                                },
                                onNext = {
                                    val steps = EngineSettings.SCALE_STEPS
                                    val idx = steps.indexOf(q).let { if (it < 0 || it >= steps.lastIndex) 0 else it + 1 }
                                    onSet(EngineSettings.SCALE4, steps[idx])
                                },
                            )
                            val aspects = listOf(0, 1333, 1778, 2222)
                            WoodOptionRow(
                                label = stringResource(R.string.aspect),
                                value = aspectName(aspect),
                                kanit = kanit, pulse = pulse.value,
                                onPrev = {
                                    val idx = aspects.indexOf(aspect).let { if (it <= 0) aspects.lastIndex else it - 1 }
                                    onSet(EngineSettings.ASPECT, aspects[idx])
                                },
                                onNext = {
                                    val idx = aspects.indexOf(aspect).let { if (it < 0 || it >= aspects.lastIndex) 0 else it + 1 }
                                    onSet(EngineSettings.ASPECT, aspects[idx])
                                },
                            )
                            WoodBoard(stringResource(R.string.cancel), null, shojumaru, kanit, pulse.value, onClick = onBack)
                        }
                        2 -> {
                            val filter = get(EngineSettings.FILTER, 0)
                            val fps = if (get(EngineSettings.FPS_MODE, 50) == 30) 30 else 50
                            val fg = get(EngineSettings.ZERO_FG, 0)
                            WoodOptionRow(
                                label = stringResource(R.string.screen_filter),
                                value = filterName(filter),
                                kanit = kanit, pulse = pulse.value,
                                modifier = Modifier.focusRequester(firstFocus),
                                onPrev = { onSet(EngineSettings.FILTER, (filter + 3) % 4) },
                                onNext = { onSet(EngineSettings.FILTER, (filter + 1) % 4) },
                            )
                            WoodOptionRow(
                                label = stringResource(R.string.panel_fps_limit),
                                value = "$fps FPS",
                                kanit = kanit, pulse = pulse.value,
                                onPrev = { onSet(EngineSettings.FPS_MODE, if (fps == 30) 50 else 30) },
                                onNext = { onSet(EngineSettings.FPS_MODE, if (fps == 30) 50 else 30) },
                            )
                            val fgs = listOf(0, 2, 3, 4)
                            WoodOptionRow(
                                label = "Frame Gen (ZeroFG · Offline)",
                                value = if (fg == 0) "Desligado" else "${fg}x (Vulkan 1.3)",
                                kanit = kanit, pulse = pulse.value,
                                onPrev = {
                                    val idx = fgs.indexOf(fg).let { if (it <= 0) fgs.lastIndex else it - 1 }
                                    onSet(EngineSettings.ZERO_FG, fgs[idx])
                                },
                                onNext = {
                                    val idx = fgs.indexOf(fg).let { if (it < 0 || it >= fgs.lastIndex) 0 else it + 1 }
                                    onSet(EngineSettings.ZERO_FG, fgs[idx])
                                },
                            )
                            WoodBoard(stringResource(R.string.cancel), null, shojumaru, kanit, pulse.value, onClick = onBack)
                        }
                        3 -> {
                            val light = get(EngineSettings.LIGHT_MODE, 0) == 1
                            val nat = get(EngineSettings.NATIVE_RENDER, 0) == 1
                            val meter = get(EngineSettings.METER, 0)
                            val api = get(EngineSettings.RENDERER, Renderer.AUTO)
                            WoodOptionRow(
                                label = stringResource(R.string.light_mode),
                                value = if (light) "ON" else "OFF",
                                kanit = kanit, pulse = pulse.value,
                                modifier = Modifier.focusRequester(firstFocus),
                                onPrev = { onSet(EngineSettings.LIGHT_MODE, if (light) 0 else 1) },
                                onNext = { onSet(EngineSettings.LIGHT_MODE, if (light) 0 else 1) },
                            )
                            WoodOptionRow(
                                label = stringResource(R.string.native_render),
                                value = if (nat) "ON" else "OFF",
                                kanit = kanit, pulse = pulse.value,
                                onPrev = { onSet(EngineSettings.NATIVE_RENDER, if (nat) 0 else 1) },
                                onNext = { onSet(EngineSettings.NATIVE_RENDER, if (nat) 0 else 1) },
                            )
                            WoodOptionRow(
                                label = stringResource(R.string.renderer),
                                value = if (api == Renderer.OPENGL) "OpenGL ES" else "Vulkan",
                                kanit = kanit, pulse = pulse.value,
                                onPrev = { onSet(EngineSettings.RENDERER, if (api == Renderer.OPENGL) Renderer.VULKAN else Renderer.OPENGL) },
                                onNext = { onSet(EngineSettings.RENDERER, if (api == Renderer.OPENGL) Renderer.VULKAN else Renderer.OPENGL) },
                            )
                            WoodOptionRow(
                                label = stringResource(R.string.show_fps),
                                value = when (meter) { 0 -> "OFF"; 1 -> "FPS"; 2 -> "FPS+ms"; else -> "Full" },
                                kanit = kanit, pulse = pulse.value,
                                onPrev = { onSet(EngineSettings.METER, (meter + 3) % 4) },
                                onNext = { onSet(EngineSettings.METER, (meter + 1) % 4) },
                            )
                            WoodBoard(stringResource(R.string.cancel), null, shojumaru, kanit, pulse.value, onClick = onBack)
                        }
                    }
                }
            }
        }

        // Classic UN5 bottom help strip
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(34.dp)
                .background(G_HELP_BG).padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                stringResource(R.string.un5_gfx_help),
                style = TextStyle(fontFamily = kanit, fontSize = 13.sp, color = G_CREAM),
            )
        }
    }
}

@Composable
private fun aspectName(milli: Int): String = when (milli) {
    1333 -> "4:3"
    1778 -> "16:9"
    2222 -> "20:9"
    else -> stringResource(R.string.aspect_screen)
}

@Composable
private fun filterName(f: Int): String = when (f) {
    1 -> stringResource(R.string.filter_sharp)
    2 -> "FXAA"
    3 -> "CAS"
    else -> stringResource(R.string.filter_bilinear)
}

@Composable
private fun WoodBoard(
    title: String,
    subtitle: String?,
    shojumaru: FontFamily,
    kanit: FontFamily,
    pulse: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = remember { CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp) }
    val woodBrush = remember { Brush.verticalGradient(listOf(G_WOOD_TOP, G_WOOD_MID, G_WOOD_BOT)) }
    Box(
        modifier.fillMaxWidth()
            .background(woodBrush, shape)
            .border(
                width = if (focused) 2.5.dp else 1.5.dp,
                color = if (focused) G_ORANGE.copy(alpha = pulse) else G_WOOD_EDGE,
                shape = shape,
            )
            .drawBehind {
                if (focused) {
                    drawRect(G_ORANGE.copy(alpha = 0.14f * pulse))
                }
            }
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (focused) {
                    Text("▶ ", color = G_ORANGE, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = shojumaru,
                        fontSize = 17.sp,
                        color = if (focused) Color.White else G_GOLD,
                        shadow = Shadow(G_INK, Offset(2f, 2f), 2f),
                    ),
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = TextStyle(fontFamily = kanit, fontSize = 12.sp, color = G_CREAM.copy(alpha = 0.88f)),
                )
            }
        }
    }
}

@Composable
private fun WoodOptionRow(
    label: String,
    value: String,
    kanit: FontFamily,
    pulse: Float,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = remember { RoundedCornerShape(8.dp) }
    val woodBrush = remember { Brush.verticalGradient(listOf(G_WOOD_TOP, G_WOOD_BOT)) }
    Row(
        modifier.fillMaxWidth()
            .background(woodBrush, shape)
            .border(if (focused) 2.dp else 1.5.dp, if (focused) G_ORANGE.copy(alpha = pulse) else G_WOOD_EDGE, shape)
            .clickable(interactionSource = src, indication = null, onClick = onNext)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = kanit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = G_GOLD),
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "◀",
                color = G_ORANGE, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onPrev).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                value,
                style = TextStyle(fontFamily = kanit, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White),
            )
            Text(
                "▶",
                color = G_ORANGE, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onNext).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
