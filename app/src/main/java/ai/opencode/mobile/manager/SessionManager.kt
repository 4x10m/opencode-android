package ai.opencode.mobile.manager

import ai.opencode.mobile.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.MessageEvent
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SessionManager private constructor() {
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private var currentEventSource: EventSource? = null

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }

    fun setSessions(sessions: List<Session>) {
        _sessions.value = sessions
    }

    fun setCurrentSession(session: Session?) {
        _currentSession.value = session
    }

    fun updateSessionStatus(sessionId: String, status: SessionStatus) {
        _sessions.value = _sessions.value.map {
            if (it.id == sessionId) it.copy(status = status) else it
        }
        if (_currentSession.value?.id == sessionId) {
            _currentSession.value = _currentSession.value?.copy(status = status)
        }
    }

    fun addSession(session: Session) {
        _sessions.value = _sessions.value + session
    }

    fun addMessage(sessionId: String, message: Message) {
        val currentMessages = _messages.value[sessionId] ?: emptyList()
        _messages.value = _messages.value + (sessionId to (currentMessages + message))
    }

    fun getMessages(sessionId: String): List<Message> {
        return _messages.value[sessionId] ?: emptyList()
    }

    fun connectToEventStream(
        serverUrl: String,
        username: String,
        password: String,
        onEvent: (ServerEvent) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        currentEventSource?.close()

        val handler = object : EventHandler {
            override fun onOpen() {
                // Connected
            }

            override fun onClosed() {
                // Connection closed
            }

            override fun onMessage(event: String?, messageEvent: MessageEvent) {
                try {
                    val json = JSONObject(messageEvent.data)
                    onEvent(ServerEvent(event ?: "message", json))
                } catch (e: Exception) {
                    onEvent(ServerEvent("raw", JSONObject().put("data", messageEvent.data)))
                }
            }

            override fun onComment(comment: String) {
                // Comment received
            }

            override fun onError(t: Throwable) {
                onError(t)
            }
        }

        val eventSourceBuilder = EventSource.Builder(handler, java.net.URI.create("$serverUrl/event"))
            .client(client)
            .headers(Headers.Builder()
                .add("Authorization", Credentials.basic(username, password))
                .add("Accept", "text/event-stream")
                .build())

        currentEventSource = eventSourceBuilder.build()
        currentEventSource?.start()
    }

    fun disconnectEventStream() {
        currentEventSource?.close()
        currentEventSource = null
    }

    fun clear() {
        _sessions.value = emptyList()
        _currentSession.value = null
        _messages.value = emptyMap()
    }
}
