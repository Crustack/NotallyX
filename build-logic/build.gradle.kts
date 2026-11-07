plugins {
    `kotlin-dsl`
}

group = "com.philkes.notallyx.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.commons.configuration)
}

gradlePlugin {
    plugins {
        register("androidPackaging") {
            id = "notallyx.android.packaging"
            implementationClass = "com.philkes.notallyx.buildlogic.AndroidPackagingPlugin"
        }
        register("screenshots") {
            id = "notallyx.android.screenshots"
            implementationClass = "com.philkes.notallyx.buildlogic.ScreenshotsPlugin"
        }
        register("localization") {
            id = "notallyx.android.localization"
            implementationClass = "com.philkes.notallyx.buildlogic.LocalizationPlugin"
        }
        register("developerTools") {
            id = "notallyx.developer.tools"
            implementationClass = "com.philkes.notallyx.buildlogic.DeveloperToolsPlugin"
        }
    }
}
