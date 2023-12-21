package io.sqlitecloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class BlobTest {
    companion object {
        private val sql: SQLiteCloud = TestContext.sqliteCloud()
    }

    private var firstRandomBufferSize: Int = 0

    @Before
    fun setUp(): Unit = runBlocking {
        sql.connect()
        sql.useDatabase("testDatabase")
        sql.execute(
            query = """
                CREATE TABLE IF NOT EXISTS Test1  (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    data BLOB
                );
            """.trimIndent()
        )
        sql.execute(query = "DELETE FROM Test1")

        firstRandomBufferSize = Random.nextInt(1_000_000)
        val data = ByteBuffer.allocateDirect(firstRandomBufferSize)
        val randomBytes = Random.nextBytes(firstRandomBufferSize)
        data.put(randomBytes)
        data.clear()

        sql.execute(
            command = SQLiteCloudCommand(
                query = "INSERT INTO Test1 (id, name, data) VALUES (1, ?, ?)",
                parameters = listOf(
                    SQLiteCloudValue.String("RandomString1"),
                    SQLiteCloudValue.Blob(data),
                ),
            )
        )
    }

    @After
    fun tearDown(): Unit = runBlocking {
        sql.disconnect()
    }

    @Test
    fun updateBlobUpdatesBlobField() = runBlocking {
        val size = Random.nextInt(1_000_000)
        val data = ByteBuffer.allocateDirect(size)
        val randomBytes = Random.nextBytes(size)
        data.put(randomBytes)
        data.clear()
        val blob = SQLiteCloudBlobStructure.write(
            info = SQLiteCloudBlobInfo(
                table = "Test1",
                column = "data",
            ),
            row = SQLiteCloudBlobStructure.Row(id = 1, dataIO = BlobIO.Write.Buffer(data))
        )

        sql.updateBlob(blob)
    }

    @Test
    fun increasingBlobFieldSizeSucceeds() = runBlocking {
        val blobInfo = SQLiteCloudBlobInfo(table = "Test1", column = "data")

        val currentSize = sql.blobFieldSizes(blobInfo = blobInfo, rowIds = listOf(1)).first()

        val newSize = currentSize + 100_000

        val expandBlobCommand = SQLiteCloudCommand.expandBlobField(
            table = blobInfo.table,
            column = blobInfo.column,
            rowId = 1,
            size = newSize,
        )
        val result = sql.execute(command = expandBlobCommand)

        assertTrue(result is SQLiteCloudResult.Success)

        val actualNewSize = sql.blobFieldSizes(blobInfo = blobInfo, rowIds = listOf(1)).first()

        assertEquals(newSize, actualNewSize)
    }

    @Test
    fun readBlobReadsBlobField() = runBlocking {
        val blob = SQLiteCloudBlobStructure.read(
            info = SQLiteCloudBlobInfo(
                table = "Test1",
                column = "data",
            ),
            row = SQLiteCloudBlobStructure.Row(id = 1, BlobIO.Read.Buffer),
        )

        val data = sql.readBlob(blob)

        assertEquals(1, data.count())
        assertEquals(firstRandomBufferSize, data.firstOrNull()?.data?.capacity())
    }
}
