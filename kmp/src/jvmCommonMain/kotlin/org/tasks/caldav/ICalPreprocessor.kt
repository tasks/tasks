/*
 * Based on ical4android which is released under GPLv3.
 * Copyright © ical4android contributors. See https://github.com/bitfireAT/ical4android
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.tasks.caldav

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.transform.rfc5545.CreatedPropertyRule
import net.fortuna.ical4j.transform.rfc5545.DateListPropertyRule
import net.fortuna.ical4j.transform.rfc5545.DatePropertyRule
import net.fortuna.ical4j.transform.rfc5545.Rfc5545PropertyRule

object ICalPreprocessor {

    private val propertyRules = arrayOf(
        CreatedPropertyRule(),
        DatePropertyRule(),
        DateListPropertyRule()
    )

    fun preprocessCalendar(calendar: Calendar) {
        for (component in calendar.components)
            for (property in component.properties)
                applyRules(property)
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyRules(property: Property) {
        propertyRules
            .filter { rule -> rule.supportedType.isAssignableFrom(property::class.java) }
            .forEach {
                (it as Rfc5545PropertyRule<Property>).applyTo(property)
            }
    }
}
