package io.sqlitecloud

import java.net.URI
import kotlin.io.path.Path

data class SQLiteCloudConfig(
    val hostname: String,
    val username: String?,
    val password: String?,
    val apiKey: String?,
    val port: Int = defaultPort,
    val family: Family = Family.IPv4,
    val passwordHashed: Boolean = false,
    val nonlinearizable: Boolean = false,
    val timeout: Int = 0,
    val compression: Boolean = false,
    val sqliteMode: Boolean = false,
    val zerotext: Boolean = true,
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
        get() {
            if (apiKey != null) {
                return "sqlitecloud://$hostname:$port/${dbname ?: ""}?apikey=$apiKey"
            } else {
                return "sqlitecloud://${username ?: ""}:****@$hostname:$port/${dbname ?: ""}"
            }
        }

    companion object {
        const val defaultPort = 8860

        /**
         * Creates a SQLiteCloudConfig object parsing a connection string in the form
         * ```kotlin
         * "sqlitecloud://$username:$password@$hostname:$port/${dbname ?: ""}"
         * ```
         *
         * @param connectionString The connection string to parse.
         * @return A SQLiteCloudConfig object.
         */
        fun fromString(connectionString: String): SQLiteCloudConfig {
            val connectionUri = URI(connectionString)

            if (connectionUri.scheme?.lowercase() != "sqlitecloud") {
                throw IllegalArgumentException("Invalid connection scheme: ${connectionUri.scheme}")
            }

            val port = if (connectionUri.port != -1) connectionUri.port else defaultPort

            val userInfo = connectionUri.userInfo?.split(":")

            val path: String? = connectionUri.path
            val dbname = if (!path.isNullOrEmpty()) path.removePrefix("/") else null

            val queryItems: Map<String, String> = connectionUri.query?.split("&")?.associate {
                val (key, value) = it.split("=")
                key to value
            } ?: emptyMap()

            val apiKey = queryItems["apikey"]
            val family = queryItems["family"]
            val passwordHashed = queryItems["passwordHashed"]
            val nonlinearizable = queryItems["nonlinearizable"]
            val timeout = queryItems["timeout"]
            val compression = queryItems["compression"]
            val sqliteMode = queryItems["sqliteMode"]
            val zerotext = queryItems["zerotext"]
            val memory = queryItems["memory"]
            val dbCreate = queryItems["create"]
            val insecure = queryItems["insecure"]
            val noblob = queryItems["noblob"]
            val maxData = queryItems["maxdata"]
            val maxRows = queryItems["maxrows"]
            val maxRowset = queryItems["maxrowset"]
            val rootCertificate = queryItems["root_certificate"]
            val clientCertificate = queryItems["client_certificate"]
            val clientCertificateKey = queryItems["client_certificate_key"]

            return SQLiteCloudConfig(
                hostname = connectionUri.host ?: "",
                username = userInfo?.get(0),
                password = userInfo?.get(1),
                apiKey = apiKey,
                port = port,
                dbname = dbname,
                family = family?.toIntOrNull()
                    ?.let { familyValue -> Family.values().firstOrNull { it.value == familyValue } }
                    ?: Family.IPv4,
                passwordHashed = passwordHashed?.toBoolean() ?: false,
                nonlinearizable = nonlinearizable?.toBoolean() ?: false,
                timeout = timeout?.toIntOrNull() ?: 0,
                compression = compression?.toBoolean() ?: false,
                sqliteMode = sqliteMode?.toBoolean() ?: false,
                zerotext = zerotext?.toBoolean() ?: true,
                memory = memory?.toBoolean() ?: false,
                dbCreate = dbCreate?.toBoolean() ?: false,
                insecure = insecure?.toBoolean() ?: false,
                noblob = noblob?.toBoolean() ?: false,
                maxData = maxData?.toIntOrNull() ?: 0,
                maxRows = maxRows?.toIntOrNull() ?: 0,
                maxRowset = maxRowset?.toIntOrNull() ?: 0,
                rootCertificate = rootCertificate,
                clientCertificate = clientCertificate,
                clientCertificateKey = clientCertificateKey,
            )
        }
    }

    /// Constants that describe the connection family.
    enum class Family(val value: Int) {
        IPv4(0),
        IPv6(1),
        IPvAny(2),
    }
}
