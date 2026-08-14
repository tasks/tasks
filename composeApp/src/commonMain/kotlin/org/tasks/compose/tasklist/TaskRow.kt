package org.tasks.compose.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.todoroo.astrid.core.SortHelper
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.INDENT_STEP_DP
import org.tasks.compose.priorityColor
import org.tasks.compose.chips.Chip
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.chips.ChipGroup
import org.tasks.compose.chips.StartDateChip
import org.tasks.compose.chips.SubtaskChip
import org.tasks.data.TaskContainer
import org.tasks.data.subtaskKey
import org.tasks.data.isHidden
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import org.tasks.kmp.formatTime
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.dueDateOverdue
import org.tasks.time.startOfDay
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.show_less
import tasks.kmp.generated.resources.show_more

internal enum class RowState {
    Draw,

    Doomed,

    Hidden,
}

internal fun rowState(deletions: Map<String, Boolean>, task: TaskContainer): RowState =
    when (deletions[subtaskKey(task.id, task.task.remoteId)]) {
        null -> RowState.Draw
        true -> RowState.Doomed
        false -> RowState.Hidden
    }

internal const val COMPLETE_BUTTON_TAG = "task-row-complete"

@Composable
internal fun TaskRow(
    task: TaskContainer,
    doomed: Boolean,
    filter: Filter,
    groupMode: Int,
    chipDataProvider: ChipDataProvider,
    is24Hour: Boolean,
    dateFormatter: DateFormatter?,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtasks: () -> Unit,
    onFilterClick: (Filter) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val checkColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.outline
    } else {
        priorityColor(task.priority)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !doomed, onClick = onClick)
            .then(
                if (doomed) {
                    Modifier.background(MaterialTheme.colorScheme.errorContainer)
                } else {
                    Modifier
                }
            )
            .padding(
                start = (INDENT_STEP_DP * task.indent).dp,
                end = 16.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(
            onClick = onToggleComplete,
            enabled = !doomed,
            modifier = Modifier.size(48.dp).testTag(COMPLETE_BUTTON_TAG),
        ) {
            Icon(
                imageVector = if (task.isCompleted)
                    Icons.Filled.CheckCircle
                else
                    Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 12.dp, bottom = 12.dp)) {
            val dueDateText = remember(task.dueDate, groupMode, is24Hour, dateFormatter) {
                if (!task.hasDueDate()) {
                    null
                } else if (groupMode == SortHelper.SORT_DUE
                    && (task.sortGroup ?: 0) >= currentTimeMillis().startOfDay()
                ) {
                    if (task.hasDueTime()) formatTime(task.dueDate, is24Hour) else null
                } else {
                    dateFormatter?.relativeDateTime(task.dueDate)
                }
            }
            val isOverdue = !task.isCompleted && dueDateOverdue(task.dueDate)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val titleColor = when {
                    doomed -> MaterialTheme.colorScheme.onErrorContainer
                    task.isCompleted || task.task.isHidden ->
                        MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Markdown(
                    content = task.title ?: "",
                    colors = markdownColor(text = titleColor),
                    typography = markdownTypography(
                        paragraph = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isCompleted || doomed) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        ),
                    ),
                    animations = markdownAnimations(animateTextSize = { this }),
                    modifier = Modifier.weight(1f),
                )
                if (dueDateText != null) {
                    Text(
                        text = dueDateText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (!task.notes.isNullOrBlank()) {
                val content = task.notes!!.trim()
                var expanded by remember { mutableStateOf(false) }
                val lines = content.lines()
                val hasMore = lines.size > 2
                val mdColors = markdownColor(
                    text = if (task.task.isHidden) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                val mdTypography = markdownTypography(
                    paragraph = MaterialTheme.typography.bodyMedium,
                )
                Markdown(
                    content = if (expanded || !hasMore) content
                              else lines.take(2).joinToString("\n"),
                    colors = mdColors,
                    typography = mdTypography,
                    animations = markdownAnimations(
                        animateTextSize = { this },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasMore) {
                    Text(
                        text = stringResource(if (expanded) Res.string.show_less else Res.string.show_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .clickable { expanded = !expanded }
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            }
            val startDate = task.task.hideUntil
            val showStartDate = task.task.isHidden
                    && startDate != task.dueDate
                    && startDate != task.dueDate.startOfDay()
            val showList = task.indent == 0
                    && filter !is CaldavFilter
                    && chipDataProvider.getCaldavList(task.caldav) != null
            val showPlace = task.hasLocation()
                    && filter !is PlaceFilter
            val tags = task.tagsString
                ?.takeIf { it.isNotBlank() }
                ?.split(",")
                ?.let { uuids ->
                    if (filter is TagFilter) uuids - filter.uuid else uuids
                }
                ?.mapNotNull { chipDataProvider.getTag(it) }
                ?.sortedBy { it.title }
                ?: emptyList()
            val hasChips = task.hasChildren() || showStartDate || showList || showPlace || tags.isNotEmpty()
            if (hasChips) {
                ChipGroup(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    if (task.hasChildren()) {
                        SubtaskChip(
                            collapsed = task.isCollapsed,
                            children = task.chipCount,
                            onClick = onToggleSubtasks,
                            enabled = !doomed,
                        )
                    }
                    if (showStartDate) {
                        StartDateChip(
                            sortGroup = task.sortGroup,
                            startDate = startDate,
                            compact = true,
                            timeOnly = false,
                            chipColor = chipColor(0, isDark),
                            dateFormatter = dateFormatter,
                        )
                    }
                    if (showPlace) {
                        task.location?.let { location ->
                            Chip(
                                text = location.place.displayName,
                                icon = location.place.icon ?: "place",
                                color = chipColor(location.place.color, isDark),
                                onClick = { onFilterClick(PlaceFilter(location.place)) },
                            )
                        }
                    }
                    if (showList) {
                        chipDataProvider.getCaldavList(task.caldav)?.let { list ->
                            Chip(
                                text = list.title,
                                icon = list.icon ?: "list",
                                color = chipColor(list.tint, isDark),
                                onClick = { onFilterClick(list) },
                            )
                        }
                    }
                    tags.forEach { tag ->
                        Chip(
                            text = tag.title,
                            icon = tag.icon ?: "label",
                            color = chipColor(tag.tint, isDark),
                            onClick = { onFilterClick(tag) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun chipColor(seedColor: Int, isDark: Boolean): Color {
    return if (seedColor == 0) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(
            org.tasks.themes.chipColors(seedColor, isDark).backgroundColor
                    or 0xFF000000.toInt()
        )
    }
}
