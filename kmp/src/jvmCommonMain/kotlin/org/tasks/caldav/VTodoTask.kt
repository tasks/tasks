package org.tasks.caldav

import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.property.Clazz
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Duration
import net.fortuna.ical4j.model.property.ExDate
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.Organizer
import net.fortuna.ical4j.model.property.RDate
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Status
import java.io.OutputStream
import java.util.LinkedList

interface VTodoTask {
    var uid: String?
    var sequence: Int?
    var createdAt: Long?
    var lastModified: Long?
    var summary: String?
    var location: String?
    var geoPosition: Geo?
    var description: String?
    var color: Int?
    var url: String?
    var organizer: Organizer?
    var priority: Int
    var classification: Clazz?
    var status: Status?
    var dtStart: DtStart?
    var due: Due?
    var duration: Duration?
    var completedAt: Completed?
    var percentComplete: Int?
    var rRule: RRule?
    val rDates: LinkedList<RDate>
    val exDates: LinkedList<ExDate>
    val categories: LinkedList<String>
    var comment: String?
    var relatedTo: LinkedList<RelatedTo>
    val unknownProperties: LinkedList<Property>
    val alarms: LinkedList<VAlarm>

    fun write(os: OutputStream)
}
