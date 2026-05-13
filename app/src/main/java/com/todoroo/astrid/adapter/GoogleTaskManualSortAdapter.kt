package com.todoroo.astrid.adapter

import org.tasks.data.dao.TaskDao
import org.tasks.data.TaskSaver
import com.todoroo.astrid.service.TaskMover
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao

class GoogleTaskManualSortAdapter internal constructor(
    googleTaskDao: GoogleTaskDao,
    caldavDao: CaldavDao,
    taskDao: TaskDao,
    taskSaver: TaskSaver,
    localBroadcastManager: LocalBroadcastManager,
    taskMover: TaskMover,
) : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        moveGoogleTask(from, to, indent)
    }
}