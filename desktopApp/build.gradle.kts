import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.koin.compiler)
    id("notallyx.kotlin.convention")
}

kotlin {
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        named("desktopMain") {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":composeApp"))
                implementation(compose.desktop.currentOs)
                implementation(compose.materialIconsExtended)
                implementation(libs.compose.material3.desktop)
                implementation(libs.koin.core)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.assertj)
                implementation(libs.kotlinx.coroutines.test)
                implementation(
                    "org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.composeMultiplatform.get()}"
                )
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.150.2")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.150.2")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.150.2")
                implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.150.2")
                implementation(libs.compose.desktop.currentOs)
                implementation(libs.compose.material3.desktop)
                implementation(libs.koin.test)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.philkes.notallyx.desktop.MainKt"
        jvmArgs += listOf("-Xmx512m")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NotallyX"
            packageVersion = "1.0.0"
            description = "NotallyX Desktop"
            vendor = "NotallyX"
        }
    }
}

tasks.register("verifyDesktopBuild") {
    group = "verification"
    description = "Verifies desktop application compilation and assembly output."
    dependsOn(tasks.named("desktopJar"), tasks.named("assemble"))
}

configurations
    .matching { it.name.contains("Desktop") || it.name.contains("desktop") }
    .configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("androidx.compose.ui:ui-test"))
                .using(
                    module(
                        "org.jetbrains.compose.ui:ui-test:${libs.versions.composeMultiplatform.get()}"
                    )
                )
            substitute(module("androidx.compose.ui:ui-test-junit4"))
                .using(
                    module(
                        "org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.composeMultiplatform.get()}"
                    )
                )
            substitute(module("androidx.compose.material3:material3"))
                .using(
                    module(
                        "org.jetbrains.compose.material3:material3-desktop:${libs.versions.composeMaterial3.get()}"
                    )
                )
        }
    }

configurations
    .matching { it.name.contains("Desktop") || it.name.contains("desktop") }
    .configureEach {
        exclude(group = "androidx.compose.ui")
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.material")
        exclude(group = "androidx.compose.material3")
        exclude(group = "androidx.compose.material3.adaptive")
        exclude(group = "androidx.compose.animation")
        exclude(group = "androidx.compose", module = "compose-bom")
    }

ktfmt { kotlinLangStyle() }
