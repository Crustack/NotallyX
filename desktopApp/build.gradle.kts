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
    .matching { it.name == "desktopRuntimeClasspath" }
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
