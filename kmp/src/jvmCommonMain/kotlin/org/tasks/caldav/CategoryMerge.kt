package org.tasks.caldav

import org.tasks.data.entity.TagData

fun mergeCategories(
    base: List<String>,
    local: List<String>,
    remote: List<String>,
): List<String> {
    val baseKeys = base.mapTo(HashSet()) { TagData.normalize(it) }
    val localKeys = local.mapTo(HashSet()) { TagData.normalize(it) }
    val removedLocally = baseKeys - localKeys
    val addedLocally = local.filter { TagData.normalize(it) !in baseKeys }
    val merged = LinkedHashMap<String, String>()
    for (name in remote + addedLocally) {
        val key = TagData.normalize(name)
        if (key in removedLocally || key in merged) continue
        merged[key] = name
    }
    return merged.values.toList()
}
