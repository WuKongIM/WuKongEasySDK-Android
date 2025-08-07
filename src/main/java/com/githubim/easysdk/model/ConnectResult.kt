package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Connection Result
 * 
 * Contains information returned by the server after successful authentication.
 * This data is provided in the CONNECT event.
 */
data class ConnectResult(
    /** Server key for encryption/validation */
    @SerializedName("server_key")
    val serverKey: String,
    
    /** Salt for encryption */
    @SerializedName("salt")
    val salt: String,
    
    /** Time difference between client and server (in milliseconds) */
    @SerializedName("time_diff")
    val timeDiff: Long,
    
    /** Reason code for connection result */
    @SerializedName("reason_code")
    val reasonCode: Int,
    
    /** Server version (optional) */
    @SerializedName("server_version")
    val serverVersion: Int? = null,
    
    /** Node ID of the connected server (optional) */
    @SerializedName("node_id")
    val nodeId: Int? = null
) {
    override fun toString(): String {
        return "ConnectResult(serverKey='$serverKey', timeDiff=$timeDiff, reasonCode=$reasonCode, serverVersion=$serverVersion, nodeId=$nodeId)"
    }
}
