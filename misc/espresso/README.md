# Robo Tests

## Add Test
- Add any `AndroidJunit4Test` class to [androidTest](../../app/src/androidTest/kotlin)
- Use AndroidStudio's `Record Espresso Test` action to create a new test via UI
- Run `./gradlew clean assembleDebug` and `./gradlew assembleDebugAndroidTest`
- Go To Firebase Test Lab -> Start Instrumented Test
- Upload APK from [app/build/outputs/apk/debug](../../app/build/outputs/apk/debug) and Test-APK from [app/build/outputs/apk/debug](../../app/build/outputs/apk/androidTest/debug)