package org.tasks.api

import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter

interface ApiTaskFactory {
    suspend fun defaultList(): CaldavFilter

    suspend fun create(title: String, list: CaldavFilter, configure: (Task) -> Unit): Task
}
