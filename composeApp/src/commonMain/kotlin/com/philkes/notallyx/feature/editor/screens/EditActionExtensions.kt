package com.philkes.notallyx.feature.editor.screens

import com.philkes.notallyx.Res
import com.philkes.notallyx.archive
import com.philkes.notallyx.change_color
import com.philkes.notallyx.check_all_items
import com.philkes.notallyx.checkbox_checked
import com.philkes.notallyx.checkbox_unchecked
import com.philkes.notallyx.content_copy
import com.philkes.notallyx.convert_to_list_note
import com.philkes.notallyx.convert_to_text
import com.philkes.notallyx.convert_to_text_note
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.delete
import com.philkes.notallyx.delete_all
import com.philkes.notallyx.delete_checked_items
import com.philkes.notallyx.delete_forever
import com.philkes.notallyx.duplicate
import com.philkes.notallyx.edit
import com.philkes.notallyx.export
import com.philkes.notallyx.label
import com.philkes.notallyx.labels
import com.philkes.notallyx.make_list_foreground
import com.philkes.notallyx.notifications
import com.philkes.notallyx.pin
import com.philkes.notallyx.pin_to_status_bar
import com.philkes.notallyx.pinboard
import com.philkes.notallyx.pinboard_filled
import com.philkes.notallyx.reminders
import com.philkes.notallyx.restore
import com.philkes.notallyx.search
import com.philkes.notallyx.share
import com.philkes.notallyx.unarchive
import com.philkes.notallyx.uncheck_all_items
import com.philkes.notallyx.unpin
import com.philkes.notallyx.unpin_from_status_bar
import com.philkes.notallyx.view
import com.philkes.notallyx.visibility
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

fun EditAction.getTitleAndIcon(
    pinned: Boolean = false,
    viewMode: NoteViewMode? = null,
    folder: Folder? = null,
    type: Type? = null,
    isPinnedToStatus: Boolean = false,
): Pair<StringResource, DrawableResource> {
    val icon =
        when (this) {
            EditAction.PIN -> if (pinned) Res.drawable.unpin else Res.drawable.pin
            EditAction.PIN_TO_STATUS ->
                if (isPinnedToStatus) Res.drawable.pinboard_filled else Res.drawable.pinboard
            EditAction.ARCHIVE ->
                if (folder == Folder.ARCHIVED) Res.drawable.unarchive else Res.drawable.archive
            EditAction.RESTORE ->
                if (folder == Folder.ARCHIVED) Res.drawable.unarchive else Res.drawable.restore
            EditAction.TOGGLE_VIEW_MODE ->
                if (viewMode == NoteViewMode.READ_ONLY) Res.drawable.edit
                else Res.drawable.visibility
            EditAction.SEARCH -> Res.drawable.search
            EditAction.REMINDERS -> Res.drawable.notifications
            EditAction.LABELS -> Res.drawable.label
            EditAction.CHANGE_COLOR -> Res.drawable.change_color
            EditAction.DUPLICATE -> Res.drawable.content_copy
            EditAction.EXPORT -> Res.drawable.export
            EditAction.SHARE -> Res.drawable.share
            EditAction.DELETE -> Res.drawable.delete
            EditAction.CONVERT ->
                if (type == Type.LIST) Res.drawable.convert_to_text
                else Res.drawable.make_list_foreground
            EditAction.DELETE_FOREVER -> Res.drawable.delete_all
        }
    val title =
        when (this) {
            EditAction.PIN -> if (pinned) Res.string.unpin else Res.string.pin
            EditAction.PIN_TO_STATUS ->
                if (isPinnedToStatus) Res.string.unpin_from_status_bar
                else Res.string.pin_to_status_bar
            EditAction.ARCHIVE ->
                if (folder == Folder.ARCHIVED) Res.string.unarchive else Res.string.archive
            EditAction.RESTORE ->
                if (folder == Folder.ARCHIVED) Res.string.unarchive else Res.string.restore
            EditAction.TOGGLE_VIEW_MODE ->
                if (viewMode == NoteViewMode.READ_ONLY) Res.string.edit else Res.string.view
            EditAction.SEARCH -> Res.string.search
            EditAction.REMINDERS -> Res.string.reminders
            EditAction.LABELS -> Res.string.labels
            EditAction.CHANGE_COLOR -> Res.string.change_color
            EditAction.DUPLICATE -> Res.string.duplicate
            EditAction.EXPORT -> Res.string.export
            EditAction.SHARE -> Res.string.share
            EditAction.DELETE -> Res.string.delete
            EditAction.CONVERT ->
                if (type == Type.LIST) Res.string.convert_to_text_note
                else Res.string.convert_to_list_note
            EditAction.DELETE_FOREVER -> Res.string.delete_forever
        }
    return Pair(title, icon)
}

fun EditListAction.getTitleAndIcon(): Pair<StringResource, DrawableResource> {
    return when (this) {
        EditListAction.DELETE_CHECKED ->
            Pair(Res.string.delete_checked_items, Res.drawable.checkbox_unchecked)
        EditListAction.CHECK_ALL -> Pair(Res.string.check_all_items, Res.drawable.checkbox_checked)
        EditListAction.UNCHECK_ALL ->
            Pair(Res.string.uncheck_all_items, Res.drawable.checkbox_unchecked)
    }
}
