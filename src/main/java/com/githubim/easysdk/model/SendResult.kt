package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Send Result
 * 
 * Contains information returned by the server after successfully sending a message.
 * This data is returned by the send() method.
 */
data class SendResult(
    /** Unique message ID assigned by the server */
    @SerializedName(value = "message_id", alternate = ["messageId"])
    val messageId: String,
    
    /** Message sequence number assigned by the server */
    @SerializedName(value = "message_seq", alternate = ["messageSeq"])
    val messageSeq: Long,

    /** Server acceptance code: 1 means success; null supports older servers. */
    @SerializedName(value = "reason_code", alternate = ["reasonCode"])
    val reasonCode: Int? = null
) {
    override fun toString(): String {
        return "SendResult(messageId=[redacted], messageSeq=$messageSeq)"
    }
}
