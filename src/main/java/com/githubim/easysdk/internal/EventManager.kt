package com.githubim.easysdk.internal

import android.os.Handler
import android.os.Looper
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.listener.WuKongEventListener
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Event Manager
 * 
 * Manages event listeners and dispatches events in a thread-safe manner.
 * Uses weak references to prevent memory leaks and dispatches events on the main thread.
 */
internal class EventManager(
    private val logger: WuKongLogger = WuKongLogger()
) {
    
    private val listeners = ConcurrentHashMap<WuKongEvent, CopyOnWriteArrayList<WeakReference<WuKongEventListener<Any>>>>()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Add an event listener
     * 
     * @param event The event to listen for
     * @param listener The listener to add
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> addEventListener(event: WuKongEvent, listener: WuKongEventListener<T>) {
        val eventListeners = listeners.getOrPut(event) { CopyOnWriteArrayList() }
        val weakListener = WeakReference(listener as WuKongEventListener<Any>)
        eventListeners.add(weakListener)
        
        // Clean up any null references
        cleanupNullReferences(event)
    }
    
    /**
     * Remove an event listener
     * 
     * @param event The event to stop listening for
     * @param listener The listener to remove
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> removeEventListener(event: WuKongEvent, listener: WuKongEventListener<T>) {
        val eventListeners = listeners[event] ?: return
        
        eventListeners.removeAll { weakRef ->
            val ref = weakRef.get()
            ref == null || ref == listener
        }
        
        if (eventListeners.isEmpty()) {
            listeners.remove(event)
        }
    }
    
    /**
     * Remove all event listeners for a specific event
     * 
     * @param event The event to clear listeners for
     */
    fun removeAllEventListeners(event: WuKongEvent) {
        listeners.remove(event)
    }
    
    /**
     * Remove all event listeners
     */
    fun removeAllEventListeners() {
        listeners.clear()
    }
    
    /**
     * Emit an event to all registered listeners
     *
     * @param event The event to emit
     * @param data The event data
     */
    fun <T> emitEvent(event: WuKongEvent, data: T) {
        val eventListeners = listeners[event]

        logger.debug(
            "EventManager",
            "Dispatching ${event.eventName} event to ${eventListeners?.size ?: 0} listeners"
        )

        if (eventListeners == null) {
            logger.warn("EventManager", "No listeners for ${event.eventName} event")
            return
        }

        // Dispatch on main thread for UI safety
        mainHandler.post {
            val listenersToNotify = mutableListOf<WuKongEventListener<Any>>()
            val nullReferences = mutableListOf<WeakReference<WuKongEventListener<Any>>>()

            // Collect valid listeners and identify null references
            // Note: CopyOnWriteArrayList iterator doesn't support remove()
            for (weakRef in eventListeners) {
                val listener = weakRef.get()
                if (listener != null) {
                    listenersToNotify.add(listener)
                } else {
                    nullReferences.add(weakRef)
                }
            }

            logger.debug(
                "EventManager",
                "Dispatching to ${listenersToNotify.size} active listeners; " +
                    "discarding ${nullReferences.size} stale listeners"
            )

            // Clean up null references using removeAll (which is supported)
            if (nullReferences.isNotEmpty()) {
                eventListeners.removeAll(nullReferences.toSet())
            }

            // Notify all valid listeners
            listenersToNotify.forEach { listener ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    (listener as WuKongEventListener<T>).onEvent(data)
                } catch (e: Exception) {
                    // Log error but don't crash - one listener shouldn't affect others
                    logger.error(
                        "EventManager",
                        "Event listener failed for ${event.eventName} (${e.javaClass.simpleName})"
                    )
                }
            }
        }
    }
    
    /**
     * Clean up null weak references for a specific event
     */
    private fun cleanupNullReferences(event: WuKongEvent) {
        val eventListeners = listeners[event] ?: return
        eventListeners.removeAll { it.get() == null }
        
        if (eventListeners.isEmpty()) {
            listeners.remove(event)
        }
    }
    
    /**
     * Get the number of listeners for an event (for testing/debugging)
     */
    fun getListenerCount(event: WuKongEvent): Int {
        val eventListeners = listeners[event] ?: return 0
        cleanupNullReferences(event)
        return eventListeners.size
    }
}
