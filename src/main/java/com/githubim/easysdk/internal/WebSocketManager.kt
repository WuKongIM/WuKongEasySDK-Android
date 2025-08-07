package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.exception.WuKongNetworkException
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebSocket Manager
 * 
 * Manages WebSocket connection lifecycle, including connection establishment,
 * message sending/receiving, and connection state management.
 */
internal class WebSocketManager(
    private val config: WuKongConfig
) {
    
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private var connectionContinuation: CancellableContinuation<Unit>? = null
    
    var isConnected: Boolean = false
        private set
    
    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionClosed: ((code: Int, reason: String) -> Unit)? = null
    var onConnectionError: ((Throwable) -> Unit)? = null
    
    /**
     * Connect to the WebSocket server
     * 
     * @return Suspends until connection is established or fails
     */
    suspend fun connect(): Unit = withContext(Dispatchers.IO) {
        if (isConnected || webSocket != null) {
            return@withContext
        }
        
        suspendCancellableCoroutine<Unit> { continuation ->
            connectionContinuation = continuation
            
            val client = createOkHttpClient()
            okHttpClient = client
            
            val request = Request.Builder()
                .url(config.serverUrl)
                .build()
            
            val listener = createWebSocketListener()
            
            try {
                webSocket = client.newWebSocket(request, listener)
            } catch (e: Exception) {
                connectionContinuation = null
                if (continuation.isActive) {
                    continuation.resumeWithException(WuKongNetworkException("Failed to create WebSocket", e))
                }
            }
            
            continuation.invokeOnCancellation {
                disconnect()
            }
        }
    }
    
    /**
     * Disconnect from the WebSocket server
     */
    fun disconnect() {
        isConnected = false
        
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnected")
        webSocket = null
        
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient = null
        
        connectionContinuation?.let { continuation ->
            connectionContinuation = null
            if (continuation.isActive) {
                continuation.resumeWithException(Exception("Connection cancelled"))
            }
        }
    }
    
    /**
     * Send a message through the WebSocket
     * 
     * @param message The message to send
     * @return true if message was queued successfully, false otherwise
     */
    fun sendMessage(message: String): Boolean {
        val ws = webSocket
        return if (ws != null && isConnected) {
            ws.send(message)
        } else {
            false
        }
    }
    
    /**
     * Create OkHttpClient with appropriate timeouts
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(config.pingIntervalMs, TimeUnit.MILLISECONDS)
            .build()
    }
    
    /**
     * Create WebSocket listener to handle connection events
     */
    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                
                connectionContinuation?.let { continuation ->
                    connectionContinuation = null
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                
                if (config.debugLogging) {
                    android.util.Log.d("WebSocketManager", "WebSocket connection opened")
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (config.debugLogging) {
                    android.util.Log.d("WebSocketManager", "Received message: $text")
                }
                onMessageReceived?.invoke(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (config.debugLogging) {
                    android.util.Log.d("WebSocketManager", "WebSocket closing: $code - $reason")
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                val wasConnected = isConnected
                isConnected = false
                
                if (config.debugLogging) {
                    android.util.Log.d("WebSocketManager", "WebSocket closed: $code - $reason")
                }
                
                connectionContinuation?.let { continuation ->
                    connectionContinuation = null
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            WuKongNetworkException("Connection closed before authentication (Code: $code)")
                        )
                    }
                }
                
                if (wasConnected) {
                    onConnectionClosed?.invoke(code, reason)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val wasConnected = isConnected
                isConnected = false
                
                if (config.debugLogging) {
                    android.util.Log.e("WebSocketManager", "WebSocket failure", t)
                }
                
                connectionContinuation?.let { continuation ->
                    connectionContinuation = null
                    if (continuation.isActive) {
                        continuation.resumeWithException(WuKongNetworkException("WebSocket failure", t))
                    }
                }
                
                if (wasConnected) {
                    onConnectionError?.invoke(t)
                }
            }
        }
    }
    
    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
