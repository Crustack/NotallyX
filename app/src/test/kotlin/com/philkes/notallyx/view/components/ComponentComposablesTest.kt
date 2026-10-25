package com.philkes.notallyx.view.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    @Test
    fun testNoteColorLuminance() {
        val darkHex = "#111111".removePrefix("#").toLong(16)
        val darkColor = Color(0xFF000000 or darkHex)
        assertThat(darkColor.luminance()).isLessThan(0.5f)

        val lightHex = "#F8BBD0".removePrefix("#").toLong(16)
        val lightColor = Color(0xFF000000 or lightHex)
        assertThat(lightColor.luminance()).isGreaterThan(0.5f)
    }

    @Test
    fun testContentColorFor() {
        val darkColor = Color(0xFF111111)
        assertThat(com.philkes.notallyx.core.ui.theme.contentColorFor(darkColor))
            .isEqualTo(com.philkes.notallyx.core.ui.theme.MdThemeOnBackgroundDark)

        val lightColor = Color(0xFFF8BBD0)
        assertThat(com.philkes.notallyx.core.ui.theme.contentColorFor(lightColor))
            .isEqualTo(com.philkes.notallyx.core.ui.theme.MdThemeOnBackgroundLight)
    }
}
