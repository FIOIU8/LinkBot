package com.fioiu8.linkbot.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fioiu8.linkbot.model.BalanceResponse
import com.fioiu8.linkbot.data.AvatarManager
import com.fioiu8.linkbot.data.ConversationData
import com.fioiu8.linkbot.data.ConversationManager
import com.fioiu8.linkbot.data.PreferencesManager
import com.fioiu8.linkbot.data.ToolManager
import com.fioiu8.linkbot.model.ApiConfig
import com.fioiu8.linkbot.model.ChatMessage
import com.fioiu8.linkbot.model.Message
import com.fioiu8.linkbot.model.SavedNote
import com.fioiu8.linkbot.model.ToolCall
import com.fioiu8.linkbot.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ChatViewModel - 聊天界面的核心 ViewModel
 *
 * 职责：
 * - 管理聊天消息的状态（发送、接收、显示）
 * - 处理与 AI API 的通信
 * - 管理对话历史和笔记的持久化
 * - 处理工具调用（如搜索、计算器等）
 * - 管理用户和 AI 头像
 *
 * 使用 StateFlow 暴露不可变状态给 UI 层，
 * UI 通过 collectAsState() 观察状态变化
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // ==================== 依赖注入 ====================
    /** PreferencesManager: 管理用户配置（API Key、主题设置等） */
    private val prefs = PreferencesManager(application)

    /** AvatarManager: 管理用户和 AI 头像的存储 */
    val avatarManager = AvatarManager(application)

    /** ConversationManager: 管理对话和笔记的持久化（带缓存） */
    val conversationManager = ConversationManager(application)

    // ==================== API 配置状态 ====================
    /** API 配置：包含 Provider、模型、API Key 等 */
    private val _apiConfig = MutableStateFlow(prefs.loadApiConfig() ?: ApiConfig())
    val apiConfig: StateFlow<ApiConfig> = _apiConfig.asStateFlow()

    // ==================== 聊天消息状态 ====================
    /** 聊天消息列表，包含用户消息、AI 消息和工具调用消息 */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** 是否正在等待 AI 响应（显示加载状态） */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 错误消息（如 API Key 未配置、请求失败等） */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== 显示设置状态 ====================
    /** 上下文消息数量（发送给 API 的历史消息条数） */
    private val _contextCount = MutableStateFlow(10)
    val contextCount: StateFlow<Int> = _contextCount.asStateFlow()

    /** 用户头像路径（本地文件路径） */
    private val _userAvatarPath = MutableStateFlow(prefs.loadUserAvatarPath())
    val userAvatarPath: StateFlow<String?> = _userAvatarPath.asStateFlow()

    /** AI 头像路径 */
    private val _aiAvatarPath = MutableStateFlow(prefs.loadAiAvatarPath())
    val aiAvatarPath: StateFlow<String?> = _aiAvatarPath.asStateFlow()

    // ==================== 笔记功能状态 ====================
    /** 保存的笔记列表 */
    private val _notes = MutableStateFlow<List<SavedNote>>(emptyList())
    val notes: StateFlow<List<SavedNote>> = _notes.asStateFlow()

    /** 对话历史列表 */
    private val _conversations = MutableStateFlow<List<ConversationData>>(emptyList())
    val conversations: StateFlow<List<ConversationData>> = _conversations.asStateFlow()

    // ==================== DeepSeek 余额查询 ====================
    /** DeepSeek API 余额信息 */
    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance: StateFlow<BalanceResponse?> = _balance.asStateFlow()

    /** 是否正在加载余额信息 */
    private val _isBalanceLoading = MutableStateFlow(false)
    val isBalanceLoading: StateFlow<Boolean> = _isBalanceLoading.asStateFlow()

    // ==================== 深度思考状态 ====================
    /** 已展开的思考内容索引集合（用于 UI 显示控制） */
    private val _expandedThinking = MutableStateFlow<Set<Int>>(emptySet())
    val expandedThinking: StateFlow<Set<Int>> = _expandedThinking.asStateFlow()

    /** 默认是否展开思考内容 */
    private val _defaultExpandThinking = MutableStateFlow(prefs.loadDefaultExpandThinking())
    val defaultExpandThinking: StateFlow<Boolean> = _defaultExpandThinking.asStateFlow()

    /** 是否启用深度思考模式 */
    private val _enableThinking = MutableStateFlow(prefs.loadEnableThinking())
    val enableThinking: StateFlow<Boolean> = _enableThinking.asStateFlow()

    /** 是否自动展开思考内容 */
    private val _enableThinkingExpanded = MutableStateFlow(prefs.loadEnableThinkingExpanded())
    val enableThinkingExpanded: StateFlow<Boolean> = _enableThinkingExpanded.asStateFlow()

    /** 是否启用工具调用 */
    private val _enableToolCalls = MutableStateFlow(prefs.loadEnableToolCalls())
    val enableToolCalls: StateFlow<Boolean> = _enableToolCalls.asStateFlow()

    /** 推理强度（low / medium / high） */
    private val _reasoningEffort = MutableStateFlow("high")
    val reasoningEffort: StateFlow<String> = _reasoningEffort.asStateFlow()

    // ==================== UI 事件 ====================
    /** Snackbar 消息事件（一次性事件，用于显示提示） */
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    // ==================== 初始化 ====================
    init {
        // 如果配置了 DeepSeek API Key，自动查询余额
        val config = _apiConfig.value
        if (config.provider == "deepseek" && config.apiKey.isNotBlank()) {
            queryBalance()
        }
        // 异步加载笔记和对话历史
        viewModelScope.launch {
            _notes.value = conversationManager.getAllNotes()
            _conversations.value = conversationManager.getAllConversations()
        }
    }

    // ==================== 自动保存 ====================
    /**
     * 自动保存当前对话到持久化存储
     * 当对话结束时调用，使用第一条消息的前30个字符作为对话名称
     */
    private suspend fun autoSaveConversation() {
        val msgs = _messages.value
        if (msgs.size < 2) return
        val name = msgs.firstOrNull()?.content?.take(30)?.replace("\n", " ") ?: "新对话"
        conversationManager.saveConversation(msgs, name)
        _conversations.value = conversationManager.getAllConversations()
    }

    // ==================== API 配置管理 ====================
    /**
     * 更新 API 配置并持久化
     * @param config 新的 API 配置
     */
    fun updateApiConfig(config: ApiConfig) {
        _apiConfig.value = config
        prefs.saveApiConfig(config)
        viewModelScope.launch { _snackbarEvent.emit("配置已保存") }
    }

    // ==================== 消息操作 ====================
    /** 清除错误消息 */
    fun clearError() { _errorMessage.value = null }

    /** 清空所有聊天消息 */
    fun clearMessages() { _messages.value = emptyList() }

    /** 设置上下文消息数量 */
    fun setContextCount(count: Int) { _contextCount.value = count }

    // ==================== 思考模式设置 ====================
    /**
     * 设置是否启用深度思考模式
     * @param enabled 是否启用
     */
    fun setEnableThinking(enabled: Boolean) {
        _enableThinking.value = enabled
        prefs.saveEnableThinking(enabled)
    }

    /**
     * 设置是否自动展开思考内容
     * @param enabled 是否启用
     */
    fun setEnableThinkingExpanded(enabled: Boolean) {
        _enableThinkingExpanded.value = enabled
        prefs.saveEnableThinkingExpanded(enabled)
    }

    /**
     * 设置默认是否展开思考内容
     * @param expand 是否展开
     */
    fun setDefaultExpandThinking(expand: Boolean) {
        _defaultExpandThinking.value = expand
        prefs.saveDefaultExpandThinking(expand)
    }

    // ==================== 工具调用设置 ====================
    /**
     * 设置是否启用工具调用
     * @param enabled 是否启用
     */
    fun setEnableToolCalls(enabled: Boolean) {
        _enableToolCalls.value = enabled
        prefs.saveEnableToolCalls(enabled)
    }

    /**
     * 设置推理强度
     * @param effort 推理强度（low / medium / high）
     */
    fun setReasoningEffort(effort: String) {
        _reasoningEffort.value = effort
        prefs.saveReasoningEffort(effort)
    }

    // ==================== 头像管理 ====================
    /**
     * 设置用户头像
     * @param uri 头像图片的 Uri
     */
    fun setUserAvatar(uri: Uri) {
        val path = avatarManager.saveAvatar(uri, "user")
        _userAvatarPath.value = path
        prefs.saveUserAvatarPath(path)
    }

    /**
     * 设置 AI 头像
     * @param uri 头像图片的 Uri
     */
    fun setAiAvatar(uri: Uri) {
        val path = avatarManager.saveAvatar(uri, "ai")
        _aiAvatarPath.value = path
        prefs.saveAiAvatarPath(path)
    }

    /** 移除用户头像 */
    fun removeUserAvatar() {
        avatarManager.deleteAvatar("user")
        _userAvatarPath.value = null
        prefs.saveUserAvatarPath(null)
    }

    /** 移除 AI 头像 */
    fun removeAiAvatar() {
        avatarManager.deleteAvatar("ai")
        _aiAvatarPath.value = null
        prefs.saveAiAvatarPath(null)
    }

    // ==================== 思考内容展开控制 ====================
    /**
     * 切换指定索引的思考内容展开状态
     * @param index 消息索引
     */
    fun toggleThinking(index: Int) {
        val set = _expandedThinking.value.toMutableSet()
        if (set.contains(index)) {
            set.remove(index)
        } else {
            set.add(index)
        }
        _expandedThinking.value = set
    }

    // ==================== 余额查询 ====================
    /** 查询 DeepSeek API 余额 */
    fun queryBalance() {
        val config = _apiConfig.value
        if (config.apiKey.isBlank() || config.provider != "deepseek") {
            _balance.value = null
            return
        }
        _isBalanceLoading.value = true
        viewModelScope.launch {
            ApiService.queryBalance(config.apiKey)
                .onSuccess { _balance.value = it }
                .onFailure { _balance.value = null }
            _isBalanceLoading.value = false
        }
    }

    // ==================== 笔记操作 ====================
    /**
     * 保存 AI 回复到笔记
     * @param aiIndex AI 消息的索引
     */
    fun saveNote(aiIndex: Int) {
        val msgs = _messages.value
        if (aiIndex < 1 || aiIndex >= msgs.size) return

        val aiMsg = msgs[aiIndex]
        val userMsg = (aiIndex - 1 downTo 0)
            .map { msgs[it] }
            .firstOrNull { it.role == "user" }
            ?: return

        if (aiMsg.role == "assistant" && aiMsg.content.isNotBlank()) {
            viewModelScope.launch {
                conversationManager.saveNote(userMsg, aiMsg)
                _notes.value = conversationManager.getAllNotes()
                _snackbarEvent.emit("已保存到笔记")
            }
        }
    }

    /**
     * 开始编辑消息
     * @param index 消息索引
     * @return 消息内容（如果是用户消息），否则返回 null
     */
    fun startEdit(index: Int): String? {
        val msgs = _messages.value
        if (index < 0 || index >= msgs.size) return null
        val msg = msgs[index]
        if (msg.role != "user") return null
        return msg.content
    }

    /**
     * 替换并重发消息
     * @param editIndex 要替换的消息索引
     * @param newContent 新的消息内容
     */
    fun replaceAndResend(editIndex: Int, newContent: String) {
        val msgs = _messages.value
        _messages.value = msgs.take(editIndex)
        sendMessage(newContent)
    }

    /**
     * 删除笔记
     * @param id 笔记 ID
     */
    fun deleteNote(id: String) {
        viewModelScope.launch {
            conversationManager.deleteNote(id)
            _notes.value = conversationManager.getAllNotes()
        }
    }

    /** 刷新笔记列表 */
    fun refreshNotes() {
        viewModelScope.launch {
            _notes.value = conversationManager.getAllNotes()
        }
    }

    /** 清空所有笔记 */
    fun clearNotes() {
        viewModelScope.launch {
            conversationManager.deleteAllNotes()
            _notes.value = emptyList()
            _snackbarEvent.emit("已清空所有笔记")
        }
    }

    /**
     * 从笔记继续对话
     * @param note 笔记对象
     */
    fun continueFromNote(note: SavedNote) {
        val existing = _messages.value.toMutableList()
        existing.add(ChatMessage(role = "user", content = note.userMessage))
        existing.add(ChatMessage(role = "assistant", content = note.aiMessage))
        _messages.value = existing
    }

    // ==================== 对话历史操作 ====================
    /**
     * 保存当前对话
     * @param name 对话名称（默认为第一条消息的前20个字符）
     */
    fun saveCurrentConversation(name: String = "") {
        if (_messages.value.isEmpty()) return
        val conversationName = name.ifBlank {
            _messages.value.firstOrNull()?.content?.take(20) ?: "对话"
        }
        viewModelScope.launch {
            conversationManager.saveConversation(_messages.value, conversationName)
            _conversations.value = conversationManager.getAllConversations()
        }
    }

    /**
     * 加载对话历史
     * @param id 对话 ID
     */
    fun loadConversation(id: String) {
        viewModelScope.launch {
            conversationManager.loadConversation(id)?.let {
                _messages.value = it.messages
            }
        }
    }

    /**
     * 删除对话
     * @param id 对话 ID
     */
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationManager.deleteConversation(id)
            _conversations.value = conversationManager.getAllConversations()
        }
    }

    /** 刷新对话列表 */
    fun refreshConversations() {
        viewModelScope.launch {
            _conversations.value = conversationManager.getAllConversations()
        }
    }

    // ==================== 消息重发 ====================
    /**
     * 重发消息
     * @param index 消息索引
     */
    fun resendMessage(index: Int) {
        val msgs = _messages.value
        if (index < 0 || index >= msgs.size) return
        val userMsg = msgs[index]
        if (userMsg.role == "user") {
            _messages.value = msgs.take(index)
            sendMessage(userMsg.content)
        }
    }

    // ==================== 消息发送 ====================
    /**
     * 发送消息到 AI API
     * @param content 用户输入的消息内容
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val config = _apiConfig.value

        // 检查 API Key 是否配置
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置 API Key"
            viewModelScope.launch { _snackbarEvent.emit("请先配置 API Key") }
            return
        }

        // 添加用户消息到列表
        _messages.value = _messages.value + ChatMessage(role = "user", content = content)
        val aiMsgIndex = _messages.value.size

        // 添加空的 AI 响应消息（用于实时更新）
        _messages.value = _messages.value + ChatMessage(role = "assistant", content = "", reasoningContent = "")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 构建消息列表
                val messageList = buildMessageList(config)

                var fullContent = ""
                var fullReasoning = ""

                // 发送流式请求
                val result = ApiService.sendMessageStream(
                    apiKey = config.apiKey,
                    model = config.model,
                    messages = messageList,
                    provider = config.provider,
                    customBaseUrl = config.customBaseUrl,
                    enableThinking = _enableThinking.value,
                    reasoningEffort = _reasoningEffort.value,
                    enableToolCalls = _enableToolCalls.value
                ) { chunk ->
                    // 处理流式响应
                    if (chunk.content.isNotEmpty()) fullContent += chunk.content
                    if (chunk.reasoningContent.isNotEmpty()) fullReasoning += chunk.reasoningContent

                    updateMessageContent(aiMsgIndex, fullContent, fullReasoning)
                }

                handleSendResult(result, aiMsgIndex, fullContent, fullReasoning)

            } catch (e: Exception) {
                handleSendError(aiMsgIndex, e.message ?: "未知错误")
            }
        }
    }

    /**
     * 构建发送给 API 的消息列表
     * @param config 当前 API 配置
     * @return 格式化后的消息列表
     */
    private fun buildMessageList(config: ApiConfig): List<Message> {
        val messageList = mutableListOf<Message>()

        // 添加系统提示词
        if (config.systemPrompt.isNotBlank()) {
            messageList.add(Message("system", config.systemPrompt))
        }

        // 添加历史消息（排除最后一条空消息）
        val messagesUpToNow = _messages.value.dropLast(1)
        messagesUpToNow.forEach { msg ->
            when (msg.role) {
                "user" -> messageList.add(
                    Message(role = msg.role, content = msg.content ?: "", reasoningContent = null)
                )
                "assistant" -> messageList.add(
                    Message(role = msg.role, content = msg.content ?: "",
                        reasoningContent = msg.reasoningContent, toolCalls = msg.toolCalls)
                )
                "tool" -> messageList.add(
                    Message(role = msg.role, content = msg.content ?: "",
                        toolCallId = msg.toolCallId, name = msg.name)
                )
            }
        }

        return messageList
    }

    /**
     * 更新消息内容
     * @param index 消息索引
     * @param content 内容
     * @param reasoningContent 思考内容
     */
    private fun updateMessageContent(index: Int, content: String, reasoningContent: String) {
        val updated = _messages.value.toMutableList()
        if (index < updated.size) {
            updated[index] = updated[index].copy(
                content = content.ifEmpty { "" },
                reasoningContent = reasoningContent
            )
            _messages.value = updated
        }
    }

    /**
     * 处理发送结果
     */
    private suspend fun handleSendResult(
        result: Result<ApiService.SendResult>,
        aiMsgIndex: Int,
        fullContent: String,
        fullReasoning: String
    ) {
        if (result.isSuccess) {
            val fullResult = result.getOrNull() ?: ApiService.SendResult()

            if (fullResult.toolCalls.isNotEmpty()) {
                // 处理工具调用
                handleToolCalls(aiMsgIndex, fullContent, fullReasoning, fullResult.toolCalls)
            } else {
                _isLoading.value = false
                autoSaveConversation()
            }
        } else {
            // 处理请求失败
            _isLoading.value = false
            val error = result.exceptionOrNull()?.message ?: "未知错误"
            val updated = _messages.value.toMutableList()
            if (aiMsgIndex < updated.size) {
                updated[aiMsgIndex] = updated[aiMsgIndex].copy(content = "请求失败: $error")
            }
            _messages.value = updated
        }
    }

    /**
     * 处理发送错误
     */
    private fun handleSendError(aiMsgIndex: Int, error: String) {
        _isLoading.value = false
        val updated = _messages.value.toMutableList()
        if (aiMsgIndex < updated.size) {
            updated[aiMsgIndex] = updated[aiMsgIndex].copy(content = "请求失败: $error")
        }
        _messages.value = updated
    }

    /**
     * 处理工具调用
     */
    private suspend fun handleToolCalls(
        aiMsgIndex: Int,
        content: String,
        reasoning: String,
        toolCalls: List<ToolCall>
    ) {
        // 更新 AI 消息，添加工具调用信息
        val updated = _messages.value.toMutableList()
        if (aiMsgIndex < updated.size) {
            updated[aiMsgIndex] = updated[aiMsgIndex].copy(
                content = content.takeIf { it.isNotEmpty() } ?: updated[aiMsgIndex].content,
                reasoningContent = reasoning,
                toolCalls = toolCalls
            )
            _messages.value = updated
        }

        // 执行工具调用
        val toolResults = mutableListOf<ChatMessage>()
        var toolExecutionFailed = false
        var hasValidResult = false
        var toolHintMessage = ""

        val userQuestion = _messages.value.lastOrNull { it.role == "user" }?.content ?: ""

        for (toolCall in toolCalls) {
            var arguments = toolCall.function.arguments

            // 参数回退处理：如果参数为空，从用户问题中提取查询词
            var usedFallback = false
            if (arguments.isBlank() || arguments == "{}") {
                val extractedQuery = extractSearchQuery(userQuestion)
                if (extractedQuery.isNotEmpty()) {
                    arguments = "{\"query\":\"$extractedQuery\"}"
                    usedFallback = true
                }
            }

            // 执行工具
            val result = executeToolWithFallback(toolCall.function.name, arguments, usedFallback)
            toolResults.add(result.message)

            if (result.failed) toolExecutionFailed = true
            if (result.hasValidResult) hasValidResult = true
            if (result.hintMessage.isNotEmpty()) toolHintMessage = result.hintMessage
        }

        _isLoading.value = false

        // 根据工具执行结果进行处理
        when {
            !toolExecutionFailed && hasValidResult -> {
                // 工具执行成功，继续对话
                val all = _messages.value.toMutableList()
                all.addAll(toolResults)
                _messages.value = all
                continueWithToolResults(toolResults, aiMsgIndex)
            }
            !toolExecutionFailed && !hasValidResult && toolHintMessage.isNotEmpty() -> {
                // 无有效结果但有提示消息
                updateMessageWithHint(aiMsgIndex, toolHintMessage)
                autoSaveConversation()
            }
            else -> {
                // 工具执行失败
                updateMessageWithError(aiMsgIndex)
                autoSaveConversation()
            }
        }
    }

    /**
     * 工具执行结果数据类
     * @param message 工具执行结果消息（用于添加到对话列表）
     * @param failed 工具执行是否失败
     * @param hasValidResult 是否有有效结果
     * @param hintMessage 提示消息（如"请提供搜索关键词"）
     */
    private data class ToolExecutionResult(
        val message: ChatMessage,
        val failed: Boolean,
        val hasValidResult: Boolean,
        val hintMessage: String
    )

    /**
     * 执行工具并处理回退逻辑
     * @param toolName 工具名称
     * @param arguments 工具参数（JSON格式）
     * @param usedFallback 是否使用了参数回退（从用户问题中提取）
     * @return 工具执行结果
     */
    private suspend fun executeToolWithFallback(
        toolName: String,
        arguments: String,
        usedFallback: Boolean
    ): ToolExecutionResult {
        // 调用 ToolManager 执行工具（suspend 函数）
        val result = ToolManager.executeTool(toolName, arguments)

        // 判断执行是否失败
        val failed = result.contains("失败") || result.contains("未知工具")
        // 判断是否有有效结果（排除需要用户提供更多信息的情况）
        val hasValidResult = !result.contains("请提供") && !failed
        // 提取提示消息（当需要用户提供信息时）
        val hintMessage = if (result.contains("请提供")) result else ""

        return ToolExecutionResult(
            message = ChatMessage(role = "tool", content = result,
                toolCallId = "", name = toolName),
            failed = failed,
            hasValidResult = hasValidResult,
            hintMessage = hintMessage
        )
    }

    /**
     * 更新消息内容（添加提示）
     */
    private fun updateMessageWithHint(index: Int, hint: String) {
        val all = _messages.value.toMutableList()
        if (index in all.indices) {
            val originalContent = all[index].content
            all[index] = all[index].copy(
                content = if (originalContent.isNotEmpty()) "$originalContent\n\n$hint" else hint,
                toolCalls = null
            )
            _messages.value = all
        }
    }

    /**
     * 更新消息内容（添加错误）
     */
    private fun updateMessageWithError(index: Int) {
        val all = _messages.value.toMutableList()
        if (index in all.indices) {
            val originalContent = all[index].content
            all[index] = all[index].copy(
                content = if (originalContent.isNotEmpty()) "$originalContent\n\n工具执行失败，请重试。" else "工具执行失败，请重试。",
                toolCalls = null
            )
            _messages.value = all
        }
    }

    /**
     * 使用工具执行结果继续对话
     */
    private fun continueWithToolResults(toolResults: List<ChatMessage>, aiMsgIndex: Int) {
        val config = _apiConfig.value
        viewModelScope.launch {
            try {
                val messageList = buildMessageList(config)

                var fullContent = ""
                var fullReasoning = ""

                val result = ApiService.sendMessageStream(
                    apiKey = config.apiKey,
                    model = config.model,
                    messages = messageList,
                    provider = config.provider,
                    customBaseUrl = config.customBaseUrl,
                    enableThinking = _enableThinking.value,
                    reasoningEffort = _reasoningEffort.value,
                    enableToolCalls = _enableToolCalls.value
                ) { chunk ->
                    if (chunk.content.isNotEmpty()) fullContent += chunk.content
                    if (chunk.reasoningContent.isNotEmpty()) fullReasoning += chunk.reasoningContent
                }

                if (result.isSuccess) {
                    val fullResult = result.getOrNull()
                    val updated = _messages.value.toMutableList()
                    if (aiMsgIndex < updated.size && fullResult != null) {
                        updated[aiMsgIndex] = updated[aiMsgIndex].copy(
                            content = fullResult.content.ifEmpty { updated[aiMsgIndex].content },
                            reasoningContent = fullReasoning
                        )
                        _messages.value = updated
                    }
                    autoSaveConversation()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "继续对话失败"
                    val updated = _messages.value.toMutableList()
                    if (aiMsgIndex < updated.size) {
                        updated[aiMsgIndex] = updated[aiMsgIndex].copy(content = error)
                    }
                    _messages.value = updated
                }
            } catch (e: Exception) {
                val updated = _messages.value.toMutableList()
                if (aiMsgIndex < updated.size) {
                    updated[aiMsgIndex] = updated[aiMsgIndex].copy(content = "继续对话失败: ${e.message}")
                }
                _messages.value = updated
            }
        }
    }

    /**
     * 从用户问题中提取搜索查询词
     * @param question 用户问题
     * @return 提取的查询词，如果没有找到则返回空字符串
     */
    private fun extractSearchQuery(question: String): String {
        // 尝试提取搜索相关的关键词
        val patterns = listOf(
            Regex("""搜索[(""']?([^("'']+)["'""']?"""),
            Regex("""查询[(""']?([^("'']+)["'""']?"""),
            Regex("""搜一下[(""']?([^("'']+)["'""']?"""),
            Regex("""找一下[(""']?([^("'']+)["'""']?"""),
            Regex("""了解一下[(""']?([^("'']+)["'""']?"""),
            Regex("""什么是([^，,。]+)"""),
            Regex("""([^，,。]+)是什么"""),
            Regex("""([^，,。]+)怎么样"""),
            Regex("""([^，,。]+)好吗""")
        )

        for (pattern in patterns) {
            val match = pattern.find(question)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // 如果没有匹配到特定模式，返回整个问题（去掉标点）
        return question.replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]"), " ").trim()
    }
}
