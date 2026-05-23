package com.fioiu8.linkbot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.linkbot.data.ConversationData
import com.fioiu8.linkbot.model.ChatMessage
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationListDialog(
    conversations: List<ConversationData>,
    currentMessages: List<ChatMessage>,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    OverlayDialog(
        title = "对话列表",
        show = true,
        onDismissRequest = onDismiss,
        renderInRootScaffold = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNew,
                    modifier = Modifier.weight(1f),
                    minHeight = 40.dp,
                    cornerRadius = 12.dp
                ) {
                    Icon(MiuixIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("新对话", fontSize = 14.sp)
                }

                if (currentMessages.isNotEmpty()) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        minHeight = 40.dp,
                        cornerRadius = 12.dp
                    ) {
                        Icon(MiuixIcons.Ok, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存当前", fontSize = 14.sp)
                    }
                }
            }

            if (conversations.isNotEmpty()) {
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine, thickness = 0.5.dp)
            }

            if (conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("暂无保存的对话", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onLoad = { onLoad(conversation.id) },
                            onDelete = { onDelete(conversation.id) },
                            isCurrentConversation = conversation.id == conversations.firstOrNull()?.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationData,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    isCurrentConversation: Boolean
) {
    val preview = conversation.messages.lastOrNull()?.content?.take(50)?.replace("\n", " ") ?: ""
    val messageCount = conversation.messages.size
    val timestamp = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(conversation.createdAt))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentConversation) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onLoad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                Text(conversation.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(preview, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$messageCount 条消息 · $timestamp", fontSize = 10.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f))
            }
            IconButton(onClick = onDelete, minWidth = 32.dp, minHeight = 32.dp, cornerRadius = 16.dp) {
                Icon(MiuixIcons.Delete, "删除", modifier = Modifier.size(16.dp), tint = MiuixTheme.colorScheme.error)
            }
        }
    }
}