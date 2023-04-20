package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskMover
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao

class GoogleTaskManualSortAdapter internal constructor(
        private val googleTaskDao: GoogleTaskDao,
        caldavDao: CaldavDao,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        taskMover: TaskMover,
) : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.caldavTask
        val previous = if (to > 0) getTask(to - 1) else null
        val list = googleTask.calendar!!
        if (previous == null) {
            googleTaskDao.move(
                task = task.task,
                list = list,
                newParent = 0,
                newPosition = 0,
            )
        } else if (to == count || to <= from) {
            when {
                indent == 0 ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = 0,
                        newPosition = previous.getPrimarySort() + if (to == count) 0 else 1,
                    )
                previous.hasParent() && previous.parent == task.parent ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.getSecondarySort() + if (to == count) 0 else 1,
                    )
                previous.hasParent() ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.getSecondarySort() + 1,
                    )
                else ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.id,
                        newPosition = 0,
                    )
            }
        } else {
            when {
                indent == 0 ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = 0,
                        newPosition = previous.getPrimarySort() + if (task.hasParent()) 1 else 0,
                    )
                previous.hasParent() && previous.parent == task.parent ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.getSecondarySort(),
                    )
                previous.hasParent() ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.parent,
                        newPosition = previous.getSecondarySort() + 1,
                    )
                else ->
                    googleTaskDao.move(
                        task = task.task,
                        list = list,
                        newParent = previous.id,
                        newPosition = 0,
                    )
            }
        }
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.caldav!!)
        }
    }
}