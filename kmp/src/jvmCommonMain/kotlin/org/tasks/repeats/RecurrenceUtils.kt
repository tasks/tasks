package org.tasks.repeats

import org.tasks.data.entity.Task.Companion.sanitizeRecur
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.property.RRule

object RecurrenceUtils {
    private val LEGACY_RRULE_PREFIX = "^RRULE:".toRegex()

    @JvmStatic
    fun newRecur(): Recur = Recur.Builder().frequency(Recur.Frequency.DAILY).build()

    @JvmStatic
    fun newRecur(rrule: String): Recur = newRRule(rrule).recur

    fun newRRule(rrule: String): RRule =
            RRule(rrule.replace(LEGACY_RRULE_PREFIX, "").sanitizeRecur())

}