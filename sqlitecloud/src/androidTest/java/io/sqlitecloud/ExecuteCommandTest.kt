package io.sqlitecloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExecuteCommandTest {
    companion object {
        private val sql: SQLiteCloud = TestContext.sqliteCloud()
    }

    @Test
    fun throwsSQLiteCloudErrorIfNotConnected() {
        assertThrows(SQLiteCloudError::class.java) {
            runBlocking {
                sql.execute(SQLiteCloudCommand.getUser)
            }
        }
    }

    @Test
    fun getUserCommandRetrievesTheConnectedUsername() = runBlocking {
        sql.connect()
        val result = sql.execute(SQLiteCloudCommand.getUser)
        sql.disconnect()

        assertEquals(sql.config.username, result.stringValue)
    }

    @Test
    fun testArrayCommandReturnsAnArrayResult() = runBlocking {
        sql.connect()
        val result = sql.execute(SQLiteCloudCommand.testArray)
        sql.disconnect()

        assertTrue(result is SQLiteCloudResult.Array)
    }
}
