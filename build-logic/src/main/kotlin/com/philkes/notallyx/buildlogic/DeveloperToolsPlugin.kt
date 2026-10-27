package com.philkes.notallyx.buildlogic

import com.philkes.notallyx.buildlogic.tasks.GenerateChangelogsTask
import com.philkes.notallyx.buildlogic.tasks.InstallGitHooksTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class DeveloperToolsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val installGitHooks =
                tasks.register("installLocalGitHooks", InstallGitHooksTask::class.java) {
                    group = "build setup"
                    description = "Installs local git hooks from .scripts into .git/hooks"
                    scriptsDir.set(rootProject.layout.projectDirectory.dir(".scripts"))
                    hooksDir.set(rootProject.layout.projectDirectory.dir(".git/hooks"))
                }

            tasks.register("generateChangelogs", GenerateChangelogsTask::class.java) {
                group = "publishing"
                description = "Generates release changelogs using generate-changelogs.sh"
                githubToken.set(providers.gradleProperty("CHANGELOG_GITHUB_TOKEN"))
                lastVersionName.set(
                    providers.gradleProperty("app.lastVersionName").orElse("")
                )
                changelogScript.set(
                    rootProject.layout.projectDirectory.file("generate-changelogs.sh")
                )
                changelogFile.set(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
                gradlePropertiesFile.set(
                    rootProject.layout.projectDirectory.file("gradle.properties")
                )
            }

            tasks.matching { it.name == "preBuild" }.configureEach {
                dependsOn(installGitHooks)
            }
        }
    }
}
