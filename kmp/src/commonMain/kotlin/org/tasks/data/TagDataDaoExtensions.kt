package org.tasks.data

import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.filters.AlphanumComparator

suspend fun TagDataDao.searchTags(query: String): List<TagData> = searchTagsInternal("%$query%").sort()

private val COMPARATOR = Comparator<TagData> { f1, f2 ->
    when {
        f1.order == NO_ORDER && f2.order == NO_ORDER -> f1.id!!.compareTo(f2.id!!)
        f1.order == NO_ORDER -> 1
        f2.order == NO_ORDER -> -1
        f1.order < f2.order -> -1
        f1.order > f2.order -> 1
        else -> AlphanumComparator.TAGDATA.compare(f1, f2)
    }
}

private fun List<TagData>.sort(): List<TagData> =
    if (all { it.order == NO_ORDER }) {
        sortedWith(AlphanumComparator.TAGDATA)
    } else {
        sortedWith(COMPARATOR)
    }
