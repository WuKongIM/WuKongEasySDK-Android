package com.githubim.easysdk.internal

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class JsonRpcManagerLoggingTest {

    @Before
    fun clearLogs() {
        ShadowLog.clear()
    }

    @After
    fun clearLogsAfterTest() {
        ShadowLog.clear()
    }

    @Test
    fun `disabled logging does not expose raw inbound data`() {
        val manager = JsonRpcManager()
        exerciseSensitiveInboundPaths(manager)

        val output = capturedLogs()

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
    }

    @Test
    fun `enabled logging reports failures without exposing raw inbound data`() {
        val manager = JsonRpcManager(WuKongLogger(enabled = true))
        exerciseSensitiveInboundPaths(manager)

        val output = capturedLogs()

        assertFalse("authentication token leaked to Logcat: $output", output.contains(TOKEN_CANARY))
        assertFalse("message payload leaked to Logcat: $output", output.contains(PAYLOAD_CANARY))
        assertTrue("debug logging should retain protocol diagnostics", output.contains("JsonRpcManager"))
    }

    private fun exerciseSensitiveInboundPaths(manager: JsonRpcManager) {
        val frames = listOf(
            """{"jsonrpc":"2.0","method":"server_request","id":"1","params":{"token":"$TOKEN_CANARY"}}""",
            """{"payload":"$PAYLOAD_CANARY"}""",
            """not-json-$TOKEN_CANARY""",
            """{"jsonrpc":"2.0","method":"recv","params":{"payload":"$PAYLOAD_CANARY"}}"""
        )

        frames.forEach { frame ->
            manager.handleMessage(frame) { _, _ ->
                throw IllegalArgumentException("rejected $PAYLOAD_CANARY")
            }
        }
    }

    private fun capturedLogs(): String = ShadowLog.getLogs()
        .joinToString("\n") { entry ->
            listOfNotNull(entry.tag, entry.msg, entry.throwable?.stackTraceToString())
                .joinToString(" ")
        }

    private companion object {
        const val TOKEN_CANARY = "TOKEN_CANARY_7fd44dd681"
        const val PAYLOAD_CANARY = "PAYLOAD_CANARY_69c604de9a"
    }
}
