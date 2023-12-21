package io.sqlitecloud

import org.junit.Assert.assertEquals
import org.junit.Test

class SQLiteCloudTest {
    @Test
    fun creationFromStringIsCorrect() {
        val connectionString = "sqlitecloud://username:password@hostname.com:1234/dbname?root_certificate=path"

        val config = SQLiteCloudConfig.fromString(connectionString)

        assertEquals("username", config.username)
        assertEquals("password", config.password)
        assertEquals("hostname.com", config.hostname)
        assertEquals(1234, config.port)
        assertEquals("dbname", config.dbname)
        assertEquals("path", config.rootCertificate)
    }
}
