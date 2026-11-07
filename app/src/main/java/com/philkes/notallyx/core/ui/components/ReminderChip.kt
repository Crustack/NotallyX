package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.preference.displaySmallerSize
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.findNextNotificationDate
import com.philkes.notallyx.data.model.extensions.haveAnyRepetition
import java.util.Date

@Composable
internal fun ReminderChip(
    reminders: List<Reminder>,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    textSize: Float = 16f,
    onClick: (() -> Unit)? = null,
) {
    if (reminders.isEmpty()) return

    val now = remember { Date() }
    val mostRecentNotificationDate =
        remember(reminders) {
            reminders.findNextNotificationDate() ?: reminders.maxOfOrNull { it.dateTime }
        } ?: return

    val formattedDate =
        remember(mostRecentNotificationDate, dateFormat, timeFormat) {
            mostRecentNotificationDate.format(dateFormat, timeFormat, ensureFullFormat = true)
        }
    val hasRepetition = remember(reminders) { reminders.haveAnyRepetition() }
    val isElapsed = mostRecentNotificationDate < now

    val remindersLabel = stringResource(id = R.string.reminders)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = CircleShape,
            color =
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (isElapsed) 0.5f else 1.0f
                ),
            modifier = modifier.semantics { contentDescription = remindersLabel },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.notifications),
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isElapsed) 0.5f else 1.0f
                        ),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDate,
                    maxLines = 1,
                    fontSize = textSize.displaySmallerSize.sp,
                    style = MaterialTheme.typography.labelSmall,
                    textDecoration =
                        if (isElapsed) TextDecoration.LineThrough else TextDecoration.None,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isElapsed) 0.5f else 1.0f
                        ),
                )
                if (hasRepetition) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.repetition),
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isElapsed) 0.5f else 1.0f
                            ),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReminderChip(
    baseNote: BaseNote,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    textSize: Float = 16f,
    onClick: (() -> Unit)? = null,
) {
    ReminderChip(
        reminders = baseNote.reminders,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        modifier = modifier,
        textSize = textSize,
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun ReminderChipPreview() {
    NotallyXTheme {
        ReminderChip(
            reminders =
                listOf(
                    Reminder(
                        id = 1L,
                        dateTime = Date(System.currentTimeMillis() + 86400000L),
                        repetition = Repetition(1, RepetitionTimeUnit.DAYS),
                    )
                ),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}
