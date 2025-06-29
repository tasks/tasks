package org.tasks.todoist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavTask
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber

class TodoistClient(
        private val context: Context,
        private val username: String,
        private val caldavDao: CaldavDao
) {
    private val cache = TodoistLocalCache.getInstance(context, username)

    fun getSession(): String = "todoist-session" // Stubbed implementation

    suspend fun getCollections(): List<TodoistCollection> {
        // Stubbed implementation for Todoist API
        return emptyList()
    }

    suspend fun fetchItems(
        collection: TodoistCollection,
        calendar: CaldavCalendar,
        callback: suspend (Pair<String?, List<TodoistItem>>) -> Unit
    ) {
        // Stubbed implementation for Todoist API
    }

    suspend fun updateItem(collection: TodoistCollection, task: CaldavTask, content: ByteArray): TodoistItem {
        // Stubbed implementation for Todoist API
        return TodoistItem()
    }

    suspend fun deleteItem(collection: TodoistCollection, task: CaldavTask): TodoistItem? {
        // Stubbed implementation for Todoist API
        return null
    }

    suspend fun updateCache(collection: TodoistCollection, items: List<TodoistItem>) {
        // Stubbed implementation for Todoist API
    }

    suspend fun uploadChanges(collection: TodoistCollection, items: List<TodoistItem>) {
        // Stubbed implementation for Todoist API
    }

    suspend fun logout() {
        try {
            TodoistLocalCache.clear(context, username)
            // Stubbed implementation for Todoist API logout
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    suspend fun makeCollection(name: String, color: Int): String {
        // Stubbed implementation for Todoist API
        return "todoist-collection-id"
    }

    suspend fun updateCollection(calendar: CaldavCalendar, name: String, color: Int): String {
        // Stubbed implementation for Todoist API
        return calendar.url ?: "todoist-collection-id"
    }

    suspend fun deleteCollection(calendar: CaldavCalendar) {
        // Stubbed implementation for Todoist API
    }

    companion object {
        private const val TYPE_TASKS = "todoist.tasks"
        private const val MAX_FETCH = 30L

        private fun Int.toHexColor(): String? = takeIf { this != 0 }?.let {
            java.lang.String.format("#%06X", 0xFFFFFF and it)
        }
    }
    
    // Stub classes to replace Etebase specific classes
    class TodoistCollection {
        var uid: String = ""
        var stoken: String = ""
        var meta = TodoistItemMetadata()
        
        fun delete() {}
    }
    
    class TodoistItem {
        var uid: String = ""
        var meta = TodoistItemMetadata()
        var content: ByteArray = ByteArray(0)
        var contentString: String = ""
        var isDeleted: Boolean = false
        
        fun delete() {}
    }
    
    class TodoistItemMetadata {
        var name: String = ""
        var color: String? = null
        var mtime: Long = 0
    }
}
