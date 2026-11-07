package com.philkes.notallyx.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.main.PickNoteActivity

class ConfigureWidgetActivity : PickNoteActivity() {

    private val id by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = Intent()
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_CANCELED, result)
    }

    override fun onNotePicked(note: BaseNote) {
        preferences.updateWidget(id, note.id, note.type)

        val manager = AppWidgetManager.getInstance(this)
        WidgetProvider.updateWidget(application, manager, id, note.id, note.type)

        val success = Intent()
        success.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_OK, success)
        finish()
    }
}
