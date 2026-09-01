package org.tasks.service

import com.todoroo.astrid.service.CommonUpgrades
import com.todoroo.astrid.service.Upgrade_15_11
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.tasks.DatabaseTest
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.kmp.createDataStore
import org.tasks.kmp.dataStoreFileName
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTime
import java.io.File

class UpgraderTest : DatabaseTest() {
    @get:Rule val folder = TemporaryFolder()

    private val preferences: TasksPreferences by lazy {
        TasksPreferences(createDataStore { File(folder.root, dataStoreFileName).absolutePath })
    }

    private val ran = mutableListOf<Int>()
    private val constructed = mutableListOf<Int>()

    private fun step(at: Int, body: suspend () -> Unit = { ran.add(at) }) =
        UpgradeStep(at) {
            constructed.add(at)
            Upgrade { body() }
        }

    private fun upgrader(vararg steps: UpgradeStep) = Upgrader(preferences, steps.toList())

    private fun commonUpgrader() =
        Upgrader(preferences, CommonUpgrades.all(db))

    private suspend fun currentVersion() = preferences.get(TasksPreferences.currentVersion, 0)

    private suspend fun monthEndTask(): Task {
        val task = Task(
            title = "rent",
            recurrence = "FREQ=MONTHLY",
            dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2026, 1, 31).millis),
        )
        db.taskDao().createNew(task)
        return task
    }

    private suspend fun recurrence(task: Task) = db.taskDao().fetch(task.id)!!.recurrence

    @Test
    fun runsOnlyStepsNewerThanStoredVersion() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 200)

        upgrader(step(100), step(300)).upgrade(TO)

        assertEquals(listOf(300), ran)
        assertEquals(TO, currentVersion())
    }

    @Test
    fun onlyEligibleStepsAreConstructed() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 200)

        upgrader(step(100), step(300), step(400)).upgrade(TO)

        assertEquals(listOf(300, 400), constructed)
    }

    @Test
    fun runsStepsInVersionOrder() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 100)

        upgrader(step(400), step(200), step(300)).upgrade(TO)

        assertEquals(listOf(200, 300, 400), ran)
    }

    @Test
    fun freshInstallRunsNothing() = runBlocking {
        upgrader(step(100)).upgrade(TO)

        assertEquals(emptyList<Int>(), ran)
        assertEquals(TO, currentVersion())
    }

    @Test
    fun alreadyUpToDateRunsNothing() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, TO)

        upgrader(step(100)).upgrade(TO)

        assertEquals(emptyList<Int>(), ran)
        assertEquals(TO, currentVersion())
    }

    @Test
    fun unknownVersionCodeIsIgnored() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 151000)

        upgrader(step(160000)).upgrade(0)

        assertEquals(emptyList<Int>(), ran)
        assertEquals(151000, currentVersion())
    }

    @Test
    fun interruptedUpgradeResumesAtTheFailedStep() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 100)
        val boom = step(300) { throw IllegalStateException("boom") }

        try {
            upgrader(step(200), boom, step(400)).upgrade(TO)
        } catch (ignored: IllegalStateException) {
        }

        assertEquals(listOf(200), ran)
        assertEquals(200, currentVersion())
    }

    @Test
    fun commonUpgradesMigrateRecurrence() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, 151000)
        val task = monthEndTask()

        commonUpgrader().upgrade(TO)

        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", recurrence(task))
        assertEquals(TO, currentVersion())
    }

    @Test
    fun commonUpgradeAlreadyAppliedIsNotRepeated() = runBlocking {
        preferences.set(TasksPreferences.currentVersion, Upgrade_15_11.VERSION)
        val task = monthEndTask()

        commonUpgrader().upgrade(TO)

        assertEquals("FREQ=MONTHLY", recurrence(task))
        assertEquals(TO, currentVersion())
    }

    @Test
    fun freshInstallHasNothingToMigrate() = runBlocking {
        val task = monthEndTask()

        commonUpgrader().upgrade(TO)

        assertEquals("FREQ=MONTHLY", recurrence(task))
        assertEquals(TO, currentVersion())
    }

    companion object {
        private const val TO = 160000
    }
}
