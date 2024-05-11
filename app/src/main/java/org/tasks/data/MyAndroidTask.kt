package org.tasks.data

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import at.bitfire.ical4android.AndroidTask
import at.bitfire.ical4android.BatchOperation
import at.bitfire.ical4android.BatchOperation.CpoBuilder.Companion.newInsert
import at.bitfire.ical4android.BatchOperation.CpoBuilder.Companion.newUpdate
import at.bitfire.ical4android.ICalendar
import at.bitfire.ical4android.Ical4Android
import at.bitfire.ical4android.Task
import at.bitfire.ical4android.UnknownProperty
import at.bitfire.ical4android.util.MiscUtils.CursorHelper.toValues
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.parameter.Related
import net.fortuna.ical4j.model.property.Action
import org.dmfs.tasks.contract.TaskContract
import org.tasks.data.OpenTaskDao.Companion.getLong
import java.util.Locale
import java.util.logging.Level

class MyAndroidTask() : AndroidTask(null) {

    constructor(cursor: Cursor) : this() {
        val values = cursor.toValues()
        id = cursor.getLong(TaskContract.Tasks._ID)
        task = Task()
        populateTask(values)
        if (values.containsKey(TaskContract.Properties.PROPERTY_ID)) {
            // process the first property, which is combined with the task row
            populateProperty(values)

            while (cursor.moveToNext()) {
                // process the other properties
                populateProperty(cursor.toValues(true))
            }
        }
    }

    constructor(task: Task, id: Long? = null) : this() {
        this.task = task
        this.id = id
    }

    val isNew: Boolean
        get() = id == null

    fun toBuilder(uri: Uri): BatchOperation.CpoBuilder {
        val builder = id
                ?.let { newUpdate(ContentUris.withAppendedId(uri, it)) }
                ?: newInsert(uri)
        buildTask(builder, true)
        if (!isNew) {
            builder.remove(TaskContract.Tasks._UID)
        }
        return builder
                .remove(TaskContract.Tasks.CREATED)
                .remove(TaskContract.Tasks.LAST_MODIFIED)
                .remove(TaskContract.Tasks._DIRTY)
                .remove(TaskContract.Tasks.SYNC_VERSION)
    }

    fun enqueueProperties(uri: Uri, batch: MutableList<BatchOperation.CpoBuilder>, idxTask: Int?) {
        insertAlarms(batch, idxTask, uri)
        insertCategories(batch, idxTask, uri)
        insertRelatedTo(batch, idxTask, uri)
        insertUnknownProperties(batch, idxTask, uri)
    }

    private fun insertAlarms(batch: MutableList<BatchOperation.CpoBuilder>, idxTask: Int?, uri: Uri) {
        val task = requireNotNull(task)
        for (alarm in task.alarms) {
            val (alarmRef, minutes) = ICalendar.vAlarmToMin(alarm, task, true) ?: continue
            val ref = when (alarmRef) {
                Related.END ->
                    TaskContract.Property.Alarm.ALARM_REFERENCE_DUE_DATE
                else /* Related.START is the default value */ ->
                    TaskContract.Property.Alarm.ALARM_REFERENCE_START_DATE
            }

            val alarmType = when (alarm.action?.value?.uppercase(Locale.ROOT)) {
                Action.AUDIO.value ->
                    TaskContract.Property.Alarm.ALARM_TYPE_SOUND
                Action.DISPLAY.value ->
                    TaskContract.Property.Alarm.ALARM_TYPE_MESSAGE
                Action.EMAIL.value ->
                    TaskContract.Property.Alarm.ALARM_TYPE_EMAIL
                else ->
                    TaskContract.Property.Alarm.ALARM_TYPE_NOTHING
            }

            val builder = newInsert(uri)
                    .withTaskId(TaskContract.Property.Alarm.TASK_ID, idxTask)
                    .withValue(TaskContract.Property.Alarm.MIMETYPE, TaskContract.Property.Alarm.CONTENT_ITEM_TYPE)
                    .withValue(TaskContract.Property.Alarm.MINUTES_BEFORE, minutes)
                    .withValue(TaskContract.Property.Alarm.REFERENCE, ref)
                    .withValue(TaskContract.Property.Alarm.MESSAGE, alarm.description?.value ?: alarm.summary)
                    .withValue(TaskContract.Property.Alarm.ALARM_TYPE, alarmType)

            Ical4Android.log.log(Level.FINE, "Inserting alarm", builder.build())
            batch.add(builder)
        }
    }

    private fun insertCategories(batch: MutableList<BatchOperation.CpoBuilder>, idxTask: Int?, uri: Uri) {
        for (category in requireNotNull(task).categories) {
            val builder = newInsert(uri)
                    .withTaskId(TaskContract.Property.Category.TASK_ID, idxTask)
                    .withValue(TaskContract.Property.Category.MIMETYPE, TaskContract.Property.Category.CONTENT_ITEM_TYPE)
                    .withValue(TaskContract.Property.Category.CATEGORY_NAME, category)
            Ical4Android.log.log(Level.FINE, "Inserting category", builder.build())
            batch.add(builder)
        }
    }

    private fun insertRelatedTo(batch: MutableList<BatchOperation.CpoBuilder>, idxTask: Int?, uri: Uri) {
        for (relatedTo in requireNotNull(task).relatedTo) {
            val relType = when ((relatedTo.getParameter(Parameter.RELTYPE) as RelType?)) {
                RelType.CHILD ->
                    TaskContract.Property.Relation.RELTYPE_CHILD
                RelType.SIBLING ->
                    TaskContract.Property.Relation.RELTYPE_SIBLING
                else /* RelType.PARENT, default value */ ->
                    TaskContract.Property.Relation.RELTYPE_PARENT
            }
            val builder = newInsert(uri)
                    .withTaskId(TaskContract.Property.Relation.TASK_ID, idxTask)
                    .withValue(TaskContract.Property.Relation.MIMETYPE, TaskContract.Property.Relation.CONTENT_ITEM_TYPE)
                    .withValue(TaskContract.Property.Relation.RELATED_UID, relatedTo.value)
                    .withValue(TaskContract.Property.Relation.RELATED_TYPE, relType)
            Ical4Android.log.log(Level.FINE, "Inserting relation", builder.build())
            batch.add(builder)
        }
    }

    private fun insertUnknownProperties(batch: MutableList<BatchOperation.CpoBuilder>, idxTask: Int?, uri: Uri) {
        for (property in requireNotNull(task).unknownProperties) {
            if (property.value.length > UnknownProperty.MAX_UNKNOWN_PROPERTY_SIZE) {
                Ical4Android.log.warning("Ignoring unknown property with ${property.value.length} octets (too long)")
                return
            }

            val builder = newInsert(uri)
                    .withTaskId(TaskContract.Properties.TASK_ID, idxTask)
                    .withValue(TaskContract.Properties.MIMETYPE, UnknownProperty.CONTENT_ITEM_TYPE)
                    .withValue(UNKNOWN_PROPERTY_DATA, UnknownProperty.toJsonString(property))
            Ical4Android.log.log(Level.FINE, "Inserting unknown property", builder.build())
            batch.add(builder)
        }
    }
}