package org.tasks.viewmodel

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import org.tasks.compose.pickers.resolveStartDate
import org.tasks.compose.pickers.startDayOf
import org.tasks.compose.pickers.startSelectionDays
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task

internal fun TaskEditViewModel.State.mergedWith(dbTask: Task): TaskEditViewModel.State {
    if (isLoading) {
        return this
    }
    val current = task
    val original = originalTask
    if (dbTask.sameEditableContentAs(original)) {
        return this
    }
    if (dbTask.isDeleted && !original.isDeleted) {
        return copy(deleted = true)
    }
    val merged = current.copy(
        title = merge(current.title, original.title, dbTask.title),
        priority = merge(current.priority, original.priority, dbTask.priority),
        dueDate = merge(current.dueDate, original.dueDate, dbTask.dueDate),
        completionDate = merge(current.completionDate, original.completionDate, dbTask.completionDate),
        deletionDate = merge(current.deletionDate, original.deletionDate, dbTask.deletionDate),
        notes = merge(current.notes, original.notes, dbTask.notes),
        estimatedSeconds = merge(current.estimatedSeconds, original.estimatedSeconds, dbTask.estimatedSeconds),
        elapsedSeconds = merge(current.elapsedSeconds, original.elapsedSeconds, dbTask.elapsedSeconds),
        timerStart = merge(current.timerStart, original.timerStart, dbTask.timerStart),
        ringFlags = merge(current.ringFlags, original.ringFlags, dbTask.ringFlags),
        recurrence = merge(current.recurrence, original.recurrence, dbTask.recurrence),
        repeatFrom = merge(current.repeatFrom, original.repeatFrom, dbTask.repeatFrom),
        calendarURI = merge(current.calendarURI, original.calendarURI, dbTask.calendarURI),
        isCollapsed = merge(current.isCollapsed, original.isCollapsed, dbTask.isCollapsed),
        parent = merge(current.parent, original.parent, dbTask.parent),
        order = merge(current.order, original.order, dbTask.order),
        readOnly = merge(current.readOnly, original.readOnly, dbTask.readOnly),
        modificationDate = dbTask.modificationDate,
        reminderLast = dbTask.reminderLast,
    )
    val start = reconcileStartDate(dbTask, merged.dueDate)
    return copy(
        task = merged.copy(hideUntil = start.hideUntil),
        originalTask = dbTask,
        startDay = start.selectedDay,
        startTime = start.selectedTime,
        originalStartDay = start.baselineDay,
        originalStartTime = start.baselineTime,
    )
}

internal fun TaskEditViewModel.State.reconcileStartDate(
    dbTask: Task,
    mergedDueDate: Long,
): StartReconciliation {
    val localStartDate = startDayOf(startDay)
    val startModifiedLocally = startDay != originalStartDay || startTime != originalStartTime
    val startModifiedExternally = dbTask.hideUntil != originalTask.hideUntil
    val dueModifiedLocally = task.dueDate != originalTask.dueDate
    val backendStoresStartDate = originalList?.account?.syncsStartDate == true
    val keepLocalStart = startModifiedLocally ||
        (dueModifiedLocally && localStartDate.isRelative) ||
        (!startModifiedExternally && !backendStoresStartDate)
    val selectedDay: Long
    val selectedTime: Int
    val hideUntil: Long
    if (keepLocalStart) {
        hideUntil = resolveStartDate(localStartDate, startTime, mergedDueDate)
        selectedDay = startDay
        selectedTime = startTime
    } else {
        val (day, time) = startSelectionDays(dbTask.hideUntil, mergedDueDate)
        selectedDay = day
        selectedTime = time
        hideUntil = dbTask.hideUntil
    }
    val (baselineDay, baselineTime) = if (startModifiedLocally) {
        startSelectionDays(dbTask.hideUntil, dbTask.dueDate)
    } else {
        selectedDay to selectedTime
    }
    return StartReconciliation(hideUntil, selectedDay, selectedTime, baselineDay, baselineTime)
}

internal data class StartReconciliation(
    val hideUntil: Long,
    val selectedDay: Long,
    val selectedTime: Int,
    val baselineDay: Long,
    val baselineTime: Int,
)

private fun <T> merge(current: T, original: T, db: T): T =
    if (current == original) db else current

internal fun mergeAlarms(
    current: ImmutableSet<Alarm>,
    original: ImmutableSet<Alarm>,
    db: ImmutableSet<Alarm>,
): ImmutableSet<Alarm> {
    val originalIdentities = original.identities()
    val deletedLocally = originalIdentities - current.identities()
    val addedLocally = current.filterNot { originalIdentities.contains(it.identity()) }
    return db
        .filterNot { deletedLocally.contains(it.identity()) }
        .plus(addedLocally)
        .distinctBy { it.identity() }
        .toPersistentSet()
}

private data class AlarmIdentity(
    val type: Int,
    val time: Long,
    val repeat: Int,
    val interval: Long,
)

private fun Alarm.identity() = AlarmIdentity(type, time, repeat, interval)

private fun Iterable<Alarm>.identities(): Set<AlarmIdentity> = mapTo(HashSet()) { it.identity() }

internal fun Set<Alarm>.sameAlarmsAs(other: Set<Alarm>): Boolean = identities() == other.identities()

internal fun Task.sameEditableContentAs(other: Task): Boolean =
    copy(
        transitoryData = null,
        id = other.id,
        creationDate = other.creationDate,
        remoteId = other.remoteId,
    ) == other.copy(transitoryData = null)
