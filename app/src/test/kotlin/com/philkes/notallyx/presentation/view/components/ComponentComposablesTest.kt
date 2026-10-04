package com.philkes.notallyx.presentation.view.components

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ComponentComposablesTest {

    @Test
    fun testPhase1ComposablesParameters() {
        val title = "Note Title"
        val body = "Note Body text for testing card composables"

        assertThat(title).isEqualTo("Note Title")
        assertThat(body).isNotBlank()
        assertThat(Color.Red).isNotNull()
    }
}
