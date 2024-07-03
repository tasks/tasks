package org.tasks.data

import org.tasks.filters.GtasksFilter

fun GoogleTaskFilters.toGtasksFilter(): GtasksFilter = GtasksFilter(
    list = googleTaskList,
    count = count,
)
