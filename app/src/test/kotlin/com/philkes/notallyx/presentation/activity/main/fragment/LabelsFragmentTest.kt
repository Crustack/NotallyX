package com.philkes.notallyx.presentation.activity.main.fragment

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.philkes.notallyx.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LabelsFragmentTest {

    @Test
    fun `confirmDeletion shows selected label name in dialog message`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        val activity = controller.get()
        activity.setTheme(R.style.AppTheme)
        controller.setup()
        val fragment = LabelsFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()

        LabelsFragment::class.java
            .getDeclaredMethod("confirmDeletion", String::class.java)
            .apply { isAccessible = true }
            .invoke(fragment, "Shopping")

        val dialog = ShadowDialog.getLatestDialog()
        val message = dialog.findViewById<TextView>(android.R.id.message)?.text?.toString()

        assertThat(message).isEqualTo("Shopping\n\n${activity.getString(R.string.your_notes_associated)}")
    }
}
