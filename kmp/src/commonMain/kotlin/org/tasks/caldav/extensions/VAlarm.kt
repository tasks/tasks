package org.tasks.caldav.extensions

import org.tasks.data.entity.Alarm
import org.tasks.icalendar.Trigger
import org.tasks.icalendar.VAlarm

fun List<Alarm>.toVAlarms(): List<VAlarm> = mapNotNull(Alarm::toVAlarm)

fun Alarm.toVAlarm(): VAlarm? {
    val trigger = when (type) {
        Alarm.TYPE_DATE_TIME -> Trigger.Absolute(time)
        Alarm.TYPE_REL_START, Alarm.TYPE_REL_END -> Trigger.Relative(time, relatedToEnd = type == Alarm.TYPE_REL_END)
        else -> return null
    }
    return VAlarm(
        trigger = trigger,
        action = "DISPLAY",
        description = "Default Tasks.org description",
        repeat = repeat.takeIf { it > 0 },
        duration = interval.takeIf { repeat > 0 },
    )
}

fun List<VAlarm>.toAlarms(): List<Alarm> = mapNotNull(VAlarm::toAlarm)

fun VAlarm.toAlarm(): Alarm? {
    val (type, time) = when (val trigger = trigger) {
        is Trigger.Absolute -> Alarm.TYPE_DATE_TIME to trigger.millis
        is Trigger.Relative -> (if (trigger.relatedToEnd) Alarm.TYPE_REL_END else Alarm.TYPE_REL_START) to trigger.millis
        is Trigger.Unknown, null -> return null
    }
    return Alarm(time = time, type = type, repeat = repeat ?: 0, interval = duration ?: 0)
}

fun List<VAlarm>.preserving(replaced: List<VAlarm>): List<VAlarm> {
    val originals = replaced.filter { it.hasPreservableDetails }.toMutableList()
    if (originals.isEmpty()) {
        return this
    }
    return map { alarm ->
        val original = originals.removeMatch(alarm) ?: return@map alarm
        alarm.copy(
            trigger = alarm.trigger.preserving(original.trigger),
            otherProperties = original.otherProperties.takeIf { original.action == alarm.action }.orEmpty(),
            propertyOrder = original.propertyOrder,
        )
    }
}

private fun MutableList<VAlarm>.removeMatch(alarm: VAlarm): VAlarm? {
    val index = indexOfFirst { it.trigger sameAs alarm.trigger && it.action == alarm.action }
        .takeIf { it >= 0 }
        ?: indexOfFirst { it.trigger sameAs alarm.trigger }
    return if (index >= 0) removeAt(index) else null
}

private val VAlarm.hasPreservableDetails: Boolean
    get() = otherProperties.isNotEmpty() || propertyOrder.isNotEmpty() || when (val t = trigger) {
        is Trigger.Absolute -> t.parameters.isNotEmpty()
        is Trigger.Relative -> t.value != null || t.parameters.isNotEmpty()
        is Trigger.Unknown, null -> false
    }

private infix fun Trigger?.sameAs(other: Trigger?): Boolean = when {
    this is Trigger.Absolute && other is Trigger.Absolute -> millis == other.millis
    this is Trigger.Relative && other is Trigger.Relative ->
        millis == other.millis && relatedToEnd == other.relatedToEnd
    else -> false
}

private fun Trigger?.preserving(original: Trigger?): Trigger? = when {
    this is Trigger.Absolute && original is Trigger.Absolute ->
        copy(parameters = original.parameters)
    this is Trigger.Relative && original is Trigger.Relative ->
        copy(value = original.value, parameters = original.parameters)
    else -> this
}
