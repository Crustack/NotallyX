package com.philkes.notallyx.di

import android.content.ContextWrapper
import com.philkes.notallyx.core.database.NotallyDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { NotallyDatabase.getDatabase(androidContext() as ContextWrapper) }
}
