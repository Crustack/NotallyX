package com.philkes.notallyx.feature.editor.history.changes

import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.editor.history.Change

abstract class ValueChange<T>(internal val oldValue: T, internal val newValue: T) : Change {

    override fun redo() {
        update(newValue, false)
    }

    override fun undo() {
        update(oldValue, true)
    }

    abstract fun update(value: T, isUndo: Boolean)

    override fun toString(): String {
        return "${javaClass.simpleName} from: ${oldValue.toString().truncate(100)} to: ${newValue.toString().truncate(100)}"
    }
}
