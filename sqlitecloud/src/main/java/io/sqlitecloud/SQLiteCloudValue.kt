package io.sqlitecloud

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

sealed class SQLiteCloudValue(open val value: Any?, internal val typeValue: Int) {
    data class Integer(override val value: Long) :
        SQLiteCloudValue(value, typeValue = Type.Integer.rawValue)

    data class Double(override val value: kotlin.Double) :
        SQLiteCloudValue(value, typeValue = Type.Double.rawValue)

    data class String(override val value: kotlin.String) :
        SQLiteCloudValue(value, typeValue = Type.String.rawValue)

    data class Blob(override val value: ByteBuffer) :
        SQLiteCloudValue(value, typeValue = Type.Blob.rawValue)

    data object Null : SQLiteCloudValue(null, typeValue = Type.Null.rawValue)

    val stringValue: kotlin.String?
        get() = when (this) {
            is Integer -> "$value"
            is Double -> "$value"
            is String -> value
            is Blob -> byteBufferToString(value)
            is Null -> null
        }

    val wrapped: ByteBuffer?
        get() {
            val buffer = when (this) {
                is Integer -> ByteBuffer.allocateDirect(8).putLong(value)
                is Double -> ByteBuffer.allocateDirect(8).putDouble(value)
                is String -> {
                    val buf = Charset.defaultCharset().encode(value)
                    ByteBuffer.allocateDirect(buf.limit()).put(buf)
                }
                is Blob -> value.run {
                    position(limit())
                    this
                }
                is Null -> null
            }
            buffer?.flip()
            return buffer
        }

    companion object {
        private fun byteBufferToString(byteBuffer: ByteBuffer): kotlin.String {
            val charset = Charset.defaultCharset()
            val decoder = charset.newDecoder()
            decoder.onMalformedInput(CodingErrorAction.REPLACE)
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE)
            val charBuffer = decoder.decode(byteBuffer)
            return charBuffer.toString()
        }
    }

    enum class Type(val rawValue: Int) {
        Integer(1),
        Double(2),
        String(3),
        Blob(4),
        Null(5),
        Unknown(-1);

        companion object {
            fun fromRawValue(rawValue: Int): Type = values().first { it.rawValue == rawValue }
        }
    }
}
