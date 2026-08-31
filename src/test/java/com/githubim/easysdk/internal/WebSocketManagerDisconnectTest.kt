package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WebSocketManagerDisconnectTest {

    @Test
    fun `manual disconnect emits connection closed callback exactly once`() {
        val serverClosed = CountDownLatch(1)
        val server = MockWebServer()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    serverClosed.countDown()
                }
            })
        )
        server.start()

        val config = WuKongConfig.Builder()
            .serverUrl(server.url("/").toString().replaceFirst("http", "ws"))
            .uid("disconnect-test")
            .token("test-token")
            .build()
        val manager = WebSocketManager(config)
        val callbackCount = AtomicInteger()
        val callbackCode = AtomicInteger()
        val callbackReason = AtomicReference<String>()
        val firstCallback = CountDownLatch(1)
        val secondCallback = CountDownLatch(2)

        manager.onConnectionClosed = { code, reason ->
            callbackCount.incrementAndGet()
            callbackCode.set(code)
            callbackReason.set(reason)
            firstCallback.countDown()
            secondCallback.countDown()
        }

        try {
            runBlocking { manager.connect() }
            assertTrue(manager.isConnected)

            manager.disconnect()

            assertTrue("manual disconnect did not emit a callback", firstCallback.await(2, TimeUnit.SECONDS))
            assertEquals(1000, callbackCode.get())
            assertEquals("Client disconnected", callbackReason.get())
            assertFalse("manual disconnect emitted more than one callback", secondCallback.await(250, TimeUnit.MILLISECONDS))
            assertEquals(1, callbackCount.get())
            assertFalse(manager.isConnected)
            assertTrue("server WebSocket did not close", serverClosed.await(2, TimeUnit.SECONDS))
        } finally {
            manager.disconnect()
            server.close()
        }
    }
}
