package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Message Payload
 * 
 * A simple example implementation of a message payload.
 * Applications can define their own payload structures based on their needs.
 */
data class MessagePayload(
    /** Message type identifier */
    @SerializedName("type")
    val type: Int,
    
    /** Message content */
    @SerializedName("content")
    val content: String,
    
    /** Additional custom data (optional) */
    @SerializedName("extra")
    val extra: Map<String, Any>? = null
) {
    override fun toString(): String {
        return "MessagePayload(type=$type, content='$content')"
    }
}
