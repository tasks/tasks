package org.tasks

import androidx.navigation3.runtime.NavKey
import org.tasks.data.entity.Task

internal sealed interface OpenTask {
    data object Ignore : OpenTask

    data object Replace : OpenTask

    data object Stack : OpenTask

    data class Resume(val index: Int) : OpenTask
}

internal fun openTaskFromList(
    backStack: List<NavKey>,
    destination: TaskEditDestination,
    heldByEditor: Boolean,
    doomedByEditor: Boolean,
): OpenTask = when {
    backStack.lastOrNull() !is TaskEditDestination -> OpenTask.Replace
    doomedByEditor -> OpenTask.Ignore
    heldByEditor -> openSubtask(backStack, destination)
    else -> OpenTask.Replace
}

internal fun openSubtask(backStack: List<NavKey>, destination: TaskEditDestination): OpenTask {
    if (backStack.lastOrNull() !is TaskEditDestination) {
        return OpenTask.Ignore
    }
    val open = backStack.indexOfLast {
        it is TaskEditDestination && it.namesTheSameTaskAs(destination)
    }
    return if (open >= 0) OpenTask.Resume(open) else OpenTask.Stack
}

private fun TaskEditDestination.namesTheSameTaskAs(other: TaskEditDestination): Boolean =
    (other.taskId > 0 && taskId == other.taskId) ||
            (other.remoteId.isRealUuid && remoteId == other.remoteId)

private val String.isRealUuid: Boolean
    get() = isNotBlank() && this != Task.NO_UUID
