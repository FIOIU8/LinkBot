package com.fioiu8.linkbot.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.linkbot.model.SavedNote
import com.fioiu8.linkbot.viewmodel.ChatViewModel
import com.fioiu8.linkbot.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 笔记页面 - 管理用户保存的笔记
 *
 * 职责：
 * - 展示保存的笔记列表
 * - 支持笔记详情查看
 * - 支持笔记删除和清空
 * - 支持继续基于笔记对话
 * - 支持下拉刷新
 *
 * @param viewModel ChatViewModel 实例
 * @param settingsViewModel SettingsViewModel 实例
 */
@Composable
fun NotesScreen(
    viewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    // 从 ViewModel 收集笔记列表
    val notes by viewModel.notes.collectAsState()
    
    // 对话框状态管理
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete: SavedNote? by remember { mutableStateOf(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailNote: SavedNote? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = "笔记",
            actions = {
                if (notes.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearNotes() },
                        minWidth = 40.dp,
                        minHeight = 40.dp,
                        cornerRadius = 20.dp
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = "清空笔记",
                            tint = MiuixTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = MiuixIcons.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("暂无笔记", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.overScrollVertical(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300))
                        ) {
                            NoteCard(
                                note = note,
                                onClick = { detailNote = note; showDetailDialog = true },
                                onContinue = {
                                    viewModel.continueFromNote(note)
                                    settingsViewModel.selectTab(0)
                                },
                                onDelete = { noteToDelete = note; showDeleteDialog = true }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showDeleteDialog && noteToDelete != null,
        enter = fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(300))
    ) {
        OverlayDialog(
            title = "删除笔记",
            show = showDeleteDialog && noteToDelete != null,
            onDismissRequest = { showDeleteDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("确定删除这条笔记？")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(text = "取消", onClick = { showDeleteDialog = false })
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it.id) }
                        showDeleteDialog = false
                    }) { Text("删除") }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showDetailDialog && detailNote != null,
        enter = fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(300))
    ) {
        OverlayDialog(
            title = detailNote?.title ?: "",
            show = showDetailDialog && detailNote != null,
            onDismissRequest = { showDetailDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("提问:", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                Card(cornerRadius = 12.dp) {
                    Text(detailNote?.userMessage ?: "", Modifier.padding(12.dp), fontSize = 14.sp)
                }
                Text("回答:", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                Card(cornerRadius = 12.dp) {
                    Text(detailNote?.aiMessage ?: "", Modifier.padding(12.dp), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: SavedNote, onClick: () -> Unit, onContinue: () -> Unit, onDelete: () -> Unit) {
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Card(
        cornerRadius = 16.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    note.title,
                    style = MiuixTheme.textStyles.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    df.format(Date(note.createdAt)),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                note.userMessage,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onContinue,
                    minWidth = 36.dp,
                    minHeight = 36.dp,
                    cornerRadius = 18.dp,
                    backgroundColor = MiuixTheme.colorScheme.primary
                ) {
                    Icon(
                        MiuixIcons.Send,
                        "继续对话",
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    minWidth = 36.dp,
                    minHeight = 36.dp,
                    cornerRadius = 18.dp
                ) {
                    Icon(
                        MiuixIcons.Delete,
                        "删除",
                        tint = MiuixTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}