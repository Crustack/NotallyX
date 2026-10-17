import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.philkes.notallyx.core.ui.components.MenuItemRow
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

class MenuDialog(context: Context) : BottomSheetDialog(context) {

    private val linearLayout: LinearLayout

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        linearLayout = LinearLayout(context)
        linearLayout.layoutParams = params
        linearLayout.orientation = LinearLayout.VERTICAL

        val scrollView = NestedScrollView(context)
        scrollView.layoutParams = params

        scrollView.addView(linearLayout)
        setContentView(scrollView)
    }

    fun add(title: String, drawable: Int = 0, onClick: () -> Unit): MenuDialog {
        val composeView =
            ComposeView(context).apply {
                setContent {
                    NotallyXTheme {
                        MenuItemRow(
                            title = title,
                            iconResId = if (drawable != 0) drawable else null,
                            onClick = {
                                dismiss()
                                onClick()
                            },
                        )
                    }
                }
            }
        linearLayout.addView(composeView)
        return this
    }

    fun add(title: Int, drawable: Int = 0, onClick: () -> Unit) =
        add(context.getString(title), drawable, onClick)
}
