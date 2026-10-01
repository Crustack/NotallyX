buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
        classpath("org.apache.commons:commons-configuration2:2.4")
    }
}

plugins {
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.firebase.testlab") version "0.0.1-alpha13" apply false
    id("io.github.takahirom.roborazzi") version "1.43.1" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory.asFile)
}