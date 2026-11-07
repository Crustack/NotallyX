11/6/26, 2:43 PM
KMP Project Testing Best Practices
KMP Project Testing Best Practices
https://gemini.google.com/app/961e3fbca7297e2d
User prompt: i have kmp project with android and desktop target. What kind of tests are best practice?
Response: In a Kotlin Multiplatform (KMP) project targeting Android and Desktop (JVM), the core best practice is the "Test Once in Common,
Validate Edges on Target" strategy. Because both targets run on JVM-based environments, you can achieve near-instant feedback cycles while
maintaining platform-specific confidence.
1. Test Pyramid & Source Set Placement
   Test LayerLocationTools / LibrariesWhat to Test
   Business & Domain
   LogiccommonTestkotlin.test, kotlinx-
   coroutines-test, TurbineRepositories, use cases, mappers, business
   algorithms.
   State &
   PresentationcommonTestkotlin.test, TurbineViewModels / StateHolders, StateFlow
   transformations, navigation state logic.
   UI Component &
   Screen TestscommonTestcompose.uiTest
   (runComposeUiTest)Shared Compose Multiplatform components,
   semantics assertions, user interaction flows.
   Platform-Specific
   Unit TestsandroidHostTest /
   jvmTestPlatform JUnit runners, Robolectric
   (Android)Platform-specific implementations (actual classes,
   Android Keystore, Desktop filesystem wrappers).
   Instrumented /
   Device TestsandroidDeviceTestAndroidJUnitRunner, EspressoAndroid system integration (Activities, Services,
   permissions).
2. Common Domain & State Testing (commonTest)
   Aim to keep 70–80% of your test suite in commonTest. These tests compile and execute quickly via ./gradlew jvmTest or during the Android
   unit test phase.
   Kotlin Test API: Rely on kotlin.test assertions (assertEquals, assertTrue) rather than importing JUnit directly into common code.
   Coroutines & Asynchronous Code: Use kotlinx-coroutines-test with runTest. This controls virtual time and advances delays
   automatically:
   @Test
   fun `loads data successfully`() = runTest {
   val repository = FakeUserRepository()
   val viewModel = UserViewModel(repository)
   viewModel.loadUser("user_1")
   assertEquals(UiState.Success, viewModel.uiState.value)
   }
   Flows and State Streams: Use CashApp’s Turbine to test StateFlow and SharedFlow emissions in sequence:
   viewModel.uiState.test {
   assertEquals(UiState.Loading, awaitItem())
   assertEquals(UiState.Success, awaitItem())
   }
   Test Doubles: Prefer Fakes over dynamic mocking frameworks. Creating interface-backed fakes (e.g., FakeDatabase, FakeAuthApi) in
   commonTest avoids reflection and platform dependency issues.
3. UI Testing with Compose Multiplatform
   If you are using Compose Multiplatform (CMP), you do not need separate UI tests for desktop and mobile.
   Unified UI Tests in commonTest: CMP provides compose.uiTest with runComposeUiTest:
   @OptIn(ExperimentalTestApi::class)
   class LoginScreenTest {
   @Test
   fun testLoginButtonDisabledWhenEmpty() = runComposeUiTest {
   setContent {
   LoginScreen()
   }
   onNodeWithTag("login_button").assertIsNotEnabled()
   onNodeWithTag("email_input").performTextInput("test@example.com")
   onNodeWithTag("login_button").assertIsEnabled()
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   1/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   }
   }
   Execution Strategy:
   Desktop (./gradlew desktopTest or jvmTest): Runs headlessly on your developer machine or CI container in seconds without
   needing an emulator.
   Android (./gradlew connectedAndroidTest): Runs the same semantic tests on an Android device or emulator to catch rendering
   or layout issues.
4. Target-Specific Edge Cases
   Reserve platform-specific test directories strictly for native boundaries:
   Android (androidHostTest / androidDeviceTest):
   Android Lifecycle edge cases (e.g., process death, configuration changes).
   Storage and IPC (Room database migrations, EncryptedSharedPreferences).
   Use Robolectric in host tests if you need Android SDK APIs without the overhead of booting an emulator.
   Desktop (desktopTest / jvmTest):
   Desktop-specific capabilities: tray menus, multi-window lifecycles, global hotkeys, or OS file picker integration.
   Window resizing behaviors (e.g., switching responsive layouts between ultra-wide desktop monitors and compact splits).
5. Recommended CI Workflow
   Because Desktop tests run directly on the JVM runner, use them as your rapid local gate:
1. Fast Local PR Gate: Run ./gradlew jvmTest desktopTest. This checks all common business logic, ViewModels, and Compose UI
   logic in seconds.
2. Nightly / Merge Gate: Run Android instrumented tests (./gradlew connectedCheck) using a headless emulator or managed device
   runner to catch Android runtime nuances.
3. Coverage & Hygiene: Use Kover (org.jetbrains.kotlinx.kover) to track test coverage across both shared and platform modules from
   a unified report.
   Sources
1. Testing Compose Multiplatform UI - Kotlin
2. Test your multiplatform app − tutorial - Kotlin
3. Kotlin Multiplatform Mini-Course: Testing and Deployment Strategies
4. Testing in Compose Multiplatform (CMP) from Common Code
   User prompt: how to write a test for desktop
   Response: To write tests specifically for the Desktop target in a KMP project, you place them in the jvmTest (or desktopTest, depending on your
   source set naming) directory. These tests run directly on the host JVM using standard JUnit machinery—meaning they start instantly and require no
   device emulator.
   Here is how to set up and write unit tests, desktop-specific feature tests, and Compose UI tests for Desktop.
1. Configure the Gradle Dependencies
   In your shared module's build.gradle.kts, ensure the JVM/Desktop test dependencies are declared:
   kotlin {
   jvm("desktop") // or jvm() depending on your setup
   sourceSets {
   val desktopTest by getting {
   dependencies {
   implementation(kotlin("test"))
   implementation(kotlin("test-junit"))
   // If testing Compose Multiplatform UI components on Desktop:
   @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
   implementation(compose.desktop.uiTestJUnit4)
   // Coroutines testing
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   2/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
   }
   }
   }
   }
2. Standard Desktop Unit Test
   Use this for testing JVM-specific integrations like desktop file systems, local preferences, or native library wrappers.
   File location: src/desktopTest/kotlin/com/example/app/DesktopFileStorageTest.kt
   package com.example.app
   import java.io.File
   import kotlin.test.AfterTest
   import kotlin.test.BeforeTest
   import kotlin.test.Test
   import kotlin.test.assertEquals
   import kotlin.test.assertTrue
   class DesktopFileStorageTest {
   private lateinit var tempDir: File
   private lateinit var storage: DesktopFileStorage
   @BeforeTest
   fun setUp() {
   tempDir = createTempDir("desktop_test")
   storage = DesktopFileStorage(baseDir = tempDir)
   }
   @AfterTest
   fun tearDown() {
   tempDir.deleteRecursively()
   }
   @Test
   fun `writes and reads file correctly on desktop OS`() {
   val fileName = "settings.json"
   val content = """{"theme":"dark"}"""
   storage.save(fileName, content)
   val retrieved = storage.read(fileName)
   assertEquals(content, retrieved)
   assertTrue(File(tempDir, fileName).exists())
   }
   }
3. Compose Multiplatform UI Test on Desktop
   Desktop Compose UI tests run headlessly on your machine. You can use standard Compose testing finders (onNodeWithTag, performClick) via
   createComposeRule().
   File location: src/desktopTest/kotlin/com/example/app/DesktopCounterTest.kt
   package com.example.app
   import androidx.compose.material.Button
   import androidx.compose.material.Text
   import androidx.compose.runtime.*
   import androidx.compose.ui.Modifier
   import androidx.compose.ui.platform.testTag
   import androidx.compose.ui.test.*
   import androidx.compose.ui.test.junit4.createComposeRule
   import org.junit.Rule
   import org.junit.Test
   class DesktopCounterTest {
   @get:Rule
   val rule = createComposeRule()
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   3/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   @Test
   fun counterIncrementsOnClick() {
   rule.setContent {
   var count by remember { mutableStateOf(0) }
   Button(
   onClick = { count++ },
   modifier = Modifier.testTag("increment_button")
   ) {
   Text(text = "Count: $count", modifier = Modifier.testTag("count_text"))
   }
   }
   // Verify initial state
   rule.onNodeWithTag("count_text").assertTextEquals("Count: 0")
   // Perform click
   rule.onNodeWithTag("increment_button").performClick()
   // Verify updated state
   rule.onNodeWithTag("count_text").assertTextEquals("Count: 1")
   }
   }
4. Running the Tests
   Execute tests directly from the terminal or using the IDE test runner gutter icons:
   Run all desktop tests:
   ./gradlew desktopTest
   (If your target is named jvm, use ./gradlew jvmTest)
   Run a specific test class:
   ./gradlew desktopTest --tests "com.example.app.DesktopCounterTest"
   Inspect the test report: Open build/reports/tests/desktopTest/index.html in your browser to inspect failed assertions and
   execution times.
   User prompt: fpr the DesktopCounterTest, I mean should we properly start the application to test something?
   Response: No, for most UI testing, you should not launch the full desktop application window.
   In Compose Multiplatform, UI tests are designed to run in a headless, lightweight test harness (createComposeRule() or runComposeUiTest).
   Launching the actual application { Window(...) { ... } } entry point is only needed for end-to-end (E2E) integration tests.
   Here is the difference between the two approaches, why the lightweight rule is standard, and how to write a full application launch test if you actually
   need one.
   Why Component/Screen Tests Don't Launch the Window
   When you call rule.setContent { ... }:
1. Virtual clock control: Compose creates an in-memory scene without a native OS window decoration. Coroutine animations and frames are
   tied to a virtual clock (rule.mainClock), allowing tests to advance time deterministically without flaky Thread.sleep() pauses.
2. Speed & CI stability: A native window requires initializing OS graphic context and display servers (like X11/Wayland on Linux CI or display
   hooks on macOS/Windows), which frequently crash on headless servers with HeadlessException.
3. Isolation: You test the composable or screen logic (CounterScreen()) isolated from window lifecycle events, global singletons, or external
   network hooks.
   When Should You Actually Launch the Full Application?
   You only need a full application launch test if you are verifying OS-level desktop integration:
   Multiple window spawning / closing logic.
   Desktop menu bars (MenuBar { Menu(...) }).
   System tray integration (Tray(...)).
   Window resizing, minimize-to-tray, or multi-window state synchronization.
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   4/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   How to Write a Full Application Launch Test
   If you need an E2E test that verifies the full application lifecycle and window setup, you can launch awaitApplication inside a separate thread or
   use Compose Desktop's window testing capabilities:
   package com.example.app
   import androidx.compose.ui.awt.ComposeWindow
   import androidx.compose.ui.window.Window
   import androidx.compose.ui.window.application
   import kotlinx.coroutines.*
   import org.junit.Test
   import java.awt.GraphicsEnvironment
   import org.junit.Assume.assumeFalse
   import kotlin.test.assertTrue
   class DesktopApplicationE2ETest {
   @Test
   fun testFullApplicationLaunch() = runBlocking {
   // Skip on headless CI servers without a virtual frame buffer (Xvfb)
   assumeFalse(
   "Skipping full window test in headless environment",
   GraphicsEnvironment.isHeadless()
   )
   var windowRef: ComposeWindow? = null
   val appJob = launch(Dispatchers.Default) {
   application(exitProcessOnExit = false) {
   Window(
   onCloseRequest = ::exitApplication,
   title = "Test App"
   ) {
   windowRef = window
   App() // Your root App composable
   }
   }
   }
   // Wait briefly for the window to initialize
   withTimeout(5000) {
   while (windowRef == null || !windowRef!!.isVisible) {
   delay(50)
   }
   }
   // Assert native window properties
   assertTrue(windowRef!!.isShowing)
   kotlin.test.assertEquals("Test App", windowRef!!.title)
   // Clean up and close the window
   withContext(Dispatchers.Main) {
   windowRef?.dispose()
   }
   appJob.cancelAndJoin()
   }
   }
   Which Approach to Choose?
   Use createComposeRule() / runComposeUiTest for 95% of tests: Testing buttons, user input, state transitions, dialogs, form validation,
   and screen navigation.
   Use full application launch only for E2E: Testing window lifecycle, OS file pickers, tray icons, and menu bars.
   User prompt: So currently i have a kmp app, with andorid and desktop target in androidTest i have a test like: package
   com.philkes.notallyx.activity.edit import android.Manifest import android.app.Activity import android.app.Instrumentation import
   android.content.ClipData import android.content.Intent import androidx.compose.ui.semantics.SemanticsActions import
   androidx.compose.ui.test.SemanticsMatcher import androidx.compose.ui.test.assert import androidx.compose.ui.test.assertCountEquals import
   androidx.compose.ui.test.assertIsDisplayed import androidx.compose.ui.test.assertIsEnabled import androidx.compose.ui.test.assertIsNotEnabled
   import androidx.compose.ui.test.assertIsSelected import androidx.compose.ui.test.assertTextEquals import
   androidx.compose.ui.test.hasAnyAncestor import androidx.compose.ui.test.hasClickAction import androidx.compose.ui.test.hasContentDescription
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   5/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   import androidx.compose.ui.test.hasScrollAction import androidx.compose.ui.test.hasSetTextAction import androidx.compose.ui.test.hasText import
   androidx.compose.ui.test.isDialog as isComposeDialog import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule import
   androidx.compose.ui.test.longClick import androidx.compose.ui.test.onNodeWithContentDescription import
   androidx.compose.ui.test.onNodeWithText import androidx.compose.ui.test.performClick import androidx.compose.ui.test.performSemanticsAction
   import androidx.compose.ui.test.performTextInput import androidx.compose.ui.test.performTextInputSelection import
   androidx.compose.ui.test.performTextReplacement import androidx.compose.ui.test.performTouchInput import androidx.compose.ui.test.swipeLeft
   import androidx.compose.ui.test.swipeRight import androidx.compose.ui.text.TextRange import androidx.core.content.IntentCompat import
   androidx.test.core.app.ActivityScenario import androidx.test.espresso.Espresso import androidx.test.espresso.intent.Intents import
   androidx.test.espresso.intent.Intents.intended import androidx.test.espresso.intent.Intents.intending import
   androidx.test.espresso.intent.matcher.IntentMatchers.hasAction import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra import
   androidx.test.espresso.intent.matcher.IntentMatchers.hasType import androidx.test.ext.junit.runners.AndroidJUnit4 import
   androidx.test.filters.LargeTest import androidx.test.platform.app.InstrumentationRegistry import androidx.test.uiautomator.UiDevice import
   com.philkes.notallyx.R import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory import
   com.philkes.notallyx.core.common.extensions.getTempAudioFile import com.philkes.notallyx.core.common.extensions.getUriForFile import
   com.philkes.notallyx.core.common.extensions.putNavKey import com.philkes.notallyx.data.model.BaseNote import
   com.philkes.notallyx.data.model.Color import com.philkes.notallyx.data.model.Label import com.philkes.notallyx.data.model.Type import
   com.philkes.notallyx.data.model.toColorString import com.philkes.notallyx.feature.main.MainActivity import
   com.philkes.notallyx.feature.migration.ExportMimeType import com.philkes.notallyx.feature.reminder.PinnedNotificationManager import
   com.philkes.notallyx.navigation.NotallyNavKey import com.philkes.notallyx.test.UiTestBase import com.philkes.notallyx.test.appendText import
   com.philkes.notallyx.test.assertSubtextHasLink import com.philkes.notallyx.test.assertSubtextHasNoFormatting import
   com.philkes.notallyx.test.assertSubtextHasStrikethrough import com.philkes.notallyx.test.assertSubtextIsBold import
   com.philkes.notallyx.test.assertSubtextIsItalic import com.philkes.notallyx.test.assertSubtextIsMonospace import
   com.philkes.notallyx.test.createBaseNote import com.philkes.notallyx.test.navigateTo import com.philkes.notallyx.test.performClickOnSubtext import
   com.philkes.notallyx.test.waitUntil import java.io.File import kotlinx.coroutines.runBlocking import org.hamcrest.Matchers.allOf import
   org.json.JSONObject import org.junit.Assert.assertEquals import org.junit.Assert.assertTrue import org.junit.FixMethodOrder import org.junit.Rule
   import org.junit.Test import org.junit.runner.RunWith @LargeTest @RunWith(AndroidJUnit4::class) @FixMethodOrder class EditRouteTest :
   UiTestBase() { @get:Rule val composeTestRule = createEmptyComposeRule() @Test fun createAndEditTextNote() { val scenario =
   ActivityScenario.launch(MainActivity::class.java) val takeNoteLabel = context.getString(R.string.take_note) val titleLabel =
   context.getString(R.string.title) val bodyLabel = context.getString(R.string.note) val saveLabel = context.getString(R.string.save) val addLabel =
   context.getString(R.string.add_label) val backLabel = context.getString(R.string.back)
   composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()
   composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("Body")
   composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Test")
   composeTestRule.onNodeWithContentDescription(context.getString(R.string.pin)).performClick() composeTestRule
   .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options)) .performClick()
   composeTestRule.onNodeWithText(context.getString(R.string.change_color)).performClick()
   composeTestRule.onNodeWithContentDescription(BaseNote.COLOR_NEW).performClick()
   composeTestRule.onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
   composeTestRule.onNodeWithText(saveLabel).performClick() composeTestRule
   .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options)) .performClick() val labelNodes =
   composeTestRule.onAllNodes(hasText(context.getString(R.string.labels))) - labelNodes[labelNodes.fetchSemanticsNodes().size 1].performClick()
   composeTestRule.onNodeWithContentDescription(addLabel).performClick()
   composeTestRule.onNode(hasSetTextAction()).performTextInput("label") composeTestRule.onNodeWithText(saveLabel).performClick()
   composeTestRule.waitUntil(10_000) { composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().isNotEmpty() }
   composeTestRule.onNodeWithContentDescription(backLabel).performClick() composeTestRule.waitUntil(10_000) {
   composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().isNotEmpty() } composeTestRule.onAllNodes(hasText("label"))
   [0].assertIsDisplayed() composeTestRule .onNodeWithContentDescription(context.getString(R.string.back)) .performClick()
   composeTestRule.onNodeWithText("Test").assertIsDisplayed() scenario.close() } Can this somehow be used with desktop as well ? or do i need to
   write separate test for dekstop
   Response: You can reuse the vast majority of this test for Desktop, but not in its current form because it is tightly coupled to Android runtime
   APIs.
   To make it cross-platform (running in both Android and Desktop without duplicating test code), you move the test into commonTest by decoupling
   three Android-specific dependencies:
1. ActivityScenario.launch(MainActivity::class.java) (Desktop has no Activity)
2. context.getString(R.string...) (Android-only resource access)
3. createEmptyComposeRule() / Android test runner
   Step 1: Replace ActivityScenario with Composable Entry Point
   On Desktop, your root UI is just a @Composable function (usually App() or MainScreen() containing your navigation host).
   Instead of launching the host activity, you launch your root composable inside the test harness. If MainActivity initializes dependency injection
   (like Koin) or databases, set those up in test fixtures rather than relying on Android lifecycle hooks.
   Step 2: Decouple String Resources
   Using context.getString(R.string.x) binds your test to Android Context. If you use Compose Multiplatform's resource library
   (compose.components.resources), strings can be accessed synchronously or resolved via shared resource identifiers:
   // Using Compose Multiplatform resources:
   import your.app.generated.resources.Res
   import your.app.generated.resources.*
   import org.jetbrains.compose.resources.getString
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   6/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   // In test:
   val takeNoteLabel = runBlocking { getString(Res.string.take_note) }
   Alternatively, use stable, platform-agnostic Modifier.testTag(...) instead of localized content descriptions. testTag makes tests independent
   of language translations and platform resource loaders:
   composeTestRule.onNodeWithTag("btn_take_note").performClick()
   Step 3: Use runComposeUiTest in commonTest
   Compose Multiplatform provides runComposeUiTest, which executes identically in commonTest for both Android and Desktop without needing
   platform-specific rules or runners.
   Move your test into src/commonTest/kotlin/...:
   package com.philkes.notallyx.feature.edit
   import androidx.compose.ui.test.*
   import kotlin.test.Test
   @OptIn(ExperimentalTestApi::class)
   class EditRouteCommonTest {
   @Test
   fun createAndEditTextNote() = runComposeUiTest {
   // 1. Launch your shared root UI instead of ActivityScenario
   setContent {
   NotallyApp() // Your root App composable containing navigation
   }
   // 2. Perform the exact same interaction sequence
   onNodeWithContentDescription("Take note").performClick()
   onNodeWithContentDescription("Note").performTextInput("Body")
   onNodeWithContentDescription("Title").performTextInput("Test")
   onNodeWithContentDescription("Pin").performClick()
   onNodeWithContentDescription("More options").performClick()
   onNodeWithText("Change color").performClick()
   onNodeWithContentDescription("Coral").performClick()
   onNodeWithText("Save").performClick()
   onNodeWithContentDescription("More options").performClick()
   onAllNodesWithText("Labels").onLast().performClick()
   onNodeWithContentDescription("Add label").performClick()
   onNode(hasSetTextAction()).performTextInput("label")
   onNodeWithText("Save").performClick()
   waitUntil(timeoutMillis = 5_000) {
   onAllNodesWithText("label").fetchSemanticsNodes().isNotEmpty()
   }
   onNodeWithContentDescription("Back").performClick()
   waitUntil(timeoutMillis = 5_000) {
   onAllNodesWithText("label").fetchSemanticsNodes().isNotEmpty()
   }
   onAllNodesWithText("label")[0].assertIsDisplayed()
   onNodeWithContentDescription("Back").performClick()
   onNodeWithText("Test").assertIsDisplayed()
   }
   }
   What Stays in androidTest?
   You only keep tests in androidTest if they directly assert Android-specific platform services present in your imports:
   UiDevice (system-level hardware buttons, notifications bar)
   Intents.intended(...) / ActivityScenario (Android intent filters, share sheets)
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   7/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   PinnedNotificationManager / Android system alarms
   Android runtime permissions (Manifest.permission.*)
   Everything driving the Compose hierarchy—clicking, text input, navigation between routes, and UI assertions—can run directly on Desktop via
   commonTest.
   User prompt: in comparison to androidtest, what kind of tests will be in desktop test?
   Response: In a Kotlin Multiplatform app, the division between androidTest and desktopTest (or jvmTest) comes down to operating system
   boundaries and hardware interaction:
   androidTest verifies integration with the Android OS ecosystem (Activities, Context, System Services, hardware sensors, Android IPC).
   desktopTest verifies integration with the Host Desktop OS environment (Windows, macOS, Linux APIs, desktop window management,
   standard JVM capabilities).
   If you move all shared business logic, navigation flows, and general Compose UI interactions into commonTest, here is what specifically belongs in
   desktopTest:
1. Window Management & Desktop Chrome
   Desktop apps have window features that do not exist on Android (which handles full-screen or split-screen via Activities). Tests in desktopTest
   verify:
   Window lifecycle: Verifying window opening, closing, minimize-to-tray, and exit-on-close policies.
   Window properties: Testing state persistence for window size, position, fullscreen toggling, and multi-monitor placement.
   Multi-window flows: Verifying scenarios where a user opens an auxiliary window (e.g., Detached Note viewer, Settings window, or inspector
   dialog).
   // Example: Verifying desktop window state handling
   @Test
   fun testWindowStatePersistence() {
   val windowState = WindowState(size = DpSize(800.dp, 600.dp))
   val tracker = DesktopWindowTracker(windowState)
   tracker.saveDimensions()
   assertEquals(800, preferences.getInt("window_width"))
   }
2. Desktop OS Shell Integration (Menu Bar & System Tray)
   Desktop apps interface with native OS menus and system indicators:
   Native OS Menu Bar (MenuBar): Verifying top-level application menus (e.g., File -> Export, Edit -> Select All) and nested
   submenus.
   System Tray / Notification Area (Tray): Testing tray icon creation, tray menus, clicking tray icons to bring the app to focus, or minimizing to
   the background.
   Context Menus: Right-click context menus (ContextMenuArea) triggered by mouse secondary buttons.
3. Keyboard Shortcuts & Mouse/Pointer Semantics
   While touch interactions (performTouchInput, swipeLeft, longClick) are standard on Android, desktop apps rely heavily on pointer-and-
   keyboard navigation:
   Global Hotkeys & Accelerators: Testing key combos like Ctrl+N / Cmd+N (New note), Ctrl+S (Save), or Esc (Dismiss/Back).
   Mouse Interactions: Hover states (pointerInput, cursor changes), mouse wheel scrolling, right-click, double-click to edit, and click-drag
   selections.
   // Example: Simulating desktop keyboard shortcuts
   @OptIn(ExperimentalTestApi::class)
   @Test
   fun testSaveShortcutTriggersAction() = runComposeUiTest {
   setContent { NoteEditorScreen(viewModel) }
   // Simulate Ctrl+S / Cmd+S on desktop
   onRoot().performKeyInput {
   keyDown(Key.CtrlLeft)
   keyDown(Key.S)
   keyUp(Key.S)
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   8/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   keyUp(Key.CtrlLeft)
   }
   assertTrue(viewModel.isSaved.value)
   }
4. Desktop File System & Native Dialogs
   Android accesses files through content URIs (android.net.Uri), Scoped Storage, and ActivityResultContracts. Desktop operates on
   regular JVM java.io.File / java.nio.file.Path:
   Local Path Resolution: Verifying app directories across OS distributions (~/.config/ or AppData/Roaming/ or
   ~/Library/Application Support/).
   Native File Choosers: Verifying file picker abstractions (AWT FileDialog or platform bridges) when importing/exporting markdown, text, or
   database files.
   OS File Associations / CLI Arguments: Testing how the app processes startup flags or command-line arguments (e.g., launching via
   terminal: notallyx --open path/to/note.json).
5. Desktop-Specific actual Implementations
   Any expect declaration in your common code that requires a desktop JVM implementation needs to be verified in desktopTest:
   System Clipboard: Desktop-specific clipboard handling (java.awt.datatransfer.Clipboard) vs. Android ClipboardManager.
   Audio / Media: If recording or playback relies on JVM libraries (e.g., Java Sound API / ALSA / CoreAudio wrapper) rather than Android
   MediaPlayer / MediaRecorder.
   Notifications: Desktop libnotify / system toast integrations vs. Android NotificationManager.
   Summary Comparison
   CapabilityandroidTestdesktopTestcommonTest
   UI Semantics &
   NavigationOnly if using Android Activity APIsOnly if using desktop window APIsDefault place (70–80% of
   tests)
   System LifecycleActivity recreation, process death,
   backgroundingApp exit, tray minimize, window closingFlow/State lifecycle
   File I/OScoped storage, Content Resolvers, SAFStandard JVM paths, AppData, file
   permissionsIn-memory fakes &
   repositories
   User InputTouch gestures, multi-touch pinch, virtual
   IMEHotkeys (Ctrl/Cmd), right-click, hoverText typing, clicks
   Platform IntegrationBroadcastReceivers, Alarms, Intents, App
   WidgetsMenuBar, System Tray, CLI argumentsPlatform-agnostic interfaces
   User prompt: but how to write a desktop test that is similar to an andoridtest that uses test acttivity scenario? Whats the equivalent for dekstop test?
   Response: The direct equivalent to ActivityScenario.launch(MainActivity::class.java) on Desktop is launching your root Desktop
   entry point (main() or App()) inside runComposeUiTest.
   On Android, an Activity is mandatory because the OS requires a host container to attach the Compose view tree and hook into OS lifecycle
   callbacks. On Desktop, there is no Activity or OS container requirement—the top-level entry point is simply a @Composable function that
   initializes dependencies and hosts your navigation graph.
   Here is how your EditRouteTest translates directly into a Desktop test, how to handle the app-level lifecycle equivalent of ActivityScenario,
   and how to address teardown.
   The Direct Desktop Translation
   In your desktop test source set (desktopTest or jvmTest), you use runComposeUiTest. Instead of launching MainActivity, you call the root
   setup and composable:
   package com.philkes.notallyx.feature.edit
   import androidx.compose.ui.test.*
   import kotlin.test.AfterTest
   import kotlin.test.BeforeTest
   import kotlin.test.Test
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   9/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   @OptIn(ExperimentalTestApi::class)
   class EditRouteDesktopTest {
   @BeforeTest
   fun setUp() {
   // Equivalent to Application.onCreate() or whatever your MainActivity setup does:
   // 1. Initialize dependency injection (e.g. Koin/Kodein test modules)
   // 2. Initialize in-memory database (e.g. Room/SQLDelight)
   }
   @AfterTest
   fun tearDown() {
   // Equivalent to scenario.close() - clear DI state, close DBs, wipe temp dirs
   }
   @Test
   fun createAndEditTextNote() = runComposeUiTest {
   // Equivalent to ActivityScenario.launch(MainActivity::class.java)
   setContent {
   // Your desktop root composable (the one called inside Window { ... })
   App()
   }
   // --- Exact same Compose interaction assertions ---
   onNodeWithContentDescription("Take note").performClick()
   onNodeWithContentDescription("Note").performTextInput("Body")
   onNodeWithContentDescription("Title").performTextInput("Test")
   onNodeWithContentDescription("Pin").performClick()
   onNodeWithContentDescription("Tap for more options").performClick()
   onNodeWithText("Change color").performClick()
   onNodeWithContentDescription("Coral").performClick()
   onNodeWithText("Save").performClick()
   onNodeWithContentDescription("Tap for more options").performClick()
   onAllNodesWithText("Labels").onLast().performClick()
   onNodeWithContentDescription("Add label").performClick()
   onNode(hasSetTextAction()).performTextInput("label")
   onNodeWithText("Save").performClick()
   waitUntil(10_000) {
   onAllNodesWithText("label").fetchSemanticsNodes().isNotEmpty()
   }
   onNodeWithContentDescription("Back").performClick()
   waitUntil(10_000) {
   onAllNodesWithText("label").fetchSemanticsNodes().isNotEmpty()
   }
   onAllNodesWithText("label")[0].assertIsDisplayed()
   onNodeWithContentDescription("Back").performClick()
   onNodeWithText("Test").assertIsDisplayed()
   // No scenario.close() needed: runComposeUiTest tears down
   // the composition tree automatically when this block finishes.
   }
   }
   Side-by-Side: Android ActivityScenario vs Desktop Equivalent
   Android (ActivityScenario)Desktop Equivalent
   ActivityScenario.launch(MainActivity::class.java)runComposeUiTest { setContent { App() } }
   context.getString(R.string.x)getString(Res.string.x) or stable onNodeWithTag(...)
   scenario.onActivity { ... }Direct access in test class or via DI container (e.g., koin.get())
   https://gemini.google.com/app/961e3fbca7297e2d?hl=de
   10/1711/6/26, 2:43 PM
   KMP Project Testing Best Practices
   Android (ActivityScenario)Desktop Equivalent
   scenario.recreate()Update state / reset composition inside setContent
   scenario.close()Executed automatically on exit of runComposeUiTest (plus @AfterTest)
   What if MainActivity Does Critical Initialization?
   In Android apps, MainActivity often runs initialization logic (setting up Koin modules, initializing databases, setting up navigation observers). On
   Desktop, that code normally lives in your main() function:
   // desktopMain/kotlin/Main.kt
   fun main() {
   initKoin()
   application {
   Window(onCloseRequest = ::exitApplication) {
   App()
   }
   }
   }
   To make your test scenario equivalent:
1. Extract your app startup logic into a reusable setup function (e.g., initDesktopApp(isTest = true)).
2. Call it inside @BeforeTest.
3. In setContent { App() }, pass your test dependencies or let your DI framework supply in-memory test doubles.
   What if You Genuinely Need a Real OS Window (AWT / Swing)?
   If you want an end-to-end test that specifically interacts with the physical OS window wrapper (not just the Compose tree inside it), Compose
   Desktop allows hosting the native window via createComposeRule():
   import androidx.compose.ui.test.junit4.createComposeRule
   import org.junit.Rule
   import org.junit.Test
   class EditRouteWindowDesktopTest {
   @get:Rule
   val composeRule = createComposeRule()
   @Test
   fun testAppInWindow() {
   // Boots the full Compose UI within the desktop test rule
   composeRule.setContent {
   App()
   }
   composeRule.onNodeWithContentDescription("Take note").performClick()
   composeRule.onNodeWithText("Save").assertIsDisplayed()
   }
   }
   Note: createComposeRule() (JUnit4) and runComposeUiTest use the identical finding and assertion syntax (onNodeWith...,
   performClick(), waitUntil), making your test logic 100% familiar.
   Sources