package com.githubim.easysdk.internal

/**
 * Single diagnostic logging boundary for the SDK.
 *
 * Callers must pass operational metadata only. This type deliberately has no
 * overload that accepts a [Throwable], so exception messages, response bodies,
 * tokens, and payloads cannot be appended by Android's logging API.
 */
internal class WuKongLogger(
    private val enabled: Boolean = false
) {
    fun debug(tag: String, message: String) {
        if (enabled) {
            android.util.Log.d(tag, message)
        }
    }

    fun warn(tag: String, message: String) {
        if (enabled) {
            android.util.Log.w(tag, message)
        }
    }

    fun error(tag: String, message: String) {
        if (enabled) {
            android.util.Log.e(tag, message)
        }
    }
}
