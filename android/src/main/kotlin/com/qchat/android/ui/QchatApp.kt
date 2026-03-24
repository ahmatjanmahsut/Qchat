package com.qchat.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun QchatApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            // ChatListScreen - will be implemented
        }
        composable("chat/{chatId}") {
            // ChatScreen - will be implemented
        }
    }
}
