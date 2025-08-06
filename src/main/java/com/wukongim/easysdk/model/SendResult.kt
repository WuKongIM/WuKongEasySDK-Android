package com.wukongim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Send Result
 * 
 * Contains information returned by the server after successfully sending a message.
 * This data is returned by the send() method.
 */
data class SendResult(
    /** Unique message ID assigned by the server */
    @SerializedName("message_id")
    val messageId: String,
    
    /** Message sequence number assigned by the server */
    @SerializedName("message_seq")
    val messageSeq: Long
) {
    override fun toString(): String {
        return "SendResult(messageId='$messageId', messageSeq=$messageSeq)"
    }
}
