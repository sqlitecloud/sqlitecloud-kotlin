package io.sqlitecloud

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.min

object SQLiteCloudBlobReadWrite {
    fun readRow(
        blob: SQLiteCloudBlobStructure<BlobIO.Read>,
        row: SQLiteCloudBlobStructure.Row<BlobIO.Read>,
        totalSize: Int,
        progressHandler: ProgressHandler?,
        callback: (buffer: ByteBuffer) -> Int,
    ): BlobIO.Write {
        val chunkSize =
            if (totalSize > blob.blobSizeThreshold) blob.chunkSize(totalSize) else totalSize

        val buffer = when (row.dataIO) {
            is BlobIO.Read.Buffer -> ByteBuffer.allocateDirect(totalSize)
            is BlobIO.Read.File -> FileChannel.open(row.dataIO.path)
                .map(FileChannel.MapMode.READ_WRITE, 0, totalSize.toLong())
        }

        while (buffer.hasRemaining()) {
            val nextChunkSize = Integer.min(chunkSize, (totalSize - buffer.position()))
            buffer.limit(buffer.position() + nextChunkSize)
            callback(buffer)
            buffer.limit(totalSize)
            if (progressHandler != null) {
                progressHandler(buffer.position().toDouble() / totalSize.toDouble())
            }
        }

        return when (row.dataIO) {
            is BlobIO.Read.Buffer -> BlobIO.Write.Buffer(buffer)
            is BlobIO.Read.File -> BlobIO.Write.File(row.dataIO.path)
        }
    }

    fun writeRow(
        blob: SQLiteCloudBlobStructure<BlobIO.Write>,
        row: SQLiteCloudBlobStructure.Row<BlobIO.Write>,
        progressHandler: ProgressHandler?,
        callback: (buffer: ByteBuffer) -> Int,
    ) {
        when (row.dataIO) {
            is BlobIO.Write.Buffer -> readFromBuffer(
                blob,
                row.dataIO.buffer,
                progressHandler,
                callback,
            )
            is BlobIO.Write.File -> readFromFile(
                blob,
                row.dataIO.path,
                progressHandler,
                callback,
            )
        }
    }

    fun readFromBuffer(
        blob: SQLiteCloudBlobStructure<BlobIO.Write>,
        buffer: ByteBuffer,
        progressHandler: ProgressHandler?,
        callback: (buffer: ByteBuffer) -> Int,
    ) {
        val totalSize = buffer.remaining()
        val chunkSize = blob.chunkSize(totalSize)

        while (buffer.hasRemaining()) {
            val nextChunkSize = Integer.min(chunkSize, buffer.remaining())
            buffer.limit(buffer.position() + nextChunkSize)
            callback(buffer)
            buffer.limit(totalSize)
            if (progressHandler != null) {
                progressHandler(buffer.position().toDouble() / totalSize.toDouble())
            }
        }
    }

    fun readFromFile(
        blob: SQLiteCloudBlobStructure<BlobIO.Write>,
        path: Path,
        progressHandler: ProgressHandler?,
        callback: (buffer: ByteBuffer) -> Int,
    ) {
        val channel = FileChannel.open(path, StandardOpenOption.READ)

        val totalSize = channel.size()
        val chunkSize = blob.chunkSize(totalSize.toInt()).toLong()

        while (channel.position() < channel.size()) {
            val nextChunkSize = min(chunkSize, channel.size() - channel.position())
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, channel.position(), nextChunkSize)
            callback(buffer)
            channel.position(channel.position() + nextChunkSize)
            if (progressHandler != null) {
                progressHandler(channel.position().toDouble() / channel.size().toDouble())
            }
        }
    }
}
