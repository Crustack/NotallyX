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
            api(libs.koin.android)
            api(libs.koin.androidx.compose)
            api(libs.koin.compose.navigation3)
        }

        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.assertj)
            implementation(libs.kotlinx.coroutines.test)
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
        }
    }
}

ktfmt { kotlinLangStyle() }

configurations
    .matching { it.name.contains("HostTest") || it.name.contains("UnitTest") }
    .configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("androidx.sqlite:sqlite-bundled-android"))
                .using(module("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}"))
        }
    }
