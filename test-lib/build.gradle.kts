plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.philkes.notallyx.test"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(project(":app"))

    // Shared test dependencies used by both unit tests (test) and instrumented tests (androidTest)
    api(platform(libs.androidx.compose.bom))
    api("androidx.compose.ui:ui-test-junit4")
    api(libs.kaspresso.core)
    api(libs.kaspresso.compose.support)
    api(libs.androidx.test.espresso.core)
    api(libs.androidx.test.espresso.contrib)
    api(libs.androidx.test.espresso.intents)
    api(libs.androidx.test.junit)

    // Dependencies required by shared test helpers in test-lib
    api(libs.swipe.drawer)
    api(libs.androidx.work)
    api(libs.androidx.sqlite)
    api(libs.androidx.room.runtime)
    api(libs.androidx.test.uiautomator)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.rules)
    api(libs.androidx.test.core.ktx)
    api(libs.androidx.test.core)
    api(libs.androidx.core.testing)
    api(libs.junit)
    api(libs.mockk)
    api(libs.mockito.kotlin)
    api(libs.mockito.core)
    api(platform(libs.koin.bom))
    api(libs.koin.test)
    api(libs.koin.test.junit4)
}
