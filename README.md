# SQLiteCloud Kotlin Library

![sqlitecloud-logo](https://github.com/sqlitecloud/sqlitecloud-kotlin/assets/3525511/392efdae-44a5-4a8c-9dd9-3eba99620108)

SQLiteCloud is a powerful Kotlin library that allows you to interact with the SQLite Cloud backend server seamlessly. It provides methods for various database operations and real-time notifications. This package is designed to simplify database operations in Android applications, making it easier than ever to work with SQLite Cloud.

## Features

- **Database Operations**: Easily perform database operations, including queries, updates, inserts, and more.

- **Real-time Notifications**: Get real-time notifications from the SQLite Cloud backend server.

- **Efficient**: SQLiteCloud is designed for efficiency, ensuring that your database operations are fast and reliable.

## Installation

You can install the SQLiteCloud Kotlin library by downloading this repository and adding the `sqlitecloud` module into your project (for example, if you're using Android Studio, place the `sqlitecloud` directory within your project's root directory, select the menu `File > New > Import Module...` and select such directory). Then, in your project's `build.gradle`, add the following line to the `dependencies` section:
```
    implementation(project(mapOf("path" to ":sqlitecloud")))
```

## Usage

#### Using explicit configuration

```kotlin
val configuration = SQLiteCloudConfig(hostname = "myproject.sqlite.cloud", username = "", password = "")
val sqliteCloud = SQLiteCloud(appContext = applicationContext, config = configuration)

myCoroutineScope.launch {
    val result = try {
        sqliteCloud.connect()
        "connected"
    } catch (e: SQLiteCloudError) {
        "connection error: ${e.message ?: "unknown error"}"
    }
    Log.d("MyTag", result)
}
```

#### Using string configuration

```kotlin
let configuration = SQLiteCloudConfig.fromString("sqlitecloud://user:pass@host.com:port/dbname?timeout=10&key2=value2&key3=value3")
val sqliteCloud = SQLiteCloud(appContext = applicationContext, config = configuration)

myCoroutineScope.launch {
    val result = try {
        sqliteCloud.connect()
        "connected"
    } catch (e: SQLiteCloudError) {
        "connection error: ${e.message ?: "unknown error"}"
    }
    Log.d("MyTag", result)
}
```

## License
SQLiteCloud is licensed under the MIT License. See the LICENSE file for details.
