package com.sqlitecloud

data class SQLiteCloudConfig(
    val hostname: String,
    val username: String,
    val password: String,
    val port: Int = defaultPort,
    val family: Family = Family.IPv4,
    val passwordHashed: Boolean = false,
    val nonlinearizable: Boolean = false,
    val timeout: Int = 0,
    val compression: Boolean = false,
    val sqliteMode: Boolean = false,
    val zerotext: Boolean = false,
    val memory: Boolean = false,
    val dbCreate: Boolean = false,
    val insecure: Boolean = false,
    val noblob: Boolean = false,
    val isReadonlyConnection: Boolean = false,
    val maxData: Int = 0,
    val maxRows: Int = 0,
    val maxRowset: Int = 0,
    val dbname: String? = null,
    val rootCertificate: String? = null,
    val clientCertificate: String? = null,
    val clientCertificateKey: String? = null,
) {
    val connectionString: String
        get() = "sqlitecloud://$username:****@$hostname:$port/${dbname ?: ""}"

    companion object {
        const val defaultPort = 8860
    }

    /// Constants that describe the connection family.
    enum class Family(val value: Int) {
        IPv4(0),
        IPv6(1),
        IPvAny(2),
    }
}
