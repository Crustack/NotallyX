package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote

fun BaseNote.compareModified(other: BaseNote) = modifiedTimestamp.compareTo(other.modifiedTimestamp)
