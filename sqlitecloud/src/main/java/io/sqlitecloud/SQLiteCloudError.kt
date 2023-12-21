package io.sqlitecloud

/// Represents errors that can occur during interactions with the `SQLiteCloud` class.
///
/// `SQLiteCloudError` is a sealed class that encompasses various error scenarios that
/// can arise when working with SQLite Cloud. Its subclasses provide detailed error context.
sealed class SQLiteCloudError : Throwable() {
    /// Unhandled error.
    data object Unhandled : SQLiteCloudError()

    /// An indication that the connection to the cloud has failed.
    /// The reasons can be of various types.
    /// code: The error code.
    /// message: The error message.
    data class Connection(val code: Int, override val message: String) : SQLiteCloudError() {
        companion object {
            val invalidConnection = Connection(
                code = -1,
                message = "Invalid connection",
            )
            val invalidUUID = Connection(
                code = -2,
                message = "Invalid UUID",
            )
        }
    }

    /// An indication that execution of the sql command failed.
    /// code: The error code.
    /// message: The error message.
    data class Execution(val code: Int, override val message: String) : SQLiteCloudError() {
        companion object {
            val unsupportedResultType = Execution(
                code = -3,
                message = "Unsupported result type",
            )
            val unsupportedValueType = Execution(
                code = -4,
                message = "Unsupported value type",
            )
        }
    }

    /// An indication that a generic sqlite error has occurred.
    /// More details can be found in the associated value `SQLiteCloudError.SqlContext`.
    /// code: The error code.
    /// message: The error message.
    /// extendedErrorCode: The extended error code.
    /// offset: The offset.
    data class Sqlite(
        val code: Int,
        override val message: String,
        val extendedErrorCode: Int,
        val offset: Int,
    ) : SQLiteCloudError()

    /// An indication that a upload or download task error has occurred.
    /// code: The error code.
    /// message: The error message.
    data class Task(val code: Int, override val message: String) : SQLiteCloudError() {
        companion object {
            val urlHandlerFailed = Task(
                code = -5,
                message = "Cannot create URL Handler",
            )
            val cannotCreateOutputFile = Task(
                code = -6,
                message = "Cannot create output file",
            )
            val invalidNumberOfRows = Task(
                code = -7,
                message = "Invalid number of rows",
            )
            val invalidBlobSizeRead = Task(
                code = -8,
                message = "Invalid blob size read",
            )
            val errorWritingBlob = Task(
                code = -9,
                message = "Error writing blob",
            )
        }
    }

    data class VirtualMachine(val code: Int, override val message: String) : SQLiteCloudError() {
        companion object {
            fun invalidParameterIndex(index: Int) =
                VirtualMachine(code = -10, message = "Invalid parameter index [$index].")
        }
    }
}
