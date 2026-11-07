package com.philkes.notallyx.migration

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionScreen
import com.philkes.notallyx.feature.migration.backup.ErrorDialogData

@Preview(showBackground = true)
@PreviewTest
@Composable
fun DatabaseTransitionScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        DatabaseTransitionScreen(statusText = stringResource(R.string.moving_database_to_internal))
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun DatabaseTransitionScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        DatabaseTransitionScreen(statusText = stringResource(R.string.moving_database_to_external))
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun DatabaseTransitionScreen_EncryptingBiometric() {
    NotallyXTheme(theme = Theme.LIGHT) {
        DatabaseTransitionScreen(
            statusText = stringResource(R.string.encrypting_database_biometric)
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun DatabaseTransitionScreen_WithError() {
    NotallyXTheme(theme = Theme.LIGHT) {
        DatabaseTransitionScreen(
            statusText = stringResource(R.string.encrypting_database_biometric),
            errorData =
                ErrorDialogData(
                    title = "Database Migration Failed",
                    message = "Failed to encrypt database",
                    details = "java.lang.RuntimeException: Device keystore is unavailable",
                ),
        )
    }
}
