package com.wukongim.easysdk.enums

/**
 * SDK Event Names Enum
 * 
 * Defines all the events that can be emitted by the WuKongEasySDK.
 * These events allow applications to respond to various SDK state changes
 * and incoming data.
 */
enum class WuKongEvent(val eventName: String) {
    /** Connection successfully established and authenticated */
    CONNECT("connect"),
    
    /** Disconnected from server */
    DISCONNECT("disconnect"),
    
    /** Received a message */
    MESSAGE("message"),
    
    /** An error occurred (WebSocket error, connection error, etc.) */
    ERROR("error"),
    
    /** Received acknowledgment for a sent message */
    SEND_ACK("sendack"),
    
    /** The SDK is attempting to reconnect */
    RECONNECTING("reconnecting");

    companion object {
        /**
         * Get WuKongEvent from event name string
         * 
         * @param eventName The string name of the event
         * @return The corresponding WuKongEvent, or null if not found
         */
        fun fromEventName(eventName: String): WuKongEvent? {
            return values().find { it.eventName == eventName }
        }
    }
}
