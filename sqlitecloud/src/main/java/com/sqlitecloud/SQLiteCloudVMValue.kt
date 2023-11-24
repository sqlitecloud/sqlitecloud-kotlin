package com.sqlitecloud

import java.nio.ByteBuffer
import java.nio.charset.Charset

sealed class SQLiteCloudVMValue {
    data class Integer(val value: Int): SQLiteCloudVMValue()
    data class Integer64(val value: Long): SQLiteCloudVMValue()
    data class Double(val value: kotlin.Double): SQLiteCloudVMValue()
    data class String(val value: kotlin.String): SQLiteCloudVMValue()

    data class Blob(val value: ByteBuffer): SQLiteCloudVMValue()

    data object BlobZero: SQLiteCloudVMValue()

    data object Null: SQLiteCloudVMValue()

    val stringByteSize: Int
        get() = when (this) {
            is String -> Charset.defaultCharset().encode(value).limit()
            else -> 0
        }
}