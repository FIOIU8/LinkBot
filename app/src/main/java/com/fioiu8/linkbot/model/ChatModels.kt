package com.fioiu8.linkbot.model

/**
 * API 配置数据类
 * @param provider API 提供商（deepseek、openai、custom）
 * @param customBaseUrl 自定义 API 基础地址
 * @param model 模型名称
 * @param apiKey API 密钥
 * @param systemPrompt 系统提示词
 * @param enableThinking 是否启用思考模式
 * @param reasoningEffort 思考深度（high/medium/low）
 * @param enableToolCalls 是否启用工具调用
 */
data class ApiConfig(
    val provider: String = "deepseek",
    val customBaseUrl: String = "",
    val model: String = "deepseek-v4-flash",
    val apiKey: String = "",
    val systemPrompt: String = "你是专业的知识答疑顾问，知识面全面、讲解通俗易懂。解答我的所有问题时，遵循以下规则：答案精准专业、有理有据，拒绝错误信息；优先用大白话解释，避开晦涩专业术语，必要时搭配举例说明；结构清晰，分点解答，主次分明，先给出核心结论，再展开细节补充；如果问题有多维度答案，全面覆盖、分类说明，简洁高效，不冗余啰嗦。",
    val enableThinking: Boolean = false,
    val reasoningEffort: String = "high",
    val enableToolCalls: Boolean = true
)

/**
 * 聊天消息数据类
 * @param role 角色（user/assistant/tool）
 * @param content 消息内容
 * @param reasoningContent 思考过程内容
 * @param toolCalls 工具调用列表
 * @param toolCallId 工具调用 ID
 * @param name 名称（用于工具调用）
 * @param timestamp 时间戳
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val reasoningContent: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * API 请求消息数据类
 * @param role 角色
 * @param content 内容
 * @param reasoningContent 思考内容
 * @param toolCalls 工具调用
 * @param toolCallId 工具调用 ID
 * @param name 名称
 */
data class Message(
    val role: String,
    val content: String,
    val reasoningContent: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null
)

/**
 * 工具调用数据类
 * @param id 调用 ID
 * @param type 类型（function）
 * @param function 函数调用信息
 */
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

/**
 * 函数调用数据类
 * @param name 函数名称
 * @param arguments 参数（JSON 字符串）
 */
data class FunctionCall(
    val name: String,
    val arguments: String
)

/**
 * 聊天请求数据类
 * @param model 模型名称
 * @param messages 消息列表
 * @param stream 是否流式响应
 * @param reasoning_effort 思考深度
 * @param thinking 思考配置
 * @param tools 可用工具列表
 */
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val reasoning_effort: String? = null,
    val thinking: ThinkingConfig? = null,
    val tools: List<ToolDef>? = null
)

/**
 * 思考配置数据类
 * @param type 类型
 */
data class ThinkingConfig(val type: String)

/**
 * 工具定义数据类
 * @param type 类型（function）
 * @param function 工具函数定义
 */
data class ToolDef(
    val type: String = "function",
    val function: ToolFunction
)

/**
 * 工具函数数据类
 * @param name 函数名称
 * @param description 函数描述
 * @param parameters 参数定义
 */
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

/**
 * 工具参数定义数据类
 * @param type 类型（object）
 * @param properties 属性定义
 * @param required 必填参数列表
 * @param additionalProperties 是否允许额外属性
 */
data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean = false
)

/**
 * 工具属性数据类
 * @param type 属性类型
 * @param description 属性描述
 */
data class ToolProperty(
    val type: String,
    val description: String
)

/**
 * 消息响应数据类
 * @param role 角色
 * @param content 内容
 * @param reasoning_content 思考内容
 * @param tool_calls 工具调用列表
 */
data class MessageResponse(
    val role: String,
    val content: String?,
    val reasoning_content: String? = null,
    val tool_calls: List<ToolCallResponse>? = null
)

/**
 * 工具调用响应数据类
 * @param id 调用 ID
 * @param type 类型
 * @param function 函数调用响应
 */
data class ToolCallResponse(
    val id: String,
    val type: String = "function",
    val function: FunctionCallResponse
)

/**
 * 函数调用响应数据类
 * @param name 函数名称
 * @param arguments 参数
 */
data class FunctionCallResponse(
    val name: String,
    val arguments: String
)