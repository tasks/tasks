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
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.util.Scanner
import java.util.logging.Level
import java.util.logging.Logger

abstract class StreamPreprocessor {

    abstract fun regexpForProblem(): Regex?

    abstract fun fixString(original: String): String

    fun preprocess(reader: Reader): Reader {
        var result: String? = null

        val resetSupported = try {
            reader.reset()
            true
        } catch (_: IOException) {
            false
        }

        if (resetSupported) {
            val regex = regexpForProblem()
            if (regex == null || Scanner(reader).findWithinHorizon(regex.toPattern(), 0) != null) {
                reader.reset()
                result = fixString(reader.readText())
            }
        } else {
            result = fixString(reader.readText())
        }

        if (result != null) {
            return StringReader(result)
        }

        reader.reset()
        return reader
    }
}

object FixInvalidUtcOffsetPreprocessor : StreamPreprocessor() {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    private val TZOFFSET_REGEXP = Regex(
        "^(TZOFFSET(FROM|TO):[+\\-]?)((18|19|[2-6]\\d)\\d\\d)$",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

    override fun regexpForProblem() = TZOFFSET_REGEXP

    override fun fixString(original: String) =
        original.replace(TZOFFSET_REGEXP) {
            logger.log(Level.FINE, "Applying Synology WebDAV fix to invalid utc-offset", it.value)
            "${it.groupValues[1]}00${it.groupValues[3]}"
        }
}

object FixInvalidDayOffsetPreprocessor : StreamPreprocessor() {

    override fun regexpForProblem() = Regex(
        "(?:^|^(?:DURATION|REFRESH-INTERVAL|RELATED-TO|TRIGGER);VALUE=)(?:DURATION|TRIGGER):(-?P((T-?\\d+D)|(-?\\d+DT)))$",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

    override fun fixString(original: String): String {
        var iCal = original
        val found = regexpForProblem()!!.findAll(iCal).toList()
        for (match in found.reversed()) {
            match.groups[1]?.let { duration ->
                val fixed = duration.value
                    .replace("PT", "P")
                    .replace("DT", "D")
                iCal = iCal.replaceRange(duration.range, fixed)
            }
        }
        return iCal
    }
}

object ICalPreprocessor {

    private val propertyRules = arrayOf(
        CreatedPropertyRule(),
        DatePropertyRule(),
        DateListPropertyRule()
    )

    private val streamPreprocessors = arrayOf(
        FixInvalidUtcOffsetPreprocessor,
        FixInvalidDayOffsetPreprocessor
    )

    fun preprocessStream(original: Reader): Reader {
        var reader = original
        for (preprocessor in streamPreprocessors)
            reader = preprocessor.preprocess(reader)
        return reader
    }

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
