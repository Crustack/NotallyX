package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainScreen
import com.philkes.notallyx.feature.settings.SettingsScreen
import com.philkes.notallyx.feature.settings.SettingsUiState
import com.philkes.notallyx.misc.appstore.BiometricPromptBottomSheet
import com.philkes.notallyx.misc.appstore.DeviceFrame
import com.philkes.notallyx.misc.appstore.MarketingPreview
import com.philkes.notallyx.navigation.NotallyNavKey

/** 8. Lock your notes via Fingerprint */
@MarketingPreview
@PreviewTest
@Composable
fun `08_FingerprintLock`() {
    NotallyXTheme {
        val backgroundColor = Color.Black.copy(alpha = 0.4f)
        DeviceFrame(
            title = stringResource(R.string.store_headline_fingerprint_lock),
            statusBarColor = backgroundColor,
            screenContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        title = stringResource(R.string.settings),
                        selectedNavKey = NotallyNavKey.Settings,
                        showFabs = false,
                        showSearchOption = false,
                        content = {
                            SettingsScreen(
                                state =
                                    SettingsUiState(
                                        biometricLock = BiometricLock.DISABLED,
                                        biometricLockEncryptsDb = false,
                                    ),
                                scrollState = rememberScrollState(initial = 7600),
                            )
                        },
                    )
                    // 2. Semi-transparent background scrim for the bottom sheet
                    Box(modifier = Modifier.fillMaxSize().background(backgroundColor))

                    // 3. Pinned Biometric Bottom Sheet at the bottom of the screen
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                        BiometricPromptBottomSheet(
                            title = stringResource(R.string.enable_lock_title)
                        )
                    }
                }
            },
        )
    }
}
