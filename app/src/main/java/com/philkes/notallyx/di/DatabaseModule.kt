package com.philkes.notallyx.di

import android.content.ContextWrapper
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.extensions.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { NotallyXPreferences.getInstance(androidContext() as ContextWrapper) }
    single { NotallyDatabase.getDatabase(androidContext() as ContextWrapper) }
}
