@file:OptIn(ExperimentalRoborazziApi::class)

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import io.github.crustack.auto.translation.plugin.config.FastlaneTranslationConfig
import io.github.crustack.auto.translation.plugin.config.StringsXmlTranslationConfig
import io.github.crustack.auto.translation.plugin.task.AutoTranslateTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.testlab)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.translations.converter)
    alias(libs.plugins.auto.translation)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.android.compose.screenshot)
    kotlin("plugin.compose") version libs.versions.kotlin
    // See build-logic module
    id("notallyx.android.packaging")
    id("notallyx.android.screenshots")
    id("notallyx.android.localization")
    id("notallyx.developer.tools")
}

android {
    namespace = "com.philkes.notallyx"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        applicationId = "com.philkes.notallyx"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = project.findProperty("app.versionCode").toString().toInt()
        versionName = project.findProperty("app.versionName").toString()
        androidResources {
            localeFilters +=
                listOf(
                    "en",
                    "ar",
                    "ca",
                    "cs",
                    "da",
                    "de",
                    "el",
                    "es",
                    "fr",
                    "hu",
                    "in",
                    "it",
                    "ja",
                    "my",
                    "nb",
                    "nl",
                    "nn",
                    "pl",
                    "pt-rBR",
                    "pt-rPT",
                    "ro",
                    "ru",
                    "sk",
                    "sv",
                    "tl",
                    "tr",
                    "uk",
                    "vi",
                    "zh-rCN",
                    "zh-rTW",
                )
        }
        vectorDrawables.generatedDensities?.clear()
        ndk { debugSymbolLevel = "FULL" }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testOptions {
            animationsDisabled = true
            execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(providers.gradleProperty("RELEASE_STORE_FILE").get())
            storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "NotallyX DEBUG")
        }
        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("beta") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-BETA"
            resValue("string", "app_name", "NotallyX BETA")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        resValues = true
        compose = true
        testFixtures.enable = true
    }

    packaging {
        resources.excludes +=
            listOf(
                "DebugProbesKt.bin",
                "META-INF/**.version",
                "kotlin/**.kotlin_builtins",
                "kotlin-tooling-metadata.json",
            )
    }

    sourceSets.getByName("main") {
        kotlin.directories.add(
            "${layout.buildDirectory.get().asFile}/generated/source/localesPreview/kotlin"
        )
    }

    sourceSets.getByName("debug") { res.directories.add("src/screenshotTestDebug/res") }

    testOptions { unitTests.isIncludeAndroidResources = true }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

screenshotTests {
    imageDifferenceThreshold = 0.00025f // 0.025%
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

ktfmt { kotlinLangStyle() }

autoTranslate {
    provider = deepL { authKey = providers.gradleProperty("DEEPL_API_KEY") }
    translateStringsXml { enabled = true }
    translateFastlane {
        enabled = false
        targetLanguages = setOf("de-DE", "ru-RU")
    }
}

tasks.register<AutoTranslateTask>("autoTranslateAppStoreStrings") {
    group = "translations"
    description = "Auto-translate screenshotTestDebug strings.xml files"
    provider.set(autoTranslate.provider)
    targetLanguages.set(android.androidResources.localeFilters - "en")
    translateFastlane.set(
        project.objects.newInstance<FastlaneTranslationConfig>().apply { enabled.set(false) }
    )
    translateStringsXml.set(
        project.objects.newInstance<StringsXmlTranslationConfig>().apply {
            enabled.set(true)
            resDirectory.set(layout.projectDirectory.dir("src/screenshotTestDebug/res"))
        }
    )
}

firebaseTestLab {
    val serviceAccountPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (!serviceAccountPath.isNullOrEmpty()) {
        serviceAccountCredentials.set(file(serviceAccountPath))
    }
    managedDevices {
        create("galaxyA06") {
            device = "a06"
            apiLevel = 35
        }

        create("pixel5") {
            device = "redfin"
            apiLevel = 30
        }

        create("pixel10ProXL") {
            device = "mustang"
            apiLevel = 36
        }

        create("mediumPhone") {
            device = "MediumPhone.arm"
            apiLevel = 28
        }
    }
    testOptions {
        fixture { grantedPermissions = "all" }

        execution { maxTestReruns = 2 }
    }
}

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
    source = project.fileTree(rootDir)
    include("**/*.kt")
}

tasks.named("preBuild").configure { dependsOn(tasks.named("exportTranslationsToExcel")) }

roborazzi {
    // Directory for reference images
    outputDir.set(file("src/screenshots"))
    compare { outputDir.set(file("build/outputs/screenshots_comparison")) }
}

dependencies {
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.tink)
    implementation(libs.tink.android)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.room3.common)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.room3.sqlite.wrapper)
    implementation(libs.sqlite.bundled)
    implementation(libs.sqlite.framework)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.work)
    implementation(libs.androidx.biometric)
    implementation(libs.customactivityoncrash)
    implementation(libs.material)
    implementation(libs.zip4j)
    implementation(libs.sqlcipher)
    implementation(libs.kotlinx.serialization)
    implementation(libs.jsoup)
    implementation(libs.prettytime)
    implementation(libs.simple.xml) { exclude(group = "xpp3", module = "xpp3") }
    implementation(libs.bundles.commonmark)
    implementation("com.github.luben:zstd-jni:${libs.versions.zstd.get()}@aar")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.colorpicker)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidgetPreview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation("androidx.compose.material:material-icons-extended:1.7.8")

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.koin.androidx.workmanager)

    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(project(":test-lib"))

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.assertj)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.zstd.jni.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":test-lib"))
}
