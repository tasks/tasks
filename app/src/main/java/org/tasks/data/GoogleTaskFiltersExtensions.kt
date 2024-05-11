package org.tasks.data

import com.todoroo.astrid.api.GtasksFilter

fun GoogleTaskFilters.toGtasksFilter(): GtasksFilter = GtasksFilter(
    list = googleTaskList,
    count = count,
)
