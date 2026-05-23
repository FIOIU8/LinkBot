package com.fioiu8.linkbot.data

import android.content.Context
import com.fioiu8.linkbot.model.ApiConfig
import com.google.gson.Gson

/**
 * 偏好设置管理器 - 负责应用设置的持久化存储
 *
 * 使用 SharedPreferences 存储各种配置项，包括 API 配置、头像路径、主题设置等。
 *
 * @param context Android 上下文
 */
class PreferencesManager(context: Context) {

    // SharedPreferences 实例
    private val prefs = context.getSharedPreferences("linkbot_prefs", Context.MODE_PRIVATE)
    
    // Gson 实例，用于 JSON 序列化/反序列化
    private val gson = Gson()

    /**
     * 保存 API 配置
     * @param config API 配置对象
     */
    fun saveApiConfig(config: ApiConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("api_config", json).apply()
    }

    /**
     * 加载 API 配置
     * @return API 配置对象，如果不存在返回 null
     */
    fun loadApiConfig(): ApiConfig? {
        val json = prefs.getString("api_config", null)
        return if (json != null) {
            gson.fromJson(json, ApiConfig::class.java)
        } else null
    }

    /**
     * 保存用户头像路径
     * @param path 头像路径
     */
    fun saveUserAvatarPath(path: String?) {
        prefs.edit().putString("user_avatar", path).apply()
    }

    /**
     * 加载用户头像路径
     * @return 用户头像路径，如果不存在返回 null
     */
    fun loadUserAvatarPath(): String? {
        return prefs.getString("user_avatar", null)
    }

    /**
     * 保存 AI 头像路径
     * @param path 头像路径
     */
    fun saveAiAvatarPath(path: String?) {
        prefs.edit().putString("ai_avatar", path).apply()
    }

    /**
     * 加载 AI 头像路径
     * @return AI 头像路径，如果不存在返回 null
     */
    fun loadAiAvatarPath(): String? {
        return prefs.getString("ai_avatar", null)
    }

    /**
     * 保存思考模式开关状态
     * @param enabled 是否启用
     */
    fun saveEnableThinking(enabled: Boolean) {
        prefs.edit().putBoolean("enable_thinking", enabled).apply()
    }

    /**
     * 加载思考模式开关状态
     * @return 是否启用思考模式，默认 false
     */
    fun loadEnableThinking(): Boolean {
        return prefs.getBoolean("enable_thinking", false)
    }

    /**
     * 保存思考内容展开状态
     * @param enabled 是否展开
     */
    fun saveEnableThinkingExpanded(enabled: Boolean) {
        prefs.edit().putBoolean("enable_thinking_expanded", enabled).apply()
    }

    /**
     * 加载思考内容展开状态
     * @return 是否展开思考内容，默认 false
     */
    fun loadEnableThinkingExpanded(): Boolean {
        return prefs.getBoolean("enable_thinking_expanded", false)
    }

    /**
     * 保存工具调用开关状态
     * @param enabled 是否启用
     */
    fun saveEnableToolCalls(enabled: Boolean) {
        prefs.edit().putBoolean("enable_tool_calls", enabled).apply()
    }

    /**
     * 加载工具调用开关状态
     * @return 是否启用工具调用，默认 false
     */
    fun loadEnableToolCalls(): Boolean {
        return prefs.getBoolean("enable_tool_calls", false)
    }

    /**
     * 保存默认展开思考状态
     * @param expand 是否默认展开
     */
    fun saveDefaultExpandThinking(expand: Boolean) {
        prefs.edit().putBoolean("default_expand_thinking", expand).apply()
    }

    /**
     * 加载默认展开思考状态
     * @return 是否默认展开思考内容，默认 false
     */
    fun loadDefaultExpandThinking(): Boolean {
        return prefs.getBoolean("default_expand_thinking", false)
    }

    /**
     * 保存思考深度设置
     * @param effort 思考深度（high/medium/low）
     */
    fun saveReasoningEffort(effort: String) {
        prefs.edit().putString("reasoning_effort", effort).apply()
    }

    /**
     * 加载思考深度设置
     * @return 思考深度，默认 high
     */
    fun loadReasoningEffort(): String {
        return prefs.getString("reasoning_effort", "high") ?: "high"
    }

    /**
     * 保存主题模式
     * @param mode 主题模式（0-自动，1-浅色，2-深色）
     */
    fun saveThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    /**
     * 加载主题模式
     * @return 主题模式，默认 0（自动）
     */
    fun loadThemeMode(): Int {
        return prefs.getInt("theme_mode", 0)
    }

    /**
     * 保存动态取色开关状态
     * @param use 是否使用动态取色
     */
    fun saveUseMonet(use: Boolean) {
        prefs.edit().putBoolean("use_monet", use).apply()
    }

    /**
     * 加载动态取色开关状态
     * @return 是否使用动态取色，默认 true
     */
    fun loadUseMonet(): Boolean {
        return prefs.getBoolean("use_monet", true)
    }

    /**
     * 保存颜色规格
     * @param spec 颜色规格索引
     */
    fun saveColorSpec(spec: Int) {
        prefs.edit().putInt("color_spec", spec).apply()
    }

    /**
     * 加载颜色规格
     * @return 颜色规格索引，默认 0
     */
    fun loadColorSpec(): Int {
        return prefs.getInt("color_spec", 0)
    }
}