package websocket

import com.soywiz.korio.async.*
import com.soywiz.korio.net.ws.WebSocketClient
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.window
import kotlin.js.Json




class AutoWebSocketClient(url: String, protocols: List<String>? = null, val DEBUG: Boolean=false) :
        WebSocketClient(url, protocols, true) {

    private lateinit var jsws: WebSocket

    fun reconnect() {
        window.setTimeout({
            cleanup()
            connect()
        }, 200)
    }

    private fun cleanup() {
        jsws.close()
        jsws.onerror = null
        jsws.onopen = null
        jsws.onclose = null
        jsws.onmessage = null
    }

    private val _onopen = { e: Event -> onOpen(Unit) }

    private val _onerror = { e: dynamic ->
        onError(Exception())
    }

    private val _onclose = { e: Event ->
        val event = e as CloseEvent

        var code = event.code.toInt()
        var reason = event.reason
        var wasClean = event.wasClean

        if (code != 1000) {
            reconnect()
        }
        onClose(Unit)
    }

    internal val _onmessage = { e: Event ->
        val event = e as MessageEvent
        val data = event.data
        if (DEBUG) println("[WS-RECV]: $data :: stringListeners=${onStringMessage.listenerCount}, binaryListeners=${onBinaryMessage.listenerCount}, anyListeners=${onAnyMessage.listenerCount}")
        if (data is String) {
            val js = data
            onStringMessage(js)
            onAnyMessage(js)
        } else {
            val jb = data

            //onBinaryMessage(jb)
            //onAnyMessage(jb)
            TODO("onBinaryMessage, onAnyMessage")
        }
    }

    fun connect() {
        jsws = if (protocols != null) {
            WebSocket(url, arrayOf(*protocols.toTypedArray()))
        } else {
            WebSocket(url)
        }.apply {
            this.binaryType = BinaryType.ARRAYBUFFER
            this.onerror = _onerror
            this.onopen = _onopen
            this.onclose = _onclose
            this.onmessage = _onmessage
        }
    }

    override fun close(code: Int, reason: String) {
        //jsws.methods["close"](code, reason)
        jsws.close()
    }

    override suspend fun send(message: String) {
        if (DEBUG) println("[WS-SEND]: $message")
        try {
            jsws.send(message)
        } catch (e: Exception) {
            //jsws.dispatchEvent(ErrorEvent)
        }
    }

    override suspend fun send(message: ByteArray) {
        if (DEBUG) println("[WS-SEND]: ${message.toList()}")
        val bb = Int8Array(message.size)
        for (n in message.indices) bb[n] = message[n]
        jsws.send(bb)
    }
}

