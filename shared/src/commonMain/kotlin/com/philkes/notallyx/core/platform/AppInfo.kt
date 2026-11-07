package com.philkes.notallyx.core.platform

/**
 * Provides application version and runtime environment metadata without depending on Android's
 * generated `BuildConfig` or `android.os.Build` in shared code.
 */
interface AppInfo {
    val versionCode: Int
    val versionName: String
    val model: String
    val device: String
    val brand: String
    val manufacturer: String
    val osVersion: String
}

object AppInfoProvider {
    @Volatile
    var current: AppInfo =
        object : AppInfo {
            override val versionCode: Int = 1
            override val versionName: String = "dev"
            override val model: String = System.getProperty("os.arch") ?: "JVM"
            override val device: String = "Desktop"
            override val brand: String = "JVM"
            override val manufacturer: String = System.getProperty("java.vendor") ?: "JVM"
            override val osVersion: String = System.getProperty("os.name") ?: "JVM"
        }
}
