package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Received Message
 * 
 * Represents a message received from the server.
 * This data is provided in the MESSAGE event.
 */
data class Message(
    /** Message header with flags and metadata */
    @SerializedName("header")
    val header: Header,
    
    /** Unique message ID */
    @SerializedName("message_id")
    val messageId: String,
    
    /** Message sequence number */
    @SerializedName("message_seq")
    val messageSeq: Long,
    
    /** Message timestamp (server time) */
    @SerializedName("timestamp")
    val timestamp: Long,
    
    /** Channel ID where the message was sent */
    @SerializedName("channel_id")
    val channelId: String,
    
    /** Channel type */
    @SerializedName("channel_type")
    val channelType: Int,
    
    /** User ID of the message sender */
    @SerializedName("from_uid")
    val fromUid: String,
    
    /** Business-defined message payload */
    @SerializedName("payload")
    val payload: Any,
    
    /** Client message number (optional) */
    @SerializedName("client_msg_no")
    val clientMsgNo: String? = null,
    
    /** Stream number (optional) */
    @SerializedName("stream_no")
    val streamNo: String? = null,
    
    /** Stream ID (optional) */
    @SerializedName("stream_id")
    val streamId: String? = null,
    
    /** Stream flag (optional) */
    @SerializedName("stream_flag")
    val streamFlag: Int? = null,
    
    /** Topic (optional) */
    @SerializedName("topic")
    val topic: String? = null
) {
    override fun toString(): String {
        return "Message(messageId='$messageId', channelId='$channelId', fromUid='$fromUid', timestamp=$timestamp)"
    }
}
