package com.fioiu8.linkbot.ui

import android.graphics.Canvas
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fioiu8.linkbot.ui.screens.*
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * MainApp - 应用主入口 Composable
 *
 * 职责：
 * - 管理底部导航栏（聊天、笔记、设置、关于）
 * - 处理页面切换动画
 * - 管理导航栏毛玻璃效果
 * - 协调各个页面的 ViewModel 共享
 *
 * 架构设计：
 * - 使用 Scaffold 作为主布局容器
 * - NavigationBar 提供底部导航
 * - AnimatedContent 实现页面切换动画
 * - 通过 ViewModel 共享状态
 */
@Composable
fun MainApp(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    // ==================== 状态收集 ====================
    /** 当前选中的标签页索引 */
    val selectedTab by settingsViewModel.selectedTab.collectAsState()

    /** 是否启用毛玻璃效果 */
    val blurEnabled by settingsViewModel.blurEnabled.collectAsState()

    /** 聊天消息列表（用于判断是否显示毛玻璃） */
    val messages by chatViewModel.messages.collectAsState()

    // ==================== 毛玻璃效果判断 ====================
    /** 是否应该显示毛玻璃效果（聊天页面有消息时） */
    // TODO: 调试用，临时简化条件。正式使用时改为: val shouldBlur = blurEnabled && selectedTab == 0 && messages.isNotEmpty()
    val shouldBlur = blurEnabled

    // ==================== 导航栏动画状态 ====================
    /** 导航栏背景颜色（带动画过渡） */
    val navBarColor by animateColorAsState(
        targetValue = if (shouldBlur) {
            MiuixTheme.colorScheme.surface.copy(alpha = 0.75f)
        } else {
            MiuixTheme.colorScheme.surface.copy(alpha = 0.95f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "navBarColor"
    )

    /** 导航栏阴影大小（带动画过渡） */
    val navBarShadow by animateDpAsState(
        targetValue = if (shouldBlur) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 300),
        label = "navBarShadow"
    )

    // ==================== 页面配置 ====================
    /** 页面标题列表 */
    val pageTitles = listOf("聊天", "笔记", "设置", "关于")

    /** 页面图标列表 */
    val pageIcons = listOf(
        MiuixIcons.Messages,
        MiuixIcons.Notes,
        MiuixIcons.Settings,
        MiuixIcons.Info
    )

    // ==================== AndroidLiquidGlass 玻璃效果 ====================
    /**
     * 创建背景层，用于捕获底层内容并应用玻璃效果
     * 使用 AndroidLiquidGlass 库实现液态玻璃效果
     */
    val backdrop = rememberLayerBackdrop()
    val contentBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, contentBackdrop)
    
    // 在 Composable 上下文预先获取颜色值
    val surfaceColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.5f)

    val blurModifier = if (shouldBlur) {
        // 使用 AndroidLiquidGlass 的 drawBackdrop 修饰符实现玻璃效果
        Modifier.drawBackdrop(
            backdrop = combinedBackdrop,
            // 设置导航栏顶部圆角形状
            shape = { RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) },
            // 玻璃效果配置 - 使用官方示例相同参数
            effects = {
                vibrancy()
                blur(8f.dp.toPx())
                lens(24f.dp.toPx(), 24f.dp.toPx())
            },
            // 绘制表面层
            onDrawSurface = { drawRect(surfaceColor) }
        )
    } else Modifier

    // ==================== 主布局 ====================
    Scaffold(
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(blurModifier)
            ) {
                NavigationBarContainer(
                    selectedTab = selectedTab,
                    onTabSelected = { settingsViewModel.selectTab(it) },
                    titles = pageTitles,
                    icons = pageIcons,
                    backgroundColor = navBarColor,
                    shadowElevation = navBarShadow
                )
            }
        }
    ) { paddingValues ->
        // 将页面内容绑定到 backdrop，使其内容能被玻璃效果捕获
        val contentBackdropModifier = if (shouldBlur) Modifier.layerBackdrop(contentBackdrop) else Modifier
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(contentBackdropModifier)
        ) {
            PageContent(
                selectedTab = selectedTab,
                chatViewModel = chatViewModel,
                settingsViewModel = settingsViewModel,
                paddingValues = paddingValues
            )
        }
    }
}

/**
 * 导航栏容器
 * @param selectedTab 当前选中的标签索引
 * @param onTabSelected 标签选中回调
 * @param titles 页面标题列表
 * @param icons 页面图标列表
 * @param backgroundColor 背景颜色
 * @param shadowElevation 阴影高度
 */
@Composable
private fun NavigationBarContainer(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    titles: List<String>,
    icons: List<ImageVector>,
    backgroundColor: androidx.compose.ui.graphics.Color,
    shadowElevation: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = shadowElevation
    ) {
        NavigationBar(modifier = Modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
}

/**
 * 页面内容区域
 * @param selectedTab 当前选中的标签索引
 * @param chatViewModel 聊天 ViewModel
 * @param settingsViewModel 设置 ViewModel
 * @param paddingValues 内边距（来自 Scaffold）
 */
@Composable
private fun PageContent(
    selectedTab: Int,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    paddingValues: PaddingValues
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            // 页面切换动画：根据切换方向决定滑动方向
            val direction = if (targetState > initialState) 1 else -1
            fadeIn(animationSpec = tween(300)) + 
            slideInHorizontally(
                initialOffsetX = { direction * it / 4 },
                animationSpec = tween(300)
            ) togetherWith 
            fadeOut(animationSpec = tween(300)) + 
            slideOutHorizontally(
                targetOffsetX = { -direction * it / 4 },
                animationSpec = tween(300)
            )
        },
        label = "pageTransition"
    ) { tab ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // 根据选中的标签显示对应的页面
            when (tab) {
                0 -> ChatScreen(chatViewModel, settingsViewModel)
                1 -> NotesScreen(chatViewModel, settingsViewModel)
                2 -> SettingsScreen(chatViewModel, settingsViewModel)
                3 -> AboutScreen(settingsViewModel)
                else -> ChatScreen(chatViewModel, settingsViewModel)
            }
        }
    }
}