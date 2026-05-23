package com.fioiu8.linkbot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.linkbot.model.ApiConfig
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfigScreen(viewModel: ChatViewModel) {
    val apiConfig by viewModel.apiConfig.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf(apiConfig.provider) }
    var customBaseUrl by remember { mutableStateOf(apiConfig.customBaseUrl) }
    var model by remember { mutableStateOf(apiConfig.model) }
    var apiKey by remember { mutableStateOf(apiConfig.apiKey) }
    var systemPrompt by remember { mutableStateOf(apiConfig.systemPrompt) }

    val messages by viewModel.messages.collectAsState()
    val messageCount = messages.size
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "API 配置",
                navigationIcon = {},
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.updateApiConfig(ApiConfig(provider = provider, customBaseUrl = customBaseUrl, model = model, apiKey = apiKey, systemPrompt = systemPrompt))
                            isEditing = false
                        }, minWidth = 40.dp, minHeight = 40.dp, cornerRadius = 20.dp) {
                            Icon(imageVector = MiuixIcons.SearchDevice, contentDescription = "保存", tint = MiuixTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }, minWidth = 40.dp, minHeight = 40.dp, cornerRadius = 20.dp) {
                            Icon(imageVector = MiuixIcons.Edit, contentDescription = "编辑", tint = MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(300))) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MiuixTheme.colorScheme.surfaceContainer) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("API 提供商", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))

                        if (!isEditing) {
                            Text(when (provider) { "deepseek" -> "DeepSeek"; "openai" -> "OpenAI"; else -> "自定义" }, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
                        } else {
                            val providers = listOf("deepseek" to "DeepSeek", "openai" to "OpenAI", "custom" to "自定义")
                            providers.forEach { (key, label) ->
                                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { provider = key }, shape = RoundedCornerShape(12.dp), color = if (provider == key) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MiuixTheme.colorScheme.surface, border = if (provider == key) BorderStroke(1.dp, MiuixTheme.colorScheme.primary) else null) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (provider == key) MiuixIcons.Reply else MiuixIcons.ReplyAll, contentDescription = null, tint = if (provider == key) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, fontSize = 14.sp, color = if (provider == key) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = provider == "custom", enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MiuixTheme.colorScheme.surfaceContainer) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("自定义 API", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        if (isEditing) {
                            TextField(value = customBaseUrl, onValueChange = { customBaseUrl = it }, label = "Base URL", useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(customBaseUrl.ifEmpty { "未设置" }, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MiuixTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("模型设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    if (isEditing) {
                        TextField(value = model, onValueChange = { model = it }, label = "模型名称", useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(model.ifEmpty { "未设置" }, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MiuixTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Key", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    if (isEditing) {
                        TextField(value = apiKey, onValueChange = { apiKey = it }, label = "API Key", useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                    } else {
                        Text(if (apiKey.isNotEmpty()) "****${apiKey.takeLast(8)}" else "未设置", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MiuixTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("系统提示词", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    if (isEditing) {
                        TextField(value = systemPrompt, onValueChange = { systemPrompt = it }, label = "系统提示词", useLabelAsPlaceholder = true, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 5)
                    } else {
                        Text(systemPrompt.ifEmpty { "未设置" }, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}