package com.philkes.notallyx.di

import android.content.ContextWrapper
import com.philkes.notallyx.core.database.AndroidDatabaseDriverFactory
import com.philkes.notallyx.core.database.DatabaseDriverFactory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<PathProvider> { AndroidPathProvider(androidContext()) }
    single<DatabaseDriverFactory> {
        AndroidDatabaseDriverFactory(androidContext() as ContextWrapper, get())
    }
    single { NotallyDatabase.getDatabase(androidContext() as ContextWrapper) }
}
