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
import com.philkes.notallyx.core.ui.extensions.toAnnotatedString
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.misc.appstore.DeviceFrame
import com.philkes.notallyx.misc.appstore.MarketingPreview
import com.philkes.notallyx.misc.appstore.MarketingSampleData

/** 3. Format your rich text notes */
@MarketingPreview
@PreviewTest
@Composable
fun `03_RichText`() {
    val noteColor =
        androidx.compose.ui.graphics.Color(
            LocalContext.current.extractColor(Color.CLAY.toColorString())
        )
    val contentColor = contentColorFor(noteColor)
    val annotated =
        MarketingSampleData.FORMATTING_BODY.toAnnotatedString(MarketingSampleData.FORMATTING_SPANS)

    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_rich_text),
            statusBarColor = noteColor,
            statusBarContentColor = contentColor,
            screenContent = {
                EditNoteScreen(
                    noteState =
                        EditNoteState(
                            title = MarketingSampleData.FORMATTING_NOTE.title,
                            bodyValue = TextFieldValue(annotated),
                            createdTimestamp = MarketingSampleData.FORMATTING_NOTE.timestamp,
                            modifiedTimestamp =
                                MarketingSampleData.FORMATTING_NOTE.modifiedTimestamp,
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
                    isFormattingMenuVisible = true,
                    formattingState = TextFormattingState(isBold = true),
                )
            },
        )
    }
}
