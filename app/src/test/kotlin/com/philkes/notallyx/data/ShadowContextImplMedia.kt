package com.philkes.notallyx.data

import java.io.File
import java.nio.file.Files
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(className = "android.app.ContextImpl")
class ShadowContextImplMedia {

    private val randomTempDir = Files.createTempDirectory("notallyx_robolectric_media_").toFile()

    @Implementation
    fun getExternalMediaDirs(): Array<File> {
        // Return a mock directory in target/tmp cache
        return arrayOf(randomTempDir)
    }
}
