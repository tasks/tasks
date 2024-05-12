/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility

import com.mdimension.jchronic.AstridChronic
import com.mdimension.jchronic.Chronic
import net.fortuna.ical4j.model.Recur.Frequency
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Task
import org.tasks.data.createDueDate
import org.tasks.repeats.RecurrenceUtils.newRecur
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object TitleParser {
    suspend fun parse(tagDataDao: TagDataDao, task: Task, tags: ArrayList<String>) {
        repeatHelper(task)
        listHelper(
                tagDataDao,
                task,
                tags) // Don't need to know if tags affected things since we don't show alerts for them
        dayHelper(task)
        priorityHelper(task)
    }

    fun trimParenthesis(pattern: String): String {
        var pattern = pattern
        if (pattern[0] == '#' || pattern[0] == '@') {
            pattern = pattern.substring(1)
        }
        return if ('(' == pattern[0]) {
            pattern.substring(1, pattern.length - 1)
        } else pattern
    }

    suspend fun listHelper(tagDataDao: TagDataDao, task: Task, tags: ArrayList<String>) {
        var inputText = task.title
        val tagPattern = Pattern.compile("(\\s|^)#(\\(.*\\)|[^\\s]+)")
        val contextPattern = Pattern.compile("(\\s|^)@(\\(.*\\)|[^\\s]+)")
        val addedTags: MutableSet<String?> = HashSet()
        while (true) {
            var m = tagPattern.matcher(inputText)
            if (m.find()) {
                val tag = trimParenthesis(m.group(2))
                tagDataDao
                        .getTagWithCase(tag)
                        ?.let {
                            if (!addedTags.contains(it)) {
                                tags.add(it)
                            }
                            addedTags.add(it)
                        }
            } else {
                m = contextPattern.matcher(inputText)
                if (m.find()) {
                    val tag = trimParenthesis(m.group(2))
                    tagDataDao
                            .getTagWithCase(tag)
                            ?.let {
                                if (!addedTags.contains(it)) {
                                    tags.add(it)
                                }
                                addedTags.add(it)
                            }
                } else {
                    break
                }
            }
            inputText = inputText!!.substring(0, m.start()) + inputText.substring(m.end())
        }
        task.title = inputText!!.trim { it <= ' ' }
    }

    private fun strToPriority(priorityStr: String?): Int {
        priorityStr?.lowercase(Locale.getDefault())?.trim { it <= ' ' }
        var priority = Task.Priority.HIGH
        if ("0" == priorityStr || "!0" == priorityStr || "least" == priorityStr || "lowest" == priorityStr) {
            priority = Task.Priority.NONE
        }
        if ("!" == priorityStr || "!1" == priorityStr || "bang" == priorityStr || "1" == priorityStr || "low" == priorityStr) {
            priority = Task.Priority.LOW
        }
        if ("!!" == priorityStr || "!2" == priorityStr || "bang bang" == priorityStr || "2" == priorityStr || "high" == priorityStr) {
            priority = Task.Priority.MEDIUM
        }
        return priority
    }

    // priorityHelper parses the string and sets the Task's importance
    private fun priorityHelper(task: Task) {
        var inputText = task.title
        val importanceStrings = arrayOf(
                """()((^|[^\w!])!+|(^|[^\w!])!\d)($|[^\w!])""",
                """()(?i)((\s?bang){1,})$""",
                """(?i)(\spriority\s?(\d)$)""",
                """(?i)(\sbang\s?(\d)$)""",
                """(?i)()(\shigh(est)?|\slow(est)?|\stop|\sleast) ?priority$"""
        )
        for (importanceString in importanceStrings) {
            val importancePattern = Pattern.compile(importanceString)
            while (true) {
                val m = importancePattern.matcher(inputText)
                if (m.find()) {
                    task.priority = strToPriority(m.group(2).trim { it <= ' ' })
                    val start = if (m.start() == 0) 0 else m.start() + 1
                    inputText = inputText!!.substring(0, start) + inputText.substring(m.end())
                } else {
                    break
                }
            }
        }
        task.title = inputText!!.trim { it <= ' ' }
    }

    // helper for dayHelper. Converts am/pm to an int 0/1.
    private fun ampmToNumber(amPmString: String?): Int {
        var time = Calendar.PM
        if (amPmString == null) {
            return time
        }
        val text = amPmString.lowercase(Locale.getDefault()).trim { it <= ' ' }
        if (text == "am" || text == "a.m" || text == "a") {
            time = Calendar.AM
        }
        if (text == "pm" || text == "p.m" || text == "p") {
            time = Calendar.PM
        }
        return time
    }

    private fun removeIfParenthetical(m: Matcher, inputText: String?): String? {
        val s = m.group()
        return if (s.startsWith("(") && s.endsWith(")")) {
            inputText!!.substring(0, m.start()) + inputText.substring(m.end())
        } else inputText
    }

    private fun stripParens(s: String): String {
        var s = s
        if (s.startsWith("(")) {
            s = s.substring(1)
        }
        if (s.endsWith(")")) {
            s = s.substring(0, s.length - 1)
        }
        return s
    }

    // ---------------------DATE--------------------------
    // Handles setting the task's date.
    // Day of week (e.g. Monday, Tuesday,..) is overridden by a set date (e.g. October 23 2013).
    // Vague times (e.g. breakfast, night) are overridden by a set time (9 am, at 10, 17:00)
    private fun dayHelper(task: Task) {
        var inputText = task.title
        var cal: Calendar? = null
        var containsSpecificTime = false
        val daysOfWeek = arrayOf(
                "(?i)(\\(|\\b)today(\\)|\\b)",
                "(?i)(\\(|\\b)tomorrow(\\)|\\b)",
                "(?i)(\\(|\\b)mon(day(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)tue(sday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)wed(nesday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)thu(rsday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)fri(day(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)sat(urday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)sun(day(\\)|\\b)|(\\)|\\.))"
        )
        for (date in daysOfWeek) {
            val pattern = Pattern.compile(date)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                val toParse = stripParens(m.group(0))
                cal = AstridChronic.parse(toParse).beginCalendar
                inputText = removeIfParenthetical(m, inputText)
                // then put it into task
            }
        }
        val dates = arrayOf(
                "(?i)(\\(|\\b)(jan(\\.|uary))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(feb(\\.|ruary))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(mar(\\.|ch))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(apr(\\.|il))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(may())(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(jun(\\.|e))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(jul(\\.|y))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(aug(\\.|ust))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(sep(\\.|tember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(oct(\\.|ober))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(nov(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(dec(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)"
        )

        // m.group(2) = "month"
        // m.group(5) = "day"
        for (date in dates) {
            val pattern = Pattern.compile(date)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                val dateCal = Chronic.parse(m.group(2)).beginCalendar
                if (m.group(5) != null) {
                    dateCal[Calendar.DAY_OF_MONTH] = m.group(5).toInt()
                }
                val today = Calendar.getInstance()
                if (m.group(6) != null) {
                    dateCal[Calendar.YEAR] = m.group(6).trim { it <= ' ' }.toInt()
                } else if (today[Calendar.MONTH] - dateCal[Calendar.MONTH]
                        > 1) { // if more than a month in the past
                    dateCal[Calendar.YEAR] = dateCal[Calendar.YEAR] + 1
                }
                if (cal == null) {
                    cal = dateCal
                } else {
                    cal[Calendar.DAY_OF_MONTH] = dateCal[Calendar.DAY_OF_MONTH]
                    cal[Calendar.MONTH] = dateCal[Calendar.MONTH]
                    cal[Calendar.YEAR] = dateCal[Calendar.YEAR]
                }
                inputText = removeIfParenthetical(m, inputText)
            }
        }

        // for dates in the format MM/DD
        val p = Pattern.compile(
                "(?i)(\\(|\\b)(1[0-2]|0?[1-9])(\\/|-)(3[0-1]|[0-2]?[0-9])(\\/|-)?(\\d{4}|\\d{2})?(\\)|\\b)")
        val match = p.matcher(inputText)
        if (match.find()) {
            val dCal = Calendar.getInstance()
            setCalendarToDefaultTime(dCal)
            dCal[Calendar.MONTH] = match.group(2).trim { it <= ' ' }.toInt() - 1
            dCal[Calendar.DAY_OF_MONTH] = match.group(4).toInt()
            if (match.group(6) != null && match.group(6).trim { it <= ' ' } != "") {
                var yearString = match.group(6)
                if (match.group(6).length == 2) {
                    yearString = "20" + match.group(6)
                }
                dCal[Calendar.YEAR] = yearString.toInt()
            }
            if (cal == null) {
                cal = dCal
            } else {
                cal[Calendar.DAY_OF_MONTH] = dCal[Calendar.DAY_OF_MONTH]
                cal[Calendar.MONTH] = dCal[Calendar.MONTH]
                cal[Calendar.YEAR] = dCal[Calendar.YEAR]
            }
            inputText = removeIfParenthetical(match, inputText)
        }
        val dayTimes = HashMap<String, Int>()
        dayTimes["(?i)\\bbreakfast\\b"] = 8
        dayTimes["(?i)\\blunch\\b"] = 12
        dayTimes["(?i)\\bsupper\\b"] = 18
        dayTimes["(?i)\\bdinner\\b"] = 18
        dayTimes["(?i)\\bbrunch\\b"] = 10
        dayTimes["(?i)\\bmorning\\b"] = 8
        dayTimes["(?i)\\bafternoon\\b"] = 15
        dayTimes["(?i)\\bevening\\b"] = 19
        dayTimes["(?i)\\bnight\\b"] = 19
        dayTimes["(?i)\\bmidnight\\b"] = 0
        dayTimes["(?i)\\bnoon\\b"] = 12
        val keys: Set<String> = dayTimes.keys
        for (dayTime in keys) {
            val pattern = Pattern.compile(dayTime)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                containsSpecificTime = true
                val timeHour = dayTimes[dayTime]!!
                val dayTimesCal = Calendar.getInstance()
                setCalendarToDefaultTime(dayTimesCal)
                dayTimesCal[Calendar.HOUR] = timeHour
                if (cal == null) {
                    cal = dayTimesCal
                } else {
                    setCalendarToDefaultTime(cal)
                    cal[Calendar.HOUR] = timeHour
                }
            }
        }
        val times = arrayOf( // [time] am/pm
                "(?i)(\\b)([01]?\\d):?([0-5]\\d)? ?([ap]\\.?m?\\.?)\\b",  // army time
                "(?i)\\b(([0-2]?[0-9]):([0-5][0-9]))(\\b)",  // [int] o'clock
                "(?i)\\b(([01]?\\d)() ?o'? ?clock) ?([ap]\\.?m\\.?)?\\b",  // at [int]
                "(?i)(\\bat) ([01]?\\d)()($|\\D($|\\D))" // m.group(2) holds the hour
                // m.group(3) holds the minutes
                // m.group(4) holds am/pm
        )
        for (time in times) {
            val pattern = Pattern.compile(time)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                containsSpecificTime = true
                val today = Calendar.getInstance()
                val timeCal = Calendar.getInstance()
                setCalendarToDefaultTime(timeCal)
                timeCal[Calendar.HOUR] = m.group(2).toInt()
                if (m.group(3) != null && m.group(3).trim { it <= ' ' } != "") {
                    timeCal[Calendar.MINUTE] = m.group(3).toInt()
                } else {
                    timeCal[Calendar.MINUTE] = 0
                }
                if (m.group(2).toInt() <= 12) {
                    timeCal[Calendar.AM_PM] = ampmToNumber(m.group(4))
                }

                // sets it to the next occurrence of that hour if no am/pm is provided. doesn't include
                // military time
                if (m.group(2).toInt() <= 12
                        && (m.group(4) == null || m.group(4).trim { it <= ' ' } == "")) {
                    while (timeCal.time.time < today.time.time) {
                        timeCal[Calendar.HOUR_OF_DAY] = timeCal[Calendar.HOUR_OF_DAY] + 12
                    }
                } else { // if am/pm is provided and the time is in the past, set it to the next day.
                    // Military time included.
                    if (timeCal[Calendar.HOUR] != 0
                            && timeCal.time.time < today.time.time) {
                        timeCal[Calendar.DAY_OF_MONTH] = timeCal[Calendar.DAY_OF_MONTH] + 1
                    }
                    if (timeCal[Calendar.HOUR] == 0) {
                        timeCal[Calendar.HOUR] = 12
                    }
                }
                if (cal == null) {
                    cal = timeCal
                } else {
                    cal[Calendar.HOUR] = timeCal[Calendar.HOUR]
                    cal[Calendar.MINUTE] = timeCal[Calendar.MINUTE]
                    cal[Calendar.SECOND] = timeCal[Calendar.SECOND]
                    cal[Calendar.AM_PM] = timeCal[Calendar.AM_PM]
                }
                break
            }
        }
        if (cal
                != null) { // if at least one of the above has been called, write to task. else do nothing.
            if (!isNullOrEmpty(inputText)) {
                task.title = inputText
            }
            if (containsSpecificTime) {
                task.dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, cal.time.time)
            } else {
                task.dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, cal.time.time)
            }
        }
    }

    // ---------------------DATE--------------------------
    // Parses through the text and sets the frequency of the task.
    private fun repeatHelper(task: Task) {
        val inputText = task.title
        val repeatTimes = HashMap<String, Frequency>()
        repeatTimes["(?i)\\bevery ?\\w{0,6} days?\\b"] = Frequency.DAILY
        repeatTimes["(?i)\\bevery ?\\w{0,6} ?nights?\\b"] = Frequency.DAILY
        repeatTimes["(?i)\\bevery ?\\w{0,6} ?mornings?\\b"] = Frequency.DAILY
        repeatTimes["(?i)\\bevery ?\\w{0,6} ?evenings?\\b"] = Frequency.DAILY
        repeatTimes["(?i)\\bevery ?\\w{0,6} ?afternoons?\\b"] = Frequency.DAILY
        repeatTimes["(?i)\\bevery \\w{0,6} ?weeks?\\b"] = Frequency.WEEKLY
        repeatTimes["(?i)\\bevery \\w{0,6} ?(mon|tues|wednes|thurs|fri|satur|sun)days?\\b"] = Frequency.WEEKLY
        repeatTimes["(?i)\\bevery \\w{0,6} ?months?\\b"] = Frequency.MONTHLY
        repeatTimes["(?i)\\bevery \\w{0,6} ?years?\\b"] = Frequency.YEARLY
        val repeatTimesIntervalOne = HashMap<String, Frequency>()
        // pre-determined intervals of 1
        repeatTimesIntervalOne["(?i)\\bdaily\\b"] = Frequency.DAILY
        repeatTimesIntervalOne["(?i)\\beveryday\\b"] = Frequency.DAILY
        repeatTimesIntervalOne["(?i)\\bweekly\\b"] = Frequency.WEEKLY
        repeatTimesIntervalOne["(?i)\\bmonthly\\b"] = Frequency.MONTHLY
        repeatTimesIntervalOne["(?i)\\byearly\\b"] = Frequency.YEARLY
        val keys: Set<String> = repeatTimes.keys
        for (repeatTime in keys) {
            val pattern = Pattern.compile(repeatTime)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                val rtime = repeatTimes[repeatTime]
                val recur = newRecur()
                recur.setFrequency(rtime!!.name)
                recur.interval = findInterval(inputText)
                task.recurrence = recur.toString()
                return
            }
        }
        for (repeatTimeIntervalOne in repeatTimesIntervalOne.keys) {
            val pattern = Pattern.compile(repeatTimeIntervalOne)
            val m = pattern.matcher(inputText)
            if (m.find()) {
                val rtime = repeatTimesIntervalOne[repeatTimeIntervalOne]
                val recur = newRecur()
                recur.setFrequency(rtime!!.name)
                recur.interval = 1
                task.recurrence = recur.toString()
                return
            }
        }
    }

    // helper method for repeatHelper.
    private fun findInterval(inputText: String?): Int {
        val wordsToNum = HashMap<String, Int?>()
        val words = arrayOf(
                "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine", "ten", "eleven", "twelve"
        )
        for (i in words.indices) {
            wordsToNum[words[i]] = i + 1
            wordsToNum[(i + 1).toString()] = i + 1
        }
        wordsToNum["other"] = 2
        val pattern = Pattern.compile("(?i)\\bevery (\\w*)\\b")
        var interval = 1
        val m = pattern.matcher(inputText)
        if (m.find() && m.group(1) != null) {
            val intervalStr = m.group(1)
            if (wordsToNum.containsKey(intervalStr)) {
                interval = wordsToNum[intervalStr]!!
            } else {
                try {
                    interval = intervalStr.toInt()
                } catch (e: NumberFormatException) {
                    // Ah well
                    Timber.e(e)
                }
            }
        }
        return interval
    }

    // helper method for DayHelper. Resets the time on the calendar to 00:00:00 am
    private fun setCalendarToDefaultTime(cal: Calendar) {
        cal[Calendar.HOUR] = 0
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.AM_PM] = Calendar.AM
    }
}