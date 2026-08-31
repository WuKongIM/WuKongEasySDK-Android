package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WebSocketManagerLoggingTest {

    @Before
    fun clearLogs() {
        ShadowLog.clear()
    }

    @After
    fun clearLogsAfterTest() {
        ShadowLog.clear()
    }

    @Test
    fun `disabled logging does not expose websocket content`() {
        val output = exerciseWebSocketLogging(debugLogging = false)

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
    }

    @Test
    fun `enabled logging retains websocket diagnostics without sensitive content`() {
        val output = exerciseWebSocketLogging(debugLogging = true)

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
        assertTrue("debug logging should retain WebSocket diagnostics", output.contains("WebSocketManager"))
    }

    private fun exerciseWebSocketLogging(debugLogging: Boolean): String {
        val serverSocket = AtomicReference<WebSocket>()
        val serverOpened = CountDownLatch(1)
        val serverClosed = CountDownLatch(1)
        val clientReceived = CountDownLatch(1)
        val server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSocket.set(webSocket)
                    serverOpened.countDown()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, "test complete")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    serverClosed.countDown()
                }
            })
        )
        server.start()

        val config = WuKongConfig.Builder()
            .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
            .uid("logging-test")
            .token(TOKEN_CANARY)
            .debugLogging(debugLogging)
            .build()
        val manager = WebSocketManager(config)
        manager.onMessageReceived = { clientReceived.countDown() }

        try {
            runBlocking { manager.connect() }
            assertTrue("server WebSocket did not open", serverOpened.await(2, TimeUnit.SECONDS))

            serverSocket.get().send(
                """{"token":"$TOKEN_CANARY","payload":"$PAYLOAD_CANARY"}"""
            )
            assertTrue("client did not receive test message", clientReceived.await(2, TimeUnit.SECONDS))

            return capturedLogs()
        } finally {
            manager.disconnect()
            serverClosed.await(2, TimeUnit.SECONDS)
            server.shutdown()
        }
    }

    private fun capturedLogs(): String = ShadowLog.getLogs()
        .joinToString("\n") { entry ->
            listOfNotNull(entry.tag, entry.msg, entry.throwable?.stackTraceToString())
                .joinToString(" ")
        }

    private companion object {
        const val TOKEN_CANARY = "TOKEN_CANARY_b6af8d60de"
        const val PAYLOAD_CANARY = "PAYLOAD_CANARY_bfc3a058ce"
    }
}
