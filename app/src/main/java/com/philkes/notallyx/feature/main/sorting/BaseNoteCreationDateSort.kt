package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote

fun BaseNote.compareCreated(other: BaseNote) = timestamp.compareTo(other.timestamp)
