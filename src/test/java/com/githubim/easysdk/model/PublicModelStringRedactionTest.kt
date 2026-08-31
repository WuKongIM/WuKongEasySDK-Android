package com.githubim.easysdk.model

import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.enums.WuKongDeviceFlag
import com.githubim.easysdk.enums.WuKongErrorCode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicModelStringRedactionTest {

    @Test
    fun `configuration string representation redacts connection credentials`() {
        val config = WuKongConfig.Builder()
            .serverUrl("wss://$SERVER_URL_CANARY.example/ws")
            .uid(UID_CANARY)
            .token(TOKEN_CANARY)
            .deviceId(DEVICE_ID_CANARY)
            .deviceFlag(WuKongDeviceFlag.APP)
            .build()

        val rendered = config.toString()

        assertDoesNotContain(
            rendered,
            SERVER_URL_CANARY,
            UID_CANARY,
            TOKEN_CANARY,
            DEVICE_ID_CANARY
        )
        assertTrue("redaction marker missing from WuKongConfig.toString(): $rendered", rendered.contains("token=[redacted]"))
        assertTrue("safe device classification should remain useful: $rendered", rendered.contains("deviceFlag=APP"))
    }

    @Test
    fun `message payload string representation redacts content and custom data`() {
        val payload = MessagePayload(
            type = 7,
            content = CONTENT_CANARY,
            extra = mapOf("secret" to EXTRA_CANARY)
        )

        val rendered = payload.toString()

        assertDoesNotContain(rendered, CONTENT_CANARY, EXTRA_CANARY)
        assertTrue("safe payload type should remain useful: $rendered", rendered.contains("type=7"))
        assertTrue("safe extra count should remain useful: $rendered", rendered.contains("extraCount=1"))
    }

    @Test
    fun `message string representation redacts payload and identifiers`() {
        val message = Message(
            header = Header(),
            messageId = MESSAGE_ID_CANARY,
            messageSeq = 44,
            timestamp = 1_725_000_000,
            channelId = CHANNEL_ID_CANARY,
            channelType = 2,
            fromUid = FROM_UID_CANARY,
            payload = mapOf("content" to PAYLOAD_CANARY),
            clientMsgNo = CLIENT_MESSAGE_NO_CANARY,
            streamNo = STREAM_NO_CANARY,
            streamId = STREAM_ID_CANARY,
            topic = TOPIC_CANARY
        )

        val rendered = message.toString()

        assertDoesNotContain(
            rendered,
            MESSAGE_ID_CANARY,
            CHANNEL_ID_CANARY,
            FROM_UID_CANARY,
            PAYLOAD_CANARY,
            CLIENT_MESSAGE_NO_CANARY,
            STREAM_NO_CANARY,
            STREAM_ID_CANARY,
            TOPIC_CANARY
        )
        assertTrue("safe message sequence should remain useful: $rendered", rendered.contains("messageSeq=44"))
        assertTrue("safe channel classification should remain useful: $rendered", rendered.contains("channelType=2"))
    }

    @Test
    fun `disconnect string representation redacts server reason`() {
        val info = DisconnectInfo(code = 1008, reason = REASON_CANARY, wasClean = false)

        val rendered = info.toString()

        assertDoesNotContain(rendered, REASON_CANARY)
        assertTrue("safe disconnect code should remain useful: $rendered", rendered.contains("code=1008"))
        assertTrue("safe disconnect classification should remain useful: $rendered", rendered.contains("wasClean=false"))
    }

    @Test
    fun `error string representation redacts message data and cause`() {
        val error = WuKongError(
            code = WuKongErrorCode.NETWORK_ERROR,
            message = ERROR_MESSAGE_CANARY,
            data = mapOf("response" to ERROR_DATA_CANARY),
            cause = IllegalStateException(ERROR_CAUSE_CANARY)
        )

        val rendered = error.toString()

        assertDoesNotContain(rendered, ERROR_MESSAGE_CANARY, ERROR_DATA_CANARY, ERROR_CAUSE_CANARY)
        assertTrue("safe error code should remain useful: $rendered", rendered.contains("code=1002"))
    }

    @Test
    fun `send result string representation redacts message identifier`() {
        val result = SendResult(messageId = SEND_MESSAGE_ID_CANARY, messageSeq = 91)

        val rendered = result.toString()

        assertDoesNotContain(rendered, SEND_MESSAGE_ID_CANARY)
        assertTrue("safe message sequence should remain useful: $rendered", rendered.contains("messageSeq=91"))
    }

    private fun assertDoesNotContain(rendered: String, vararg canaries: String) {
        canaries.forEach { canary ->
            assertFalse("sensitive canary leaked from toString(): $rendered", rendered.contains(canary))
        }
    }

    private companion object {
        const val SERVER_URL_CANARY = "SERVER_URL_CANARY_a55a80"
        const val UID_CANARY = "UID_CANARY_a55a80"
        const val TOKEN_CANARY = "TOKEN_CANARY_a55a80"
        const val DEVICE_ID_CANARY = "DEVICE_ID_CANARY_a55a80"
        const val CONTENT_CANARY = "CONTENT_CANARY_a55a80"
        const val EXTRA_CANARY = "EXTRA_CANARY_a55a80"
        const val MESSAGE_ID_CANARY = "MESSAGE_ID_CANARY_a55a80"
        const val CHANNEL_ID_CANARY = "CHANNEL_ID_CANARY_a55a80"
        const val FROM_UID_CANARY = "FROM_UID_CANARY_a55a80"
        const val PAYLOAD_CANARY = "PAYLOAD_CANARY_a55a80"
        const val CLIENT_MESSAGE_NO_CANARY = "CLIENT_MESSAGE_NO_CANARY_a55a80"
        const val STREAM_NO_CANARY = "STREAM_NO_CANARY_a55a80"
        const val STREAM_ID_CANARY = "STREAM_ID_CANARY_a55a80"
        const val TOPIC_CANARY = "TOPIC_CANARY_a55a80"
        const val REASON_CANARY = "REASON_CANARY_a55a80"
        const val ERROR_MESSAGE_CANARY = "ERROR_MESSAGE_CANARY_a55a80"
        const val ERROR_DATA_CANARY = "ERROR_DATA_CANARY_a55a80"
        const val ERROR_CAUSE_CANARY = "ERROR_CAUSE_CANARY_a55a80"
        const val SEND_MESSAGE_ID_CANARY = "SEND_MESSAGE_ID_CANARY_a55a80"
    }
}
