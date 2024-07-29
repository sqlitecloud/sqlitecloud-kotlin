package io.sqlitecloud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * SQLiteCloud acts as an actor that interfaces with SQLite Cloud, providing methods for database
 * operations and real-time notifications.
 *
 * SQLiteCloud is a set of functions that allow Swift programs to interact with
 * the SQLite Cloud backend server, pass queries and SQL commands, and receive
 * the results of these queries.
 *
 * In addition to standard SQLite statements, several other commands are supported,
 * and the SQLiteCloud APIs implement the SQLiteCloud Serialization Protocol.
 *
 * To use this class, create an instance of `SQLiteCloud` with the appropriate
 * configuration and call its methods.
 *
 * Example usage:
 * ```kotlin
 * val config = SQLiteCloudConfig(...) // Initialize with your configuration
 * val sqliteCloud = SQLiteCloud(context, config)
 * ```
 * Note: This class provides both synchronous and suspending methods for various
 * database operations.
 *
 * @constructor Creates a new instance of `SQLiteCloud`.
 * @param appContext The Android application context.
 * @param config The configuration for establishing a connection to the SQLite Cloud server.
 * This property defines various connection parameters such as the hostname, port, username,
 * password, and other options required to connect to the SQLite Cloud server.
 * @property logger The optional logger to use for logging messages.
 * It uses [DefaultSQLiteCloudLogger] by default.
 * @property scope The coroutine scope to use for executing the suspending methods. It defaults to
 * [CoroutineScope(Dispatchers.IO)].
 *
 * - SeeAlso: [SQLiteCloudConfig] for configuring the SQLite Cloud connection.
 * - SeeAlso: [SQLiteCloudCommand] for representing SQL commands and queries.
 * - SeeAlso: [SQLiteCloudResult] for representing the result of database operations.
*/
class SQLiteCloud(
    appContext: Context,
    config: SQLiteCloudConfig,
    val logger: SQLiteCloudLogger? = DefaultSQLiteCloudLogger(isEnabled = true),
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    var config: SQLiteCloudConfig = config
        private set

    private var channels = mutableMapOf<SQLiteCloudChannel, Int>()

    private var observers = mutableListOf<SQLiteCloudSubscription>()

    private val bridge = SQLiteCloudBridge(logger)

    init {
        if (config.rootCertificate == null) {
            val file = File(appContext.filesDir, tlsDefaultCertificateName)
            if (!file.exists()) {
                val inputStream = appContext.assets.open(tlsDefaultCertificateName)
                val outputStream = file.outputStream()
                outputStream.write(inputStream.readBytes())
                inputStream.close()
                outputStream.close()
            }
            this.config = config.copy(rootCertificate = file.path)
        }
    }

    /**
     * Whether the client is currently connected to the SQLite Cloud server.
     */
    val isConnected
        get() = runBlocking(scope.coroutineContext) { bridge.isConnected }

    /**
     * Whether an error has occurred during the preceding database operations.
     */
    val isError: Boolean
        get() = runBlocking(scope.coroutineContext) { bridge.isError() }

    /**
     * Whether the error is an SQLite error.
     */
    val isSQLiteError: Boolean
        get() = runBlocking(scope.coroutineContext) { bridge.isSQLiteError() }

    /**
     * The code for the eventual error occurred during the preceding database operations.
     */
    val errorCode: Int?
        get() = runBlocking(scope.coroutineContext) { bridge.errorCode() }

    /**
     * The message for the eventual error occurred during the preceding database operations.
     */
    val errorMessage: String?
        get() = runBlocking(scope.coroutineContext) { bridge.errorMessage() }

    /**
     * The extended error code for the eventual error occurred during the preceding database
     * operations.
     */
    val extendedErrorCode: Int?
        get() = runBlocking(scope.coroutineContext) { bridge.extendedErrorCode() }

    /**
     * The error offset for the eventual error occurred during the preceding database operations.
     */
    val errorOffset: Int?
        get() = runBlocking(scope.coroutineContext) { bridge.errorOffset() }

    /**
     * Establishes a connection to a database node using the specified
     * configuration, then configures a callback function
     * to handle Pub/Sub notifications, and sets up the connection for use.
     *
     *  Example usage:
     *  ```kotlin
     *  try {
     *     val sqliteCloud: SQLiteCLoud = ...
     *     sqliteCloud.connect()
     *     // Connection successful, perform database operations here
     *  } catch (error: Error) {
     *     print("Error: $error")
     *  }
     *  ```
     *
     * @throws SQLiteCloudError If an error occurs during the connection process, this
     *            method throws an SQLiteCloudError.connectionFailure with context details
     *            about the error.
     */
    suspend fun connect(): Unit = withContext(scope.coroutineContext) {
        logger?.logDebug(
            category = "CONNECTION",
            message = "📡 Connecting to ${config.connectionString}...",
        )

        val success = bridge.connect(
            hostname = config.hostname,
            port = config.port,
            username = config.username,
            password = config.password,
            database = config.dbname,
            apiKey = config.apiKey,
            timeout = config.timeout,
            family = config.family.value,
            compression = config.compression,
            zeroText = config.zerotext,
            passwordHashed = config.passwordHashed,
            nonlinearizable = config.nonlinearizable,
            dbMemory = config.memory,
            noBlob = config.noblob,
            dbCreate = config.dbCreate,
            maxData = config.maxData,
            maxRows = config.maxRows,
            maxRowset = config.maxRowset,
            tlsRootCertificate = config.rootCertificate,
            tlsCertificate = config.clientCertificate,
            tlsCertificateKey = config.clientCertificateKey,
            insecure = config.insecure,
        )

        if (!success) {
            bridge.disconnect()
            throw error()
        }

        setupPubSubCallback()

        if (config.isReadonlyConnection) {
            bridge.setPubSubOnly()

            if (isError) {
                val error = error()
                logger?.logError(
                    category = "CONNECTION",
                    message = "🚨 Error during SQCloudSetPubSubOnly: $error",
                )
                bridge.disconnect()
                throw error
            }
        }

        logger?.logDebug(
            category = "CONNECTION",
            message = "📡 Connection to ${config.connectionString} successful",
        )
    }

    /**
     * Disconnects from the database server.
     *
     * This method closes the active database connection, releasing any associated resources,
     * and resets the connection properties to indicate that the connection is closed.
     *
     * @throws SQLiteCloudError If an error if the connection cannot be closed or if
     *           the connection is already closed.
     *
     * - Important: Before calling this method, ensure that you have an open and valid database
     *              connection by using the [connect] method. If the connection is not open or
     *              is invalid, this method will throw an error.
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *     val sqliteCloud: SQLiteCLoud = ...
     *     sqliteCloud.connect()
     *
     *     // perform database operations
     *
     *     sqliteCloud.disconnect()
     *     // Connection successfully closed
     * } catch (error: Error) {
     *     print("Error: $error")
     * }
     * ```
    */
    suspend fun disconnect() = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        bridge.disconnect()
    }

    private fun ensureConnectedOrThrow() {
        if (!isConnected) {
            throw SQLiteCloudError.Connection.invalidConnection
        }
    }

    private fun setupPubSubCallback() {
        bridge.setPubSubCallback { result ->
            if (result is SQLiteCloudResult.Json) {
                val data = result.value
                val payload = try {
                    Json.decodeFromString<SQLiteCloudPayload>(data)
                } catch (e: Error) {
                    logger?.logError(
                        category = "PUB/SUB",
                        message = "🚨 Message decoding error: $e",
                    )
                    return@setPubSubCallback
                }

                observers.forEach { observer ->
                    if (observer.channel.name == payload.channel) {
                        observer.callback(payload)
                    }
                }

                logger?.logDebug(category = "PUB/SUB", message = "✉️ Message received: $payload")
            }
        }
    }

    /**
     * Get the unique client UUID associated with the current connection.
     *
     * This method retrieves the unique client UUID value for the active database connection.
     * The UUID serves as a client identifier and can be used for various purposes, such as
     * tracking client-specific data.
     *
     * @return A `UUID` instance representing the client's unique identifier.
     *
     * @throws SQLiteCloudError if an error if the connection cannot be established, or
     *           if the UUID retrieved from the database is invalid or null.
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *     val clientUUID = await sqliteCloud.getClientUUID()
     *     print("Client UUID: $clientUUID")
     * } catch (error) {
     *     print("Error: $error")
     * }
     * ```
     *
     * - Note: The UUID is typically represented as a string in the format
     *         "E621E1F8-C36C-495A-93FC-0C247A3E6E5F". This method retrieves it from the
     *          database as a raw string and converts it into a [UUID] instance.
    */
    suspend fun getClientUUID(): UUID = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        val uuidString = bridge.getClientUUID() ?: throw SQLiteCloudError.Connection.invalidUUID
        UUID.fromString(uuidString)
    }

    /**
     * Execute a SQL command on the SQLite Cloud database.
     *
     * This method allows to execute SQL commands (queries or updates) on the SQLite Cloud database
     * associated with the active connection. It takes a `SQLiteCloudCommand` object as input,
     * which includes the SQL query string and any parameters if needed.
     *
     * @param command A `SQLiteCloudCommand` object containing the SQL command and optional parameters.
     *
     * @return A [SQLiteCloudResult] object containing the result of the SQL command execution.
     *
     * @throws SQLiteCloudError.Connection if the connection cannot be established,
     *
     * @throws SQLiteCloudError.Execution if there is an issue with the SQL command or
     *           parameters, or if an error occurs during the execution of the query.
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *     val sql = "SELECT * FROM users WHERE id = ?"
     *     val command = SQLiteCloudCommand(query: sql, parameters: [.integer(42)])
     *     val result = try sqliteCloud.execute(command: command)
     *     // Process the result
     * } catch (error) {
     *     print("Error: $error")
     * }
     * ```
     */
    suspend fun execute(command: SQLiteCloudCommand) = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        bridge.execute(command)
    }

    /**
     * Execute a SQL query on the SQLite Cloud database.
     *
     * This method allows to execute SQL commands (queries or updates) on the SQLite Cloud database
     * associated with the active connection. It takes a generic SQL statement as input.
     *
     * @param query A generic SQL statement to execute.
     * @param parameters An array of `SQLiteCloudValue` objects representing optional parameters to
     *                 bind into the query.
     *
     * @return A `SQLiteCloudResult` object containing the result of the SQL command execution.
     *
     * @throws SQLiteCloudError.Connection if the connection cannot be established.
     *
     * @throws SQLiteCloudError.Execution if there is an issue with the SQL command or
     *           parameters, or if an error occurs during the execution of the query.
     *
     * - Important: Please use [execute] with [SQLiteCloudCommand] if you have parameters in SQL statement.
     *              This is important for safely binding values into the query and preventing SQL injection.
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *     val sql = "SELECT * FROM users"
     *     val result = sqliteCloud.execute(query = sql)
     *     // Process the result
     * } catch (error) {
     *     print("Error: $error")
     * }
     * ```
     */
    suspend fun execute(query: String, vararg parameters: SQLiteCloudValue) =
        withContext(scope.coroutineContext) {
            execute(SQLiteCloudCommand(query = query, parameters = parameters))
        }

    /**
     * Execute a SQL query on the SQLite Cloud database.
     *
     * This method allows to execute SQL commands (queries or updates) on the SQLite Cloud database
     * associated with the active connection. It takes a generic SQL statement as input.
     *
     * @param query A generic SQL statement to execute.
     * @param parameters An array of `SQLiteCloudValue` objects representing optional parameters to
     *                 bind into the query.
     *
     * @return A [SQLiteCloudResult] object containing the result of the SQL command execution.
     *
     * @throws SQLiteCloudError.Connection if the connection cannot be established,
     *
     * @throws SQLiteCloudError.Execution if there is an issue with the SQL command or
     *           parameters, or if an error occurs during the execution of the query.
     *
     * - Important: Please use [execute] with [SQLiteCloudCommand] if you have parameters in SQL statement.
     *              This is important for safely binding values into the query and preventing SQL injection.
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *     val sql = "SELECT * FROM users"
     *     val result = try sqliteCloud.execute(query = sql)
     *     // Process the result
     * } catch (error) {
     *     print("Error: $error")
     * }
     * ```
     */
    suspend fun execute(query: String, parameters: List<SQLiteCloudValue>) =
        withContext(scope.coroutineContext) {
            execute(SQLiteCloudCommand(query = query, parameters = parameters))
        }

    suspend fun useDatabase(databaseName: String) = withContext(scope.coroutineContext) {
        execute(SQLiteCloudCommand.useDatabase(databaseName))
    }

    /**
     * Create a Pub/Sub channel in the SQLite Cloud database.
     *
     * This method allows you to create a Pub/Sub channel within the SQLite Cloud database.
     * A Pub/Sub channel is used for publishing and subscribing to real-time notifications
     * and updates.
     *
     * @param channel A string specifying the name of the Pub/Sub channel to create.
     * @param ifNotExists A boolean value indicating whether to create the channel if it does not
     *                  already exist (default is true). If set to true and the channel already
     *                  exists, the method will not throw an error.
     *
     * @return A [SQLiteCloudResult] object containing the result of the channel creation.
     *
     * @throws SQLiteCloudError.Connection if the connection is not invalid or a
     *            network error has occurred.
     *
     * @throws SQLiteCloudError.Execution if the channel query is not valid.
     *
     *  Example usage:
     * ```kotlin
     * try {
     *     val channelName = "myChannel"
     *     val result = await sqliteCloud.create(channel = channelName)
     *     // Channel created successfully
     * } catch error {
     *     print("Error: $error")
     * }
     * ```
     */
    suspend fun createChannel(channel: String, ifNotExists: Boolean = true): SQLiteCloudResult {
        return execute(
            SQLiteCloudCommand.createChannel(
                channel = channel,
                ifNotExists = ifNotExists
            ),
        )
    }

    /**
     * Send a notification message to a Pub/Sub channel in the SQLite Cloud database.
     *
     * This method allows you to send a notification message to a specific Pub/Sub channel
     * within the SQLite Cloud database. The message can carry a payload annotated with the
     * `@Serializable` annotation. If the specified channel
     * does not exist and `createChannelIfNotExist` is set to true in the message, the channel
     * will be created before sending the message.
     *
     * - Parameters:
     *     - message: A `SQLiteCloudMessage` object containing the channel name, payload, and optional configuration.
     *
     * - Returns: A `SQLiteCloudResult` object containing the result of the notification message operation.
     *
     * - Throws: `SQLiteCloudError.connectionFailure` if the connection is not invalid or a
     *            network error has occurred.
     *
     * - Throws: `SQLiteCloudError.executionFailed` if the channel query is not valid.
     *
     * Example usage:
     *
     * ```kotlin
     * @Serializable
     * data class MyPayload(val content: String)
     *
     * val mypayload = MyPayload(content = "Hello, World!")
     * val message = SQLiteCloudMessage(channel = "myChannel", payload = mypayload, createChannelIfNotExist = true)
     *
     * try {
     *     val result = await sqliteCloud.notify(message)
     *     // Message sent successfully
     * } catch error {
     *     print("Error: $error")
     * }
     * ```
     */
    suspend inline fun <reified P> notify(message: SQLiteCloudMessage<P>): SQLiteCloudResult =
        withContext(scope.coroutineContext) {
            if (message.createChannelIfNotExist) {
                execute(
                    command = SQLiteCloudCommand.createChannel(
                        message.channel,
                        ifNotExists = true,
                    ),
                )
            }

            execute(
                command = SQLiteCloudCommand.notify(
                    channel = message.channel,
                    payload = Json.encodeToString(message.payload),
                ),
            )
        }

    private suspend fun change(channel: SQLiteCloudChannel, counter: Int): Unit =
        withContext(scope.coroutineContext) {
            channels[channel] = (channels[channel] ?: 0) + counter

            if (channels[channel] == 0) {
                execute(SQLiteCloudCommand.unlisten(channel))
                logger?.logInfo(
                    category = "PUB/SUB",
                    message = "🙉 Unlisten channel '${channel.name}'",
                )
            }
        }

    /**
     * Listen for notifications on a specified SQLite Cloud channel.
     *
     * This method allows you to listen for real-time notifications and updates on a specific
     * SQLite Cloud channel. When a notification is received on the channel, the provided
     * callback function is invoked. You can use this feature for building real-time data
     * synchronization and event-driven applications.
     *
     * Listening to a table means you'll receive notification about all the write operations in
     * that table. In the case of table, the table name can be *, which means you'll start
     * receiving notifications from all the tables inside the current database.
     *
     * @param channel A `SQLiteCloudChannel` object representing the channel to listen to.
     * @param callback A callback function that is called when a notification is received on the
     *               specified channel. The callback takes a `SQLiteCloudNotification` object as
     *               its parameter.
     *
     * @return An object that allows you to unsubscribe from the channel when you
     *            no longer wish to receive notifications.
     *
     * @throws SQLiteCloudError.Connection if the connection is not invalid or a network
     *            error has occurred.
     *
     * @throws SQLiteCloudError.Execution if the channel query is not valid.
     *
     * Example usage:
     *
     * ```kotlin
     * val myChannel = SQLiteCloudChannel.name("myChannel")
     * this.listener = sqliteCloud.listen(channel = myChannel) { payload ->
     *     // Handle the received payload
     *     print("Received payload: $payload")
     * }
     *
     * // Later, when you want to stop listening to the channel:
     * this.listener = null
     * ```
     * - Note: The provided callback function is executed whenever a notification is received
     *         on the specified channel. You can use the returned object to unsubscribe
     *         from the channel when you no longer wish to receive notifications, freeing up resources.
     */
    suspend fun listen(channel: SQLiteCloudChannel, callback: NotificationHandler): Any =
        withContext(scope.coroutineContext) {
            // Starts listening notifications for a given channel/table.
            execute(SQLiteCloudCommand.listen(channel))

            channels[channel] = (channels[channel] ?: 0) + 1

            val onUnsubscribe: Callback<SQLiteCloudChannel> = { channel ->
                scope.launch {
                    change(channel = channel, counter = -1)
                }
            }

            val subscription = SQLiteCloudSubscription(channel, callback, onUnsubscribe)

            val observer = observers.add(subscription)

            logger?.logInfo(
                category = "PUB/SUB",
                message = "🎧 Listening to channel '${channel.name}'",
            )

            return@withContext observer
        }

    /**
     * Uploads a database file to the SQLite Cloud server with progress tracking.
     *
     * This method allows you to upload a database file to the SQLite Cloud server.
     * The upload process is asynchronous and provides progress tracking through
     * the `progressHandler` callback.
     *
     * @param databaseName the uploaded database name.
     * @param databasePath the path of the local database file.
     * @param databaseEncryptionKey the optional key the database was encrypted with.
     * @throws SQLiteCloudError
     *
     * Example usage:
     *
     * ```kotlin
     * val databasePath = Path("/path/to/your/database.db")
     * sqliteCloud.upload(databseName = "myDatabase", databasePath = databasePath) { progress in
     *     print("Progress: ${progress * 100}%")
     * }
     * ```
     *
     * - Note: The progressHandler closure is called periodically during the upload process,
     *         allowing you to track the progress of the upload.
     */
    suspend fun upload(
        databaseName: String,
        databasePath: Path,
        databaseEncryptionKey: String?,
        progressHandler: ProgressHandler,
    ) = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        // Create an upload helper that manages the data stream and the progress of the upload.
        val dataHandler = DataHandler(path = databasePath, progressHandler = progressHandler)
        val success = bridge.uploadDatabase(
            name = databaseName,
            encryptionKey = databaseEncryptionKey,
            dataHandler = dataHandler,
            fileSize = dataHandler.fileSize,
        ) { dataHandler, buffer, bufferLength, totalLength, previousProgress ->
            dataHandler.read(
                buffer,
                bufferLength,
                totalLength,
                previousProgress,
            ).bytesTransferred
        }

        if (!success) {
            val error = error()
            logger?.logError(category = "UPLOAD", message = "🚨 Database upload failed: $error")
            throw error
        }
        logger?.logDebug(category = "UPLOAD", message = "✅ Database upload succesfully")
    }

    /**
     * Downloads a database from SQLite Cloud.
     *
     * This method asynchronously downloads a database from SQLite Cloud. It first checks if
     * the connection to SQLite Cloud is open and valid. If the connection is valid, it
     * proceeds with the download operation.
     *
     * @param databaseName: the name of the database to download.
     * @param progressHandler: A closure that receives progress updates during the download.
     *
     * @throws SQLiteCloudError if the download process fails.
     *
     * @return The path of the downloaded database file.
     *
     * - Note: temporary files are created during the download process and should be cleaned up
     *         by the caller when no longer needed.
     *
     * Example usage:
     *
     * ```kotlin
     * val downloadedDatabasePath = await sqliteCloud.download(databaseName = "myDatabase") { progress in
     *     // Handle download progress updates
     * }
     * ```
     */
    suspend fun download(
        databaseName: String,
        progressHandler: ProgressHandler,
    ): Path = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        val path = Files.createTempFile(UUID.randomUUID().toString(), null)
        val dataHandler = DataHandler(path = path, progressHandler = progressHandler)
        val success = bridge.downloadDatabase(
            name = databaseName,
            dataHandler = dataHandler,
        ) { dataHandler, buffer, bufferLength, totalLength, previousProgress ->
            dataHandler.write(
                buffer,
                bufferLength,
                totalLength,
                previousProgress,
            ).bytesTransferred
        }

        if (success) {
            return@withContext path
        } else {
            val error = error()
            logger?.logError(category = "DOWNLOAD", message = "🚨 Database download failed: $error")
            throw error
        }
    }

    /**
     * Retrieve the size in bytes of one or more BLOB (Binary Large Object) fields in the
     * SQLite Cloud database.
     *
     * This method allows you to retrieve the size in bytes of BLOB fields for one or more rows
     * specified by their unique row IDs. The result is an array of `Int32` values, each representing
     * the size in bytes of a BLOB field for a corresponding row ID.
     *
     * @param blobInfo A [SQLiteCloudBlobInfo] object containing information about the BLOB field.
     * @param rowIds The row IDs to retrieve the blob size for.
     *
     * @return A list of Int values, each representing the size in bytes of a BLOB field
     * for a corresponding row ID.
     *
     * @throws SQLiteCloudError.Connection if the connection is not invalid or a
     * network error has occurred.
     *
     * @throws SQLiteCloudError.Task if the blob read failed.
     *
     * Example usage:
     *
     * ```kotlin
     * val blobInfo = SQLiteCloudBlobInfo(schema = "my_schema", table = "my_table", column = "my_blob_column")
     * val rows = listOf(1, 2, 3) // Row IDs to retrieve BLOB sizes for
     * val blobSizes = sqliteCloud.blobFieldSizes(blobInfo, rowIds)
     * ```
     */
    suspend fun blobFieldSizes(blobInfo: SQLiteCloudBlobInfo, rowIds: List<Long>): List<Int> =
        withContext(scope.coroutineContext) {
            ensureConnectedOrThrow()
            bridge.blobFieldSizes(blobInfo, rowIds)
        }

    /**
     * Read BLOB (Binary Large Object) data from the SQLite Cloud database.
     *
     * This method allows you to read BLOB data from the SQLite Cloud database. It reads BLOB
     * data for one or more rows specified by their unique row IDs and returns the BLOB data
     * as an array of `SQLiteCloudBlobReadResult` objects. The `progressHandler` callback
     * allows you to track the progress of reading the BLOB data.
     *
     * @param blob A [SQLiteCloudBlobStructure] object containing information about the BLOB field
     *            and the row IDs to read.
     * @param progressHandler An optional callback to track the progress of the BLOB data reading.
     *                       It is called with a progress value between 0.0 and 1.0 as the read
     *                       operation progresses.
     *
     * @return An array of [ByteBuffer] objects, each containing the BLOB data for a corresponding
     * row ID.
     *
     * @throws SQLiteCloudError.Connection if the connection is not invalid or a
     * network error has occurred.
     *
     * @throws SQLiteCloudError.Task if the blob read failed.
     *
     * Example usage:
     *
     * ```kotlin
     * val blobInfo = SQLiteCloudBlobInfo(schema = "my_schema", table = "my_table", column = "my_blob_column")
     * val row = SQLiteCloudBlobStructure.Row(id = 1, BlobIO.Read.Buffer)
     * val blob = SQLiteCloudBlobStructure.read(info = blobInfo, row = row)
     * val blobData = sqliteCloud.read(blob = blob) { progress ->
     *     println("Progress: ${progress * 100}%")
     * }
     * ```
     */
    suspend fun readBlob(
        blob: SQLiteCloudBlobStructure<BlobIO.Read>,
        progressHandler: ProgressHandler? = null,
    ): List<BlobIO.Write> = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        bridge.readBlob(blob, progressHandler)
    }

    /**
     * Update a SQLite Cloud BLOB data field with new content.
     *
     * This method allows you to update a BLOB (Binary Large Object) data field in the
     * SQLite Cloud database with new content. You can use it to replace the existing BLOB
     * data with new binary data, making it suitable for scenarios like image or file storage.
     * The `autoIncreaseFieldSize` property controls whether the method should
     * automatically expand the BLOB field if the new data exceeds the current size.
     *
     * @param blob A `SQLiteCloudBlobWrite` object containing information about the BLOB field
     *            and the new data to be written.
     * @param progressHandler An optional callback to track the progress of the BLOB data writing.
     *                       It is called with a progress value between 0.0 and 1.0 as the write
     *                       operation progresses.
     *
     * @throws SQLiteCloudError.Connection if the connection is not invalid or a
     *            network error has occurred.
     *
     * @throws SQLiteCloudError.Task if the blob upload failed.
     *
     * Example usage:
     *
     * ```kotlin
     * val blobInfo = SQLiteCloudBlobInfo(schema = "my_schema", table = "my_table", column = "my_blob_column")
     * val buffer = ByteBuffer.allocateDirect(size) // Replace BLOB data with new binary data
     * buffer.put(blobData)
     * buffer.clear()
     * val row = SQLiteCloudBlobStructure.Row(
     *     id = 1,
     *     dataIO = BlobIO.Write.Buffer(buffer),
     * )
     * val blob = SQLiteCloudBlob.write(
     *     info = blobInfo,
     *     rows = listOf(row),
     * )
     * sqliteCloud.updateBlob(blobWrite) { progress ->
     *     print("Progress: ${progress * 100}%")
     * }
     * ```
     */
    suspend fun updateBlob(
        blob: SQLiteCloudBlobStructure<BlobIO.Write>,
        progressHandler: ProgressHandler? = null,
    ) = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        bridge.updateBlob(blob, progressHandler)
    }

    /**
     * Compiles an SQL query into a byte-code virtual machine (VM). This method creates a
     * [SQLiteCloudVM] instance that you can use to execute the compiled SQL statement.
     *
     * @param query The SQL query to compile.
     *
     * @throws SQLiteCloudError.Connection If the connection to the SQLite Cloud backend has failed.
     *
     * @throws SQLiteCloudError If an error occurs while handling the SQLite Cloud operation.
     *
     * @return A [SQLiteCloudVM] instance representing the compiled virtual machine for the SQL
     *    query. You can use this VM to execute the SQL statement.
     *
     *  Example usage:
     *
     *  ```kotlin
     *  val query = "SELECT * FROM your_table"
     *  val vm = sqliteCloud.compileQuery(query)
     *  vm.step()
     *  ```
     */
    suspend fun compileQuery(query: String): SQLiteCloudVM = withContext(scope.coroutineContext) {
        ensureConnectedOrThrow()
        val vm = bridge.vmCompile(query)
        if (vm == null) {
            val error = error()
            logger?.logError(category = "VIRTUAL MACHINE", message = "🚨 VM compile failed: $error")
            throw error
        }
        logger?.logInfo(
            category = "VIRTUAL MACHINE",
            message = "🚀 '$query' virtual machine created successfully",
        )
        SQLiteCloudVM(vm, bridge, scope)
    }

    private suspend fun error(): SQLiteCloudError = withContext(scope.coroutineContext) {
        if (isError) {
            val code = errorCode!!
            val message = errorMessage!!

            if (isSQLiteError) {
                SQLiteCloudError.Sqlite(code, message, extendedErrorCode!!, errorOffset!!)
            } else {
                SQLiteCloudError.Connection(code, message)
            }
        }

        SQLiteCloudError.Unhandled
    }

    companion object {
        private const val tlsDefaultCertificateName = "cert.pem"
    }
}
