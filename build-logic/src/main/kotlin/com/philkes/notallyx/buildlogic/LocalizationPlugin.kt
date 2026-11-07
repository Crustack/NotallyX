package com.philkes.notallyx.buildlogic

import com.philkes.notallyx.buildlogic.tasks.GenerateLocalesPreviewTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LocalizationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val composeResDir = layout.projectDirectory.dir("src/main/composeResources")
            val sharedResDir =
                rootProject.layout.projectDirectory.dir("shared/src/androidMain/res")
            val defaultResDir = layout.projectDirectory.dir("src/main/res")
            val generateLocalesPreview =
                tasks.register("generateLocalesPreview", GenerateLocalesPreviewTask::class.java) {
                    group = "build"
                    description =
                        "Generates AppStorePreview multi-preview annotation for all locales in composeResources/values-* or res/values-*/strings.xml"
                    resDir.set(
                        if (sharedResDir.asFile.exists()) sharedResDir else defaultResDir
                    )
                    if (composeResDir.asFile.exists()) {
                        composeResourcesDir.set(composeResDir)
                    }
                    outputDir.set(
                        layout.buildDirectory.dir("generated/source/localesPreview/kotlin")
                    )
                }

            tasks
                .matching { it.name == "preBuild" }
                .configureEach { dependsOn(generateLocalesPreview) }
        }
    }
}
