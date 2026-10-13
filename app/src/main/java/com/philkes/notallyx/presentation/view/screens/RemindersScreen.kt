package com.philkes.notallyx.presentation.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.toRepetitionText
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.ReminderChipItem
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.TimeFormat

/** Composable replacing activity_reminders.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    modifier: Modifier = Modifier,
    onAddReminderClick: () -> Unit = {},
    onEditReminder: (Reminder) -> Unit = {},
    onDeleteReminder: (Reminder) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.edit_reminders)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("Back"),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddReminderClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = stringResource(R.string.add_reminder),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_reminders),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(reminders, key = { it.id }) { reminder ->
                        val dateTimeText =
                            reminder.dateTime.format(
                                dateFormat,
                                timeFormat,
                                ensureFullFormat = true,
                            )
                        val repetitionText = reminder.toRepetitionText(context)
                        ReminderChipItem(
                            dateTimeText = dateTimeText,
                            repetitionText = repetitionText,
                            onEdit = { onEditReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersScreenPreview() {
    NotallyXTheme {
        RemindersScreen(
            reminders = emptyList(),
            dateFormat = DateFormat.FULL,
            timeFormat = TimeFormat.AM_PM,
        )
    }
}
