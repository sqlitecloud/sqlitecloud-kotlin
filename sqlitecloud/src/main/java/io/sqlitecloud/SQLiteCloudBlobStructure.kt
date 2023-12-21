package io.sqlitecloud

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class SQLiteCloudBlobInfo(
    val schema: String? = null,
    val table: String,
    val column: String,
)

sealed interface BlobIO {
    sealed class Read: BlobIO {
        data object Buffer: Read()
        data class File(val path: java.nio.file.Path): Read()
    }

    sealed class Write: BlobIO {
        data class Buffer(val buffer: ByteBuffer): Write()
        data class File(val path: java.nio.file.Path): Write()

        val size: Int
            get() {
                return when (this) {
                    is Buffer -> buffer.remaining()
                    is File -> FileChannel.open(path).size().toInt()
                }
            }

        val data: ByteBuffer?
            get() = when (this) {
                is Buffer -> buffer
                is File -> null
            }
    }
}

data class SQLiteCloudBlobStructure<T: BlobIO>(
    val info: SQLiteCloudBlobInfo,
    val rows: List<Row<T>>,
    val blobSizeThreshold: Int = defaultBlobSizeThreshold,
    val autoIncreaseFieldSize: Boolean = true,
    val blobChunkCount: (blobSize: Int) -> Int = ::defaultBlobChunkCount
) {
    data class Row<T: BlobIO>(val id: Long, val dataIO: T)

    fun chunkSize(dataSize: Int) =
        if (dataSize > blobSizeThreshold) blobChunkCount(dataSize) else dataSize

    companion object {
        fun read(
            info: SQLiteCloudBlobInfo,
            rows: List<Row<BlobIO.Read>>,
            blobSizeThreshold: Int = defaultBlobSizeThreshold,
            autoIncreaseFieldSize: Boolean = true,
            blobChunkCount: (blobSize: Int) -> Int = ::defaultBlobChunkCount,
        ) = SQLiteCloudBlobStructure(
            info = info,
            rows = rows,
            blobSizeThreshold = blobSizeThreshold,
            autoIncreaseFieldSize = autoIncreaseFieldSize,
            blobChunkCount = blobChunkCount,
        )

        fun read(
            info: SQLiteCloudBlobInfo,
            row: Row<BlobIO.Read>,
            blobSizeThreshold: Int = defaultBlobSizeThreshold,
            autoIncreaseFieldSize: Boolean = true,
            blobChunkCount: Int = defaultBlobChunkCount(blobSizeThreshold),
        ) = SQLiteCloudBlobStructure(
            info = info,
            rows = listOf(row),
            blobSizeThreshold = blobSizeThreshold,
            autoIncreaseFieldSize = autoIncreaseFieldSize,
            blobChunkCount = { blobChunkCount },
        )

        fun write(
            info: SQLiteCloudBlobInfo,
            rows: List<Row<BlobIO.Write>>,
            blobSizeThreshold: Int = defaultBlobSizeThreshold,
            autoIncreaseFieldSize: Boolean = true,
            blobChunkCount: (blobSize: Int) -> Int = ::defaultBlobChunkCount,
        ) = SQLiteCloudBlobStructure(
            info = info,
            rows = rows,
            blobSizeThreshold = blobSizeThreshold,
            autoIncreaseFieldSize = autoIncreaseFieldSize,
            blobChunkCount = blobChunkCount,
        )

        fun write(
            info: SQLiteCloudBlobInfo,
            row: Row<BlobIO.Write>,
            blobSizeThreshold: Int = defaultBlobSizeThreshold,
            autoIncreaseFieldSize: Boolean = true,
            blobChunkCount: Int = defaultBlobChunkCount(blobSizeThreshold),
        ) = SQLiteCloudBlobStructure(
            info = info,
            rows = listOf(row),
            blobSizeThreshold = blobSizeThreshold,
            autoIncreaseFieldSize = autoIncreaseFieldSize,
            blobChunkCount = { blobChunkCount },
        )
    }
}
