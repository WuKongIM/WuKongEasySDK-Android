package com.githubim.easysdk.example.stream

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.TimeUnit

internal data class ModelConfig(val url: String, val key: String, val model: String) {
    override fun toString() = "ModelConfig(url=[redacted], key=[redacted], model=[redacted])"
}

/** Bounded OpenAI-compatible Chat Completions SSE; keys never enter IM events. */
internal class ModelStream {
    private val client = OkHttpClient.Builder().callTimeout(180, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    @Volatile private var call: Call? = null
    @Volatile private var cancelled = false

    fun cancel() { cancelled = true; call?.cancel() }

    suspend fun consume(config: ModelConfig, context: List<Map<String, String>>, emit: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            check(config.model.isNotBlank()) { "请填写模型名称" }
            val original = config.url.toHttpUrl()
            require(original.username.isEmpty() && original.password.isEmpty() && original.fragment == null) { "模型 URL 必须为 HTTP(S)，且不含用户名、密码或片段" }
            var path = original.encodedPath.trimEnd('/')
            if (!path.endsWith("/chat/completions")) path = "${path.ifEmpty { "/v1" }}/chat/completions"
            val url = original.newBuilder().encodedPath(path).build()
            val body = Gson().toJson(mapOf("model" to config.model, "messages" to context, "stream" to true))
            check(body.toByteArray().size <= 128 * 1024) { "模型请求超过演示上限" }
            val request = Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType()))
                .apply { if (config.key.isNotEmpty()) header("Authorization", "Bearer ${config.key}") }.build()
            val pending = client.newCall(request); call = pending
            if (cancelled) pending.cancel()
            pending.await().use { response ->
                check(response.isSuccessful) { "模型请求失败（HTTP ${response.code}）" }
                check(response.header("Content-Type")?.contains("text/event-stream") == true) { "模型未返回 SSE 流" }
                val source = response.body?.source() ?: error("模型响应为空")
                val line = ByteArrayOutputStream()
                val data = mutableListOf<String>()
                var total = 0; var frame = 0; var textSize = 0
                var skipLF = false; var received = false; var finished = false
                suspend fun dispatch(): Boolean {
                    val raw = data.joinToString("\n"); data.clear(); frame = 0
                    if (raw.isEmpty()) return false
                    if (raw == "[DONE]") { check(received) { "模型没有返回文本" }; return true }
                    val packet = try { JsonParser.parseString(raw).asJsonObject } catch (_: Exception) { error("模型 SSE JSON 无效") }
                    check(!packet.has("error")) { "模型返回错误" }
                    val choices = packet.getAsJsonArray("choices") ?: return false
                    val choice = choices.map { it.asJsonObject }.find { it.number("index") == 0L } ?: return false
                    val delta = choice.obj("delta")
                    val content = delta?.string("content").orEmpty().ifEmpty { delta?.string("refusal").orEmpty() }
                    if (content.isNotEmpty()) {
                        textSize += content.length; check(textSize <= 16384) { "回复超过 16,384 字符的演示上限" }
                        received = true; emit(content)
                    }
                    val reason = choice.string("finish_reason")
                    if (reason.isNotEmpty()) { check(reason in setOf("stop", "length")) { "模型未完成文本生成" }; finished = true }
                    return false
                }
                suspend fun endLine(): Boolean {
                    val value = try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(line.toByteArray())).toString() }
                        catch (_: Exception) { error("模型 SSE 文本编码无效") }
                    line.reset()
                    if (value.isEmpty()) return dispatch()
                    if (value.startsWith("data:")) {
                        val part = value.substring(5).removePrefix(" ")
                        frame += part.toByteArray().size; check(frame <= 65536) { "模型 SSE 帧超过上限" }; data.add(part)
                    }
                    return false
                }
                while (!source.exhausted()) {
                    check(!cancelled) { "生成已取消" }
                    val b = source.readByte().toInt() and 255
                    total++; check(total <= 4 * 1024 * 1024) { "模型流超过演示上限" }
                    if (skipLF && b == 10) { skipLF = false; continue }; skipLF = false
                    if (b == 10 || b == 13) { if (endLine()) return@use; skipLF = b == 13 }
                    else { check(line.size() < 65536) { "模型 SSE 行超过上限" }; line.write(b) }
                }
                check(finished && received) { "模型流意外中断，回复未完成" }
            }
        } finally {
            call = null; client.dispatcher.cancelAll(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown()
        }
    }
}
