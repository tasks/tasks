package org.tasks.tasklist

sealed class TasksResults {
    data object Loading : TasksResults()
    data class Results(val tasks: SectionedDataSource) : TasksResults()
}
