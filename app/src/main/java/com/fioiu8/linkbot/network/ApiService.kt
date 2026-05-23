package com.fioiu8.linkbot.network

import com.fioiu8.linkbot.data.ToolManager
import com.fioiu8.linkbot.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * ApiService - AI API 网络服务
 *
 * 职责：
 * - 与各种 AI API 进行通信（DeepSeek、OpenAI 等）
 * - 处理流式响应，逐块接收 AI 回复
 * - 解析工具调用请求（支持 XML 和标准 JSON 两种格式）
 * - 查询 API 余额信息
 *
 * 支持的 Provider：
 * - deepseek: DeepSeek API（默认）
 * - openai: OpenAI API
 * - custom: 用户自定义 API 地址
 *
 * 使用 OkHttp 进行 HTTP 请求，Gson 进行 JSON 解析
 * 流式响应通过 SSE（Server-Sent Events）实现
 */
object ApiService {

    // ==================== HTTP 客户端配置 ====================
    // OkHttp 客户端，单例复用
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时 30 秒
        .readTimeout(60, TimeUnit.SECONDS)      // 读取超时 60 秒
        .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时 30 秒
        .build()

    // Gson 实例用于 JSON 序列化/反序列化
    private val gson = Gson()

    // JSON 内容类型
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // ==================== 数据类定义 ====================
    /**
     * API 发送结果
     *
     * @param content AI 回复的文本内容
     * @param reasoningContent 深度思考内容（reasoning_content）
     * @param toolCalls 工具调用列表
     */
    data class SendResult(
        val content: String = "",
        val reasoningContent: String = "",
        val toolCalls: List<ToolCall> = emptyList()
    )

    /**
     * 流式响应片段
     *
     * @param content 文本内容片段
     * @param reasoningContent 思考内容片段
     */
    data class Chunk(
        val content: String = "",
        val reasoningContent: String = ""
    )

    // ==================== XML 工具调用解析 ====================
    /**
     * 解析 DeepSeek API 返回的 XML 格式工具调用
     *
     * DeepSeek 使用特殊的 XML 标签格式：
     * <｜｜DSML｜｜invoke name="tool_name">...参数...</｜｜DSML｜｜invoke>
     *
     * @param content AI 返回的完整内容
     * @return 解析后的工具调用列表
     */
    private fun parseXmlToolCalls(content: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        // 匹配 XML 格式的工具调用
        val toolPattern = Pattern.compile(
            "<｜｜DSML｜｜invoke name=\"([^\"]+)\">(.*?)</｜｜DSML｜｜invoke>",
            Pattern.DOTALL
        )
        val matcher = toolPattern.matcher(content)

        while (matcher.find()) {
            val toolName = matcher.group(1) ?: continue
            val toolBody = matcher.group(2) ?: ""

            // 解析参数：尝试多种格式
            val arguments = parseToolArguments(toolBody)

            val argsJson = gson.toJson(arguments)

            toolCalls.add(
                ToolCall(
                    id = "call_${System.currentTimeMillis()}_${toolCalls.size}",
                    type = "function",
                    function = FunctionCall(
                        name = toolName,
                        arguments = argsJson
                    )
                )
            )
        }

        return toolCalls
    }

    /**
     * 解析工具调用参数
     *
     * 支持多种参数格式：
     * 1. 嵌套 XML 参数格式：<｜｜DSML｜｜param name="xxx">value</｜｜DSML｜｜param>
     * 2. 简单参数格式：<param name="xxx">value</param>
     * 3. 直接 JSON 格式
     * 4. 如果以上都失败，将整个 body 作为 query 参数
     *
     * @param body 工具调用的参数部分
     * @return 解析后的参数字典
     */
    private fun parseToolArguments(body: String): Map<String, String> {
        val arguments = mutableMapOf<String, String>()
        val trimmedBody = body.trim()

        // 调试：打印原始参数
        println("parseToolArguments - 原始 body: '$trimmedBody'")

        // 尝试解析格式1：<｜｜DSML｜｜param name="xxx">value</｜｜DSML｜｜param>
        val dsmlParamPattern = Pattern.compile(
            "<｜｜DSML｜｜param name=\"([^\"]+)\">(.*?)</｜｜DSML｜｜param>",
            Pattern.DOTALL
        )
        val dsmlMatcher = dsmlParamPattern.matcher(trimmedBody)
        while (dsmlMatcher.find()) {
            val paramName = dsmlMatcher.group(1) ?: continue
            val paramValue = dsmlMatcher.group(2)?.trim() ?: ""
            if (paramValue.isNotBlank()) {
                arguments[paramName] = paramValue
            }
        }

        // 如果没有找到参数，尝试格式2：<param name="xxx">value</param>
        if (arguments.isEmpty()) {
            val paramPattern = Pattern.compile(
                "<param name=\"([^\"]+)\">(.*?)</param>",
                Pattern.DOTALL
            )
            val paramMatcher = paramPattern.matcher(trimmedBody)
            while (paramMatcher.find()) {
                val paramName = paramMatcher.group(1) ?: continue
                val paramValue = paramMatcher.group(2)?.trim() ?: ""
                if (paramValue.isNotBlank()) {
                    arguments[paramName] = paramValue
                }
            }
        }

        // 如果没有找到参数，尝试格式3：JSON 格式
        if (arguments.isEmpty() && trimmedBody.startsWith("{") && trimmedBody.endsWith("}")) {
            try {
                val jsonObject = gson.fromJson(trimmedBody, JsonObject::class.java)
                for (entry in jsonObject.entrySet()) {
                    arguments[entry.key] = entry.value.asString
                }
            } catch (e: Exception) {
                // JSON 解析失败，继续尝试其他格式
            }
        }

        // 如果以上都失败，格式4：直接将 body 作为 query 参数（适用于简单搜索）
        if (arguments.isEmpty() && trimmedBody.isNotBlank()) {
            // 清理可能存在的标签和特殊字符
            val cleanQuery = trimmedBody
                .replace("<[^>]*>".toRegex(), "")           // 移除 XML 标签
                .replace("</[^>]*>".toRegex(), "")          // 移除闭合标签
                .replace("｜｜DSML｜｜", "")                  // 移除 DSML 标记
                .replace("\\s+".toRegex(), " ")             // 合并多个空格
                .trim()

            if (cleanQuery.isNotBlank()) {
                arguments["query"] = cleanQuery
            }
        }

        // 格式5：尝试解析 key=value 格式
        if (arguments.isEmpty() && trimmedBody.isNotBlank()) {
            val keyValuePattern = Regex("(\\w+)\\s*=\\s*[\"']?(.*?)[\"']?$")
            val matchResult = keyValuePattern.find(trimmedBody)
            if (matchResult != null) {
                val key = matchResult.groupValues[1].trim()
                val value = matchResult.groupValues[2].trim()
                if (key.isNotBlank() && value.isNotBlank()) {
                    arguments[key] = value
                }
            }
        }

        return arguments
    }

    /**
     * 清理 XML 标签，获取纯文本内容
     *
     * 当 AI 返回的内容中包含工具调用 XML 标签时，
     * 需要将这些标签移除，只保留纯文本回复
     *
     * @param content 包含 XML 标签的原始内容
     * @return 清理后的纯文本内容
     */
    private fun cleanXmlContent(content: String): String {
        // 移除所有 <｜｜DSML｜｜...> 标签
        val toolCallPattern = Pattern.compile(
            "<｜｜DSML｜｜[^>]*>.*?</｜｜DSML｜｜>",
            Pattern.DOTALL
        )
        var cleaned = toolCallPattern.matcher(content).replaceAll("")

        // 移除可能残留的标签
        cleaned = cleaned.replace("<｜｜DSML｜｜[^>]*>".toRegex(), "")
            .replace("</｜｜DSML｜｜>".toRegex(), "")
            .trim()

        return cleaned
    }

    // ==================== 核心功能：发送消息 ====================
    /**
     * 发送消息给 AI 并接收流式响应
     *
     * 这是与应用的主要交互方法：
     * 1. 构建请求体（包含消息历史、工具定义等）
     * 2. 发送 POST 请求到 API
     * 3. 以流式方式接收响应（SSE）
     * 4. 每收到一个片段就调用 onChunk 回调
     * 5. 检查并解析工具调用（支持 XML 和 JSON 两种格式）
     *
     * @param apiKey API 密钥
     * @param model 模型名称
     * @param messages 消息历史列表
     * @param provider AI 提供商（deepseek/openai/custom）
     * @param customBaseUrl 自定义 API 地址（当 provider=custom 时使用）
     * @param enableThinking 是否启用深度思考
     * @param reasoningEffort 推理强度
     * @param enableToolCalls 是否启用工具调用
     * @param onChunk 流式回调，每收到一个片段就调用
     * @return 发送结果，包含完整内容和工具调用
     */
    suspend fun sendMessageStream(
        apiKey: String,
        model: String,
        messages: List<Message>,
        provider: String = "deepseek",
        customBaseUrl: String = "",
        enableThinking: Boolean = false,
        reasoningEffort: String = "high",
        enableToolCalls: Boolean = false,
        onChunk: suspend (Chunk) -> Unit
    ): Result<SendResult> = withContext(Dispatchers.IO) {
        try {
            // 根据 provider 确定 API 地址
            val baseUrl = when (provider) {
                "deepseek" -> "https://api.deepseek.com/v1"
                "openai" -> "https://api.openai.com/v1"
                "custom" -> customBaseUrl
                else -> "https://api.deepseek.com/v1"
            }

            val url = "$baseUrl/chat/completions"

            // 构建消息列表
            val requestMessages = messages.map { msg ->
                val msgMap = mutableMapOf<String, Any>(
                    "role" to msg.role,
                    "content" to msg.content
                )

                // 添加 reasoning_content（thinking mode 要求必须传递）
                if (msg.reasoningContent != null) {
                    msgMap["reasoning_content"] = msg.reasoningContent
                }

                // 添加 tool_calls（如果有）
                if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                    msgMap["tool_calls"] = msg.toolCalls.map { tc ->
                        mapOf(
                            "id" to tc.id,
                            "type" to tc.type,
                            "function" to mapOf(
                                "name" to tc.function.name,
                                "arguments" to tc.function.arguments
                            )
                        )
                    }
                }

                // 添加 tool_call_id（对于工具返回的消息）
                if (msg.role == "tool") {
                    msgMap["tool_call_id"] = msg.toolCallId ?: ""
                }
                if (msg.name != null) {
                    msgMap["name"] = msg.name
                }

                msgMap
            }

            // 构建请求体
            val requestBody = mutableMapOf<String, Any>(
                "model" to model,
                "messages" to requestMessages,
                "stream" to true  // 启用流式响应
            )

            // 添加 thinking mode 配置（如果启用）
            if (enableThinking) {
                requestBody["thinking"] = mapOf("type" to "enabled")
                requestBody["reasoning_effort"] = reasoningEffort
            }

            // 添加工具定义（如果启用）
            if (enableToolCalls) {
                requestBody["tools"] = ToolManager.tools.map { tool ->
                    mapOf(
                        "type" to "function",
                        "function" to mapOf(
                            "name" to tool.function.name,
                            "description" to tool.function.description,
                            "parameters" to tool.function.parameters
                        )
                    )
                }
            }

            // 发送请求
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            // 接收流式响应
            var fullContent = ""
            var fullReasoning = ""
            val toolCallsMap = mutableMapOf<String, ToolCall>()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext Result.failure(IOException("API error: ${response.code} - $errorBody"))
                }

                // 读取 SSE 流
                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: return@withContext Result.failure(IOException("No response body"))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    // SSE 格式：data: {...}
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6)
                        if (data == "[DONE]") {
                            continue
                        }

                        try {
                            // 安全解析 JSON
                            val jsonResponse = runCatching {
                                gson.fromJson(data, JsonObject::class.java)
                            }.getOrNull()

                            if (jsonResponse == null) {
                                continue
                            }

                            // 安全获取 choices 数组
                            val choicesElement = jsonResponse.get("choices")
                            if (choicesElement == null || choicesElement.isJsonNull) {
                                continue
                            }

                            val choices = choicesElement.asJsonArray
                            if (choices.size() == 0) {
                                continue
                            }

                            // 安全获取第一个元素
                            val firstChoiceElement = choices[0]
                            if (firstChoiceElement.isJsonNull) {
                                continue
                            }
                            val firstChoice = firstChoiceElement.asJsonObject

                            // 安全获取 delta，处理 JsonNull 情况
                            val deltaElement = firstChoice.get("delta")
                            if (deltaElement == null || deltaElement.isJsonNull) {
                                continue  // delta 为空，跳过此帧
                            }
                            val delta = deltaElement.asJsonObject

                            // 文本内容
                            val content = delta.get("content")?.asString ?: ""
                            if (content.isNotEmpty()) {
                                fullContent += content
                                onChunk(Chunk(content = content))
                            }

                            // 思考内容
                            val reasoningContent = delta.get("reasoning_content")?.asString ?: ""
                            if (reasoningContent.isNotEmpty()) {
                                fullReasoning += reasoningContent
                                onChunk(Chunk(reasoningContent = reasoningContent))
                            }

                            // 标准 JSON 格式的工具调用
                            val toolCallsArrayElement = delta.get("tool_calls")
                            if (toolCallsArrayElement != null && !toolCallsArrayElement.isJsonNull) {
                                val toolCallsArray = toolCallsArrayElement.asJsonArray
                                if (toolCallsArray != null && toolCallsArray.size() > 0) {
                                    for (i in 0 until toolCallsArray.size()) {
                                        // 处理工具调用元素为 JsonNull 的情况
                                        val tcElement = toolCallsArray[i]
                                        if (tcElement.isJsonNull) {
                                            continue
                                        }
                                        val tc = tcElement.asJsonObject
                                        val id = tc.get("id")?.asString ?: continue

                                        // 处理 function 为 JsonNull 的情况
                                        val functionElement = tc.get("function")
                                        if (functionElement == null || functionElement.isJsonNull) {
                                            continue
                                        }
                                        val function = functionElement.asJsonObject
                                        val name = function.get("name")?.asString ?: ""

                                        // 调试：打印完整的工具调用对象
                                        println("Tool call delta - id: '$id', name: '$name', function: ${gson.toJson(function)}")

                                        // 处理 arguments：可能是字符串或 JSON 对象
                                        // 流式响应中 arguments 可能是逐步发送的，需要累积
                                        val argumentsElement = function.get("arguments")
                                        var newArguments = ""
                                        if (argumentsElement?.isJsonPrimitive == true) {
                                            val argStr = argumentsElement.asString
                                            println("Arguments is string: '$argStr'")
                                            newArguments = argStr
                                        } else if (argumentsElement?.isJsonObject == true) {
                                            val argJson = gson.toJson(argumentsElement)
                                            println("Arguments is object: '$argJson'")
                                            newArguments = argJson
                                        } else if (argumentsElement != null) {
                                            println("Arguments type: ${argumentsElement.javaClass.name}, value: ${gson.toJson(argumentsElement)}")
                                            newArguments = gson.toJson(argumentsElement)
                                        } else {
                                            println("Arguments is null or missing")
                                            newArguments = ""
                                        }

                                        // 获取已存在的工具调用（用于累积 arguments）
                                        val existingToolCall = toolCallsMap[id]
                                        var accumulatedArguments = newArguments
                                        if (existingToolCall != null && existingToolCall.function.arguments.isNotEmpty()) {
                                            // 尝试合并参数（处理流式累积）
                                            accumulatedArguments = existingToolCall.function.arguments + newArguments
                                        }

                                        toolCallsMap[id] = ToolCall(
                                            id = id,
                                            type = "function",
                                            function = FunctionCall(name = name, arguments = accumulatedArguments)
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Parse error: ${e.message}")
                        }
                    }
                }
            }

            // 调试：打印完整响应内容
            println("sendMessageStream - fullContent: '$fullContent'")
            println("sendMessageStream - toolCallsMap size: ${toolCallsMap.size}")
            // 打印每个工具调用的详细信息
            toolCallsMap.forEach { (id, tc) ->
                println("Tool call - id: '$id', name: '${tc.function.name}', arguments: '${tc.function.arguments}'")
            }

            // 检查内容中是否包含 XML 格式的工具调用
            val xmlToolCalls = parseXmlToolCalls(fullContent)
            println("sendMessageStream - xmlToolCalls size: ${xmlToolCalls.size}")

            if (xmlToolCalls.isNotEmpty()) {
                // 如果有 XML 工具调用，清理内容中的 XML 标签
                val cleanedContent = cleanXmlContent(fullContent)

                return@withContext Result.success(
                    SendResult(
                        content = cleanedContent,
                        reasoningContent = fullReasoning,
                        toolCalls = xmlToolCalls
                    )
                )
            }

            // 返回标准结果
            return@withContext Result.success(
                SendResult(
                    content = fullContent,
                    reasoningContent = fullReasoning,
                    toolCalls = toolCallsMap.values.toList()
                )
            )
        } catch (e: Exception) {
            println("Request exception: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    // ==================== 余额查询 ====================
    /**
     * 查询 DeepSeek API 账户余额
     *
     * @param apiKey DeepSeek API 密钥
     * @return 余额信息结果
     */
    suspend fun queryBalance(apiKey: String): Result<BalanceResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.deepseek.com/user/balance"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Balance API error: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                val balanceResponse = gson.fromJson(body, BalanceResponse::class.java)
                return@withContext Result.success(balanceResponse)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}
