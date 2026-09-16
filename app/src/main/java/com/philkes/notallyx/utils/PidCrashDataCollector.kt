package com.philkes.notallyx.utils

import android.os.Process
import cat.ereza.customactivityoncrash.CustomActivityOnCrash

class PidCrashDataCollector : CustomActivityOnCrash.CustomCrashDataCollector {
    override fun onCrash(): String? {
        return Process.myPid().toString()
    }
}
