package org.tasks.googleapis

data class GoogleTaskList(val id: String, val title: String)

interface GoogleTaskListClient {
    suspend fun createGtaskList(title: String): GoogleTaskList
    suspend fun renameGtaskList(listId: String, title: String)
    suspend fun deleteGtaskList(listId: String)
}
