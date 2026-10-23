package com.githubim.easysdk.example.stream

import com.githubim.easysdk.model.Message
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Bounded chat projection shared by live SDK events and full history snapshots. */
class StreamTimeline(private val channel: String) {
    data class Row(
        val key: String,
        var sequence: Long = 0,
        val user: Boolean = false,
        var text: String = "",
        var status: String = "生成中",
        var finished: Boolean = false,
        val seen: MutableSet<String> = linkedSetOf()
    )
    private val entries = linkedMapOf<String, Row>()
    val rows: List<Row> get() = entries.values.sortedBy { if (it.sequence > 0) it.sequence else Long.MAX_VALUE }

    private fun row(key: String, sequence: Long = 0, user: Boolean = false): Row {
        val result = entries.getOrPut(key) { Row(key, user = user) }
        if (sequence > 0) result.sequence = sequence
        while (entries.size > 40) entries.remove(rows.first().key)
        return result
    }

    fun receive(message: Message) {
        if (message.channelId != channel || message.channelType != 2 || message.clientMsgNo.isNullOrEmpty()) return
        val item = row(message.clientMsgNo!!, message.messageSeq, message.setting?.stream != true)
        if (item.user) {
            item.text = payloadText(Gson().toJsonTree(message.payload))
            item.status = "已发送"; item.finished = true
        }
    }

    /** Deduplicate IDs and merge offsets in UTF-8 bytes, never Kotlin string positions. */
    fun apply(event: JsonObject) {
        val type = event.string("type")
        if (type !in setOf("stream.open", "stream.delta", "stream.snapshot", "stream.close", "stream.cancel", "stream.error", "stream.finish")) return
        val data = event.obj("data") ?: return
        if (data.string("channel_id") != channel || data.number("channel_type") != 2L) return
        if (data.string("event_key") != "main" && type != "stream.finish") return
        val key = data.string("client_msg_no"); val id = event.string("id")
        if (key.isEmpty() || id.isEmpty()) return
        val item = row(key, data.number("message_seq"))
        if (!item.seen.add(id)) return
        if (item.seen.size > 2048) item.seen.remove(item.seen.first())
        if (item.finished && type != "stream.finish") return
        val payload = data.obj("payload") ?: JsonObject()
        if (type == "stream.delta") {
            val delta = payload.string("delta").toByteArray(Charsets.UTF_8)
            val current = item.text.toByteArray(Charsets.UTF_8)
            val offset = if (data.has("text_offset")) data.number("text_offset") else current.size.toLong()
            if (offset < 0 || offset > current.size) item.status = "增量中断，等待快照"
            else if (offset + delta.size > current.size) {
                try {
                    val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    item.text += decoder.decode(ByteBuffer.wrap(delta, current.size - offset.toInt(), (offset + delta.size - current.size).toInt())).toString()
                } catch (_: Exception) { item.status = "增量中断，等待快照" }
            }
        }
        if (type in setOf("stream.snapshot", "stream.close", "stream.cancel", "stream.error", "stream.finish")) {
            val snapshot = payload.obj("snapshot") ?: payload
            if (snapshot.string("kind") == "text" && snapshot.has("text")) item.text = snapshot.string("text")
        }
        when (type) {
            "stream.cancel" -> { item.status = "已取消"; item.finished = true }
            "stream.error" -> { item.status = "失败"; item.finished = true }
            "stream.close", "stream.finish" -> {
                if (item.status !in setOf("已取消", "失败")) item.status = "完成"
                item.finished = true
            }
        }
        bound(item)
    }

    /** Apply one authoritative page before replaying buffered online events. */
    fun restore(history: JsonObject) {
        entries.clear()
        for (element in history.getAsJsonArray("messages") ?: return) {
            val message = element.asJsonObject
            val key = message.string("client_msg_no"); if (key.isEmpty()) continue
            val stream = (message.number("setting").toInt() and 2) != 0
            val item = row(key, message.number("message_seq"), !stream)
            if (!stream) { item.text = payloadText(message.get("payload")); item.status = "已发送"; item.finished = true; continue }
            val meta = message.obj("event_meta")
            val lane = meta?.getAsJsonArray("events")?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
                ?.find { it.string("event_key") == "main" }
            val snapshot = lane?.obj("snapshot")
            if (snapshot?.string("kind") == "text") item.text = snapshot.string("text")
            item.status = when {
                lane?.string("status") == "cancelled" -> "已取消"
                lane?.string("status") == "error" -> "失败"
                meta?.get("completed")?.asBoolean == true || lane?.string("status") == "closed" -> "完成"
                else -> "生成中"
            }
            item.finished = item.status != "生成中"
            bound(item)
        }
    }

    fun context(): List<Map<String, String>> {
        var chars = 0
        return rows.filter { it.user || it.status == "完成" }.takeLast(39).asReversed().filter {
            chars += it.text.length; chars <= 24000
        }.asReversed().map { mapOf("role" to if (it.user) "user" else "assistant", "content" to it.text) }
    }

    private fun bound(item: Row) {
        if (item.text.length > 16384) {
            item.text = item.text.take(16384).let { if (it.last().isHighSurrogate()) it.dropLast(1) else it }
            item.status = "回复超出演示上限"; item.finished = true
        }
    }

    private fun payloadText(value: JsonElement?): String {
        if (value == null || value.isJsonNull) return ""
        if (value.isJsonObject) return value.asJsonObject.string("content").take(16384)
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return ""
        val raw = value.asString
        val parsed = try { JsonParser.parseString(raw) } catch (_: Exception) {
            try { JsonParser.parseString(String(android.util.Base64.decode(raw, android.util.Base64.DEFAULT), Charsets.UTF_8)) } catch (_: Exception) { null }
        }
        return if (parsed?.isJsonObject == true) parsed.asJsonObject.string("content").take(16384) else raw.take(16384)
    }
}

internal fun JsonObject.string(key: String): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""
internal fun JsonObject.number(key: String): Long = try { get(key)?.asLong ?: 0L } catch (_: Exception) { 0L }
internal fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
