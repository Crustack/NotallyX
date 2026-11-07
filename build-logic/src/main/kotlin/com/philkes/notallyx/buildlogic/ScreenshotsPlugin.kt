package com.philkes.notallyx.buildlogic

import com.philkes.notallyx.buildlogic.tasks.CopyScreenshotsToDocumentationTask
import com.philkes.notallyx.buildlogic.tasks.CopyScreenshotsToFastlaneTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ScreenshotsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val updateScreenshotsTask = tasks.matching { it.name == "updateDebugScreenshotTest" }

            val copyFastlane =
                tasks.register(
                    "copyScreenshotsToFastlane",
                    CopyScreenshotsToFastlaneTask::class.java,
                ) {
                    group = "documentation"
                    description =
                        "Copies screenshotTest images to fastlane metadata phoneScreenshots by locale."
                    dependsOn(updateScreenshotsTask)
                    sourceDirectory.set(layout.projectDirectory.dir("src/screenshotTestDebug"))
                    fastlaneDirectory.set(
                        rootProject.layout.projectDirectory.dir("fastlane/metadata/android")
                    )
                }

            val copyDocs =
                tasks.register(
                    "copyScreenshotsToDocumentation",
                    CopyScreenshotsToDocumentationTask::class.java,
                ) {
                    group = "documentation"
                    description =
                        "Copies screenshotTest images to Docusaurus documentation phoneScreenshots."
                    dependsOn(updateScreenshotsTask)
                    sourceDirectory.set(layout.projectDirectory.dir("src/screenshotTestDebug"))
                    documentationDirectory.set(
                        rootProject.layout.projectDirectory.dir(
                            "documentation/static/img/phoneScreenshots"
                        )
                    )
                }

            afterEvaluate {
                tasks
                    .matching { it.name == "generateChangelogs" }
                    .configureEach { finalizedBy(copyFastlane, copyDocs) }
            }
        }
    }
}
