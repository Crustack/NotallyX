package com.philkes.notallyx.buildlogic.tasks

import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateChangelogsTask : DefaultTask() {
    @get:Input @get:Optional abstract val githubToken: Property<String>

    @get:Input abstract val lastVersionName: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val changelogScript: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val changelogFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gradlePropertiesFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val token = githubToken.orNull
        val script = changelogScript.asFile.get()
        val changelog = changelogFile.asFile.get()
        val rootDir = script.parentFile

        val command =
            mutableListOf(
                "bash",
                script.absolutePath,
                "v${lastVersionName.get()}",
                changelog.absolutePath,
            )

        if (!token.isNullOrEmpty()) {
            command.add(token)
        } else {
            logger.lifecycle(
                "CHANGELOG_GITHUB_TOKEN not found, which limits the allowed amount of Github API calls"
            )
        }

        val process =
            ProcessBuilder(command)
                .directory(rootDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("Changelog generation script failed with exit code $exitCode")
        }

        val propFile = gradlePropertiesFile.asFile.get()
        val config = PropertiesConfiguration()
        val fileHandler =
            FileHandler(config).apply {
                file = propFile
                load()
            }
        val currentVersionName = config.getProperty("app.versionName")?.toString() ?: ""
        config.setProperty("app.lastVersionName", currentVersionName)
        fileHandler.save()
        logger.lifecycle("Updated app.lastVersionName to $currentVersionName")
    }
}
