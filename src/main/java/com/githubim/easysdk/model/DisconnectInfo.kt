package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Disconnect Information
 * 
 * Contains information about why the connection was closed.
 * This data is provided in the DISCONNECT event.
 */
data class DisconnectInfo(
    /** Disconnect code (WebSocket close code) */
    @SerializedName("code")
    val code: Int,
    
    /** Reason for disconnection */
    @SerializedName("reason")
    val reason: String,
    
    /** Whether the disconnect was initiated by the client */
    @SerializedName("was_clean")
    val wasClean: Boolean = false
) {
    override fun toString(): String {
        return "DisconnectInfo(code=$code, reason='$reason', wasClean=$wasClean)"
    }
}
