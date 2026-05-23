package com.fioiu8.linkbot.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.theme.*

@Composable
fun LinkBotTheme(
    settingsViewModel: SettingsViewModel? = null,
    content: @Composable () -> Unit
) {
    val themeMode by (settingsViewModel?.themeMode?.collectAsState() ?: remember { mutableStateOf(0) })
    val useMonet by (settingsViewModel?.useMonet?.collectAsState() ?: remember { mutableStateOf(true) })
    val colorSpec by (settingsViewModel?.colorSpec?.collectAsState() ?: remember { mutableStateOf(0) })
    val paletteStyle by (settingsViewModel?.paletteStyle?.collectAsState() ?: remember { mutableStateOf(0) })
    val smoothRounding by (settingsViewModel?.smoothRounding?.collectAsState() ?: remember { mutableStateOf(true) })
    val useCustomTheme by (settingsViewModel?.useCustomTheme?.collectAsState() ?: remember { mutableStateOf(false) })
    val customPrimaryColor by (settingsViewModel?.customPrimaryColor?.collectAsState() ?: remember { mutableStateOf(Color(0xFF2196F3)) })

    val controller = remember(themeMode, useMonet, colorSpec, paletteStyle, customPrimaryColor, useCustomTheme) {
        val mode = when {
            useMonet && themeMode == 0 -> ColorSchemeMode.MonetSystem
            useMonet && themeMode == 1 -> ColorSchemeMode.MonetLight
            useMonet && themeMode == 2 -> ColorSchemeMode.MonetDark
            !useMonet && themeMode == 0 -> ColorSchemeMode.System
            !useMonet && themeMode == 1 -> ColorSchemeMode.Light
            !useMonet && themeMode == 2 -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }

        ThemeController(
            colorSchemeMode = mode,
            keyColor = if (useCustomTheme) customPrimaryColor else null,
            colorSpec = if (colorSpec == 0) ThemeColorSpec.Spec2021 else ThemeColorSpec.Spec2025,
            paletteStyle = ThemePaletteStyle.entries.getOrElse(paletteStyle) { ThemePaletteStyle.TonalSpot }
        )
    }

    MiuixTheme(
        controller = controller,
        smoothRounding = smoothRounding,
        content = content
    )
}