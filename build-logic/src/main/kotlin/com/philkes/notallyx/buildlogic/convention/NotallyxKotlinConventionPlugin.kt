package com.philkes.notallyx.buildlogic.convention

import com.ncorti.ktfmt.gradle.KtfmtExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class NotallyxKotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.findByType(JavaPluginExtension::class.java)?.apply {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                jvmToolchain(17)
                targets.all {
                    if (this is KotlinJvmTarget) {
                        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
                    }
                }
            }

            tasks.withType(KotlinCompile::class.java).configureEach {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }

            pluginManager.withPlugin("com.ncorti.ktfmt.gradle") {
                extensions.findByType(KtfmtExtension::class.java)?.apply {
                    kotlinLangStyle()
                }
            }
        }
    }
}
