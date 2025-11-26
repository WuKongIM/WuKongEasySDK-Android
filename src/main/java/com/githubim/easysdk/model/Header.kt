package com.githubim.easysdk.model

import com.google.gson.annotations.SerializedName

/**
 * Message Header
 *
 * Contains metadata and flags for message handling.
 * These flags control how the message is processed and stored.
 *
 * Note: Supports both snake_case and camelCase field names from server
 */
data class Header(
    /** Whether to persist the message (default: true) */
    @SerializedName(value = "no_persist", alternate = ["noPersist"])
    val noPersist: Boolean = false,

    /** Whether to show red dot notification (default: true) */
    @SerializedName(value = "red_dot", alternate = ["redDot"])
    val redDot: Boolean = true,

    /** Whether to sync only once (default: false) */
    @SerializedName(value = "sync_once", alternate = ["syncOnce"])
    val syncOnce: Boolean = false,

    /** Whether this is a duplicate message (default: false) */
    @SerializedName(value = "dup", alternate = ["Dup"])
    val dup: Boolean = false
)
