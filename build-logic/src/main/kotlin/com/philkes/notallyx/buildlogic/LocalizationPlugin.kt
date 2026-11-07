package com.philkes.notallyx.buildlogic

import com.philkes.notallyx.buildlogic.tasks.GenerateLocalesPreviewTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LocalizationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val generateLocalesPreview =
                tasks.register("generateLocalesPreview", GenerateLocalesPreviewTask::class.java) {
                    group = "build"
                    description =
                        "Generates AppStorePreview multi-preview annotation for all locales in res/values-*/strings.xml"
                    resDir.set(layout.projectDirectory.dir("src/main/res"))
                    outputDir.set(
                        layout.buildDirectory.dir("generated/source/localesPreview/kotlin")
                    )
                }

            tasks.matching { it.name == "preBuild" }.configureEach {
                dependsOn(generateLocalesPreview)
            }
        }
    }
}
