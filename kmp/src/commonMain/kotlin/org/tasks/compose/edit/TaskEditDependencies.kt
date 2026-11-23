package org.tasks.compose.edit

import androidx.compose.runtime.staticCompositionLocalOf
import org.tasks.repeats.RepeatRuleToString
import org.tasks.tags.TagPickerViewModel

class TaskEditDependencies(
    val repeatRuleToString: RepeatRuleToString,
    val createTagPickerViewModel: () -> TagPickerViewModel,
    val is24HourFormat: Boolean,
)

val LocalTaskEditDependencies = staticCompositionLocalOf<TaskEditDependencies> {
    error("TaskEditDependencies not provided")
}
