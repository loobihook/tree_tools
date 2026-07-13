package me.rpgz.treetools.api.qwen

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.scale


private fun bitmapToBase64Url(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 50
): String {
    require(format == Bitmap.CompressFormat.PNG || format == Bitmap.CompressFormat.JPEG) {
        "Only PNG and JPG formats are supported"
    }

    val scaledBitmap = if (bitmap.width > 500 || bitmap.height > 500) {
        val scale = minOf(500f / bitmap.width, 500f / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        bitmap.scale(newWidth, newHeight)
    } else {
        bitmap
    }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(format, quality, outputStream)
    val byteArray = outputStream.toByteArray()
    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

    val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"

    return "data:$mimeType;base64,$base64"
}

data class DashScopeResponse(
    val success: Boolean,
    val content: String?,
    val error: String?
)

interface StreamingCallback {
    fun onStart()
    fun onChunk(chunk: String)
    fun onComplete(fullContent: String)
    fun onError(error: String)
}

suspend fun callDashScopeAPI(
    apiKey: String,
    imageUrl: String,
    textPrompt: String
): DashScopeResponse = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    
    try {
        // Build the request JSON
        val messageContent = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", imageUrl)
                })
            })
            put(JSONObject().apply {
                put("type", "text")
                put("text", textPrompt)
            })
        }
        
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", messageContent)
            })
        }
        
        val requestBody = JSONObject().apply {
            put("model", "qwen-vl-plus")
            put("messages", messages)
        }
        
        val mediaType = "application/json".toMediaType()
        val body = requestBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content")
                    
                    DashScopeResponse(
                        success = true,
                        content = content,
                        error = null
                    )
                } else {
                    DashScopeResponse(
                        success = false,
                        content = null,
                        error = "No choices in response"
                    )
                }
            } else {
                DashScopeResponse(
                    success = false,
                    content = null,
                    error = "Empty response body"
                )
            }
        } else {
            val errorBody = response.body?.string()
            DashScopeResponse(
                success = false,
                content = null,
                error = "HTTP ${response.code}: ${response.message}${errorBody?.let { "\n$it" } ?: ""}"
            )
        }
    } catch (e: IOException) {
        DashScopeResponse(
            success = false,
            content = null,
            error = "Network error: ${e.message}"
        )
    } catch (e: Exception) {
        DashScopeResponse(
            success = false,
            content = null,
            error = "Error: ${e.message}"
        )
    }
}

suspend fun callDashScopeAPIStreaming(
    apiKey: String,
    imageUrl: String,
    textPrompt: String,
    callback: StreamingCallback
) = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    
    try {
        // Build the request JSON with streaming enabled
        val messageContent = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", imageUrl)
                })
            })
            put(JSONObject().apply {
                put("type", "text")
                put("text", textPrompt)
            })
        }
        
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", messageContent)
            })
        }
        
        val requestBody = JSONObject().apply {
            put("model", "qwen-vl-plus")
            put("messages", messages)
            put("stream", true) // Enable streaming
        }
        
        val mediaType = "application/json".toMediaType()
        val body = requestBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()
        
        val fullContent = StringBuilder()
        
        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                callback.onStart()
            }
            
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                
                // Check for end of stream
                if (data.trim() == "[DONE]") {
                    callback.onComplete(fullContent.toString())
                    eventSource.cancel()
                    return
                }
                
                try {
                    val eventJson = JSONObject(data)
                    val choices = eventJson.getJSONArray("choices")
                    
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val delta = firstChoice.getJSONObject("delta")
                        
                        if (delta.has("content")) {
                            val content = delta.getString("content")
                            fullContent.append(content)
                            callback.onChunk(content)
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed JSON chunks
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                callback.onComplete(fullContent.toString())
            }
            
            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                super.onFailure(eventSource, t, response)
                val errorMessage = when {
                    response != null -> {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    t != null -> "Error: ${t.message}"
                    else -> "Unknown error occurred"
                }
                callback.onError(errorMessage)
            }
        }
        
        val eventSource = EventSources.createFactory(client).newEventSource(request, eventSourceListener)
        
        // Keep the coroutine alive until the EventSource is closed
        // In a real implementation, you might want to add a timeout or cancellation mechanism
        
    } catch (e: IOException) {
        callback.onError("Network error: ${e.message}")
    } catch (e: Exception) {
        callback.onError("Error: ${e.message}")
    }
}

fun generateTreeReportPrompt(
    treeSpecies: String?,
    treeHeight: Float?,
    treeDiameter: Float?,
    soilTemperature: Double?,     // 土壤温度
    soilMoisture: Double?,        // 土壤湿度
    airCO2: Double?,              // 空气二氧化碳含量
    ambientTemperature: Double?,  // 环境温度
    ambientHumidity: Double?      // 环境湿度
): String {
    return buildString {
        appendLine("# 树木健康分析报告生成")
        appendLine()
        appendLine("请根据提给给你的图片和环境数据，生成一份详细的树木健康分析报告：")
        appendLine()

        appendLine("## 基本信息")

        treeSpecies?.let {
            appendLine("**树种**: $it")
        } ?: appendLine("**树种**: 自行判断")


        treeHeight?.let {
            appendLine("**树高**: ${it}米")
        } ?: appendLine("**树高**: 未测量")

        treeDiameter?.let {
            appendLine("**直径**: ${it}厘米")
        } ?: appendLine("**直径**: 未测量")

        appendLine()
        appendLine("## 环境数据")

        soilTemperature?.let {
            appendLine("**土壤温度**: ${it}°C")
        } ?: appendLine("**土壤温度**: 未测量")

        soilMoisture?.let {
            appendLine("**土壤湿度**: ${it}%")
        } ?: appendLine("**土壤湿度**: 未测量")

        airCO2?.let {
            appendLine("**空气CO₂含量**: ${it}ppm")
        } ?: appendLine("**空气CO₂含量**: 未测量")

        ambientTemperature?.let {
            appendLine("**环境温度**: ${it}°C")
        } ?: appendLine("**环境温度**: 未测量")

        ambientHumidity?.let {
            appendLine("**环境湿度**: ${it}%")
        } ?: appendLine("**环境湿度**: 未测量")

        appendLine()
        appendLine("## 分析要求")
        appendLine("请基于上述信息，从以下几个方面进行分析：")
        appendLine()
        appendLine("1. **生长状态评估**")
        appendLine("   - 根据树高和直径数据评估树木的生长情况")
        appendLine("   - 与该树种的标准生长指标进行对比")
        appendLine()
        appendLine("2. **环境适应性分析**")
        appendLine("   - 分析当前环境条件是否适合该树种生长")
        appendLine("   - 评估土壤和气候条件对树木健康的影响")
        appendLine()
        appendLine("3. **健康风险识别**")
        appendLine("   - 识别可能存在的健康风险因子")
        appendLine("   - 预测潜在的病虫害或环境胁迫")
        appendLine()
        appendLine("4. **养护建议**")
        appendLine("   - 提供具体的养护措施建议")
        appendLine("   - 建议最佳的浇水、施肥、修剪时机")
        appendLine()
        appendLine("请以专业、详细且易于理解的方式撰写报告，确保内容科学准确。")
    }
}

suspend fun genTreeReport(
    leafImage: Bitmap,
    treeSpecies: String?,
    treeHeight: Float?,
    treeDiameter: Float?,
    soilTemperature: Double?,     // 土壤温度
    soilMoisture: Double?,        // 土壤湿度
    airCO2: Double?,              // 空气二氧化碳含量
    ambientTemperature: Double?,  // 环境温度
    ambientHumidity: Double?,      // 环境湿度
    apiKey: String
): DashScopeResponse {
    val textPrompt = generateTreeReportPrompt(
        treeSpecies = treeSpecies,
        treeHeight = treeHeight,
        treeDiameter = treeDiameter,
        soilTemperature = soilTemperature,
        soilMoisture = soilMoisture,
        airCO2 = airCO2,
        ambientTemperature = ambientTemperature,
        ambientHumidity = ambientHumidity
    )
    
    val imageUrl = bitmapToBase64Url(leafImage)
    
    return callDashScopeAPI(
        apiKey = apiKey,
        imageUrl = imageUrl,
        textPrompt = textPrompt
    )
}

suspend fun genTreeReportStreaming(
    leafImage: Bitmap,
    treeSpecies: String?,
    treeHeight: Float?,
    treeDiameter: Float?,
    soilTemperature: Double?,     // 土壤温度
    soilMoisture: Double?,        // 土壤湿度
    airCO2: Double?,              // 空气二氧化碳含量
    ambientTemperature: Double?,  // 环境温度
    ambientHumidity: Double?,      // 环境湿度
    apiKey: String,
    callback: StreamingCallback
) {
    val textPrompt = generateTreeReportPrompt(
        treeSpecies = treeSpecies,
        treeHeight = treeHeight,
        treeDiameter = treeDiameter,
        soilTemperature = soilTemperature,
        soilMoisture = soilMoisture,
        airCO2 = airCO2,
        ambientTemperature = ambientTemperature,
        ambientHumidity = ambientHumidity
    )
    
    val imageUrl = bitmapToBase64Url(leafImage)
    
    callDashScopeAPIStreaming(
        apiKey = apiKey,
        imageUrl = imageUrl,
        textPrompt = textPrompt,
        callback = callback
    )
}