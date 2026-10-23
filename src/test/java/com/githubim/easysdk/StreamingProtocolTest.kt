package com.githubim.easysdk

import android.os.Looper
import com.githubim.easysdk.enums.WuKongChannelType
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.EventNotification
import com.githubim.easysdk.model.Message
import com.githubim.easysdk.model.MessageSetting
import com.githubim.easysdk.model.SendOptions
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class StreamingProtocolTest {
    @Test fun `independent peers send stream settings and custom events need no ack`() {
        val requests = LinkedBlockingQueue<JsonObject>()
        val sockets = LinkedBlockingQueue<WebSocket>()
        val server = MockWebServer()
        repeat(2) {
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { sockets.add(webSocket) }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val request = JsonParser.parseString(text).asJsonObject
                    requests.add(request)
                    val id = request.get("id") ?: return
                    val result = if (request["method"].asString == "send")
                        """{"messageId":"123","messageSeq":9,"reasonCode":1}"""
                    else """{"reason_code":1}"""
                    webSocket.send("""{"jsonrpc":"2.0","id":$id,"result":$result}""")
                }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, reason) }
            }))
        }
        server.start()
        val reader = WuKongEasySDK.createInstance()
        val writer = WuKongEasySDK.createInstance()
        val events = mutableListOf<EventNotification>()
        val listener = object : WuKongEventListener<EventNotification> {
            override fun onEvent(data: EventNotification) { events.add(data) }
        }
        try {
            for ((index, sdk) in listOf(reader, writer).withIndex()) {
                sdk.init(RuntimeEnvironment.getApplication(), WuKongConfig.Builder()
                    .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
                    .uid("peer-$index").token("private-token").pingInterval(60_000).maxReconnectAttempts(0).build())
                runBlocking { sdk.connect() }
            }
            assertNotSame(reader, writer)
            reader.addEventListener(WuKongEvent.CUSTOM_EVENT, listener)
            val result = runBlocking { writer.send("room", WuKongChannelType.GROUP, mapOf("type" to 1, "content" to ""),
                SendOptions(clientMsgNo = "reply-key", setting = MessageSetting(stream = true))) }
            assertEquals("123", result.messageId)
            assertEquals(9L, result.messageSeq)
            assertEquals(1, result.reasonCode)
            val sent = generateSequence { requests.poll() }.first { it["method"].asString == "send" }["params"].asJsonObject
            assertEquals("reply-key", sent["client_msg_no"].asString)
            assertTrue(sent["setting"].asJsonObject["stream"].asBoolean)
            val socket = sockets.poll(2, TimeUnit.SECONDS)!!
            val data = """{"channel_id":"room","delta":"你好🙂"}"""
            socket.send("""{"jsonrpc":"2.0","method":"event","params":{"id":"e1","type":"stream.delta","timestamp":1,"data":$data}}""")
            socket.send("""{"jsonrpc":"2.0","method":"event","params":{"id":"e2","type":"stream.delta","timestamp":2,"data":${Gson().toJson(data)}}}""")
            socket.send("""{"jsonrpc":"2.0","method":"event","params":{"id":"bad","type":"stream.delta","data":{}}}""")
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (events.size < 2 && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertEquals(listOf("e1", "e2"), events.map { it.id })
            assertEquals("你好🙂", (events[1].data as Map<*, *>)["delta"])
            assertFalse(events[0].toString().contains("你好"))
            assertNull("Custom events must not send recvack", requests.poll(100, TimeUnit.MILLISECONDS))
            reader.destroy()
            assertTrue("Destroying reader must not disconnect writer", writer.isConnected())
        } finally {
            reader.destroy(); writer.destroy(); server.shutdown()
        }
    }

    @Test fun `stream flags decode alongside existing message fields`() {
        val message = Gson().fromJson("""{"header":{},"message_id":"123","message_seq":8,"timestamp":1,"channel_id":"room","channel_type":2,"from_uid":"assistant","client_msg_no":"reply-key","payload":{},"setting":{"stream":true}}""", Message::class.java)
        assertTrue(message.setting!!.stream)
        assertEquals("reply-key", message.clientMsgNo)
    }
}
