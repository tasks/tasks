package org.tasks.compose.tasklist

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.todoroo.astrid.core.SortHelper
import org.jetbrains.compose.resources.stringResource
import org.tasks.themes.isDarkTheme
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
    style: TaskRowStyle = DEFAULT_TASK_ROW_STYLE,
    is24Hour: Boolean,
    dateFormatter: DateFormatter?,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubtasks: () -> Unit,
    onFilterClick: (Filter) -> Unit,
    descriptionExpanded: Boolean,
    onToggleDescriptionExpanded: () -> Unit,
) {
    val isDark = isDarkTheme()
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
            SymbolIcon(
                name = if (task.isCompleted) TasksIcons.CHECK_CIRCLE else TasksIcons.RADIO_BUTTON_UNCHECKED,
                filled = task.isCompleted,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = style.rowSpacingDp.dp, bottom = style.rowSpacingDp.dp)) {
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
                    content = titleContent(task.title, style.showFullTitle),
                    colors = markdownColor(text = titleColor),
                    typography = markdownTypography(
                        paragraph = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = style.titleFontSizeSp.sp,
                            textDecoration = if (task.isCompleted || doomed) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                        ),
                    ),
                    components = if (style.showFullTitle) {
                        markdownComponents()
                    } else {
                        singleLineTitleComponents()
                    },
                    animations = markdownAnimations(animateTextSize = { this }),
                    modifier = Modifier.weight(1f),
                )
                if (dueDateText != null) {
                    Text(
                        text = dueDateText!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = style.detailFontSizeSp.sp,
                        ),
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (!task.notes.isNullOrBlank() && style.showDescription) {
                val content = task.notes!!.trim()
                var expanded = descriptionExpanded
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
                    paragraph = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = style.titleFontSizeSp.sp,
                    ),
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
                            .clickable {
                                expanded = !expanded
                                onToggleDescriptionExpanded()
                            }
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
            val hasChips = (task.hasChildren() && style.subtaskChips)
                    || (showStartDate && style.startDateChips)
                    || (showPlace && style.placeChips)
                    || (showList && style.listChips)
                    || (tags.isNotEmpty() && style.tagChips)
            if (hasChips) {
                ChipGroup(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    if (task.hasChildren() && style.subtaskChips) {
                        SubtaskChip(
                            collapsed = task.isCollapsed,
                            children = task.chipCount,
                            onClick = onToggleSubtasks,
                            enabled = !doomed,
                        )
                    }
                    if (showStartDate && style.startDateChips) {
                        StartDateChip(
                            sortGroup = task.sortGroup,
                            startDate = startDate,
                            compact = !style.chipShowText,
                            timeOnly = false,
                            chipColor = chipColor(0, isDark),
                            dateFormatter = dateFormatter,
                        )
                    }
                    if (showPlace && style.placeChips) {
                        task.location?.let { location ->
                            Chip(
                                text = if (style.chipShowText) location.place.displayName else null,
                                icon = if (style.chipShowIcon) location.place.icon ?: "place" else null,
                                color = chipColor(location.place.color, isDark),
                                onClick = { onFilterClick(PlaceFilter(location.place)) },
                            )
                        }
                    }
                    if (showList && style.listChips) {
                        chipDataProvider.getCaldavList(task.caldav)?.let { list ->
                            Chip(
                                text = if (style.chipShowText) list.title else null,
                                icon = if (style.chipShowIcon) list.icon ?: "list" else null,
                                color = chipColor(list.tint, isDark),
                                onClick = { onFilterClick(list) },
                            )
                        }
                    }
                    if (style.tagChips) {
                        tags.forEach { tag ->
                            Chip(
                                text = if (style.chipShowText) tag.title else null,
                                icon = if (style.chipShowIcon) tag.icon ?: "label" else null,
                                color = chipColor(tag.tint, isDark),
                                onClick = { onFilterClick(tag) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Markdown components that clamp the title to a single line with an
 * ellipsis, mirroring the native task list (singleLine=true,
 * ellipsize=end) while the "Full task title" option is off. Inline markdown
 * formatting is preserved; only the line count is capped.
 */
internal fun singleLineTitleComponents(): MarkdownComponents = markdownComponents(
    paragraph = { model ->
        val styledText = buildAnnotatedString {
            pushStyle(model.typography.paragraph.toSpanStyle())
            buildMarkdownAnnotatedString(
                content = model.content,
                node = model.node,
                annotatorSettings = annotatorSettings(),
            )
            pop()
        }
        Text(
            text = styledText,
            style = model.typography.paragraph,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
)

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
