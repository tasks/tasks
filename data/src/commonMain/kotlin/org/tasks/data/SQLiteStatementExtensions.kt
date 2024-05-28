package org.tasks.data

import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.SQLiteStatement
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task

/*
room kmp doesn't support raw query yet 😢
https://issuetracker.google.com/issues/330586815
 */
fun SQLiteStatement.getTasks(): List<TaskContainer> {
    val result = mutableListOf<TaskContainer>()
    val _cursorIndexOfAccountType: Int = getColumnIndex(this, "accountType")
    val _cursorIndexOfParentComplete: Int = getColumnIndex(this, "parentComplete")
    val _cursorIndexOfTagsString: Int = getColumnIndex(this, "tags")
    val _cursorIndexOfChildren: Int = getColumnIndex(this, "children")
    val _cursorIndexOfSortGroup: Int = getColumnIndex(this, "sortGroup")
    val _cursorIndexOfPrimarySort: Int = getColumnIndex(this, "primarySort")
    val _cursorIndexOfSecondarySort: Int = getColumnIndex(this, "secondarySort")
    val _cursorIndexOfIndent: Int = getColumnIndex(this, "indent")
    val _cursorIndexOfId: Int = getColumnIndexOrThrow(this, "_id")
    val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(this, "title")
    val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(this, "importance")
    val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(this, "dueDate")
    val _cursorIndexOfHideUntil: Int = getColumnIndexOrThrow(this, "hideUntil")
    val _cursorIndexOfCreationDate: Int = getColumnIndexOrThrow(this, "created")
    val _cursorIndexOfModificationDate: Int = getColumnIndexOrThrow(this, "modified")
    val _cursorIndexOfCompletionDate: Int = getColumnIndexOrThrow(this, "completed")
    val _cursorIndexOfDeletionDate: Int = getColumnIndexOrThrow(this, "deleted")
    val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(this, "notes")
    val _cursorIndexOfEstimatedSeconds: Int = getColumnIndexOrThrow(this, "estimatedSeconds")
    val _cursorIndexOfElapsedSeconds: Int = getColumnIndexOrThrow(this, "elapsedSeconds")
    val _cursorIndexOfTimerStart: Int = getColumnIndexOrThrow(this, "timerStart")
    val _cursorIndexOfRingFlags: Int = getColumnIndexOrThrow(this, "notificationFlags")
    val _cursorIndexOfReminderLast: Int = getColumnIndexOrThrow(this, "lastNotified")
    val _cursorIndexOfRecurrence: Int = getColumnIndexOrThrow(this, "recurrence")
    val _cursorIndexOfRepeatFrom: Int = getColumnIndexOrThrow(this, "repeat_from")
    val _cursorIndexOfCalendarURI: Int = getColumnIndexOrThrow(this, "calendarUri")
    val _cursorIndexOfRemoteId: Int = getColumnIndexOrThrow(this, "remoteId")
    val _cursorIndexOfIsCollapsed: Int = getColumnIndexOrThrow(this, "collapsed")
    val _cursorIndexOfParent: Int = getColumnIndexOrThrow(this, "parent")
    val _cursorIndexOfOrder: Int = getColumnIndexOrThrow(this, "order")
    val _cursorIndexOfReadOnly: Int = getColumnIndexOrThrow(this, "read_only")
    val _cursorIndexOfId_1: Int = getColumnIndex(this, "cd_id")
    val _cursorIndexOfTask: Int = getColumnIndex(this, "cd_task")
    val _cursorIndexOfCalendar: Int = getColumnIndex(this, "cd_calendar")
    val _cursorIndexOfRemoteId_1: Int = getColumnIndex(this, "cd_remote_id")
    val _cursorIndexOfObj: Int = getColumnIndex(this, "cd_object")
    val _cursorIndexOfEtag: Int = getColumnIndex(this, "cd_etag")
    val _cursorIndexOfLastSync: Int = getColumnIndex(this, "cd_last_sync")
    val _cursorIndexOfDeleted: Int = getColumnIndex(this, "cd_deleted")
    val _cursorIndexOfRemoteParent: Int = getColumnIndex(this, "cd_remote_parent")
    val _cursorIndexOfIsMoved: Int = getColumnIndex(this, "gt_moved")
    val _cursorIndexOfRemoteOrder: Int = getColumnIndex(this, "gt_remote_order")
    val _cursorIndexOfId_2: Int = getColumnIndex(this, "geofence_id")
    val _cursorIndexOfTask_1: Int = getColumnIndex(this, "task")
    val _cursorIndexOfPlace: Int = getColumnIndex(this, "place")
    val _cursorIndexOfIsArrival: Int = getColumnIndex(this, "arrival")
    val _cursorIndexOfIsDeparture: Int = getColumnIndex(this, "departure")
    val _cursorIndexOfId_3: Int = getColumnIndex(this, "place_id")
    val _cursorIndexOfUid: Int = getColumnIndex(this, "uid")
    val _cursorIndexOfName: Int = getColumnIndex(this, "name")
    val _cursorIndexOfAddress: Int = getColumnIndex(this, "address")
    val _cursorIndexOfPhone: Int = getColumnIndex(this, "phone")
    val _cursorIndexOfUrl: Int = getColumnIndex(this, "url")
    val _cursorIndexOfLatitude: Int = getColumnIndex(this, "latitude")
    val _cursorIndexOfLongitude: Int = getColumnIndex(this, "longitude")
    val _cursorIndexOfColor: Int = getColumnIndex(this, "place_color")
    val _cursorIndexOfIcon: Int = getColumnIndex(this, "place_icon")
    val _cursorIndexOfOrder_1: Int = getColumnIndex(this, "place_order")
    val _cursorIndexOfRadius: Int = getColumnIndex(this, "radius")
    while (step()) {
        val task = Task(
            id = getLong(_cursorIndexOfId),
            title = getTextOrNull(_cursorIndexOfTitle),
            priority = getInt(_cursorIndexOfPriority),
            dueDate = getLong(_cursorIndexOfDueDate),
            hideUntil = getLong(_cursorIndexOfHideUntil),
            creationDate = getLong(_cursorIndexOfCreationDate),
            modificationDate = getLong(_cursorIndexOfModificationDate),
            completionDate = getLong(_cursorIndexOfCompletionDate),
            deletionDate = getLong(_cursorIndexOfDeletionDate),
            notes = getTextOrNull(_cursorIndexOfNotes),
            estimatedSeconds = getInt(_cursorIndexOfEstimatedSeconds),
            elapsedSeconds = getInt(_cursorIndexOfElapsedSeconds),
            timerStart = getLong(_cursorIndexOfTimerStart),
            ringFlags = getInt(_cursorIndexOfRingFlags),
            reminderLast = getLong(_cursorIndexOfReminderLast),
            recurrence = getTextOrNull(_cursorIndexOfRecurrence),
            repeatFrom = getInt(_cursorIndexOfRepeatFrom),
            calendarURI = getTextOrNull(_cursorIndexOfCalendarURI),
            remoteId = getTextOrNull(_cursorIndexOfRemoteId),
            isCollapsed = getBoolean(_cursorIndexOfIsCollapsed),
            parent = getLong(_cursorIndexOfParent),
            order = getLongOrNull(_cursorIndexOfOrder),
            readOnly = getBoolean(_cursorIndexOfReadOnly),
        )
        val caldavTask = getLongOrNull(_cursorIndexOfId_1)?.takeIf { it > 0 }?.let {
            CaldavTask(
                id = it,
                task = getLong(_cursorIndexOfTask),
                calendar = getTextOrNull(_cursorIndexOfCalendar),
                remoteId = getTextOrNull(_cursorIndexOfRemoteId_1),
                obj = getTextOrNull(_cursorIndexOfObj),
                etag = getTextOrNull(_cursorIndexOfEtag),
                lastSync = getLong(_cursorIndexOfLastSync),
                deleted = getLong(_cursorIndexOfDeleted),
                remoteParent = getTextOrNull(_cursorIndexOfRemoteParent),
                isMoved = getBoolean(_cursorIndexOfIsMoved),
                remoteOrder = getLong(_cursorIndexOfRemoteOrder),
            )
        }
        val accountType = getIntOrNull(_cursorIndexOfAccountType) ?: 0
        val geofence = getLongOrNull(_cursorIndexOfId_2)?.takeIf { it > 0 }?.let {
            Geofence(
                id = it,
                task = getLong(_cursorIndexOfTask_1),
                place = getTextOrNull(_cursorIndexOfPlace),
                isArrival = getBoolean(_cursorIndexOfIsArrival),
                isDeparture = getBoolean(_cursorIndexOfIsDeparture),
            )
        }
        val place = getLongOrNull(_cursorIndexOfId_3)?.takeIf { it > 0 }?.let {
            Place(
                id = it,
                uid = getTextOrNull(_cursorIndexOfUid),
                name = getTextOrNull(_cursorIndexOfName),
                address = getTextOrNull(_cursorIndexOfAddress),
                phone = getTextOrNull(_cursorIndexOfPhone),
                url = getTextOrNull(_cursorIndexOfUrl),
                latitude = getDouble(_cursorIndexOfLatitude),
                longitude = getDouble(_cursorIndexOfLongitude),
                color = getInt(_cursorIndexOfColor),
                icon = getInt(_cursorIndexOfIcon),
                order = getInt(_cursorIndexOfOrder_1),
                radius = getInt(_cursorIndexOfRadius),
            )
        }
        result.add(
            TaskContainer(
                task = task,
                caldavTask = caldavTask,
                accountType = accountType,
                location = if (geofence != null && place != null) {
                    Location(geofence, place)
                } else {
                   null
                },
                tagsString = getTextOrNull(_cursorIndexOfTagsString),
                indent = getIntOrNull(_cursorIndexOfIndent) ?: 0,
                sortGroup = getLongOrNull(_cursorIndexOfSortGroup),
                children = getIntOrNull(_cursorIndexOfChildren) ?: 0,
                primarySort = getLongOrNull(_cursorIndexOfPrimarySort) ?: 0,
                secondarySort = getLongOrNull(_cursorIndexOfSecondarySort) ?: 0,
                parentComplete = getBooleanOrNull(_cursorIndexOfParentComplete) ?: false,
            )
        )
    }
    return result
}

private fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (index == -1 || isNull(index)) null else this.getText(index)

private fun SQLiteStatement.getLongOrNull(index: Int): Long? =
    if (index == -1 || isNull(index)) null else this.getLong(index)

private fun SQLiteStatement.getIntOrNull(index: Int): Int? =
    if (index == -1 || isNull(index)) null else this.getInt(index)

private fun SQLiteStatement.getBooleanOrNull(index: Int): Boolean? =
    if (index == -1 || isNull(index)) null else this.getBoolean(index)
