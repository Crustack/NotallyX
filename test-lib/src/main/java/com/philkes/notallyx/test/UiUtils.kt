package com.philkes.notallyx.test

import android.graphics.Point
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cn.leaqi.drawer.SwipeDrawer
import com.philkes.notallyx.R
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.navigation.NotallyNavKey
import junit.framework.TestCase.assertTrue
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import java.security.SecureRandom

fun onLabelItem(labelText: String): ViewInteraction =
    onView(
        allOf(
            withId(R.id.LabelText),
            withText(labelText),
            withParent(withParent(withId(R.id.MainListView))),
            isDisplayed(),
        )
    )

fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {

    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Child at position $position in parent ")
            parentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            val parent = view.parent
            return parent is ViewGroup &&
                parentMatcher.matches(parent) &&
                view == parent.getChildAt(position)
        }
    }
}

fun ViewInteraction.checkIsPositionChildItem(position: Int, isChild: Boolean = true) =
    check(
        matches(
            atPosition(
                position = position,
                itemMatcher =
                    allOf(hasDescendant(if (isChild) isDrawerOpen() else isDrawerClosed())),
            )
        )
    )

/**
 * Checks whether the item containing [text] matches the expected [checked] state, with optional
 * assertions for list [position] and child indentation ([isChild]).
 *
 * @param text The text inside the item's EditText
 * @param checked Expected state of the CheckBox (default: true)
 * @param position Optional adapter position index to verify list order
 * @param isChild Optional flag to check if item is indented (true = VISIBLE, false =
 *   GONE/INVISIBLE)
 */
fun ViewInteraction.checkListItem(
    position: Int,
    text: String,
    checked: Boolean? = null,
    isChild: Boolean? = null,
): ViewInteraction {
    checkPositionHasText(position, text)
    checked?.let { this.checkIsPositionItemChecked(position, it) }
    return this
}

fun ViewInteraction.checkIsPositionItemChecked(position: Int, checked: Boolean = true) =
    check(
        matches(
            atPosition(
                position = position,
                itemMatcher =
                    hasDescendant(
                        allOf(withId(R.id.CheckBox), if (checked) isChecked() else isNotChecked())
                    ),
            )
        )
    )

fun ViewInteraction.checkPositionHasText(position: Int, text: String) =
    check(
        matches(atPosition(position = position, itemMatcher = allOf(hasDescendant(withText(text)))))
    )

// Custom matcher to target a specific position in a RecyclerView
fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(recyclerView: RecyclerView): Boolean {
            val viewHolder =
                recyclerView.findViewHolderForAdapterPosition(position)
                    ?: return false // Returns false if position doesn't exist or isn't bound
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}

/**
 * Drags the receiver [SemanticsNodeInteraction] to a position relative to [targetNode].
 *
 * @param targetNode The destination node reference.
 * @param verticalOffset Extra spacing in pixels to place the drop point beyond the target's edge (default: 20px).
 * @param holdDurationMillis Time to hold before dragging (required for long-press drag detectors; default: 600ms).
 * @param dragDurationMillis Total duration over which the drag motion occurs (default: 500ms).
 * @param steps Number of intermediate steps for the drag motion (default: 30).
 * @param settleDelayMillis Time to hold at the destination before releasing (default: 200ms).
 */
fun SemanticsNodeInteraction.dragAndDropTo(
    targetNode: SemanticsNodeInteraction,
    verticalOffset: Float = 20f,
    holdDurationMillis: Long = 600L,
    dragDurationMillis: Long = 200L,
    steps: Int = 30,
    settleDelayMillis: Long = 200L,
): SemanticsNodeInteraction {
    val sourceBounds = fetchSemanticsNode().boundsInRoot
    val targetBounds = targetNode.fetchSemanticsNode().boundsInRoot

    val sourceCenter = sourceBounds.center

    val isDraggingDown = targetBounds.center.y > sourceCenter.y
    val targetY =
        if (isDraggingDown) {
            targetBounds.bottom + verticalOffset
        } else {
            targetBounds.top - verticalOffset
        }

    val destinationPoint = Offset(x = sourceCenter.x, y = targetY)

    val totalDelta = destinationPoint - sourceCenter
    val stepDelta = totalDelta / steps.toFloat()
    val stepDurationMillis = if (steps > 0) dragDurationMillis / steps else 0L

    performTouchInput { down(center) }
    fetchSemanticsNode()

    if (holdDurationMillis > 0) {
        val holdSteps = 6
        val holdStepDuration = holdDurationMillis / holdSteps
        repeat(holdSteps) {
            performTouchInput { moveBy(Offset.Zero, delayMillis = holdStepDuration) }
            fetchSemanticsNode()
            if (holdStepDuration > 0) {
                SystemClock.sleep(holdStepDuration)
            }
        }
    }

    repeat(steps) {
        performTouchInput { moveBy(stepDelta, delayMillis = stepDurationMillis) }
        fetchSemanticsNode()
        if (stepDurationMillis > 0) {
            SystemClock.sleep(stepDurationMillis)
        }
    }

    if (settleDelayMillis > 0) {
        performTouchInput { moveBy(Offset.Zero, delayMillis = settleDelayMillis) }
        fetchSemanticsNode()
        SystemClock.sleep(settleDelayMillis)
    }

    performTouchInput { up() }
    fetchSemanticsNode()

    return this
}

/**
 * Finds the parent item views matching [sourceSelector] and [targetSelector], extracts their
 * sibling @+id/DragHandle views, and delegates to [dragAndDrop].
 *
 * @param sourceSelector Selector matching text or properties inside the source item's EditText
 * @param targetSelector Selector matching text or properties inside the target item's EditText
 * @param dragSpeedMs Duration of the drag movement in milliseconds
 * @param dragHandleResId Resource ID of the handle (defaults to "DragHandle")
 * @param packageName App package name
 */
fun dragAndDropByDragHandle(
    sourceSelector: BySelector,
    targetSelector: BySelector,
    dragSpeedMs: Int = 500,
    dragHandleDesc: String = "Drag Handle",
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    val sourceTextObject =
        device.wait(Until.findObject(sourceSelector), 5000)
            ?: throw IllegalStateException("Source element not found: $sourceSelector")

    val targetTextObject =
        device.wait(Until.findObject(targetSelector), 5000)
            ?: throw IllegalStateException("Target element not found: $targetSelector")

    val sourceHandleObject = findDragHandleForNode(sourceTextObject, dragHandleDesc)
    val targetHandleObject = findDragHandleForNode(targetTextObject, dragHandleDesc)

    dragAndDrop(
        sourceObject = sourceHandleObject,
        targetObject = targetHandleObject,
        dragSpeedMs = dragSpeedMs,
    )
}

private fun findDragHandleForNode(node: UiObject2, dragHandleDesc: String): UiObject2 {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val nodeBounds = node.visibleBounds
    val handles = device.findObjects(By.desc(dragHandleDesc))
    return handles.find { handle ->
        val handleBounds = handle.visibleBounds
        kotlin.math.abs(handleBounds.centerY() - nodeBounds.centerY()) < nodeBounds.height()
    } ?: throw IllegalStateException("Drag handle not found for item node with bounds $nodeBounds")
}

/**
 * Performs a drag-and-drop gesture using UiAutomator [BySelector] inputs.
 *
 * Automatically calculates start coordinates, target coordinates, and extends the drop point past
 * the center of the target item to trigger [ItemTouchHelper] reordering.
 *
 * @param sourceSelector Selector matching the item/handle to drag (e.g. By.text("Item 1"))
 * @param targetSelector Selector matching the item to drop onto (e.g. By.text("Item 3"))
 * @param dragSpeedMs Total duration of the drag motion in milliseconds (default 1200ms)
 */
fun dragAndDrop(
    sourceSelector: BySelector,
    targetSelector: BySelector,
    dragSpeedMs: Int = 300,
    longClickBefore: Boolean = false,
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val sourceObject =
        device.wait(Until.findObject(sourceSelector), 5000)
            ?: throw IllegalStateException("Source element not found: $sourceSelector")
    val targetObject =
        device.wait(Until.findObject(targetSelector), 5000)
            ?: throw IllegalStateException("Target element not found: $targetSelector")
    return dragAndDrop(sourceObject, targetObject, dragSpeedMs, longClickBefore)
}

fun dragAndDrop(
    sourceObject: UiObject2,
    targetObject: UiObject2,
    dragSpeedMs: Int = 300,
    longClickBefore: Boolean = false,
) {
    val sourceBounds = sourceObject.visibleBounds
    val targetBounds = targetObject.visibleBounds

    val isDraggingDown = targetBounds.centerY() > sourceBounds.centerY()
    val endX = targetBounds.centerX()
    val endY =
        if (isDraggingDown) {
            targetBounds.bottom + (targetBounds.height() * 0.25).toInt()
        } else {
            targetBounds.top - (targetBounds.height() * 0.25).toInt()
        }

    val dropPoint = Point(endX, endY)
    sourceObject.drag(dropPoint, dragSpeedMs)
    SystemClock.sleep(300)
}

fun performLongPressDrag(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    holdTimeMs: Long = 650L,
    dragSteps: Int = 50,
) {
    val downTime = SystemClock.uptimeMillis()

    var eventTime = SystemClock.uptimeMillis()
    val downEvent =
        MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            startX.toFloat(),
            startY.toFloat(),
            0,
        )
    InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(downEvent, true)
    downEvent.recycle()

    SystemClock.sleep(holdTimeMs)

    for (i in 1..dragSteps) {
        eventTime = SystemClock.uptimeMillis()
        val x = startX + (endX - startX) * i / dragSteps
        val y = startY + (endY - startY) * i / dragSteps
        val moveEvent =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                x.toFloat(),
                y.toFloat(),
                0,
            )
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .injectInputEvent(moveEvent, true)
        moveEvent.recycle()
        SystemClock.sleep(10)
    }

    eventTime = SystemClock.uptimeMillis()
    val upEvent =
        MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_UP,
            endX.toFloat(),
            endY.toFloat(),
            0,
        )
    InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(upEvent, true)
    upEvent.recycle()

    SystemClock.sleep(300)
}

fun swipeItem(
    itemSelector: BySelector,
    direction: Direction = Direction.LEFT,
    percent: Float = 0.8f,
    speed: Float = 0.8f,
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    val itemContainer =
        device.wait(Until.findObject(itemSelector), 5000)
            ?: throw IllegalStateException("Item container matching $itemSelector not found")

    itemContainer.swipe(direction, percent, (speed * 1000).toInt())

    SystemClock.sleep(300)
}

/**
 * Finds the item containing [text] in its EditText and sets its CheckBox to the target [checked]
 * state if it is not already in that state.
 *
 * @param text The string text inside the item's EditText
 * @param checked The desired state of the CheckBox (default: true)
 */
fun ViewInteraction.performListItemCheck(text: String, checked: Boolean = true): ViewInteraction {
    val targetCheckBox =
        onView(
            allOf(
                withId(R.id.CheckBox),
                isDescendantOfA(
                    allOf(
                        withId(R.id.SwipeLayout),
                        hasDescendant(allOf(withId(R.id.EditText), withText(text))),
                    )
                ),
            )
        )

    val currentMatcher = if (checked) isNotChecked() else isChecked()

    try {
        targetCheckBox.check(matches(currentMatcher)).perform(click())
    } catch (e: AssertionError) {
        // Already in desired state
    }

    return this
}

/**
 * Returns a ViewInteraction targeting a specific child view matching [childMatcher] inside the
 * RecyclerView item at [position].
 */
fun ViewInteraction.onPositionView(
    position: Int,
    childMatcher: Matcher<View>? = null,
): ViewInteraction {
    var itemContainer: View? = null

    this.check { view, _ ->
        if (view !is RecyclerView) {
            throw IllegalArgumentException(
                "onPositionView must be called on a RecyclerView ViewInteraction."
            )
        }

        val viewHolder =
            view.findViewHolderForAdapterPosition(position)
                ?: throw AssertionError(
                    "No ViewHolder found at position $position in RecyclerView."
                )

        itemContainer = viewHolder.itemView
    }

    val targetItem =
        itemContainer
            ?: throw IllegalStateException("Could not resolve itemView for position $position.")
    return onView(allOf(childMatcher, isDescendantOfA(org.hamcrest.Matchers.`is`(targetItem))))
}

/** Matches a SwipeDrawer that is currently open in the specified direction. */
fun isDrawerOpen(): Matcher<View> {
    return object : BoundedMatcher<View, SwipeDrawer>(SwipeDrawer::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("is SwipeDrawer open")
        }

        override fun matchesSafely(swipeDrawer: SwipeDrawer): Boolean {
            Log.e(
                "UiUtils",
                "isDrawerOpen: ${swipeDrawer.isShown}, direction: ${swipeDrawer.direction}, leftdragOpen: ${swipeDrawer.leftDragOpen}, rightDragOpen: ${swipeDrawer.rightDragOpen}",
            )

            return swipeDrawer.isShown
        }
    }
}

/** Matches a SwipeDrawer that is fully closed. */
fun isDrawerClosed(): Matcher<View> {
    return object : BoundedMatcher<View, SwipeDrawer>(SwipeDrawer::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("is SwipeDrawer closed")
        }

        override fun matchesSafely(swipeDrawer: SwipeDrawer): Boolean {
            Log.e(
                "UiUtils",
                "isDrawerOpen: ${swipeDrawer.isShown}, direction: ${swipeDrawer.direction}, leftdragOpen: ${swipeDrawer.leftDragOpen}, rightDragOpen: ${swipeDrawer.rightDragOpen}",
            )
            return !swipeDrawer.isShown
        }
    }
}

fun createBaseNote(
    id: Long = 0L,
    type: Type = Type.NOTE,
    folder: Folder = Folder.NOTES,
    color: String = BaseNote.COLOR_DEFAULT,
    title: String = "Note",
    pinned: Boolean = false,
    isPinnedToStatus: Boolean = false,
    timestamp: Long = System.currentTimeMillis(),
    modifiedTimestamp: Long = System.currentTimeMillis(),
    labels: List<String> = listOf(),
    body: String = "",
    spans: List<SpanRepresentation> = listOf(),
    items: List<ListItem> = listOf(),
    images: List<FileAttachment> = listOf(),
    files: List<FileAttachment> = listOf(),
    audios: List<Audio> = listOf(),
    reminders: List<Reminder> = listOf(),
): BaseNote {
    return BaseNote(
        id,
        type,
        folder,
        color,
        title,
        pinned,
        timestamp,
        modifiedTimestamp,
        labels,
        body,
        spans,
        items,
        images,
        files,
        audios,
        reminders,
        NoteViewMode.EDIT,
        isPinnedToStatus = isPinnedToStatus,
    )
}

fun onDisplayView(viewMatcher: Matcher<View>) = onView(allOf(viewMatcher, isDisplayed()))

fun navigateTo(navKey: NotallyNavKey) {
    val stringResId =
        when (navKey) {
            is NotallyNavKey.Notes -> R.string.notes
            is NotallyNavKey.Unlabeled -> R.string.unlabeled
            is NotallyNavKey.Labels -> R.string.labels
            is NotallyNavKey.Deleted -> R.string.deleted
            is NotallyNavKey.Archived -> R.string.archived
            is NotallyNavKey.Reminders -> R.string.reminders
            is NotallyNavKey.Settings -> R.string.settings
            else -> error("Unsupported navKey: $navKey")
        }
    navigateTo(stringResId)
}

fun navigateTo(stringResId: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val appName = context.getString(R.string.app_name)
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val menuButton = device.findObject(By.desc(appName))
    if (menuButton != null) {
        menuButton.click()
    } else {
        onView(withContentDescription(appName)).perform(click())
    }
    val targetText = context.getString(stringResId)
    val item = device.findObject(By.text(targetText))
    if (item != null) {
        item.click()
    } else {
        onView(withText(stringResId)).perform(click())
    }
}

fun ComposeTestRule.navigateTo(navKey: NotallyNavKey) {
    val stringResId =
        when (navKey) {
            is NotallyNavKey.Notes -> R.string.notes
            is NotallyNavKey.Unlabeled -> R.string.unlabeled
            is NotallyNavKey.Labels -> R.string.labels
            is NotallyNavKey.Deleted -> R.string.deleted
            is NotallyNavKey.Archived -> R.string.archived
            is NotallyNavKey.Reminders -> R.string.reminders
            is NotallyNavKey.Settings -> R.string.settings
            else -> error("Unsupported navKey: $navKey")
        }
    navigateTo(stringResId)
}

fun ComposeTestRule.navigateTo(stringResId: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithContentDescription(context.getString(R.string.app_name)).performClick()
    onNodeWithText(context.getString(stringResId)).performClick()
}

fun Int.byId(viewMatcher: Matcher<View>? = null, checkDisplayed: Boolean = true): ViewInteraction {
    val matcher = viewMatcher?.let { allOf(withId(this), it) } ?: withId(this)
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

fun Int.byText(
    viewMatcher: Matcher<View>? = null,
    checkDisplayed: Boolean = true,
): ViewInteraction {
    val matcher = viewMatcher?.let { allOf(withText(this), it) } ?: withText(this)
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

fun Int.byContentDescription(
    viewMatcher: Matcher<View>? = null,
    checkDisplayed: Boolean = true,
): ViewInteraction {
    val matcher =
        viewMatcher?.let { allOf(withContentDescription(this), it) } ?: withContentDescription(this)
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

fun String.byText(
    viewMatcher: Matcher<View>? = null,
    checkDisplayed: Boolean = true,
): ViewInteraction {
    val matcher = viewMatcher?.let { allOf(withText(this), it) } ?: withText(this)
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

fun String.byContentDescription(
    viewMatcher: Matcher<View>? = null,
    checkDisplayed: Boolean = true,
): ViewInteraction {
    val matcher =
        viewMatcher?.let { allOf(withContentDescription(this), it) } ?: withContentDescription(this)
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

fun String.byTag(
    viewMatcher: Matcher<View>? = null,
    checkDisplayed: Boolean = true,
): ViewInteraction {
    val tagMatcher = anyOf(withResourceName(this), withTagValue(equalTo(this as Any)))
    val matcher = viewMatcher?.let { allOf(tagMatcher, it) } ?: tagMatcher
    return if (checkDisplayed) onDisplayView(matcher) else onView(matcher)
}

class ToastMatcher : TypeSafeMatcher<Root>() {

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams.get().type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken = root.decorView.windowToken
            val appToken = root.decorView.getApplicationWindowToken()
            if (windowToken === appToken) {
                return true
            }
        }
        return false
    }
}

fun isToast(): TypeSafeMatcher<Root> = ToastMatcher()

fun assertToastDisplayed(textResId: Int, timeoutMs: Long = 10000) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    assertToastDisplayed(context.getString(textResId), timeoutMs)
}

fun assertToastDisplayed(text: String, timeoutMs: Long = 10000) {
    var toastDisplayed = false
    val startTimeMs = System.currentTimeMillis()

    InstrumentationRegistry.getInstrumentation().uiAutomation.setOnAccessibilityEventListener {
        event ->
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val className = event.className?.toString() ?: ""
            val eventText = event.text.toString()

            if (className.contains("android.widget.Toast") && eventText.contains(text)) {
                toastDisplayed = true
            }
        }
    }

    while (!toastDisplayed && System.currentTimeMillis() - startTimeMs < timeoutMs) {
        SystemClock.sleep(100)
    }

    InstrumentationRegistry.getInstrumentation().uiAutomation.setOnAccessibilityEventListener(null)

    assertTrue("Toast with text '$text' not found within ${timeoutMs}ms", toastDisplayed)
}

fun assertLogAppeared(tag: String, expectedMessagePrefix: String, timeoutMs: Long = 10000) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeoutMs) {
        val logcatOutput = device.executeShellCommand("logcat -d -s $tag")
        if (logcatOutput.contains(expectedMessagePrefix)) {
            return
        }
        Thread.sleep(200)
    }

    throw AssertionError(
        "Log with tag '$tag' containing '$expectedMessagePrefix' was not found in Logcat."
    )
}

fun assertWorkExecuted(workName: String, timeoutMs: Long = 5000, pollIntervalMs: Long = 100) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val workManager = WorkManager.getInstance(context)
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeoutMs) {
        val workInfos = workManager.getWorkInfosForUniqueWork(workName).get()
        val workInfo = workInfos.firstOrNull()

        if (workInfo != null) {
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                return
            }

            if (workInfo.state == WorkInfo.State.ENQUEUED && workInfo.runAttemptCount == 0) {
                val hasCompletedPeriod = workInfo.nextScheduleTimeMillis > 0
                if (hasCompletedPeriod) {
                    return
                }
            }
        }

        Thread.sleep(pollIntervalMs)
    }

    val finalState =
        workManager.getWorkInfosForUniqueWork(workName).get().firstOrNull()?.state?.name
            ?: "NOT_SCHEDULED"

    throw AssertionError(
        "Work '$workName' did not complete successfully within ${timeoutMs}ms. Final state: $finalState"
    )
}

fun waitUntilSucceeds(timeoutMs: Long = 5000, interaction: () -> ViewInteraction) {
    val endTime = System.currentTimeMillis() + timeoutMs
    var last: Throwable? = null
    while (System.currentTimeMillis() < endTime) {
        try {
            interaction.invoke()
            return
        } catch (t: Throwable) {
            last = t
        }
        SystemClock.sleep(100)
    }
    throw AssertionError("Condition not met within ${timeoutMs}ms", last)
}

fun waitUntil(timeoutMs: Long = 5000, condition: () -> Boolean) {
    val endTime = System.currentTimeMillis() + timeoutMs
    var last: Throwable? = null
    while (System.currentTimeMillis() < endTime) {
        try {
            if (condition.invoke()) return
        } catch (t: Throwable) {
            last = t
        }
        SystemClock.sleep(100)
    }
    throw AssertionError("Condition not met within ${timeoutMs}ms", last)
}

fun UiTestBase.initFakeBiometric(): FakeBiometricAuthenticator {
    val fakeBiometricAuthenticator = FakeBiometricAuthenticator()
    val randomIv = ByteArray(16)
    SecureRandom().nextBytes(randomIv)
    AuthenticatorProvider.instance = fakeBiometricAuthenticator.apply { iv = randomIv }
    preferences.iv.save(randomIv)
    return fakeBiometricAuthenticator
}

fun ComposeTestRule.checkAndUpdateNoteTitle(textToMatch: String, textToAdd: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    waitUntil(10_000) { onAllNodes(hasText(textToMatch)).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText(textToMatch).performClick()
    onNodeWithContentDescription(context.getString(R.string.title)).appendText(textToAdd)
    onNodeWithContentDescription(context.getString(R.string.back)).performClick()
    waitUntil(10_000) {
        onAllNodes(hasText(textToMatch + textToAdd)).fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText(textToMatch + textToAdd).assertIsDisplayed()
}

fun SemanticsNodeInteraction.appendText(textToAppend: String) {
    val currentText = fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.EditableText)?.text ?: ""
    performTextInputSelection(TextRange(currentText.length))
    performTextInput(textToAppend)
}

fun ComposeTestRule.waitUntilSettingsValue(
    titleResId: Int,
    valueResId: Int,
    timeoutMs: Long = 15_000L,
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    waitUntilSettingsValue(titleResId, context.getString(valueResId), timeoutMs)
}

fun ComposeTestRule.waitUntilSettingsValue(
    titleResId: Int,
    expectedText: String,
    timeoutMs: Long = 15_000L,
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val titleText = context.getString(titleResId)
    val matcher = hasAnyChild(hasText(titleText)) and hasAnyChild(hasText(expectedText))
    val endTime = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < endTime) {
        try {
            if (onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                onNode(matcher, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                return
            }
        } catch (_: Throwable) {
            // Ignore temporary absence of Compose hierarchy during Activity transitions
        }
        SystemClock.sleep(200)
    }
    onNode(matcher, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
}

private val radioButtonMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

fun ComposeTestRule.enableBiometricLock(encryptDb: Boolean = false) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithText(context.getString(R.string.biometric_lock)).performScrollTo().performClick()
    onNode(hasText(context.getString(R.string.enabled)) and radioButtonMatcher).performClick()
    onNodeWithText(context.getString(R.string.save)).performClick()

    val continueText = context.getString(R.string.continue_)
    waitUntil(10_000) { onAllNodes(hasText(continueText)).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText(continueText).performClick()

    if (encryptDb) {
        val encryptText = context.getString(R.string.encrypt_database_via_biometric)
        waitUntil(10_000) { onAllNodes(hasText(encryptText)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(encryptText).performClick()
    }
    onNodeWithText(continueText).performClick()

    val expectedValueResId =
        if (encryptDb) R.string.biometric_lock_enabled_with_encryption else R.string.enabled
    waitUntilSettingsValue(R.string.biometric_lock, expectedValueResId)
}

fun ComposeTestRule.disableBiometricLock() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithText(context.getString(R.string.biometric_lock)).performScrollTo().performClick()
    onNode(hasText(context.getString(R.string.disabled)) and radioButtonMatcher).performClick()
    onNodeWithText(context.getString(R.string.save)).performClick()

    val continueText = context.getString(R.string.continue_)
    waitUntil(10_000) { onAllNodes(hasText(continueText)).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText(continueText).performClick()

    waitUntilSettingsValue(R.string.biometric_lock, R.string.disabled)
}

fun ComposeTestRule.enableDataInPublic(context: android.content.Context) {
    onNodeWithText(context.getString(R.string.data_in_public)).performScrollTo().performClick()
    onNode(hasText(context.getString(R.string.enabled)) and radioButtonMatcher).performClick()
    onNodeWithText(context.getString(R.string.save)).performClick()
    waitUntilSettingsValue(R.string.data_in_public, R.string.enabled)
    val contextWrapper = android.content.ContextWrapper(context)
    waitUntil(15_000L) {
        NotallyDatabase.getCurrentDatabaseFile(contextWrapper) ==
            NotallyDatabase.getExternalDatabaseFile(contextWrapper)
    }
}

fun ComposeTestRule.disableDataInPublic(context: android.content.Context) {
    onNodeWithText(context.getString(R.string.data_in_public)).performScrollTo().performClick()
    onNode(hasText(context.getString(R.string.disabled)) and radioButtonMatcher).performClick()
    onNodeWithText(context.getString(R.string.save)).performClick()
    waitUntilSettingsValue(R.string.data_in_public, R.string.disabled)
    val contextWrapper = android.content.ContextWrapper(context)
    waitUntil(15_000L) {
        NotallyDatabase.getCurrentDatabaseFile(contextWrapper) ==
            NotallyDatabase.getInternalDatabaseFile(contextWrapper)
    }
}

fun lockAndUnlockDevice() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.sleep()
    SystemClock.sleep(2000)
    device.wakeUp()
    device.executeShellCommand("wm dismiss-keyguard")
    device.pressMenu()
}

fun moveCursorToEnd(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(EditText::class.java)
        }

        override fun getDescription(): String {
            return "Move cursor to end of EditText"
        }

        override fun perform(uiController: UiController?, view: View?) {
            val editText = view as EditText
            editText.setSelection(editText.text.length)
        }
    }
}

fun ViewInteraction.typeTextAtEnd(text: String): ViewInteraction {
    return perform(click(), moveCursorToEnd(), typeText(text), closeSoftKeyboard())
}

fun setSelection(start: Int, end: Int): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(EditText::class.java)
        }

        override fun getDescription(): String {
            return "Set selection ($start, $end)"
        }

        override fun perform(uiController: UiController?, view: View?) {
            val editText = view as EditText
            editText.setSelection(start, end)
        }
    }
}

fun hasBoldSpan(start: Int, end: Int): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has bold span from $start to $end")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return false
            val styleSpans = editable.getSpans(0, editable.length, StyleSpan::class.java)
            return styleSpans.any { span ->
                span.style == Typeface.BOLD &&
                    editable.getSpanStart(span) == start &&
                    editable.getSpanEnd(span) == end
            }
        }
    }
}

fun hasItalicSpan(start: Int, end: Int): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has italic span from $start to $end")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return false
            val styleSpans = editable.getSpans(0, editable.length, StyleSpan::class.java)
            return styleSpans.any { span ->
                span.style == Typeface.ITALIC &&
                    editable.getSpanStart(span) == start &&
                    editable.getSpanEnd(span) == end
            }
        }
    }
}

fun hasStrikethroughSpan(start: Int, end: Int): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has strikethrough span from $start to $end")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return false
            val spans = editable.getSpans(0, editable.length, StrikethroughSpan::class.java)
            return spans.any { span ->
                editable.getSpanStart(span) == start && editable.getSpanEnd(span) == end
            }
        }
    }
}

fun hasMonospaceSpan(start: Int, end: Int): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has monospace span from $start to $end")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return false
            val spans = editable.getSpans(0, editable.length, TypefaceSpan::class.java)
            return spans.any { span ->
                span.family == "monospace" &&
                    editable.getSpanStart(span) == start &&
                    editable.getSpanEnd(span) == end
            }
        }
    }
}

fun hasUrlSpan(start: Int, end: Int, url: String? = null): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has url span from $start to $end with url $url")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return false
            val spans = editable.getSpans(0, editable.length, URLSpan::class.java)
            return spans.any { span ->
                (url == null || span.url == url) &&
                    editable.getSpanStart(span) == start &&
                    editable.getSpanEnd(span) == end
            }
        }
    }
}

fun hasNoSpans(start: Int = 0, end: Int = -1): Matcher<View> {
    return object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has no formatting spans")
        }

        override fun matchesSafely(editText: EditText): Boolean {
            val editable = editText.text as? Spanned ?: return true
            val effectiveEnd = if (end == -1) editable.length else end
            val spans =
                editable.getSpans(start, effectiveEnd, Any::class.java).filter {
                    it is StyleSpan ||
                        it is StrikethroughSpan ||
                        it is TypefaceSpan ||
                        it is URLSpan
                }
            return spans.isEmpty()
        }
    }
}

fun waitUntilNoDialogShown(timeoutMs: Long = 5000, pollIntervalMs: Long = 100) {
    val endTime = System.currentTimeMillis() + timeoutMs

    while (System.currentTimeMillis() < endTime) {
        try {
            onView(isRoot()).inRoot(isDialog()).check(matches(not(isDisplayed())))
            Thread.sleep(pollIntervalMs)
        } catch (e: NoMatchingRootException) {
            return
        }
    }

    throw AssertionError("Timed out after ${timeoutMs}ms waiting for dialog window to dismiss.")
}


private fun SpanStyle.hasFormatting(): Boolean {
    return (fontWeight != null && fontWeight!!.weight >= FontWeight.Bold.weight) ||
            fontStyle == FontStyle.Italic ||
            textDecoration?.contains(TextDecoration.LineThrough) == true ||
            fontFamily == FontFamily.Monospace ||
            fontFamily?.toString()?.contains("monospace", ignoreCase = true) == true
}

/**
 * Helper to inspect the AnnotatedString from EditableText or Text semantics.
 */
private fun SemanticsNodeInteraction.assertSubstringStyle(
    description: String,
    substring: String,
    requireFormatting: Boolean = true,
    predicate: (SpanStyle) -> Boolean
): SemanticsNodeInteraction {
    return assert(
        SemanticsMatcher("has substring '$substring' with $description") { node ->
            val annotatedString: AnnotatedString =
                node.config.getOrNull(SemanticsProperties.EditableText)
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
                    ?: return@SemanticsMatcher false

            val text = annotatedString.text
            val substringLength = substring.length
            if (substringLength == 0) return@SemanticsMatcher false

            val startIndices = mutableListOf<Int>()
            var index = text.indexOf(substring)
            while (index != -1) {
                startIndices.add(index)
                index = text.indexOf(substring, index + 1)
            }
            if (startIndices.isEmpty()) return@SemanticsMatcher false

            startIndices.any { startIndex ->
                val endIndex = startIndex + substringLength
                (startIndex until endIndex).all { charIndex ->
                    if (requireFormatting) {
                        annotatedString.spanStyles.any { range ->
                            range.start <= charIndex && range.end > charIndex && predicate(range.item)
                        }
                    } else {
                        annotatedString.spanStyles.none { range ->
                            range.start <= charIndex && range.end > charIndex && predicate(range.item)
                        }
                    }
                }
            }
        }
    )
}

// 1. Bold Assertion
fun SemanticsNodeInteraction.assertSubtextIsBold(
    substring: String,
    expectedWeight: FontWeight = FontWeight.Bold
): SemanticsNodeInteraction = assertSubstringStyle("bold weight ($expectedWeight)", substring) {
    it.fontWeight == expectedWeight || (it.fontWeight != null && it.fontWeight!!.weight >= expectedWeight.weight)
}

// 2. Italic Assertion
fun SemanticsNodeInteraction.assertSubtextIsItalic(
    substring: String
): SemanticsNodeInteraction = assertSubstringStyle("italic style", substring) {
    it.fontStyle == FontStyle.Italic
}

// 3. Strikethrough Assertion
fun SemanticsNodeInteraction.assertSubtextHasStrikethrough(
    substring: String
): SemanticsNodeInteraction = assertSubstringStyle("strikethrough decoration", substring) {
    it.textDecoration?.contains(TextDecoration.LineThrough) == true
}

// 4. Monospace Assertion
fun SemanticsNodeInteraction.assertSubtextIsMonospace(
    substring: String
): SemanticsNodeInteraction = assertSubstringStyle("monospace font family", substring) {
    it.fontFamily == FontFamily.Monospace ||
            it.fontFamily?.toString()?.contains("monospace", ignoreCase = true) == true
}

// 5. Unformatted / Plain Text Assertion
fun SemanticsNodeInteraction.assertSubtextHasNoFormatting(
    substring: String
): SemanticsNodeInteraction {
    return assertSubstringStyle("no formatting", substring, requireFormatting = false) {
        it.hasFormatting()
    }
}

@OptIn(ExperimentalTextApi::class)
private fun AnnotatedString.hasLinkAtIndex(charIndex: Int, expectedUrl: String? = null): Boolean {
    val stringAnnotations = getStringAnnotations(start = charIndex, end = charIndex + 1)
    val hasStringAnnotation = stringAnnotations.any { annotation ->
        val matchesUrl = expectedUrl == null || annotation.item == expectedUrl
        matchesUrl && (annotation.tag.equals("URL", ignoreCase = true) || annotation.tag.equals("link", ignoreCase = true) || expectedUrl != null)
    }
    if (hasStringAnnotation) return true

    @Suppress("DEPRECATION")
    val urlAnnotations = getUrlAnnotations(start = charIndex, end = charIndex + 1)
    val hasUrlAnnotation = urlAnnotations.any { annotation ->
        expectedUrl == null || annotation.item.url == expectedUrl
    }
    if (hasUrlAnnotation) return true

    val linkAnnotations = getLinkAnnotations(start = charIndex, end = charIndex + 1)
    val hasLinkAnnotation = linkAnnotations.any { annotation ->
        val linkUrl = (annotation.item as? LinkAnnotation.Url)?.url
        linkUrl != null && (expectedUrl == null || linkUrl == expectedUrl)
    }
    return hasLinkAnnotation
}

// 6. Link Assertion
@OptIn(ExperimentalTextApi::class)
fun SemanticsNodeInteraction.assertSubtextHasLink(
    substring: String,
    url: String? = null
): SemanticsNodeInteraction {
    val description = if (url != null) "link to '$url'" else "a link"
    return assert(
        SemanticsMatcher("has substring '$substring' with $description") { node ->
            val annotatedString: AnnotatedString =
                node.config.getOrNull(SemanticsProperties.EditableText)
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
                    ?: return@SemanticsMatcher false

            val text = annotatedString.text
            val substringLength = substring.length
            if (substringLength == 0) return@SemanticsMatcher false

            val startIndices = mutableListOf<Int>()
            var index = text.indexOf(substring)
            while (index != -1) {
                startIndices.add(index)
                index = text.indexOf(substring, index + 1)
            }
            if (startIndices.isEmpty()) return@SemanticsMatcher false

            startIndices.any { startIndex ->
                val endIndex = startIndex + substringLength
                (startIndex until endIndex).all { charIndex ->
                    annotatedString.hasLinkAtIndex(charIndex, url)
                }
            }
        }
    )
}

fun SemanticsNodeInteraction.performClickOnSubtext(substring: String): SemanticsNodeInteraction {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
    val layout =
        textLayoutResults.firstOrNull() ?: error("Failed to retrieve TextLayoutResult for node")
    val annotatedString: AnnotatedString =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)
            ?: fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
            ?: error("Node has no text")
    val index = annotatedString.text.indexOf(substring)
    require(index != -1) { "Substring '$substring' not found in '${annotatedString.text}'" }
    val boundingBox = layout.getBoundingBox(index)
    return performTouchInput { click(boundingBox.center) }
}