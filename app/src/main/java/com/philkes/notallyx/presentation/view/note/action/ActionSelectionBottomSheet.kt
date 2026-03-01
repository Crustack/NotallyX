package com.philkes.notallyx.presentation.view.note.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.widget.TextViewCompat
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.dp
import com.philkes.notallyx.presentation.getColorFromAttr
import com.philkes.notallyx.presentation.getString
import com.philkes.notallyx.presentation.viewmodel.NotallyModel
import com.philkes.notallyx.presentation.viewmodel.preference.EditAction

class ActionSelectionBottomSheet(
    actions: List<EditAction>,
    model: NotallyModel,
    private val oldAction: EditAction,
    private val title: String? = null,
    private val onReset: (() -> Unit)? = null,
    @ColorInt color: Int?,
    onActionSelected: (EditAction) -> Unit,
) :
    ActionBottomSheet(
        actions.map { action ->
            val (title, icon) =
                action.getTitleAndIcon(model.pinned, model.viewMode.value, model.folder, model.type)
            Action(labelResId = title, drawableResId = icon, isSelected = action == oldAction) {
                onActionSelected(action)
                true
            }
        },
        color,
    ) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        title?.let {
            val titleContainer =
                RelativeLayout(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                            .apply { setMargins(16.dp, 8.dp, 16.dp, 16.dp) }
                }

            var resetIconId: Int? = null
            onReset?.let { resetCallback ->
                val resetIcon =
                    ImageView(context).apply {
                        id = View.generateViewId()
                        resetIconId = id
                        setImageResource(R.drawable.undo)
                        contentDescription = getString(R.string.reset_settings)
                        layoutParams =
                            RelativeLayout.LayoutParams(24.dp, 24.dp).apply {
                                addRule(RelativeLayout.ALIGN_PARENT_END)
                                addRule(RelativeLayout.CENTER_VERTICAL)
                            }
                        context?.let { ctx ->
                            imageTintList =
                                android.content.res.ColorStateList.valueOf(
                                    ctx.getColorFromAttr(
                                        com.google.android.material.R.attr.colorPrimary
                                    )
                                )
                        }
                        setOnClickListener {
                            resetCallback()
                            dismiss()
                        }
                    }
                titleContainer.addView(resetIcon)
            }

            val titleView =
                TextView(context).apply {
                    text = it
                    layoutParams =
                        RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                            )
                            .apply {
                                addRule(RelativeLayout.ALIGN_PARENT_START)
                                addRule(RelativeLayout.CENTER_VERTICAL)
                                resetIconId?.let { iconId ->
                                    addRule(RelativeLayout.START_OF, iconId)
                                }
                            }
                    TextViewCompat.setTextAppearance(
                        this,
                        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
                    )
                    context?.let { ctx ->
                        setTextColor(
                            ctx.getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
                        )
                    }
                }
            titleContainer.addView(titleView)

            layout.addView(titleContainer, 0)
        }
        return view
    }

    companion object {
        const val TAG = "ActionSelectionBottomSheet"
    }
}
