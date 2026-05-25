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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AboutScreen(settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current

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
                        VersionInfoRow("构建日期", "2026年05月25日")
                        VersionInfoRow(
                            "最低支持",
                            "Android 12 (API 31)"
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
}

data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val buildDate: String,
    val buildNumber: String,
    val installTime: Long,
    val updateTime: Long
)

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
            text = label,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
