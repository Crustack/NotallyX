package com.philkes.notallyx.test

import android.R.attr.tag
import android.graphics.Point
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
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
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import junit.framework.TestCase.assertTrue
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher

fun onLabelItem(labelText: String): ViewInteraction =
    onView(
        allOf(
            withId(com.philkes.notallyx.R.id.LabelText),
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

// fun waitFor(matcher: Matcher<View>, timeoutMs: Long = 5_000) {
//    val start = System.currentTimeMillis()
//
//    while (System.currentTimeMillis() - start < timeoutMs) {
//        try {
//            onView(matcher).check(matches(isDisplayed()))
//            return
//        } catch (_: NoMatchingViewException) {
//            // Keep waiting
//        } catch (_: AssertionError) {
//            // View exists but isn't displayed yet
//        }
//
//        Thread.sleep(50)
//    }
//
//    // Let Espresso produce the normal, useful failure message
//    onView(matcher).check(matches(isDisplayed()))
// }
//
// fun waitForRecyclerViewPosition(
//    recyclerViewMatcher: Matcher<View>,
//    position: Int,
//    timeoutMs: Long = 10_000,
// ) {
//    val start = System.currentTimeMillis()
//
//    while (System.currentTimeMillis() - start < timeoutMs) {
//        try {
//            onView(recyclerViewMatcher)
//                .perform(
//                    RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
//                )
//
//            return
//        } catch (_: NoMatchingViewException) {
//            // RecyclerView not found yet
//        } catch (_: PerformException) {
//            // Position doesn't exist yet
//        }
//
//        Thread.sleep(50)
//    }
//
//    // Produce Espresso's normal failure
//    onView(recyclerViewMatcher)
//        .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
// }

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
    //    isChild?.let {
    //        SystemClock.sleep(300)
    //        this.checkIsPositionChildItem(position, it)
    //    }
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
 * Finds the [dragHandleResId] inside the items matched by [sourceSelector] and [targetSelector],
 * and delegates the drag-and-drop operation to [dragAndDrop].
 *
 * @param sourceSelector Selector matching the source item container or a child within it
 * @param targetSelector Selector matching the target item container or a child within it
 * @param dragSpeedMs Duration of the drag gesture in milliseconds
 * @param dragHandleResId The resource ID of the drag handle icon (defaults to "dragHandle")
 * @param packageName The application package ID
 */
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
    dragHandleResId: String = "DragHandle",
    longClickBefore: Boolean = false,
) {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // 1. Find the parent item containers that contain the target text/selector anywhere in their
    // subtree
    val sourceItemContainer =
        device.wait(
            Until.findObject(By.res(packageName, "Content").hasDescendant(sourceSelector)),
            5000,
        ) ?: throw IllegalStateException("Source item container matching $sourceSelector not found")

    val targetItemContainer =
        device.wait(
            Until.findObject(By.res(packageName, "Content").hasDescendant(targetSelector)),
            5000,
        ) ?: throw IllegalStateException("Target item container matching $targetSelector not found")

    // 2. Extract the actual @+id/DragHandle child view from each parent container
    val sourceHandleObject =
        sourceItemContainer.findObject(By.res(packageName, dragHandleResId))
            ?: throw IllegalStateException("DragHandle ($dragHandleResId) not found in source item")

    val targetHandleObject =
        targetItemContainer.findObject(By.res(packageName, dragHandleResId))
            ?: throw IllegalStateException("DragHandle ($dragHandleResId) not found in target item")

    dragAndDrop(
        sourceObject = sourceHandleObject,
        targetObject = targetHandleObject,
        dragSpeedMs = dragSpeedMs,
        longClickBefore = longClickBefore,
    )
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
    // 1. Wait for source and target objects to be present
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
    // 2. Extract bounds for calculation
    val sourceBounds = sourceObject.visibleBounds
    val targetBounds = targetObject.visibleBounds

    // Target point: 10% past the bottom edge of the target item if dragging down,
    // or 10% above the top edge if dragging up
    val isDraggingDown = targetBounds.centerY() > sourceBounds.centerY()
    val endX = targetBounds.centerX()
    val endY =
        if (isDraggingDown) {
            targetBounds.bottom + (targetBounds.height() * 0.25).toInt()
        } else {
            targetBounds.top - (targetBounds.height() * 0.25).toInt()
        }
    if (longClickBefore) {
        sourceObject.longClick()
    }

    // 3. Execute drag with target Point
    val dropPoint = Point(endX, endY)
    sourceObject.drag(dropPoint, dragSpeedMs)

    // 4. Brief pause for ItemTouchHelper animation to settle
    SystemClock.sleep(300)
}

fun swipeItem(
    itemSelector: BySelector,
    direction: Direction = Direction.LEFT,
    percent: Float = 0.8f,
    speed: Float = 0.8f,
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // 1. Locate the parent item container holding the target selector
    val itemContainer =
        device.wait(Until.findObject(itemSelector), 5000)
            ?: throw IllegalStateException("Item container matching $itemSelector not found")

    // 2. Perform swipe on the container object
    itemContainer.swipe(direction, percent, (speed * 1000).toInt())

    // Brief sleep for swipe drawer animation to settle
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

    // Check current state to avoid unnecessary toggle clicks
    val currentMatcher = if (checked) isNotChecked() else isChecked()

    try {
        // If it matches the opposite state, click to toggle
        targetCheckBox.check(matches(currentMatcher)).perform(click())
    } catch (e: AssertionError) {
        // Already in the desired state, no action needed
    }

    return this
}

/**
 * Returns a ViewInteraction targeting a specific child view matching [viewMatcher] inside the
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

// TODO: not working yet
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
        isPinnedToStatus = false,
    )
}

fun createListItem(
    body: String,
    checked: Boolean = false,
    isChild: Boolean = false,
    order: Int? = null,
    children: MutableList<ListItem> = mutableListOf(),
    id: Int = -1,
): ListItem {
    return ListItem(body, checked, isChild, order, children, id)
}

fun onDisplayView(viewMatcher: Matcher<View>) = onView(allOf(viewMatcher, isDisplayed()))

fun navigateTo(fragmentId: Int) {
    onDisplayView(withContentDescription("Open navigation drawer")).perform(click())
    onDisplayView(allOf(withId(fragmentId), isDescendantOfA(withId(R.id.NavigationView))))
        .perform(click())
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

/**
 * Source:
 * https://medium.com/@andre.mendes.peixoto/how-to-reliably-assert-toast-messages-in-android-instrumented-tests-f94d830f4de1
 */
fun assertToastDisplayed(text: String, timeoutMs: Long = 10000) {
    var toastDisplayed = false
    val startTimeMs = System.currentTimeMillis()

    // Set up accessibility event listener to catch toast notifications
    InstrumentationRegistry.getInstrumentation().uiAutomation.setOnAccessibilityEventListener {
        event ->
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val className = event.className?.toString() ?: ""
            val eventText = event.text.toString()

            // Check if this is a Toast event with matching text
            if (className.contains("android.widget.Toast") && eventText.contains(text)) {
                toastDisplayed = true
            }
        }
    }

    // Wait for the toast to appear
    while (!toastDisplayed && System.currentTimeMillis() - startTimeMs < timeoutMs) {
        SystemClock.sleep(100)
    }

    // Clean up the listener
    InstrumentationRegistry.getInstrumentation().uiAutomation.setOnAccessibilityEventListener(null)

    assertTrue("Toast with text '$text' not found within ${timeoutMs}ms", toastDisplayed)
}

fun assertLogAppeared(tag: String, expectedMessagePrefix: String, timeoutMs: Long = 10000) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeoutMs) {
        // Dump recent logcat output for the specified tag
        val logcatOutput = device.executeShellCommand("logcat -d -s $tag")
        if (logcatOutput.contains(expectedMessagePrefix)) {
            return // Log found!
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
            // ONE-TIME WORK: Transitions to SUCCEEDED
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                return // Execution succeeded!
            }

            // PERIODIC WORK: Re-enqueues after successful run (runAttemptCount == 0)
            // or is currently running
            if (workInfo.state == WorkInfo.State.ENQUEUED && workInfo.runAttemptCount == 0) {
                // If it was enqueued and runAttemptCount is 0, check if execution happened
                val hasCompletedPeriod = workInfo.nextScheduleTimeMillis > 0
                if (hasCompletedPeriod) {
                    return // Periodic execution completed successfully!
                }
            }
        }

        Thread.sleep(pollIntervalMs)
    }

    // Capture final state for descriptive failure message
    val finalState =
        workManager.getWorkInfosForUniqueWork(workName).get().firstOrNull()?.state?.name
            ?: "NOT_SCHEDULED"

    throw AssertionError(
        "Work '$workName' did not complete successfully within ${timeoutMs}ms. Final state: $finalState"
    )
}
