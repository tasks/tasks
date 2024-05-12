package org.tasks.ui.editviewmodel

import com.todoroo.astrid.core.BuiltInFilterExposer
import org.tasks.data.entity.Task
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.ui.TaskListViewModel
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TaskListViewModelTest : InjectingTestCase() {
    private lateinit var viewModel: TaskListViewModel
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var deletionDao: DeletionDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var firebase: Firebase

    @Before
    override fun setUp() {
        super.setUp()
        viewModel = TaskListViewModel(
            context = context,
            preferences = preferences,
            taskDao = taskDao,
            deletionDao = deletionDao,
            taskDeleter = taskDeleter,
            localBroadcastManager = localBroadcastManager,
            inventory = inventory,
            firebase = firebase,
        )
        viewModel.setFilter(BuiltInFilterExposer.getMyTasksFilter(context.resources))
    }

    @Test
    fun clearCompletedTask() = runBlocking {
        val task = taskDao.createNew(
            Task(completionDate = currentTimeMillis())
        )

        clearCompleted()

        assertTrue(taskDao.fetch(task)!!.isDeleted)
    }

    @Test
    fun dontDeleteTaskWithRecurringParent() = runBlocking {
        val parent = taskDao.createNew(
            Task(
                recurrence = "RRULE:FREQ=DAILY;INTERVAL=1"
            )
        )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        clearCompleted()

        assertFalse(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun dontDeleteTaskWithRecurringGrandparent() = runBlocking {
        val grandparent = taskDao.createNew(
            Task(recurrence = "RRULE:FREQ=DAILY;INTERVAL=1")
        )
        val parent = taskDao.createNew(
            Task(parent = grandparent)
        )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        clearCompleted()

        assertFalse(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun clearGrandchildWithNoRecurringAncestors() = runBlocking {
        val grandparent = taskDao.createNew(Task())
        val parent = taskDao.createNew(
            Task(parent = grandparent)
        )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        clearCompleted()

        assertTrue(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun clearGrandchildWithCompletedRecurringAncestor() = runBlocking {
        val grandparent = taskDao.createNew(
            Task(
                recurrence = "RRULE:FREQ=DAILY;INTERVAL=1",
                completionDate = currentTimeMillis(),
            )
        )
        val parent = taskDao.createNew(
            Task(parent = grandparent)
        )
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        clearCompleted()

        assertTrue(taskDao.fetch(child)!!.isDeleted)
    }

    @Test
    fun clearHiddenSubtask() = runBlocking {
        preferences.showCompleted = false
        val parent = taskDao.createNew(Task())
        val child = taskDao.createNew(
            Task(
                parent = parent,
                completionDate = currentTimeMillis(),
            )
        )

        clearCompleted()

        assertTrue(taskDao.fetch(child)!!.isDeleted)
    }

    private suspend fun clearCompleted() = viewModel.markDeleted(viewModel.getTasksToClear())
}