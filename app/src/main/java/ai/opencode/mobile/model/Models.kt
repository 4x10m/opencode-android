package ai.opencode.mobile.model

import org.json.JSONObject

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parentID: String? = null,
    val status: SessionStatus = SessionStatus.IDLE,
    val isShared: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): Session {
            return Session(
                id = json.optString("id", ""),
                title = json.optString("title", "Untitled"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                parentID = json.optString("parentID").takeIf { it.isNotEmpty() },
                status = SessionStatus.fromString(json.optString("status", "idle")),
                isShared = json.optBoolean("isShared", false)
            )
        }
    }
}

enum class SessionStatus {
    IDLE, RUNNING, ERROR, ABORTED;
    
    companion object {
        fun fromString(status: String): SessionStatus {
            return when (status.lowercase()) {
                "running" -> RUNNING
                "error" -> ERROR
                "aborted" -> ABORTED
                else -> IDLE
            }
        }
    }
}

data class MessageInfo(
    val id: String,
    val sessionID: String,
    val role: String,
    val createdAt: Long,
    val model: String? = null,
    val agent: String? = null
)

data class MessagePart(
    val type: String,
    val content: String,
    val toolCall: ToolCall? = null,
    val toolResult: ToolResult? = null
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)

data class ToolResult(
    val toolCallId: String,
    val content: String,
    val isError: Boolean = false
)

data class ApiMessage(
    val info: MessageInfo,
    val parts: List<MessagePart>
) {
    fun getTextContent(): String {
        return parts.filter { it.type == "text" }
            .joinToString("\n") { it.content }
    }
}

data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val modifiedAt: Long = System.currentTimeMillis()
)

data class FileContent(
    val path: String,
    val content: String,
    val encoding: String = "utf-8"
)

data class FileDiff(
    val path: String,
    val oldContent: String?,
    val newContent: String,
    val isDeleted: Boolean = false
)

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val isGit: Boolean = false
)

data class Provider(
    val id: String,
    val name: String,
    val isConnected: Boolean = false,
    val models: List<String> = emptyList()
)

data class Command(
    val name: String,
    val description: String,
    val arguments: List<CommandArgument> = emptyList()
)

data class CommandArgument(
    val name: String,
    val type: String,
    val required: Boolean,
    val description: String
)

data class ServerHealth(
    val healthy: Boolean,
    val version: String
)

data class ServerEvent(
    val type: String,
    val data: JSONObject
)
