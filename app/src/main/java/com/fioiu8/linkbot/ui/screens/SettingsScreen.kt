package com.fioiu8.linkbot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.linkbot.model.ApiConfig
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页面 - 应用配置中心
 *
 * 职责：
 * - 管理 API 配置（提供商、模型、API Key、系统提示词）
 * - 管理主题设置（主题模式、动态取色、主题色、圆角、毛玻璃效果）
 * - 支持高级功能开关（深度思考模式、工具调用）
 *
 * 采用卡片式布局，将相关设置分组展示，提升用户体验
 */
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    // ==================== 状态收集 ====================
    // API 配置状态
    val currentConfig by viewModel.apiConfig.collectAsState()
    
    // 主题设置状态
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val useMonet by settingsViewModel.useMonet.collectAsState()
    val colorSpec by settingsViewModel.colorSpec.collectAsState()
    val paletteStyle by settingsViewModel.paletteStyle.collectAsState()
    val smoothRounding by settingsViewModel.smoothRounding.collectAsState()
    val blurEnabled by settingsViewModel.blurEnabled.collectAsState()
    val customPrimaryColor by settingsViewModel.customPrimaryColor.collectAsState()
    val useCustomTheme by settingsViewModel.useCustomTheme.collectAsState()
    
    // 高级功能状态
    val enableThinking by viewModel.enableThinking.collectAsState()
    val enableThinkingExpanded by viewModel.enableThinkingExpanded.collectAsState()
    val enableToolCalls by viewModel.enableToolCalls.collectAsState()

    // ==================== 本地状态 ====================
    // API 配置本地编辑状态
    var provider by remember { mutableStateOf(currentConfig.provider) }
    var customBaseUrl by remember { mutableStateOf(currentConfig.customBaseUrl) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var systemPrompt by remember { mutableStateOf(currentConfig.systemPrompt) }
    
    // 自定义模型状态
    var customModel by remember { mutableStateOf("") }
    var useCustomModel by remember { mutableStateOf(false) }
    
    // 颜色选择对话框状态
    var showThemeColorDialog by remember { mutableStateOf(false) }

    // ==================== 常量定义 ====================
    // 可用的 API 提供商列表
    val providers = listOf("deepseek", "openai", "custom")

    // DeepSeek 预设模型列表
    val presetModels = listOf(
        PresetModel("deepseek-v4-flash", "DeepSeek V4 Flash", "快速响应，适合日常对话"),
        PresetModel("deepseek-v4-pro", "DeepSeek V4 Pro", "高性能，适合复杂任务"),
        PresetModel("deepseek-chat", "DeepSeek Chat", "经典版本 (将于 2026/07/24 弃用)"),
        PresetModel("deepseek-reasoner", "DeepSeek Reasoner", "推理增强 (将于 2026/07/24 弃用)")
    )

    // ==================== 副作用处理 ====================
    // 监听配置变化，自动同步自定义模型状态
    LaunchedEffect(currentConfig.model) {
        val currentModel = currentConfig.model
        if (presetModels.none { it.id == currentModel }) {
            useCustomModel = true
            customModel = currentModel
        } else {
            useCustomModel = false
            model = currentModel
        }
    }

    // ==================== 主布局 ====================
    Scaffold(
        topBar = {
            SmallTopAppBar(title = "设置")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API 配置卡片
            ApiConfigSection(
                provider = provider,
                onProviderChange = { provider = it },
                customBaseUrl = customBaseUrl,
                onCustomBaseUrlChange = { customBaseUrl = it },
                model = model,
                onModelChange = { model = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                systemPrompt = systemPrompt,
                onSystemPromptChange = { systemPrompt = it },
                customModel = customModel,
                onCustomModelChange = { customModel = it },
                useCustomModel = useCustomModel,
                onUseCustomModelChange = { useCustomModel = it },
                enableThinking = enableThinking,
                onEnableThinkingChange = { viewModel.setEnableThinking(it) },
                enableThinkingExpanded = enableThinkingExpanded,
                onEnableThinkingExpandedChange = { viewModel.setEnableThinkingExpanded(it) },
                enableToolCalls = enableToolCalls,
                onEnableToolCallsChange = { viewModel.setEnableToolCalls(it) },
                onSaveClick = {
                    val finalModel = if (useCustomModel) customModel else model
                    viewModel.updateApiConfig(
                        ApiConfig(
                            provider = provider,
                            customBaseUrl = customBaseUrl,
                            model = finalModel,
                            apiKey = apiKey,
                            systemPrompt = systemPrompt
                        )
                    )
                }
            )

            // 主题设置卡片
            ThemeSettingsSection(
                themeMode = themeMode,
                onThemeModeChange = { settingsViewModel.setThemeMode(it) },
                useMonet = useMonet,
                onUseMonetChange = { settingsViewModel.setUseMonet(it) },
                colorSpec = colorSpec,
                onColorSpecChange = { settingsViewModel.setColorSpec(it) },
                paletteStyle = paletteStyle,
                onPaletteStyleChange = { settingsViewModel.setPaletteStyle(it) },
                customPrimaryColor = customPrimaryColor,
                smoothRounding = smoothRounding,
                onSmoothRoundingChange = { settingsViewModel.setSmoothRounding(it) },
                blurEnabled = blurEnabled,
                onBlurEnabledChange = { settingsViewModel.setBlurEnabled(it) },
                onThemeColorClick = { showThemeColorDialog = true }
            )

            // 底部留白
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ==================== 主题色选择对话框 ====================
    ThemeColorPickerDialog(
        visible = showThemeColorDialog,
        onDismiss = { showThemeColorDialog = false },
        selectedColor = customPrimaryColor,
        onColorSelected = {
            settingsViewModel.setCustomPrimaryColor(it)
            showThemeColorDialog = false
        }
    )
}

// ==================== 组合函数 ====================

/**
 * API 配置区域 - 管理 API 相关设置
 */
@Composable
private fun ApiConfigSection(
    provider: String,
    onProviderChange: (String) -> Unit,
    customBaseUrl: String,
    onCustomBaseUrlChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    customModel: String,
    onCustomModelChange: (String) -> Unit,
    useCustomModel: Boolean,
    onUseCustomModelChange: (Boolean) -> Unit,
    enableThinking: Boolean,
    onEnableThinkingChange: (Boolean) -> Unit,
    enableThinkingExpanded: Boolean,
    onEnableThinkingExpandedChange: (Boolean) -> Unit,
    enableToolCalls: Boolean,
    onEnableToolCallsChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit
) {
    // DeepSeek 预设模型列表
    val presetModels = listOf(
        PresetModel("deepseek-v4-flash", "DeepSeek V4 Flash", "快速响应，适合日常对话"),
        PresetModel("deepseek-v4-pro", "DeepSeek V4 Pro", "高性能，适合复杂任务"),
        PresetModel("deepseek-chat", "DeepSeek Chat", "经典版本 (将于 2026/07/24 弃用)"),
        PresetModel("deepseek-reasoner", "DeepSeek Reasoner", "推理增强 (将于 2026/07/24 弃用)")
    )

    Card(cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 标题
            Text(text = "API 配置", style = MiuixTheme.textStyles.title3)

            // 提供商选择
            Text(text = "选择 API 提供商", fontSize = 14.sp)
            ProviderSelector(
                providers = listOf("deepseek", "openai", "custom"),
                selected = provider,
                onSelected = onProviderChange
            )

            // 自定义 Base URL（仅自定义模式显示）
            AnimatedVisibility(
                visible = provider == "custom",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextField(
                    value = customBaseUrl,
                    onValueChange = onCustomBaseUrlChange,
                    label = "Base URL",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // DeepSeek 模型选择
            if (provider == "deepseek") {
                ModelSelector(
                    presetModels = presetModels,
                    model = model,
                    onModelChange = onModelChange,
                    customModel = customModel,
                    onCustomModelChange = onCustomModelChange,
                    useCustomModel = useCustomModel,
                    onUseCustomModelChange = onUseCustomModelChange
                )

                // 高级功能开关
                Text(text = "高级功能", style = MiuixTheme.textStyles.title3)

                SwitchPreference(
                    title = "深度思考模式",
                    summary = if (enableThinking) "启用后 AI 会展示详细思考过程，提升回答质量" else "标准回复模式，响应更快",
                    checked = enableThinking,
                    onCheckedChange = onEnableThinkingChange
                )

                AnimatedVisibility(
                    visible = enableThinking,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SwitchPreference(
                            title = "默认展开思考过程",
                            summary = if (enableThinkingExpanded) "自动展开 AI 的思考过程，方便查看推理逻辑" else "需要手动点击展开思考过程",
                            checked = enableThinkingExpanded,
                            onCheckedChange = onEnableThinkingExpandedChange
                        )
                    }
                }

                SwitchPreference(
                    title = "联网搜索 (Tool Calls)",
                    summary = if (enableToolCalls) "允许 AI 联网搜索实时信息，获取最新数据" else "仅基于训练数据回复，离线可用",
                    checked = enableToolCalls,
                    onCheckedChange = onEnableToolCallsChange
                )

                AnimatedVisibility(
                    visible = enableToolCalls,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    ToolCallInfoCard()
                }
            } else {
                // 非 DeepSeek 提供商的模型输入
                TextField(
                    value = model,
                    onValueChange = onModelChange,
                    label = "模型",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 分隔线
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // API Key 输入
            TextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = "API Key",
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 系统提示词输入
            TextField(
                value = systemPrompt,
                onValueChange = onSystemPromptChange,
                label = "系统提示词",
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
            )

            // 保存按钮
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存 API 配置")
            }
        }
    }
}

/**
 * 提供商选择器
 */
@Composable
private fun ProviderSelector(
    providers: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        providers.forEach { p ->
            Button(
                onClick = {
                    onSelected(p)
                    // 切换提供商时重置默认模型
                    when (p) {
                        "deepseek" -> onSelected(p)
                        "openai" -> {}
                        // custom 保持原有模型
                    }
                },
                enabled = selected != p,
                minHeight = 36.dp,
                cornerRadius = 18.dp
            ) {
                Text(
                    text = when (p) {
                        "deepseek" -> "DeepSeek"
                        "openai" -> "OpenAI"
                        else -> "自定义"
                    },
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 模型选择器（DeepSeek 专用）
 */
@Composable
private fun ModelSelector(
    presetModels: List<PresetModel>,
    model: String,
    onModelChange: (String) -> Unit,
    customModel: String,
    onCustomModelChange: (String) -> Unit,
    useCustomModel: Boolean,
    onUseCustomModelChange: (Boolean) -> Unit
) {
    Text(text = "选择模型", fontSize = 14.sp)

    // 预设模型列表
    presetModels.forEach { presetModel ->
        ModelItem(
            model = presetModel,
            isSelected = !useCustomModel && model == presetModel.id,
            onClick = {
                onUseCustomModelChange(false)
                onModelChange(presetModel.id)
            }
        )
    }

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

    // 自定义模型选项
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onUseCustomModelChange(true) },
        shape = RoundedCornerShape(12.dp),
        color = if (useCustomModel) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MiuixTheme.colorScheme.surface,
        border = if (useCustomModel) BorderStroke(1.dp, MiuixTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                MiuixIcons.UploadCloud,
                contentDescription = null,
                tint = if (useCustomModel) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "自定义模型",
                    fontSize = 14.sp,
                    fontWeight = if (useCustomModel) FontWeight.Bold else FontWeight.Normal,
                    color = if (useCustomModel) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                )
                Text(
                    if (useCustomModel && customModel.isNotEmpty()) "当前: $customModel" else "输入自定义模型名称",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }

    // 自定义模型输入框
    AnimatedVisibility(
        visible = useCustomModel,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        TextField(
            value = customModel,
            onValueChange = {
                onCustomModelChange(it)
                onModelChange(it)
            },
            label = "自定义模型名称",
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 单个模型项
 */
@Composable
private fun ModelItem(
    model: PresetModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MiuixTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.dp, MiuixTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) MiuixIcons.Reply else MiuixIcons.ReplyAll,
                contentDescription = null,
                tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    model.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                )
                Text(model.description, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

/**
 * 工具调用信息卡片
 */
@Composable
private fun ToolCallInfoCard() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = MiuixIcons.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "支持搜索、天气、计算器、时间查询等功能",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 主题设置区域
 */
@Composable
private fun ThemeSettingsSection(
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    useMonet: Boolean,
    onUseMonetChange: (Boolean) -> Unit,
    colorSpec: Int,
    onColorSpecChange: (Int) -> Unit,
    paletteStyle: Int,
    onPaletteStyleChange: (Int) -> Unit,
    customPrimaryColor: Color,
    smoothRounding: Boolean,
    onSmoothRoundingChange: (Boolean) -> Unit,
    blurEnabled: Boolean,
    onBlurEnabledChange: (Boolean) -> Unit,
    onThemeColorClick: () -> Unit
) {
    Card(cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "主题设置", style = MiuixTheme.textStyles.title3)
            Spacer(modifier = Modifier.height(12.dp))

            // 主题模式
            WindowDropdownPreference(
                title = "主题模式",
                items = listOf("跟随系统", "浅色", "深色"),
                selectedIndex = themeMode,
                onSelectedIndexChange = onThemeModeChange
            )

            // 动态取色开关
            SwitchPreference(
                title = "动态取色 (Monet)",
                summary = if (useMonet) "使用系统壁纸颜色" else "使用默认主题颜色",
                checked = useMonet,
                onCheckedChange = onUseMonetChange
            )

            // Monet 设置（启用时显示）
            AnimatedVisibility(
                visible = useMonet,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    WindowDropdownPreference(
                        title = "颜色规范",
                        items = listOf("Spec 2021", "Spec 2025"),
                        selectedIndex = colorSpec,
                        onSelectedIndexChange = onColorSpecChange
                    )
                    WindowDropdownPreference(
                        title = "调色板风格",
                        items = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive"),
                        selectedIndex = paletteStyle,
                        onSelectedIndexChange = onPaletteStyleChange
                    )
                }
            }

            // 自定义主题色（禁用 Monet 时显示）
            AnimatedVisibility(
                visible = !useMonet,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeColorSelector(customPrimaryColor, onThemeColorClick)
                }
            }

            // 平滑圆角开关
            SwitchPreference(
                title = "平滑圆角",
                summary = if (smoothRounding) "使用 G2 连续圆角" else "使用标准圆弧",
                checked = smoothRounding,
                onCheckedChange = onSmoothRoundingChange
            )

            // 毛玻璃效果开关
            SwitchPreference(
                title = "毛玻璃效果",
                summary = if (blurEnabled) "启用背景模糊效果" else "禁用背景模糊效果",
                checked = blurEnabled,
                onCheckedChange = onBlurEnabledChange
            )
        }
    }
}

/**
 * 主题色选择器
 */
@Composable
private fun ThemeColorSelector(
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = selectedColor,
                    border = BorderStroke(1.dp, MiuixTheme.colorScheme.outline)
                ) {}
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("选择主题色", fontSize = 15.sp)
                    Text("点击选择您喜欢的颜色", fontSize = 13.sp)
                }
            }
            Icon(MiuixIcons.Forward, null)
        }
    }
}

/**
 * 主题色选择对话框
 */
@Composable
private fun ThemeColorPickerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    OverlayDialog(
        title = "选择主题色",
        show = visible,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 预设颜色列表
            val presetColors = listOf(
                "默认蓝" to Color(0xFF2196F3),
                "珊瑚红" to Color(0xFFFF6B6B),
                "薄荷绿" to Color(0xFF4ECDC4),
                "阳光橙" to Color(0xFFFFA726),
                "优雅紫" to Color(0xFFAB47BC),
                "深海蓝" to Color(0xFF1565C0),
                "玫瑰金" to Color(0xFFE91E63),
                "森林绿" to Color(0xFF2E7D32),
                "暗夜灰" to Color(0xFF607D8B),
                "纯黑" to Color(0xFF212121)
            )

            // 按 5 列网格显示
            presetColors.chunked(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (name, color) ->
                        ColorOption(
                            name = name,
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { onColorSelected(color) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 填充空白列
                    repeat(5 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 单个颜色选项
 */
@Composable
private fun ColorOption(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick)
                .then(
                    if (isSelected) Modifier.border(3.dp, MiuixTheme.colorScheme.primary, CircleShape)
                    else Modifier.border(1.dp, MiuixTheme.colorScheme.outline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(MiuixIcons.Refresh, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

// ==================== 数据类 ====================

/**
 * 预设模型数据类
 * @param id 模型标识符
 * @param displayName 显示名称
 * @param description 描述信息
 */
private data class PresetModel(
    val id: String,
    val displayName: String,
    val description: String
)