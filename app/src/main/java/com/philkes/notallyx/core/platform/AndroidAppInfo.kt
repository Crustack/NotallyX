package com.philkes.notallyx.core.platform

import android.os.Build
import com.philkes.notallyx.app.BuildConfig

class AndroidAppInfo : AppInfo {
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val versionName: String = BuildConfig.VERSION_NAME
    override val model: String = Build.MODEL
    override val device: String = Build.DEVICE
    override val brand: String = Build.BRAND
    override val manufacturer: String = Build.MANUFACTURER
    override val osVersion: String = Build.VERSION.SDK_INT.toString()
}
