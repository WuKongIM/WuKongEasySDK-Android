package com.githubim.easysdk.listener

/**
 * WuKong Event Listener
 * 
 * Generic interface for handling SDK events.
 * Type-safe event handling ensures compile-time checking of event data types.
 * 
 * @param T The type of data associated with the event
 */
interface WuKongEventListener<T> {
    /**
     * Called when the event occurs
     * 
     * @param data The event data
     */
    fun onEvent(data: T)
}
