package org.tasks.repeats

import net.fortuna.ical4j.model.property.RRule

fun newRRule(rrule: String): RRule = RRule(Recur.parse(rrule).toIcal4j())
