package org.tasks.tasklist

sealed interface TasksResults {
    data object Loading : TasksResults
    data class Results(val tasks: SectionedDataSource) : TasksResults
}
