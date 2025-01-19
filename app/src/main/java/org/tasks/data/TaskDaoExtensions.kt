package org.tasks.data

import com.todoroo.astrid.api.PermaSql
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.Task
import org.tasks.data.sql.Field
import org.tasks.data.sql.Query
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences

suspend fun TaskDao.fetchTasks(preferences: QueryPreferences, filter: Filter): List<TaskContainer> =
    fetchTasks(TaskListQuery.getQuery(preferences, filter))

internal suspend fun TaskDao.setCollapsed(preferences: QueryPreferences, filter: Filter, collapsed: Boolean) {
    fetchTasks(preferences, filter)
        .filter(TaskContainer::hasChildren)
        .map(TaskContainer::id)
        .eachChunk { setCollapsed(it, collapsed) }
}

suspend fun TaskDao.fetchFiltered(filter: Filter): List<Task> = fetchFiltered(filter.sql!!)

suspend fun TaskDao.fetchFiltered(queryTemplate: String): List<Task> {
    val query = getQuery(queryTemplate, Task.FIELDS)
    val tasks = fetchTasks(query)
    return tasks.map(TaskContainer::task)
}

suspend fun TaskDao.count(filter: Filter): Int {
    val query = getQuery(filter.sql!!, Field.COUNT)
    return count(query)
}

private fun getQuery(queryTemplate: String, vararg fields: Field): String =
    Query.select(*fields)
        .withQueryTemplate(PermaSql.replacePlaceholdersForQuery(queryTemplate))
        .from(Task.TABLE)
        .toString()
