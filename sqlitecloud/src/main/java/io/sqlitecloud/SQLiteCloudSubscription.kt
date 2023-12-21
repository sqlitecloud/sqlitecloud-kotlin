package io.sqlitecloud

class SQLiteCloudSubscription(
    val channel: SQLiteCloudChannel,
    val callback: NotificationHandler,
    private val onUnsubscribe: Callback<SQLiteCloudChannel>,
)
