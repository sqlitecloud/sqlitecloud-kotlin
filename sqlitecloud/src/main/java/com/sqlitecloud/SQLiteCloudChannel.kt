package com.sqlitecloud

sealed class SQLiteCloudChannel(open val name: String) {
    data class Channel(override val name: String): SQLiteCloudChannel(name)
    data class Table(override val name: String): SQLiteCloudChannel(name)
    object AllTables: SQLiteCloudChannel(name = "*")
}
