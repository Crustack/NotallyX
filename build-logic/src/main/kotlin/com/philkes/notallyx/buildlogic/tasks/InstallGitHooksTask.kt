package com.philkes.notallyx.buildlogic.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class InstallGitHooksTask
@Inject
constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val scriptsDir: DirectoryProperty

    @get:OutputDirectory abstract val hooksDir: DirectoryProperty

    @TaskAction
    fun install() {
        val scripts = scriptsDir.asFile.get()
        val hooks = hooksDir.asFile.get()

        fileSystemOperations.copy {
            from(scripts) { include("pre-commit", "pre-commit.bat") }
            into(hooks)
            filePermissions { unix(509) }
        }
        logger.lifecycle("Git hooks installed into ${hooks.path}")
    }
}
