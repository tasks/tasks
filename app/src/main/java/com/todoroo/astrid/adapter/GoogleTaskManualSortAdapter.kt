package com.todoroo.astrid.adapter

import org.tasks.data.dao.TaskDao
import org.tasks.data.TaskSaver
import org.tasks.data.TaskMover
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.GoogleTaskDao

class GoogleTaskManualSortAdapter internal constructor(
    googleTaskDao: GoogleTaskDao,
    caldavDao: CaldavDao,
    taskDao: TaskDao,
    taskSaver: TaskSaver,
    dirtyDao: DirtyDao,
    localBroadcastManager: LocalBroadcastManager,
    taskMover: TaskMover,
) : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, taskSaver, dirtyDao, localBroadcastManager, taskMover) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        moveGoogleTask(from, to, indent)
    }
}