package com.philkes.notallyx.desktop

import com.philkes.notallyx.core.platform.AppInfo

class DesktopAppInfo(
    override val versionCode: Int = 1,
    override val versionName: String = "1.0.0",
    override val model: String = System.getProperty("os.arch") ?: "JVM",
    override val device: String = System.getProperty("os.name") ?: "Desktop",
    override val brand: String = System.getProperty("java.vendor") ?: "Unknown",
    override val manufacturer: String = System.getProperty("java.vm.vendor") ?: "Unknown",
    override val osVersion: String =
        "${System.getProperty("os.name")} ${System.getProperty("os.version")} (Java ${System.getProperty("java.version")})",
) : AppInfo {
    val hostSummary: String
        get() = "$osVersion [$model]"
}
