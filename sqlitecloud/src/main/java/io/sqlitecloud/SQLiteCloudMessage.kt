package io.sqlitecloud

/// Represents a message that can be sent via SQLite Cloud.
///
/// A `SQLiteCloudMessage` encapsulates a message to be sent over SQLite Cloud. It includes
/// the message content, the target channel, and an option to create the channel if it does
/// not exist.
///
/// - Parameters:
///   - channel: A string specifying the target channel for the message.
///   - payload: The payload of the message.
///   - createChannelIfNotExist: A boolean value indicating whether to create the target
///                              channel if it does not exist. The default is `false`.
///
/// - Note: The `Payload` type must be serializable. Channels are used
///         to categorize messages, allowing clients to subscribe to specific channels of interest.
data class SQLiteCloudMessage<Payload>(
    val channel: String,
    val payload: Payload,
    val createChannelIfNotExist: Boolean,
)
