package com.philkes.notallyx.core.ui.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.philkes.notallyx.core.common.extensions.truncate
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.platform.PlatformUri

class AndroidShareHandler(private val context: Context) : ShareHandler {
    override fun shareText(title: String, body: String) {
        val text = body.truncate(150_000)
        val intent =
            Intent(Intent.ACTION_SEND)
                .apply {
                    type = "text/*"
                    putExtra(Intent.EXTRA_TEXT, text.toString())
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                .wrapWithChooser(context)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    override fun shareFile(uri: PlatformUri, mimeType: String) {
        val androidUri = Uri.parse(uri.uriString)
        val intent =
            Intent(Intent.ACTION_SEND)
                .apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, androidUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                .wrapWithChooser(context)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    override fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean = runCatching {
        context.viewFile(Uri.parse(uri.uriString), mimeType)
        true
    }
        .getOrDefault(false)

    override fun openUrl(url: String): Boolean = runCatching {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
        true
    }
        .getOrDefault(false)

    override fun sendBugReportEmail(recipient: String, subject: String, body: String) {
        runCatching {
            val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }
}
