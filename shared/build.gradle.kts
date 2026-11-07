import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.koin.compiler)
    id("notallyx.kotlin.convention")
}

kotlin {
    android {
        namespace = "com.philkes.notallyx"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        withJava()

        androidResources { enable = true }

        withHostTest { isIncludeAndroidResources = true }

        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room3.common)
            api(libs.androidx.room3.runtime)
            api(libs.sqlite.bundled)
            api(libs.kotlinx.serialization)
            api(libs.androidx.datastore)
            api(libs.androidx.datastore.preferences)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.jsoup)
            api(libs.prettytime)
            api(libs.simple.xml)
            api(libs.bundles.commonmark)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.androidx.appcompat)
            api(libs.androidx.documentfile)
            api(libs.androidx.datastore.tink)
            api(libs.tink.android)
            api(libs.androidx.room3.sqlite.wrapper)
            api(libs.sqlite.framework)
            api(libs.androidx.sqlite)
            api(libs.sqlcipher)
            api(libs.zip4j)
            api(libs.material)
            api(libs.koin.android)
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
            implementation(libs.androidx.room3.testing)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.core.ktx)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.core.testing)
        }

        named("desktopMain") { dependencies {} }

        named("desktopTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.assertj)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
}

ktfmt { kotlinLangStyle() }

configurations.configureEach { exclude(mapOf("group" to "xpp3", "module" to "xpp3")) }

configurations
    .matching { it.name.contains("HostTest") || it.name.contains("UnitTest") }
    .configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("androidx.sqlite:sqlite-bundled-android"))
                .using(module("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}"))
        }
    }
