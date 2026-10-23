package com.githubim.easysdk.example.stream

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.WuKongEasySDK
import com.githubim.easysdk.enums.WuKongChannelType
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

/** Owns two independent peers and one sequential producer for this screen session. */
class StreamController(application: Application) : AndroidViewModel(application) {
    private data class Session(val reader: String, val writer: String, val channel: String, val token: String) {
        override fun toString() = "Session([redacted])"
    }
    private var session: Session? = null
    private var api: StreamApi? = null
    private var reader: WuKongEasySDK? = null
    private var writer: WuKongEasySDK? = null
    // SDK listeners are weak references; keep ownership for the lifetime of each peer.
    private val listeners = mutableMapOf<WuKongEasySDK, MutableList<Any>>()
    private var restoring = false
    private val buffered = mutableListOf<JsonObject>()
    private var recovery: Job? = null
    private var recoveryEpoch = 0
    private var run: Run? = null
    private val gson = Gson()
    private val logs = ArrayDeque<String>()
    var onChange: (() -> Unit)? = null
    var timeline: StreamTimeline? = null; private set
    var connecting = false; private set
    var sending = false; private set
    var error = ""; private set
    var generator = "就绪"; private set
    var userOnline = false; private set
    var writerOnline = false; private set
    private var recoveryFailed = false
    val busy: Boolean get() = connecting || sending || restoring
    val generating: Boolean get() = run != null
    val canSend: Boolean get() = userOnline && writerOnline && !busy && !generating && !recoveryFailed
    val hasSession: Boolean get() = session != null
    val status: String get() = when {
        connecting -> "连接中"
        restoring -> "恢复历史中"
        recoveryFailed -> "历史恢复失败，请重试"
        userOnline && writerOnline -> "在线"
        !userOnline && writerOnline -> "接收端离线"
        session != null -> "离线"
        else -> "未连接"
    }
    val logText: String get() = logs.joinToString("\n")

    private class EventWriteException : Exception()

    private class Run(val key: String) {
        var text = ""
        var index = 0
        var stop = ""
        var model: ModelStream? = null
    }

    fun connect(base: String) {
        if (busy || generating) return
        viewModelScope.launch {
            connecting = true; error = ""; changed()
            try {
                if (session == null) {
                    api?.close(); api = StreamApi(base.trim().trimEnd('/'))
                    val id = UUID.randomUUID().toString().take(8)
                    val candidate = Session("android-reader-$id", "android-writer-$id", "android-stream-$id", UUID.randomUUID().toString())
                    for (uid in listOf(candidate.reader, candidate.writer)) api!!.post("/user/token", mapOf(
                        "uid" to uid, "token" to candidate.token, "device_flag" to 0, "device_level" to 0))
                    api!!.post("/channel", mapOf("channel_id" to candidate.channel, "channel_type" to 2,
                        "subscribers" to listOf(candidate.reader, candidate.writer)))
                    session = candidate; timeline = StreamTimeline(candidate.channel)
                    log("HTTP 演示账号及群频道已创建")
                }
                connectPeers()
                if (userOnline && recoveryFailed) recover()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { closePeers(); error = "连接失败，请检查 API、外部 WebSocket 地址与演示账号写入权限，再重试连接。" }
            finally { connecting = false; changed() }
        }
    }

    private suspend fun connectPeers() {
        val current = session ?: return
        val ws = api!!.route(current.reader)
        if (reader?.isConnected() != true) {
            reader?.let { discard(it) }
            reader = peer(ws, current.reader, true)
            withTimeout(15000) { reader!!.connect() }
        }
        if (writer?.isConnected() != true) {
            writer?.let { discard(it) }
            writer = peer(ws, current.writer, false)
            withTimeout(15000) { writer!!.connect() }
        }
    }

    private fun peer(ws: String, uid: String, receiving: Boolean): WuKongEasySDK {
        val sdk = WuKongEasySDK.createInstance()
        sdk.init(getApplication(), WuKongConfig.Builder().serverUrl(ws).uid(uid).token(session!!.token).build())
        fun current() = if (receiving) reader === sdk else writer === sdk
        listen<ConnectResult>(sdk, WuKongEvent.CONNECT) {
            if (current()) {
                if (receiving) { userOnline = true; recover() } else writerOnline = true
                log(if (receiving) "SDK 接收端已连接" else "SDK 生成端已连接")
            }
        }
        listen<DisconnectInfo>(sdk, WuKongEvent.DISCONNECT) {
            if (current()) {
                if (receiving) { userOnline = false; cancelRecovery() } else writerOnline = false
                log(if (receiving) "SDK 接收端已断开" else "SDK 生成端已断开")
            }
        }
        listen<WuKongError>(sdk, WuKongEvent.ERROR) {
            if (current()) {
                if (receiving && !sdk.isConnected()) { userOnline = false; cancelRecovery() }
                if (!receiving && !sdk.isConnected()) writerOnline = false
                error = "SDK 连接或协议异常，请检查网络后重试。"; log("SDK 错误：${it.code.name}")
            }
        }
        if (receiving) {
            listen<Message>(sdk, WuKongEvent.MESSAGE) { if (current()) { timeline?.receive(it); changed() } }
            listen<EventNotification>(sdk, WuKongEvent.CUSTOM_EVENT) {
                if (current()) {
                    val event = gson.toJsonTree(it).asJsonObject
                    log("SDK ← ${it.type.take(48)}")
                    if (restoring) {
                        if (buffered.size < 2048) buffered.add(event)
                        else { recoveryFailed = true; error = "恢复期间事件过多，请重连接收端。" }
                    } else timeline?.apply(event)
                    changed()
                }
            }
        }
        return sdk
    }

    private fun <T> listen(sdk: WuKongEasySDK, event: WuKongEvent, block: (T) -> Unit) {
        val listener = object : WuKongEventListener<T> { override fun onEvent(data: T) { block(data) } }
        listeners.getOrPut(sdk) { mutableListOf() }.add(listener)
        sdk.addEventListener(event, listener)
    }

    /** One full snapshot per successful connection, with bounded live buffering. */
    private fun recover() {
        cancelRecovery()
        val epoch = recoveryEpoch
        restoring = true; recoveryFailed = false; buffered.clear(); changed()
        recovery = viewModelScope.launch {
            try {
                val current = session ?: return@launch
                val history = api!!.post("/channel/messagesync", mapOf("login_uid" to current.reader,
                    "channel_id" to current.channel, "channel_type" to 2, "limit" to 40, "event_summary_mode" to "full"))
                if (epoch != recoveryEpoch) return@launch
                timeline?.restore(history)
                buffered.forEach { timeline?.apply(it) }; buffered.clear()
                log("HTTP 历史快照已恢复")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (epoch == recoveryEpoch) { recoveryFailed = true; error = "历史恢复失败，请重连接收端。" } }
            finally { if (epoch == recoveryEpoch) { restoring = false; changed() } }
        }
    }
    private fun cancelRecovery() { recoveryEpoch++; recovery?.cancel(); recovery = null; restoring = false; buffered.clear() }

    fun toggleReceiver() {
        if (busy || session == null) return
        if (userOnline) {
            reader?.disconnect(); userOnline = false; cancelRecovery(); changed()
        } else viewModelScope.launch {
            connecting = true; error = ""; changed()
            try { connectPeers() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { reader?.disconnect(); userOnline = false; error = "重连失败，请检查网络后重试。" }
            finally { connecting = false; changed() }
        }
    }

    fun reset() {
        if (busy || generating) return
        closePeers(); api?.close(); api = null; session = null; timeline = null
        error = ""; generator = "就绪"; logs.clear(); changed()
    }

    internal fun send(question: String, reply: String, chunk: Int, interval: Long, model: ModelConfig?) {
        if (!canSend || question.isBlank()) return
        viewModelScope.launch {
            sending = true; error = ""; changed()
            try {
                val current = session ?: return@launch
                val key = UUID.randomUUID().toString()
                val payload = mapOf("type" to 1, "content" to question.take(4000))
                val ack = reader!!.send(current.channel, WuKongChannelType.GROUP, payload, SendOptions(clientMsgNo = key))
                check(ack.reasonCode == 1) { "Question rejected" }
                timeline?.receive(Message(Header(), ack.messageId, ack.messageSeq, 0, current.channel, 2, current.reader, payload, key))
                log("SDK 问题已发送")
                generate(reply, chunk, interval, model)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "发送失败，结果未确认；请重连查看历史后再决定是否重发。" }
            finally { sending = false; changed() }
        }
    }

    internal fun generate(reply: String, chunk: Int, interval: Long, model: ModelConfig? = null) {
        if (!writerOnline || generating || session == null) return
        val active = Run(UUID.randomUUID().toString()); run = active
        generator = "生成中"; error = ""; changed()
        viewModelScope.launch {
            try {
                val current = session!!
                val ack = writer!!.send(current.channel, WuKongChannelType.GROUP, mapOf("type" to 1, "content" to ""),
                    SendOptions(clientMsgNo = active.key, setting = MessageSetting(stream = true)))
                check(ack.reasonCode == 1) { "Base message rejected" }
                append(active, "stream.open", mapOf("kind" to "text"))
                if (model != null) {
                    active.model = ModelStream()
                    try {
                        if (active.stop.isEmpty()) active.model!!.consume(model, timeline!!.context()) { delta ->
                            withContext(Dispatchers.Main) {
                                if (active.stop.isEmpty()) {
                                    append(active, "stream.delta", mapOf("kind" to "text", "delta" to delta))
                                    active.text += delta
                                }
                            }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { if (e is EventWriteException) throw e; if (active.stop.isEmpty()) { active.stop = "error"; error = "模型请求失败或流意外中断，已保存部分内容。请检查模型配置与额度。" } }
                } else {
                    val text = reply.ifBlank { DEFAULT_REPLY }.take(16384)
                    var offset = 0
                    while (offset < text.length && active.stop.isEmpty()) {
                        val end = text.offsetByCodePoints(offset, minOf(chunk.coerceIn(1, 64), text.codePointCount(offset, text.length)))
                        val delta = text.substring(offset, end)
                        append(active, "stream.delta", mapOf("kind" to "text", "delta" to delta))
                        active.text += delta; offset = end
                        delay(interval.coerceIn(40, 2000))
                    }
                }
                val snapshot = mapOf("kind" to "text", "text" to active.text)
                if (active.stop.isNotEmpty()) append(active, "stream.${active.stop}", mapOf("snapshot" to snapshot))
                append(active, "stream.finish", mapOf("snapshot" to snapshot))
                generator = when (active.stop) { "cancel" -> "已取消"; "error" -> "失败"; else -> "完成" }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { generator = "写入结果未确认"; error = "流事件写入失败，结果未确认；请重连恢复历史。" }
            finally { active.model?.cancel(); run = null; changed() }
        }
    }

    private suspend fun append(active: Run, type: String, payload: Any) {
        val current = session ?: error("No session")
        try { api!!.post("/message/event", mapOf("channel_id" to current.channel, "channel_type" to 2,
            "from_uid" to current.writer, "client_msg_no" to active.key, "event_id" to "${active.key}:${++active.index}",
            "event_type" to type, "event_key" to "main", "payload" to payload)) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { throw EventWriteException() }
        log("HTTP → $type 已确认")
    }

    fun stop(failed: Boolean = false) {
        val active = run ?: return
        if (active.stop.isEmpty()) active.stop = if (failed) "error" else "cancel"
        active.model?.cancel(); changed()
    }
    private fun discard(sdk: WuKongEasySDK) { sdk.destroy(); listeners.remove(sdk) }
    private fun closePeers() {
        cancelRecovery(); reader?.let { discard(it) }; writer?.let { discard(it) }
        reader = null; writer = null; userOnline = false; writerOnline = false
    }
    private fun log(value: String) { if (logs.size == 100) logs.removeFirst(); logs.addLast(value); changed() }
    private fun changed() { onChange?.invoke() }
    override fun onCleared() { onChange = null; run?.model?.cancel(); closePeers(); api?.close(); super.onCleared() }

    companion object {
        const val DEFAULT_REPLY = "WuKongIM 会先创建一条流消息，再把回复逐段送达。\n\n你现在看到的文字通过 Android EasySDK 实时接收。中文和 emoji 🙂 也按正确的字节偏移合并。\n\n试试取消生成，或者断开接收端后重新连接，观察历史快照恢复完整内容。"
    }
}
