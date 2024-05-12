package org.tasks.opentasks

import com.todoroo.astrid.dao.TaskDao
import org.junit.Before
import org.tasks.R
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.InjectingTestCase
import org.tasks.preferences.Preferences
import javax.inject.Inject

abstract class OpenTasksTest : InjectingTestCase() {
    @Inject lateinit var openTaskDao: TestOpenTaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var synchronizer: OpenTasksSynchronizer
    @Inject lateinit var taskDao: TaskDao

    @Before
    override fun setUp() {
        super.setUp()

        openTaskDao.reset()
        preferences.setBoolean(R.string.p_debug_pro, true)
    }

    protected suspend fun withVtodo(vtodo: String): Pair<Long, CaldavCalendar> =
            openTaskDao
                    .insertList()
                    .let { (listId, list) ->
                        openTaskDao.insertTask(listId, vtodo)
                        Pair(listId, list)
                    }
}