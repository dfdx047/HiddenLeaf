package com.dfdx047.hiddenleaf

import android.os.Bundle
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.dfdx047.hiddenleaf.ui.components.NinjaIcons
import com.dfdx047.hiddenleaf.ui.theme.Konoha
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dfdx047.hiddenleaf.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dfdx047.hiddenleaf.ui.components.BusyBar
import com.dfdx047.hiddenleaf.ui.components.leafBackground
import com.dfdx047.hiddenleaf.ui.screens.CheatsScreen
import com.dfdx047.hiddenleaf.ui.screens.ControlsScreen
import com.dfdx047.hiddenleaf.ui.screens.MoreItem
import com.dfdx047.hiddenleaf.ui.screens.MoreScreen
import com.dfdx047.hiddenleaf.ui.screens.OnlineScreen
import com.dfdx047.hiddenleaf.ui.screens.SubPage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Public
import com.dfdx047.hiddenleaf.ui.screens.HomeScreen
import com.dfdx047.hiddenleaf.ui.screens.ModsScreen
import com.dfdx047.hiddenleaf.ui.screens.SettingsScreen
import com.dfdx047.hiddenleaf.ui.screens.TexturesScreen
import com.dfdx047.hiddenleaf.ui.screens.UpdateDialog
import com.dfdx047.hiddenleaf.ui.screens.DebugScreen
import com.dfdx047.hiddenleaf.ui.screens.DriversScreen
import com.dfdx047.hiddenleaf.ui.screens.GpuOfferDialog
import com.dfdx047.hiddenleaf.ui.screens.LastRunDialog
import com.dfdx047.hiddenleaf.ui.screens.TexFailDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.dfdx047.hiddenleaf.ui.theme.HiddenLeafTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // a page to open at start (a route of Dest), for the screenshot tests and shortcuts
        val page = intent?.getStringExtra(EXTRA_PAGE)
        setContent {
            val ui by vm.uiState.collectAsStateWithLifecycle()
            HiddenLeafTheme(mode = ui.theme, dynamicColor = ui.dynamicColor) {
                HiddenLeafApp(vm, page)
            }
        }
    }

    companion object {
        const val EXTRA_PAGE = "page"
    }
}

/**
 * The launcher's pages. The navigation bar (or the side panel in landscape) holds the ones with [inBar]; the others
 * are reached from More, and More stays lit while one of them is shown.
 */
private enum class Dest(val route: String, val label: Int, val icon: ImageVector, val inBar: Boolean, val text: Int = 0) {
    HOME("home", R.string.nav_home, NinjaIcons.Gate, inBar = true),
    ONLINE("online", R.string.ln_nav_online, Icons.Rounded.Public, inBar = true),
    CONTROLS("controls", R.string.nav_controls, NinjaIcons.Shuriken, inBar = true),
    SETTINGS("settings", R.string.nav_settings, NinjaIcons.Tools, inBar = true),
    MORE("more", R.string.ln_nav_more, Icons.Rounded.GridView, inBar = true),
    TEXTURES("textures", R.string.nav_textures, NinjaIcons.BrushPen, inBar = false, text = R.string.ln_more_textures),
    MODS("mods", R.string.nav_mods, NinjaIcons.Scroll, inBar = false, text = R.string.ln_more_mods),
    DRIVERS("drivers", R.string.nav_drivers, NinjaIcons.Bolt, inBar = false, text = R.string.ln_more_drivers),
    CHEATS("cheats", R.string.ln_nav_cheats, Icons.Rounded.LockOpen, inBar = false, text = R.string.ln_more_cheats),
    DEBUG("debug", R.string.nav_debug, NinjaIcons.Eye, inBar = false, text = R.string.ln_more_debug),
    ;

    companion object {
        val bar = entries.filter { it.inBar }
        val more = entries.filter { !it.inBar }

        /** The bar's page lit for [route]: its own, or More for a page reached from it. */
        fun tabOf(route: String?): Dest? = entries.firstOrNull { it.route == route }?.let { if (it.inBar) it else MORE }
    }
}

@Composable
private fun HiddenLeafApp(vm: AppViewModel, startPage: String? = null) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val tab = Dest.tabOf(current)
    val snackbar = remember { SnackbarHostState() }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    // files may have been changed from outside (file manager, adb) while the app was in the background
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshAll(); vm.resumeUpdate(); vm.checkLastRun() }
    UpdateDialog(vm)
    GpuOfferDialog(vm)
    LastRunDialog(vm)
    TexFailDialog(vm)

    fun go(d: Dest) {
        // More tapped while one of its pages is shown: back to More's list
        if (d == Dest.MORE && Dest.tabOf(nav.currentDestination?.route) == Dest.MORE && nav.currentDestination?.route != Dest.MORE.route) {
            nav.popBackStack(Dest.MORE.route, inclusive = false)
            return
        }
        nav.navigate(d.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun open(d: Dest) = nav.navigate(d.route) { launchSingleTop = true }

    LaunchedEffect(startPage) {
        val d = Dest.entries.firstOrNull { it.route == startPage } ?: return@LaunchedEffect
        if (d.inBar) go(d) else { go(Dest.MORE); open(d) }
    }

    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxSize().background(colors.background).leafBackground(colors.primary, colors.secondary)) {
        if (wide) SideScroll(tab) { go(it) }
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    BusyBar(busy)
                    if (!wide) {
                        // five pages, each a fifth of the width
                        Surface(color = colors.surfaceContainerLowest.copy(alpha = 0.94f)) {
                            Row(
                                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                            ) {
                                Dest.bar.forEach { d ->
                                    NavItem(d, tab == d, Modifier.weight(1f)) { go(d) }
                                }
                            }
                        }
                    }
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
                NavHost(
                    nav, startDestination = Dest.HOME.route,
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                ) {
                    composable(Dest.HOME.route) { HomeScreen(vm) }
                    composable(Dest.ONLINE.route) { OnlineScreen(vm) }
                    composable(Dest.CONTROLS.route) { ControlsScreen(vm) }
                    composable(Dest.SETTINGS.route) { SettingsScreen(vm) }
                    composable(Dest.MORE.route) {
                        val accents = listOf(Konoha.SasukeBlueLight, Konoha.LeeGreen, Konoha.ChakraBlue, Konoha.ChakraAmber, Konoha.NarutoOrange)
                        MoreScreen(
                            Dest.more.mapIndexed { i, d ->
                                MoreItem(stringResource(d.label), stringResource(d.text), d.icon, accents[i % accents.size]) { open(d) }
                            },
                        )
                    }
                    val back = { nav.popBackStack(); Unit }
                    composable(Dest.TEXTURES.route) { SubPage(back) { TexturesScreen(vm) } }
                    composable(Dest.MODS.route) { SubPage(back) { ModsScreen(vm) } }
                    composable(Dest.DRIVERS.route) { SubPage(back) { DriversScreen(vm) } }
                    composable(Dest.CHEATS.route) { SubPage(back) { CheatsScreen(vm) } }
                    composable(Dest.DEBUG.route) { SubPage(back) { DebugScreen(vm) } }
                }
            }
        }
    }
}

/**
 * The side panel in landscape: a dark strip with a thin wooden edge, the app's leaf at the top and the pages under it;
 * the page shown sits on an orange plate with cut corners and a kunai pointing at it.
 */
@Composable
private fun SideScroll(tab: Dest?, go: (Dest) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxHeight().width(92.dp)
            .background(colors.surfaceContainerLowest.copy(alpha = 0.85f))
            .drawBehind {
                drawRect(Konoha.Wood, topLeft = Offset(size.width - 3.dp.toPx(), 0f), size = Size(3.dp.toPx(), size.height))
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // (the leaf only where the five pages still fit under it: a 1560x720 phone is 360 dp high in landscape)
        if (LocalConfiguration.current.screenHeightDp >= 420) {
            Icon(NinjaIcons.Leaf, null, tint = Konoha.LeeGreen, modifier = Modifier.padding(bottom = 8.dp).size(30.dp))
        }
        Dest.bar.forEach { d -> NavItem(d, tab == d, Modifier.width(80.dp), pad = 3.dp) { go(d) } }
    }
}

/** One page of the navigation: its icon on a plate that lights orange when it is the page shown, and its name. */
@Composable
private fun NavItem(d: Dest, selected: Boolean, modifier: Modifier, pad: Dp = 5.dp, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val plate = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    Column(
        modifier.clip(plate).clickable(onClick = onClick).padding(vertical = pad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(plate)
                .background(if (selected) Brush.horizontalGradient(listOf(Konoha.NarutoOrangeDeep, Konoha.NarutoOrange)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                .padding(horizontal = 16.dp, vertical = 5.dp),
        ) {
            Icon(d.icon, null, tint = if (selected) Konoha.NarutoBlack else colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
        Text(
            stringResource(d.label), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall, color = if (selected) colors.primary else colors.onSurfaceVariant,
        )
    }
}
