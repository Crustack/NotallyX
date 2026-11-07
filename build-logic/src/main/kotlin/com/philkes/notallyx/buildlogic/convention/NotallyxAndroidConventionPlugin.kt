package com.philkes.notallyx.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class NotallyxAndroidConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("notallyx.kotlin.convention")

            extensions.findByType(ApplicationExtension::class.java)?.apply {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            extensions.findByType(LibraryExtension::class.java)?.apply {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }
}
