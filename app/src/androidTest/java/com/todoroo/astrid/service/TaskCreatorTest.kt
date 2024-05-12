package com.todoroo.astrid.service

import com.todoroo.astrid.api.PermaSql.VALUE_EOD
import com.todoroo.astrid.api.PermaSql.VALUE_EOD_NEXT_WEEK
import com.todoroo.astrid.api.PermaSql.VALUE_EOD_TOMORROW
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.DUE_DATE
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.R
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.data.createDueDate
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskCreatorTest : InjectingTestCase() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCreator: TaskCreator

    @Test
    fun setStartAndDueFromFilter() = runBlocking {
        val task = freezeAt(DateTime(2021, 2, 4, 14, 56, 34, 126)) {
            taskCreator.create(mapOf(
                    HIDE_UNTIL.name!! to VALUE_EOD,
                    DUE_DATE.name!! to VALUE_EOD_TOMORROW
            ), null)
        }

        assertEquals(DateTime(2021, 2, 4).millis, task.hideUntil)
        assertEquals(
                createDueDate(URGENCY_SPECIFIC_DAY, DateTime(2021, 2, 5).millis),
                task.dueDate
        )
    }

    @Test
    fun setDefaultStartWithFilterDue() = runBlocking {
        preferences.setString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_DUE.toString())
        val task = freezeAt(DateTime(2021, 2, 4, 14, 56, 34, 126)) {
            taskCreator.create(mapOf(
                    DUE_DATE.name!! to VALUE_EOD
            ), null)
        }

        assertEquals(DateTime(2021, 2, 4).millis, task.hideUntil)
    }

    @Test
    fun setStartAndDueFromPreferences() = runBlocking {
        preferences.setString(R.string.p_default_urgency_key, Task.URGENCY_TODAY.toString())
        preferences.setString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_DUE.toString())

        val task = freezeAt(DateTime(2021, 2, 4, 14, 56, 34, 126)) {
            taskCreator.create(null, "test")
        }

        assertEquals(DateTime(2021, 2, 4).millis, task.hideUntil)
        assertEquals(
                createDueDate(URGENCY_SPECIFIC_DAY, DateTime(2021, 2, 4).millis),
                task.dueDate
        )
    }

    @Test
    fun filterStartOverridesDefaultStart() = runBlocking {
        preferences.setString(R.string.p_default_urgency_key, Task.URGENCY_TODAY.toString())
        preferences.setString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_DUE.toString())

        val task = freezeAt(DateTime(2021, 2, 4, 14, 56, 34, 126)) {
            taskCreator.create(mapOf(
                    HIDE_UNTIL.name!! to VALUE_EOD_NEXT_WEEK
            ), null)
        }

        assertEquals(DateTime(2021, 2, 11).millis, task.hideUntil)
    }

    @Test
    fun filterDueOverridesDefaultDue() = runBlocking {
        preferences.setString(R.string.p_default_urgency_key, Task.URGENCY_TODAY.toString())

        val task = freezeAt(DateTime(2021, 2, 4, 14, 56, 34, 126)) {
            taskCreator.create(mapOf(
                    DUE_DATE.name!! to VALUE_EOD_TOMORROW
            ), null)
        }

        assertEquals(
                createDueDate(URGENCY_SPECIFIC_DAY, DateTime(2021, 2, 5).millis),
                task.dueDate
        )
    }
}