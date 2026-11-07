package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
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
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.misc.appstore.DeviceFrame
import com.philkes.notallyx.misc.appstore.MarketingPreview
import com.philkes.notallyx.misc.appstore.MarketingSampleData

/** 4. Enhance your notes with images and files */
@MarketingPreview
@PreviewTest
@Composable
fun `04_ImagesAndFiles`() {
    val noteColor =
        androidx.compose.ui.graphics.Color(
            LocalContext.current.extractColor(Color.DUSK.toColorString())
        )
    val contentColor = contentColorFor(noteColor)

    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_images_and_files),
            statusBarColor = noteColor,
            statusBarContentColor = contentColor,
            screenContent = {
                EditNoteScreen(
                    noteState =
                        EditNoteState(
                            title = MarketingSampleData.VACATION_NOTE.title,
                            bodyValue = TextFieldValue(MarketingSampleData.VACATION_NOTE.body),
                            createdTimestamp = MarketingSampleData.VACATION_NOTE.timestamp,
                            modifiedTimestamp = MarketingSampleData.VACATION_NOTE.modifiedTimestamp,
                            images = MarketingSampleData.VACATION_NOTE.images,
                            files = MarketingSampleData.VACATION_NOTE.files,
                            reminders = MarketingSampleData.VACATION_NOTE.reminders,
                            type = Type.NOTE,
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
