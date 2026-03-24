package com.qchat.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qchat.app.ui.screens.MainScreen
import com.qchat.app.ui.screens.settings.ServerConfigScreen
import com.qchat.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object ServerConfig : Screen("server_config")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.ServerConfig.route) {
            ServerConfigScreen()
        }
    }
}
