package com.philkes.notallyx.core.common.error

import android.os.Process
import cat.ereza.customactivityoncrash.CustomActivityOnCrash

class PidCrashDataCollector : CustomActivityOnCrash.CustomCrashDataCollector {
    override fun onCrash(): String {
        return Process.myPid().toString()
    }
}
