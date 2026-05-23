package com.fioiu8.linkbot.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.*
import java.text.SimpleDateFormat
import java.util.*

/**
     * 关于页面 - 展示应用信息和系统设置入口
     *
     * 职责：
     * - 显示应用版本信息
     * - 显示构建信息
     * - 提供系统设置入口
     * - 支持应用介绍和版权信息展示
     *
     * @param settingsViewModel SettingsViewModel 实例
     */
    @Composable
    fun AboutScreen(settingsViewModel: SettingsViewModel) {
        // 获取毛玻璃效果开关状态
        val blurEnabled by settingsViewModel.blurEnabled.collectAsState()
        val context = LocalContext.current

        // 获取应用信息（版本号、构建日期等）
        val appInfo = remember {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionName = packageInfo.versionName ?: "未知"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }

                val installTime = packageInfo.firstInstallTime
                val updateTime = packageInfo.lastUpdateTime
                val dateFormat = SimpleDateFormat("yyyy.M.d", Locale.getDefault())
                val buildNumber = String.format("%03d", versionCode)
                val buildDate = dateFormat.format(Date(updateTime))

                AppInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    buildDate = buildDate,
                    buildNumber = buildNumber,
                    installTime = installTime,
                    updateTime = updateTime
            )
        } catch (e: PackageManager.NameNotFoundException) {
            AppInfo(
                versionName = "未知",
                versionCode = 0,
                buildDate = "未知",
                buildNumber = "000",
                installTime = 0L,
                updateTime = 0L
            )
        }
    }

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = "关于")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .overScrollVertical()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) +
                        scaleIn(initialScale = 0.9f, animationSpec = tween(400))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MiuixTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MiuixIcons.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MiuixTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "LinkBot",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(300, delayMillis = 100)
                        )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "版本信息",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))

                            VersionInfoRow("应用版本", appInfo.versionName)
                            VersionInfoRow("版本代号", "${appInfo.versionCode}")
                            VersionInfoRow("构建版本", "${appInfo.buildDate}.${appInfo.buildNumber}")
                            VersionInfoRow(
                                "最低支持",
                                "Android ${getAndroidVersionName(Build.VERSION_CODES.S)} (API ${Build.VERSION_CODES.S})"
                            )
                            VersionInfoRow(
                                "目标版本",
                                "Android 16（API 级别 36）"
                            )
                        }
                    }
                }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(300, delayMillis = 200)
                        )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "技术信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        VersionInfoRow("UI 框架", "Miuix UI")
                        VersionInfoRow("开发语言", "Kotlin Multiplatform")
                        VersionInfoRow("架构", "MVVM + Compose")
                        VersionInfoRow("AI 接口", "DeepSeek / OpenAI / 自定义")
                        VersionInfoRow("数据存储", "本地文本存储")
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 300)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(300, delayMillis = 300)
                        )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "制作信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        VersionInfoRow("开发者", "FIOIU8")
                        VersionInfoRow("设计指导", "compose-miuix-ui")
                        VersionInfoRow("特别鸣谢", "开源社区贡献者")
                        VersionInfoRow("项目地址", "https://github.com/FIOIU8/LinkBot")
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(300, delayMillis = 400)
                        )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SwitchPreference(
                            title = "AndroidLiquidGlass",
                            summary = if (blurEnabled) "液态玻璃效果已启用，可实现惊人的视觉效果" else "使用普通导航栏样式",
                            checked = blurEnabled,
                            onCheckedChange = { settingsViewModel.setBlurEnabled(it) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 500)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(300, delayMillis = 500)
                        )
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClearDialog = true },
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "清除所有数据",
                                            fontSize = 16.sp,
                                            color = MiuixTheme.colorScheme.error
                                        )
                                        Text(
                                            "清除聊天记录、笔记和设置",
                                            fontSize = 13.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                    Icon(
                                        imageVector = MiuixIcons.Delete,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MiuixTheme.colorScheme.dividerLine
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* TODO: 重置设置 */ },
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "重置所有设置",
                                            fontSize = 16.sp,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "恢复默认设置，保留对话数据",
                                            fontSize = 13.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                    Icon(
                                        imageVector = MiuixIcons.Refresh,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "© 2026 FIOIU8. All rights reserved.",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Built with ❤️ using Kotlin & Compose Multiplatform",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    AnimatedVisibility(
        visible = showClearDialog,
        enter = fadeIn(animationSpec = tween(300)) +
                    scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(300))
        ) {
            OverlayDialog(
                title = "清除所有数据",
                show = showClearDialog,
                onDismissRequest = { showClearDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "确定要清除所有数据吗？",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "这将删除所有聊天记录、笔记和配置。此操作不可撤销。",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { showClearDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            text = "确认清除",
                            onClick = {
                                showClearDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }
        }
    }
}

private data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: String,
    val buildNumber: String,
    val installTime: Long,
    val updateTime: Long
)

private fun getAndroidVersionName(sdkInt: Int): String {
    return when (sdkInt) {
        Build.VERSION_CODES.S -> "12"
        Build.VERSION_CODES.S_V2 -> "12L"
        Build.VERSION_CODES.TIRAMISU -> "13"
        Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"
        Build.VERSION_CODES.VANILLA_ICE_CREAM -> "15"
        else -> sdkInt.toString()
    }
}

@Composable
private fun VersionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            value,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}