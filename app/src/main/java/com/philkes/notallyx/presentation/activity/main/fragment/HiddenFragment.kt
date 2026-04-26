package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.utils.security.showBiometricOrPinPromptHidden

open class HiddenFragment : NotallyFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.folder.value = Folder.HIDDEN
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun getBackground() = R.drawable.label_off

    override fun getObservable(): LiveData<List<Item>> {
        return model.hiddenNotes!!
    }

    override fun onStart() {
        super.onStart()
        hide()
        showBiometricOrPinPromptHidden(
            this,
            R.string.hidden_lock_title,
            onSuccess = { show() },
            onFailure = { findNavController().popBackStack() },
        )
    }

    protected fun show() {
        binding?.root?.visibility = VISIBLE
    }

    protected fun hide() {
        binding?.root?.visibility = INVISIBLE
    }
}
