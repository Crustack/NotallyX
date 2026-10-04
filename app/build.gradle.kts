@file:OptIn(ExperimentalRoborazziApi::class)

import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler

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

    testOptions { unitTests.isIncludeAndroidResources = true }
}

androidComponents {
    onVariants { variant ->
        val versionName = project.findProperty("app.versionName").toString()
        variant.outputs.forEach { output -> output.outputFileName.set("NotallyX-$versionName.apk") }

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
                    val target =
                        File(project.projectDir, "obfuscation/mapping-$buildTypeName-$suffix.txt")
                    target.parentFile.mkdirs()
                    mappingFile.copyTo(target, overwrite = true)
                    println("Copied mapping to: ${target.absolutePath}")
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
                                println("No native debug symbols found in $nativeLibsDir")
                                return@doLast
                            }
                            val outputZip =
                                File(
                                    project.projectDir,
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
                            println("Native debug symbols zipped to: ${outputZip.absolutePath}")
                        }
                    }
            }
        }
    }
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

//
// tasks.named<Task>("assembleDebugAndroidTest") {
//    doLast {
//        logger.lifecycle("Task ${name} finished assembling!")
//    }
// }

// Access internal AGP tasks with their explicit type
// tasks.named<PackageAndroidArtifact>("packageDebugAndroidTest") {
//    doLast {
//        // Strongly typed access to task properties
//        val apkFolder = outputDirectory.get().asFile
//        logger.lifecycle("Packaging APKs into: ${apkFolder.absolutePath}")
//    }
// }

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
    source = project.fileTree(rootDir)
    include("**/*.kt")
}

tasks.register<Copy>("installLocalGitHooks") {
    val scriptsDir = File(rootProject.rootDir, ".scripts/")
    val hooksDir = File(rootProject.rootDir, ".git/hooks")
    from(scriptsDir) { include("pre-commit", "pre-commit.bat") }
    into(hooksDir)
    inputs.files(file("${scriptsDir}/pre-commit"), file("${scriptsDir}/pre-commit.bat"))
    outputs.dir(hooksDir)
    filePermissions { unix(509) }
    // If this throws permission denied:
    // chmod +rwx ./.git/hooks/pre-commit*
}

tasks.preBuild.dependsOn(tasks.named("installLocalGitHooks"), tasks.exportTranslationsToExcel)

tasks.register("generateChangelogs") {
    doLast {
        val githubToken = providers.gradleProperty("CHANGELOG_GITHUB_TOKEN").orNull

        val command =
            mutableListOf(
                "bash",
                rootProject.file("generate-changelogs.sh").absolutePath,
                "v${project.findProperty("app.lastVersionName").toString()}",
                rootProject.file("CHANGELOG.md").absolutePath,
            )
        if (!githubToken.isNullOrEmpty()) {
            command.add(githubToken)
        } else {
            println(
                "CHANGELOG_GITHUB_TOKEN not found, which limits the allowed amount of Github API calls"
            )
        }
        providers.exec {
            commandLine(command)
            standardOutput = System.out
            errorOutput = System.err
        }

        val config = PropertiesConfiguration()
        val fileHandler =
            FileHandler(config).apply {
                file = rootProject.file("gradle.properties")
                load()
            }
        val currentVersionName = config.getProperty("app.versionName")
        config.setProperty("app.lastVersionName", currentVersionName)
        fileHandler.save()
        println("Updated app.lastVersionName to $currentVersionName")
    }
}

afterEvaluate {
    tasks.named("bundleRelease").configure { dependsOn(tasks.named("testReleaseUnitTest")) }
    tasks.named("assembleRelease").configure {
        dependsOn(tasks.named("testReleaseUnitTest"))
        finalizedBy(tasks.named("generateChangelogs"))
    }
}

roborazzi {
    // Directory for reference images
    outputDir.set(file("src/screenshots"))
    compare { outputDir.set(file("build/outputs/screenshots_comparison")) }
}

dependencies {
    implementation(libs.androidx.documentfile)
    implementation(libs.bundles.navigation)
    implementation(libs.androidx.preference)
    ksp(libs.androidx.room.compiler)
    implementation(libs.bundles.room)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.work)
    implementation(libs.androidx.biometric)
    implementation(libs.customactivityoncrash)
    implementation(libs.subsampling.scale.image.view)
    implementation(libs.glide)
    implementation(libs.swipe.drawer)
    implementation(libs.colorpickerview)
    implementation(libs.material)
    implementation(libs.jsr305)
    implementation(libs.fastscroll)
    implementation(libs.zip4j)
    implementation(libs.sqlcipher)
    implementation(libs.kotlinx.serialization)
    implementation(libs.jsoup)
    implementation(libs.prettytime)
    implementation(libs.simple.xml) { exclude(group = "xpp3", module = "xpp3") }
    implementation(libs.bundles.commonmark)
    implementation("com.github.luben:zstd-jni:${libs.versions.zstd.get()}@aar")

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)

    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.json)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.zstd.jni.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.espresso.contrib)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}
