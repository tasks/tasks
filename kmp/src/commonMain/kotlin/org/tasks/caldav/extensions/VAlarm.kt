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

fun List<VAlarm>.toAlarms(): List<Alarm> = map(VAlarm::toAlarm)

fun VAlarm.toAlarm(): Alarm {
    val (type, time) = when (val trigger = trigger) {
        is Trigger.Absolute -> Alarm.TYPE_DATE_TIME to trigger.millis
        is Trigger.Relative -> (if (trigger.relatedToEnd) Alarm.TYPE_REL_END else Alarm.TYPE_REL_START) to trigger.millis
    }
    return Alarm(time = time, type = type, repeat = repeat ?: 0, interval = duration ?: 0)
}
