package org.tasks.opentasks

import org.tasks.data.dao.TaskDao
import org.tasks.data.TaskSaver
import org.junit.Before
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

abstract class OpenTasksTest : InjectingTestCase() {
    @Inject lateinit var openTaskDao: TestOpenTaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var synchronizer: OpenTasksSynchronizer
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskSaver: TaskSaver

    @Before
    override fun setUp() {
        super.setUp()

        openTaskDao.reset()
    }

    protected suspend fun withVtodo(vtodo: String): Pair<Long, CaldavCalendar> =
            openTaskDao
                    .insertList()
                    .let { (listId, list) ->
                        openTaskDao.insertTask(listId, vtodo)
                        Pair(listId, list)
                    }
}