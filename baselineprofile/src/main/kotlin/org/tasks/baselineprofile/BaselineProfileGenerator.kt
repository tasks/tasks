package org.tasks.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

private const val PACKAGE = "org.tasks"
private const val TASK_TITLE = "Baseline profile"
private const val MAIN_ACTIVITY = "com.todoroo.astrid.activity.MainActivity"
private const val LIST_PICKER_ACTIVITY = "org.tasks.compose.FilterSelectionActivity"

private val fab = By.res(PACKAGE, "fab")
private val continueWithoutSync = By.res("continue_without_sync")
private val navigationDrawer = By.res("navigation_drawer")
private val listPicker = By.res("list_picker")
private val dueDateRow = By.res("due_date_row")
private val todayShortcut = By.res("date_shortcut_today")
private val datePickerOk = By.res("date_picker_ok")
private val taskTitle = By.res(PACKAGE, "task_title")
private val saveTask = By.res("save_task")

private const val TIMEOUT = 20_000L

private const val DIALOG_TIMEOUT = 5_000L

class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun completeFirstRun() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm clear $PACKAGE")
        device.executeShellCommand("monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1")
        device.dismissCompatibilityDialog()
        device.tap(continueWithoutSync)
        device.awaitTaskList()
        // Don't force-stop
    }

    @Test
    fun startup() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        device.launchTaskList()
    }

    @Test
    fun createTask() = rule.collect(packageName = PACKAGE) {
        pressHome()
        device.launchTaskList()
        device.tap(fab)
        device.tap(dueDateRow)
        device.tap(todayShortcut)
        device.tap(datePickerOk)
        device.setText(taskTitle, TASK_TITLE)
        device.await(By.text(TASK_TITLE))
        device.tap(saveTask)
        device.tap(By.text(TASK_TITLE))
        device.await(saveTask)
        device.pressBack()
        device.openNavigationDrawer()
        device.pressBack()
    }

    @Test
    fun widgetTaskList() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        device.startWidgetTap(MAIN_ACTIVITY, "open_list", fab)
    }

    @Test
    fun widgetNewTask() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        device.startWidgetTap(
            MAIN_ACTIVITY,
            "new_task",
            saveTask,
            "--el open_task 0",
            "--es create_source widget",
            "--ez remove_task true",
            "--ez finish_affinity true",
        )
    }

    @Test
    fun widgetChooseList() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        device.startWidgetTap(
            LIST_PICKER_ACTIVITY,
            "choose_list",
            listPicker,
            "--ei appWidgetId -1",
        )
    }

    companion object {
        private var autoRotate: String? = null

        @BeforeClass
        @JvmStatic
        fun pinOrientation() {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            autoRotate = device.executeShellCommand("settings get system accelerometer_rotation")
                .trim()
                .takeIf { it.isNotEmpty() && it != "null" }
            device.executeShellCommand("settings put system accelerometer_rotation 0")
            device.setOrientationNatural()
        }

        @AfterClass
        @JvmStatic
        fun restoreOrientation() {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.unfreezeRotation()
            autoRotate?.let {
                device.executeShellCommand("settings put system accelerometer_rotation $it")
            }
        }
    }
}

private fun UiDevice.launchTaskList() = launchAndAwait(fab) {
    executeShellCommand("monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1")
}

private fun UiDevice.launchAndAwait(until: BySelector, launch: () -> Unit) {
    launch()
    await(until)
}

private fun UiDevice.startWidgetTap(
    component: String,
    action: String,
    until: BySelector,
    vararg extras: String,
) = launchAndAwait(until) {
    executeShellCommand(
        "am start -a $action -n $PACKAGE/$component ${extras.joinToString(" ")}".trim()
    )
}

private fun UiDevice.awaitTaskList() = await(fab)

private fun UiDevice.await(selector: BySelector) {
    check(wait(Until.hasObject(selector), TIMEOUT)) { "no view matching $selector" }
}

private fun UiDevice.dismissCompatibilityDialog() {
    wait(Until.findObject(By.res("android", "button1")), DIALOG_TIMEOUT)?.let {
        it.click()
        waitForIdle()
    }
}

private fun UiDevice.tap(selector: BySelector) = onView(selector) { it.click() }

private fun UiDevice.setText(selector: BySelector, text: String) =
    onView(selector) { it.text = text }

private fun UiDevice.onView(selector: BySelector, action: (UiObject2) -> Unit) {
    repeat(2) {
        val target = wait(Until.findObject(selector), TIMEOUT) ?: error("no view matching $selector")
        try {
            action(target)
            waitForIdle()
            return
        } catch (_: StaleObjectException) {
            waitForIdle()
        }
    }
    error("view kept going stale: $selector")
}

private fun UiDevice.openNavigationDrawer() {
    awaitTaskList()
    val button = findObjects(By.clazz("android.widget.ImageButton"))
        .firstOrNull { it.contentDescription.isNullOrEmpty() }
        ?: error("no navigation drawer button on the task list")
    button.click()
    await(navigationDrawer)
}
