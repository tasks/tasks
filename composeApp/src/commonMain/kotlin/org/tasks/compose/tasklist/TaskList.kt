package org.tasks.compose.tasklist

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.TaskListPreferencesViewModel
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.rememberDateFormatter
import org.tasks.compose.tasklist.RowState
import org.tasks.compose.tasklist.TaskRow
import org.tasks.compose.tasklist.rowState
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.TaskContainer
import org.tasks.data.deletions
import org.tasks.data.subtaskKey
import org.tasks.filters.Filter
import org.tasks.filters.key
import org.tasks.tasklist.SectionedDataSource

@Composable
internal fun TaskList(
    tasks: SectionedDataSource,
    filter: Filter,
    chipDataProvider: ChipDataProvider,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
    onTaskClick: (TaskContainer) -> Unit,
    onCompleteTask: (TaskContainer, Boolean) -> Unit,
    onToggleGroup: (Long) -> Unit = {},
    onToggleSubtasks: (Long, Boolean) -> Unit = { _, _ -> },
    onFilterClick: (Filter) -> Unit = {},
    is24Hour: Boolean = false,
) {
    val dateFormatter = rememberDateFormatter(is24Hour)
    val subtaskTrees = koinInject<SubtaskTreeRegistry>()
    val deletions by remember(subtaskTrees) { subtaskTrees.deletions }
        .collectAsState(initial = emptyMap())
    // Live font size / row spacing from the "Task list options" pane. The
    // pane writes these while the list is on screen, so collect the DataStore
    // flow instead of reading once (same pattern as the per-list sort gate).
    val tasksPreferences = koinInject<TasksPreferences>()
    val fontSize by tasksPreferences
        .flow(TasksPreferences.taskListFontSize, TaskListPreferencesViewModel.DEFAULT_FONT_SIZE)
        .distinctUntilChanged()
        .collectAsState(initial = TaskListPreferencesViewModel.DEFAULT_FONT_SIZE)
    val rowSpacing by tasksPreferences
        .flow(TasksPreferences.taskListRowSpacing, TaskListPreferencesViewModel.DEFAULT_ROW_SPACING)
        .distinctUntilChanged()
        .collectAsState(initial = TaskListPreferencesViewModel.DEFAULT_ROW_SPACING)
    val showFullTitle by tasksPreferences
        .flow(TasksPreferences.taskListShowFullTitle, false)
        .distinctUntilChanged()
        .collectAsState(initial = false)
    val chipAppearance by tasksPreferences
        .flow(
            TasksPreferences.taskListChipAppearance,
            TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_AND_ICON,
        )
        .distinctUntilChanged()
        .collectAsState(initial = TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_AND_ICON)
    val subtaskChips by tasksPreferences
        .flow(TasksPreferences.taskListSubtaskChips, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val startDateChips by tasksPreferences
        .flow(TasksPreferences.taskListStartDateChips, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val placeChips by tasksPreferences
        .flow(TasksPreferences.taskListPlaceChips, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val listChips by tasksPreferences
        .flow(TasksPreferences.taskListListChips, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val tagChips by tasksPreferences
        .flow(TasksPreferences.taskListTagChips, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val showDescription by tasksPreferences
        .flow(TasksPreferences.taskListShowDescription, true)
        .distinctUntilChanged()
        .collectAsState(initial = true)
    val showFullDescription by tasksPreferences
        .flow(TasksPreferences.taskListShowFullDescription, false)
        .distinctUntilChanged()
        .collectAsState(initial = false)
    val rowStyle = remember(
        fontSize, rowSpacing, showFullTitle,
        chipAppearance, subtaskChips, startDateChips, placeChips, listChips, tagChips,
        showDescription, showFullDescription,
    ) {
        taskRowStyle(
            fontSize = fontSize,
            rowSpacingDp = rowSpacing,
            showFullTitle = showFullTitle,
            chipAppearance = chipAppearance,
            subtaskChips = subtaskChips,
            startDateChips = startDateChips,
            placeChips = placeChips,
            listChips = listChips,
            tagChips = tagChips,
            showDescription = showDescription,
            showFullDescription = showFullDescription,
        )
    }
    val expandedDescriptions = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topPadding,
            bottom = 88.dp, // floating toolbar clearance
        ),
    ) {
        items(
            count = tasks.size,
            key = { if (tasks.isHeader(it)) -it.toLong() else tasks.getItem(it).id },
        ) { index ->
            if (tasks.isHeader(index)) {
                val section = tasks.getSection(index)
                SectionHeader(
                    header = if (filter.supportsSorting()) section.header else null,
                    collapsed = section.collapsed,
                    onToggle = { onToggleGroup(section.value) },
                )
                return@items
            }
            val task = tasks.getItem(index)
            val state = rowState(deletions, task)
            if (state == RowState.Hidden) {
                return@items
            }
            TaskRow(
                task = task,
                doomed = state == RowState.Doomed,
                filter = filter,
                groupMode = tasks.groupMode,
                chipDataProvider = chipDataProvider,
                style = rowStyle,
                is24Hour = is24Hour,
                dateFormatter = dateFormatter,
                onClick = { onTaskClick(task) },
                onToggleComplete = { onCompleteTask(task, !task.isCompleted) },
                onToggleSubtasks = { onToggleSubtasks(task.id, !task.isCollapsed) },
                onFilterClick = onFilterClick,
                descriptionExpanded = expandedDescriptions[subtaskKey(task.id, task.task.remoteId)]
                        ?: rowStyle.showFullDescription,
                onToggleDescriptionExpanded = {
                    val key = subtaskKey(task.id, task.task.remoteId)
                    expandedDescriptions[key] = !(expandedDescriptions[key] ?: rowStyle.showFullDescription)
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    header: String?,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    if (header == null) {
        return
    }
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -180f else 0f,
        animationSpec = tween(durationMillis = 250),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SymbolIcon(
            name = TasksIcons.KEYBOARD_ARROW_DOWN,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}
