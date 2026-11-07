package com.philkes.notallyx.feature.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getOpenNoteIntent
import com.philkes.notallyx.core.common.extensions.isSystemInDarkMode
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.displayBodySize
import com.philkes.notallyx.core.preference.displayTitleSize
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.getContrastFontColor
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as NotallyXApplication
        val preferences = NotallyXPreferences.getInstance(app)

        val appWidgetId =
            try {
                GlanceAppWidgetManager(context).getAppWidgetId(id)
            } catch (_: Exception) {
                AppWidgetManager.INVALID_APPWIDGET_ID
            }

        provideContent {
            val prefs = currentState<Preferences>()
            var noteId = prefs[NoteIdKey] ?: 0L
            if (noteId == 0L && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                noteId = preferences.getWidgetData(appWidgetId)
            }

            val updatedAt = prefs[UpdatedAtKey] ?: 0L

            val lockedState = app.locked.collectAsState()
            val locked = preferences.isLockEnabled && lockedState.value

            val noteState =
                produceState<BaseNote?>(initialValue = null, key1 = noteId, key2 = updatedAt) {
                    value =
                        withContext(Dispatchers.IO) {
                            if (noteId != 0L) {
                                val database = NotallyDatabase.getDatabase(app).value
                                database?.getBaseNoteDao()?.get(noteId)
                            } else null
                        }
                }

            val titleTextSize = preferences.textSizeNoteEditor.value.displayTitleSize.sp
            val bodyTextSize = preferences.textSizeNoteEditor.value.displayBodySize.sp
            val theme = preferences.theme.value

            NoteWidgetContent(
                appWidgetId = appWidgetId,
                note = noteState.value,
                locked = locked,
                theme = theme,
                titleTextSize = titleTextSize,
                bodyTextSize = bodyTextSize,
            )
        }
    }

    companion object {
        val NoteIdKey = longPreferencesKey("widget_note_id")
        val NoteTypeKey = stringPreferencesKey("widget_note_type")
        val UpdatedAtKey = longPreferencesKey("widget_updated_at")

        fun updateWidgets(context: Context, noteIds: LongArray? = null) {
            MainScope().launch(Dispatchers.IO) { updateWidgetsSuspend(context, noteIds) }
        }

        suspend fun updateWidgetsSuspend(context: Context, noteIds: LongArray? = null) =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext as Application
                val preferences = NotallyXPreferences.getInstance(app)

                val updatableWidgets = preferences.getUpdatableWidgets(noteIds)
                if (updatableWidgets.isEmpty()) {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceWidget = NoteWidget()
                    try {
                        val glanceIds = manager.getGlanceIds(NoteWidget::class.java)
                        for (glanceId in glanceIds) {
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs[UpdatedAtKey] = System.currentTimeMillis()
                            }
                        }
                        glanceWidget.updateAll(context)
                    } catch (_: Exception) {
                        glanceWidget.updateAll(context)
                    }
                } else {
                    updatableWidgets.forEach { (appWidgetId, nId) ->
                        val type = preferences.getWidgetNoteType(appWidgetId)
                        updateWidgetSuspend(context, appWidgetId, nId, type)
                    }
                }
            }

        fun updateWidget(
            context: Context,
            appWidgetId: Int,
            noteId: Long? = null,
            noteType: Type? = null,
        ) {
            MainScope().launch(Dispatchers.IO) {
                updateWidgetSuspend(context, appWidgetId, noteId, noteType)
            }
        }

        suspend fun updateWidgetSuspend(
            context: Context,
            appWidgetId: Int,
            noteId: Long? = null,
            noteType: Type? = null,
        ) =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext as Application
                val preferences = NotallyXPreferences.getInstance(app)
                val targetNoteId = noteId ?: preferences.getWidgetData(appWidgetId)
                val targetNoteType = noteType ?: preferences.getWidgetNoteType(appWidgetId)

                val manager = GlanceAppWidgetManager(context)
                val glanceWidget = NoteWidget()

                try {
                    val glanceId = manager.getGlanceIdBy(appWidgetId)
                    if (targetNoteId != 0L && targetNoteType != null) {
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[NoteIdKey] = targetNoteId
                            prefs[NoteTypeKey] = targetNoteType.name
                            prefs[UpdatedAtKey] = System.currentTimeMillis()
                        }
                    }
                    glanceWidget.update(context, glanceId)
                } catch (_: Exception) {
                    try {
                        val glanceIds = manager.getGlanceIds(NoteWidget::class.java)
                        var updated = false
                        for (glanceId in glanceIds) {
                            val id =
                                try {
                                    manager.getAppWidgetId(glanceId)
                                } catch (_: Exception) {
                                    -1
                                }
                            if (id == appWidgetId) {
                                if (targetNoteId != 0L && targetNoteType != null) {
                                    updateAppWidgetState(context, glanceId) { prefs ->
                                        prefs[NoteIdKey] = targetNoteId
                                        prefs[NoteTypeKey] = targetNoteType.name
                                        prefs[UpdatedAtKey] = System.currentTimeMillis()
                                    }
                                }
                                glanceWidget.update(context, glanceId)
                                updated = true
                                break
                            }
                        }
                        if (!updated) {
                            for (glanceId in glanceIds) {
                                updateAppWidgetState(context, glanceId) { prefs ->
                                    prefs[UpdatedAtKey] = System.currentTimeMillis()
                                }
                            }
                            glanceWidget.updateAll(context)
                        }
                    } catch (_: Exception) {
                        glanceWidget.updateAll(context)
                    }
                }
            }

        fun sendBroadcast(context: Context, ids: LongArray) {
            val intent =
                Intent(context, NoteWidgetReceiver::class.java).apply {
                    action = ACTION_NOTES_MODIFIED
                    putExtra(EXTRA_MODIFIED_NOTES, ids)
                }
            context.sendBroadcast(intent)
        }

        fun createOpenNoteIntent(context: Context, note: BaseNote): Intent {
            return context.getOpenNoteIntent(note.id, note.type).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }

        fun createSelectNoteIntent(context: Context, appWidgetId: Int): Intent {
            return Intent(context, ConfigureWidgetActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }

        fun Context.extractWidgetColors(
            color: String,
            theme: Theme,
        ): Pair<Int, Int> {
            val backgroundColor =
                if (color == BaseNote.COLOR_DEFAULT) {
                    val id =
                        when (theme) {
                            Theme.DARK -> R.color.ContainerDark
                            Theme.SUPER_DARK -> R.color.ContainerSuperDark
                            Theme.LIGHT -> R.color.ContainerLight
                            Theme.FOLLOW_SYSTEM -> {
                                if (isSystemInDarkMode()) R.color.ContainerDark
                                else R.color.ContainerLight
                            }
                        }
                    ContextCompat.getColor(this, id)
                } else extractColor(color)
            return Pair(backgroundColor, getContrastFontColor(backgroundColor))
        }

        const val ACTION_NOTES_MODIFIED = "com.philkes.notallyx.ACTION_NOTE_MODIFIED"
        const val EXTRA_MODIFIED_NOTES = "com.philkes.notallyx.EXTRA_MODIFIED_NOTES"
    }
}

@Composable
internal fun NoteWidgetContent(
    appWidgetId: Int,
    note: BaseNote?,
    locked: Boolean,
    theme: Theme,
    titleTextSize: TextUnit,
    bodyTextSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    if (note == null) {
        EmptyWidgetView(appWidgetId = appWidgetId, modifier = modifier)
        return
    }

    if (locked) {
        LockedWidgetView(note = note, modifier = modifier)
        return
    }

    val context = LocalContext.current
    val (backgroundColorInt, controlsColorInt) =
        remember(note.color, theme) {
            with(NoteWidget) { context.extractWidgetColors(note.color, theme) }
        }
    val backgroundColor = Color(backgroundColorInt)
    val controlsColor = Color(controlsColorInt)

    val openIntent = NoteWidget.createOpenNoteIntent(context, note)
    val selectNoteIntent = NoteWidget.createSelectNoteIntent(context, appWidgetId)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .padding(12.dp)
                .cornerRadius(16.dp)
    ) {
        if (note.type == Type.NOTE) {
            NoteContentView(
                note = note,
                controlsColor = controlsColor,
                openIntent = openIntent,
                selectNoteIntent = selectNoteIntent,
                titleTextSize = titleTextSize,
                bodyTextSize = bodyTextSize,
            )
        } else {
            ListContentView(
                note = note,
                controlsColor = controlsColor,
                openIntent = openIntent,
                selectNoteIntent = selectNoteIntent,
                titleTextSize = titleTextSize,
                bodyTextSize = bodyTextSize,
            )
        }
    }
}

@Composable
internal fun EmptyWidgetView(
    appWidgetId: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val selectNoteIntent = NoteWidget.createSelectNoteIntent(context, appWidgetId)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(Color.DarkGray)
                .padding(12.dp)
                .clickable(actionStartActivity(selectNoteIntent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.select_note),
            style =
                TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

@Composable
internal fun LockedWidgetView(
    note: BaseNote,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val openIntent = NoteWidget.createOpenNoteIntent(context, note)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(Color.DarkGray)
                .padding(12.dp)
                .clickable(actionStartActivity(openIntent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.lock_big),
            contentDescription = null,
            modifier = GlanceModifier.size(48.dp),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.locked),
            style =
                TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
internal fun NoteContentView(
    note: BaseNote,
    controlsColor: Color,
    openIntent: Intent,
    selectNoteIntent: Intent,
    titleTextSize: TextUnit,
    bodyTextSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize().clickable(actionStartActivity(openIntent))) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = note.title,
                style =
                    TextStyle(
                        color = ColorProvider(day = controlsColor, night = controlsColor),
                        fontSize = titleTextSize,
                        fontWeight = FontWeight.Bold,
                    ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Image(
                provider = ImageProvider(R.drawable.notebook),
                contentDescription = context.getString(R.string.select_note),
                modifier =
                    GlanceModifier.size(24.dp).clickable(actionStartActivity(selectNoteIntent)),
            )
        }
        if (note.body.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = note.body,
                style =
                    TextStyle(
                        color = ColorProvider(day = controlsColor, night = controlsColor),
                        fontSize = bodyTextSize,
                    ),
            )
        }
    }
}

@Composable
internal fun ListContentView(
    note: BaseNote,
    controlsColor: Color,
    openIntent: Intent,
    selectNoteIntent: Intent,
    titleTextSize: TextUnit,
    bodyTextSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                GlanceModifier.fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = note.title,
                style =
                    TextStyle(
                        color = ColorProvider(day = controlsColor, night = controlsColor),
                        fontSize = titleTextSize,
                        fontWeight = FontWeight.Bold,
                    ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Image(
                provider = ImageProvider(R.drawable.notebook),
                contentDescription = context.getString(R.string.select_note),
                modifier =
                    GlanceModifier.size(24.dp).clickable(actionStartActivity(selectNoteIntent)),
            )
        }
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(
                items = note.items,
                itemId = { index, item ->
                    if (item.id != -1) item.id.toLong() else (note.id * 31L + index)
                },
            ) { index, item ->
                ListItemRow(
                    noteId = note.id,
                    index = index,
                    item = item,
                    controlsColor = controlsColor,
                    bodyTextSize = bodyTextSize,
                )
            }
        }
    }
}

@Composable
internal fun ListItemRow(
    noteId: Long,
    index: Int,
    item: ListItem,
    controlsColor: Color,
    bodyTextSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val startPadding = if (item.isChild) 24.dp else 0.dp
    Row(
        modifier = modifier.fillMaxWidth().padding(start = startPadding, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(
            checked = item.checked,
            onCheckedChange =
                actionRunCallback<ToggleChecklistAction>(
                    actionParametersOf(
                        ToggleChecklistAction.NoteIdKey to noteId,
                        ToggleChecklistAction.PositionKey to index,
                    )
                ),
            modifier = GlanceModifier.padding(end = 8.dp),
        )
        Text(
            text = item.body,
            style =
                TextStyle(
                    color = ColorProvider(day = controlsColor, night = controlsColor),
                    fontSize = bodyTextSize,
                    textDecoration =
                        if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                ),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun LockedWidgetViewPreview() {
    LockedWidgetView(note = SampleNotes.LOREM_IPSUM_NOTE)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun ListItemRowPreview() {
    ListItemRow(
        noteId = 1L,
        index = 0,
        item =
            ListItem(
                body = "Sample Item",
                checked = false,
                isChild = false,
                order = 0,
                children = mutableListOf(),
                id = 1,
            ),
        controlsColor = Color.Black,
        bodyTextSize = 14.sp,
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun NoteWidgetContentPreview() {
    NoteWidgetContent(
        appWidgetId = 1,
        note = SampleNotes.LOREM_IPSUM_NOTE,
        locked = false,
        theme = Theme.FOLLOW_SYSTEM,
        titleTextSize = 18.sp,
        bodyTextSize = 14.sp,
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun NoteListWidgetContentPreview() {
    NoteWidgetContent(
        appWidgetId = 1,
        note = SampleNotes.GENERATED_LIST_NOTE,
        locked = false,
        theme = Theme.FOLLOW_SYSTEM,
        titleTextSize = 18.sp,
        bodyTextSize = 14.sp,
    )
}
