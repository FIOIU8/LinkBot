package com.fioiu8.linkbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fioiu8.linkbot.ui.MainApp
import com.fioiu8.linkbot.ui.theme.LinkBotTheme
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SettingsViewModel(applicationContext)
                    }
                }
            )

            LinkBotTheme(settingsViewModel = settingsViewModel) {
                MainApp(
                    chatViewModel = chatViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
