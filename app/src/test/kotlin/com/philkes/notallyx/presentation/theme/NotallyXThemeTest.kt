package com.philkes.notallyx.theme

import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.theme.ContainerDark
import com.philkes.notallyx.core.ui.theme.ContainerLight
import com.philkes.notallyx.core.ui.theme.ContainerSuperDark
import com.philkes.notallyx.core.ui.theme.MdThemePrimary
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.extensions.*
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
