package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote

fun BaseNote.compareColor(other: BaseNote) = color.compareTo(other.color)
