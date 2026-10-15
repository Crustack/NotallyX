package com.philkes.notallyx.core.ui.components

interface ItemListener {

    fun onClick(position: Int)

    fun onLongClick(position: Int)

    fun onReminderClick(position: Int) {}
}
