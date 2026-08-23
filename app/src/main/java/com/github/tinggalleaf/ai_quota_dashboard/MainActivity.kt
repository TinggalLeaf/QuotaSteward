package com.github.tinggalleaf.ai_quota_dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.tinggalleaf.ai_quota_dashboard.ui.dashboard.DashboardScreen
import com.github.tinggalleaf.ai_quota_dashboard.ui.dashboard.DashboardViewModel
import com.github.tinggalleaf.ai_quota_dashboard.ui.editor.EditorScreen
import com.github.tinggalleaf.ai_quota_dashboard.ui.editor.PresetPickerScreen
import com.github.tinggalleaf.ai_quota_dashboard.ui.nav.Destination
import com.github.tinggalleaf.ai_quota_dashboard.ui.services.ServicesScreen
import com.github.tinggalleaf.ai_quota_dashboard.ui.settings.SettingsScreen
import com.github.tinggalleaf.ai_quota_dashboard.ui.theme.AIQuotaTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIQuotaTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = remember {
        setOf(
            Destination.Dashboard.route,
            Destination.Services.route,
            Destination.Settings.route,
        )
    }

    val isTabScreen = currentRoute in tabRoutes

    val navigateTab: (String) -> Unit = { target ->
        if (currentRoute != target) {
            nav.navigate(target) {
                popUpTo(Destination.Dashboard.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (isTabScreen) {
                when (currentRoute) {
                    Destination.Dashboard.route -> SmallTopAppBar(title = "AI 额度")
                    Destination.Services.route -> SmallTopAppBar(title = "服务管理")
                    Destination.Settings.route -> SmallTopAppBar(title = "设置")
                }
            }
        },
        bottomBar = {
            if (isTabScreen) {
                val selected = when (currentRoute) {
                    Destination.Services.route -> 1
                    Destination.Settings.route -> 2
                    else -> 0
                }
                NavigationBar {
                    NavigationBarItem(
                        selected = selected == 0,
                        onClick = { navigateTab(Destination.Dashboard.route) },
                        icon = MiuixIcons.Folder,
                        label = "总览",
                    )
                    NavigationBarItem(
                        selected = selected == 1,
                        onClick = { navigateTab(Destination.Services.route) },
                        icon = MiuixIcons.Add,
                        label = "服务",
                    )
                    NavigationBarItem(
                        selected = selected == 2,
                        onClick = { navigateTab(Destination.Settings.route) },
                        icon = MiuixIcons.Settings,
                        label = "设置",
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Destination.Dashboard.route,
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(
                    onAdd = { nav.navigate(Destination.AddService.build("new")) },
                    onEdit = { svc -> nav.navigate(Destination.EditService.build(svc.id)) },
                    onPickPreset = { nav.navigate(Destination.AddFromPreset.build()) },
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
            composable(Destination.Services.route) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
                val entries by vm.entries.collectAsState()
                ServicesScreen(
                    entries = entries,
                    onAdd = { nav.navigate(Destination.AddService.build("new")) },
                    onEdit = { svc -> nav.navigate(Destination.EditService.build(svc.id)) },
                    onPickPreset = { nav.navigate(Destination.AddFromPreset.build()) },
                    onRefresh = { id -> vm.refreshOne(id) },
                    onToggle = { svc, on -> vm.toggleEnabled(svc, on) },
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
            composable(Destination.AddFromPreset.route) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
                PresetPickerScreen(
                    presets = vm.builtInPresets,
                    onPick = { preset ->
                        vm.addPreset(preset)
                        nav.popBackStack()
                    },
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
            composable(
                route = Destination.EditService.PATH,
                arguments = listOf(navArgument(Destination.EditService.ARG) { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString(Destination.EditService.ARG) ?: "new"
                val initial = remember(id) {
                    if (id == "new") null
                    else ServiceLocator.repository.builtInPresets.firstOrNull { it.id == id }
                        ?: runBlocking {
                            ServiceLocator.repository.services.let { flow ->
                                var result: com.github.tinggalleaf.ai_quota_dashboard.data.model.ServiceConfig? = null
                                flow.collect { list ->
                                    result = list.firstOrNull { it.id == id }
                                    return@collect
                                }
                                result
                            }
                        }
                }
                EditorScreen(
                    initial = initial,
                    onSave = { svc ->
                        kotlinx.coroutines.GlobalScope.launch {
                            ServiceLocator.repository.upsert(svc)
                        }
                        nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() },
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
        }
    }
}
