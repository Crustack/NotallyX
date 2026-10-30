package com.philkes.notallyx.di

import androidx.datastore.core.DataStore
import com.google.crypto.tink.Aead
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.model.SecurePreferences
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.core.security.TinkAeadProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val USER_DATASTORE_QUALIFIER = named("userPreferencesDataStore")
val SECURE_DATASTORE_QUALIFIER = named("securePreferencesDataStore")

val preferencesModule = module {
    single<Aead> { TinkAeadProvider.provideAead(androidContext()) }

    factory { NotallyXPreferences.getInstance(androidContext()) }

    factory<DataStore<UserPreferences>>(USER_DATASTORE_QUALIFIER) {
        get<NotallyXPreferences>().userDataStore
    }

    factory<DataStore<SecurePreferences>>(SECURE_DATASTORE_QUALIFIER) {
        get<NotallyXPreferences>().secureDataStore
    }
}
