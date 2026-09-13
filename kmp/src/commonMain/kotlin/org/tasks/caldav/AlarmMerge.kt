package org.tasks.caldav

import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE

fun mergeReminders(
    base: Collection<Alarm>,
    local: Collection<Alarm>,
    remote: Collection<Alarm>,
): Set<Alarm> {
    val baseSet = base.toSet()
    val localSet = local.toSet()
    val remoteSet = remote.toSet()
    val merged = (localSet + remoteSet)
        .filterNot { it in baseSet && (it !in localSet || it !in remoteSet) }
        .toMutableSet()
    val snoozes = merged.filter { it.type == TYPE_SNOOZE }
    if (snoozes.size > 1) {
        merged.removeAll(snoozes.toSet())
        merged.add(snoozes.maxBy { it.time })
    }
    return merged
}
