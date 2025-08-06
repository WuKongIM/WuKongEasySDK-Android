package com.wukongim.easysdk.example

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.wukongim.easysdk.WuKongConfig
import com.wukongim.easysdk.WuKongEasySDK
import com.wukongim.easysdk.enums.WuKongChannelType
import com.wukongim.easysdk.enums.WuKongDeviceFlag
import com.wukongim.easysdk.enums.WuKongEvent
import com.wukongim.easysdk.listener.WuKongEventListener
import com.wukongim.easysdk.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive example application demonstrating WuKongIM Android EasySDK functionality
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WuKongExample"
        private const val MAX_LOG_LINES = 100
    }
    
    // SDK instance
    private lateinit var easySDK: WuKongEasySDK
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    // UI Components - Connection Section
    private lateinit var serverUrlEdit: EditText
    private lateinit var userIdEdit: EditText
    private lateinit var tokenEdit: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var connectionStatusText: TextView
    private lateinit var connectionIndicator: View
    
    // UI Components - Messaging Section
    private lateinit var targetChannelEdit: EditText
    private lateinit var messageContentEdit: EditText
    private lateinit var sendButton: Button
    private lateinit var messageHistoryText: TextView
    private lateinit var messageHistoryScroll: ScrollView
    
    // UI Components - Logs Section
    private lateinit var logsText: TextView
    private lateinit var logsScroll: ScrollView
    private lateinit var clearLogsButton: Button
    
    // Event listeners (keep references for proper cleanup)
    private var connectListener: WuKongEventListener<ConnectResult>? = null
    private var disconnectListener: WuKongEventListener<DisconnectInfo>? = null
    private var messageListener: WuKongEventListener<Message>? = null
    private var errorListener: WuKongEventListener<WuKongError>? = null
    private var reconnectingListener: WuKongEventListener<Map<String, Any>>? = null
    
    // State tracking
    private var isConnected = false
    private var isConnecting = false
    private var areListenersRegistered = false
    private val messageHistory = mutableListOf<String>()
    private val logHistory = mutableListOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeSDK()
        createEventListeners() // Create listeners but don't add them to SDK yet
        setupClickListeners()
        setDefaultValues()
        updateConnectionUI()

        addLog("Example application loaded. Configure connection and click Connect.", LogLevel.INFO)
    }
    
    private fun initializeViews() {
        // Connection section
        serverUrlEdit = findViewById(R.id.serverUrlEdit)
        userIdEdit = findViewById(R.id.userIdEdit)
        tokenEdit = findViewById(R.id.tokenEdit)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        connectionIndicator = findViewById(R.id.connectionIndicator)
        
        // Messaging section
        targetChannelEdit = findViewById(R.id.targetChannelEdit)
        messageContentEdit = findViewById(R.id.messageContentEdit)
        sendButton = findViewById(R.id.sendButton)
        messageHistoryText = findViewById(R.id.messageHistoryText)
        messageHistoryScroll = findViewById(R.id.messageHistoryScroll)
        
        // Logs section
        logsText = findViewById(R.id.logsText)
        logsScroll = findViewById(R.id.logsScroll)
        clearLogsButton = findViewById(R.id.clearLogsButton)
    }
    
    private fun initializeSDK() {
        easySDK = WuKongEasySDK.getInstance()
    }
    
    private fun createEventListeners() {
        // Connection established
        connectListener = object : WuKongEventListener<ConnectResult> {
            override fun onEvent(result: ConnectResult) {
                Log.d(TAG, "Connected: $result")
                runOnUiThread {
                    isConnected = true
                    isConnecting = false
                    updateConnectionStatus("Connected", ConnectionState.CONNECTED)
                    updateConnectionUI()
                    addLog("Connection established successfully", LogLevel.SUCCESS)
                    addLog("Server info: ${result.serverKey}, TimeDiff: ${result.timeDiff}ms", LogLevel.INFO)
                }
            }
        }
        
        // Connection lost
        disconnectListener = object : WuKongEventListener<DisconnectInfo> {
            override fun onEvent(disconnectInfo: DisconnectInfo) {
                Log.d(TAG, "Disconnected: $disconnectInfo")
                runOnUiThread {
                    isConnected = false
                    isConnecting = false
                    updateConnectionStatus("Disconnected: ${disconnectInfo.reason}", ConnectionState.DISCONNECTED)
                    updateConnectionUI()
                    addLog("Disconnected - Code: ${disconnectInfo.code}, Reason: ${disconnectInfo.reason}", LogLevel.WARNING)
                }
            }
        }
        
        // Message received
        messageListener = object : WuKongEventListener<Message> {
            override fun onEvent(message: Message) {
                Log.d(TAG, "Message received: $message")
                runOnUiThread {
                    val messageText = try {
                        gson.toJson(message.payload)
                    } catch (e: Exception) {
                        message.payload.toString()
                    }
                    
                    addMessageToHistory("📥 Received from ${message.fromUid}", messageText, MessageType.RECEIVED)
                    addLog("Message received from ${message.fromUid}: $messageText", LogLevel.INFO)
                }
            }
        }
        
        // Error occurred
        errorListener = object : WuKongEventListener<WuKongError> {
            override fun onEvent(error: WuKongError) {
                Log.e(TAG, "Error: $error")
                runOnUiThread {
                    updateConnectionStatus("Error: ${error.message}", ConnectionState.ERROR)
                    addLog("Error occurred: ${error.code.description} - ${error.message}", LogLevel.ERROR)
                    
                    // Show user-friendly error message
                    showErrorToast(error)
                }
            }
        }
        
        // Reconnection attempt
        reconnectingListener = object : WuKongEventListener<Map<String, Any>> {
            override fun onEvent(data: Map<String, Any>) {
                val attempt = data["attempt"] as? Int ?: 0
                val delay = data["delay"] as? Long ?: 0
                Log.d(TAG, "Reconnecting: attempt $attempt, delay ${delay}ms")
                runOnUiThread {
                    updateConnectionStatus("Reconnecting... (attempt $attempt)", ConnectionState.RECONNECTING)
                    addLog("Reconnection attempt $attempt in ${delay}ms", LogLevel.WARNING)
                }
            }
        }
    }

    private fun registerEventListeners() {
        // Add listeners to SDK (only call this after SDK is initialized)
        if (!areListenersRegistered) {
            connectListener?.let { easySDK.addEventListener(WuKongEvent.CONNECT, it) }
            disconnectListener?.let { easySDK.addEventListener(WuKongEvent.DISCONNECT, it) }
            messageListener?.let { easySDK.addEventListener(WuKongEvent.MESSAGE, it) }
            errorListener?.let { easySDK.addEventListener(WuKongEvent.ERROR, it) }
            reconnectingListener?.let { easySDK.addEventListener(WuKongEvent.RECONNECTING, it) }
            areListenersRegistered = true
        }
    }
    
    private fun setupClickListeners() {
        connectButton.setOnClickListener {
            connectToServer()
        }
        
        disconnectButton.setOnClickListener {
            disconnectFromServer()
        }
        
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        clearLogsButton.setOnClickListener {
            clearLogs()
        }
    }
    
    private fun setDefaultValues() {
        serverUrlEdit.setText("ws://localhost:5200")
        userIdEdit.setText("testUser")
        tokenEdit.setText("testToken")
        targetChannelEdit.setText("friendUser")
        messageContentEdit.setText("{\"type\":1, \"content\":\"Hello!\"}")
    }
    
    private fun connectToServer() {
        val serverUrl = serverUrlEdit.text.toString().trim()
        val userId = userIdEdit.text.toString().trim()
        val token = tokenEdit.text.toString().trim()
        
        // Validate input
        if (serverUrl.isEmpty() || userId.isEmpty() || token.isEmpty()) {
            showErrorToast("Please fill in all connection fields")
            addLog("Connection failed: Missing required fields", LogLevel.ERROR)
            return
        }
        
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            showErrorToast("Server URL must start with ws:// or wss://")
            addLog("Connection failed: Invalid server URL format", LogLevel.ERROR)
            return
        }
        
        isConnecting = true
        updateConnectionStatus("Connecting...", ConnectionState.CONNECTING)
        updateConnectionUI()
        addLog("Attempting to connect to $serverUrl with UID: $userId", LogLevel.INFO)
        
        try {
            val config = WuKongConfig.Builder()
                .serverUrl(serverUrl)
                .uid(userId)
                .token(token)
                .deviceFlag(WuKongDeviceFlag.APP)
                .debugLogging(true)
                .connectionTimeout(10000)
                .requestTimeout(15000)
                .pingInterval(25000)
                .maxReconnectAttempts(5)
                .build()
            
            // Initialize SDK if not already done
            if (!easySDK.isInitialized()) {
                easySDK.init(this, config)
                addLog("SDK initialized with configuration", LogLevel.INFO)
            }

            // Register event listeners after SDK initialization
            registerEventListeners()
            addLog("Event listeners registered", LogLevel.INFO)
            
            lifecycleScope.launch {
                try {
                    easySDK.connect()
                    addLog("Connection request sent", LogLevel.INFO)
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    runOnUiThread {
                        isConnecting = false
                        updateConnectionStatus("Connection failed: ${e.message}", ConnectionState.ERROR)
                        updateConnectionUI()
                        addLog("Connection failed: ${e.message}", LogLevel.ERROR)
                        showErrorToast("Connection failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Configuration error", e)
            isConnecting = false
            updateConnectionStatus("Configuration error: ${e.message}", ConnectionState.ERROR)
            updateConnectionUI()
            addLog("Configuration error: ${e.message}", LogLevel.ERROR)
            showErrorToast("Configuration error: ${e.message}")
        }
    }
    
    private fun disconnectFromServer() {
        addLog("Disconnecting from server...", LogLevel.INFO)
        easySDK.disconnect()
        isConnected = false
        isConnecting = false
        updateConnectionStatus("Disconnecting...", ConnectionState.DISCONNECTING)
        updateConnectionUI()
    }

    private fun sendMessage() {
        val targetChannel = targetChannelEdit.text.toString().trim()
        val messageContent = messageContentEdit.text.toString().trim()

        if (!isConnected) {
            showErrorToast("Not connected to server")
            addLog("Send failed: Not connected to server", LogLevel.ERROR)
            return
        }

        if (targetChannel.isEmpty()) {
            showErrorToast("Please enter target channel ID")
            addLog("Send failed: Target channel ID is empty", LogLevel.ERROR)
            return
        }

        if (messageContent.isEmpty()) {
            showErrorToast("Please enter message content")
            addLog("Send failed: Message content is empty", LogLevel.ERROR)
            return
        }

        // Parse message content as JSON
        val payload = try {
            gson.fromJson(messageContent, Any::class.java)
        } catch (e: JsonSyntaxException) {
            // If not valid JSON, create a simple text message
            MessagePayload(type = 1, content = messageContent)
        }

        addLog("Sending message to $targetChannel: $messageContent", LogLevel.INFO)

        lifecycleScope.launch {
            try {
                val result = easySDK.send(
                    channelId = targetChannel,
                    channelType = WuKongChannelType.PERSON,
                    payload = payload
                )

                Log.d(TAG, "Message sent: $result")
                runOnUiThread {
                    addMessageToHistory("📤 Sent to $targetChannel", messageContent, MessageType.SENT)
                    addLog("Message sent successfully - ID: ${result.messageId}, Seq: ${result.messageSeq}", LogLevel.SUCCESS)

                    // Clear message input
                    messageContentEdit.setText("")

                    // Show success feedback
                    Toast.makeText(this@MainActivity, "Message sent!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send failed", e)
                runOnUiThread {
                    addLog("Send failed: ${e.message}", LogLevel.ERROR)
                    showErrorToast("Send failed: ${e.message}")
                }
            }
        }
    }

    private fun clearLogs() {
        logHistory.clear()
        logsText.text = ""
        addLog("Logs cleared", LogLevel.INFO)
    }

    private fun updateConnectionStatus(status: String, state: ConnectionState) {
        connectionStatusText.text = status

        val color = when (state) {
            ConnectionState.CONNECTED -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            ConnectionState.DISCONNECTED, ConnectionState.DISCONNECTING -> ContextCompat.getColor(this, android.R.color.darker_gray)
            ConnectionState.ERROR -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
        }

        connectionIndicator.setBackgroundColor(color)
        connectionStatusText.setTextColor(color)
    }

    private fun updateConnectionUI() {
        val canConnect = !isConnected && !isConnecting
        val canDisconnect = isConnected || isConnecting
        val canSend = isConnected

        connectButton.isEnabled = canConnect
        disconnectButton.isEnabled = canDisconnect
        sendButton.isEnabled = canSend

        // Disable connection fields when connected
        serverUrlEdit.isEnabled = canConnect
        userIdEdit.isEnabled = canConnect
        tokenEdit.isEnabled = canConnect

        // Enable/disable messaging fields
        targetChannelEdit.isEnabled = canSend
        messageContentEdit.isEnabled = canSend
    }

    private fun addMessageToHistory(header: String, content: String, type: MessageType) {
        val timestamp = dateFormat.format(Date())
        val message = "[$timestamp] $header\n$content\n"

        messageHistory.add(message)

        // Limit history size
        if (messageHistory.size > MAX_LOG_LINES) {
            messageHistory.removeAt(0)
        }

        // Update UI with colored text
        val spannable = SpannableStringBuilder()
        messageHistory.forEach { msg ->
            val start = spannable.length
            spannable.append(msg)
            spannable.append("\n")

            // Color code messages
            val color = when (type) {
                MessageType.SENT -> ContextCompat.getColor(this, android.R.color.holo_blue_dark)
                MessageType.RECEIVED -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            }
            spannable.setSpan(ForegroundColorSpan(color), start, spannable.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        messageHistoryText.text = spannable

        // Auto-scroll to bottom
        messageHistoryScroll.post {
            messageHistoryScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun addLog(message: String, level: LogLevel) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message"

        logHistory.add(logMessage)
        Log.d(TAG, logMessage)

        // Limit log history size
        if (logHistory.size > MAX_LOG_LINES) {
            logHistory.removeAt(0)
        }

        // Update UI with colored text
        val spannable = SpannableStringBuilder()
        logHistory.forEachIndexed { index, log ->
            val start = spannable.length
            spannable.append(log)
            spannable.append("\n")

            // Color code log levels
            val color = when (level) {
                LogLevel.ERROR -> Color.RED
                LogLevel.WARNING -> Color.rgb(255, 165, 0) // Orange
                LogLevel.SUCCESS -> Color.GREEN
                LogLevel.INFO -> Color.BLACK
            }

            if (index == logHistory.size - 1) { // Only color the latest message
                spannable.setSpan(ForegroundColorSpan(color), start, spannable.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        logsText.text = spannable

        // Auto-scroll to bottom
        logsScroll.post {
            logsScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showErrorToast(error: WuKongError) {
        val message = when (error.code) {
            com.wukongim.easysdk.enums.WuKongErrorCode.AUTH_FAILED -> "Authentication failed. Please check your credentials."
            com.wukongim.easysdk.enums.WuKongErrorCode.NETWORK_ERROR -> "Network error. Please check your connection."
            com.wukongim.easysdk.enums.WuKongErrorCode.CONNECTION_TIMEOUT -> "Connection timeout. Please try again."
            com.wukongim.easysdk.enums.WuKongErrorCode.INVALID_CHANNEL -> "Invalid channel or no permission."
            com.wukongim.easysdk.enums.WuKongErrorCode.MESSAGE_TOO_LARGE -> "Message is too large."
            com.wukongim.easysdk.enums.WuKongErrorCode.NOT_CONNECTED -> "Not connected to server."
            else -> error.message
        }
        showErrorToast(message)
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove event listeners to prevent memory leaks
        if (areListenersRegistered && easySDK.isInitialized()) {
            connectListener?.let { easySDK.removeEventListener(WuKongEvent.CONNECT, it) }
            disconnectListener?.let { easySDK.removeEventListener(WuKongEvent.DISCONNECT, it) }
            messageListener?.let { easySDK.removeEventListener(WuKongEvent.MESSAGE, it) }
            errorListener?.let { easySDK.removeEventListener(WuKongEvent.ERROR, it) }
            reconnectingListener?.let { easySDK.removeEventListener(WuKongEvent.RECONNECTING, it) }
            areListenersRegistered = false
        }

        // Disconnect if connected
        if (easySDK.isConnected()) {
            easySDK.disconnect()
        }

        addLog("Application destroyed, resources cleaned up", LogLevel.INFO)
    }

    // Enums for UI state management
    private enum class ConnectionState {
        CONNECTED, CONNECTING, DISCONNECTED, DISCONNECTING, RECONNECTING, ERROR
    }

    private enum class MessageType {
        SENT, RECEIVED
    }

    private enum class LogLevel {
        INFO, SUCCESS, WARNING, ERROR
    }
}
