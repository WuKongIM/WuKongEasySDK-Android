package com.githubim.easysdk.internal

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * JSON-RPC Manager
 * 
 * Handles JSON-RPC protocol communication including request/response correlation,
 * timeout management, and notification handling.
 */
internal class JsonRpcManager {
    
    private val gson = Gson()
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    
    /**
     * Represents a pending JSON-RPC request
     */
    private data class PendingRequest(
        val continuation: CancellableContinuation<JsonObject>,
        val timeoutJob: Job
    )
    
    /**
     * JSON-RPC Request structure
     */
    private data class JsonRpcRequest(
        val method: String,
        val params: Any,
        val id: String
    )
    
    /**
     * JSON-RPC Response structure
     */
    private data class JsonRpcResponse(
        val result: JsonObject?,
        val error: JsonRpcError?,
        val id: String
    )
    
    /**
     * JSON-RPC Error structure
     */
    private data class JsonRpcError(
        val code: Int,
        val message: String,
        val data: Any?
    )
    
    /**
     * JSON-RPC Notification structure
     */
    private data class JsonRpcNotification(
        val method: String,
        val params: JsonObject
    )
    
    /**
     * Send a JSON-RPC request and wait for response
     * 
     * @param method The RPC method name
     * @param params The request parameters
     * @param timeoutMs Request timeout in milliseconds
     * @param sendMessage Function to send the serialized message
     * @return The response result
     */
    suspend fun sendRequest(
        method: String,
        params: Any,
        timeoutMs: Long,
        sendMessage: (String) -> Unit
    ): JsonObject = withContext(Dispatchers.IO) {
        val requestId = generateRequestId()
        val request = JsonRpcRequest(method, params, requestId)
        val requestJson = gson.toJson(request)
        
        suspendCancellableCoroutine<JsonObject> { continuation ->
            val timeoutJob = CoroutineScope(Dispatchers.IO).launch {
                delay(timeoutMs)
                pendingRequests.remove(requestId)
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        Exception("Request timeout for method $method (id: $requestId)")
                    )
                }
            }
            
            val pendingRequest = PendingRequest(continuation, timeoutJob)
            pendingRequests[requestId] = pendingRequest
            
            continuation.invokeOnCancellation {
                pendingRequests.remove(requestId)
                timeoutJob.cancel()
            }
            
            try {
                sendMessage(requestJson)
            } catch (e: Exception) {
                pendingRequests.remove(requestId)
                timeoutJob.cancel()
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    /**
     * Send a JSON-RPC notification (no response expected)
     * 
     * @param method The RPC method name
     * @param params The notification parameters
     * @param sendMessage Function to send the serialized message
     */
    fun sendNotification(
        method: String,
        params: Any,
        sendMessage: (String) -> Unit
    ) {
        val notification = JsonRpcNotification(method, gson.toJsonTree(params).asJsonObject)
        val notificationJson = gson.toJson(notification)
        sendMessage(notificationJson)
    }
    
    /**
     * Handle incoming JSON-RPC message
     * 
     * @param message The raw JSON message
     * @param onNotification Callback for handling notifications
     */
    fun handleMessage(
        message: String,
        onNotification: (method: String, params: JsonObject) -> Unit
    ) {
        try {
            val jsonObject = JsonParser.parseString(message).asJsonObject
            
            when {
                jsonObject.has("id") -> {
                    // It's a response
                    handleResponse(jsonObject)
                }
                jsonObject.has("method") -> {
                    // It's a notification
                    handleNotification(jsonObject, onNotification)
                }
                else -> {
                    android.util.Log.w("JsonRpcManager", "Unknown message format: $message")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("JsonRpcManager", "Failed to parse JSON-RPC message: $message", e)
        }
    }
    
    /**
     * Handle JSON-RPC response
     */
    private fun handleResponse(jsonObject: JsonObject) {
        val id = jsonObject.get("id")?.asString ?: return
        val pendingRequest = pendingRequests.remove(id) ?: return
        
        pendingRequest.timeoutJob.cancel()
        
        if (jsonObject.has("error")) {
            val errorObj = jsonObject.getAsJsonObject("error")
            val error = gson.fromJson(errorObj, JsonRpcError::class.java)
            val exception = Exception("JSON-RPC Error ${error.code}: ${error.message}")
            
            if (pendingRequest.continuation.isActive) {
                pendingRequest.continuation.resumeWithException(exception)
            }
        } else {
            val result = jsonObject.getAsJsonObject("result") ?: JsonObject()
            if (pendingRequest.continuation.isActive) {
                pendingRequest.continuation.resume(result)
            }
        }
    }
    
    /**
     * Handle JSON-RPC notification
     */
    private fun handleNotification(
        jsonObject: JsonObject,
        onNotification: (method: String, params: JsonObject) -> Unit
    ) {
        val method = jsonObject.get("method")?.asString ?: return
        val params = jsonObject.getAsJsonObject("params") ?: JsonObject()
        
        onNotification(method, params)
    }
    
    /**
     * Cancel all pending requests
     */
    fun cancelAllRequests() {
        pendingRequests.values.forEach { pendingRequest ->
            pendingRequest.timeoutJob.cancel()
            if (pendingRequest.continuation.isActive) {
                pendingRequest.continuation.resumeWithException(
                    Exception("Connection closed")
                )
            }
        }
        pendingRequests.clear()
    }
    
    /**
     * Generate a unique request ID
     */
    private fun generateRequestId(): String {
        return UUID.randomUUID().toString()
    }
}
