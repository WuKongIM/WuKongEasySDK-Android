package com.githubim.easysdk

import android.content.Context
import com.githubim.easysdk.enums.WuKongChannelType
import com.githubim.easysdk.enums.WuKongErrorCode
import com.githubim.easysdk.enums.WuKongEvent
import com.githubim.easysdk.exception.WuKongAuthenticationException
import com.githubim.easysdk.exception.WuKongConfigurationException
import com.githubim.easysdk.exception.WuKongException
import com.githubim.easysdk.exception.WuKongNetworkException
import com.githubim.easysdk.exception.WuKongNotConnectedException
import com.githubim.easysdk.internal.EventManager
import com.githubim.easysdk.internal.HeartbeatManager
import com.githubim.easysdk.internal.JsonRpcManager
import com.githubim.easysdk.internal.ReconnectionManager
import com.githubim.easysdk.internal.WebSocketManager
import com.githubim.easysdk.listener.WuKongEventListener
import com.githubim.easysdk.model.ConnectResult
import com.githubim.easysdk.model.DisconnectInfo
import com.githubim.easysdk.model.Header
import com.githubim.easysdk.model.Message
import com.githubim.easysdk.model.SendResult
import com.githubim.easysdk.model.WuKongError
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

/**
 * WuKong Easy SDK
 * 
 * Main SDK class providing a simple interface for WuKongIM communication.
 * Implements singleton pattern for easy access throughout the application.
 */
class WuKongEasySDK private constructor() {
    
    private var config: WuKongConfig? = null
    private var context: Context? = null
    private var isInitialized = false
    
    // Internal managers
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var jsonRpcManager: JsonRpcManager
    private lateinit var eventManager: EventManager
    private lateinit var reconnectionManager: ReconnectionManager
    private lateinit var heartbeatManager: HeartbeatManager
    
    // SDK state
    private var isConnected = false
    private var isConnecting = false
    
    // Coroutine scope for SDK operations
    private val sdkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // JSON serialization
    private val gson = Gson()
    
    /**
     * Initialize the SDK with configuration
     * 
     * @param context Application or Activity context
     * @param config SDK configuration
     * @throws WuKongConfigurationException if configuration is invalid
     */
    fun init(context: Context, config: WuKongConfig) {
        if (isInitialized) {
            throw WuKongConfigurationException("SDK is already initialized")
        }
        
        this.context = context.applicationContext
        this.config = config
        
        initializeManagers()
        setupManagerCallbacks()
        
        isInitialized = true
        
        if (config.debugLogging) {
            android.util.Log.d("WuKongEasySDK", "SDK initialized successfully")
        }
    }
    
    /**
     * Connect to the WuKongIM server
     * 
     * @throws WuKongConfigurationException if SDK is not initialized
     * @throws WuKongNetworkException if connection fails
     */
    suspend fun connect() {
        ensureInitialized()
        
        if (isConnected || isConnecting) {
            if (config?.debugLogging == true) {
                android.util.Log.w("WuKongEasySDK", "Already connected or connecting")
            }
            return
        }
        
        isConnecting = true
        
        try {
            webSocketManager.connect()
            authenticate()
            
            isConnected = true
            isConnecting = false
            
            heartbeatManager.start()
            reconnectionManager.onConnectionSuccess()
            
            if (config?.debugLogging == true) {
                android.util.Log.d("WuKongEasySDK", "Connected successfully")
            }
            
        } catch (e: Exception) {
            isConnecting = false
            isConnected = false
            
            if (config?.debugLogging == true) {
                android.util.Log.e("WuKongEasySDK", "Connection failed", e)
            }
            
            throw when (e) {
                is WuKongException -> e
                else -> WuKongNetworkException("Connection failed", e)
            }
        }
    }
    
    /**
     * Disconnect from the server
     */
    fun disconnect() {
        if (!isInitialized) {
            return
        }
        
        reconnectionManager.stopReconnection(isManual = true)
        heartbeatManager.stop()
        webSocketManager.disconnect()
        jsonRpcManager.cancelAllRequests()
        
        isConnected = false
        isConnecting = false
        
        if (config?.debugLogging == true) {
            android.util.Log.d("WuKongEasySDK", "Disconnected")
        }
    }
    
    /**
     * Send a message to a channel
     * 
     * @param channelId Target channel ID
     * @param channelType Target channel type
     * @param payload Message payload
     * @param header Optional message header
     * @param topic Optional topic
     * @return Send result with message ID and sequence
     * @throws WuKongNotConnectedException if not connected
     */
    suspend fun send(
        channelId: String,
        channelType: WuKongChannelType,
        payload: Any,
        header: Header = Header(),
        topic: String? = null
    ): SendResult {
        ensureConnected()
        
        val clientMsgNo = generateClientMessageId()
        val params = mapOf(
            "client_msg_no" to clientMsgNo,
            "channel_id" to channelId,
            "channel_type" to channelType.value,
            "payload" to payload,
            "header" to header,
            "topic" to topic
        )
        
        try {
            val result = jsonRpcManager.sendRequest(
                method = "send",
                params = params,
                timeoutMs = config?.requestTimeoutMs ?: 15000L,
                sendMessage = { message -> webSocketManager.sendMessage(message) }
            )
            
            return gson.fromJson(result, SendResult::class.java)
            
        } catch (e: Exception) {
            if (config?.debugLogging == true) {
                android.util.Log.e("WuKongEasySDK", "Failed to send message", e)
            }
            
            throw when {
                e.message?.contains("timeout") == true -> 
                    WuKongNetworkException("Message send timeout", e)
                else -> WuKongNetworkException("Failed to send message", e)
            }
        }
    }
    
    /**
     * Add an event listener
     * 
     * @param event The event to listen for
     * @param listener The listener to add
     */
    fun <T> addEventListener(event: WuKongEvent, listener: WuKongEventListener<T>) {
        ensureInitialized()
        eventManager.addEventListener(event, listener)
    }
    
    /**
     * Remove an event listener
     * 
     * @param event The event to stop listening for
     * @param listener The listener to remove
     */
    fun <T> removeEventListener(event: WuKongEvent, listener: WuKongEventListener<T>) {
        if (!isInitialized) {
            return
        }
        eventManager.removeEventListener(event, listener)
    }
    
    /**
     * Remove all event listeners for a specific event
     * 
     * @param event The event to clear listeners for
     */
    fun removeAllEventListeners(event: WuKongEvent) {
        if (!isInitialized) {
            return
        }
        eventManager.removeAllEventListeners(event)
    }
    
    /**
     * Remove all event listeners
     */
    fun removeAllEventListeners() {
        if (!isInitialized) {
            return
        }
        eventManager.removeAllEventListeners()
    }
    
    /**
     * Check if the SDK is connected
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Check if the SDK is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Get current configuration (read-only)
     */
    fun getConfig(): WuKongConfig? = config
    
    /**
     * Initialize internal managers
     */
    private fun initializeManagers() {
        val cfg = config ?: throw WuKongConfigurationException("Configuration is null")
        
        webSocketManager = WebSocketManager(cfg)
        jsonRpcManager = JsonRpcManager()
        eventManager = EventManager()
        reconnectionManager = ReconnectionManager(cfg)
        heartbeatManager = HeartbeatManager(cfg)
    }
    
    /**
     * Setup callbacks for internal managers
     */
    private fun setupManagerCallbacks() {
        // WebSocket callbacks
        webSocketManager.onMessageReceived = { message ->
            handleIncomingMessage(message)
        }
        
        webSocketManager.onConnectionClosed = { code, reason ->
            handleConnectionClosed(code, reason)
        }
        
        webSocketManager.onConnectionError = { error ->
            handleConnectionError(error)
        }
        
        // Reconnection callbacks
        reconnectionManager.onReconnectAttempt = { attempt, delay ->
            eventManager.emitEvent(WuKongEvent.RECONNECTING, mapOf(
                "attempt" to attempt,
                "delay" to delay
            ))
        }
        
        reconnectionManager.onConnect = {
            connect()
        }
        
        // Heartbeat callbacks
        heartbeatManager.onSendPing = {
            sendPing()
        }
        
        heartbeatManager.onPongTimeout = {
            handlePongTimeout()
        }
    }
    
    /**
     * Authenticate with the server
     */
    private suspend fun authenticate() {
        val cfg = config ?: throw WuKongConfigurationException("Configuration is null")

        val params = mapOf(
            "uid" to cfg.uid,
            "token" to cfg.token,
            "device_id" to cfg.deviceId,
            "device_flag" to cfg.deviceFlag.value,
            "client_timestamp" to System.currentTimeMillis()
        )

        try {
            val result = jsonRpcManager.sendRequest(
                method = "connect",
                params = params,
                timeoutMs = cfg.connectionTimeoutMs,
                sendMessage = { message -> webSocketManager.sendMessage(message) }
            )

            val connectResult = gson.fromJson(result, ConnectResult::class.java)
            eventManager.emitEvent(WuKongEvent.CONNECT, connectResult)

        } catch (e: Exception) {
            throw WuKongAuthenticationException("Authentication failed: ${e.message}")
        }
    }

    /**
     * Send ping to server
     */
    private suspend fun sendPing() {
        try {
            // According to WuKongIM JSON-RPC schema, ping params can be null or empty object
            val result = jsonRpcManager.sendRequest(
                method = "ping",
                params = mapOf<String, Any>(), // Empty object as per schema
                timeoutMs = config?.pongTimeoutMs ?: 10000L,
                sendMessage = { message -> webSocketManager.sendMessage(message) }
            )

            // Pong response received successfully
            heartbeatManager.onPongReceived()

            if (config?.debugLogging == true) {
                android.util.Log.d("WuKongEasySDK", "Pong received successfully")
            }
        } catch (e: Exception) {
            if (config?.debugLogging == true) {
                android.util.Log.e("WuKongEasySDK", "Ping failed", e)
            }
            throw e
        }
    }

    /**
     * Handle incoming WebSocket messages
     */
    private fun handleIncomingMessage(message: String) {
        jsonRpcManager.handleMessage(message) { method, params ->
            when (method) {
                "recv" -> {
                    try {
                        if (config?.debugLogging == true) {
                            android.util.Log.d("WuKongEasySDK", "Parsing recv params: $params")
                        }

                        val messageData = gson.fromJson(params, Message::class.java)

                        if (config?.debugLogging == true) {
                            android.util.Log.d("WuKongEasySDK", "Parsed message: $messageData")
                            android.util.Log.d("WuKongEasySDK", "Message payload: ${messageData.payload}")
                            android.util.Log.d("WuKongEasySDK", "准备触发 MESSAGE 事件")
                        }

                        eventManager.emitEvent(WuKongEvent.MESSAGE, messageData)

                        if (config?.debugLogging == true) {
                            android.util.Log.d("WuKongEasySDK", "MESSAGE 事件已触发")
                        }

                        // Send acknowledgment
                        sendReceiveAck(messageData)
                    } catch (e: Exception) {
                        android.util.Log.e("WuKongEasySDK", "Failed to parse recv message", e)
                        android.util.Log.e("WuKongEasySDK", "Params: $params")
                    }
                }
                // Note: "pong" is handled as a response to "ping" request, not as a notification
                "disconnect" -> {
                    val disconnectInfo = gson.fromJson(params, DisconnectInfo::class.java)
                    eventManager.emitEvent(WuKongEvent.DISCONNECT, disconnectInfo)
                }
                else -> {
                    if (config?.debugLogging == true) {
                        android.util.Log.w("WuKongEasySDK", "Unhandled notification method: $method")
                    }
                }
            }
        }
    }

    /**
     * Send receive acknowledgment
     */
    private fun sendReceiveAck(message: Message) {
        val params = mapOf(
            "header" to message.header,
            "message_id" to message.messageId,
            "message_seq" to message.messageSeq
        )

        jsonRpcManager.sendNotification(
            method = "recvack",
            params = params,
            sendMessage = { msg -> webSocketManager.sendMessage(msg) }
        )
    }

    /**
     * Handle connection closed
     */
    private fun handleConnectionClosed(code: Int, reason: String) {
        val wasConnected = isConnected
        isConnected = false
        isConnecting = false

        heartbeatManager.stop()
        jsonRpcManager.cancelAllRequests()

        val disconnectInfo = DisconnectInfo(code, reason, code == 1000)
        eventManager.emitEvent(WuKongEvent.DISCONNECT, disconnectInfo)

        if (wasConnected) {
            reconnectionManager.startReconnection(true)
        }
    }

    /**
     * Handle connection error
     */
    private fun handleConnectionError(error: Throwable) {
        val wasConnected = isConnected
        isConnected = false
        isConnecting = false

        heartbeatManager.stop()
        jsonRpcManager.cancelAllRequests()

        val wuKongError = WuKongError(
            code = WuKongErrorCode.NETWORK_ERROR,
            message = error.message ?: "Network error",
            cause = error
        )
        eventManager.emitEvent(WuKongEvent.ERROR, wuKongError)

        if (wasConnected) {
            reconnectionManager.startReconnection(true)
        }
    }

    /**
     * Handle pong timeout
     */
    private fun handlePongTimeout() {
        if (config?.debugLogging == true) {
            android.util.Log.w("WuKongEasySDK", "Pong timeout - triggering reconnection")
        }

        val wuKongError = WuKongError(
            code = WuKongErrorCode.CONNECTION_TIMEOUT,
            message = "Ping timeout"
        )
        eventManager.emitEvent(WuKongEvent.ERROR, wuKongError)

        // Disconnect and trigger reconnection
        webSocketManager.disconnect()
    }

    /**
     * Generate unique client message ID
     */
    private fun generateClientMessageId(): String {
        return "android_${UUID.randomUUID().toString().replace("-", "")}"
    }

    /**
     * Ensure SDK is initialized
     */
    private fun ensureInitialized() {
        if (!isInitialized) {
            throw WuKongConfigurationException("SDK is not initialized. Call init() first.")
        }
    }

    /**
     * Ensure SDK is connected
     */
    private fun ensureConnected() {
        ensureInitialized()
        if (!isConnected) {
            throw WuKongNotConnectedException("Not connected to server. Call connect() first.")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WuKongEasySDK? = null

        /**
         * Get the singleton instance of WuKongEasySDK
         *
         * @return The SDK instance
         */
        fun getInstance(): WuKongEasySDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WuKongEasySDK().also { INSTANCE = it }
            }
        }
    }
}
