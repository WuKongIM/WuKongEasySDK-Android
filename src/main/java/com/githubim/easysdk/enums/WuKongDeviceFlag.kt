package com.githubim.easysdk.enums

/**
 * Device Flag Enum
 * 
 * Defines the different device types that can connect to WuKongIM.
 * This helps the server identify and manage different client types.
 */
enum class WuKongDeviceFlag(val value: Int) {
    /** Mobile application (WuKongIM protocol value: 0) */
    APP(0),
    
    /** Web browser (WuKongIM protocol value: 1) */
    WEB(1),
    
    /** Desktop application / PC (WuKongIM protocol value: 2) */
    DESKTOP(2),
    
    /** Other device types */
    OTHER(4);

    companion object {
        /**
         * Get WuKongDeviceFlag from integer value
         * 
         * @param value The integer value of the device flag
         * @return The corresponding WuKongDeviceFlag, or null if not found
         */
        fun fromValue(value: Int): WuKongDeviceFlag? {
            return values().find { it.value == value }
        }
    }
}
