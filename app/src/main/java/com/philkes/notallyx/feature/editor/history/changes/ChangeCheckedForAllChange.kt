package com.philkes.notallyx.feature.editor.history.changes

import com.philkes.notallyx.feature.editor.listitem.ListManager
import com.philkes.notallyx.feature.editor.listitem.ListState

class ChangeCheckedForAllChange(old: ListState, new: ListState, listManager: ListManager) :
    ListBatchChange(old, new, listManager)
