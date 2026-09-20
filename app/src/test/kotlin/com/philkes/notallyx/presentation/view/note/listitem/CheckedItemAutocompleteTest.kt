package com.philkes.notallyx.presentation.view.note.listitem

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CheckedItemAutocompleteTest {

    @Test
    fun `matches candidate beginning case insensitively`() {
        assertThat(matchesCheckedItem("lat", "Latte intero")).isTrue()
        assertThat(matchesCheckedItem("LATTE I", "latte intero")).isTrue()
    }

    @Test
    fun `does not match letters found after candidate beginning`() {
        assertThat(matchesCheckedItem("latte", "Comprare latte")).isFalse()
    }

    @Test
    fun `does not suggest blank text`() {
        assertThat(matchesCheckedItem("  ", "Latte intero")).isFalse()
    }

    @Test
    fun `matches text equal to candidate case insensitively`() {
        assertThat(matchesCheckedItem("Latte", "Latte")).isTrue()
        assertThat(matchesCheckedItem("LATTE", "latte")).isTrue()
    }

    @Test
    fun `requires at least two typed letters`() {
        assertThat(matchesCheckedItem("l", "Latte intero")).isFalse()
        assertThat(matchesCheckedItem("la", "Latte intero")).isTrue()
    }
}
