package com.philkes.notallyx.buildlogic.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CopyScreenshotsToDocumentationTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val documentationDirectory: DirectoryProperty

    @TaskAction
    fun copyScreenshots() {
        val sourceDir = sourceDirectory.asFile.get()
        val docusaurusDir = documentationDirectory.asFile.get()
        val screenshots = ScreenshotParser.parse(sourceDir)

        var copiedCount = 0
        screenshots
            .filter { it.locale == "en-US" || it.locale == "en" }
            .forEach { screenshot ->
                docusaurusDir.mkdirs()
                val docusaurusTargetFile = File(docusaurusDir, "${screenshot.baseName}.png")

                screenshot.file.copyTo(docusaurusTargetFile, overwrite = true)
                logger.lifecycle("Copied ${screenshot.file.name} -> ${docusaurusTargetFile.path}")
                copiedCount++
            }
        logger.lifecycle("Successfully copied $copiedCount screenshots to docusaurus.")
    }
}
