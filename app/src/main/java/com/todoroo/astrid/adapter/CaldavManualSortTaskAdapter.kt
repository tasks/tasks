package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskMover
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao

class CaldavManualSortTaskAdapter internal constructor(
        googleTaskDao: GoogleTaskDao,
        caldavDao: CaldavDao,
        taskDao: TaskDao,
        localBroadcastManager: LocalBroadcastManager,
        taskMover: TaskMover,
) : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager, taskMover) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        moveCaldavTask(from, to, indent)
    }
}