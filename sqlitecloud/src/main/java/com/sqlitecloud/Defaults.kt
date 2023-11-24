package com.sqlitecloud

const val defaultBlobSizeThreshold = 1_048_576 // 1MB
const val defaultBlobNumOfParts = 20
fun defaultBlobChunkCount(blobSize: Int) = blobSize / defaultBlobNumOfParts

typealias Callback<A> = (A) -> Unit
typealias ProgressHandler = (Double) -> Unit
typealias NotificationHandler = Callback<SQLiteCloudPayload>
