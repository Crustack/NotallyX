package com.philkes.notallyx.test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], qualifiers = "w1024dp-h768dp")
actual abstract class BaseComposeTest actual constructor()
