package org.tasks

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.tasks.data.TaskListQuery
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.preferences.DefaultQueryPreferences

abstract class TaskListTest : DatabaseTest() {
    protected val taskDao = db.taskDao()
    protected val caldavDao = db.caldavDao()

    protected open val filter: Filter
        get() = CaldavFilter(
            calendar = CaldavCalendar(uuid = CALENDAR, account = ACCOUNT),
            account = CaldavAccount(uuid = ACCOUNT),
        )

    @Before
    fun createList() = runBlocking {
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT))
        caldavDao.insert(CaldavCalendar(uuid = CALENDAR, account = ACCOUNT))
    }

    protected suspend fun newTask(
        title: String,
        parent: Long = 0,
        completed: Boolean = false,
        dueDate: Long = 0,
        hideUntil: Long = 0,
    ): Task {
        val task = Task(
            title = title,
            parent = parent,
            remoteId = "uuid-$title",
            dueDate = dueDate,
            hideUntil = hideUntil,
            completionDate = if (completed) COMPLETED_AT else 0,
        )
        taskDao.createNew(task)
        caldavDao.insert(
            task = task,
            caldavTask = CaldavTask(task = task.id, calendar = CALENDAR, remoteId = "uuid-$title"),
            addToTop = false,
        )
        return task
    }

    protected fun preferences(
        showCompleted: Boolean = true,
        atBottom: Boolean = true,
        showCompletedSubtasks: Boolean = true,
    ) = DefaultQueryPreferences().apply {
        this.showCompleted = showCompleted
        this.completedTasksAtBottom = atBottom
        this.showCompletedSubtasks = showCompletedSubtasks
    }

    protected suspend fun rows(
        showCompleted: Boolean = true,
        atBottom: Boolean = true,
        showCompletedSubtasks: Boolean = true,
        filter: Filter = this.filter,
    ) = taskDao.fetchTasks(
        TaskListQuery.getQuery(
            preferences(showCompleted, atBottom, showCompletedSubtasks),
            filter,
        )
    )

    protected suspend fun titles(
        showCompleted: Boolean = true,
        atBottom: Boolean = true,
        showCompletedSubtasks: Boolean = true,
        filter: Filter = this.filter,
    ) = rows(showCompleted, atBottom, showCompletedSubtasks, filter).map { it.title }

    protected suspend fun row(
        title: String,
        showCompleted: Boolean = true,
        atBottom: Boolean = true,
        showCompletedSubtasks: Boolean = true,
    ) = rows(showCompleted, atBottom, showCompletedSubtasks).first { it.title == title }

    companion object {
        const val ACCOUNT = "account-1"
        const val CALENDAR = "calendar-1"
        const val PARENT = "parent"
        const val CHILD = "child"
        const val TODO = "todo"
        const val DONE = "done"
        const val DONE_ROOT = "done-root"
        const val COMPLETED_AT = 1_700_000_000_000L
    }
}
