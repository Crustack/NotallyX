package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote

fun BaseNote.compareTitle(other: BaseNote) = title.compareTo(other.title)
