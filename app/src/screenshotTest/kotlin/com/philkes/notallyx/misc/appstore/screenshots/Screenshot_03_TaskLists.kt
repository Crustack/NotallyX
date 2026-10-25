package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.misc.appstore.AppStorePreview
import com.philkes.notallyx.misc.appstore.AppStoreSampleData
import com.philkes.notallyx.misc.appstore.DeviceFrame

/** 3. Keep track of your task lists */
@AppStorePreview
@PreviewTest
@Composable
fun `03_TaskLists`() {
    val noteColor =
        androidx.compose.ui.graphics.Color(
            LocalContext.current.extractColor(Color.FOG.toColorString())
        )
    val contentColor = contentColorFor(noteColor)

    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_task_lists),
            statusBarColor = noteColor,
            statusBarContentColor = contentColor,
            screenContent = {
                EditListScreen(
                    noteState =
                        EditNoteState(
                            title = AppStoreSampleData.GROCERY_LIST_NOTE.title,
                            items = AppStoreSampleData.GROCERY_LIST_NOTE.items,
                            labels = AppStoreSampleData.GROCERY_LIST_NOTE.labels,
                            createdTimestamp = AppStoreSampleData.GROCERY_LIST_NOTE.timestamp,
                            modifiedTimestamp =
                                AppStoreSampleData.GROCERY_LIST_NOTE.modifiedTimestamp,
                            type = Type.LIST,
                        ),
                    preferences =
                        EditNotePreferences(
                            notesSortBy = NotesSortBy.MODIFIED_DATE,
                            dateFormat = DateFormat.DD_MM_YY_GER,
                            timeFormat = TimeFormat.TWENTY_FOUR_H,
                            topActions =
                                listOf(
                                    EditAction.SEARCH,
                                    EditAction.REMINDERS,
                                    EditAction.PIN,
                                ),
                            bottomAction = EditAction.TOGGLE_VIEW_MODE,
                        ),
                    backgroundColor = noteColor,
                    canUndo = true,
                )
            },
        )
    }
}
