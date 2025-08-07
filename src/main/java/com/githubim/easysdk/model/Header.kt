package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Message Header
 * 
 * Contains metadata and flags for message handling.
 * These flags control how the message is processed and stored.
 */
data class Header(
    /** Whether to persist the message (default: true) */
    @SerializedName("no_persist")
    val noPersist: Boolean = false,
    
    /** Whether to show red dot notification (default: true) */
    @SerializedName("red_dot")
    val redDot: Boolean = true,
    
    /** Whether to sync only once (default: false) */
    @SerializedName("sync_once")
    val syncOnce: Boolean = false,
    
    /** Whether this is a duplicate message (default: false) */
    @SerializedName("dup")
    val dup: Boolean = false
)
