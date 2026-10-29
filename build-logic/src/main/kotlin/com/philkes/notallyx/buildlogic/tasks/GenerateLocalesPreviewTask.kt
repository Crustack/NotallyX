package com.philkes.notallyx.buildlogic.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateLocalesPreviewTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val resDirectory = resDir.asFile.get()
        val outputDirectory = outputDir.asFile.get()

        val localeDirs =
            resDirectory
                .listFiles { f ->
                    f.isDirectory &&
                        f.name.startsWith("values-") &&
                        !f.name.startsWith("values-v") &&
                        f.name != "values-night" &&
                        File(f, "strings.xml").exists()
                }
                ?.sortedBy { it.name } ?: emptyList()

        val locales = mutableListOf("en" to "en-US", "de" to "de-DE")
        // TODO: check which locales should really be included
        //            localeDirs.forEach { dir ->
        //                val rawTag = dir.name.removePrefix("values-")
        //                val tag = rawTag.replace("-r", "-")
        //                val name = defaultRegionMap[rawTag] ?: defaultRegionMap[tag] ?: tag
        //                locales.add(tag to name)
        //            }
        val annotations =
            locales.joinToString("\n") { (code, label) ->
                """@Preview(name = "$label", locale = "$code", showBackground = true)"""
            }

        val outputFile =
            outputDirectory.resolve("com/philkes/notallyx/misc/appstore/AppStorePreview.kt")
        outputFile.parentFile.mkdirs()

        val generatedContent =
            """
package com.philkes.notallyx.misc.appstore

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation for generating app store screenshots across multiple locales.
 * Auto-generated during build from res/values-* folders by the generateLocalesPreview task.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
$annotations
annotation class AppStorePreview

typealias LocalesPreview = AppStorePreview
"""
                .trimStart()

        if (!outputFile.exists() || outputFile.readText() != generatedContent) {
            outputFile.writeText(generatedContent)
            logger.lifecycle(
                "Generated AppStorePreview with ${locales.size} locales at: ${outputFile.absolutePath}"
            )
        }
    }
}
