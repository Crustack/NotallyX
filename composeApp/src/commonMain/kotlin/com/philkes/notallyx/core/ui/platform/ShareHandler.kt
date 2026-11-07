package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri

interface ShareHandler {
    fun shareText(title: String, body: String)

    fun shareFile(uri: PlatformUri, mimeType: String)

    fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean

    fun openUrl(url: String): Boolean

    fun sendBugReportEmail(recipient: String, subject: String, body: String)
}

class NoOpShareHandler : ShareHandler {
    override fun shareText(title: String, body: String) {}

    override fun shareFile(uri: PlatformUri, mimeType: String) {}

    override fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean = false

    override fun openUrl(url: String): Boolean = false

    override fun sendBugReportEmail(recipient: String, subject: String, body: String) {}
}
