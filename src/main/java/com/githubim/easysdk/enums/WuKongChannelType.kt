package com.githubim.easysdk.enums

/**
 * Channel Type Enum based on WuKongIM protocol
 * 
 * Defines the different types of channels supported by WuKongIM.
 * Each channel type has specific behavior and use cases.
 */
enum class WuKongChannelType(val value: Int) {
    /** Person channel - for one-on-one conversations */
    PERSON(1),
    
    /** Group channel - for group conversations */
    GROUP(2),
    
    /** Customer Service channel (Consider using VISITORS channel instead) */
    CUSTOMER_SERVICE(3),
    
    /** Community channel - for community-wide communications */
    COMMUNITY(4),
    
    /** Community Topic channel - for specific topics within a community */
    COMMUNITY_TOPIC(5),
    
    /** Info channel (with concept of temporary subscribers) */
    INFO(6),
    
    /** Data channel - for data transmission */
    DATA(7),
    
    /** Temporary channel - for temporary communications */
    TEMP(8),
    
    /** Live channel (does not save recent session data) */
    LIVE(9),
    
    /** Visitors channel (replaces CustomerService for new implementations) */
    VISITORS(10);

    companion object {
        /**
         * Get WuKongChannelType from integer value
         * 
         * @param value The integer value of the channel type
         * @return The corresponding WuKongChannelType, or null if not found
         */
        fun fromValue(value: Int): WuKongChannelType? {
            return values().find { it.value == value }
        }
    }
}
