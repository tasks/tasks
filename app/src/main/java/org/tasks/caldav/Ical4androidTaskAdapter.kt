package org.tasks.caldav

import java.io.OutputStream

class Ical4androidTaskAdapter(
    val task: at.bitfire.ical4android.Task
) : VTodoTask {
    override var uid by task::uid
    override var sequence by task::sequence
    override var createdAt by task::createdAt
    override var lastModified by task::lastModified
    override var summary by task::summary
    override var location by task::location
    override var geoPosition by task::geoPosition
    override var description by task::description
    override var color by task::color
    override var url by task::url
    override var organizer by task::organizer
    override var priority by task::priority
    override var classification by task::classification
    override var status by task::status
    override var dtStart by task::dtStart
    override var due by task::due
    override var duration by task::duration
    override var completedAt by task::completedAt
    override var percentComplete by task::percentComplete
    override var rRule by task::rRule
    override val rDates by task::rDates
    override val exDates by task::exDates
    override val categories by task::categories
    override var comment by task::comment
    override var relatedTo by task::relatedTo
    override val unknownProperties by task::unknownProperties
    override val alarms by task::alarms

    override fun write(os: OutputStream) = task.write(os)
}
