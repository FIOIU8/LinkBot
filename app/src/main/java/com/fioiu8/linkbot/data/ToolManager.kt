package com.fioiu8.linkbot.data

import com.fioiu8.linkbot.model.ToolDef
import com.fioiu8.linkbot.model.ToolFunction
import com.fioiu8.linkbot.model.ToolParameters
import com.fioiu8.linkbot.model.ToolProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * 工具管理器 - 负责管理和执行 AI 可用的工具
 *
 * 支持的工具：
 * - get_current_time: 获取当前时间
 * - get_weather: 获取天气信息
 * - calculator: 数学计算器
 * - get_random_number: 生成随机数
 * - web_search: 联网搜索
 */
object ToolManager {

    /**
     * 工具定义列表 - 用于向 AI 模型声明可用工具
     */
    val tools: List<ToolDef> = listOf(
        // 获取当前时间工具
        ToolDef(function = ToolFunction(
            name = "get_current_time",
            description = "获取当前日期和时间",
            parameters = ToolParameters(
                properties = emptyMap(),
                required = emptyList()
            )
        )),
        // 获取天气工具
        ToolDef(function = ToolFunction(
            name = "get_weather",
            description = "获取指定城市的天气信息",
            parameters = ToolParameters(
                properties = mapOf(
                    "location" to ToolProperty("string", "城市名称，例如：北京、上海、杭州")
                ),
                required = listOf("location")
            )
        )),
        // 计算器工具
        ToolDef(function = ToolFunction(
            name = "calculator",
            description = "执行数学计算，支持加减乘除",
            parameters = ToolParameters(
                properties = mapOf(
                    "expression" to ToolProperty("string", "数学表达式，例如：2+3*4")
                ),
                required = listOf("expression")
            )
        )),
        // 随机数工具
        ToolDef(function = ToolFunction(
            name = "get_random_number",
            description = "获取指定范围内的随机整数",
            parameters = ToolParameters(
                properties = mapOf(
                    "min" to ToolProperty("number", "最小值"),
                    "max" to ToolProperty("number", "最大值")
                ),
                required = listOf("min", "max")
            )
        )),
        // 联网搜索工具
        ToolDef(function = ToolFunction(
            name = "web_search",
            description = "联网搜索实时信息，获取最新网页内容。当需要查询实时数据、最新新闻、天气、人物信息等信息时使用。必须包含query参数。",
            parameters = ToolParameters(
                properties = mapOf(
                    "query" to ToolProperty("string", "搜索关键词，必须从用户问题中提取，例如用户问'搜索周杰伦'，则query为'周杰伦'"),
                    "num_results" to ToolProperty("number", "返回结果数量，默认3，最大5")
                ),
                required = listOf("query")
            )
        ))
    )

    /**
     * 执行指定工具
     * @param name 工具名称
     * @param arguments 工具参数（JSON格式字符串）
     * @return 工具执行结果
     */
    suspend fun executeTool(name: String, arguments: String): String = withContext(Dispatchers.IO) {
        try {
            println("executeTool - name: '$name', arguments: '$arguments'")
            
            // 清理参数，确保不为空
            val cleanArgs = if (arguments.isBlank()) "{}" else arguments

            // 解析 JSON 参数
            val args = try {
                JSONObject(cleanArgs)
            } catch (e: Exception) {
                JSONObject()
            }

            // 根据工具名称分发执行
            when (name) {
                // 计算器工具：支持 expression 参数或 a、b、op 参数格式
                "calculator" -> {
                    val expr = when {
                        args.has("expression") -> args.optString("expression", "0")
                        args.has("a") && args.has("b") -> {
                            val a = args.optString("a", "0")
                            val b = args.optString("b", "0")
                            val op = args.optString("op", "+")
                            when (op) {
                                "+", "加" -> "$a+$b"
                                "-", "减" -> "$a-$b"
                                "*", "乘" -> "$a*$b"
                                "/", "除" -> "$a/$b"
                                else -> "$a+$b"
                            }
                        }
                        else -> "0"
                    }
                    val result = simpleCalc(expr)
                    "$expr = $result"
                }
                // 天气查询工具：模拟返回天气信息
                "get_weather" -> {
                    val location = args.optString("location", args.optString("city", "北京"))
                    val conditions = listOf("晴", "多云", "阴", "小雨", "中雨")
                    val temps = (15..35).random()
                    "${location}天气: ${conditions.random()}，温度 ${temps}°C"
                }
                // 时间工具：返回当前时间
                "get_current_time" -> {
                    val df = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
                    "当前时间: ${df.format(Date())}"
                }
                // 随机数工具：返回指定范围内的随机整数
                "get_random_number" -> {
                    val min = args.optInt("min", args.optInt("from", 0))
                    val max = args.optInt("max", args.optInt("to", 100))
                    val num = (min..max).random()
                    "随机数 ($min-$max): $num"
                }
                // 联网搜索工具：支持多种参数格式解析
                "web_search" -> {
                    var query = ""
                    
                    // 尝试从 JSON 参数中提取查询词
                    // 支持的参数名：query, q, keyword, search_query
                    if (query.isBlank()) {
                        try {
                            val jsonArgs = if (cleanArgs.isNotBlank() && cleanArgs != "{}") {
                                JSONObject(cleanArgs)
                            } else {
                                JSONObject()
                            }

                            query = jsonArgs.optString("query", "")
                            if (query.isBlank()) query = jsonArgs.optString("q", "")
                            if (query.isBlank()) query = jsonArgs.optString("keyword", "")
                            if (query.isBlank()) query = jsonArgs.optString("search_query", "")

                            // 如果以上都为空，遍历所有键找第一个非空值（排除 num_results）
                            if (query.isBlank()) {
                                val keys = jsonArgs.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val value = jsonArgs.optString(key, "")
                                    if (value.isNotBlank() && !key.contains("num", ignoreCase = true)) {
                                        query = value
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略解析错误
                        }
                    }

                    // 尝试从嵌套的 function.arguments 中提取
                    if (query.isBlank()) {
                        try {
                            val jsonArgs = JSONObject(cleanArgs)
                            if (jsonArgs.has("function")) {
                                val functionObj = jsonArgs.getJSONObject("function")
                                if (functionObj.has("arguments")) {
                                    val argsStr = functionObj.optString("arguments", "")
                                    if (argsStr.isNotBlank()) {
                                        val argsObj = JSONObject(argsStr)
                                        query = argsObj.optString("query", argsObj.optString("q", ""))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略解析错误
                        }
                    }

                    // 尝试从原始字符串中提取（移除括号和引号）
                    if (query.isBlank() && arguments.isNotBlank()) {
                        val clean = arguments.trim()
                        query = clean
                            .removePrefix("{").removeSuffix("}")
                            .removePrefix("[").removeSuffix("]")
                            .removePrefix("\"").removeSuffix("\"")
                            .trim()
                        
                        // 处理 "key: value" 格式
                        if (query.contains(":") && query.contains("\"")) {
                            val parts = query.split(":", limit = 2)
                            if (parts.size == 2) {
                                query = parts[1].trim().removePrefix("\"").removeSuffix("\"").trim()
                            }
                        }
                    }

                    // 尝试从 XML 格式参数中提取
                    if (query.isBlank() && arguments.isNotBlank()) {
                        val paramPattern = Regex("<param\\s+name=\"([^\"]+)\">(.*?)</param>", RegexOption.DOT_MATCHES_ALL)
                        paramPattern.find(arguments)?.let { matchResult ->
                            val paramName = matchResult.groupValues[1]
                            val paramValue = matchResult.groupValues[2].trim()
                            if ((paramName == "query" || paramName == "q" || paramName == "keyword") && paramValue.isNotBlank()) {
                                query = paramValue
                            }
                        }
                    }

                    // 如果还是没有找到查询词，返回提示信息
                    if (query.isBlank()) {
                        "请提供您想要搜索的内容关键词，例如：'最新科技新闻'"
                    } else {
                        // 获取返回结果数量（默认3条，范围1-5）
                        val num = runCatching {
                            JSONObject(cleanArgs).optInt("num_results", 3)
                        }.getOrDefault(3).coerceIn(1, 5)

                        // 执行实际搜索
                        webSearch(query, num)
                    }
                }
                // 未知工具处理
                else -> "未知工具: $name"
            }
        } catch (e: Exception) {
            "工具执行失败: ${e.message}"
        }
    }

    /**
     * 简单计算器 - 支持加减乘除运算
     * 使用递归方式解析表达式，按照先乘除后加减的顺序
     * @param expression 数学表达式
     * @return 计算结果
     */
    private fun simpleCalc(expression: String): Double {
        return try {
            // 移除所有空格
            val expr = expression.replace(" ", "")

            // 加法运算（最后处理，优先级最低）
            if (expr.contains("+")) {
                val parts = expr.split("+", limit = 2)
                return simpleCalc(parts[0]) + simpleCalc(parts[1])
            }

            // 减法运算（从右往左找减号，处理负数情况）
            if (expr.contains("-") && expr.lastIndexOf('-') > 0) {
                val idx = expr.lastIndexOf('-')
                return simpleCalc(expr.substring(0, idx)) - simpleCalc(expr.substring(idx + 1))
            }

            // 乘法运算
            if (expr.contains("*")) {
                val parts = expr.split("*", limit = 2)
                return simpleCalc(parts[0]) * simpleCalc(parts[1])
            }

            // 除法运算（注意除零处理）
            if (expr.contains("/")) {
                val parts = expr.split("/", limit = 2)
                val divisor = simpleCalc(parts[1])
                return if (divisor == 0.0) 0.0 else simpleCalc(parts[0]) / divisor
            }

            // 如果没有运算符，尝试直接转换为数字
            expr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * 执行联网搜索（优先使用必应搜索）
     * @param query 搜索关键词
     * @param numResults 返回结果数量
     * @return 搜索结果字符串
     */
    private suspend fun webSearch(query: String, numResults: Int): String = withContext(Dispatchers.IO) {
        try {
            // URL 编码搜索关键词
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

            // 构建搜索 URL
            val url = URL("https://m.bing.com/search?q=$encodedQuery&count=$numResults")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000  // 10秒连接超时
            conn.readTimeout = 10000     // 10秒读取超时

            // 设置请求头，模拟浏览器访问
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "text/html")
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")

            // 如果响应不成功，使用备用搜索
            if (conn.responseCode != 200) {
                return@withContext fallbackSearch(query, numResults)
            }

            // 读取 HTML 内容
            val html = conn.inputStream.bufferedReader().use { it.readText() }

            // 解析搜索结果
            val results = mutableListOf<String>()

            // 匹配搜索结果项
            val algoRegex = Regex("""<li[^>]*class="[^"]*b_algo[^"]*"[^>]*>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL)
            val algoMatches = algoRegex.findAll(html).take(numResults).toList()

            // 提取每个结果的标题、链接和摘要
            for ((index, match) in algoMatches.withIndex()) {
                val block = match.groupValues[1]

                // 提取标题
                val titleMatch = Regex("""<h2[^>]*>(.*?)</h2>""", RegexOption.DOT_MATCHES_ALL).find(block)
                val title = titleMatch?.groupValues?.get(1)
                    ?.replace("<[^>]*>".toRegex(), "")
                    ?.trim() ?: continue

                // 提取链接
                val linkMatch = Regex("""<a[^>]*href="([^"]+)"[^>]*>""").find(block)
                val href = linkMatch?.groupValues?.get(1) ?: ""

                // 提取摘要
                val snippet = block
                    .replace("<[^>]*>".toRegex(), " ")  // 移除 HTML 标签
                    .replace("&amp;", "&")              // 解码 HTML 实体
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("\\s+".toRegex(), " ")     // 合并多余空格
                    .trim()
                    .take(300)                          // 限制长度

                if (title.isNotEmpty()) {
                    results.add("${index + 1}. **$title**\n   $snippet${if (href.isNotEmpty()) "\n   $href" else ""}")
                }
            }

            // 如果没有找到结果，使用备用搜索
            if (results.isEmpty()) {
                return@withContext fallbackSearch(query, numResults)
            }

            // 格式化结果
            "必应搜索结果:\n\n${results.joinToString("\n\n")}\n\n请基于以上搜索结果回答用户问题。"
        } catch (e: Exception) {
            // 发生异常时使用备用搜索
            fallbackSearch(query, numResults)
        }
    }

    /**
     * 备用搜索（当主搜索失败时使用 Google 搜索）
     * @param query 搜索关键词
     * @param numResults 返回结果数量（此参数在此函数中未使用）
     * @return 搜索结果字符串
     */
    private fun fallbackSearch(query: String, numResults: Int): String {
        return try {
            // URL 编码搜索关键词
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            // 构建 Google 搜索 URL
            val url = URL("https://www.google.com/search?q=$encodedQuery&hl=zh-CN")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000  // 8秒连接超时
            conn.readTimeout = 8000     // 8秒读取超时
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")

            // 如果响应不成功，返回错误信息
            if (conn.responseCode != 200) {
                return "抱歉，当前无法完成联网搜索。请检查网络连接后重试。"
            }

            // 读取 HTML 内容
            val html = conn.inputStream.bufferedReader().use { it.readText() }

            // 清理 HTML 内容，提取纯文本
            val text = html
                .replace("<script[^>]*>.*?</script>".toRegex(), " ")  // 移除脚本
                .replace("<style[^>]*>.*?</style>".toRegex(), " ")   // 移除样式
                .replace("<[^>]*>".toRegex(), " ")                   // 移除 HTML 标签
                .replace("&[a-z]+;".toRegex(), " ")                  // 移除 HTML 实体
                .replace("\\s+".toRegex(), " ")                      // 合并多余空格
                .trim()
                .take(5000)                                          // 限制长度为 5000 字符

            // 判断是否有有效结果
            if (text.length < 200) {
                "未找到相关搜索结果，请尝试更换关键词"
            } else {
                "搜索页面文本摘要（前5000字符）:\n$text\n\n请基于以上内容回答用户问题。"
            }
        } catch (e: Exception) {
            "搜索失败: ${e.message?.take(80)}"
        }
    }

    /**
     * 尝试执行搜索（内部辅助函数）
     * @param engine 搜索引擎名称
     * @param urlStr 搜索 URL
     * @param query 搜索关键词
     * @param numResults 返回结果数量
     * @return 搜索结果字符串，如果失败返回 null
     */
    private fun trySearch(engine: String, urlStr: String, query: String, numResults: Int): String? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")

            if (conn.responseCode != 200) return null

            val html = conn.inputStream.bufferedReader().use { it.readText() }

            if (html.length < 5000) return null

            val text = html.replace("<[^>]*>".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
                .take(3000)

            if (text.length < 100) return null

            "来自 $engine 的搜索结果摘要:\n$text\n\n请基于以上搜索结果回答用户的问题。"
        } catch (e: Exception) {
            null
        }
    }
}