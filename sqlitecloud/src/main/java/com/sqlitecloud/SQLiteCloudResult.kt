package com.sqlitecloud

sealed class SQLiteCloudResult(open val value: Any?) {
    data object Success : SQLiteCloudResult(null)

    data class Json(override val value: String) : SQLiteCloudResult(value)

    data class Value(override val value: SQLiteCloudValue) : SQLiteCloudResult(value)

    data class Array(override val value: List<SQLiteCloudValue>) : SQLiteCloudResult(value)

    data class Rowset(override val value: SQLiteCloudRowset) : SQLiteCloudResult(value)

    val stringValue: String?
        get() = when (this) {
            is Value -> value.stringValue
            is Json -> value
            is Array -> value.toString()
            is Rowset -> value.toString()
            is Success -> "success"
        }

    enum class Type(val rawValue: Int) {
        OK(0),
        ERROR(1),
        STRING(2),
        INTEGER(3),
        FLOAT(4),
        ROWSET(5),
        ARRAY(6),
        NULL(7),
        JSON(8),
        BLOB(9);

        companion object {
            fun fromRawValue(rawValue: Int): Type = values().first { it.rawValue == rawValue }
        }
    }
}
