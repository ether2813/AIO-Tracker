package com.aio.tracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aio.tracker.AppContainer
import com.aio.tracker.container
import com.aio.tracker.data.prefs.AppSettings
import com.aio.tracker.reminders.Notifications
import com.aio.tracker.ui.ai.AiScreen
import com.aio.tracker.ui.fitness.FitnessScreen
import com.aio.tracker.ui.home.HomeScreen
import com.aio.tracker.ui.money.MoneyScreen
import com.aio.tracker.ui.planner.NoteEditorScreen
import com.aio.tracker.ui.planner.PlannerScreen
import com.aio.tracker.ui.salat.SalatScreen
import com.aio.tracker.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class TopDestination(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    SALAT("salat", "Salat", Icons.Rounded.DarkMode, Icons.Outlined.DarkMode),
    FITNESS("fitness", "Fitness", Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter),
    MONEY("money", "Money", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PLANNER("planner", "Planner", Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt),
}

object Routes {
    const val AI = "ai"
    const val SETTINGS = "settings"
    const val NOTE = "note/{id}"
    fun note(id: Long) = "note/$id"
    const val NEW_NOTE_ID = -1L
}

/** Creates a ViewModel with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel { create(container) }
}

fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AioRoot(
    settings: AppSettings,
    pendingDestination: String?,
    onDestinationConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = TopDestination.entries.any { it.route == currentRoute }

    // Ask for notification permission once, so prayer reminders can be shown.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        context.container.reschedulePrayerReminders()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !settings.askedNotificationPermission && !Notifications.canPost(context)) {
            scope.launch { runCatching { context.container.settings.setAskedNotificationPermission() } }
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pendingDestination) {
        val dest = pendingDestination ?: return@LaunchedEffect
        if (TopDestination.entries.any { it.route == dest }) nav.navigateTop(dest)
        onDestinationConsumed()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (!selected) nav.navigateTop(dest.route) },
                            icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 30 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 30 } },
        ) {
            composable(TopDestination.HOME.route) {
                HomeScreen(
                    onOpen = { nav.navigateTop(it) },
                    onOpenAi = { nav.navigate(Routes.AI) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                )
            }
            composable(TopDestination.SALAT.route) { SalatScreen(onOpenSettings = { nav.navigate(Routes.SETTINGS) }) }
            composable(TopDestination.FITNESS.route) { FitnessScreen() }
            composable(TopDestination.MONEY.route) { MoneyScreen() }
            composable(TopDestination.PLANNER.route) {
                PlannerScreen(onOpenNote = { nav.navigate(Routes.note(it)) })
            }
            composable(
                Routes.AI,
                enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)) },
            ) {
                AiScreen(onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(
                Routes.SETTINGS,
                enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)) },
            ) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.NOTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { slideInVertically(tween(280)) { it / 8 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutVertically(tween(220)) { it / 8 } + fadeOut(tween(180)) },
            ) { entry ->
                NoteEditorScreen(
                    noteId = entry.arguments?.getLong("id") ?: Routes.NEW_NOTE_ID,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
