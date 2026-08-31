package com.githubim.easysdk.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ExampleLoggingSourceTest {

    @Test
    fun `example does not expose the connection server key`() {
        val sourcePath = listOf(
            Path.of("src/main/java/com/githubim/easysdk/example/MainActivity.kt"),
            Path.of("example/src/main/java/com/githubim/easysdk/example/MainActivity.kt")
        ).firstOrNull(Files::exists)

        assertNotNull("MainActivity.kt was not found for the example source audit", sourcePath)
        val source = String(Files.readAllBytes(sourcePath!!), Charsets.UTF_8)

        assertFalse(
            "The example must not log the connection server key",
            source.contains("addLog(\"Server info: \${result.serverKey}")
        )
        assertFalse(
            "The example must not directly access the connection server key",
            Regex("""\.\s*serverKey\b""").containsMatchIn(source)
        )
        assertTrue(
            "The example should report connection success with a safe fixed status",
            source.contains("addLog(\"Server handshake completed\", LogLevel.INFO)")
        )
    }
}
