package com.githubim.easysdk.model

import com.githubim.easysdk.enums.WuKongErrorCode

/**
 * WuKong Error
 * 
 * Represents an error that occurred during SDK operations.
 * This data is provided in the ERROR event.
 */
data class WuKongError(
    /** Error code */
    val code: WuKongErrorCode,
    
    /** Error message */
    val message: String,
    
    /** Additional error data (optional) */
    val data: Any? = null,
    
    /** Underlying exception that caused this error (optional) */
    val cause: Throwable? = null
) {
    constructor(
        code: Int,
        message: String,
        data: Any? = null,
        cause: Throwable? = null
    ) : this(WuKongErrorCode.fromCode(code), message, data, cause)

    override fun toString(): String {
        return "WuKongError(code=${code.code}, message='$message', data=$data)"
    }
}
