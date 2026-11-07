package com.philkes.notallyx.data

import java.io.File
import java.nio.file.Files
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowContextImpl

@Implements(className = "android.app.ContextImpl")
class ShadowContextImplMedia : ShadowContextImpl() {

    private val randomTempDir = Files.createTempDirectory("notallyx_robolectric_media_").toFile()

    @Implementation
    fun getExternalMediaDirs(): Array<File> {
        return arrayOf(randomTempDir)
    }
}
