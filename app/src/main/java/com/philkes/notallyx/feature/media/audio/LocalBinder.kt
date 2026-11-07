package com.philkes.notallyx.feature.media.audio

import android.app.Service
import android.os.Binder

class LocalBinder<T : Service>(private val service: T) : Binder() {

    fun getService(): T = service
}
