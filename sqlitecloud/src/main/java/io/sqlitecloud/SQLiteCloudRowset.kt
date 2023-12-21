package io.sqlitecloud

/// Represents a result set in SQLite Cloud.
///
/// The `SQLiteCloudRowset` data class is used to represent a result set returned from a
/// query in SQLite Cloud. It consists of two properties: `columns` and `rows`. The
/// `columns` property is an array of column names, and the `rows` property is an array
/// of arrays, where each inner array represents a row of data, and the elements within
/// it are `SQLiteCloudValue` instances.
///
/// - Note: This struct is commonly used to retrieve and work with query results when
///         interacting with SQLite Cloud.
data class SQLiteCloudRowset(val columns: List<String>, val rows: List<List<SQLiteCloudValue>>)
