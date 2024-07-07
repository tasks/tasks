package org.tasks.data

import org.tasks.filters.TagFilter

fun TagFilters.toTagFilter(): TagFilter = TagFilter(
    tagData = tagData,
    count = count,
)
