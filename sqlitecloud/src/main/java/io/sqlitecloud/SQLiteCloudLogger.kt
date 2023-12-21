package io.sqlitecloud

import android.util.Log

interface SQLiteCloudLogger {
    var isEnabled: Boolean

    fun logInfo(tag: String? = null, category: String? = null, message: String)
    fun logError(tag: String? = null, category: String? = null, message: String)
    fun logDebug(tag: String? = null, category: String? = null, message: String)
    fun logWarning(tag: String? = null, category: String? = null, message: String)
    fun logVerbose(tag: String? = null, category: String? = null, message: String)
}

class DefaultSQLiteCloudLogger(override var isEnabled: Boolean): SQLiteCloudLogger {
    override fun logInfo(tag: String?, category: String?, message: String) {
        if (isEnabled) {
            Log.i(tag ?: "SQLiteCloud", formatLogMessage(category, message))
        }
    }

    override fun logError(tag: String?, category: String?, message: String) {
        if (isEnabled) {
            Log.e(tag ?: "SQLiteCloud", formatLogMessage(category, message))
        }
    }

    override fun logDebug(tag: String?, category: String?, message: String) {
        if (isEnabled) {
            Log.d(tag ?: "SQLiteCloud", formatLogMessage(category, message))
        }
    }

    override fun logWarning(tag: String?, category: String?, message: String) {
        if (isEnabled) {
            Log.w(tag ?: "SQLiteCloud", formatLogMessage(category, message))
        }
    }

    override fun logVerbose(tag: String?, category: String?, message: String) {
        if (isEnabled) {
            Log.v(tag ?: "SQLiteCloud", formatLogMessage(category, message))
        }
    }

    companion object {
        private fun formatLogMessage(category: String?, message: String): String {
            return "${if (category != null) "[$category] " else ""}$message"
        }
    }

}