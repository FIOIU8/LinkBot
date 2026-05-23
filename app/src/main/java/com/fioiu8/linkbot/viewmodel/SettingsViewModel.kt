package com.fioiu8.linkbot.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("linkbot_settings", Context.MODE_PRIVATE)

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _themeMode = MutableStateFlow(loadInt("themeMode", 0))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _useMonet = MutableStateFlow(loadBoolean("useMonet", true))
    val useMonet: StateFlow<Boolean> = _useMonet.asStateFlow()

    private val _colorSpec = MutableStateFlow(loadInt("colorSpec", 0))
    val colorSpec: StateFlow<Int> = _colorSpec.asStateFlow()

    private val _paletteStyle = MutableStateFlow(loadInt("paletteStyle", 0))
    val paletteStyle: StateFlow<Int> = _paletteStyle.asStateFlow()

    private val _smoothRounding = MutableStateFlow(loadBoolean("smoothRounding", true))
    val smoothRounding: StateFlow<Boolean> = _smoothRounding.asStateFlow()

    private val _blurEnabled = MutableStateFlow(loadBoolean("blurEnabled", true))
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _blurRadius = MutableStateFlow(loadFloat("blurRadius", 60f))
    val blurRadius: StateFlow<Float> = _blurRadius.asStateFlow()

    private val _useCustomTheme = MutableStateFlow(loadBoolean("useCustomTheme", true))
    val useCustomTheme: StateFlow<Boolean> = _useCustomTheme.asStateFlow()

    private val _customPrimaryColor = MutableStateFlow(loadColor("customPrimaryColor", Color(0xFF2196F3)))
    val customPrimaryColor: StateFlow<Color> = _customPrimaryColor.asStateFlow()

    fun selectTab(index: Int) { 
        _selectedTab.value = index 
        saveInt("selectedTab", index)
    }
    
    fun setThemeMode(mode: Int) { 
        _themeMode.value = mode 
        saveInt("themeMode", mode)
    }
    
    fun setUseMonet(enabled: Boolean) { 
        _useMonet.value = enabled 
        saveBoolean("useMonet", enabled)
    }
    
    fun setColorSpec(spec: Int) { 
        _colorSpec.value = spec 
        saveInt("colorSpec", spec)
    }
    
    fun setPaletteStyle(style: Int) { 
        _paletteStyle.value = style 
        saveInt("paletteStyle", style)
    }
    
    fun setSmoothRounding(enabled: Boolean) { 
        _smoothRounding.value = enabled 
        saveBoolean("smoothRounding", enabled)
    }
    
    fun setBlurEnabled(enabled: Boolean) { 
        _blurEnabled.value = enabled 
        saveBoolean("blurEnabled", enabled)
    }
    
    fun setBlurRadius(radius: Float) { 
        _blurRadius.value = radius 
        saveFloat("blurRadius", radius)
    }
    
    fun setUseCustomTheme(enabled: Boolean) { 
        _useCustomTheme.value = enabled 
        saveBoolean("useCustomTheme", enabled)
    }
    
    fun setCustomPrimaryColor(color: Color) { 
        _customPrimaryColor.value = color 
        saveColor("customPrimaryColor", color)
    }

    fun saveAllSettings() {
        sharedPreferences.edit().apply {
            putInt("themeMode", _themeMode.value)
            putBoolean("useMonet", _useMonet.value)
            putInt("colorSpec", _colorSpec.value)
            putInt("paletteStyle", _paletteStyle.value)
            putBoolean("smoothRounding", _smoothRounding.value)
            putBoolean("blurEnabled", _blurEnabled.value)
            putFloat("blurRadius", _blurRadius.value)
            putBoolean("useCustomTheme", _useCustomTheme.value)
            putLong("customPrimaryColor", colorToLong(_customPrimaryColor.value))
            apply()
        }
    }

    private fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    private fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    private fun saveColor(key: String, value: Color) {
        sharedPreferences.edit().putLong(key, colorToLong(value)).apply()
    }

    private fun loadInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    private fun loadBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun loadFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    private fun loadColor(key: String, defaultValue: Color): Color {
        val longValue = sharedPreferences.getLong(key, colorToLong(defaultValue))
        return longToColor(longValue)
    }

    private fun colorToLong(color: Color): Long {
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        ).toLong()
    }

    private fun longToColor(longValue: Long): Color {
        val color = android.graphics.Color.valueOf(longValue.toFloat())
        return Color(color.red(), color.green(), color.blue(), color.alpha())
    }
}
