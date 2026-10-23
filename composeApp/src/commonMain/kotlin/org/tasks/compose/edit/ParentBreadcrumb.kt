package org.tasks.compose.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.tasks.compose.components.SymbolIcon
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.no_title
import tasks.kmp.generated.resources.parent_breadcrumb

/**
 * Maximum number of parent levels to resolve when building the breadcrumb.
 * A legitimate chain is a handful of levels deep; anything longer is
 * almost certainly corrupt data (e.g. a parent cycle), so we stop.
 */
internal const val MAX_PARENT_CHAIN_DEPTH = 10

/**
 * Walks the parent chain upwards from [startId] (the task's direct parent).
 *
 * Returns the chain oldest-first: the last element is the task's direct
 * parent. Resolution stops at a top-level task (parent <= 0), a missing
 * row (the parent's calendar may not be synced on this device), a cycle,
 * or [maxDepth].
 *
 * [fetch] is a lambda rather than a [TaskDao] so the traversal is unit
 * testable without a Room database.
 */
internal suspend fun fetchParentChain(
    fetch: suspend (Long) -> Task?,
    startId: Long,
    excludeId: Long,
    maxDepth: Int = MAX_PARENT_CHAIN_DEPTH,
): List<Task> {
    val chain = ArrayList<Task>()
    val visited = hashSetOf(excludeId)
    var next = startId
    while (next > 0 && chain.size < maxDepth) {
        if (!visited.add(next)) break
        val task = fetch(next) ?: break
        chain.add(task)
        next = task.parent
    }
    // walked direct-parent-first; the breadcrumb wants oldest-first
    return chain.asReversed()
}

/**
 * Breadcrumb row for the task edit pane: `In: Project ▸ Parent`, each
 * segment opens that task in the editor. Desktop-only (composeApp
 * module); the Android edit screen does not use this composable.
 *
 * Renders nothing when [task] has no resolvable parent.
 */
@Composable
fun ParentBreadcrumb(
    task: Task,
    onOpenTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val taskDao = koinInject<TaskDao>()
    val chain by produceState<List<Task>>(
        initialValue = emptyList(),
        key1 = task.id,
        key2 = task.parent,
    ) {
        value = if (task.parent > 0) {
            fetchParentChain(taskDao::fetch, task.parent, task.id)
        } else {
            emptyList()
        }
    }
    if (chain.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.parent_breadcrumb),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        chain.forEach { parent ->
            SymbolIcon(
                name = TasksIcons.KEYBOARD_ARROW_RIGHT,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = parent.title.orEmpty().ifBlank {
                    stringResource(Res.string.no_title)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onOpenTask(parent) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
