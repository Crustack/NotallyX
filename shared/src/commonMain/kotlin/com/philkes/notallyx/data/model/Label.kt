package com.philkes.notallyx.data.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity class Label(@PrimaryKey val value: String, val order: Int)
