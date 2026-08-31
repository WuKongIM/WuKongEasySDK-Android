package com.githubim.easysdk

import android.os.Looper
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.DisconnectInfo
import com.google.gson.JsonParser
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import java.lang.ref.Reference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class WuKongEasySDKLoggingTest {

    @Before
    fun clearLogs() {
        resetSdk()
        ShadowLog.clear()
    }

    @After
    fun resetSdkAndLogs() {
        resetSdk()
        ShadowLog.clear()
    }

    @Test
    fun `disabled logging does not expose malformed recv params`() {
        val output = exerciseSdkLogging(debugLogging = false)

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
        assertTrue("disabled debug logging should emit no SDK logs: $output", output.isBlank())
    }

    @Test
    fun `enabled logging retains SDK diagnostics without sensitive content`() {
        val output = exerciseSdkLogging(debugLogging = true)

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
        assertTrue("debug logging should retain SDK diagnostics", output.contains("WuKongEasySDK"))
        assertTrue("RECV failures should retain a safe diagnostic", output.contains("Failed to parse recv message"))
    }

    private fun exerciseSdkLogging(debugLogging: Boolean): String {
        val serverSocket = AtomicReference<WebSocket>()
        val serverClosed = CountDownLatch(1)
        val server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSocket.set(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val request = JsonParser.parseString(text).asJsonObject
                    if (request.get("method")?.asString == "connect") {
                        val id = request.get("id").asString
                        webSocket.send(
                            """{"jsonrpc":"2.0","id":"$id","result":{"server_key":"test","salt":"test","time_diff":0,"reason_code":1}}"""
                        )
                    }
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

        val sdk = WuKongEasySDK.getInstance()
        val disconnectReceived = CountDownLatch(1)
        val disconnectListener = object : WuKongEventListener<DisconnectInfo> {
            override fun onEvent(data: DisconnectInfo) {
                disconnectReceived.countDown()
                throw IllegalArgumentException("listener rejected $PAYLOAD_CANARY")
            }
        }
        val config = WuKongConfig.Builder()
            .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
            .uid("logging-test")
            .token(TOKEN_CANARY)
            .pingInterval(60_000)
            .maxReconnectAttempts(0)
            .debugLogging(debugLogging)
            .build()

        try {
            sdk.init(RuntimeEnvironment.getApplication(), config)
            sdk.addEventListener(WuKongEvent.DISCONNECT, disconnectListener)
            runBlocking { sdk.connect() }

            serverSocket.get().send(
                """{"jsonrpc":"2.0","method":"recv","params":{"header":"invalid","token":"$TOKEN_CANARY","payload":"$PAYLOAD_CANARY"}}"""
            )
            serverSocket.get().send(
                """{"jsonrpc":"2.0","method":"$TOKEN_CANARY","params":{"payload":"$PAYLOAD_CANARY"}}"""
            )
            serverSocket.get().send(
                """{"jsonrpc":"2.0","method":"disconnect","params":{"code":1000,"reason":"test complete","was_clean":true}}"""
            )

            assertTrue(
                "SDK did not process the marker notification after malformed RECV",
                awaitMainThreadEvent(disconnectReceived)
            )
            Reference.reachabilityFence(disconnectListener)

            return capturedLogs()
        } finally {
            sdk.disconnect()
            serverClosed.await(2, TimeUnit.SECONDS)
            server.shutdown()
        }
    }

    private fun resetSdk() {
        val instanceField = WuKongEasySDK::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private fun awaitMainThreadEvent(latch: CountDownLatch): Boolean {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (latch.await(10, TimeUnit.MILLISECONDS)) {
                return true
            }
        }
        return false
    }

    private fun capturedLogs(): String = ShadowLog.getLogs()
        .joinToString("\n") { entry ->
            listOfNotNull(entry.tag, entry.msg, entry.throwable?.stackTraceToString())
                .joinToString(" ")
        }

    private companion object {
        const val TOKEN_CANARY = "TOKEN_CANARY_259dcb72b5"
        const val PAYLOAD_CANARY = "PAYLOAD_CANARY_d7286fb9f8"
    }
}
