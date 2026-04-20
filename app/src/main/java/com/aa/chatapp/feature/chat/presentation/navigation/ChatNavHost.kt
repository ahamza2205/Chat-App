package com.aa.chatapp.feature.chat.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Route constants — single source of truth for screen destinations. */
object ChatRoutes {
    const val CHAT = "chat"
}

/**
 * Root navigation graph for the Chat feature.
 *
 * Currently holds a single destination. Add new [composable] entries here
 * as new screens are introduced (e.g., media viewer, thread detail).
 *
 * TODO (Phase 4): Replace the placeholder with the real ChatScreen composable.
 */
@Composable
fun ChatNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ChatRoutes.CHAT,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = ChatRoutes.CHAT) {
            // TODO (Phase 4): Replace with ChatScreen(viewModel = hiltViewModel())
            ChatScreenPlaceholder()
        }
    }
}

/** Temporary placeholder — removed when ChatScreen is implemented in Phase 4. */
@Composable
private fun ChatScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Chat — coming in Phase 4",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
