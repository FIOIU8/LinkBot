package com.fioiu8.linkbot.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.fioiu8.linkbot.model.ChatMessage
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
     * 聊天界面 - 应用的核心交互界面
     *
     * 职责：
     * - 展示聊天消息列表
     * - 处理用户输入和消息发送
     * - 管理头像设置
     * - 支持消息操作（复制、编辑、重发、保存笔记）
     * - 管理对话历史和上下文
     *
     * @param viewModel ChatViewModel 实例，管理聊天状态
     * @param settingsViewModel SettingsViewModel 实例，管理应用设置
     */
    @Composable
    fun ChatScreen(
        viewModel: ChatViewModel,
        settingsViewModel: SettingsViewModel
    ) {
        // 从 ViewModel 收集状态
        val messages by viewModel.messages.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val contextCount by viewModel.contextCount.collectAsState()
        val userAvatarPath by viewModel.userAvatarPath.collectAsState()
        val aiAvatarPath by viewModel.aiAvatarPath.collectAsState()
        val expandedThinkingSet by viewModel.expandedThinking.collectAsState()
        val defaultExpandThinking by viewModel.defaultExpandThinking.collectAsState()

        // 本地状态管理
        var inputText by remember { mutableStateOf("") }           // 用户输入文本
        var showContextDialog by remember { mutableStateOf(false) } // 上下文对话框显示状态
        var showAvatarDialog by remember { mutableStateOf(false) }  // 头像设置对话框显示状态
        var avatarType by remember { mutableStateOf("user") }       // 当前设置的头像类型
        var editingIndex by remember { mutableStateOf<Int?>(null) } // 当前编辑的消息索引
        val listState = rememberLazyListState()                     // 消息列表滚动状态

        var showConversationDialog by remember { mutableStateOf(false) } // 对话历史对话框
        val conversations by viewModel.conversations.collectAsState()    // 对话历史列表

        // 获取系统服务和上下文
        val context = LocalContext.current
        val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

        // Snackbar 和协程作用域
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // 图片选择器 - 用于选择头像
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                if (avatarType == "user") viewModel.setUserAvatar(it)
                else viewModel.setAiAvatar(it)
            }
        }

        // 监听 Snackbar 事件
        LaunchedEffect(Unit) {
            viewModel.snackbarEvent.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "LinkBot 对话",
                navigationIcon = {},
                actions = {
                    IconButton(
                        onClick = { showConversationDialog = true },
                        minWidth = 40.dp,
                        minHeight = 40.dp,
                        cornerRadius = 20.dp
                    ) {
                        Icon(
                            imageVector = MiuixIcons.ListView,
                            contentDescription = "对话列表",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 96.dp)
        ) {
            if (messages.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = MiuixIcons.Messages,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("开始对话吧", fontSize = 16.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    if (messages.isNotEmpty()) {
                        item {
                            Surface(
                                color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("对话中 · ${messages.size} 条消息", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        Text("上下文: ${if (contextCount == 0) "全部历史 (不限制)" else "最近 ${contextCount} 条消息"}", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { showAvatarDialog = true }, minWidth = 30.dp, minHeight = 30.dp, cornerRadius = 15.dp) {
                                            Icon(MiuixIcons.Contacts, "头像", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { showContextDialog = true }, minWidth = 30.dp, minHeight = 30.dp, cornerRadius = 15.dp) {
                                            Icon(MiuixIcons.Tune, "上下文", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { viewModel.clearMessages() }, minWidth = 30.dp, minHeight = 30.dp, cornerRadius = 15.dp) {
                                            Icon(MiuixIcons.Delete, "清空", tint = MiuixTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(messages.filter { it.role == "user" || it.role == "assistant" }) { displayIndex, message ->
                        val originalIndex = messages.indexOfFirst { it.timestamp == message.timestamp && it.role == message.role }
                        MessageBubble(
                            message = message,
                            index = originalIndex,
                            userAvatarPath = userAvatarPath,
                            aiAvatarPath = aiAvatarPath,
                            onSaveNote = { idx -> viewModel.saveNote(idx) },
                            onCopy = { text ->
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))
                                scope.launch { snackbarHostState.showSnackbar("已复制") }
                            },
                            onResend = { idx -> viewModel.resendMessage(idx) },
                            onEdit = { idx ->
                                viewModel.startEdit(idx)?.let { content ->
                                    inputText = content
                                    editingIndex = idx
                                }
                            },
                            expandedThinkingSet = expandedThinkingSet,
                            defaultExpandThinking = defaultExpandThinking,
                            onToggleThinking = viewModel::toggleThinking
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp
            ) {
                InputBar(
                    inputText = inputText,
                    isLoading = isLoading,
                    onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val idx = editingIndex
                            if (idx != null) {
                                viewModel.replaceAndResend(idx, inputText)
                                editingIndex = null
                            } else {
                                viewModel.sendMessage(inputText)
                            }
                            inputText = ""
                        }
                    },
                    isEditing = editingIndex != null
                )
            }
        }

        val balance by viewModel.balance.collectAsState()

        AnimatedVisibility(
            visible = showConversationDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300))
        ) {
            ConversationListDialog(
                conversations = conversations,
                currentMessages = messages,
                onLoad = { id -> viewModel.loadConversation(id); showConversationDialog = false },
                onDelete = { id -> viewModel.deleteConversation(id) },
                onSave = { viewModel.saveCurrentConversation(); viewModel.refreshConversations() },
                onNew = { viewModel.clearMessages(); showConversationDialog = false },
                onDismiss = { showConversationDialog = false }
            )
        }

        AnimatedVisibility(
            visible = showAvatarDialog,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 10 }, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { it / 10 }, animationSpec = tween(300))
        ) {
            OverlayDialog(
                title = "头像设置",
                show = showAvatarDialog,
                onDismissRequest = { showAvatarDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarImage(path = userAvatarPath, size = 48, label = "你")
                            Spacer(Modifier.width(12.dp))
                            Text("用户头像", fontSize = 16.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { avatarType = "user"; imagePickerLauncher.launch("image/*") }, minHeight = 32.dp, cornerRadius = 16.dp) { Text("选择", fontSize = 13.sp) }
                            if (userAvatarPath != null) {
                                Button(onClick = { viewModel.removeUserAvatar() }, minHeight = 32.dp, cornerRadius = 16.dp) { Text("删除", fontSize = 13.sp) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarImage(path = aiAvatarPath, size = 48, label = "AI")
                            Spacer(Modifier.width(12.dp))
                            Text("AI 头像", fontSize = 16.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { avatarType = "ai"; imagePickerLauncher.launch("image/*") }, minHeight = 32.dp, cornerRadius = 16.dp) { Text("选择", fontSize = 13.sp) }
                            if (aiAvatarPath != null) {
                                Button(onClick = { viewModel.removeAiAvatar() }, minHeight = 32.dp, cornerRadius = 16.dp) { Text("删除", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showContextDialog,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 10 }, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { it / 10 }, animationSpec = tween(300))
        ) {
            OverlayDialog(
                title = "上下文设置",
                show = showContextDialog,
                onDismissRequest = { showContextDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择每次发送给 AI 的历史消息数量", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    Text("消息越多，AI 越了解上下文，但消耗更多 Token。", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    Spacer(Modifier.height(4.dp))
                    WindowSpinnerPreference(
                        title = "上下文长度",
                        summary = "当前: ${if (contextCount == 0) "不限制" else "${contextCount} 条"}",
                        items = listOf(
                            SpinnerEntry(title = "不限制 (全部历史)", summary = "最佳连贯性"),
                            SpinnerEntry(title = "最近 5 条", summary = "简短对话"),
                            SpinnerEntry(title = "最近 10 条 (推荐)", summary = "平衡"),
                            SpinnerEntry(title = "最近 20 条", summary = "更多记忆"),
                            SpinnerEntry(title = "最近 50 条", summary = "长对话"),
                            SpinnerEntry(title = "仅当前消息", summary = "无上下文")
                        ),
                        selectedIndex = when (contextCount) { 0 -> 0; 5 -> 1; 10 -> 2; 20 -> 3; 50 -> 4; 1 -> 5; else -> 2 },
                        onSelectedIndexChange = { idx -> viewModel.setContextCount(when (idx) { 0 -> 0; 1 -> 5; 2 -> 10; 3 -> 20; 4 -> 50; 5 -> 1; else -> 10 }) }
                    )
                    if (balance != null && balance!!.is_available) {
                        balance!!.balance_infos.firstOrNull()?.let { info ->
                            Text(
                                "余额: ${info.total_balance} ${info.currency}",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
        ) {
            OverlayDialog(
                title = "错误",
                show = errorMessage != null,
                onDismissRequest = { viewModel.clearError() }
            ) {
                Text(errorMessage ?: "")
            }
        }
    }
}

/**
     * 头像图片组件
     * @param path 头像文件路径，如果为空或文件不存在则显示默认头像
     * @param size 头像大小（dp）
     * @param label 显示的文字标签（当没有图片时显示）
     */
    @Composable
    fun AvatarImage(path: String?, size: Int, label: String) {
        if (path != null && File(path).exists()) {
            // 显示自定义头像
            Image(
                painter = rememberAsyncImagePainter(path),
                contentDescription = label,
                modifier = Modifier.size(size.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // 显示默认头像（带字母的圆形背景）
            Box(
                modifier = Modifier.size(size.dp).clip(CircleShape)
                    .background(MiuixTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, 
                    fontSize = (size / 3).sp, 
                    color = MiuixTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    /**
     * 消息气泡组件
     * @param message 聊天消息数据
     * @param index 消息在列表中的索引
     * @param userAvatarPath 用户头像路径
     * @param aiAvatarPath AI 头像路径
     * @param onSaveNote 保存笔记回调
     * @param onCopy 复制消息回调
     * @param onResend 重发消息回调
     * @param onEdit 编辑消息回调
     * @param expandedThinkingSet 已展开的思考过程集合
     * @param defaultExpandThinking 是否默认展开思考过程
     * @param onToggleThinking 切换思考过程展开状态的回调
     */
    @Composable
    fun MessageBubble(
        message: ChatMessage, index: Int,
        userAvatarPath: String?, aiAvatarPath: String?,
        onSaveNote: (Int) -> Unit, onCopy: (String) -> Unit,
        onResend: (Int) -> Unit, onEdit: (Int) -> Unit,
        expandedThinkingSet: Set<Int>, defaultExpandThinking: Boolean,
        onToggleThinking: (Int) -> Unit
    ) {
        // 判断是否为用户消息
        val isUser = message.role == "user"

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            AvatarImage(path = aiAvatarPath, size = 36, label = "AI")
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                if (isUser) "我" else "AI",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            if (!isUser && message.toolCalls != null && message.toolCalls!!.isNotEmpty()) {
                val toolNames = message.toolCalls?.map { it.function.name } ?: emptyList()
                val label = when {
                    toolNames.any { it in listOf("web_search", "search") } -> "🔍 正在联网搜索..."
                    toolNames.any { it == "get_weather" } -> "☁️ 正在查询天气..."
                    toolNames.any { it == "get_current_time" } -> "🕐 正在获取时间..."
                    toolNames.any { it == "calculator" } -> "🧮 正在计算..."
                    toolNames.any { it == "get_random_number" } -> "🎲 正在生成随机数..."
                    else -> "⚙️ 正在执行操作..."
                }

                Surface(
                    modifier = Modifier.widthIn(max = 300.dp).padding(bottom = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }
            }

            val hasReasoning = !message.reasoningContent.isNullOrBlank()

            if (!isUser && hasReasoning) {
                val isExpanded = expandedThinkingSet.contains(index) || defaultExpandThinking

                Surface(
                    modifier = Modifier.widthIn(max = 300.dp).clickable { onToggleThinking(index) },
                    shape = RoundedCornerShape(12.dp),
                    color = MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("💭 思考过程", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (isExpanded) "收起 ▲" else "展开 ▼", fontSize = 11.sp)
                        }
                        if (isExpanded) {
                            Spacer(Modifier.height(4.dp))
                            Text(text = message.reasoningContent ?: "", fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }

            if (!isUser && hasReasoning && !message.content.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MiuixTheme.colorScheme.dividerLine)
            }

            if (!message.content.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.widthIn(max = 300.dp),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(12.dp)
                ) {
                    MarkdownText(content = message.content, contentAlignment = if (isUser) Alignment.End else Alignment.Start)
                }
            }

            if (!isUser) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(onClick = { onCopy(message.content ?: "") }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                        Icon(MiuixIcons.Copy, "复制", modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    IconButton(onClick = { onSaveNote(index) }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                        Icon(MiuixIcons.Notes, "笔记", modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    IconButton(onClick = { onResend(index) }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                        Icon(MiuixIcons.Send, "重发", modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(onClick = { onCopy(message.content ?: "") }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                        Icon(MiuixIcons.Copy, "复制", modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    IconButton(onClick = { onEdit(index) }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                        Icon(MiuixIcons.Edit, "编辑", modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            AvatarImage(path = userAvatarPath, size = 36, label = "我")
        }
    }
}

@Composable
fun InputBar(
    inputText: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            IconButton(onClick = onSend, modifier = Modifier.size(40.dp)) {
                Icon(MiuixIcons.Close, "取消编辑", tint = MiuixTheme.colorScheme.error)
            }
        }

        TextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            label = "输入消息...",
            useLabelAsPlaceholder = true,
            enabled = !isLoading,
            trailingIcon = null
        )
        
        Spacer(Modifier.width(8.dp))
        
        IconButton(onClick = onSend, enabled = inputText.isNotBlank() && !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(MiuixIcons.Send, "发送", tint = if (inputText.isNotBlank()) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}