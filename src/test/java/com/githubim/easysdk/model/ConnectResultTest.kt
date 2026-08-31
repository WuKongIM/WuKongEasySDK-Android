package com.githubim.easysdk.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectResultTest {

    @Test
    fun `string representation redacts the server key`() {
        val result = ConnectResult(
            serverKey = SERVER_KEY_CANARY,
            salt = "salt",
            timeDiff = 42,
            reasonCode = 1,
            serverVersion = 3,
            nodeId = 7
        )

        val rendered = result.toString()

        assertFalse(
            "server key leaked from ConnectResult.toString(): $rendered",
            rendered.contains(SERVER_KEY_CANARY)
        )
        assertTrue(
            "redaction marker missing from ConnectResult.toString(): $rendered",
            rendered.contains("serverKey=[redacted]")
        )
        assertTrue(
            "safe connection metadata should remain useful: $rendered",
            rendered.contains("timeDiff=42")
        )
    }

    private companion object {
        const val SERVER_KEY_CANARY = "SERVER_KEY_CANARY_7e5bc4af"
    }
}
