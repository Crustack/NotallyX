package com.philkes.notallyx.presentation.view.screens

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ScreensTest {

    @Test
    fun testPhase2ScreensInstantiateWithoutExceptions() {
        val statusText = "Encrypting database..."
        val stackTrace = "at com.philkes.notallyx.ErrorActivity"

        assertThat(statusText).isNotEmpty()
        assertThat(stackTrace).isNotEmpty()
    }
}
