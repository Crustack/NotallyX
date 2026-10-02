buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.commons.configuration)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.firebase.testlab) apply false
    alias(libs.plugins.roborazzi) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory.asFile)
}