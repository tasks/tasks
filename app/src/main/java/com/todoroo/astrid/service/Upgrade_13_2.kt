@file:Suppress("ClassName")

package com.todoroo.astrid.service

import com.todoroo.astrid.core.CriterionInstance
import org.tasks.activities.FilterSettingsActivity.Companion.sql
import org.tasks.data.Filter
import org.tasks.data.FilterDao
import org.tasks.filters.FilterCriteriaProvider
import javax.inject.Inject

class Upgrade_13_2 @Inject constructor(
    private val filterDao: FilterDao,
    private val filterCriteriaProvider: FilterCriteriaProvider,
) {
    internal suspend fun rebuildFilters() =
        filterDao.getFilters().forEach { rebuildFilter(it) }

    private suspend fun rebuildFilter(filter: Filter) {
        val serialized = filter.criterion?.takeIf { it.isNotBlank() }
        val criterion = filterCriteriaProvider.fromString(serialized)
        filter.setSql(criterion.sql)
        filter.criterion = CriterionInstance.serialize(criterion)
        filterDao.update(filter)
    }

    companion object {
        const val VERSION = 130200
    }
}