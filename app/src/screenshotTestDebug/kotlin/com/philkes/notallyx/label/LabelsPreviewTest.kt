package com.philkes.notallyx.label

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.labels.LabelData
import com.philkes.notallyx.feature.labels.LabelsScreen
import com.philkes.notallyx.feature.main.MainDrawerContent
import com.philkes.notallyx.navigation.NotallyNavKey

private val SAMPLE_LABELS =
    listOf(
        LabelData("Personal", visibleInNavigation = true, order = 0),
        LabelData("Work", visibleInNavigation = false, order = 1),
        LabelData(
            "Very Long Label Name That Might Wrap Or Ellipsize In Labels Manager",
            visibleInNavigation = true,
            order = 2,
        ),
        LabelData("Hidden Projects", visibleInNavigation = false, order = 3),
    )

private val DRAWER_LABELS =
    listOf(
        Label("Personal", 0),
        Label("Very Long Label Name That Might Wrap Or Ellipsize In Drawer", 1),
        Label("Projects", 2),
    )

// ==================== Label Dialog Previews ====================

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditLabelDialog_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        TextInputDialog(
            title = stringResource(R.string.edit_label),
            initialValue = "Personal",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditLabelDialog_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        TextInputDialog(
            title = stringResource(R.string.edit_label),
            initialValue = "Personal",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun NewLabelDialog_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        TextInputDialog(
            title = stringResource(R.string.add_label),
            initialValue = "",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun NewLabelDialog_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        TextInputDialog(
            title = stringResource(R.string.add_label),
            initialValue = "",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun DeleteLabelDialog_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ConfirmDialog(
            title = stringResource(R.string.delete_label),
            message = stringResource(R.string.your_notes_associated),
            positiveButtonText = stringResource(R.string.delete),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun DeleteLabelDialog_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ConfirmDialog(
            title = stringResource(R.string.delete_label),
            message = stringResource(R.string.your_notes_associated),
            positiveButtonText = stringResource(R.string.delete),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

// ==================== LabelsScreen Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun LabelsScreen_WithHiddenAndLongLabels_Light() {
    NotallyXTheme(theme = Theme.LIGHT) { LabelsScreen(labels = SAMPLE_LABELS) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun LabelsScreen_WithHiddenAndLongLabels_Dark() {
    NotallyXTheme(theme = Theme.DARK) { LabelsScreen(labels = SAMPLE_LABELS) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun LabelsScreen_WithHiddenAndLongLabels_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) { LabelsScreen(labels = SAMPLE_LABELS) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun LabelsScreen_Empty_Light() {
    NotallyXTheme(theme = Theme.LIGHT) { LabelsScreen(labels = emptyList()) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun LabelsScreen_Empty_Dark() {
    NotallyXTheme(theme = Theme.DARK) { LabelsScreen(labels = emptyList()) }
}

// ==================== Navigation Drawer Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun MainDrawerContent_WithLongLabels_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        MainDrawerContent(
            selectedNavKey = NotallyNavKey.Notes,
            title = "NotallyX",
            labels = DRAWER_LABELS,
            onNavigationItemClick = {},
            onCloseDrawer = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun MainDrawerContent_WithLongLabels_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        MainDrawerContent(
            selectedNavKey = NotallyNavKey.Notes,
            title = "NotallyX",
            labels = DRAWER_LABELS,
            onNavigationItemClick = {},
            onCloseDrawer = {},
        )
    }
}
