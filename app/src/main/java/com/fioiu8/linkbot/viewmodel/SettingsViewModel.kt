package com.fioiu8.linkbot.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _useMonet = MutableStateFlow(true)
    val useMonet: StateFlow<Boolean> = _useMonet.asStateFlow()

    private val _colorSpec = MutableStateFlow(0)
    val colorSpec: StateFlow<Int> = _colorSpec.asStateFlow()

    private val _paletteStyle = MutableStateFlow(0)
    val paletteStyle: StateFlow<Int> = _paletteStyle.asStateFlow()

    private val _smoothRounding = MutableStateFlow(true)
    val smoothRounding: StateFlow<Boolean> = _smoothRounding.asStateFlow()

    private val _blurEnabled = MutableStateFlow(false)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _blurRadius = MutableStateFlow(60f)
    val blurRadius: StateFlow<Float> = _blurRadius.asStateFlow()

    private val _useCustomTheme = MutableStateFlow(false)
    val useCustomTheme: StateFlow<Boolean> = _useCustomTheme.asStateFlow()

    private val _customPrimaryColor = MutableStateFlow(Color(0xFF2196F3))
    val customPrimaryColor: StateFlow<Color> = _customPrimaryColor.asStateFlow()

    fun selectTab(index: Int) { _selectedTab.value = index }
    fun setThemeMode(mode: Int) { _themeMode.value = mode }
    fun setUseMonet(enabled: Boolean) { _useMonet.value = enabled }
    fun setColorSpec(spec: Int) { _colorSpec.value = spec }
    fun setPaletteStyle(style: Int) { _paletteStyle.value = style }
    fun setSmoothRounding(enabled: Boolean) { _smoothRounding.value = enabled }
    fun setBlurEnabled(enabled: Boolean) { _blurEnabled.value = enabled }
    fun setBlurRadius(radius: Float) { _blurRadius.value = radius }
    fun setUseCustomTheme(enabled: Boolean) { _useCustomTheme.value = enabled }
    fun setCustomPrimaryColor(color: Color) { _customPrimaryColor.value = color }
}