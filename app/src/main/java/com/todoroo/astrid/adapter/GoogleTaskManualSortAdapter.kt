package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao

class GoogleTaskManualSortAdapter internal constructor(
        private val googleTaskDao: GoogleTaskDao,
        caldavDao: CaldavDao,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager)
    : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.caldavTask
        val previous = if (to > 0) getTask(to - 1) else null
        if (previous == null) {
            googleTaskDao.move(googleTask, 0, 0)
        } else if (to == count || to <= from) {
            when {
                indent == 0 -> googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + if (to == count) 0 else 1)
                previous.hasParent() && previous.parent == task.parent -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + if (to == count) 0 else 1)
                previous.hasParent() -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + 1)
                else -> googleTaskDao.move(googleTask, previous.id, 0)
            }
        } else {
            when {
                indent == 0 -> googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + if (task.hasParent()) 1 else 0)
                previous.hasParent() && previous.parent == task.parent -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort())
                previous.hasParent() -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + 1)
                else -> googleTaskDao.move(googleTask, previous.id, 0)
            }
        }
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.caldav!!)
        }
    }
}