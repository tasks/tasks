package org.tasks.caldav

import org.tasks.icalendar.VTodo
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import java.io.ByteArrayOutputStream
import java.io.StringReader

fun at.bitfire.ical4android.Task.toVTodo(): VTodo =
    parseVTodos(ByteArrayOutputStream().also(::write).toString("UTF-8")).single()

fun VTodo.toIcal4android(): at.bitfire.ical4android.Task =
    at.bitfire.ical4android.Task.tasksFromReader(StringReader(serialize())).first()
