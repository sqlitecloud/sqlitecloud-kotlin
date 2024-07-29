package io.sqlitecloud

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import java.io.File

object TestContext {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val certFile = File(context.filesDir, "cert.pem")
    private val secretsFileName = "secrets.json"
    val secrets: Secrets?

    init {
        if (!certFile.exists()) {
            val inputStream = context.resources.assets.open(certFile.name)
            val outputStream = certFile.outputStream()
            outputStream.write(inputStream.readBytes())
            inputStream.close()
            outputStream.close()
        }
        secrets = try {
            val secretsInputStream = context.resources.assets.open(secretsFileName)
            secretsInputStream.reader().use {
                val json = it.readText()
                Json.decodeFromString<Secrets>(json)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun sqliteCloud(
        hostname: String = secrets?.hostname ?: "",
        username: String = secrets?.username ?: "",
        password: String = secrets?.password ?: "",
        rootCertificatePath: String = certFile.path,
    ) = SQLiteCloud(
        appContext = context,
        config = SQLiteCloudConfig(
            hostname = hostname,
            username = username,
            password = password,
            apiKey = null,
            rootCertificate = rootCertificatePath,
        )
    )

    fun sqliteCloudApiKey(
        hostname: String = secrets?.hostname ?: "",
        apiKey: String = secrets?.apiKey ?: "",
        rootCertificatePath: String = certFile.path,
    ) = SQLiteCloud(
        appContext = context,
        config = SQLiteCloudConfig(
            hostname = hostname,
            username = null,
            password = null,
            apiKey = apiKey,
            rootCertificate = rootCertificatePath,
        )
    )
}
