package com.githubim.easysdk

import android.os.Looper
import com.githubim.easysdk.enums.WuKongErrorCode
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.DisconnectInfo
import com.githubim.easysdk.model.WuKongError
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.ref.Reference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class WuKongEasySDKPongTimeoutTest {

    @Before
    fun resetBeforeTest() {
        resetSdk()
    }

    @After
    fun resetAfterTest() {
        resetSdk()
    }

    @Test
    fun `pong timeout emits an abnormal disconnect`() {
        val server = authenticatedServer()
        val sdk = WuKongEasySDK.getInstance()
        val errorReceived = CountDownLatch(1)
        val disconnectReceived = CountDownLatch(1)
        val error = AtomicReference<WuKongError>()
        val disconnect = AtomicReference<DisconnectInfo>()
        val errorListener = object : WuKongEventListener<WuKongError> {
            override fun onEvent(data: WuKongError) {
                error.set(data)
                errorReceived.countDown()
            }
        }
        val disconnectListener = object : WuKongEventListener<DisconnectInfo> {
            override fun onEvent(data: DisconnectInfo) {
                disconnect.set(data)
                disconnectReceived.countDown()
            }
        }
        val listeners = listOf(errorListener, disconnectListener)
        val config = WuKongConfig.Builder()
            .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
            .uid("pong-timeout-test")
            .token("test-token")
            .pingInterval(60_000)
            .maxReconnectAttempts(0)
            .build()

        try {
            sdk.init(RuntimeEnvironment.getApplication(), config)
            sdk.addEventListener(WuKongEvent.ERROR, errorListener)
            sdk.addEventListener(WuKongEvent.DISCONNECT, disconnectListener)
            runBlocking { sdk.connect() }

            val timeoutMethod = WuKongEasySDK::class.java.getDeclaredMethod("handlePongTimeout")
            timeoutMethod.isAccessible = true
            timeoutMethod.invoke(sdk)

            assertTrue("timeout error was not emitted", awaitMainThreadEvent(errorReceived))
            assertTrue("timeout disconnect was not emitted", awaitMainThreadEvent(disconnectReceived))
            assertEquals(WuKongErrorCode.CONNECTION_TIMEOUT, error.get().code)
            assertEquals("Ping timeout", error.get().message)
            assertNotEquals("pong timeout was reported as a normal close", 1000, disconnect.get().code)
            assertEquals("Ping timeout", disconnect.get().reason)
            assertFalse("pong timeout was reported as a clean close", disconnect.get().wasClean)

        } finally {
            sdk.disconnect()
            server.shutdown()
            Reference.reachabilityFence(listeners)
        }
    }

    @Test
    fun `live heartbeat timeout emits one public error and disconnect`() {
        val server = authenticatedServer()
        val sdk = WuKongEasySDK.getInstance()
        val firstError = CountDownLatch(1)
        val firstDisconnect = CountDownLatch(1)
        val errorCount = AtomicInteger()
        val disconnectCount = AtomicInteger()
        val errorListener = object : WuKongEventListener<WuKongError> {
            override fun onEvent(data: WuKongError) {
                if (data.code == WuKongErrorCode.CONNECTION_TIMEOUT) {
                    errorCount.incrementAndGet()
                    firstError.countDown()
                }
            }
        }
        val disconnectListener = object : WuKongEventListener<DisconnectInfo> {
            override fun onEvent(data: DisconnectInfo) {
                disconnectCount.incrementAndGet()
                firstDisconnect.countDown()
            }
        }
        val listeners = listOf(errorListener, disconnectListener)
        val config = WuKongConfig.Builder()
            .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
            .uid("live-pong-timeout-test")
            .token("test-token")
            // This interval also controls OkHttp protocol pings. Keep it above
            // the JSON-RPC timeout so this test observes the SDK heartbeat path.
            .pingInterval(250)
            .pongTimeout(50)
            .maxReconnectAttempts(0)
            .build()

        try {
            sdk.init(RuntimeEnvironment.getApplication(), config)
            sdk.addEventListener(WuKongEvent.ERROR, errorListener)
            sdk.addEventListener(WuKongEvent.DISCONNECT, disconnectListener)
            runBlocking { sdk.connect() }

            assertTrue("heartbeat timeout error was not emitted", awaitMainThreadEvent(firstError))
            assertTrue(
                "heartbeat timeout disconnect was not emitted",
                awaitMainThreadEvent(firstDisconnect)
            )

            repeat(50) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }

            assertEquals("heartbeat timeout emitted duplicate errors", 1, errorCount.get())
            assertEquals("heartbeat timeout emitted duplicate disconnects", 1, disconnectCount.get())

        } finally {
            sdk.disconnect()
            server.shutdown()
            Reference.reachabilityFence(listeners)
        }
    }

    private fun authenticatedServer(): MockWebServer {
        return MockWebServer().apply {
            enqueue(
                MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
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
                        webSocket.close(code, reason)
                    }
                })
            )
            start()
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
}
