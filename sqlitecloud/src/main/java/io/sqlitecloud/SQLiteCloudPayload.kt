package io.sqlitecloud

import kotlinx.serialization.Serializable

@Serializable
data class SQLiteCloudPayload(
    val sender: String,
    val channel: String,
    val messageType: MessageType,
    val pk: List<String>,
    val payload: String?,
) {
    enum class MessageType(val rawValue: String) {
        Table("TABLE"),
        Message("MESSAGE"),
        Insert("INSERT"),
        Update("UPDATE"),
        Delete("DELETE"),
        NotSupported(""),
    }

    override fun toString(): String {
        return """
        - sender: $sender
        - channel: $channel
        - message type: ${messageType.rawValue}
        - pk: $pk
        - payload: ${payload ?: "(empty)"}
        """
    }
}