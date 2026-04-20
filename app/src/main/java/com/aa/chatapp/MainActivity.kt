package com.aa.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aa.chatapp.feature.chat.presentation.navigation.ChatNavHost
import com.aa.chatapp.ui.theme.ChatAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity — owns the Compose content tree.
 *
 * @AndroidEntryPoint makes this activity a Hilt injection target.
 * No business logic lives here; routing is delegated to [ChatNavHost].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAppTheme {
                ChatNavHost()
            }
        }
    }
}