package com.sqlitecloud.sampleapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.sqlitecloud.SQLiteCloud
import com.sqlitecloud.SQLiteCloudConfig
import com.sqlitecloud.SQLiteCloudError
import com.sqlitecloud.sampleapp.ui.theme.SQLiteCloudSampleAppTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var sql: SQLiteCloud
    private var secrets: Secrets? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val secretsInputStream = applicationContext.resources.assets.open("secrets.json")
            secretsInputStream.reader().use {
                val json = it.readText()
                secrets = Json.decodeFromString<Secrets>(json)
            }
            secretsInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sql = SQLiteCloud(
            appContext = applicationContext,
            config = SQLiteCloudConfig(
                hostname = secrets?.hostname ?: "",
                username = secrets?.username ?: "",
                password = secrets?.password ?: "",
            ),
        )

        viewModel.viewModelScope.launch {
            val result = try {
                sql.connect()
                "success!"
            } catch (e: SQLiteCloudError) {
                e.message ?: "unknown error"
            }
            viewModel.updateText(result)
        }

        viewModel.textData.observe(this) {
            setContent {
                SQLiteCloudSampleAppTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column {
                            Text("SQLiteCloud")
                            Box(modifier = Modifier.size(width = 0.dp, height = 20.dp))
                            Text("Connection result: ${viewModel.textData.value}")
                        }
                    }
                }
            }
        }
    }
}
