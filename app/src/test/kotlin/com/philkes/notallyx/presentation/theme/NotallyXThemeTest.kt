package com.philkes.notallyx.presentation.theme

import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NotallyXThemeTest {

    @Test
    fun testColorDefinitionsMatchXmlValues() {
        assertThat(MdThemePrimary).isEqualTo(Color(0xFF7285D0))
        assertThat(ContainerLight).isEqualTo(androidx.compose.ui.graphics.Color(0xFFFAF8FF))
        assertThat(ContainerDark).isEqualTo(androidx.compose.ui.graphics.Color(0xFF1A1B21))
        assertThat(ContainerSuperDark).isEqualTo(androidx.compose.ui.graphics.Color(0xFF000000))
    }

    @Test
    fun testThemeEnumValuesExist() {
        assertThat(Theme.LIGHT.name).isEqualTo("LIGHT")
        assertThat(Theme.DARK.name).isEqualTo("DARK")
        assertThat(Theme.SUPER_DARK.name).isEqualTo("SUPER_DARK")
        assertThat(Theme.FOLLOW_SYSTEM.name).isEqualTo("FOLLOW_SYSTEM")
    }
}
