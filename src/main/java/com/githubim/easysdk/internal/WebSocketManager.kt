package com.githubim.easysdk.internal

import com.githubim.easysdk.WuKongConfig
import com.githubim.easysdk.exception.WuKongNetworkException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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
    private val config: WuKongConfig,
    private val logger: WuKongLogger = WuKongLogger(config.debugLogging)
) {

    private val stateLock = Any()

    /** The only connection whose callbacks may mutate manager state. */
    private var activeConnection: Connection? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionClosed: ((code: Int, reason: String) -> Unit)? = null
    var onConnectionError: ((Throwable) -> Unit)? = null

    /**
     * Connect to the WebSocket server.
     *
     * @return suspends until the WebSocket connection is established or fails
     */
    suspend fun connect(): Unit = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val connection = synchronized(stateLock) {
                if (activeConnection != null) {
                    null
                } else {
                    Connection(createOkHttpClient(), continuation).also {
                        activeConnection = it
                    }
                }
            }

            if (connection == null) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                completeRetirement(
                    retireConnection(connection, NORMAL_CLOSURE_STATUS, NORMAL_CLOSURE_REASON)
                )
            }

            if (!continuation.isActive) {
                return@suspendCancellableCoroutine
            }

            try {
                val request = Request.Builder()
                    .url(config.serverUrl)
                    .build()
                val listener = createWebSocketListener(connection)
                val socket = connection.client.newWebSocket(request, listener)
                val accepted = synchronized(stateLock) {
                    if (activeConnection === connection &&
                        (connection.socket == null || connection.socket === socket)
                    ) {
                        connection.socket = socket
                        true
                    } else {
                        false
                    }
                }

                if (!accepted) {
                    socket.cancel()
                }
            } catch (error: Exception) {
                failConnectionAttempt(connection, error)
            }
        }
    }

    /**
     * Disconnect from the WebSocket server.
     *
     * @param code WebSocket close code reported to the lifecycle callback
     * @param reason human-readable close reason
     */
    fun disconnect(
        code: Int = NORMAL_CLOSURE_STATUS,
        reason: String = NORMAL_CLOSURE_REASON
    ) {
        completeRetirement(retireConnection(null, code, reason))
    }

    /**
     * Send a message through the WebSocket.
     *
     * @param message the message to send
     * @return true if the message was queued successfully, false otherwise
     */
    fun sendMessage(message: String): Boolean {
        val socket = synchronized(stateLock) {
            activeConnection
                ?.takeIf { it.isOpen }
                ?.socket
        }
        return socket?.send(message) ?: false
    }

    /** Create an OkHttpClient with WebSocket-appropriate timeouts. */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(config.pingIntervalMs, TimeUnit.MILLISECONDS)
            .build()
    }

    /** Create a listener bound to exactly one connection attempt. */
    private fun createWebSocketListener(connection: Connection): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                var pending: CancellableContinuation<Unit>? = null
                val accepted = synchronized(stateLock) {
                    if (!isCurrentConnection(connection, webSocket, allowUnassignedSocket = true)) {
                        false
                    } else {
                        connection.socket = webSocket
                        connection.isOpen = true
                        isConnected = true
                        pending = connection.continuation
                        connection.continuation = null
                        true
                    }
                }

                if (!accepted) {
                    return
                }
                pending?.let { continuation ->
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                logger.debug("WebSocketManager", "WebSocket connection opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val messageHandler = synchronized(stateLock) {
                    if (isCurrentConnection(connection, webSocket) && connection.isOpen) {
                        onMessageReceived
                    } else {
                        null
                    }
                } ?: return

                logger.debug(
                    "WebSocketManager",
                    "Received WebSocket message (${text.length} chars)"
                )
                messageHandler.invoke(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                val isCurrent = synchronized(stateLock) {
                    isCurrentConnection(connection, webSocket)
                }
                if (isCurrent) {
                    logger.debug("WebSocketManager", "WebSocket closing (code: $code)")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                finishSocket(connection, webSocket, code, reason, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                finishSocket(connection, webSocket, null, null, t)
            }
        }
    }

    /** Atomically detach a connection before close and callback code can re-enter. */
    private fun retireConnection(
        expected: Connection?,
        code: Int,
        reason: String
    ): ConnectionRetirement? = synchronized(stateLock) {
        val connection = activeConnection ?: return@synchronized null
        if (expected != null && activeConnection !== expected) {
            return@synchronized null
        }

        activeConnection = null
        isConnected = false
        val pending = connection.continuation
        connection.continuation = null
        ConnectionRetirement(
            socket = connection.socket,
            client = connection.client,
            pending = pending,
            wasConnected = connection.isOpen,
            code = code,
            reason = reason
        )
    }

    private fun completeRetirement(retirement: ConnectionRetirement?) {
        if (retirement == null) {
            return
        }

        retirement.socket?.close(retirement.code, retirement.reason)
        shutdownClient(retirement.client)
        retirement.pending?.let { continuation ->
            if (continuation.isActive) {
                continuation.resumeWithException(WuKongNetworkException("Connection cancelled"))
            }
        }

        // The later OkHttp callback belongs to a retired connection and is
        // ignored, so the upper lifecycle receives this transition exactly once.
        if (retirement.wasConnected) {
            onConnectionClosed?.invoke(retirement.code, retirement.reason)
        }
    }

    private fun finishSocket(
        connection: Connection,
        socket: WebSocket,
        code: Int?,
        reason: String?,
        error: Throwable?
    ) {
        val termination = synchronized(stateLock) {
            if (!isCurrentConnection(connection, socket, allowUnassignedSocket = true)) {
                null
            } else {
                activeConnection = null
                isConnected = false
                val pending = connection.continuation
                connection.continuation = null
                SocketTermination(connection.client, pending, connection.isOpen)
            }
        } ?: return

        shutdownClient(termination.client)
        termination.pending?.let { continuation ->
            if (continuation.isActive) {
                val connectionError = if (error != null) {
                    WuKongNetworkException("WebSocket failure", error)
                } else {
                    WuKongNetworkException(
                        "Connection closed before authentication (Code: $code)"
                    )
                }
                continuation.resumeWithException(connectionError)
            }
        }

        if (error != null) {
            logger.error(
                "WebSocketManager",
                "WebSocket failure (${error.javaClass.simpleName})"
            )
            if (termination.wasConnected) {
                onConnectionError?.invoke(error)
            }
        } else {
            logger.debug("WebSocketManager", "WebSocket closed (code: $code)")
            if (termination.wasConnected) {
                onConnectionClosed?.invoke(code ?: ABNORMAL_CLOSURE_STATUS, reason.orEmpty())
            }
        }
    }

    private fun failConnectionAttempt(connection: Connection, error: Exception) {
        val pending = synchronized(stateLock) {
            if (activeConnection !== connection) {
                null
            } else {
                activeConnection = null
                isConnected = false
                connection.continuation.also {
                    connection.continuation = null
                }
            }
        }

        shutdownClient(connection.client)
        if (pending?.isActive == true) {
            pending.resumeWithException(
                WuKongNetworkException("Failed to create WebSocket", error)
            )
        }
    }

    /** Call only while holding [stateLock]. */
    private fun isCurrentConnection(
        connection: Connection,
        socket: WebSocket,
        allowUnassignedSocket: Boolean = false
    ): Boolean {
        return activeConnection === connection &&
            (connection.socket === socket || (allowUnassignedSocket && connection.socket == null))
    }

    private fun shutdownClient(client: OkHttpClient) {
        client.dispatcher.executorService.shutdown()
    }

    private class Connection(
        val client: OkHttpClient,
        var continuation: CancellableContinuation<Unit>?,
        var socket: WebSocket? = null,
        var isOpen: Boolean = false
    )

    private data class ConnectionRetirement(
        val socket: WebSocket?,
        val client: OkHttpClient,
        val pending: CancellableContinuation<Unit>?,
        val wasConnected: Boolean,
        val code: Int,
        val reason: String
    )

    private data class SocketTermination(
        val client: OkHttpClient,
        val pending: CancellableContinuation<Unit>?,
        val wasConnected: Boolean
    )

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val ABNORMAL_CLOSURE_STATUS = 1006
        private const val NORMAL_CLOSURE_REASON = "Client disconnected"
    }
}
