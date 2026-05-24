package com.fioiu8.linkbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fioiu8.linkbot.ui.components.LiquidBottomTabs
import com.fioiu8.linkbot.ui.screens.*
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainApp(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val selectedTab by settingsViewModel.selectedTab.collectAsState()
    val blurEnabled by settingsViewModel.blurEnabled.collectAsState()

    val pageTitles = listOf("聊天", "笔记", "设置", "关于")
    val pageIcons = listOf(
        MiuixIcons.Messages,
        MiuixIcons.Notes,
        MiuixIcons.Settings,
        MiuixIcons.Info
    )

    val backdrop = rememberLayerBackdrop()
    val backgroundColor = MiuixTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurEnabled) Modifier.layerBackdrop(backdrop)
                    else Modifier
                )
                .then(
                    if (!blurEnabled) Modifier.padding(bottom = 80.dp)
                    else Modifier
                )
        ) {
            val chatScreen = remember {
                @Composable { ChatScreen(chatViewModel, settingsViewModel) }
            }
            val notesScreen = remember {
                @Composable { NotesScreen(chatViewModel, settingsViewModel) }
            }
            val settingsScreen = remember {
                @Composable { SettingsScreen(chatViewModel, settingsViewModel) }
            }
            val aboutScreen = remember {
                @Composable { AboutScreen(settingsViewModel) }
            }

            val screens = listOf(chatScreen, notesScreen, settingsScreen, aboutScreen)

            screens.forEachIndexed { index, screen ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == index) {
                        screen()
                    }
                }
            }
        }

        if (blurEnabled) {
            LiquidBottomTabs(
                selectedTabIndex = { selectedTab },
                onTabSelected = { settingsViewModel.selectTab(it) },
                backdrop = backdrop,
                tabsCount = pageTitles.size,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) { index ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pageIcons[index],
                        contentDescription = pageTitles[index]
                    )
                }
            }
        } else {
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                pageTitles.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { settingsViewModel.selectTab(index) },
                        icon = pageIcons[index],
                        label = label
                    )
                }
            }
        }
    }
}
