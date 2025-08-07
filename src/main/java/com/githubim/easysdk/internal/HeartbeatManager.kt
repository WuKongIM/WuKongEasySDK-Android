package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import kotlinx.coroutines.*

/**
 * Heartbeat Manager
 * 
 * Manages ping/pong heartbeat mechanism to detect connection health.
 * Automatically sends ping requests and monitors pong responses.
 */
internal class HeartbeatManager(
    private val config: WuKongConfig
) {
    
    private var heartbeatJob: Job? = null
    private var pongTimeoutJob: Job? = null
    private var isRunning = false
    
    var onSendPing: (suspend () -> Unit)? = null
    var onPongTimeout: (() -> Unit)? = null
    
    /**
     * Start the heartbeat mechanism
     */
    fun start() {
        if (isRunning) {
            return
        }
        
        isRunning = true
        startHeartbeatLoop()
        
        if (config.debugLogging) {
            android.util.Log.d("HeartbeatManager", "Heartbeat started (interval: ${config.pingIntervalMs}ms)")
        }
    }
    
    /**
     * Stop the heartbeat mechanism
     */
    fun stop() {
        isRunning = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        pongTimeoutJob?.cancel()
        pongTimeoutJob = null
        
        if (config.debugLogging) {
            android.util.Log.d("HeartbeatManager", "Heartbeat stopped")
        }
    }
    
    /**
     * Handle pong response (cancels timeout)
     */
    fun onPongReceived() {
        pongTimeoutJob?.cancel()
        pongTimeoutJob = null
        
        if (config.debugLogging) {
            android.util.Log.d("HeartbeatManager", "Pong received")
        }
    }
    
    /**
     * Check if heartbeat is currently running
     */
    fun isRunning(): Boolean = isRunning
    
    /**
     * Start the heartbeat loop
     */
    private fun startHeartbeatLoop() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                try {
                    delay(config.pingIntervalMs)
                    
                    if (!isRunning) {
                        break
                    }
                    
                    sendPingWithTimeout()
                    
                } catch (e: CancellationException) {
                    if (config.debugLogging) {
                        android.util.Log.d("HeartbeatManager", "Heartbeat loop cancelled")
                    }
                    break
                } catch (e: Exception) {
                    if (config.debugLogging) {
                        android.util.Log.e("HeartbeatManager", "Error in heartbeat loop", e)
                    }
                    
                    // Continue the loop unless explicitly stopped
                    if (!isRunning) {
                        break
                    }
                }
            }
        }
    }
    
    /**
     * Send ping and start pong timeout
     */
    private suspend fun sendPingWithTimeout() {
        if (config.debugLogging) {
            android.util.Log.d("HeartbeatManager", "Sending ping")
        }
        
        // Start pong timeout
        pongTimeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.pongTimeoutMs)
            
            if (config.debugLogging) {
                android.util.Log.w("HeartbeatManager", "Pong timeout")
            }
            
            onPongTimeout?.invoke()
        }
        
        try {
            onSendPing?.invoke()
        } catch (e: Exception) {
            if (config.debugLogging) {
                android.util.Log.e("HeartbeatManager", "Failed to send ping", e)
            }
            
            pongTimeoutJob?.cancel()
            pongTimeoutJob = null
            
            // Trigger timeout callback on ping failure
            onPongTimeout?.invoke()
        }
    }
}
