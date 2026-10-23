package com.githubim.easysdk.model

/** Optional SEND fields. Reuse clientMsgNo when correlating a base message and events. */
data class SendOptions(
    val clientMsgNo: String? = null,
    val header: Header = Header(),
    val topic: String? = null,
    val setting: MessageSetting? = null
) {
    override fun toString(): String = "SendOptions(clientMsgNo=[redacted], header=$header, topic=[redacted], setting=$setting)"
}
