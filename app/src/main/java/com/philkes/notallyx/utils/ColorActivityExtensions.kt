package com.philkes.notallyx.utils

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.databinding.DialogColorBinding
import com.philkes.notallyx.databinding.DialogColorPickerBinding
import com.philkes.notallyx.presentation.createTextView
import com.philkes.notallyx.presentation.dp
import com.philkes.notallyx.presentation.extractColor
import com.philkes.notallyx.presentation.setLightStatusAndNavBar
import com.philkes.notallyx.presentation.showAndFocus
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.main.ColorAdapter
import com.philkes.notallyx.presentation.view.misc.ItemListener
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

fun AppCompatActivity.showColorSelectDialog(
    colors: Set<ColorString>,
    currentColor: ColorString?,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: ColorString, oldColor: ColorString?) -> Unit,
    deleteCallback: (colorToDelete: ColorString, newColor: ColorString) -> Unit,
) {
    val preferences = NotallyXPreferences.getInstance(this)
    val actualColors =
        (colors + preferences.defaultNoteColor.value).toMutableList().apply {
            remove(BaseNote.COLOR_DEFAULT)
            remove(BaseNote.COLOR_NEW)
            add(0, BaseNote.COLOR_NEW)
            add(1, BaseNote.COLOR_DEFAULT)
        }
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.change_color).create()
    lateinit var colorAdapter: ColorAdapter
    colorAdapter =
        ColorAdapter(
            actualColors,
            currentColor,
            preferences.defaultNoteColor.value,
            object : ItemListener {
                override fun onClick(position: Int) {
                    dialog.dismiss()
                    val selectedColor = actualColors[position]
                    if (selectedColor == BaseNote.COLOR_NEW) {
                        showEditColorDialog(
                            actualColors,
                            null,
                            setNavigationbarLight,
                            callback,
                            deleteCallback,
                        )
                    } else callback(selectedColor, null)
                }

                override fun onLongClick(position: Int) {
                    val oldColor = actualColors[position]
                    if (oldColor == BaseNote.COLOR_NEW) {
                        return
                    }
                    if (oldColor == BaseNote.COLOR_DEFAULT) {
                        if (oldColor == preferences.defaultNoteColor.value) {
                            showToast(R.string.default_color_hint)
                            return
                        } else {
                            MaterialAlertDialogBuilder(this@showColorSelectDialog)
                                .setTitle(R.string.set_as_default_color)
                                .setMessage(R.string.set_as_default_color_message)
                                .setPositiveButton(R.string.make_default) { _, _ ->
                                    preferences.defaultNoteColor.save(oldColor)
                                    colorAdapter.defaultColor = oldColor
                                    colorAdapter.notifyItemRangeChanged(1, actualColors.size - 1)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                            return
                        }
                    }
                    dialog.dismiss()
                    showEditColorDialog(
                        actualColors,
                        oldColor,
                        setNavigationbarLight,
                        callback,
                        deleteCallback,
                    )
                }
            },
        )
    DialogColorBinding.inflate(layoutInflater).apply {
        Message.text =
            "${getString(R.string.change_color_message)}\n${getString(R.string.change_color_default_message)}"
        MainListView.adapter = colorAdapter
        dialog.setView(root)
        dialog.setOnShowListener {
            setNavigationbarLight?.let { dialog.window?.setLightStatusAndNavBar(it) }
        }
        dialog.show()
    }
}

private fun AppCompatActivity.showEditColorDialog(
    colors: List<ColorString>,
    oldColor: ColorString?,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: ColorString, oldColor: ColorString?) -> Unit,
    deleteCallback: (colorToDelete: ColorString, newColor: ColorString) -> Unit,
) {
    val selectedColor = oldColor?.let { extractColor(it) } ?: extractColor(BaseNote.COLOR_DEFAULT)
    var editTextChangedByUser = false
    var dialog: androidx.appcompat.app.AlertDialog? = null
    val binding =
        DialogColorPickerBinding.inflate(layoutInflater).apply {
            BrightnessSlideBar.setSelectorDrawableRes(
                com.skydoves.colorpickerview.R.drawable.colorpickerview_wheel
            )
            ColorPicker.apply {
                BrightnessSlideBar.attachColorPickerView(ColorPicker)
                attachBrightnessSlider(BrightnessSlideBar)
                setInitialColor(selectedColor)
                ColorPicker.postDelayed({ ColorPicker.selectByHsvColor(selectedColor) }, 100)
                ColorCode.doAfterTextChanged { text ->
                    val isValueChangedByUser = ColorCode.hasFocus()
                    val hexCode = text.toString()
                    if (isValueChangedByUser && hexCode.length == 6) {
                        try {
                            val color = this@showEditColorDialog.extractColor("#$hexCode")
                            editTextChangedByUser = true
                            ColorPicker.selectByHsvColor(color)
                        } catch (e: Exception) {}
                    }
                }
                CopyCode.setOnClickListener { _ ->
                    this@showEditColorDialog.copyToClipBoard(ColorCode.text)
                }
            }
            Restore.setOnClickListener { ColorPicker.selectByHsvColor(selectedColor) }
            oldColor?.let {
                DeleteColor.apply {
                    isVisible = true
                    setOnClickListener {
                        dialog?.dismiss()
                        showDeleteColorDialog(
                            colors,
                            oldColor,
                            setNavigationbarLight,
                            callback,
                            deleteCallback,
                        )
                    }
                }
            }

            ExistingColors.apply {
                val existingColors = Color.allColorStrings()
                val colorAdapter =
                    ColorAdapter(
                        existingColors,
                        null,
                        null,
                        object : ItemListener {
                            override fun onClick(position: Int) {
                                ColorPicker.selectByHsvColor(
                                    this@showEditColorDialog.extractColor(existingColors[position])
                                )
                            }

                            override fun onLongClick(position: Int) {}
                        },
                    )
                adapter = colorAdapter
            }
        }
    val preferences = NotallyXPreferences.getInstance(this@showEditColorDialog)
    dialog =
        MaterialAlertDialogBuilder(this).run {
            setTitle(if (oldColor != null) R.string.edit_color else R.string.new_color)
            setView(binding.root)
            setPositiveButton(R.string.save) { _, _ ->
                val newColor = binding.ColorPicker.colorEnvelope.toColorString()
                if (newColor == oldColor) {
                    callback(oldColor, null)
                } else {
                    callback(newColor, oldColor)
                }
            }
            setNegativeButton(R.string.back) { _, _ ->
                showColorSelectDialog(
                    colors.toSet(),
                    oldColor,
                    setNavigationbarLight,
                    callback,
                    deleteCallback,
                )
            }
            setNeutralButton(R.string.text_default, null)
            showAndFocus(
                allowFullSize = true,
                onShowListener = {
                    setNavigationbarLight?.let {
                        window?.apply { setLightStatusAndNavBar(it, binding.root) }
                    }
                },
                applyToPositiveButton = { positiveButton ->
                    binding.apply {
                        BrightnessSlideBar.setSelectorDrawableRes(
                            com.skydoves.colorpickerview.R.drawable.colorpickerview_wheel
                        )
                        ColorPicker.setColorListener(
                            ColorEnvelopeListener { color, _ ->
                                TileView.setPaintColor(color.color)
                                val colorString = color.toColorString()
                                val isSaveEnabled =
                                    colorString == oldColor || colorString !in colors
                                positiveButton.isEnabled = isSaveEnabled
                                ColorExistsText.visibility =
                                    if (isSaveEnabled) View.GONE else View.VISIBLE
                                if (!editTextChangedByUser) {
                                    ColorCode.setText(color.hexCode.argbToRgbString())
                                } else editTextChangedByUser = false
                            }
                        )
                    }
                },
            )
        }
    val observer: Observer<ColorString> = Observer { defaultColor ->
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            val currentColor = binding.ColorPicker.colorEnvelope.toColorString()
            val isDefaultColor = currentColor == defaultColor
            setText(if (isDefaultColor) R.string.text_default else R.string.make_default)
            if (!isDefaultColor) {
                TooltipCompat.setTooltipText(this, getString(R.string.set_as_default_color_message))
                setOnClickListener {
                    val newColor = binding.ColorPicker.colorEnvelope.toColorString()
                    preferences.defaultNoteColor.save(newColor)
                }
            } else {
                TooltipCompat.setTooltipText(this, getString(R.string.default_color_hint))
                setOnClickListener { performLongClick() }
            }
            (this as? MaterialButton)?.apply {
                icon =
                    if (isDefaultColor)
                        ContextCompat.getDrawable(
                            this@showEditColorDialog,
                            R.drawable.star_rate_filled,
                        )
                    else ContextCompat.getDrawable(this@showEditColorDialog, R.drawable.star)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 0
            }
        }
    }
    preferences.defaultNoteColor.observe(this@showEditColorDialog, observer)
    dialog.setOnDismissListener { preferences.defaultNoteColor.removeObserver(observer) }
}

private fun AppCompatActivity.showDeleteColorDialog(
    colors: List<String>,
    oldColor: String,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: String, oldColor: String?) -> Unit,
    deleteCallback: (colorToDelete: String, newColor: String) -> Unit,
) {
    val dialog =
        MaterialAlertDialogBuilder(this)
            .setCustomTitle(createTextView(R.string.delete_color_message))
            .setNeutralButton(R.string.back) { _, _ ->
                showEditColorDialog(
                    colors,
                    oldColor,
                    setNavigationbarLight,
                    callback,
                    deleteCallback,
                )
            }
            .create()
    val selectableColors = colors.filter { it != BaseNote.COLOR_NEW && it != oldColor }
    val colorAdapter =
        ColorAdapter(
            selectableColors,
            null,
            null,
            object : ItemListener {
                override fun onClick(position: Int) {
                    dialog.dismiss()
                    val selectedColor = selectableColors[position]
                    deleteCallback(oldColor, selectedColor)
                }

                override fun onLongClick(position: Int) {}
            },
        )
    DialogColorBinding.inflate(layoutInflater).apply {
        MainListView.apply {
            updatePadding(left = 2.dp, right = 2.dp)
            (layoutManager as? GridLayoutManager)?.let { it.spanCount = 6 }
            adapter = colorAdapter
        }
        Message.isVisible = false
        dialog.setView(root)
        dialog.setOnShowListener {
            setNavigationbarLight?.let { window?.apply { setLightStatusAndNavBar(it, root) } }
        }
        dialog.show()
    }
}

private fun ColorEnvelope.toColorString(): ColorString {
    return "#${hexCode.argbToRgbString()}"
}

private fun ColorString.argbToRgbString(): ColorString = substring(2)
