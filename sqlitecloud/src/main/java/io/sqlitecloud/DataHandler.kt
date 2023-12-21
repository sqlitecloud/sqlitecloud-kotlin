package io.sqlitecloud

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path

data class DataTransferResult(val bytesTransferred: Int) {
    val isError: Boolean
        get() = bytesTransferred < 0

    val isComplete: Boolean
        get() = bytesTransferred <= 0
}

class DataHandler(path: Path, val progressHandler: ProgressHandler) {
    enum class Mode {
        Read,
        Write,
    }

    private val file: FileChannel
    val fileSize get() = file.size()

    init {
        try {
            file = FileChannel.open(path)
        } catch (e: Exception) {
            throw SQLiteCloudError.Task.urlHandlerFailed
        }
    }

    fun read(
        buffer: ByteBuffer?,
        bufferLength: OpaquePointer<Int>?,
        totalLength: Long,
        previouslyReadLength: Long,
    ) = transfer(mode = Mode.Read, buffer, bufferLength, totalLength, previouslyReadLength)

    fun write(
        buffer: ByteBuffer?,
        bufferLength: OpaquePointer<Int>?,
        totalLength: Long,
        previouslyReadLength: Long,
    ) = transfer(mode = Mode.Write, buffer, bufferLength, totalLength, previouslyReadLength)

    private fun transfer(
        mode: Mode,
        buffer: ByteBuffer?,
        bufferLength: OpaquePointer<Int>?,
        totalLength: Long,
        previouslyReadLength: Long,
    ): DataTransferResult {
        if (buffer == null) {
            return DataTransferResult(-1)
        }

        return try {
            val result = DataTransferResult(
                bytesTransferred = if (mode == Mode.Read) file.read(buffer) else file.write(buffer)
            )

            if (!result.isComplete) {
                // Check the progress of the stream.
                val progress =
                    (previouslyReadLength.toDouble() + result.bytesTransferred.toDouble()) / totalLength.toDouble()
                progressHandler(progress)
            }

            bufferLength?.reset()
            bufferLength?.putInt(result.bytesTransferred)
            bufferLength?.flip()

            DataTransferResult(result.bytesTransferred)
        } catch (e: IOException) {
            DataTransferResult(-1)
        }
    }
}
