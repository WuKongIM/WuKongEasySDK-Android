package com.githubim.easysdk.model

/** Wire flags shared by SEND and RECV; stream marks a persisted event parent. */
data class MessageSetting(
    /** Request delivery/read receipts. */
    val receipt: Boolean = false,
    /** Marks a signaling/control message. */
    val signal: Boolean = false,
    /** Persist this message as the parent of a stream. */
    val stream: Boolean = false,
    /** Marks a message associated with a topic. */
    val topic: Boolean = false
)
