package com.fioiu8.linkbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                .layerBackdrop(backdrop)
                .padding(bottom = 88.dp)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(chatViewModel, settingsViewModel)
                1 -> NotesScreen(chatViewModel, settingsViewModel)
                2 -> SettingsScreen(chatViewModel, settingsViewModel)
                3 -> AboutScreen(settingsViewModel)
                else -> ChatScreen(chatViewModel, settingsViewModel)
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
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pageIcons[index],
                        contentDescription = pageTitles[index],
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MiuixTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    pageTitles.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { settingsViewModel.selectTab(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = pageIcons[index],
                                    contentDescription = label,
                                    tint = if (selectedTab == index)
                                        MiuixTheme.colorScheme.primary
                                    else
                                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = label,
                                    style = MiuixTheme.textStyles.subtitle,
                                    color = if (selectedTab == index)
                                        MiuixTheme.colorScheme.primary
                                    else
                                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
