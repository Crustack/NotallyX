package com.philkes.notallyx.buildlogic.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CopyScreenshotsToFastlaneTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val fastlaneDirectory: DirectoryProperty

    @TaskAction
    fun copyScreenshots() {
        val sourceDir = sourceDirectory.asFile.get()
        val fastlaneDir = fastlaneDirectory.asFile.get()
        val screenshots = ScreenshotParser.parse(sourceDir)

        var copiedCount = 0
        screenshots.forEach { screenshot ->
            val fastlaneTargetDir =
                File(fastlaneDir, "${screenshot.locale}/images/phoneScreenshots")
            fastlaneTargetDir.mkdirs()
            val fastlaneTargetFile = File(fastlaneTargetDir, "${screenshot.baseName}.png")

            screenshot.file.copyTo(fastlaneTargetFile, overwrite = true)
            logger.lifecycle("Copied ${screenshot.file.name} -> ${fastlaneTargetFile.path}")
            copiedCount++
        }
        logger.lifecycle("Successfully copied $copiedCount screenshots to fastlane metadata.")
    }
}
