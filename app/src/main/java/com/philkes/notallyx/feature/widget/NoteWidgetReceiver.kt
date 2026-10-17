package com.philkes.notallyx.feature.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.philkes.notallyx.core.preference.NotallyXPreferences

class NoteWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NoteWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            NoteWidget.ACTION_NOTES_MODIFIED,
            Intent.ACTION_LOCALE_CHANGED -> {
                val noteIds = intent.getLongArrayExtra(NoteWidget.EXTRA_MODIFIED_NOTES)
                NoteWidget.updateWidgets(context, noteIds)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext as Application
        val preferences = NotallyXPreferences.getInstance(app)

        appWidgetIds.forEach { id -> preferences.deleteWidget(id) }
    }
}
