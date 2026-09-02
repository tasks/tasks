package org.tasks.api

import com.todoroo.astrid.service.TaskCreator
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

class AppApiTaskFactory @Inject constructor(
    private val taskCreator: TaskCreator,
    private val defaultFilterProvider: DefaultFilterProvider,
) : ApiTaskFactory {
    override suspend fun defaultList(): CaldavFilter = defaultFilterProvider.getDefaultList()

    override suspend fun create(
        title: String,
        list: CaldavFilter,
        configure: (Task) -> Unit,
    ): Task = taskCreator.basicQuickAddTask(
        title = title,
        filter = list,
        parseTitle = false,
        applyDefaults = false,
        configure = configure,
    )
}
