@file:OptIn(ExperimentalStdlibApi::class)

package com.sqlitecloud

import com.sqlitecloud.SQLiteCloudResult.Type.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import java.lang.Long.min
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

typealias OpaquePointer<T> = ByteBuffer

internal object SQLiteCloudConnection

internal object SQLiteCloudBlob

internal class SQLiteCloudBridge(val logger: SQLiteCloudLogger?) {
    private var connection: OpaquePointer<SQLiteCloudConnection>? = null
    private var pubSubCallback: ((SQLiteCloudResult) -> Unit)? = null

    val isConnected: Boolean
        get() = connection != null && !isError()

    external fun isError(): Boolean

    external fun isSQLiteError(): Boolean

    external fun errorCode(): Int?

    external fun errorMessage(): String?

    external fun extendedErrorCode(): Int?

    external fun errorOffset(): Int?

    fun isVmError(vm: OpaquePointer<SQLiteCloudVM>): Boolean {
        val code = vmErrorCode(vm)
        return code != null && code != 0
    }

    private external fun vmErrorCode(vm: OpaquePointer<SQLiteCloudVM>): Int?

    private external fun vmErrorMessage(vm: OpaquePointer<SQLiteCloudVM>): String?

    private external fun doConnect(
        hostname: String,
        port: Int,
        username: String,
        password: String,
        database: String?,
        timeout: Int,
        family: Int,
        compression: Boolean,
        sqliteMode: Boolean,
        zeroText: Boolean,
        passwordHashed: Boolean,
        nonlinearizable: Boolean,
        dbMemory: Boolean,
        noBlob: Boolean,
        dbCreate: Boolean,
        maxData: Int,
        maxRows: Int,
        maxRowset: Int,
        tlsRootCertificate: String?,
        tlsCertificate: String?,
        tlsCertificateKey: String?,
        insecure: Boolean,
    ): OpaquePointer<SQLiteCloudConnection>

    fun connect(
        hostname: String,
        port: Int,
        username: String,
        password: String,
        database: String?,
        timeout: Int,
        family: Int,
        compression: Boolean,
        sqliteMode: Boolean,
        zeroText: Boolean,
        passwordHashed: Boolean,
        nonlinearizable: Boolean,
        dbMemory: Boolean,
        noBlob: Boolean,
        dbCreate: Boolean,
        maxData: Int,
        maxRows: Int,
        maxRowset: Int,
        tlsRootCertificate: String?,
        tlsCertificate: String?,
        tlsCertificateKey: String?,
        insecure: Boolean,
    ): Boolean {
        connection = doConnect(
            hostname = hostname,
            port = port,
            username = username,
            password = password,
            database = database,
            timeout = timeout,
            family = family,
            compression = compression,
            sqliteMode = sqliteMode,
            zeroText = zeroText,
            passwordHashed = passwordHashed,
            nonlinearizable = nonlinearizable,
            dbMemory = dbMemory,
            noBlob = noBlob,
            dbCreate = dbCreate,
            maxData = maxData,
            maxRows = maxRows,
            maxRowset = maxRowset,
            tlsRootCertificate = tlsRootCertificate,
            tlsCertificate = tlsCertificate,
            tlsCertificateKey = tlsCertificateKey,
            insecure = insecure,
        )
        return !isError()
    }

    private external fun doDisconnect()

    fun disconnect() {
        doDisconnect()
        connection?.clear()
        connection = null
    }

    external fun getClientUUID(): String?

    private external fun executeCommand(query: String): OpaquePointer<SQLiteCloudResult>?

    private external fun executeArrayCommand(
        query: String,
        params: Array<ByteBuffer>,
        paramTypes: IntArray,
    ): OpaquePointer<SQLiteCloudResult>?

    private external fun freeResult(result: OpaquePointer<SQLiteCloudResult>)

    private external fun resultType(result: OpaquePointer<SQLiteCloudResult>): Int

    private external fun intResult(result: OpaquePointer<SQLiteCloudResult>): Int

    private external fun longResult(result: OpaquePointer<SQLiteCloudResult>): Long

    private external fun doubleResult(result: OpaquePointer<SQLiteCloudResult>): Double

    private external fun stringResult(result: OpaquePointer<SQLiteCloudResult>): String

    private external fun bufferResult(result: OpaquePointer<SQLiteCloudResult>): ByteBuffer

    private external fun arrayResultSize(result: OpaquePointer<SQLiteCloudResult>): Int

    private external fun arrayResultValueType(
        result: OpaquePointer<SQLiteCloudResult>,
        index: Int,
    ): Int

    private external fun arrayResultLongValue(
        result: OpaquePointer<SQLiteCloudResult>,
        index: Int,
    ): Long

    private external fun arrayResultDoubleValue(
        result: OpaquePointer<SQLiteCloudResult>,
        index: Int,
    ): Double

    private external fun arrayResultStringValue(
        result: OpaquePointer<SQLiteCloudResult>,
        index: Int,
    ): String

    private external fun arrayResultBufferValue(
        result: OpaquePointer<SQLiteCloudResult>,
        index: Int,
    ): ByteBuffer

    private external fun rowsetResultRowCount(result: OpaquePointer<SQLiteCloudResult>): Int

    private external fun rowsetResultColumnCount(result: OpaquePointer<SQLiteCloudResult>): Int

    private external fun rowsetResultColumnName(
        result: OpaquePointer<SQLiteCloudResult>,
        column: Int,
    ): String

    private external fun rowsetResultValueType(
        result: OpaquePointer<SQLiteCloudResult>,
        row: Int,
        column: Int,
    ): Int

    private external fun rowsetResultLongValue(
        result: OpaquePointer<SQLiteCloudResult>,
        row: Int,
        column: Int,
    ): Long

    private external fun rowsetResultDoubleValue(
        result: OpaquePointer<SQLiteCloudResult>,
        row: Int,
        column: Int,
    ): Double

    private external fun rowsetResultStringValue(
        result: OpaquePointer<SQLiteCloudResult>,
        row: Int,
        column: Int,
    ): String

    private external fun rowsetResultBufferValue(
        result: OpaquePointer<SQLiteCloudResult>,
        row: Int,
        column: Int,
    ): ByteBuffer

    fun execute(command: SQLiteCloudCommand): SQLiteCloudResult {
        val nativeResult = if (command.parameters.isEmpty()) {
            executeCommand(command.query)
        } else {
            val params = command.parameters.mapNotNull {
                if (it is SQLiteCloudValue.Null) {
                    null
                } else {
                    it.wrapped
                }
            }.toTypedArray()
            val types = command.parameters.mapNotNull {
                if (it is SQLiteCloudValue.Null) {
                    null
                } else {
                    it.typeValue
                }
            }.toIntArray()
            executeArrayCommand(command.query, params, types)
        }

        // If the result is null, there was an error either during the
        // connection (e.g. invalid credentials) or during the execution
        // of the query (e.g. database does not exist.)
        if (nativeResult == null) {
            val error = error()
            logger?.logError(
                category = "COMMAND",
                message = "🚨 '${command.query}' command failed: $error",
            )
            throw error
        }

        // Try parsing the result. Parsing can throw several errors.
        val result = parseResult(nativeResult)
        // Before returning the result it is necessary to free the opaque pointer.
        freeResult(nativeResult)

        logger?.logInfo(
            category = "COMMAND",
            message = "🚀 '${command.query}' command executed successfully",
        )

        return result
    }

    fun setPubSubCallback(callback: (SQLiteCloudResult) -> Unit) {
        pubSubCallback = callback
        setPubSubCallback()
    }

    fun pubSubCallback(result: OpaquePointer<SQLiteCloudResult>) {
        val liteResult = parseResult(result)
        pubSubCallback?.invoke(liteResult)
    }

    private external fun setPubSubCallback()

    external fun setPubSubOnly(): OpaquePointer<SQLiteCloudResult>

    private fun openBlob(
        info: SQLiteCloudBlobInfo,
        rowId: Long,
        readWrite: Boolean,
    ): OpaquePointer<SQLiteCloudBlob> {
        val handle = openBlob(info.schema, info.table, info.column, rowId, readWrite)

        if (handle == null) {
            val error = error()
            logger?.logError(category = "BLOB", message = "🚨 Blob Open failed: $error")
            throw error
        }

        return handle
    }

    private fun reopenBlobIfNeeded(
        handle: OpaquePointer<SQLiteCloudBlob>,
        rowIndex: Int,
        rowId: Long,
    ) {
        if (rowIndex > 0) {
            val success = reopenBlob(handle, rowId = rowId)
            if (isError()) {
                val error = error()
                logger?.logError(
                    category = "BLOB",
                    message = "🚨 ReOpen blob handler failed: $error",
                )
                throw error
            }
            if (!success) {
                throw throw SQLiteCloudError.Task.invalidNumberOfRows
            }
        }
    }

    fun blobFieldSizes(blobInfo: SQLiteCloudBlobInfo, rowIds: List<Long>): List<Int> {
        val firstRowId = rowIds.firstOrNull() ?: throw SQLiteCloudError.Task.invalidNumberOfRows

        val handle = openBlob(info = blobInfo, rowId = firstRowId, readWrite = false)

        val sizes = rowIds.mapIndexed { index, rowId ->
            reopenBlobIfNeeded(handle, index, rowId)
            blobFieldSize(handle)
        }

        closeBlob(handle)

        return sizes
    }

    fun readBlob(
        blob: SQLiteCloudBlobStructure<BlobIO.Read>,
        progressHandler: ProgressHandler?,
    ): List<BlobIO.Write> {
        val firstRow = blob.rows.firstOrNull() ?: return emptyList()

        val handle = openBlob(info = blob.info, rowId = firstRow.id, readWrite = false)

        val result = blob.rows.mapIndexed { index, row ->
            reopenBlobIfNeeded(handle, index, row.id)

            val internalProgress: ProgressHandler = { progress ->
                val total = blob.rows.count()
                val totalProgress = (progress + index.toDouble()) / total.toDouble()
                if (progressHandler != null) {
                    progressHandler(totalProgress)
                }
            }

            try {
                val totalSize = blobFieldSize(handle)
                val result = SQLiteCloudBlobReadWrite.readRow(blob, row, totalSize, internalProgress) { buffer ->
                    val bufferSlice = buffer.slice()
                    val result = readBlob(handle, bufferSlice)
                    if (isError()) {
                        throw error()
                    }
                    if (result < 0) {
                        val error = SQLiteCloudError.Task.invalidBlobSizeRead
                        logger?.logError(category = "BLOB", message = "🚨 Blob read failed: $error")
                        throw error
                    }
                    buffer.position(buffer.position() + bufferSlice.capacity())
                    bufferSlice.capacity()
                }

                logger?.logDebug(
                    category = "BLOB",
                    message = "🗃️ Blob reading successful - rowId: ${row.id} - bytes: $totalSize",
                )

                result
            } catch (e: Error) {
                throw error()
            }
        }

        closeBlob(handle)

        return result
    }

    fun updateBlob(
        blob: SQLiteCloudBlobStructure<BlobIO.Write>,
        progressHandler: ProgressHandler?,
    ) {
        val firstRow = blob.rows.firstOrNull() ?: return

        val handle = openBlob(info = blob.info, rowId = firstRow.id, readWrite = true)

        blob.rows.forEachIndexed { index, row ->
            reopenBlobIfNeeded(handle, index, row.id)

            // We need to check whether the blob field is large enough to store the entire data.
            // If it is not large enough we need to "expand" the column via a sql query.
            val currentFieldSize = blobFieldSize(handle)
            val dataSize = row.dataIO.size

            if (currentFieldSize < dataSize && blob.autoIncreaseFieldSize) {
                val info = blob.info
                try {
                    val result = execute(
                        command = SQLiteCloudCommand.expandBlobField(
                            table = info.table,
                            column = info.column,
                            rowId = row.id,
                            size = dataSize,
                        )
                    )
                    println(result.stringValue)
                } catch (e: Error) {
                    logger?.logDebug(
                        category = "BLOB",
                        message = "🚨 Blob field size increase failed - rowId: ${row.id} - bytes: $dataSize",
                    )
                    throw error()
                }

                // After increasing the blob size, it is necessary to reopen the handle in order
                // to acknowledge the change.
                reopenBlob(handle, row.id)
            }

            val internalProgress: ProgressHandler = { progress ->
                val total = blob.rows.count()
                val totalProgress = (progress + index.toDouble()) / total.toDouble()
                if (progressHandler != null) {
                    progressHandler(totalProgress)
                }
            }

            try {
                SQLiteCloudBlobReadWrite.writeRow(blob, row, internalProgress) { buffer ->
                    val bufferSlice = buffer.slice()
                    val result = writeBlob(handle, bufferSlice)
                    if (result < 1) {
                        val nativeError = error()
                        logger?.logError(
                            category = "BLOB",
                            message = "🚨 Blob writing failed: $nativeError",
                        )
                        throw SQLiteCloudError.Task.errorWritingBlob
                    }
                    buffer.position(buffer.position() + bufferSlice.capacity())
                    bufferSlice.capacity()
                }
            } catch (e: Error) {
                throw error()
            }

            // Checks if writing is failed.
            if (isError()) {
                val error = error()
                logger?.logError(category = "BLOB", message = "🚨 Blob writing failed: $error")
                closeBlob(handle)
                throw error
            }

            logger?.logDebug(
                category = "BLOB",
                message = "🗃️ Blob writing successful - rowId ${row.id} - bytes: $dataSize",
            )
        }

        closeBlob(handle)
    }

    // The SQCloudBlobOpen interface opens a BLOB for incremental I/O. This interfaces opens a
    // handle to the BLOB located in row rowid, column colname, table tablename in database dbname;
    // in other words, the same BLOB that would be selected by:
    // SELECT colname FROM dbname.tablename WHERE rowid = rowid;
    private external fun openBlob(
        schema: String?,
        table: String,
        column: String,
        rowId: Long,
        readWrite: Boolean,
    ): OpaquePointer<SQLiteCloudBlob>?

    private external fun reopenBlob(handle: OpaquePointer<SQLiteCloudBlob>, rowId: Long): Boolean

    private external fun closeBlob(handle: OpaquePointer<SQLiteCloudBlob>): Boolean

    private external fun blobFieldSize(handle: OpaquePointer<SQLiteCloudBlob>): Int

    private external fun readBlob(handle: OpaquePointer<SQLiteCloudBlob>, buffer: ByteBuffer): Int

    private external fun writeBlob(handle: OpaquePointer<SQLiteCloudBlob>, buffer: ByteBuffer): Int

    external fun vmCompile(query: String): OpaquePointer<SQLiteCloudVM>?

    external fun vmClose(vm: OpaquePointer<SQLiteCloudVM>): Boolean

    external fun vmBindInt(vm: OpaquePointer<SQLiteCloudVM>, rowIndex: Int, value: Int): Boolean

    external fun vmBindInt64(
        vm: OpaquePointer<SQLiteCloudVM>,
        rowIndex: Int,
        value: Long,
    ): Boolean

    external fun vmBindDouble(
        vm: OpaquePointer<SQLiteCloudVM>,
        rowIndex: Int,
        value: Double,
    ): Boolean

    external fun vmBindText(
        vm: OpaquePointer<SQLiteCloudVM>,
        rowIndex: Int,
        value: String,
        byteSize: Int,
    ): Boolean

    external fun vmBindBlob(
        vm: OpaquePointer<SQLiteCloudVM>,
        rowIndex: Int,
        value: ByteBuffer,
    ): Boolean

    external fun vmBindZeroBlob(vm: OpaquePointer<SQLiteCloudVM>, rowIndex: Int): Boolean

    external fun vmBindNull(vm: OpaquePointer<SQLiteCloudVM>, rowIndex: Int): Boolean

    external fun vmStep(vm: OpaquePointer<SQLiteCloudVM>): Int

    external fun vmColumnCount(vm: OpaquePointer<SQLiteCloudVM>): Int

    external fun vmLastRowID(vm: OpaquePointer<SQLiteCloudVM>): Long

    external fun vmChanges(vm: OpaquePointer<SQLiteCloudVM>): Long

    external fun vmTotalChanges(vm: OpaquePointer<SQLiteCloudVM>): Long

    external fun vmIsReadOnly(vm: OpaquePointer<SQLiteCloudVM>): Boolean

    external fun vmIsExplain(vm: OpaquePointer<SQLiteCloudVM>): Int

    external fun vmIsFinalized(vm: OpaquePointer<SQLiteCloudVM>): Boolean

    external fun vmBindParameterCount(vm: OpaquePointer<SQLiteCloudVM>): Int

    external fun vmBindParameterIndex(vm: OpaquePointer<SQLiteCloudVM>, name: String): Int

    external fun vmBindParameterName(vm: OpaquePointer<SQLiteCloudVM>, index: Int): String?

    external fun vmColumnType(vm: OpaquePointer<SQLiteCloudVM>, index: Int): Int

    external fun vmResult(vm: OpaquePointer<SQLiteCloudVM>): OpaquePointer<SQLiteCloudResult>

    external fun rowsetColumnName(result: OpaquePointer<SQLiteCloudResult>, index: Int): String

    external fun vmColumnInt64(vm: OpaquePointer<SQLiteCloudVM>, index: Int): Long

    external fun vmColumnDouble(vm: OpaquePointer<SQLiteCloudVM>, index: Int): Double

    external fun vmColumnText(vm: OpaquePointer<SQLiteCloudVM>, index: Int): String

    external fun vmColumnBlob(vm: OpaquePointer<SQLiteCloudVM>, index: Int): ByteBuffer

    private fun parseResult(result: OpaquePointer<SQLiteCloudResult>): SQLiteCloudResult {
        val resultType = SQLiteCloudResult.Type.fromRawValue(resultType(result))
        return when (resultType) {
            OK -> SQLiteCloudResult.Success
            NULL -> SQLiteCloudResult.Value(SQLiteCloudValue.Null)
            INTEGER -> SQLiteCloudResult.Value(SQLiteCloudValue.Integer(longResult(result)))
            FLOAT -> SQLiteCloudResult.Value(SQLiteCloudValue.Double(doubleResult(result)))
            STRING -> SQLiteCloudResult.Value(SQLiteCloudValue.String(stringResult(result)))
            JSON -> SQLiteCloudResult.Json(stringResult(result))
            BLOB -> SQLiteCloudResult.Value(SQLiteCloudValue.Blob(bufferResult(result)))
            ARRAY -> SQLiteCloudResult.Array(parseArrayResult(result))
            ROWSET -> SQLiteCloudResult.Rowset(parseRowsetResult(result))
            ERROR -> throw error()
        }
    }

    private fun parseArrayResult(array: OpaquePointer<SQLiteCloudResult>): List<SQLiteCloudValue> {
        val count = arrayResultSize(array)
        return (0..<count).map { index ->
            val valueType = SQLiteCloudValue.Type.fromRawValue(arrayResultValueType(array, index))
            when (valueType) {
                SQLiteCloudValue.Type.Integer -> {
                    SQLiteCloudValue.Integer(arrayResultLongValue(array, index))
                }

                SQLiteCloudValue.Type.Double -> {
                    SQLiteCloudValue.Double(arrayResultDoubleValue(array, index))
                }

                SQLiteCloudValue.Type.String -> {
                    SQLiteCloudValue.String(arrayResultStringValue(array, index))
                }

                SQLiteCloudValue.Type.Blob -> {
                    SQLiteCloudValue.Blob(arrayResultBufferValue(array, index))
                }

                SQLiteCloudValue.Type.Null -> {
                    SQLiteCloudValue.Null
                }

                SQLiteCloudValue.Type.Unknown -> {
                    throw SQLiteCloudError.Execution.unsupportedResultType
                }
            }
        }
    }

    private fun parseRowsetResult(rowset: OpaquePointer<SQLiteCloudResult>): SQLiteCloudRowset {
        val rowCount = rowsetResultRowCount(rowset)
        val columnCount = rowsetResultColumnCount(rowset)

        val columns = (0..<columnCount).map { index -> rowsetResultColumnName(rowset, index) }

        val rows = (0..<rowCount).map { row ->
            (0..<columnCount).map { column ->
                val valueType = SQLiteCloudValue.Type.fromRawValue(
                    rowsetResultValueType(rowset, row, column),
                )
                when (valueType) {
                    SQLiteCloudValue.Type.Integer -> {
                        SQLiteCloudValue.Integer(rowsetResultLongValue(rowset, row, column))
                    }

                    SQLiteCloudValue.Type.Double -> {
                        SQLiteCloudValue.Double(rowsetResultDoubleValue(rowset, row, column))
                    }

                    SQLiteCloudValue.Type.String -> {
                        SQLiteCloudValue.String(rowsetResultStringValue(rowset, row, column))
                    }

                    SQLiteCloudValue.Type.Blob -> {
                        SQLiteCloudValue.Blob(rowsetResultBufferValue(rowset, row, column))
                    }

                    SQLiteCloudValue.Type.Null -> {
                        SQLiteCloudValue.Null
                    }

                    SQLiteCloudValue.Type.Unknown -> {
                        throw SQLiteCloudError.Execution.unsupportedResultType
                    }
                }
            }
        }

        return SQLiteCloudRowset(columns, rows)
    }

    external fun uploadDatabase(
        name: String,
        encryptionKey: String?,
        dataHandler: DataHandler,
        fileSize: Long,
        callback: (dataHandler: DataHandler, buffer: ByteBuffer?, bufferLength: OpaquePointer<Int>?, totalLength: Long, previousProgress: Long) -> Int,
    ): Boolean

    external fun downloadDatabase(
        name: String,
        dataHandler: DataHandler,
        callback: (dataHandler: DataHandler, buffer: ByteBuffer?, bufferLength: OpaquePointer<Int>?, totalLength: Long, previousProgress: Long) -> Int,
    ): Boolean

    fun error(): SQLiteCloudError {
        if (isError()) {
            val code = errorCode()!!
            val message = errorMessage()!!

            return if (isSQLiteError()) {
                SQLiteCloudError.Sqlite(code, message, extendedErrorCode()!!, errorOffset()!!)
            } else {
                SQLiteCloudError.Connection(code, message)
            }
        }

        return SQLiteCloudError.Unhandled
    }

    fun vmError(vm: OpaquePointer<SQLiteCloudVM>): SQLiteCloudError {
        if (isVmError(vm)) {
            val code = vmErrorCode(vm)!!
            val message = vmErrorMessage(vm)!!

            return SQLiteCloudError.VirtualMachine(code, message)
        }

        return SQLiteCloudError.Unhandled
    }

    companion object {
        init {
            System.loadLibrary("sqlitecloud")
        }
    }
}
