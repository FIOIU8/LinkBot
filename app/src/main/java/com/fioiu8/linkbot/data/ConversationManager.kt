package com.fioiu8.linkbot.data

import android.content.Context
import com.fioiu8.linkbot.model.ChatMessage
import com.fioiu8.linkbot.model.SavedNote
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 对话管理器 - 负责对话和笔记的持久化存储
 *
 * 使用 JSON 格式将数据存储到应用私有目录，支持内存缓存提高读取性能。
 *
 * @param context Android 上下文
 */
class ConversationManager(private val context: Context) {

    // Gson 实例，用于 JSON 序列化/反序列化
    private val gson by lazy { Gson() }
    
    // 对话内存缓存
    private val conversationsCache = ConcurrentHashMap<String, ConversationData>()
    
    // 笔记内存缓存
    private val notesCache = ConcurrentHashMap<String, SavedNote>()

    // 对话存储目录
    private val conversationsDir by lazy {
        File(context.filesDir, "conversations").apply { mkdirs() }
    }

    // 笔记存储目录
    private val notesDir by lazy {
        File(context.filesDir, "notes").apply { mkdirs() }
    }

    /**
     * 保存对话到文件系统
     * @param messages 消息列表
     * @param name 对话名称，默认自动生成
     * @return 对话 ID
     */
    suspend fun saveConversation(messages: List<ChatMessage>, name: String = "对话_${System.currentTimeMillis()}"): String = withContext(Dispatchers.IO) {
        val conversation = ConversationData(
            id = System.currentTimeMillis().toString(),
            name = name,
            messages = messages,
            createdAt = System.currentTimeMillis()
        )
        val file = File(conversationsDir, "${conversation.id}.json")
        file.writeText(gson.toJson(conversation))
        conversationsCache[conversation.id] = conversation
        conversation.id
    }

    /**
     * 根据 ID 加载对话
     * @param id 对话 ID
     * @return 对话数据，如果不存在返回 null
     */
    suspend fun loadConversation(id: String): ConversationData? = withContext(Dispatchers.IO) {
        // 优先从缓存读取
        conversationsCache[id]?.let { return@withContext it }

        val file = File(conversationsDir, "$id.json")
        if (!file.exists()) return@withContext null

        try {
            val conversation = gson.fromJson(file.readText(), ConversationData::class.java)
            conversationsCache[id] = conversation
            conversation
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取所有对话列表
     * @return 按创建时间降序排列的对话列表
     */
    suspend fun getAllConversations(): List<ConversationData> = withContext(Dispatchers.IO) {
        conversationsDir.listFiles()
            ?.mapNotNull { file ->
                val id = file.nameWithoutExtension
                conversationsCache[id] ?: run {
                    try {
                        val conversation = gson.fromJson(file.readText(), ConversationData::class.java)
                        conversationsCache[id] = conversation
                        conversation
                    } catch (e: Exception) { null }
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    /**
     * 删除指定对话
     * @param id 对话 ID
     */
    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        conversationsCache.remove(id)
        File(conversationsDir, "$id.json").delete()
    }

    /**
     * 重命名对话
     * @param id 对话 ID
     * @param newName 新名称
     */
    suspend fun renameConversation(id: String, newName: String) = withContext(Dispatchers.IO) {
        val conversation = loadConversation(id) ?: return@withContext
        conversation.name = newName
        conversationsCache[id] = conversation
        File(conversationsDir, "$id.json").writeText(gson.toJson(conversation))
    }

    /**
     * 保存笔记
     * @param userMessage 用户消息
     * @param aiMessage AI 回复消息
     * @param title 笔记标题，默认从用户消息截取
     * @return 保存的笔记对象
     */
    suspend fun saveNote(userMessage: ChatMessage, aiMessage: ChatMessage, title: String = ""): SavedNote = withContext(Dispatchers.IO) {
        val note = SavedNote(
            id = System.currentTimeMillis().toString(),
            title = title.ifBlank {
                // 默认标题：截取用户消息前30个字符
                userMessage.content.take(30).replace("\n", " ") + if (userMessage.content.length > 30) "..." else ""
            },
            userMessage = userMessage.content,
            aiMessage = aiMessage.content,
            createdAt = System.currentTimeMillis()
        )
        val file = File(notesDir, "${note.id}.json")
        file.writeText(gson.toJson(note))
        notesCache[note.id] = note
        note
    }

    /**
     * 获取所有笔记列表
     * @return 按创建时间降序排列的笔记列表
     */
    suspend fun getAllNotes(): List<SavedNote> = withContext(Dispatchers.IO) {
        notesDir.listFiles()
            ?.mapNotNull { file ->
                val id = file.nameWithoutExtension
                notesCache[id] ?: run {
                    try {
                        val note = gson.fromJson(file.readText(), SavedNote::class.java)
                        notesCache[id] = note
                        note
                    } catch (e: Exception) { null }
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    /**
     * 删除指定笔记
     * @param id 笔记 ID
     */
    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        notesCache.remove(id)
        File(notesDir, "$id.json").delete()
    }

    /**
     * 删除所有笔记
     */
    suspend fun deleteAllNotes() = withContext(Dispatchers.IO) {
        notesCache.clear()
        notesDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * 对话数据类
 * @param id 对话唯一标识
 * @param name 对话名称
 * @param messages 消息列表
 * @param createdAt 创建时间戳
 */
data class ConversationData(
    var id: String = "",
    var name: String = "",
    var messages: List<ChatMessage> = emptyList(),
    var createdAt: Long = 0L
)