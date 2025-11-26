package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Received Message
 *
 * Represents a message received from the server.
 * This data is provided in the MESSAGE event.
 *
 * Note: Supports both snake_case and camelCase field names from server
 */
data class Message(
    /** Message header with flags and metadata */
    @SerializedName(value = "header", alternate = ["Header"])
    val header: Header,

    /** Unique message ID */
    @SerializedName(value = "message_id", alternate = ["messageId"])
    val messageId: String,

    /** Message sequence number */
    @SerializedName(value = "message_seq", alternate = ["messageSeq"])
    val messageSeq: Long,

    /** Message timestamp (server time) */
    @SerializedName(value = "timestamp", alternate = ["Timestamp"])
    val timestamp: Long,

    /** Channel ID where the message was sent */
    @SerializedName(value = "channel_id", alternate = ["channelId"])
    val channelId: String,

    /** Channel type */
    @SerializedName(value = "channel_type", alternate = ["channelType"])
    val channelType: Int,

    /** User ID of the message sender */
    @SerializedName(value = "from_uid", alternate = ["fromUid"])
    val fromUid: String,

    /** Business-defined message payload */
    @SerializedName(value = "payload", alternate = ["Payload"])
    val payload: Any,

    /** Client message number (optional) */
    @SerializedName(value = "client_msg_no", alternate = ["clientMsgNo"])
    val clientMsgNo: String? = null,

    /** Stream number (optional) */
    @SerializedName(value = "stream_no", alternate = ["streamNo"])
    val streamNo: String? = null,

    /** Stream ID (optional) */
    @SerializedName(value = "stream_id", alternate = ["streamId"])
    val streamId: String? = null,

    /** Stream flag (optional) */
    @SerializedName(value = "stream_flag", alternate = ["streamFlag"])
    val streamFlag: Int? = null,

    /** Topic (optional) */
    @SerializedName(value = "topic", alternate = ["Topic"])
    val topic: String? = null
) {
    override fun toString(): String {
        return "Message(messageId='$messageId', channelId='$channelId', fromUid='$fromUid', timestamp=$timestamp)"
    }
}
