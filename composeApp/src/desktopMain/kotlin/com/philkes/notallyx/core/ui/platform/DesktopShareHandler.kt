package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DesktopShareHandler : ShareHandler {

    override fun shareText(title: String, body: String) {
        runCatching {
            val content = if (title.isBlank()) body else "$title\n\n$body"
            val selection = StringSelection(content)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }
    }

    override fun shareFile(uri: PlatformUri, mimeType: String) {
        val file = uri.toFileOrNull() ?: return
        if (file.exists()) {
            openFileExternally(uri, mimeType)
        }
    }

    override fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean = runCatching {
        val file = uri.toFileOrNull() ?: return false
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file)
            true
        } else {
            false
        }
    }
        .getOrDefault(false)

    override fun openUrl(url: String): Boolean = runCatching {
        if (
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        ) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            false
        }
    }
        .getOrDefault(false)

    override fun sendBugReportEmail(recipient: String, subject: String, body: String) {
        runCatching {
            if (
                Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.MAIL)
            ) {
                val encodedSubject =
                    URLEncoder.encode(subject, StandardCharsets.UTF_8.name()).replace("+", "%20")
                val encodedBody =
                    URLEncoder.encode(body, StandardCharsets.UTF_8.name()).replace("+", "%20")
                Desktop.getDesktop()
                    .mail(URI("mailto:$recipient?subject=$encodedSubject&body=$encodedBody"))
            } else {
                shareText(subject, body)
            }
        }
    }
}
