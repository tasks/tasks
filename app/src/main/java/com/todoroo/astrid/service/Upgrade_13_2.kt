@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.FilterDao
import org.tasks.filters.FilterCriteriaProvider
import javax.inject.Inject

class Upgrade_13_2 @Inject constructor(
    private val filterDao: FilterDao,
    private val filterCriteriaProvider: FilterCriteriaProvider,
) {
    internal suspend fun rebuildFilters() =
        filterDao.getFilters().forEach {
            filterCriteriaProvider.rebuildFilter(it)
            filterDao.update(it)
        }

    companion object {
        const val VERSION = 130200
    }
}