package com.githubim.easysdk.model

/** Generic public application event. Stream projection remains an application concern. */
data class EventNotification(
    val id: String,
    val type: String,
    val timestamp: Long,
    val data: Any?,
    val header: Header? = null
) {
    override fun toString(): String = "EventNotification(id=[redacted], type=[redacted], timestamp=$timestamp, data=[redacted])"
}
