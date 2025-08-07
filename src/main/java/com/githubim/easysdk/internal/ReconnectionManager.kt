package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.pow

/**
 * Reconnection Manager
 * 
 * Handles automatic reconnection logic with exponential backoff strategy.
 * Manages reconnection attempts and provides callbacks for reconnection events.
 */
internal class ReconnectionManager(
    private val config: WuKongConfig
) {
    
    private var reconnectionJob: Job? = null
    private var reconnectAttempts = 0
    private var isReconnecting = false
    private var manualDisconnect = false
    
    var onReconnectAttempt: ((attempt: Int, delay: Long) -> Unit)? = null
    var onReconnectSuccess: (() -> Unit)? = null
    var onReconnectFailed: (() -> Unit)? = null
    var onConnect: (suspend () -> Unit)? = null
    
    /**
     * Start reconnection process
     * 
     * @param wasConnected Whether the client was previously connected
     */
    fun startReconnection(wasConnected: Boolean) {
        if (isReconnecting || manualDisconnect || !wasConnected) {
            return
        }
        
        if (config.maxReconnectAttempts <= 0) {
            if (config.debugLogging) {
                android.util.Log.d("ReconnectionManager", "Reconnection disabled (maxReconnectAttempts = 0)")
            }
            return
        }
        
        isReconnecting = true
        scheduleReconnection()
    }
    
    /**
     * Stop reconnection process
     * 
     * @param isManual Whether the stop was initiated manually
     */
    fun stopReconnection(isManual: Boolean = false) {
        manualDisconnect = isManual
        isReconnecting = false
        reconnectionJob?.cancel()
        reconnectionJob = null
        
        if (isManual) {
            reconnectAttempts = 0
        }
    }
    
    /**
     * Reset reconnection state after successful connection
     */
    fun onConnectionSuccess() {
        reconnectAttempts = 0
        isReconnecting = false
        manualDisconnect = false
        reconnectionJob?.cancel()
        reconnectionJob = null
        
        onReconnectSuccess?.invoke()
    }
    
    /**
     * Check if currently reconnecting
     */
    fun isReconnecting(): Boolean = isReconnecting
    
    /**
     * Get current reconnection attempt count
     */
    fun getReconnectAttempts(): Int = reconnectAttempts
    
    /**
     * Schedule the next reconnection attempt
     */
    private fun scheduleReconnection() {
        if (reconnectAttempts >= config.maxReconnectAttempts) {
            if (config.debugLogging) {
                android.util.Log.w("ReconnectionManager", "Max reconnect attempts reached. Giving up.")
            }
            isReconnecting = false
            reconnectAttempts = 0
            onReconnectFailed?.invoke()
            return
        }
        
        val delay = calculateReconnectDelay()
        reconnectAttempts++
        
        if (config.debugLogging) {
            android.util.Log.d(
                "ReconnectionManager", 
                "Will attempt to reconnect in ${delay}ms (Attempt $reconnectAttempts)"
            )
        }
        
        onReconnectAttempt?.invoke(reconnectAttempts, delay)
        
        reconnectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(delay)
                
                if (!isReconnecting) {
                    if (config.debugLogging) {
                        android.util.Log.d("ReconnectionManager", "Reconnection cancelled")
                    }
                    return@launch
                }
                
                if (config.debugLogging) {
                    android.util.Log.d("ReconnectionManager", "Attempting reconnection...")
                }
                
                onConnect?.invoke()
                
            } catch (e: CancellationException) {
                if (config.debugLogging) {
                    android.util.Log.d("ReconnectionManager", "Reconnection cancelled")
                }
            } catch (e: Exception) {
                if (config.debugLogging) {
                    android.util.Log.e("ReconnectionManager", "Reconnection attempt failed", e)
                }
                
                if (isReconnecting) {
                    scheduleReconnection()
                }
            }
        }
    }
    
    /**
     * Calculate reconnection delay using exponential backoff
     */
    private fun calculateReconnectDelay(): Long {
        val exponentialDelay = config.initialReconnectDelayMs * 
            2.0.pow((reconnectAttempts - 1).toDouble()).toLong()
        
        // Cap the delay at 30 seconds
        return min(exponentialDelay, MAX_RECONNECT_DELAY_MS)
    }
    
    companion object {
        private const val MAX_RECONNECT_DELAY_MS = 30_000L // 30 seconds
    }
}
