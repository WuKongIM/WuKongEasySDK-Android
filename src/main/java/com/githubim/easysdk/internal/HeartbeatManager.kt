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
    private val config: WuKongConfig,
    private val logger: WuKongLogger = WuKongLogger(config.debugLogging)
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
        
        logger.debug("HeartbeatManager", "Heartbeat started (interval: ${config.pingIntervalMs}ms)")
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
        
        logger.debug("HeartbeatManager", "Heartbeat stopped")
    }
    
    /**
     * Handle pong response (cancels timeout)
     */
    fun onPongReceived() {
        pongTimeoutJob?.cancel()
        pongTimeoutJob = null
        
        logger.debug("HeartbeatManager", "Pong received")
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
                    logger.debug("HeartbeatManager", "Heartbeat loop cancelled")
                    break
                } catch (e: Exception) {
                    logger.error(
                        "HeartbeatManager",
                        "Error in heartbeat loop (${e.javaClass.simpleName})"
                    )
                    
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
        logger.debug("HeartbeatManager", "Sending ping")
        
        // Start pong timeout
        pongTimeoutJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.pongTimeoutMs)
            
            logger.warn("HeartbeatManager", "Pong timeout")
            
            onPongTimeout?.invoke()
        }
        
        try {
            onSendPing?.invoke()
        } catch (e: Exception) {
            logger.error(
                "HeartbeatManager",
                "Failed to send ping (${e.javaClass.simpleName})"
            )
            
            pongTimeoutJob?.cancel()
            pongTimeoutJob = null
            
            // Trigger timeout callback on ping failure
            onPongTimeout?.invoke()
        }
    }
}
