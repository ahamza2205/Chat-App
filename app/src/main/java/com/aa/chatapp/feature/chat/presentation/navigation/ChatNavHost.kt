package com.aa.chatapp.feature.chat.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aa.chatapp.feature.chat.presentation.chat.ChatScreen
import com.aa.chatapp.feature.chat.presentation.profile.ProfileScreen

private const val ROUTE_CHAT = "chat"
private const val ROUTE_PROFILE = "profile"

@Composable
fun ChatNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ROUTE_CHAT,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = ROUTE_CHAT) {
            ChatScreen(onNavigateToProfile = { navController.navigate(ROUTE_PROFILE) })
        }
        composable(route = ROUTE_PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}
