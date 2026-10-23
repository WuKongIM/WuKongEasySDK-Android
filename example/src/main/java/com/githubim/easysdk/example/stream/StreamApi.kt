package com.githubim.easysdk.example.stream

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Demo-only direct Product API access. A real app delegates writes to its backend. */
internal class StreamApi(val base: String) {
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).followRedirects(false).build()
    private val gson = Gson()

    init {
        val url = base.toHttpUrl()
        require(url.username.isEmpty() && url.password.isEmpty() && url.fragment == null && url.query == null) { "请输入 HTTP(S) API 地址" }
    }

    suspend fun post(path: String, body: Any): JsonObject = json(Request.Builder().url(base.trimEnd('/') + path)
        .post(gson.toJson(body).toRequestBody("application/json".toMediaType())).build())

    suspend fun route(uid: String): String {
        val url = (base.trimEnd('/') + "/route").toHttpUrl().newBuilder().addQueryParameter("uid", uid).build()
        val result = json(Request.Builder().url(url).build())
        return result.string("wss_addr").ifEmpty { result.string("ws_addr") }.also {
            require(it.startsWith("ws://") || it.startsWith("wss://")) { "路由未返回可用 WebSocket 地址" }
        }
    }

    private suspend fun json(request: Request): JsonObject = withContext(Dispatchers.IO) {
        client.newCall(request).await().use { response ->
            check(response.isSuccessful) { "服务端 HTTP ${response.code}，请检查连接设置" }
            val body = response.body ?: error("服务端响应为空")
            check(body.contentLength() <= 2 * 1024 * 1024) { "响应超过演示上限" }
            val source = body.source()
            check(!source.request(2 * 1024 * 1024 + 1L)) { "响应超过演示上限" }
            val value = JsonParser.parseString(source.readUtf8()).asJsonObject
            check(!value.has("status") || value.number("status") == 200L) { "服务端拒绝请求，请检查账号、频道及权限" }
            value
        }
    }
    fun close() { client.dispatcher.cancelAll(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(IOException("网络请求失败，请检查地址与网络"))
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { response.close() }
        }
    })
}
