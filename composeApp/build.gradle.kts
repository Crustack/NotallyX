import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.koin.compiler)
    id("notallyx.kotlin.convention")
}

kotlin {
    android {
        namespace = "com.philkes.notallyx.composeapp"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources { enable = true }

        withHostTest { isIncludeAndroidResources = true }

        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(project.dependencies.platform(libs.androidx.compose.bom))
            api(libs.androidx.compose.ui)
            api(libs.androidx.compose.material3)
            api(libs.androidx.compose.material3.adaptive)
            api(libs.androidx.compose.ui.tooling.preview)
            api(libs.androidx.compose.material.icons.extended)
            api(libs.androidx.lifecycle.runtime.compose)
            api(libs.androidx.navigation3.runtime)
            api(libs.androidx.navigation3.ui)
            api(libs.androidx.lifecycle.viewmodel.navigation3)
            api(libs.coil.compose)
            api(libs.compose.colorpicker)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
        }

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            api(libs.androidx.compose.ui.tooling)
            api(libs.koin.android)
            api(libs.koin.androidx.compose)
            api(libs.koin.compose.navigation3)
        }

        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.assertj)
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("reflect"))
            implementation("androidx.compose.ui:ui-test")
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.assertj)
            implementation(libs.json)
            implementation(libs.mockk)
            implementation(libs.mockito.kotlin)
            implementation(libs.mockito.core)
            implementation(libs.robolectric)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.core.ktx)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.core.testing)
            implementation(libs.androidx.room3.testing)
            implementation("androidx.compose.ui:ui-test-manifest")
        }

        named("desktopMain") { dependencies {} }
        named("desktopTest") {
            dependencies {
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
            }
        }
    }
}

ktfmt { kotlinLangStyle() }

koinCompiler { compileSafety = false }

configurations
    .matching { it.name.contains("HostTest") || it.name.contains("UnitTest") }
    .configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("androidx.sqlite:sqlite-bundled-android"))
                .using(module("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}"))
        }
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
    .matching {
        it.name.contains("desktopTestRuntimeClasspath") ||
            it.name.contains("DesktopTestRuntimeClasspath")
    }
    .configureEach {
        exclude(group = "androidx.compose.ui")
        exclude(group = "androidx.compose.foundation")
        exclude(group = "androidx.compose.material")
        exclude(group = "androidx.compose.material3")
        exclude(group = "androidx.compose.animation")
        exclude(group = "androidx.compose", module = "compose-bom")
    }
