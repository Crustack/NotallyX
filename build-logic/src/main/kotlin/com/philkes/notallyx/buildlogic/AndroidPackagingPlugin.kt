package com.philkes.notallyx.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidPackagingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val androidComponents =
                extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
                    ?: return

            androidComponents.onVariants { variant ->
                val versionName = project.findProperty("app.versionName")?.toString() ?: ""
                variant.outputs.forEach { output ->
                    output.outputFileName.set("NotallyX-$versionName.apk")
                }

                val variantName = variant.name
                val variantCapitalized = variantName.replaceFirstChar { it.uppercase() }
                val buildTypeName = variant.buildType ?: variantName

                if (buildTypeName == "release" || buildTypeName == "beta") {
                    fun copyMapping(suffix: String) {
                        val mappingFile =
                            layout.buildDirectory
                                .file("outputs/mapping/$buildTypeName/mapping.txt")
                                .get()
                                .asFile
                        if (mappingFile.exists()) {
                            val targetFile =
                                File(projectDir, "obfuscation/mapping-$buildTypeName-$suffix.txt")
                            targetFile.parentFile.mkdirs()
                            mappingFile.copyTo(targetFile, overwrite = true)
                            logger.lifecycle("Copied mapping to: ${targetFile.absolutePath}")
                        }
                    }

                    tasks
                        .matching { it.name == "assemble$variantCapitalized" }
                        .configureEach { doLast { copyMapping("apk") } }

                    tasks
                        .matching { it.name == "bundle$variantCapitalized" }
                        .configureEach { doLast { copyMapping("bundle") } }

                    if (buildTypeName == "release") {
                        tasks
                            .matching { it.name == "bundle$variantCapitalized" }
                            .configureEach {
                                doLast {
                                    val nativeLibsDir =
                                        layout.buildDirectory
                                            .file(
                                                "intermediates/merged_native_libs/$buildTypeName/merge${variantCapitalized}NativeLibs/out/lib"
                                            )
                                            .get()
                                            .asFile

                                    if (!nativeLibsDir.exists()) {
                                        logger.lifecycle(
                                            "No native debug symbols found in $nativeLibsDir"
                                        )
                                        return@doLast
                                    }
                                    val outputZip =
                                        File(
                                            projectDir,
                                            "obfuscation/$buildTypeName-debug-symbols.zip",
                                        )
                                    outputZip.parentFile.mkdirs()
                                    ZipOutputStream(outputZip.outputStream()).use { zipOut ->
                                        nativeLibsDir.walkTopDown().forEach { file ->
                                            if (file.isFile) {
                                                val relativePath =
                                                    nativeLibsDir
                                                        .toPath()
                                                        .relativize(file.toPath())
                                                        .toString()
                                                zipOut.putNextEntry(ZipEntry(relativePath))
                                                file.inputStream().use { it.copyTo(zipOut) }
                                                zipOut.closeEntry()
                                            }
                                        }
                                    }
                                    logger.lifecycle(
                                        "Native debug symbols zipped to: ${outputZip.absolutePath}"
                                    )
                                }
                            }
                    }
                }
            }

            afterEvaluate {
                tasks.matching { it.name == "bundleRelease" }.configureEach {
                    dependsOn(tasks.matching { it.name == "testReleaseUnitTest" })
                }
                tasks.matching { it.name == "assembleRelease" }.configureEach {
                    dependsOn(tasks.matching { it.name == "testReleaseUnitTest" })
                    finalizedBy(tasks.matching { it.name == "generateChangelogs" })
                }
            }
        }
    }
}
