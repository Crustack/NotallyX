package com.philkes.notallyx.di

import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckModulesTest : KoinTest {

    @Test
    fun verifyKoinModules() {
        koinApplication { modules(appModules) }
    }
}
