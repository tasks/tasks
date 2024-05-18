package org.tasks.caldav.extensions

import at.bitfire.ical4android.util.TimeApiExtensions.toDuration
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.parameter.Related
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Repeat
import net.fortuna.ical4j.model.property.Trigger
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.getDateTime
import org.tasks.data.entity.Alarm
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAmount

fun List<Alarm>.toVAlarms(): List<VAlarm> = mapNotNull(Alarm::toVAlarm)

internal fun Alarm.toVAlarm(): VAlarm? {
    val trigger = when (type) {
        Alarm.TYPE_DATE_TIME ->
            Trigger(getDateTime(time))
        Alarm.TYPE_REL_START,
        Alarm.TYPE_REL_END ->
            Trigger(
                ParameterList().apply {
                    add(if (type == Alarm.TYPE_REL_END) Related.END else Related.START)
                },
                Duration.ofMillis(time)
            )
        else ->
            return null
    }
    return VAlarm().also {
        with(it.properties) {
            add(trigger)
            add(Action.DISPLAY)
            add(Description("Default Tasks.org description"))
            if (repeat > 0) {
                add(Repeat(repeat))
                add(
                    net.fortuna.ical4j.model.property.Duration(
                        Duration.ofMillis(interval)
                    )
                )
            }
        }
    }
}

fun List<VAlarm>.toAlarms(): List<Alarm> = mapNotNull(VAlarm::toAlarm)

internal fun VAlarm.toAlarm(): Alarm? {
    val (type, time) = when {
        trigger.date != null ->
            Pair(Alarm.TYPE_DATE_TIME, iCalendar.getLocal(trigger))
        trigger.duration != null ->
            Pair(
                if (trigger.parameters.getParameter<Related>(Related.RELATED) == Related.END) {
                    Alarm.TYPE_REL_END
                } else {
                    Alarm.TYPE_REL_START
                },
                trigger.duration.toMillis()
            )
        else ->
            return null
    }
    return Alarm(
        time = time,
        type = type,
        repeat = repeat?.count ?: 0,
        interval = duration?.toMillis() ?: 0
    )
}

private fun net.fortuna.ical4j.model.property.Duration.toMillis() = duration.toMillis()

private fun TemporalAmount.toMillis() = toDuration(Instant.EPOCH).toMillis()

