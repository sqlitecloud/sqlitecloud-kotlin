package com.sqlitecloud

data class SQLiteCloudCommand(
    val query: String,
    val parameters: List<SQLiteCloudValue> = emptyList(),
) {
    constructor(
        query: String,
        vararg parameters: SQLiteCloudValue,
    ) : this(query, parameters.toList())

    companion object {
        fun expandBlobField(
            table: String,
            column: String,
            rowId: Long,
            size: Long,
        ): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "UPDATE $table SET $column = zeroblob(?) WHERE rowId = ?;",
                parameters = listOf(
                    SQLiteCloudValue.Integer(size),
                    SQLiteCloudValue.Integer(rowId)
                ),
            )
        }

        fun useDatabase(name: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "USE DATABASE ?;",
                parameters = listOf(SQLiteCloudValue.String(name)),
            )
        }

        fun getClient(keyname: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET CLIENT KEY ?",
                parameters = listOf(SQLiteCloudValue.String(keyname)),
            )
        }

        fun getKey(keyname: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET KEY",
                parameters = listOf(SQLiteCloudValue.String(keyname)),
            )
        }

        fun getRuntimeKey(keyname: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET RUNTIME KEY ?",
                parameters = listOf(SQLiteCloudValue.String(keyname)),
            )
        }

        fun getDatabaseKey(database: String, keyname: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET DATABASE ? KEY ?",
                parameters = listOf(
                    SQLiteCloudValue.String(database),
                    SQLiteCloudValue.String(keyname)
                ),
            )
        }

        val getLeader get() = SQLiteCloudCommand(query = "GET LEADER;")
        val getLeaderId get() = SQLiteCloudCommand(query = "GET LEADER ID;")

        // General

        /// The LIST TABLES command retrieves the information about the tables available inside the current database.
        /// Note that the output of this command can change depending on the privileges associated with the currently
        /// connected username. If the PUBSUB parameter is used, then the output will contain the column chname only
        /// (to have the same format as the rowset returned by the LIST CHANNELS command).
        val listTables get() = SQLiteCloudCommand(query = "LIST TABLES;")

        /// The GET INFO command retrieves a single specific information about a key. The NODE argument forces the
        /// execution of the command to a specific node of the cluster.
        ///
        /// - Parameter key: The information key.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun getInfo(key: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET INFO ?",
                parameters = listOf(SQLiteCloudValue.String(key))
            )
        }

        /// The GET SQL command retrieves the SQL statement used to generate the table name.
        ///
        /// - Parameter tableName: The name of the table.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun getSQL(tableName: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "GET SQL ?;",
                parameters = listOf(SQLiteCloudValue.String(tableName))
            )
        }

        // User

        /// The GET USER command returns the username of the currency-connected user.
        val getUser get() = SQLiteCloudCommand(query = "GET USER;")

        /// The CREATE USER command adds a new user username with a specified password to the server.
        /// During user creation, you can also pass a comma-separated list of roles to apply to that user.
        /// The DATABASE and/or TABLE arguments can further restrict the which resources the user can access.
        ///
        /// - Parameters:
        ///   - username: The username.
        ///   - password: The user password.
        ///   - roles: A comma-separated list of roles.
        ///   - database: The database name.
        ///   - table: The table name.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun createUser(
            username: String,
            password: String,
            roles: String? = null,
            database: String? = null,
            table: String? = null
        ): SQLiteCloudCommand {
            var query = "CREATE USER ? PASSWORD ?"

            val parameters =
                mutableListOf(SQLiteCloudValue.String(username), SQLiteCloudValue.String(password))
            if (roles != null) {
                query += " ROLE ?"
                parameters.add(SQLiteCloudValue.String(roles))
            }

            if (database != null) {
                query += " DATABASE ?"
                parameters.add(SQLiteCloudValue.String(database))
            }

            if (table != null) {
                query += " TABLE ?"
                parameters.add(SQLiteCloudValue.String(table))
            }

            query += ";"
            return SQLiteCloudCommand(query = query, parameters = parameters)
        }


        /// The LIST CHANNELS command returns a list of previously created channels that can be used to
        /// exchange messages. This command returns only channels created with the CREATE CHANNEL command.
        /// You can also subscribe to a table to receive all table-related events (INSERT, UPDATE, and DELETE).
        /// The LIST TABLES PUBSUB return a rowset compatible with the rowset returned by the LIST CHANNELS command.
        val listChannels = SQLiteCloudCommand(query = "LIST CHANNELS;")

        /// The LISTEN command is used to start receiving notifications for a given channel. Nothing is done
        /// if the current connection is registered as a listener for this notification channel.
        ///
        /// - Parameter channel: The channel name.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun listenToChannel(channel: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "LISTEN ?;",
                parameters = listOf(SQLiteCloudValue.String(channel)),
            )
        }

        /// The LISTEN command is used to start receiving notifications for a given table. Nothing is done
        /// if the current connection is registered as a listener for this notification channel.
        ///
        /// LISTENING to a table means you'll receive notification about all the write operations in that
        /// table. In the case of TABLE, the channel_name can be *, which means you'll start receiving notifications
        /// from all the tables inside the current database.
        ///
        /// - Parameter table: The table name or * to listen all tables.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun listenToTable(table: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "LISTEN TABLE ?;",
                parameters = listOf(SQLiteCloudValue.String(table)),
            )
        }

        /// The CREATE CHANNEL command creates a new Pub/Sub environment channel. It is usually an error to attempt
        /// to create a new channel if another one exists with the same name. However, if the `ifNotExists` parameter is `true`
        /// and a channel of the same name already exists, the CREATE CHANNEL command has no effect (and no error message
        /// is returned). An error is still returned if the channel cannot be created for any other reason, even if
        /// the `ifNotExists` parameter is `true`.
        ///
        /// - Parameters:
        ///   - channel: The channel name to create.
        ///   - ifNotExists: Create channel only if not already exist.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun createChannel(channel: String, ifNotExists: Boolean): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "CREATE CHANNEL ?${if (ifNotExists) " IF NOT EXISTS" else ""};",
                parameters = listOf(SQLiteCloudValue.String(channel)),
            )
        }

        /// The NOTIFY command sends an optional payload (usually a string) to a specified channel name.
        /// If no payload is specified, then an empty notification is sent.
        ///
        /// - Parameters:
        ///   - channel: The channel on which to send the message.
        ///   - payload: The message payload, usually a string.
        /// - Returns: An instance of `SQLiteCloudCommand`.
        fun notify(channel: String, payload: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "NOTIFY ? '?'",
                parameters = listOf(
                    SQLiteCloudValue.String(channel),
                    SQLiteCloudValue.String(payload)
                ),
            )
        }

        fun removeChannel(channel: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "REMOVE CHANNEL ?",
                parameters = listOf(SQLiteCloudValue.String(channel)),
            )
        }

        fun unlistenToChannel(channel: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "UNLISTEN ?",
                parameters = listOf(SQLiteCloudValue.String(channel)),
            )
        }

        fun unlistenToTable(table: String): SQLiteCloudCommand {
            return SQLiteCloudCommand(
                query = "UNLISTEN ?",
                parameters = listOf(SQLiteCloudValue.String(table)),
            )
        }

        fun listen(channel: SQLiteCloudChannel): SQLiteCloudCommand {
            return when (channel) {
                is SQLiteCloudChannel.Channel ->
                    listenToChannel(channel = channel.name)

                is SQLiteCloudChannel.Table, is SQLiteCloudChannel.AllTables ->
                    listenToTable(table = channel.name)

            }
        }

        fun unlisten(channel: SQLiteCloudChannel): SQLiteCloudCommand {
            return when (channel) {
                is SQLiteCloudChannel.Channel ->
                    unlistenToChannel(channel = channel.name)

                is SQLiteCloudChannel.Table, SQLiteCloudChannel.AllTables ->
                    unlistenToTable(table = channel.name)
            }
        }

        // Test

        // The TEST command is used for debugging purposes and can be used by developers while developing the SCSP for a new language.
        // By specifying a different test_name, the server will reply with different responses so you can test the parsing capabilities
        // of your new binding. Supported test_name are: STRING, STRING0, ZERO_STRING, ERROR, EXTERROR, INTEGER, FLOAT, BLOB, BLOB0,
        // ROWSET, ROWSET_CHUNK, JSON, NULL, COMMAND, ARRAY, ARRAY0.
        val testCommand get() = SQLiteCloudCommand(query = "TEST COMMAND;")
        val testNull get() = SQLiteCloudCommand(query = "TEST NULL;")
        val testArray get() = SQLiteCloudCommand(query = "TEST ARRAY;")
        val testArray0 get() = SQLiteCloudCommand(query = "TEST ARRAY0;")
        val testJson get() = SQLiteCloudCommand(query = "TEST JSON;")
        val testBlob get() = SQLiteCloudCommand(query = "TEST BLOB;")
        val testBlob0 get() = SQLiteCloudCommand(query = "TEST BLOB0;")
        val testError get() = SQLiteCloudCommand(query = "TEST ERROR;")
        val testExtError get() = SQLiteCloudCommand(query = "TEST EXTERROR;")
        val testInteger get() = SQLiteCloudCommand(query = "TEST INTEGER;")
        val testFloat get() = SQLiteCloudCommand(query = "TEST FLOAT;")
        val testString get() = SQLiteCloudCommand(query = "TEST STRING;")
        val testString0 get() = SQLiteCloudCommand(query = "TEST STRING0;")
        val testZeroString get() = SQLiteCloudCommand(query = "TEST ZERO_STRING;")
        val testRowset get() = SQLiteCloudCommand(query = "TEST ROWSET;")
        val testRowsetChunk get() = SQLiteCloudCommand(query = "TEST ROWSET_CHUNK;")
    }
}
