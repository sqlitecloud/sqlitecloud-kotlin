package com.sqlitecloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * The [SQLiteCloudVM] class provides an interface for executing SQL statements with the SQLite Cloud
 * backend server. It allows you to prepare, bind, and step through SQL statements, retrieve data,
 * and handle errors. It compiles an SQL statement into a byte-code virtual machine.
 *
 * - Note: This class wraps the underlying SQLite Cloud Virtual Machine C functions and simplifies
 *  its usage in Swift.
 *
 * ## Initialization
 * To use [SQLiteCloudVM], create an instance of this class using [SQLiteCloud].[compileQuery]
 * method, which compiles SQL queries into virtual machines.
 *
 * Example usage:
 *
 * ```kotlin
 * val query = "SELECT * FROM your_table"
 * val vm = sqliteCloud.compileQuery(query)
 * ```
 */
class SQLiteCloudVM internal constructor(
    val vm: OpaquePointer<SQLiteCloudVM>,
    private val bridge: SQLiteCloudBridge,
    private val scope: CoroutineScope,
) {
    /**
     * Binds a value to a parameter in the compiled SQL query represented by this virtual machine.
     *
     * Use this method to bind different types of values, such as integers, strings, blobs, or
     * nulls, to parameters in a compiled SQL query. The query is executed when you call
     * the [step] method on the virtual machine.
     *
     * @param value The value to bind to the parameter.
     * @param index The index of the parameter in the SQL query.
     *
     * @return A boolean indicating whether the binding was successful.
     * @throws SQLiteCloudError if there is an issue with the binding
     *           operation or if the parameter index is out of bounds
     *
     * Example usage:
     *
     * ```kotlin
     * try {
     *      // Create a virtual machine.
     *      val query = "INSERT INTO employees (name, age) VALUES (?1, ?2)"
     *      val vm = sqliteCloud.compileQuery(query)
     *      // Bind values to parameters in the SQL query.
     *      val name = "John Doe"
     *      val age = 30
     *      val isBindingSuccessful = vm.bindValue(SQLiteCloudVMValue.String(name), rowIndex = 1) &&
     *                                vm.bindValue(SQLiteCloudVMValue.Integer(age), rowIndex = 2)
     *      // Execute the query using the virtual machine.
     *      if (isBindingSuccessful) {
     *          vm.step()
     *      }
     *  } catch (error) {
     *      // Handle any errors that occur during the binding or execution.
     *      print("Error: $error")
     *  }
     * ```
     */
    suspend fun bindValue(value: SQLiteCloudVMValue, rowIndex: Int): Boolean =
        withContext(scope.coroutineContext) {
            val success: Boolean = when (value) {
                is SQLiteCloudVMValue.Integer ->
                    bridge.vmBindInt(vm, rowIndex, value.value)

                is SQLiteCloudVMValue.Integer64 ->
                    bridge.vmBindInt64(vm, rowIndex, value.value)

                is SQLiteCloudVMValue.Double ->
                    bridge.vmBindDouble(vm, rowIndex, value.value)

                is SQLiteCloudVMValue.String ->
                    bridge.vmBindText(vm, rowIndex, value.value, value.stringByteSize)

                is SQLiteCloudVMValue.Blob ->
                    bridge.vmBindBlob(vm, rowIndex, value.value.slice())

                is SQLiteCloudVMValue.BlobZero ->
                    bridge.vmBindZeroBlob(vm, rowIndex)

                is SQLiteCloudVMValue.Null ->
                    bridge.vmBindNull(vm, rowIndex)
            }

            success
        }

    /**
     * Executes a single step in a prepared SQLite query. This method advances the virtual
     * machine's program counter, processing the next operation in the SQLite query.
     *
     * This method is used when working with a prepared SQLite query in a virtual machine
     * (VM). It advances the VM's program counter and performs the next operation in the
     * query. If an error occurs during execution, a corresponding error is thrown,
     * encapsulated within a [SQLiteCloudError].
     *
     * @throws SQLiteCloudError if an issue occurs during the execution
     *           of the query.
     */
    suspend fun step() = withContext(scope.coroutineContext) {
        val result = bridge.vmStep(vm)
        val resultType = SQLiteCloudResult.Type.fromRawValue(result)
        if (resultType == SQLiteCloudResult.Type.ERROR) {
            throw bridge.vmError(vm)
        }
    }

    /**
     * Retrieves all values from the current row of a virtual machine (VM) that
     * represents the result of an SQLite query.
     *
     * This method fetches all the values from the current row of the virtual machine.
     * It's useful when working with the results of a prepared SQLite query. The
     * values are returned as an array of [SQLiteCloudVMValue] objects.
     *
     * @throws SQLiteCloudError if an issue occurs while retrieving the values.
     *
     * @return An array of [SQLiteCloudVMValue] objects, each representing a value
     *            in the current row of the SQLite query result.
     */
    suspend fun getValues(): List<SQLiteCloudVMValue> = withContext(scope.coroutineContext) {
        (0..<columnCount()).map { index ->
            getValue(index)
        }
    }

    /**
     * Retrieves the value at a specified column index from the current row of a
     * virtual machine (VM) representing an SQLite query result.
     *
     * This method fetches the value at the specified column index in the current
     * row of the virtual machine. The retrieved value is returned as an
     * [SQLiteCloudVMValue] object.
     *
     * @param index: The zero-based index of the column to retrieve the value from.
     *
     * @throws SQLiteCloudError if an issue occurs while retrieving
     *           the value or if the value's type is unsupported.
     *
     * @return A [SQLiteCloudVMValue] object representing the value at the specified
     *            column index in the current row of the SQLite query result.
     */
    suspend fun getValue(index: Int): SQLiteCloudVMValue = withContext(scope.coroutineContext) {
        val valueType = columnType(index)

        when (valueType) {
            SQLiteCloudValue.Type.Integer -> intergerValue(index)
            SQLiteCloudValue.Type.Double -> doubleValue(index)
            SQLiteCloudValue.Type.String -> textValue(index)
            SQLiteCloudValue.Type.Blob -> blobValue(index)
            SQLiteCloudValue.Type.Null -> nullValue(index)
            else -> throw SQLiteCloudError.Execution.unsupportedValueType
        }
    }

    /**
     * Closes a SQLite virtual machine (VM) associated with a prepared statement.
     *
     * This method is used to close a virtual machine (VM) that was previously
     * prepared for executing an SQLite query. Closing the VM releases any resources
     * associated with it and finalizes the prepared statement.
     *
     * - Note: Closing the VM is important to free up resources and maintain the
     *         integrity of the SQLite database.
     *
     * @throws SQLiteCloudError if an issue occurs while closing the VM.
     */
    suspend fun close() = withContext(scope.coroutineContext) {
        val success = bridge.vmClose(vm)
        if (!success) {
            throw bridge.vmError(vm)
        }
    }

    /**
     * Returns the number of columns in the result set of the executed query.
     *
     * This method retrieves the count of columns in the result set produced
     * by the most recent execution of the SQL query. It is typically used
     * after executing a query to determine the number of columns in the result set.
     *
     * @return An [Int] value representing the number of columns in the result set.
     */
    suspend fun columnCount(): Int = withContext(scope.coroutineContext) {
        bridge.vmColumnCount(vm)
    }


    /**
     * Returns the identifier of the last inserted row.
     *
     * This method retrieves the identifier (row ID) of the last row inserted into
     * the database. It is useful for retrieving the primary key value of the most
     * recently added record.
     *
     * @return A [Long] value representing the identifier (row ID) of the last inserted row.
     */
    suspend fun lastRowID(): Long = withContext(scope.coroutineContext) {
        bridge.vmLastRowID(vm)
    }

    /**
     * Retrieves the number of rows that were modified, inserted, or deleted by the
     * most recent query execution.
     *
     * This method returns the total number of rows inserted, modified or deleted by all
     * INSERT, UPDATE or DELETE statements completed since the database connection was
     * opened, including those executed as part of trigger programs. Executing any other
     * type of SQL statement does not affect the value returned by this method.
     * Changes made as part of foreign key actions are included in the count, but those
     * made as part of REPLACE constraint resolution are not. Changes to a view that are
     * intercepted by INSTEAD OF triggers are not counted.
     *
     * - Note: If you need to get the total changes from a SQCloudConnection object
     *         you can send a DATABASE GET TOTAL CHANGES command.
     *
     * @return A [Long] value representing the number of rows that were modified,
     *            inserted, or deleted.
     */
    suspend fun changes(): Long = withContext(scope.coroutineContext) {
        bridge.vmChanges(vm)
    }

    /**
     * Retrieves the total number of rows that were modified, inserted, or deleted
     * since the database connection was opened.
     *
     * This method returns the total number of rows that were modified, inserted, or
     * deleted since the database connection was opened. It provides a cumulative count
     * of changes made during the current database connection.
     *
     * @return An [Long] value representing the total number of rows that were
     *            modified, inserted, or deleted since the database connection was opened.
     */
    suspend fun totalChanges(): Long = withContext(scope.coroutineContext) {
        bridge.vmTotalChanges(vm)
    }

    /**
     * Checks whether the virtual machine is read-only.
     *
     * This method returns true if and only if the prepared statement bound to vm makes
     * no direct changes to the content of the database file. This routine returns false
     * if there is any possibility that the statement might change the database file.
     * A false return does not guarantee that the statement will change the database file.
     *
     * @return [true] if the virtual machine is in read-only mode, [false] otherwise.
     */
    suspend fun isReadOnly(): Boolean = withContext(scope.coroutineContext) {
        bridge.vmIsReadOnly(vm)
    }

    /**
     * Checks whether the virtual machine is in "EXPLAIN" mode.
     *
     * This method returns 1 if the prepared statement is an EXPLAIN statement,
     * or 2 if the statement S is an EXPLAIN QUERY PLAN. This method returns 0 if
     * the statement is an ordinary statement or a NULL pointer.
     *
     * @return An [Int] value indicating whether the virtual machine is in "EXPLAIN" mode.
     */
    suspend fun isExplain(): Int = withContext(scope.coroutineContext) {
        bridge.vmIsExplain(vm)
    }

    /**
     * Checks whether the virtual machine has been finalized.
     *
     * This method returns true if the prepared statement bound to the vm has been
     * stepped at least once but has neither run to completion nor been reset.
     *
     * @return [true] if the virtual machine has been finalized, [false] otherwise.
     */
    suspend fun isFinalized(): Boolean = withContext(scope.coroutineContext) {
        bridge.vmIsFinalized(vm)
    }

    /**
     * Retrieves the number of SQL parameters in a prepared statement.
     *
     * This method can be used to find the number of SQL parameters in a prepared statement.
     * SQL parameters are tokens of the form "?", "?NNN", ":AAA", "$AAA", or "@AAA" that
     * serve as placeholders for values that are bound to the parameters at a later time.
     * This method actually returns the index of the largest (rightmost) parameter. For
     * all forms except ?NNN, this will correspond to the number of unique parameters.
     * If parameters of the ?NNN form are used, there may be gaps in the list.
     *
     * @return An [Int] value representing the number of parameters in a prepared statement.
     */
    suspend fun bindParameterCount(): Int = withContext(scope.coroutineContext) {
        bridge.vmBindParameterCount(vm)
    }

    /**
     * Retrieves the index of a parameter by its name.
     *
     * This method retrieves the index of a parameter by its name. It is typically used
     * to find the index of a named parameter that can be subsequently bound to a value
     * using the [bindValue] method.
     *
     * @param name The name of the parameter to find.
     * @return An [Int] value representing the index of the specified parameter.
     */
    suspend fun bindParameterIndex(name: String): Int = withContext(scope.coroutineContext) {
        bridge.vmBindParameterIndex(vm, name)
    }

    /**
     * Retrieves the name of a parameter by its index.
     *
     * This method retrieves the name of a parameter by its index. It is typically used
     * to find the name of a parameter after specifying it by index with the
     * [bindParameterIndex] method.
     *
     * @param index The index of the parameter for which the name is to be determined.
     *
     * @throws SQLiteCloudError if an issue occurs while retrieving
     *           the parameter name. The error may indicate an invalid parameter index.
     *
     * @return A [String] value representing the name of the specified parameter.
     */
    suspend fun bindParameterName(index: Int): String = withContext(scope.coroutineContext) {
        bridge.vmBindParameterName(vm, index)
            ?: throw SQLiteCloudError.VirtualMachine.invalidParameterIndex(index)
    }

    /**
     * Retrieves the data type of a column by its index.
     *
     * This method retrieves the data type of a column in the result set by its index.
     * It returns an enumeration value representing the type of data stored in the specified column.
     *
     * @param index The index of the column for which the data type is to be determined.
     *
     * @return A [SQLiteCloudValue.Type] enumeration value representing the data type of
     *           the specified column.
     */
    suspend fun columnType(index: Int): SQLiteCloudValue.Type = withContext(scope.coroutineContext) {
        try {
            SQLiteCloudValue.Type.fromRawValue(bridge.vmColumnType(vm, index))
        } catch (e: Error) {
            SQLiteCloudValue.Type.Unknown
        }
    }

    /**
     * Retrieves the name of a column by its index.
     *
     * This method retrieves the name of a column in the result set by its index.
     * It returns the name as a string.
     *
     * @param index The index of the column for which the name is to be determined.
     *
     * @return A [String] value representing the name of the specified column.
     */
    suspend fun columnName(index: Int): String = withContext(scope.coroutineContext) {
        bridge.rowsetColumnName(bridge.vmResult(vm), index)
    }

    /**
     * Retrieves an integer value at the specified column index.
     *
     * This method retrieves an integer value at the specified column index from
     * the current row of the virtual machine's result set.
     *
     * @param index The index of the column to retrieve.
     *
     * @return A [SQLiteCloudVMValue] containing the integer value found at
     *            the specified column index.
     */
    suspend fun intergerValue(index: Int): SQLiteCloudVMValue = withContext(scope.coroutineContext) {
        SQLiteCloudVMValue.Integer64(bridge.vmColumnInt64(vm, index))
    }

    /**
     * Retrieves a double value at the specified column index.
     *
     * This method retrieves a double value at the specified column index from the
     * current row of the virtual machine's result set.
     *
     * @param index The index of the column to retrieve.
     *
     * @return A [SQLiteCloudVMValue] containing the double value found at the
     *            specified column index.
     */
    suspend fun doubleValue(index: Int): SQLiteCloudVMValue = withContext(scope.coroutineContext) {
        SQLiteCloudVMValue.Double(bridge.vmColumnDouble(vm, index))
    }

    /**
     * Retrieves a text (string) value at the specified column index.
     *
     * This method retrieves a text (string) value at the specified column index
     * from the current row of the virtual machine's result set.
     *
     * @param index The index of the column to retrieve.
     *
     * @return A [SQLiteCloudVMValue] containing the text (string) value found
     *            at the specified column index.
     */
    suspend fun textValue(index: Int): SQLiteCloudVMValue = withContext(scope.coroutineContext) {
        SQLiteCloudVMValue.String(bridge.vmColumnText(vm, index))
    }

    /**
     * Retrieves a blob (binary data) value at the specified column index.
     *
     * This method retrieves a blob (binary data) value at the specified column
     * index from the current row of the virtual machine's result set.
     *
     * @param index The index of the column to retrieve.
     *
     * @return A [SQLiteCloudVMValue] containing the blob (binary data) value
     *            found at the specified column index.
     */
    suspend fun blobValue(index: Int): SQLiteCloudVMValue = withContext(scope.coroutineContext) {
        SQLiteCloudVMValue.Blob(bridge.vmColumnBlob(vm, index))
    }

    /**
     * Retrieves a null value at the specified column index.
     *
     * This method retrieves a null value at the specified column index from the
     * current row of the virtual machine's result set.
     *
     * @param index The index of the column to retrieve.
     *
     * @return A [SQLiteCloudVMValue] containing a null value.
     */
    fun nullValue(index: Int) = SQLiteCloudVMValue.Null
}
