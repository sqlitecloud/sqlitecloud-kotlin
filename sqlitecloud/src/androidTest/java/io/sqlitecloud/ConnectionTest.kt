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
class ConnectionTest {
    companion object {
        private val sql: SQLiteCloud = TestContext.sqliteCloud()
        private val sqlApiKey: SQLiteCloud = TestContext.sqliteCloudApiKey()
    }

    @Test
    fun connectWithValidCredentialsSucceeds() = runBlocking {
        sql.connect()
        assertTrue(sql.isConnected)
        sql.disconnect()
        assertFalse(sql.isConnected)
    }

    @Test
    fun connectWithValidApiKeyCredentialsSucceeds() = runBlocking {
        sqlApiKey.connect()
        assertTrue(sqlApiKey.isConnected)
        sqlApiKey.disconnect()
        assertFalse(sqlApiKey.isConnected)
    }

    @Test
    fun connectWithInvalidCredentialsThrowsSQLiteCloudError() {
        val invalidSql = SQLiteCloud(
            appContext = TestContext.context,
            config = sql.config.copy(password = "INVALID PASSWORD"),
        )
        assertThrows(SQLiteCloudError::class.java) {
            runBlocking {
                invalidSql.connect()
            }
        }
    }

    @Test
    fun connectWithInvalidCredentialsApiKeyThrowsSQLiteCloudError() {
        val invalidSql = SQLiteCloud(
            appContext = TestContext.context,
            config = sqlApiKey.config.copy(apiKey = "INVALID APIKEY"),
        )
        assertThrows(SQLiteCloudError::class.java) {
            runBlocking {
                invalidSql.connect()
            }
        }
    }

    @Test
    fun disconnectWhenNotConnectedThrowsSQLiteCloudError() {
        assertThrows(SQLiteCloudError::class.java) {
            runBlocking {
                sql.disconnect()
            }
        }
    }
}
