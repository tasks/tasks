package org.tasks.data

import com.todoroo.astrid.api.TagFilter

fun TagFilters.toTagFilter(): TagFilter = TagFilter(
    tagData = tagData,
    count = count,
)
